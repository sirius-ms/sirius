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

package de.unijena.bioinf.ms.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.09.16.
 */

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class AbstractArccordeoDialog extends JDialog implements ActionListener {
    public enum ExtentionPos {NORTH, CENTER, SOUTH/*, BELOW_BUTTONS*/}


    private final ExtentionPos pos;
    private final JButton expander = new JButton();
    private JPanel expandation = null;

    public AbstractArccordeoDialog(Frame owner, ExtentionPos pos) {
        super(owner);
        this.pos = pos;

    }

    public AbstractArccordeoDialog(Frame owner, boolean modal, ExtentionPos pos) {
        super(owner, modal);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Frame owner, String title, ExtentionPos pos) {
        super(owner, title);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Frame owner, String title, boolean modal, ExtentionPos pos) {
        super(owner, title, modal);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc, ExtentionPos pos) {
        super(owner, title, modal, gc);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Dialog owner, ExtentionPos pos) {
        super(owner);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Dialog owner, boolean modal, ExtentionPos pos) {
        super(owner, modal);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Dialog owner, String title, ExtentionPos pos) {
        super(owner, title);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Dialog owner, String title, boolean modal, ExtentionPos pos) {
        super(owner, title, modal);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc, ExtentionPos pos) {
        super(owner, title, modal, gc);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Window owner, ExtentionPos pos) {
        super(owner);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Window owner, ModalityType modalityType, ExtentionPos pos) {
        super(owner, modalityType);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Window owner, String title, ExtentionPos pos) {
        super(owner, title);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Window owner, String title, ModalityType modalityType, ExtentionPos pos) {
        super(owner, title, modalityType);
        this.pos = pos;
    }

    public AbstractArccordeoDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc, ExtentionPos pos) {
        super(owner, title, modalityType, gc);
        this.pos = pos;
    }

    public void expand() {
        expander.setIcon(Icons.LIST_REMOVE_16);
        expander.setToolTipText("less");
        expandation.setVisible(true);
    }

    public void contract() {
        expander.setIcon(Icons.LIST_ADD_16);
        expander.setToolTipText("more");
        expandation.setVisible(false);
    }

    public void setExpanded(boolean expanded) {
        if (expanded == isExpanded())
            return;
        if (expanded) {
            expand();
        } else {
            contract();
        }
        Window window = SwingUtilities.windowForComponent(expandation);
        window.pack();
    }

    public boolean isExpanded() {
        return expandation.isVisible();
    }


    protected abstract JPanel buildNorthPanel();

    protected abstract JPanel buildSouthPanel();

    protected abstract JPanel buildExpandPanel();

    protected abstract JPanel buildButtonPanel();


    protected void buildAndPackDialog() {
        expandation = buildExpandPanel();
        expander.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setExpanded(!isExpanded());
            }
        });

        expand();

        setLayout(new BorderLayout());

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.PAGE_AXIS));

        final java.util.List<JPanel> comps = new ArrayList<>(3);
        comps.add(buildNorthPanel());
        comps.add(buildSouthPanel());
        comps.add(pos.ordinal(), expandation);


        for (JPanel comp : comps) {
            body.add(Box.createVerticalStrut(GuiUtils.MEDIUM_GAP));
            body.add(comp);
        }


        add(body, BorderLayout.NORTH);
        JPanel bottom = buildBottomPanel();
        add(bottom, BorderLayout.SOUTH);



        setResizable(false);
        pack();
        setLocationRelativeTo(getParent());
        setExpanded(false);
        setVisible(true);
    }


    private JPanel buildBottomPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());

        JPanel button = new JPanel(new FlowLayout(FlowLayout.LEFT, GuiUtils.SMALL_GAP, 0));
        button.add(expander);

        buttonPanel.add(button, BorderLayout.WEST);

        JPanel buttons = buildButtonPanel();
        if (buttons != null)
            buttonPanel.add(buttons, BorderLayout.EAST);

        return buttonPanel;
    }
}

