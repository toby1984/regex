package de.codesourcery.regex;

public interface IScanner
{
    boolean eof();

    char next();

    void goBack();

    int offset();
}
