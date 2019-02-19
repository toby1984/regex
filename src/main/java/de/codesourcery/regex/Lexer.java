package de.codesourcery.regex;

import java.util.ArrayList;
import java.util.List;

public class Lexer
{
    private final List<Token> tokens = new ArrayList<>();

    private final IScanner scanner;

    private static final int ANY_CHARACTER_INDEX = 1;

    /**
     * Size of alphabet understood by DFA.
     * Index 0 is always the 'any char' transition (if any)
     */

    /**
     * Index of initial state when starting to loook for the next token.
     */

    /**
     * State transition map. For each state there are {@link #ALPHABET_SIZE} entries
     * in this array. If a state does not have a transition for the current input character
     * the array will contain a -1. Otherwise it will contain the offset (=state no. * ALPHABET_SIZE) of
     * the next state to go to.
     *
     * Each entry looks like this
     *
     * struct TransitionMapEntry {
     *     int tokenTypeIdx; // -1 if this is not a terminal state, otherwise the index into the tokenTypes array
     *     int anyCharOffset; // offset to the next TransitionMapEntry for 'any' character
     *     int ...; // offset to the next TransitionMapEntry for the specific symbol of the input alphabet
     * }
     */

    /**
     * Mapping from integer indices to actual token types
     * a terminal state, the token type will be NULL.
     */

    // --------------------------
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

        int currentState = INITIAL_STATE_OFFSET;
        while ( ! scanner.eof() )
        {
            final int character = mapChar( scanner.next() );
            buffer.append( character );
            int nextOffset = transitionMap[currentState + character];
            if ( nextOffset == -1 )
            {
                // character not matched, fall-back to 'any' character
                nextOffset = transitionMap[currentState + 1];
                if ( nextOffset == -1 )
                {
                    // check whether we reached a terminal state, bail if not
                    if ( transitionMap[ currentState ] != -1 ) {
                        break;
                    }
                    throw new RuntimeException( "Failed to match any token @ " + scanner.offset() );
                }
            }
            currentState = nextOffset;
            matchedTokenType = transitionMap[ currentState ];
        }
        if ( matchedTokenType == -1 ) {
            throw new RuntimeException("Lexer stopped in non-terminal state?");
        }
        final TokenType type = tokenTypes[ matchedTokenType ];
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
}