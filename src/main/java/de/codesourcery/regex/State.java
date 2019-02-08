package de.codesourcery.regex;

import java.util.ArrayList;
import java.util.List;

public final class State 
{
    private static long ID = 0;

    private final long id = ID++;
    private final List<Transition> transitions = new ArrayList<>();

    public List<Transition> transitions() {
        return this.transitions;
    }

    public State transitionAnyChar(State nextState)
    {
        transitions.add( new Transition("anyChar", nextState)
        {
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
        });
        return nextState;
    }

    /**
     * Add character transition.
     *
     * @param c
     * @param nextState
     * @return
     */
    public State transition(char c, State nextState)
    {
        transitions.add( new Transition("'"+c+"'", nextState)
        {
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
        });
        return nextState;
    }

    /**
     * Add epsilon transition.
     *
     * @param nextState
     * @return
     */
    public State transition(State nextState)
    {
        transitions.add( new Transition("epsilon", nextState)
        {
            @Override
            public State next(State current)
            {
                return destination;
            }

            @Override
            public boolean matches(Scanner scanner)
            {
                return true;
            }
        });
        return nextState;
    }

    public long getID() {
        return id;
    }
}