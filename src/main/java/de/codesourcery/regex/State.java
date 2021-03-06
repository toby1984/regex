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

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class State
{
    public static int ID = 0;

    public int id = ID++;

    public final List<Transition> allTransitions = new ArrayList<>();
    public String color;
    public boolean isAcceptingState;
    public String debugLabel;
    public LexerBuilder.LexerRule lexerRule;

    public State() {
    }

    public State(String debugLabel)
    {
        this.debugLabel = debugLabel;
    }

    public boolean isDFA()
    {
        final Set<Character> seen = new HashSet<>();
        for ( State s : gatherAllStates().values() )
        {
            if ( ! s.isDFA(seen) ) {
                return false;
            }
        }
        return true;
    }
    public boolean isDFA(Set<Character> seen)
    {
        seen.clear();
        boolean gotAny = false;
        for ( Transition t : getOutgoingTransitions() ) {
            if ( t.isEpsilon() ) {
                return false;
            }
            else if ( t.isAnyChar() )
            {
                if ( gotAny ) {
                    return false;
                }
                gotAny = true;
            }
            else
            {
                final char c = ((TransitionChar) t).c;
                if ( seen.contains( c ) ) {
                    return false;
                }
                seen.add( c );
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( o instanceof State )
        {
            return this.id == ((State) o).id;
        }
        return false;
    }

    public void resetAllColors()
    {
        gatherAllStates().values().forEach( s -> s.color = null );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( id );
    }

    public List<State> getTerminalStates() {
        return gatherAllStates().values().stream().filter( x -> x.isTerminalState() ).collect( Collectors.toList());
    }

    public State anyCharacter(State nextState)
    {
        return addTransition( new TransitionAnyChar( this, nextState ) );
    }


    public State addTransition(Transition t)
    {
        insertTransition( allTransitions.size(), t );
        return this;
    }

    public State insertTransition(int idx,Transition newTransition)
    {
        if ( ! newTransition.origination.equals( this ) &&
             ! newTransition.destination.equals( this ) )
        {
            throw new IllegalArgumentException( "Cannot add transition "+newTransition+" to "+this+" when neither the origination or destination are this node" );
        }
        if ( allTransitions.contains( newTransition ) )
        {
            throw new IllegalArgumentException("State "+this+" - duplicate transition "+newTransition);
        }
        if ( newTransition.isOutgoing(this ) && ! newTransition.isEpsilon() )
        {
            if ( allTransitions.stream().filter( x-> x.isOutgoing(this) ).anyMatch(x -> x.matchesIgnoringDirection(newTransition )))
            {
                throw new IllegalArgumentException("State "+this+" - same outgoing transition "+newTransition);
            }
        }
        allTransitions.add(idx,newTransition );
        if ( ! newTransition.isLoop() )
        {
            if (!newTransition.isEpsilon())
            {
                if (newTransition.destination.allTransitions.stream().filter(x -> x.isIncoming(this)).anyMatch(x -> x.matchesIgnoringDirection(newTransition)))
                {
                    throw new IllegalArgumentException("State " + newTransition.destination + " - same outgoing transition " + newTransition);
                }
            }
            if (newTransition.destination.allTransitions.contains(newTransition))
            {
                throw new IllegalArgumentException("State " + newTransition.destination + " - duplicate transition " + newTransition);
            }
            newTransition.destination.allTransitions.add(newTransition);
        }
        return this;
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
        return addTransition( new TransitionChar( c, this, nextState ) );
    }

    /**
     * Add epsilon transition.
     *
     * @param nextState
     * @return
     */
    public State transition(State nextState)
    {
        return addTransition( new TransitionEpsilon( this, nextState ) );
    }

    public Subgraph copyGraph() {
        return copyGraph( true );
    }

    public Subgraph copyGraph(boolean unifyExits)
    {
        final Set<State> exits = new HashSet<>();
        final Map<Integer, State> seen = new HashMap<>();
        final State entry = copyGraph( this, seen, exits );
        if ( exits.size() > 1 && unifyExits )
        {
            final State uniqueExit = new State();
            exits.forEach( t -> t.transition( uniqueExit ) );
            return new Subgraph( entry, uniqueExit );
        }
        else if ( exits.size() == 1 )
        {
            return new Subgraph( entry, exits.iterator().next() );
        }
        return new Subgraph(entry,entry);
    }

    public Alphabet getAlphabet()
    {
        final Alphabet alphabet = new Alphabet();
        visitOutgoingTransitions( transition -> {
            if ( transition.isAnyChar() ) {
                alphabet.addAnyChar();
            } else if ( transition.isChar() ) {
                alphabet.add( ((TransitionChar) transition).c );
            }
        });
        return alphabet;
    }

    private State copyGraph(State currentNode, Map<Integer, State> copies, Set<State> exits)
    {
        State newSource = copies.get( currentNode.getID() );
        if ( newSource == null )
        {
            newSource = new State();
            newSource.debugLabel = currentNode.debugLabel;
            newSource.color = currentNode.color;
            newSource.isAcceptingState = currentNode.isAcceptingState;
            newSource.lexerRule = currentNode.lexerRule;

            copies.put( currentNode.getID(), newSource );

            for (Transition t : currentNode.getOutgoingTransitions() )
            {
                final State destination = copyGraph( t.destination, copies, exits );
                newSource.addTransition( t.copy( newSource, destination ) );
            }
            if ( newSource.isTerminalState() )
            {
                exits.add( newSource );
            }
        }
        return newSource;
    }

    public void visitOutgoingTransitions(Consumer<Transition> visitor)
    {
        visitOutgoingTransitions( visitor, new HashSet<>() );
    }

    private void visitOutgoingTransitions(Consumer<Transition> visitor, Set<State> visited)
    {
        if ( visited.contains(  this ) ) {
            return;
        }
        visited.add( this );
        for ( Transition t : getOutgoingTransitions() )
        {
            if ( t.isOutgoing( this ) )
            {
                visitor.accept( t );
                t.destination.visitOutgoingTransitions( visitor, visited );
            }
        }
    }

    public String getDebugInfo() {

        final Map<Integer, State> allStates = gatherAllStates();
        int transitionCount = 0;
        for ( State s : allStates.values() ) {
            transitionCount += s.getOutgoingTransitions().size();
        }
        return allStates.size()+" states, "+transitionCount+" transitions";
    }

    public void visitOutgoingStates(Consumer<State> visitor)
    {
        visitOutgoingStates(visitor,new HashSet<>());
    }

    private void visitOutgoingStates(Consumer<State> visitor, Set<State> visited)
    {
        if ( visited.contains(  this ) ) {
            return;
        }
        visited.add( this );
        visitor.accept(this);
        for ( Transition t : getOutgoingTransitions() )
        {
            if ( t.isOutgoing( this ) )
            {
                t.destination.visitOutgoingStates( visitor, visited );
            }
        }
    }

    public boolean matches(Scanner scanner)
    {
        final int offset = scanner.offset();

        final List<Transition> transitions = getOutgoingTransitions();
        if ( transitions.isEmpty() )
        {
            return true; // terminal state reached
        }
        for (int i = 0, len = transitions.size(); i < len; i++)
        {
            final Transition transition = transitions.get( i );
            if ( transition.matches( scanner ) &&
                    transition.next( this ).matches( scanner ) &&
                    scanner.eof() )
            {
                return true;
            }
            scanner.setOffset( offset );
        }
        return false;
    }

    public List<Transition> getOutgoingTransitions()
    {
        return allTransitions.stream().filter( x -> x.isOutgoing( this ) ).collect( Collectors.toList() );
    }

    public List<Transition> getIncomingTransitions()
    {
        return allTransitions.stream().filter( x -> x.isIncoming( this ) ).collect( Collectors.toList() );
    }

    public int outgoingTransitionCount()
    {
        return (int) allTransitions.stream().filter( x -> x.isOutgoing( this ) ).count();
    }

    public int incomingTransitionCount()
    {
        return (int) allTransitions.stream().filter( x -> x.isIncoming( this ) ).count();
    }

    public Transition firstOutgoingTransition() {
        return allTransitions.stream().filter( x -> x.isOutgoing( this ) ).findFirst().get();
    }

    public boolean isTerminalState()
    {
        if ( isAcceptingState )
        {
            return true;
        }
        if ( outgoingTransitionCount() == 0 )
        {
            return true;
        }
        return getOutgoingTransitions().stream().allMatch( t -> t.destination.equals( this ) );
    }

    public int removeTransition(Transition t)
    {
        int idx = -1;
        int i = 0;
        for (Iterator<Transition> iterator = allTransitions.iterator(); iterator.hasNext(); i++ )
        {
            final Transition current = iterator.next();
            if ( t.equals(  current ) )
            {
                if ( idx != -1 ) {
                    throw new IllegalStateException( "Already removed once ? "+t );
                }
                if ( this.equals( t.origination ) )
                {
                    if ( ! t.destination.allTransitions.remove(t) ) {
                        throw new IllegalStateException("Failed to remove "+t+" from "+t.destination);
                    }
                }
                else
                {
                    if ( ! t.origination.allTransitions.remove(t) )
                    {
                        throw new IllegalStateException("Failed to remove " + t + " from " + t.destination);
                    }
                }
                idx = i;
                iterator.remove();
            }
        }
        if ( idx == -1 ) {
            throw new IllegalStateException("Failed to remove transition");
        }
        return idx;
    }

    public void postprocess()
    {
        gatherAllStates().values().forEach( State::collapseIntermediateStates );
    }

    public Map<Integer, State> gatherAllStates() {
        final Map<Integer,State> allStates = new HashMap<>();
        visitOutgoingStates( state -> allStates.put( state.getID(), state ) );
        return allStates;
    }

    private void collapseIntermediateStates()
    {
        final List<Transition> outgoing = getOutgoingTransitions();
        for (int idx = 0, len = outgoing.size() ; idx < len ; idx++)
        {
            final Transition transition = outgoing.get( idx );
            if ( ! transition.destination.equals(this) && transition.destination.outgoingTransitionCount() == 1  )
            {
                final Transition nextHop = transition.destination.firstOutgoingTransition();
                if ( nextHop.isEpsilon() )
                {
                    // [state1] -> any -> [state2] -> epsilon -> [state3]
                    if ( transition.destination.outgoingTransitionCount() == 1 )
                    {
                        final int removedIdx = removeTransition(transition);
                        final Transition newTransition = transition.copyNewDestination(nextHop.destination);
                        insertTransition(removedIdx, newTransition);
                    }
                }
                else if ( transition.isEpsilon() )
                {
                    // [state1] -> epsilon -> [state2] -> any -> [state3]
                    System.out.println("Removing transition "+nextHop);

                    if ( getOutgoingTransitions().stream().noneMatch(t -> t.matchesIgnoringDirection(nextHop) ) )
                    {
                        final int removedIdx = removeTransition(transition);
                        final Transition copy = nextHop.copy(this, nextHop.destination);
                        insertTransition(removedIdx, copy);
                    }
                }
            }
        }
    }

    public int getID() {
        return id;
    }

    public List<Transition> getAllTransitions()
    {
        return allTransitions;
    }

    @Override
    public String toString()
    {
        if ( outgoingTransitionCount() == 1 ) {
            return getID() +" [ "+getOutgoingTransitions().get(0).name+" ]";
        }
        return Long.toString( getID() );
    }

    public String toDOT(Dimension size)
    {
        return toDOT(size, false);
    }

    public String toDOT(Dimension size, boolean alsoRenderIncoming)
    {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("digraph {\n");
        if ( size != null )
        {
            final float w = (float) (size.getWidth()/100f);
            final float h = (float) (size.getHeight()/100f);
            buffer.append("graph[ dpi=100,size=\""+w+","+h+"!\"];\n");
        }
        buffer.append("forcelabels=true;\n");
        buffer.append("ratio=0.5;rankdir=LR;\n");

        final Map<Integer, State> allStates = gatherAllStates();
        List<String> attributes = new ArrayList<>();
        for ( State s : allStates.values() )
        {
            attributes.clear();
            String label = s.debugLabel;
            if ( s.isTerminalState() )
            {
                if ( label == null )
                {
                    label = "END";
                } else {
                    label += " (END)";
                }
                if ( s.lexerRule != null ) {
                    label += "["+s.lexerRule.ruleName+"]";
                }
            }
            if ( label != null )
            {
                attributes.add("label=\""+dotEscape(label)+"\"");
            }
            if ( s.color != null ) {
                attributes.add("color="+s.color);
            }
            if ( ! attributes.isEmpty() )
            {
                buffer.append( getDOTId( s ) + " ["+attributes.stream().collect( Collectors.joining( "," ) )+"];\n" );
            }
        }

        final List<State> list = new ArrayList<>(allStates.values());
        list.sort( Comparator.comparingInt( State::incomingTransitionCount ) );
        for ( State s : list)
        {
            for ( Transition t : s.getAllTransitions() )
            {
                final boolean outgoing = t.isOutgoing( s );
                final String color = outgoing ? "black" : "gray";
                final String label = t.name;
                if ( outgoing )
                {
                    buffer.append( getDOTId( t.origination ) + " -> " + getDOTId( t.destination ) + " [color=" + color + ",label=\""+dotEscape(label)+"\"]\n" );
                } else if (alsoRenderIncoming) {
                    buffer.append( getDOTId( t.destination ) + " -> " + getDOTId( t.origination ) + " [color=" + color + ",label=\""+dotEscape(label)+"\"]\n" );
                }
            }
        }
        buffer.append("}");
        return buffer.toString();
    }

    private String dotEscape(String input) {
        return input == null ? null : input.replace( "\"", "\\\"" );
    }

    private static String getDOTId(State s) {
        return Long.toString( s.getID() );
    }
}