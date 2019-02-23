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

import java.util.ArrayList;
import java.util.List;

public class GraphList
{
    private final List<Subgraph> list = new ArrayList<>();

    public Subgraph last() {
        return list.get( list.size() - 1 );
    }

    public void add(Subgraph subgraph) {
        this.list.add( subgraph );
    }

    public void set(Subgraph b) {
        list.clear();
        list.add( b );
    }

    public void set(int idx, Subgraph b) {
        list.set(idx,b);
    }

    public int size() {
        return list.size();
    }

    public static GraphList of(Subgraph b1) {
        final GraphList result = new GraphList();
        result.list.add(b1);
        return result;
    }

    public static GraphList of(Subgraph b1, Subgraph b2) {
        final GraphList result = new GraphList();
        result.list.add(b1);
        result.list.add(b2);
        return result;
    }

    public Subgraph join()
    {
        if ( list.size() == 1 ) {
            return list.get(0);
        }
        final Subgraph result = Subgraph.join( this.list );
        set( result );
        return result;
    }
}
