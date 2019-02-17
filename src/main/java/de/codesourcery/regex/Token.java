package de.codesourcery.regex;

public class Token
{
    public final String text;
    public final int offset;
    public final TokenType tokenType;

    public Token(String text, int offset, TokenType tokenType)
    {
        this.text = text;
        this.offset = offset;
        this.tokenType = tokenType;
    }

    public boolean hasType(TokenType t) {
        return t == tokenType;
    }
}
