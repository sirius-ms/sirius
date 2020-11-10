

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

package de.unijena.bioinf.ms.gui.compute;

import javax.swing.*;
import java.awt.*;

public class FingerIdDialog extends JDialog {

    public static final int COMPUTE = 1, CANCELED = 0, COMPUTE_ALL = 2;

    protected boolean showComputeButton;
    protected int returnState = CANCELED;
    protected FingerIDConfigPanel dbForm;
    private String buttonSuffix = "compounds";

    public FingerIdDialog(Frame owner, boolean showComputeButton, boolean local) {
        super(owner, "Search with CSI:FingerID", true);
        dbForm = new FingerIDConfigPanel(null, null);
        this.showComputeButton = showComputeButton;
        setLocationRelativeTo(owner);
        if (local)
            buttonSuffix = "molecular formulas";
    }

    public int run() {
        refresh();
        return returnState;
    }

    public void refresh() {
        this.setLayout(new BorderLayout());
        Box mainPanel = Box.createVerticalBox();
        add(mainPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        final JPanel dirForm = new JPanel();
        dirForm.setLayout(new BoxLayout(dirForm, BoxLayout.Y_AXIS));

        mainPanel.add(dirForm);
        mainPanel.add(dbForm);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        this.add(southPanel, BorderLayout.SOUTH);

        if (showComputeButton) {
            final JButton computeAll = new JButton("Search all");
            computeAll.setToolTipText("Search ALL " + buttonSuffix + " with CSI:FingerID");
            computeAll.addActionListener(e -> {
                returnState = COMPUTE_ALL;
                dispose();
            });
            southPanel.add(computeAll);
        }

        JButton approve = new JButton("Search selected");
        approve.setToolTipText("Search SELECTED " + buttonSuffix + " with CSI:FingerID");
        approve.addActionListener(e -> {
            returnState = COMPUTE;
            dispose();
        });

        final JButton abort = new JButton("Close");
        abort.addActionListener(e -> dispose());
        southPanel.add(approve);
        southPanel.add(abort);
        pack();
        setVisible(true);
    }
}
