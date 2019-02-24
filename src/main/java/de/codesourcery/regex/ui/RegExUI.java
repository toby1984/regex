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
package de.codesourcery.regex.ui;

import de.codesourcery.regex.LexerBuilder;
import de.codesourcery.regex.State;
import de.codesourcery.regex.StateMachine;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class RegExUI extends JFrame
{
    private static boolean GRAPHICAL_DEBUG = false;

    private LexerBuilder.Configuration lexerConfiguration = new LexerBuilder.Configuration( true );
    private StateMachine stateMachine = new StateMachine();
    private final JTextArea regex = new JTextArea( loadConfig() );
    private final JTextField input = new JTextField();

    private final ImagePanel image = new ImagePanel();
    private final JButton toDFA = new JButton("To DFA");

    private static String loadConfig()
    {
        final String resource = "/rules.txt";
        try (final InputStream stream = RegExUI.class.getResourceAsStream( resource ) )
        {
            if ( stream == null ) {
                throw new FileNotFoundException( "Failed to open "+resource );
            }
            return new String(stream.readAllBytes());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public RegExUI()
    {
        super("RegEx UI");
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException
    {
        new RegExUI().run();
    }

    public void run() throws InvocationTargetException, InterruptedException
    {
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        final Runnable r = () ->
        {
            final Container pane = getContentPane();
            pane.setLayout( new GridBagLayout() );

            // setup UI

            // regex
            GridBagConstraints cnstrs = new GridBagConstraints();
            cnstrs.gridx = 0; cnstrs.gridy = 0;
            cnstrs.weightx = 0 ; cnstrs.weighty = 0.1;
            cnstrs.gridwidth = 1; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.NONE;

            pane.add( new JLabel("RegEx") , cnstrs );

            cnstrs = new GridBagConstraints();
            cnstrs.gridx = 1; cnstrs.gridy = 0;
            cnstrs.weightx = 1 ; cnstrs.weighty = 0.1;
            cnstrs.gridwidth = 2; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.HORIZONTAL;

            regex.setColumns( 40 );
            regex.setRows( 4 );
            pane.add( new JScrollPane(regex), cnstrs );

            // input
            cnstrs = new GridBagConstraints();
            cnstrs.gridx = 0; cnstrs.gridy = 1;
            cnstrs.weightx = 0 ; cnstrs.weighty = 0.1;
            cnstrs.gridwidth = 1; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.NONE;

            pane.add( new JLabel("Input") , cnstrs );

            cnstrs = new GridBagConstraints();
            cnstrs.gridx = 1; cnstrs.gridy = 1;
            cnstrs.weightx = 1 ; cnstrs.weighty = 0.1;
            cnstrs.gridwidth = 1; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.HORIZONTAL;

            pane.add( input , cnstrs );

            cnstrs = new GridBagConstraints();
            cnstrs.gridx = 2; cnstrs.gridy = 1;
            cnstrs.weightx = 0 ; cnstrs.weighty = 0;
            cnstrs.gridwidth = 1; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.NONE;

            final JButton button = new JButton("apply");
            button.addActionListener( ev -> this.doApplyChanges() );
            input.addActionListener( ev -> this.doApplyChanges() );
            pane.add( button , cnstrs );

            // simplify button

            cnstrs = new GridBagConstraints();
            cnstrs.gridx = 0; cnstrs.gridy = 2;
            cnstrs.weightx = 0 ; cnstrs.weighty = 0;
            cnstrs.gridwidth = 1; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.NONE;

            final JButton simplify = new JButton("Simplify");
            simplify.addActionListener( ev ->
            {
                stateMachine.simplify();
                System.out.println(" *** "+stateMachine.initialState.getDebugInfo());
                updateButtons();
                updateImage();
            });
            pane.add( simplify, cnstrs );

            // to DFA button
            cnstrs = new GridBagConstraints();
            cnstrs.gridx = 1; cnstrs.gridy = 2;
            cnstrs.weightx = 0 ; cnstrs.weighty = 0;
            cnstrs.gridwidth = 2; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.NONE;

            toDFA.addActionListener( ev ->
            {
                final Consumer<State> stateConsumer = new Consumer<State>()
                {
                    private ImageFrame frame;

                    @Override
                    public void accept(State s)
                    {
                        if ( ! GRAPHICAL_DEBUG ) {
                            return;
                        }

                        final State stateCopy = s.copyGraph().entry;

                        try
                        {
                            SwingUtilities.invokeAndWait( () ->
                            {
                                if ( frame == null )
                                {
                                    frame = new ImageFrame();
                                    frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
                                    frame.setLocationRelativeTo( null );
                                }
                                try
                                {
                                    frame.show( stateCopy );
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                                frame.pack();
                                if ( !frame.isVisible() )
                                {
                                    frame.setVisible( true );
                                }
                            } );
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                final Thread t = new Thread( () ->
                {
                    try
                    {
                        stateMachine.toDFA( stateConsumer , LexerBuilder.getAmbiguousRulesResolver( lexerConfiguration ) );
                        System.out.println(" *** "+stateMachine.initialState.getDebugInfo());
                    }
                    finally
                    {
                        SwingUtilities.invokeLater( () ->
                        {
                            updateButtons();
                            updateImage(stateMachine.initialState);
                        } );
                    }
                });
                t.setDaemon( true );
                t.start();
            });
            pane.add( toDFA, cnstrs );

            // image
            cnstrs = new GridBagConstraints();
            cnstrs.gridx = 0; cnstrs.gridy = 3;
            cnstrs.weightx = 1 ; cnstrs.weighty = 0.8;
            cnstrs.gridwidth = 3; cnstrs.gridheight = 1;
            cnstrs.insets = new Insets( 5,5,5,5 );
            cnstrs.fill = GridBagConstraints.BOTH;

            pane.add( new JScrollPane(image), cnstrs );

            // show UI
            setPreferredSize( new Dimension( 640, 480 ) );
            setLocationRelativeTo( null );
            pack();
            setVisible( true );

            doApplyChanges();
        };
        SwingUtilities.invokeAndWait( r );
    }

    private void updateButtons()
    {
        if ( stateMachine.initialState != null ) {
            toDFA.setEnabled( ! stateMachine.initialState.isDFA() );
        }
    }

    private void doApplyChanges()
    {
        String sRegex = regex.getText();
        if ( sRegex != null && ! sRegex.isBlank() )
        {
            try
            {
                final LexerBuilder builder = new LexerBuilder();
                lexerConfiguration = builder.parseConfiguration( new ByteArrayInputStream( sRegex.getBytes() ), true );
                stateMachine = builder.buildStateMachine( lexerConfiguration );
                final String source = builder.build( new ByteArrayInputStream( sRegex.getBytes() ) );
//                stateMachine = builder.stateMachine;
                System.out.println(" *** "+stateMachine.initialState.getDebugInfo());
                System.out.println("\n\n\n\n"+source);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            updateButtons();
            updateImage();
        }
    }

    private void updateImage() {
        updateImage( stateMachine.initialState );
    }

    private void updateImage(State state) {
        try
        {
            image.show( state );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}