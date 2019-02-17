package de.codesourcery.regex;

public interface IScanner
{
    boolean eof();

    char next();

    public int offset();
}
