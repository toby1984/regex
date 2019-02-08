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