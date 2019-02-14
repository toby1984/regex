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

public class TransitionAnyChar extends Transition<TransitionAnyChar>
{
    public TransitionAnyChar(State source, State nextState)
    {
        super( "anyChar", source, nextState );
    }

    @Override
    public Transition copy(State newSource, State newDestination)
    {
        return new TransitionAnyChar( newSource, newDestination );
    }

    @Override
    protected boolean equalsHook(TransitionAnyChar object)
    {
        return true;
    }

    @Override
    protected int hashCodeHook()
    {
        return 42;
    }

    @Override
    public boolean matchesIgnoringDirection(Transition other)
    {
        return other instanceof TransitionAnyChar;
    }

    @Override
    public State next(State current)
    {
        return destination;
    }

    @Override
    public boolean matches(Scanner scanner)
    {
        if ( scanner.eof() )
        {
            return false;
        }
        scanner.consume();
        return true;
    }

    @Override
    public boolean isAnyChar()
    {
        return true;
    }
}