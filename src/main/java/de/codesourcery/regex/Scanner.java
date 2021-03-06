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

public class Scanner implements IScanner
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

    @Override
    public boolean eof() {
        return offset >= input.length();
    }

    @Override
    public char next()
    {
        return consume();
    }

    @Override
    public void goBack()
    {
        if ( offset == 0 ) {
            throw new IllegalStateException( "Already at beginning of input" );
        }
        offset--;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }
}