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

public class TransitionEpsilon extends Transition<TransitionEpsilon>
{
    public TransitionEpsilon(State source,State nextState)
    {
        super( "epsilon", source, nextState);
    }

    @Override
    public Transition copy(State newSource, State newDestination)
    {
        return new TransitionEpsilon( newSource, newDestination );
    }

    @Override
    protected boolean equalsHook(TransitionEpsilon object)
    {
        return true;
    }

    @Override
    protected int hashCodeHook()
    {
        return 13;
    }

    @Override
    public boolean matchesIgnoringDirection(Transition other)
    {
        return other instanceof TransitionEpsilon;
    }

    @Override
    public State next(State current)
    {
        return destination;
    }

    @Override
    public boolean isEpsilon()
    {
        return true;
    }

    @Override
    public boolean matches(Scanner scanner)
    {
        return true;
    }
}