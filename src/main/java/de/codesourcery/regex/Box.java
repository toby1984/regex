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
