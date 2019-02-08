package de.codesourcery.regex;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ImageFrame extends JFrame
{
    private final ImagePanel imagePanel = new ImagePanel();

    public ImageFrame() throws HeadlessException
    {
        super("Debug");

        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( imagePanel, BorderLayout.CENTER );
    }

    public void show(State s) throws IOException, InterruptedException
    {
        imagePanel.show(s);
    }

}
