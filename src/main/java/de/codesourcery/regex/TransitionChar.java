package de.codesourcery.regex;

public class TransitionChar extends Transition<TransitionChar>
{
    public final char c;

    public TransitionChar(char c,State source, State nextState) {
        super("'"+c+"'",source, nextState);
        this.c = c;
    }

    @Override
    public Transition copy(State newSource, State newDestination)
    {
        return new TransitionChar( this.c, newSource, newDestination );
    }

    @Override
    protected boolean equalsHook(TransitionChar object)
    {
        return this.c == object.c;
    }

    @Override
    protected int hashCodeHook()
    {
        return Character.hashCode(c );
    }

    @Override
    public boolean matchesIgnoringDirection(Transition other)
    {
        return other instanceof TransitionChar && ((TransitionChar) other).c == this.c;
    }

    @Override
    public State next(State current)
    {
        return destination;
    }

    @Override
    public boolean matches(Scanner scanner)
    {
        return scanner.consume(c) ? true : false;
    }

    @Override
    public boolean isChar()
    {
        return true;
    }
}