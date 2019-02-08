package de.codesourcery.regex;

import java.util.HashSet;
import java.util.Set;

public class Alphabet
{
    public boolean containsAnyChar;

    public interface IAlphabetVisitor
    {
        void visitAny();

        void visitChar( char c );
    }

    public final Set<Character> characters = new HashSet<>();

    public void add(char c) {
        characters.add(c);
    }

    public void addAnyChar() {
        containsAnyChar = true;
    }

    public void visit(IAlphabetVisitor v) {
        if ( containsAnyChar ) {
            v.visitAny();
        }
        characters.forEach( v::visitChar );
    }
}
