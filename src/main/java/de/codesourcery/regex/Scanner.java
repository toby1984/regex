package de.codesourcery.regex;

public class Scanner
{
    private final String input;
    private int offset;

    public Scanner(String input)
    {
        this.input = input;
    }


    public char peek() {
        return input.charAt(offset);
    }

    public char consume() {
        return input.charAt(offset++);
    }

    public boolean consume(char c) {
        if ( ! eof() && input.charAt(offset) == c ) {
            offset++;
            return true;
        }
        return false;
    }

    public boolean eof() {
        return offset >= input.length();
    }

    public int offset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}