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

import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;

public class TextHeaderBoxPanel extends JPanel {
    private JPanel body;

    public TextHeaderBoxPanel(final String headerText, Component toAdd) {
        this(headerText);
        add(toAdd);
    }

    public TextHeaderBoxPanel(final String headerText) {
        this(headerText, false);
    }

    public TextHeaderBoxPanel(final String headerText, boolean horizontal) {
        super(new BorderLayout());
        body = new JPanel();
        body.setLayout(new BoxLayout(body, horizontal ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));
        super.add(new JXTitledSeparator(headerText), BorderLayout.NORTH);
        super.add(body, BorderLayout.CENTER);
    }

    @Override
    public Component add(Component comp) {
        return body.add(comp);
    }

    @Override
    public Component add(String name, Component comp) {
        return body.add(name, comp);
    }

    @Override
    public Component add(Component comp, int index) {
        return body.add(comp, index);
    }

    @Override
    public void add(Component comp, Object constraints) {
        body.add(comp, constraints);
    }

    @Override
    public void add(Component comp, Object constraints, int index) {
        body.add(comp, constraints, index);
    }

    @Override
    public void remove(int index) {
        body.remove(index);
    }

    @Override
    public void remove(Component comp) {
        body.remove(comp);
    }

    @Override
    public void removeAll() {
        body.removeAll();
    }

    public JPanel getBody() {
        return body;
    }
}
