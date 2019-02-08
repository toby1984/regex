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