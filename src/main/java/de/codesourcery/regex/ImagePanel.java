package de.codesourcery.regex;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

public final class ImagePanel extends JPanel implements Consumer<State>
{
    private static final String DOT_PATH = "/usr/bin/dot";

    private Image image;

    private State state;

    public ImagePanel()
    {
        addComponentListener( new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                try
                {
                    redraw();
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }
            }
        } );
    }

    private void setImage(Image image)
    {
        this.image = image;
        revalidate();
        repaint();
        Toolkit.getDefaultToolkit().sync();
    }

    public void show(State s)
    {
        this.state = s.copyGraph().entry;
        try
        {
            redraw();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void redraw() throws IOException, InterruptedException
    {
        renderImage();
        setImage( image );
    }

    private void renderImage() throws IOException, InterruptedException
    {
        image = null;
        if ( state == null ) {
            return;
        }

        final String dot = state.toDOT(getSize(), false);
        File dotFile = File.createTempFile( "temp", ".dot" );
        System.out.println("Dot output is in "+dotFile.getAbsolutePath());
        dotFile.deleteOnExit();
        File imageFile = File.createTempFile( "temp", ".png" );
        imageFile.deleteOnExit();
        try ( PrintWriter w = new PrintWriter( dotFile) ) {
            w.write( dot );
        }
        final Process process = Runtime.getRuntime().exec( new String[]{DOT_PATH, "-Tpng",
        "-o" + imageFile.getAbsolutePath(), dotFile.getAbsolutePath()} );

        final int exitCode = process.waitFor();
        if ( exitCode != 0 ) {
            throw new IOException("DOT execution failed with exit code "+exitCode);
        }
        image = ImageIO.read( imageFile );
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent( g );
        if ( state!= null )
        {
            if ( image == null || image.getWidth( null ) != getWidth() || image.getHeight( null ) != getHeight() ) {
                try
                {
                    renderImage();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if ( image != null )
            {
                g.drawImage( image, 0, 0, null );
            }
        }
    }

    @Override
    public void accept(State state)
    {
        show(state);
    }
}