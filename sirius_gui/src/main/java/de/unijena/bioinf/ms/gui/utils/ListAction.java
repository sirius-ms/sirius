/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ListAction implements MouseListener {
    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    private JList list;
    private KeyStroke keyStroke;

    /*
     *	Add an Action to the JList bound by the default KeyStroke
     */
    public ListAction(JList list, Action action) {
        this(list, action, ENTER);
    }

    /*
     *	Add an Action to the JList bound by the specified KeyStroke
     */
    public ListAction(JList list, Action action, KeyStroke keyStroke) {
        this.list = list;
        this.keyStroke = keyStroke;

        //  Add the KeyStroke to the InputMap

        InputMap im = list.getInputMap();
        im.put(keyStroke, keyStroke);

        //  Add the Action to the ActionMap

        setAction(action);

        //  Handle mouse double click

        list.addMouseListener(this);
    }

    /*
     *  Add the Action to the ActionMap
     */
    public void setAction(Action action) {
        list.getActionMap().put(keyStroke, action);
    }

    //  Implement MouseListener interface

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            Action action = list.getActionMap().get(keyStroke);

            if (action != null) {
                ActionEvent event = new ActionEvent(
                        list,
                        ActionEvent.ACTION_PERFORMED,
                        "");
                action.actionPerformed(event);
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
