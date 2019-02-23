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

import de.codesourcery.regex.State;

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
        this.state = s.copyGraph(false).entry;
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