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

import java.util.Objects;

public abstract class Transition<T extends Transition>
{
    public final String name;
    public final State destination;
    public final State origination;

    public Transition(String name,State source, State destination)
    {
        this.origination = source;
        this.destination = destination;
        this.name = name;
    }

    public abstract boolean matchesIgnoringDirection(Transition other);

    public boolean isEpsilon() {
        return false;
    }

    public boolean isChar() {
        return false;
    }

    public boolean isAnyChar() {
        return false;
    }

    @Override
    public final boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final Transition that = (Transition) o;
        return destination.equals(that.destination) && origination.equals(that.origination) && equalsHook((T) o);
    }

    protected abstract boolean equalsHook(T object);

    @Override
    public final int hashCode()
    {
        return Objects.hash(destination, origination,hashCodeHook());
    }

    protected abstract int hashCodeHook();

    public boolean isLoop() {
        return this.destination.equals( this.origination );
    }

    public boolean isOutgoing(State s)
    {
        return origination.equals( s );
    }

    public boolean isIncoming(State s)
    {
        return destination.equals( s );
    }

    public abstract Transition copy(State newSource,State newDestination);

    public final Transition copyNewSource(State source) {
        return copy(source,destination);
    }

    public final Transition copyNewDestination(State newDestination) {
        return copy(origination, newDestination );
    }

    public abstract State next(State current);

    public abstract boolean matches(Scanner scanner);

    @Override
    public String toString()
    {
        return name+"[ "+origination.getID()+" -> "+destination.getID()+" ]";
    }
}
