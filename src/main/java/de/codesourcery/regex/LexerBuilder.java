package de.codesourcery.regex;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LexerBuilder
{
    public StateMachine build(InputStream configFile) throws IOException {

        final Map<String,StateMachine> matchers = new HashMap<>();

        StateMachine result = null;

        try ( BufferedReader reader = new BufferedReader( new InputStreamReader(configFile) ) ) {
            String line;
            int lineNo = 1;
            outer:
            for ( ; ( line = reader.readLine() ) != null ; lineNo++ )
            {
                line = line.trim();
                System.out.println("LINE: "+line);
                for ( int i = 0 , l = line.length(); i < l ; i++ )
                {
                    final char c = line.charAt( i );
                    if ( c == '#' )
                    {
                        continue outer;
                    }
                    if ( c != ' ' && c != '\t' ) {
                        break;
                    }
                }
                final int idx = line.indexOf( "=" );
                if ( idx == -1 ) {
                    throw new IllegalArgumentException("Missing '=' on line "+lineNo);
                }
                final String tokenType = line.substring(0,idx);
                if ( matchers.containsKey( tokenType ) ) {
                    throw new IllegalArgumentException("Duplicate token type '"+tokenType+"' on line "+lineNo);
                }
                final String regex = line.substring(idx+1);
                if ( regex.isBlank() ) {
                    throw new IllegalArgumentException( "Blank regex on line "+lineNo );
                }
                final StateMachine sm = new StateMachine();
                matchers.put( tokenType, sm );
                try
                {
                    sm.setup( regex );
                    sm.initialState.getTerminalStates().forEach( s -> s.tokenType = tokenType );
                    if ( result == null ) {
                        result = sm;
                    } else {
                        result = result.union( sm );
                    }
                }
                catch(Exception e) {
                    throw new IllegalArgumentException("Invalid regex for token '"+tokenType+"' on line "+lineNo+": "+e.getMessage(),e);
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException
    {
        final LexerBuilder builder = new LexerBuilder();
        final String input = "# comment\n"+
                "number=[0-9]+";
        builder.build( new ByteArrayInputStream( input.getBytes() ) );
    }
}
