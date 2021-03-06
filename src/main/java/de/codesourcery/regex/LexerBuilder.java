package de.codesourcery.regex;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LexerBuilder
{
    public StateMachine stateMachine;

    public static final class Configuration
    {
        public final List<LexerRule> rules = new ArrayList<>();
        public final boolean caseInsensitive;

        public Configuration(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        public int indexOf(LexerRule rule)
        {
            final int idx = rules.indexOf( rule );
            if ( idx == -1 ) {
                throw new IllegalArgumentException( "Unknown rule "+rule );
            }
            return idx;
        }

        public Optional<LexerRule> getRule(String ruleName) {
            return rules.stream().filter( r -> r.ruleName.equals( ruleName ) ).findFirst();
        }

        public void addRule(String ruleName,String regex, String tokenType)
        {
            final LexerRule newRule = new LexerRule( ruleName, regex, tokenType);
            if ( rules.stream().anyMatch( x -> x.ruleName.equals( newRule.ruleName ) ) ) {
                throw new IllegalArgumentException( "Duplicate rule '"+newRule.ruleName+"'" );
            }
            rules.add( newRule );
        }
    }

    public static final class LexerRule {

        public final String ruleName;
        public final String regex;
        public final String tokenType;

        public LexerRule(String ruleName, String regex,String tokenType)
        {
            this.ruleName = ruleName;
            this.regex = regex;
            this.tokenType = tokenType;
        }

        @Override
        public boolean equals(Object o)
        {
            if ( o instanceof LexerRule)  {
                final LexerRule lexerRule = (LexerRule) o;
                return ruleName.equals( lexerRule.ruleName );
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return ruleName.hashCode();
        }

        @Override
        public String toString()
        {
            return ruleName+"="+regex+( tokenType == null ? "" : " -> "+tokenType);
        }
    }

    public Configuration parseConfiguration(InputStream configFile,boolean caseInsensitive) throws IOException {

        final Configuration result = new Configuration(caseInsensitive);
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader(configFile) ) )
        {
            String line;
            int lineNo = 1;
outer:
            for ( ; ( line = reader.readLine() ) != null ; lineNo++ )
            {
                line = line.trim();
                if ( line.isBlank() ) {
                    continue;
                }
                System.out.println("LINE: "+line);
                for ( int i = 0 , l = line.length(); i < l ; i++ )
                {
                    final char c = line.charAt( i );
                    if ( c == '#' )
                    {
                        continue outer;
                    }
                    if ( c != ' ' && c != '\t' ) {
                        break;
                    }
                }
                final int idx = line.indexOf( "=" );
                if ( idx == -1 ) {
                    throw new IllegalArgumentException("Missing '=' on line "+lineNo);
                }
                final String tokenType = line.substring(0,idx);
                final String regex = line.substring(idx+1);
                if ( regex.isBlank() ) {
                    throw new IllegalArgumentException( "Blank regex on line "+lineNo );
                }
                // TODO: tokenType is also used as rule name here
                result.addRule( tokenType, regex, tokenType);
            }
        }
        return result;
    }

    /**
     *
     * @param config
     * @return function that takes a set of rule names and returns the one to use.
     */
    public static Function<Set<LexerRule>, LexerRule> getAmbiguousRulesResolver(Configuration config)
    {
        return rules ->
        {
            LexerRule first = null;
            int bestIdx = -1;
            for ( LexerRule rule : rules )
            {
                final int foundIdx = config.indexOf( rule );
                if ( first == null || foundIdx < bestIdx ) {
                    first = rule;
                    bestIdx = foundIdx;
                }
            }
            if ( rules.size() > 1 ) {
                System.err.println("WARNING: Ambiguous lexer rules.");
                System.err.println("WARNING: Candidates: "+rules);
                System.err.println("WARNING: Using "+first);
            }
            return first;
        };
    }

    public String build(InputStream configFile) throws IOException
    {
        final Configuration config = parseConfiguration( configFile, true );

        stateMachine = buildStateMachine( config );

        if ( ! stateMachine.isDFA() )
        {
            stateMachine.toDFA( state -> {}, getAmbiguousRulesResolver( config ) );
            if ( ! stateMachine.isDFA() ) {
                throw new IllegalStateException("DFA conversion failed");
            }
        }

        final Alphabet alphabet = stateMachine.initialState.getAlphabet();
        final Function<State,Integer> stateIdToArrayOffset = state -> state.getID() * (alphabet.size());

        final StringBuilder source = new StringBuilder();

        // alphabet size
        /*
     * Size of alphabet understood by DFA.
     * Index 0 is always the 'any char' transition (if any)
        private static final int ALPHABET_SIZE = 26; // TODO: Generated code
         */
        final String line = "private static final int ALPHABET_SIZE = {0};\n\n";
        source.append( MessageFormat.format( line, alphabet.size() ) );

        // renumber state IDs so they start with zero
        Map<Integer, State> existingStates = stateMachine.initialState.gatherAllStates();
        if ( existingStates.isEmpty() ) {
            throw new IllegalStateException("Graph needs to have at least one node");
        }

        final AtomicInteger id = new AtomicInteger(0);
        existingStates.values().forEach( state ->
        {
            final int newId = id.get();
            id.incrementAndGet();
            if ( state.id != newId ) {
                System.out.println("Re-assigning state "+state.getID()+ " -> "+newId );
            }
            state.id = newId;
        } );
        existingStates = stateMachine.initialState.gatherAllStates();

        // get initial state
        final List<State> entryStates =
                existingStates.values().stream().filter( x -> x.incomingTransitionCount() == 0 ).collect( Collectors.toList() );

        if ( entryStates.isEmpty() ) {
            throw new IllegalStateException("Graph has no entry node ?");
        }
        if ( entryStates.size() != 1 ) {
            throw new IllegalStateException("Graph has more than one entry node ?");
        }

        // gather terminal states
        final Map<Integer,State> terminalStates = new HashMap<>();
        // TODO: State that is not associated with any token type
        // TODO: but is terminal nonetheless ... maybe an artifact of a bug in my NFA -> DFA conversion ?
        State theUnmatchedState = null;
        for ( State state : existingStates.values() )
        {
            if ( state.isTerminalState() )
            {
                if ( state.lexerRule == null ) {
                    if ( theUnmatchedState != null )
                    {
                        throw new IllegalStateException( "Terminal state " + state + " has no lexer rule assigned ?" );
                    }
                    theUnmatchedState = state;
                }
                if ( terminalStates.put( state.getID(), state ) != null ) {
                    throw new IllegalStateException( "State with ID "+state.getID()+" already seen?");
                }
            }
        }

        // Index of initial state
        /*
         * Index of initial state when starting to look for the next token.
        private static final int INITIAL_STATE_OFFSET = -1; // TODO: Generated code
         */
        String line2 = "private static final int INITIAL_STATE_OFFSET = {0}; // TODO: Generated code\n\n";
        source.append( MessageFormat.format( line2, stateIdToArrayOffset.apply( entryStates.get(0) ) ) );

        // character -> index mapping
        final StringBuilder mappingFunc = new StringBuilder("    private int mapChar(char c)\n" +
                "    {\n" +
                "        switch( c ) {\n");

        final Map<Character,Integer> characterMap = new HashMap<>();
        final Map<Integer,Character> intToCharMap = new HashMap<>();
        int idx = 1; // index 0 is reserved for ANY_CHARACTER_INDEX
        final List<Character> sorted = new ArrayList<>( alphabet.characters );
        sorted.sort( Character::compare );
        for ( Character c : sorted)
        {
            if ( c == '\\' ) {
                mappingFunc.append( "        case '\\" + c + "': return " + idx + ";\n" );
            }
            else
            {
                mappingFunc.append( "        case '" + c + "': return " + idx + ";\n" );
            }
            characterMap.put( c, idx );
            intToCharMap.put( idx, c );
            idx++;
        }
        mappingFunc.append("        default: return ANY_CHARACTER_INDEX;\n    }\n" +
                "    }\n");

        // transition map
        final String s = "private static final int[] transitionMap = new int[] { ";
        source.append( s );
        int lineLength = s.length();
        for ( int stateId = 0 ; stateId < existingStates.values().size() ; stateId++ )
        {
            final State state = existingStates.get( stateId );
            if ( state == null ) {
                throw new RuntimeException("Found no state with ID "+stateId+" ?");
            }

            final int[] tmp = new int[ alphabet.size() ];

            // prepare data
            final Map<Character,Transition> transitionMap = new HashMap<>();
            boolean gotAny = false;
            for ( Transition t : state.getOutgoingTransitions() )
            {
                if ( t.isAnyChar() )
                {
                    if ( gotAny ) {
                        throw new IllegalStateException( "State "+state+" has more than one ANY_CHARACTER transition?" );
                    }
                    tmp[ 0 ] = stateIdToArrayOffset.apply( t.destination );
                    gotAny = true;
                }
                else if ( t.isChar() )
                {
                    final char c = ((TransitionChar) t).c;
                    if ( transitionMap.put( c, t ) != null ) {
                        throw new IllegalStateException( "State "+state+" has more than one '"+c+"' character transition?" );
                    }
                } else {
                    throw new RuntimeException("Unhandled transition type: "+t);
                }
            }
            // fill in array entries
            for ( int arrayIdx = 1 ; arrayIdx < alphabet.size() ; arrayIdx++ )
            {
                final Character c = intToCharMap.get( arrayIdx );
                Transition transition = transitionMap.get( c );
                if ( transition != null ) {
                    if ( transition.destination.equals( theUnmatchedState ) ) {
                        tmp[arrayIdx] = -2; // special marker indicating that we moved past a recognized token
                    } else
                    {
                        tmp[arrayIdx] = stateIdToArrayOffset.apply( transition.destination );
                    }
                } else {
                    tmp[arrayIdx] = -1;
                }
            }

            for (int i = 0, tmpLength = tmp.length; i < tmpLength; i++)
            {
                final int value = tmp[i];
                final String tmpString = Integer.toString( value );
                source.append( tmpString );
                lineLength += tmpString.length();
                if ( (i+1) < tmpLength ) {
                    source.append(",");
                    lineLength++;
                }

                if ( lineLength > 80 )
                {
                    source.append( "\n" );
                    lineLength = 0;
                }
            }
        }
        source.append("};\n\n");

        // output mapping of terminal states to token types
        final String termStates = "private static final TokenType[] tokenTypes = new TokenType[] {    \n";
        source.append( termStates );

        int lineLen = termStates.length();
        for ( int i = 0, len= existingStates.values().size() ; i < len  ; i++ )
        {
            State terminal = terminalStates.get( i );
            final String toAppend;
            if ( terminal == null || terminal.lexerRule == null ) {
                toAppend = "null";
            } else {
                toAppend = terminal.lexerRule.tokenType;
            }
            source.append( toAppend );
            lineLen+=toAppend.length();
            if ( (i+1) < len ) {
                source.append(",");
                lineLen++;
            }
            if ( lineLen > 80 ) {
                source.append("\n    ");
            }
        }
        source.append("};\n\n");

        // append mapping function
        source.append( mappingFunc );
        return source.toString();
    }

    public StateMachine buildStateMachine(Configuration config) throws IOException {

        final Map<String, StateMachine> matchers = new HashMap<>();

        StateMachine result = null;

        for ( LexerRule rule : config.rules )
        {
            final String tokenType = rule.ruleName;
            final String regex = rule.regex;
            final StateMachine sm = new StateMachine();
            matchers.put( tokenType, sm );
            try
            {
                sm.setup( regex,false );
                sm.initialState.getTerminalStates().forEach( s -> s.lexerRule = rule );
                if ( result == null ) {
                    result = sm;
                } else {
                    result = result.union( sm );
                }
            }
            catch(Exception e) {
                throw new IllegalArgumentException("Invalid regex for rule '"+tokenType+"'");
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException
    {
        final LexerBuilder builder = new LexerBuilder();
        final String input = "# comment\n"+
                "number=[0-9]+";
        builder.build( new ByteArrayInputStream( input.getBytes() ) );
    }
}
