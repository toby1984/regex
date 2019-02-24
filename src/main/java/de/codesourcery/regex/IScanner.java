package de.codesourcery.regex;

public interface IScanner
{
    boolean eof();

    char next();

    char peek();

    void goBack();

    void setOffset(int offset);

    int offset();
}
