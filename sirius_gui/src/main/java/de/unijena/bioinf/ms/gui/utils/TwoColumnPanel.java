/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

/**
 * @author Markus Fleischauer
 */
public class TwoColumnPanel extends JPanel {
    public  final GridBagConstraints both, left, right;



    public TwoColumnPanel() {
        this(GridBagConstraints.EAST,GridBagConstraints.WEST);
    }
    public TwoColumnPanel(final int leftAnchor, final int rightAnchor) {
        super();
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
        left = new GridBagConstraints();
        left.gridx = 0;
        left.fill = GridBagConstraints.NONE;
        left.anchor = leftAnchor;
        left.weightx = 0;
        left.weighty = 0;
        left.insets = new Insets(0, 0, 0, 5);

        right = new GridBagConstraints();
        right.gridx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = rightAnchor;
        right.weightx = 1;
        right.weighty = 0;

        both = new GridBagConstraints();
        both.gridx = 0;
        both.gridwidth = 2;
        both.fill = GridBagConstraints.HORIZONTAL;
        both.insets = new Insets(0, 0, 5, 0);
        setRow(0);
    }

    public TwoColumnPanel(String leftLabel, Component right) {
        this(new JLabel(leftLabel), right);
    }

    public TwoColumnPanel(Component left, Component right) {
        this();
        add(left, right);
    }

    public TwoColumnPanel(Component center) {
        this();
        add(center);
    }

    public void setRow(int i) {
        left.gridy = i;
        right.gridy = i;
        both.gridy = i;
    }

    public void resetRow() {
        int i = this.getComponentCount();
        left.gridy = i;
        right.gridy = i;
        both.gridy = i;
    }


    public void addNamed(String name, Component rightComp) {
        addNamed(name, rightComp, null);
    }
    public void addNamed(String name, Component rightComp, String toolTip) {
        addNamed(name, rightComp, toolTip, 0);
    }
    public void addNamed(String name, Component rightComp, String toolTip, int gap) {
        JLabel l = new JLabel(name);
        if (toolTip != null && ! toolTip.isBlank()) {
            l.setToolTipText(toolTip);
            if (rightComp instanceof JComponent)
                ((JComponent)rightComp).setToolTipText(toolTip);
        }

        add(l, rightComp, gap, false);
    }

    public void add(Component leftComp, Component rightComp) {
        add(leftComp, rightComp, 0, false);
    }

    public void add(Component leftComp, Component rightComp, int gap, boolean verticalResize) {
        if (verticalResize) {
            left.fill = GridBagConstraints.VERTICAL;
            left.weighty = 1;
            right.fill = GridBagConstraints.BOTH;
            right.weighty = 1;
        }

        left.insets.top += gap;
        right.insets.top += gap;

        if (leftComp != null)
            add(leftComp, left);

        if (rightComp != null)
            add(rightComp, right);


        left.fill = GridBagConstraints.NONE;
        right.fill = GridBagConstraints.HORIZONTAL;

        left.weighty = 0;
        right.weighty = 0;

        left.insets.top -= gap;
        right.insets.top -= gap;

        left.gridy++;
        right.gridy++;
        both.gridy++;
    }

    @Override
    public Component add(Component comp){
        add(comp,0,false);
        return comp;
    }

    public void add(Component comp, int gap, boolean verticalResize) {
        if (verticalResize){
            both.fill = GridBagConstraints.BOTH;
            both.weighty = 1;
        }
        both.insets.top += gap;

        super.add(comp, both);

        both.fill = GridBagConstraints.HORIZONTAL;
        both.insets.top -= gap;
        both.weighty = 0;

        left.gridy++;
        right.gridy++;
        both.gridy++;
    }

    public void addVerticalGlue(){
        double weighty = both.weighty;
        both.weighty = Integer.MAX_VALUE;
        add(Box.createVerticalBox());
        both.weighty = weighty;
    }

    public static TwoColumnPanel of(Component leftComp, Component rightComp){
        TwoColumnPanel p = new TwoColumnPanel();
        p.add(leftComp,rightComp);
        return p;
    }
}
