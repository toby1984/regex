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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ImagePanel extends JPanel implements Consumer<State>, Scrollable
{
    private static final int REPAINT_DELAY_MILLIS = 500;

    private static final String DOT_PATH = "/usr/bin/dot";

    private final AtomicReference<Image> imageRef = new AtomicReference<>();

    private final AtomicReference<State> stateRef = new AtomicReference<>();

    private final RedrawThread thread = new RedrawThread();

    private double zoom = 1.0;

    public ImagePanel()
    {
        thread.start();
        addComponentListener( new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                redraw();
            }
        } );
    }

    public void zoomOut() {

        if ( zoom-0.1 > 0 ) {
            zoom -= 0.1;
            redraw();
        }
    }

    public void zoomIn() {

        if ( zoom+0.1 <= 5 ) {
            zoom += 0.1;
            redraw();
        }
    }

    private void setImage(Image image)
    {
        this.imageRef.set( image );
        revalidate();
        repaint();
        Toolkit.getDefaultToolkit().sync();
        System.out.println("sync() called");
    }

    public void show(State s)
    {
        this.stateRef.set( s.copyGraph(false).entry );
        try
        {
            redraw();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        final Image image =
                imageRef.get();
        return image == null ? super.getPreferredSize() : new Dimension(image.getWidth( null ), image.getHeight(null) );
    }

    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
        return new Dimension(400,200);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return 10;
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    private final class RedrawThread extends Thread {

        private final Object LOCK = new Object();

        // @GuardedBy( LOCK )
        private long lastTrigger = System.currentTimeMillis();

        {
            setDaemon(true);
        }

        @Override
        public void run()
        {
            while ( true )
            {
                synchronized(LOCK)
                {
                    if ( ! shouldRun() )
                    {
                        System.out.println("Deferring redraw");
                        try
                        {
                            LOCK.wait( REPAINT_DELAY_MILLIS );
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    lastTrigger = 0;
                }
                try
                {
                    internalRedraw();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        private boolean shouldRun()
        {
            synchronized(LOCK)
            {
                if ( lastTrigger == 0 ) {
                    return false;
                }
                final long now = System.currentTimeMillis();
                final long elapsed = now - lastTrigger;
                return elapsed > REPAINT_DELAY_MILLIS;
            }
        }

        public void trigger()
        {
            synchronized( LOCK )
            {
                lastTrigger = System.currentTimeMillis();
                LOCK.notifyAll();
            }
        }
    }

    private void redraw() {
        thread.trigger();
    }

    private void internalRedraw() throws IOException, InterruptedException
    {
        System.out.println("Now rendering image...");
        final Image image = renderImage();
        System.out.println("Done rendering image ("+image.getWidth( null) +" x "+image.getHeight( null ));
        SwingUtilities.invokeLater( () -> setImage( image ) );
    }

    private Image renderImage() throws IOException, InterruptedException
    {
        if ( stateRef.get() == null ) {
            return imageRef.get();
        }

//      final String dot = state.toDOT(getSize(), false);
        final String dot = stateRef.get().toDOT(null, false);
        File dotFile = File.createTempFile( "temp", ".dot" );
        System.out.println("Dot file is "+dotFile.getAbsolutePath());
        dotFile.deleteOnExit();
        File imageFile = File.createTempFile( "temp", ".png" );
        System.out.println("PNG image is "+imageFile.getAbsolutePath());
        imageFile.deleteOnExit();
        try ( PrintWriter w = new PrintWriter( dotFile) ) {
            w.write( dot );
        }
        final Process process = Runtime.getRuntime().exec( new String[]{DOT_PATH, "-Tpng",
        "-o" + imageFile.getAbsolutePath(), dotFile.getAbsolutePath()} );

        final int exitCode = process.waitFor();
        if ( exitCode != 0 ) {
            final byte[] bytes = process.getErrorStream().readAllBytes();
            System.err.println("ERROR: "+new String(bytes));
            throw new IOException("DOT execution failed with exit code "+exitCode);
        }
        return ImageIO.read( imageFile );
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent( g );
        if ( stateRef.get() != null )
        {
            Image image = imageRef.get();
            if ( image == null ) {
                redraw();
            }
            else
            {
                System.out.println("Painting image");
                if ( zoom == 1.0 )
                {
                    g.drawImage( image, 0, 0, null );
                } else {
                    int w = (int) (image.getWidth( null )*zoom);
                    int h = (int) (image.getHeight( null )*zoom);
                    g.drawImage( image, 0, 0,  w, h, null );
                }
            }
        }
    }

    @Override
    public void accept(State state)
    {
        show(state);
    }
}