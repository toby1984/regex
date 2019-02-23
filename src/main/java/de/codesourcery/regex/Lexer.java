package de.codesourcery.regex;

import java.util.ArrayList;
import java.util.List;

public class Lexer
{
    private static final boolean DEBUG = true;

    private final List<Token> tokens = new ArrayList<>();

    private final IScanner scanner;

    private static final int ANY_CHARACTER_INDEX = 0;

    private final StringBuilder buffer = new StringBuilder();

    public Lexer(IScanner scanner) {
        this.scanner = scanner;
    }

    public boolean eof() {
        if ( tokens.isEmpty() ) {
            parse();
        }
        return tokens.get(0).hasType( TokenType.EOF );
    }

    public Token next() {
        if ( tokens.isEmpty() ) {
            parse();
        }
        return tokens.remove(0);
    }

    private void parse()
    {
        if ( scanner.eof() ) {
            tokens.add( new Token("",scanner.offset(),TokenType.EOF));
            return;
        }

        buffer.setLength( 0 );

        int matchedTokenType = -1;
        final int startOffset = scanner.offset();
        int previousState = -1;
        int currentState = INITIAL_STATE_OFFSET;
        while ( ! scanner.eof() )
        {
            if ( DEBUG )
            {
                System.out.println( "current_state = " + currentState + " / matchedTokenType = " + matchedTokenType );
            }
            final char c = scanner.next();
            final int character = mapChar( c );
            if ( DEBUG )
            {
                System.out.println( "'" + c + "' maps to " + character );
            }
            int nextState = transitionMap[currentState + character];
            if ( nextState == -2 )
            {
                scanner.goBack();
                if ( DEBUG )
                {
                    System.out.println( "Reached terminal state" );
                }
                break;
            }
            else if ( nextState == -1 )
            {
                // character not matched, try to match 'any' character
                if ( DEBUG )
                {
                    System.out.println( "Match failed, trying 'any' character match." );
                }
                nextState = transitionMap[ currentState ];
                if ( nextState < 0 )
                {
                    scanner.goBack();
                    if ( DEBUG )
                    {
                        System.out.println( "'any' character match FAILED, giving up." );
                    }
                    break; // failed to match
                } else
                {
                    if ( DEBUG )
                    {
                        System.out.println( "'any' character match succeeded" );
                    }
                }
            } else {
                if ( DEBUG )
                {
                    System.out.println("New state: "+nextState);
                }
            }
            buffer.append(c);
            currentState = nextState;
            if ( previousState != currentState )
            {
                // state transition
                if ( DEBUG )
                {
                    System.out.println( "State transition: " + previousState + " -> " + currentState );
                }
                matchedTokenType = currentState;
            }
            previousState = currentState;
        }
        final TokenType type;
        if ( matchedTokenType < 0 )
        {
            type = TokenType.TEXT;
        }
        else
        {
            type = tokenTypes[matchedTokenType / ALPHABET_SIZE];
        }
        tokens.add( new Token( buffer.toString(), startOffset, type ) );
    }

    private static final int ALPHABET_SIZE = 36;

    private static final int INITIAL_STATE_OFFSET = 72; // TODO: Generated code

    private static final int[] transitionMap = new int[] { 0,0,0,0,0,0,0,0,0,0,0,-2,-2,
    -2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-20,-2,-2,-2,-2,
    -2,-2,-2,-2,-2,-2,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,
    36,36,36,360,144,144,144,144,144,144,144,144,144,144,108,108,108,108,108,108,108,
    108,108,108,108,108,108,108,108,108,108,108,108,108,108,108,108,108,1080,-2,-2,-2,
    -2,-2,-2,-2,-2,-2,-2,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,36,
    36,36,36,36,360,0,0,0,0,0,0,0,0,0,0,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,
    -2,-2,-2,-2,-2,-2,-2,-2,-2,-20,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,
    -2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2,-2};

    private static final TokenType[] tokenTypes = new TokenType[] {
    TokenType.NUMBER,
    TokenType.IDENTIFIER,
    null,
    TokenType.IDENTIFIER,
    TokenType.NUMBER,
    null
    };

    private int mapChar(char c)
    {
        switch( c ) {
            case '0': return 1;
            case '1': return 2;
            case '2': return 3;
            case '3': return 4;
            case '4': return 5;
            case '5': return 6;
            case '6': return 7;
            case '7': return 8;
            case '8': return 9;
            case '9': return 10;
            case 'a': return 11;
            case 'b': return 12;
            case 'c': return 13;
            case 'd': return 14;
            case 'e': return 15;
            case 'f': return 16;
            case 'g': return 17;
            case 'h': return 18;
            case 'i': return 19;
            case 'j': return 20;
            case 'k': return 21;
            case 'l': return 22;
            case 'm': return 23;
            case 'n': return 24;
            case 'o': return 25;
            case 'p': return 26;
            case 'q': return 27;
            case 'r': return 28;
            case 's': return 29;
            case 't': return 30;
            case 'u': return 31;
            case 'v': return 32;
            case 'w': return 33;
            case 'x': return 34;
            case 'y': return 35;
            case 'z': return 36;
            default: return ANY_CHARACTER_INDEX;
        }
    }

    public static void main(String[] args)
    {
        String input = "???";

        final Lexer lexer = new Lexer( new Scanner( input ) );
        while ( ! lexer.eof() ) {
            System.out.println("Got: "+lexer.next());
        }
    }
}