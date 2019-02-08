package de.codesourcery.regex;

import java.util.ArrayList;
import java.util.List;

public class Boxes
{
    private final List<Box> list = new ArrayList<>();

    public Box last() {
        return list.get( list.size() - 1 );
    }

    public void add(Box box) {
        this.list.add( box );
    }

    public void set(Box b) {
        list.clear();
        list.add( b );
    }

    public void set(int idx,Box b) {
        list.set(idx,b);
    }

    public int size() {
        return list.size();
    }

    public static Boxes of(Box b1) {
        final Boxes result = new Boxes();
        result.list.add(b1);
        return result;
    }

    public static Boxes of(Box b1,Box b2) {
        final Boxes result = new Boxes();
        result.list.add(b1);
        result.list.add(b2);
        return result;
    }

    public Box join()
    {
        if ( list.size() == 1 ) {
            return list.get(0);
        }
        final Box result = Box.join( this.list );
        set( result );
        return result;
    }
}
