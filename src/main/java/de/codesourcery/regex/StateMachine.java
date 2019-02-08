package de.codesourcery.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class StateMachine
{
    public State initialState;
    private String expression;

    public void setup(String regex)
    {
        this.expression = regex;
        setup(new Scanner(regex));
    }

    private void setup(Scanner scanner)
    {
        State.ID = 0;

        final Boxes boxes = new Boxes();
        while ( ! scanner.eof() ) {
            parse(scanner, boxes);
        }
        final Box first = boxes.join();
        initialState = first.entry;
    }

    private void parse(Scanner scanner, Boxes boxes)
    {
        while ( ! scanner.eof() )
        {
            char currentChar = scanner.consume();
            if (currentChar == '\\')
            {
                currentChar = scanner.consume();
            }

            if ( currentChar == '[' ) { // character class
                boxes.add( parseCharacterClass( scanner ) );
                return;
            }

            if (currentChar == '|') // union
            {
                final Boxes expr = new Boxes();
                parse(scanner, expr);
                Box union1 = boxes.join();
                Box union2 = expr.join();

                final Box result = new Box(new State(), new State());
                result.entry.transition(union1.entry);
                result.entry.transition(union2.entry);

                union1.exit.transition(result.exit );
                union2.exit.transition(result.exit );

                boxes.set(result);
                return;
            }

            if (currentChar == '(')
            {
                final Boxes list = new Boxes();
                parse(scanner, list);
                final Box joined = list.join();
                final boolean createNewBox = boxes.size() == 0 || (!scanner.eof() && isPostfixOperator( scanner.peek() ) );
                if ( createNewBox )
                {
                    boxes.add( joined );
                }
                else
                {
                    final Box existing = boxes.join();
                    existing.exit.transition( joined.entry );
                    boxes.set( new Box( existing.entry, joined.exit ) );
                }
                return;
            }

            if (currentChar == ')')
            {
                // calling code relies on only one box being present
                boxes.join();
                return;
            }

            switch (currentChar)
            {
                case '+': // 1...n
                    Box last = boxes.last();
                    Box repeat = last.entry.copyGraph();
                    repeat.exit.transition(repeat.entry);
                    final State newExit = new State();
                    repeat.entry.transition(newExit );

                    final Box merged = Boxes.of(last,repeat).join();
                    merged.exit = newExit;
                    boxes.set( boxes.size()-1 , merged );
                    return;
                case '*': // 0...n

                    final Box newBox = boxes.join();
                    newBox.exit.transition( newBox.entry );

                    final State newStart = new State();
                    newStart.transition( newBox.entry );

                    final State newEnd = new State();
                    newBox.exit.transition( newEnd );

                    newStart.transition( newEnd );
                    boxes.set( new Box( newStart, newEnd ) );
                    return;
                case '?': // 0..1
                    last = boxes.last();
                    last.entry.transition(last.exit);
                    return;
                default:
                    final boolean createNewBox = boxes.size() == 0 || (!scanner.eof() && isPostfixOperator( scanner.peek() ) );
                    final State entry = createNewBox ? new State() : boxes.last().exit;
                    final State exit = new State();
                    if (currentChar == '.')
                    {
                        entry.anyCharacter(exit);
                    }
                    else
                    {
                        entry.transition(currentChar, exit);
                    }
                    if ( createNewBox )
                    {
                        boxes.add( new Box( entry, exit ) );
                    } else {
                        boxes.last().exit = exit;
                    }
            }
        }
    }

    private Box parseCharacterClass(Scanner scanner) {

        final State start = new State();
        final State end = new State();
        final Box box = new Box( start, end );
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
        if ( box.entry.getAllTransitions().isEmpty() ) {
            throw new IllegalStateException("Empty character class at offset "+scanner.offset());
        }
        return box;
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

    public void toDFA(Consumer<State> debugImage)
    {
        final String stateNames = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        /*
        Operation        Description
        ε-closure(s)     Set of NFA states reachable from NFA state s on ε-transitions alone
        ε-closure(T)     Set of NFA states reachable from set of states T on ε-transitions alone
        move(T,a)        Set of states to which there is a transition on input symbol a from some NFA state in T
        */

        // calculate size of language
        final Alphabet alphabet = new Alphabet();
        initialState.visitOutgoingTransitions( transition -> {
            if ( transition.isAnyChar() ) {
                alphabet.addAnyChar();
            } else if ( transition.isChar() ) {
                alphabet.add( ((TransitionChar) transition).c );
            }
        });

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

                    final boolean isAcceptingState = containsTerminalState( epsilonClosure );
                    final State nextState = new State( isAcceptingState ? "END" : null );
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
    }

    public boolean isDFA() {
        return initialState.isDFA();
    }

    private boolean containsTerminalState(Set<State> set) {
        return set.stream().anyMatch( s -> s.isTerminalState() );
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