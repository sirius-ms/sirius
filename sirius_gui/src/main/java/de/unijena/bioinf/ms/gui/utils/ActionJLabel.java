/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class ActionJLabel extends JLabel {

    public ActionJLabel(String text, Action action) {
        this(text, e -> action.actionPerformed(null));
    }

    public ActionJLabel(String text, Consumer<MouseEvent> action) {
        super(text);
        init(action);
    }

    public ActionJLabel(Icon image, Action action) {
        this(image, e -> action.actionPerformed(null));
    }

    public ActionJLabel(Icon image, Consumer<MouseEvent> action) {
        super(image);
        init(action);
    }


    public void setAction(Consumer<MouseEvent> action){

    }

    protected void init(Consumer<MouseEvent> action) {
        setText("<html><a href=''>" + getText() + "</a></html>");
        setForeground(Color.BLUE.darker());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.accept(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // the mouse has entered the label
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // the mouse has exited the label
            }

        });
    }
}
