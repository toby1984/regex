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
    private static final int ALPHABET_SIZE = 26; // TODO: Generated code

    /**
     * Index of initial state when starting to loook for the next token.
     */
    private static final int INITIAL_STATE_OFFSET = -1; // TODO: Generated code

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
    private static final int[] transitionMap = {}; // TODO: Generated code

    /**
     * Mapping from integer indices to actual token types
     * a terminal state, the token type will be NULL.
     */
    private static final TokenType[] tokenTypes =  {}; // TODO: Generated code

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

    /**
     * Responsible for mapping characters to alphabet symbol indices.
     *
     * @param c
     * @return
     */
    private int mapChar(char c)
    {
        switch( c )
        {
            // TODO: Generated code
            default:
                return ANY_CHARACTER_INDEX;
        }
    }
}
