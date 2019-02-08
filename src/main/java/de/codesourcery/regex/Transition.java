package de.codesourcery.regex;

public abstract class Transition
{
    protected final String name;
    protected final State destination;

    public Transition(String name,State destination) {
        this.destination = destination;
        this.name = name;
    }

    public State destination() {
        return destination;
    }

    public abstract State next(State current);

    public abstract boolean matches(Scanner scanner);

    @Override
    public String toString()
    {
        return name;
    }
}
