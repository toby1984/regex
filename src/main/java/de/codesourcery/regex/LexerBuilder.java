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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LexerBuilder
{
    public StateMachine stateMachine;

    public String build(InputStream configFile) throws IOException
    {
        stateMachine = buildStateMachine( configFile );

        if ( ! stateMachine.isDFA() )
        {
            stateMachine.toDFA( state -> {} );
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
                    tmp[arrayIdx] = stateIdToArrayOffset.apply( transition.destination );
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
        final Map<Integer,State> terminalStates = new HashMap<>();
        for ( State state : existingStates.values() )
        {
            if ( state.isTerminalState() )
            {
                if ( state.tokenType == null || state.tokenType.isBlank() ) {
                    throw new IllegalStateException( "Terminal state "+state+" has no token type assigned ?" );
                }
                if ( terminalStates.put( state.getID(), state ) != null ) {
                    throw new IllegalStateException( "State with ID "+state.getID()+" already seen?");
                }
            }
        }
        int lineLen = termStates.length();
        for ( int i = 0, len= existingStates.values().size() ; i < len  ; i++ )
        {
            State terminal = terminalStates.get( i );
            final String toAppend;
            if ( terminal == null ) {
                toAppend = "null";
            } else {
                toAppend = terminal.tokenType;
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

    public StateMachine buildStateMachine(InputStream configFile) throws IOException {

        final Map<String,StateMachine> matchers = new HashMap<>();

        StateMachine result = null;

        try ( BufferedReader reader = new BufferedReader( new InputStreamReader(configFile) ) ) {
            String line;
            int lineNo = 1;
outer:
            for ( ; ( line = reader.readLine() ) != null ; lineNo++ )
            {
                line = line.trim();
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
                if ( matchers.containsKey( tokenType ) ) {
                    throw new IllegalArgumentException("Duplicate token type '"+tokenType+"' on line "+lineNo);
                }
                final String regex = line.substring(idx+1);
                if ( regex.isBlank() ) {
                    throw new IllegalArgumentException( "Blank regex on line "+lineNo );
                }
                final StateMachine sm = new StateMachine();
                matchers.put( tokenType, sm );
                try
                {
                    sm.setup( regex );
                    sm.initialState.getTerminalStates().forEach( s -> s.tokenType = tokenType );
                    if ( result == null ) {
                        result = sm;
                    } else {
                        result = result.union( sm );
                    }
                }
                catch(Exception e) {
                    throw new IllegalArgumentException("Invalid regex for token '"+tokenType+"' on line "+lineNo+": "+e.getMessage(),e);
                }
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
