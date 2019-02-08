package de.codesourcery.regex;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

public class StateMachine
{
    private State initialState;

    public void setup(String regex)
    {
        final char firstChar = regex.charAt(0);
        switch(firstChar)
        {
            case '*':
            case '+':
            case '?':
                throw new IllegalArgumentException( "'"+firstChar+"' quantifier is not allowed at start of expression" );
        }

        State first = new State();
        State currentState = first;

        boolean quoted = false;
        for ( int i = 0 , len = regex.length() ; i < len ; i++ )
        {
            final char currentChar = regex.charAt(i);
            if ( ! quoted && currentChar == '\\' )
            {
                if ( (i+1) == len ) {
                    throw new IllegalArgumentException("Cannot use '\\' at the end of the pattern");
                }
                quoted = true;
                continue;
            }

            if ( ! quoted && currentChar == '|') {
                currentState = first;
                continue;
            }

            final char nextChar = (i+1) < len ? regex.charAt(i+1) : 'x';
            State newState;
            switch(nextChar)
            {
                case '+': // 1...n
                    final State repeat = new State();
                    if ( quoted || currentChar != '.' )
                    {
                        currentState.transition( currentChar, repeat );
                        repeat.transition( currentChar, repeat );
                    } else {
                        currentState.transitionAnyChar( repeat );
                        repeat.transitionAnyChar( repeat );
                    }
                    currentState = repeat.transition( new State() );
                    i++;
                    break;
                case '*': // 0...n
                    newState = new State();
                    if ( quoted || currentChar != '.' )
                    {
                        currentState.transition( currentChar, currentState );
                        currentState = currentState.transition( newState );
                    } else {
                        currentState.transitionAnyChar( currentState );
                        currentState = currentState.transition( newState );
                    }
                    i++;
                    break;
                case '?': // 0..1
                    newState = new State();
                    if ( quoted || currentChar != '.' )
                    {
                        currentState.transition( currentChar, newState );
                    } else {
                        currentState.transitionAnyChar( newState );
                    }
                    currentState = currentState.transition( newState );
                    i++;
                    break;
                default:
                    if ( quoted || currentChar != '.')
                    {
                        if ( ! quoted && isSpecialCharacter( currentChar ) ) {
                            throw new IllegalArgumentException("Cannot use '"+currentChar+"' as first character of expression");
                        }
                        currentState = currentState.transition( currentChar, new State() );
                    } else {
                        currentState = currentState.transitionAnyChar( new State() );
                    }
            }
            quoted = false;
        }
        initialState = first;
    }

    private static boolean isSpecialCharacter(char c) {
        switch(c) {
            case '+':
            case '*':
            case '|':
            case '?':
                return true;
            default:
                return false;
        }
    }

    public boolean matches(String input)
    {
        return matches(new Scanner(input),initialState);
    }

    private boolean matches(Scanner scanner, State current)
    {
        final int offset = scanner.offset();

        final List<Transition> transitions = current.transitions();
        if ( transitions.isEmpty() ) {
            return true; // terminal state reached
        }
        for (int i = 0, len = transitions.size() ; i < len; i++)
        {
            final Transition transition = transitions.get( i );
            if ( transition.matches( scanner ) && matches( scanner, transition.next( current ) ) )
            {
                return true;
            }
            scanner.setOffset( offset );
        }
        return false;
    }

    public String toDOT()
    {
        final Map<Long,State> allStates = new HashMap<>();
        final Stack<State> stack = new Stack<>();
        stack.push( initialState );
        allStates.put( initialState.getID(), initialState );
        while ( ! stack.isEmpty() )
        {
            final State state = stack.pop();
            state.transitions().forEach( t ->
            {
                if ( ! allStates.containsKey( t.destination().getID() ) )
                {
                    allStates.put( t.destination.getID() , t.destination() );
                    stack.push( t.destination() );
                }
            });
        }
        final StringBuilder buffer = new StringBuilder();
        buffer.append("digraph {\n");
        for ( State s : allStates.values() )
        {
            s.transitions().forEach(  t ->
            {
                buffer.append( s.getID()+" -> "+t.destination().getID() + "[label=\""+t.toString()+"\"];\n" );
            });
        }
        buffer.append("}");
        return buffer.toString();
    }

    public static void main(String[] args) throws IOException
    {
        final String regex = "a+|b|c";
        final String input = "a";

        final StateMachine matcher = new StateMachine();
        final Pattern pattern = Pattern.compile( regex );
        matcher.setup( regex );

        try ( PrintWriter w = new PrintWriter( new FileWriter("/home/tobi/tmp/test.dot") ) ) {
            w.write( matcher.toDOT() );
        }

        boolean expected = pattern.matcher( input ).matches();
        boolean actual = matcher.matches( input );
        System.out.println("Matching: "+input);
        System.out.println("Expected: "+expected);
        System.out.println("Actual  : "+actual);
    }
}