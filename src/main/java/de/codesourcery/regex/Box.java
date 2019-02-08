package de.codesourcery.regex;

import java.util.List;

public class Box
{
    public State entry;
    public State exit;

    public Box() {
    }

    @Override
    public String toString()
    {
        return "Box[ "+entry+" -> "+exit+" ]";
    }

    public Box(State entry, State exit)
    {
        this.entry = entry;
        this.exit = exit;
    }

    public static Box join(List<Box> boxes)
    {
        if ( boxes.isEmpty() ) {
            throw new IllegalArgumentException("Nothing to merge");
        }
        for ( int i = 0 ; (i+1) < boxes.size() ; i++ ) {
            boxes.get(i).exit.transition(boxes.get(i+1).entry );
        }
        return new Box( boxes.get(0).entry, boxes.get( boxes.size()-1 ).exit );
    }
}
