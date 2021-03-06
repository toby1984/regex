/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public int size() {
        return characters.size()+ ( containsAnyChar ? 1 : 0 );
    }
}
