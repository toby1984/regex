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

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ImageFrame extends JFrame
{
    private final ImagePanel imagePanel = new ImagePanel();

    public ImageFrame()
    {
        super("Debug");

        getContentPane().setLayout( new GridBagLayout() );
        final GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.weightx=1.0;
        cnstrs.weighty=1.0;
        cnstrs.fill = GridBagConstraints.BOTH;
        final JScrollPane pane = new JScrollPane( imagePanel );
        getContentPane().add( pane, cnstrs );
    }

    public void show(State s)
    {
        imagePanel.show(s);
    }

}
