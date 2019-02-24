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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StateMachine
{
    public State initialState;
    private String expression;
    private boolean caseInsensitive;

    public void setup(String regex,boolean caseInsensitive)
    {
        this.expression = regex;
        this.caseInsensitive = caseInsensitive;
        setup(new Scanner(regex));
    }

    private void setup(Scanner scanner)
    {
        final GraphList graphList = new GraphList();
        while ( ! scanner.eof() ) {
            parse(scanner, graphList );
        }
        final Subgraph first = graphList.join();
        initialState = first.entry;
    }

    private void parse(Scanner scanner, GraphList graphList)
    {
        while ( ! scanner.eof() )
        {
            char currentChar = scanner.consume();
            if (currentChar == '\\')
            {
                currentChar = scanner.consume();
                addCharTransition( scanner, graphList, currentChar, true );
                continue;
            }

            if ( currentChar == '[' ) { // character class
                graphList.add( parseCharacterClass( scanner ) );
                return;
            }

            if (currentChar == '|') // union
            {
                final GraphList expr = new GraphList();
                parse(scanner, expr);
                Subgraph union1 = graphList.join();
                Subgraph union2 = expr.join();

                final Subgraph result = new Subgraph(new State(), new State());
                result.entry.transition(union1.entry);
                result.entry.transition(union2.entry);

                union1.exit.transition(result.exit );
                union2.exit.transition(result.exit );

                graphList.set(result);
                return;
            }

            if (currentChar == '(')
            {
                final GraphList list = new GraphList();
                parse(scanner, list);
                final Subgraph joined = list.join();
                final boolean createNewBox = graphList.size() == 0 || (!scanner.eof() && isPostfixOperator( scanner.peek() ) );
                if ( createNewBox )
                {
                    graphList.add( joined );
                }
                else
                {
                    final Subgraph existing = graphList.join();
                    existing.exit.transition( joined.entry );
                    graphList.set( new Subgraph( existing.entry, joined.exit ) );
                }
                return;
            }

            if (currentChar == ')')
            {
                // calling code relies on only one box being present
                graphList.join();
                return;
            }

            switch (currentChar)
            {
                case '+': // 1...n
                    Subgraph last = graphList.last();
                    Subgraph repeat = last.entry.copyGraph();
                    repeat.exit.transition( repeat.entry );
                    final State newExit = new State();
                    repeat.entry.transition( newExit );

                    final Subgraph merged = GraphList.of( last, repeat ).join();
                    merged.exit = newExit;
                    graphList.set( graphList.size() - 1, merged );
                    return;
                case '*': // 0...n

                    final Subgraph newSubgraph = graphList.join();
                    newSubgraph.exit.transition( newSubgraph.entry );

                    final State newStart = new State();
                    newStart.transition( newSubgraph.entry );

                    final State newEnd = new State();
                    newSubgraph.exit.transition( newEnd );

                    newStart.transition( newEnd );
                    graphList.set( new Subgraph( newStart, newEnd ) );
                    return;
                case '?': // 0..1
                    last = graphList.last();
                    last.entry.transition(last.exit);
                    return;
                default:
                    addCharTransition( scanner, graphList, currentChar, false );
            }
        }
    }

    private void addCharTransition(Scanner scanner, GraphList graphList, char currentChar,boolean quoted)
    {
        final boolean createNewBox = graphList.size() == 0 || (!scanner.eof() && isPostfixOperator( scanner.peek() ) );
        final State entry = createNewBox ? new State() : graphList.last().exit;
        final State exit = new State();
        if (currentChar == '.' && ! quoted )
        {
            entry.anyCharacter(exit);
        }
        else
        {
            if ( caseInsensitive )
            {
                final char lower = Character.toLowerCase( currentChar );
                final char upper= Character.toUpperCase( currentChar );
                if ( lower != upper )
                {
                    entry.transition( lower, exit );
                    entry.transition( upper, exit );
                } else {
                    entry.transition( currentChar, exit );
                }
            } else {
                entry.transition( currentChar, exit );
            }
        }
        if ( createNewBox )
        {
            graphList.add( new Subgraph( entry, exit ) );
        } else {
            graphList.last().exit = exit;
        }
    }

    private Subgraph parseCharacterClass(Scanner scanner) {

        final State start = new State();
        final State end = new State();
        final Subgraph subgraph = new Subgraph( start, end );
        boolean quoted = false;
        while ( ! quoted )
        {
            final char c = scanner.consume();
            if ( ! quoted && c == ']' ) {
                break;
            }
            if ( c == '\\' ) {
                quoted = true;
                continue;
            }
            if ( scanner.peek() == '-' ) {
                // range
                scanner.consume(); // consume '-'
                char c2 = scanner.consume();
                for ( char current = c ; current <= c2 ; current++ ) {
                    start.transition( current, end );
                }
            } else {
                start.transition( c, end );
            }
            quoted = false;
        }
        if ( subgraph.entry.getAllTransitions().isEmpty() ) {
            throw new IllegalStateException("Empty character class at offset "+scanner.offset());
        }
        return subgraph;
    }

    private static boolean isPostfixOperator(char c) {
        switch(c)
        {
            case '*':
            case '?':
            case '+':
                return true;
            default:
                return false;
        }
    }

    public void simplify() {
        initialState.postprocess();
    }

    public boolean matches(String input)
    {
        return initialState.matches(new Scanner(input) );
    }

    public StateMachine union(StateMachine other) {

        // unify starts
        final State newStart = new State();
        newStart.transition( this.initialState );
        newStart.transition( other.initialState );

        final StateMachine result = new StateMachine();
        result.initialState = newStart;
        return result;
    }

    private void highlight(Set<State> toHighlight, String color, Consumer<State> debugImage)
    {
        initialState.resetAllColors();
        highlightNoReset( toHighlight, color, debugImage );
    }

    private void highlightNoReset(Set<State> toHighlight, String color, Consumer<State> debugImage)
    {
        toHighlight.forEach( s -> s.color = color );
        debugImage.accept( initialState );
    }

    public void toDFA(Consumer<State> debugImage, Function<Set<LexerBuilder.LexerRule>, LexerBuilder.LexerRule> ambiguityResolver)
    {
        final String stateNames = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        /*
        Operation        Description
        ε-closure(s)     Set of NFA states reachable from NFA state s on ε-transitions alone
        ε-closure(T)     Set of NFA states reachable from set of states T on ε-transitions alone
        move(T,a)        Set of states to which there is a transition on input symbol a from some NFA state in T
        */

        // calculate size of language
        final Alphabet alphabet = initialState.getAlphabet();

        // Begin with the Start X state and find ε-closure(X)
        final State first = new State();

        final Map<State,Set<State>> newGraph = new HashMap<>();
        final Consumer<State> assignName = (state) -> {
            final int idx = newGraph.size();
            final String prefix = Character.toString( stateNames.charAt( idx % stateNames.length() ) );
            int count = 0;
            while ( true )
            {
                final String newName = count == 0 ? prefix : prefix+count;
                if ( newGraph.keySet().stream().noneMatch( x -> newName.equals(x.debugLabel) ) ) {
                    break;
                }
                count++;
            }
            state.debugLabel = count == 0 ? prefix : prefix+count;
            System.out.println("State "+state.getID()+" becomes "+state.debugLabel);
        };
        assignName.accept(  first );

        final Set<State> a = epsilonClosure( initialState );
        newGraph.put( first , a );

        final Stack<State> toProcess = new Stack<>();
        toProcess.push( first );

        while ( ! toProcess.isEmpty() )
        {
            final State current = toProcess.pop();
            System.out.println("Now processing "+current+" "+newGraph.get(current)+" , stack size: "+toProcess.size());

            highlight( a , "red" , debugImage );

            alphabet.visit( new Alphabet.IAlphabetVisitor()
            {
                final Set<State> moveSet = new HashSet<>();

                @Override
                public void visitAny()
                {
                    process( new Predicate<>()
                    {
                        @Override
                        public boolean test(Transition t)
                        {
                            return t.isAnyChar();
                        }

                        @Override
                        public String toString()
                        {
                            return "'anyChar'";
                        }
                    }, (src, dst) -> src.transition( dst ) );
                }

                @Override
                public void visitChar(char c)
                {
                    process( new Predicate<>()
                    {
                        @Override
                        public boolean test(Transition t)
                        {
                            return t.isChar() && ((TransitionChar) t).c == c;
                        }

                        @Override
                        public String toString()
                        {
                            return "'" + c + "'";
                        }
                    }, (src, dst) -> src.transition( c , dst ) );
                }

                private void process(Predicate<Transition> transitionCheck, BiConsumer<State,State> transitionCreator)
                {
                    moveSet.clear();
                    for (State state : newGraph.get( current ) )
                    {
                        for ( Transition t : state.getOutgoingTransitions() )
                        {
                            if ( transitionCheck.test(t) )
                            {
                                moveSet.add( t.destination );
                            }
                        }
                    }
                    System.out.println("Move set for "+transitionCheck+" : "+moveSet);

                    highlightNoReset( moveSet , "green" , debugImage );

                    final Set<State> epsilonClosure = epsilonClosure(moveSet);

                    highlightNoReset( epsilonClosure , "blue" , debugImage );

                    System.out.println("Epsilon closure: "+epsilonClosure);
                    for (var entry : newGraph.entrySet() )
                    {
                        var existing = entry.getValue();
                        if ( existing.equals( epsilonClosure ) )
                        {
                            System.out.println("State "+current+" loops to already existing state "+entry.getKey());
                            transitionCreator.accept( current, entry.getKey() );
                            return;
                        }
                    }

                    final Set<LexerBuilder.LexerRule> matchingRules =
                            epsilonClosure.stream()
                                    .filter( State::isTerminalState )
                                    .map( x -> x.lexerRule )
                                    .collect( Collectors.toSet() );
                    final State nextState = new State();
                    final boolean isAcceptingState = ! matchingRules.isEmpty();
                    if ( isAcceptingState )
                    {
                        if ( matchingRules.size() > 1 )
                        {
                            nextState.lexerRule = ambiguityResolver.apply( matchingRules );
//                            throw new IllegalStateException("Grammar contains ambiguous lexer states: "+tokenTypes);
                        } else
                        {
                            nextState.lexerRule = matchingRules.iterator().next();
                        }
                    }
                    nextState.isAcceptingState = isAcceptingState;

                    assignName.accept( nextState );
                    toProcess.push( nextState );
                    System.out.println("State "+current+" loops to new state "+nextState+" "+epsilonClosure);
                    transitionCreator.accept( current, nextState );
                    newGraph.put( nextState, epsilonClosure );
                }
            });
        }
        initialState = first;

        final Map<Integer, State> verify = first.gatherAllStates();
        if ( ! initialState.isDFA() ) {
            throw new IllegalStateException("Automaton is not a DFA ?");
        }
    }

    public boolean isDFA() {
        return initialState.isDFA();
    }

    private Set<State> epsilonClosure(Set<State> set)
    {
        final Set<State> epsilonMove = new HashSet<>(set);
        for ( State state : set )
        {
            epsilonMove.addAll( epsilonClosure( state ) );
        }
        return epsilonMove;
    }

    private Set<State> epsilonClosure(State s)
    {
        final HashSet result = new HashSet<>();
        epsilonClosure(s, result, new HashSet<>() );
        return result;
    }

    private void epsilonClosure(State current,Set<State> result,Set<State> visited)
    {
        if ( visited.contains( current ) ) {
            return;
        }
        visited.add( current );

        if ( current.incomingTransitionCount() == 0 || // initial state is implicitly reachable via epsilon
                current.getIncomingTransitions().stream().anyMatch(  t -> t.isEpsilon() ) )
        {
            result.add( current );
        }

        for ( Transition t : current.getOutgoingTransitions() )
        {
            if ( t.isEpsilon() )
            {
                epsilonClosure( t.destination, result, visited );
            }
        }
    }
}