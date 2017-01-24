/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FingerIdDialog extends JDialog {

    public static final int COMPUTE =1, CANCELED=0, COMPUTE_ALL=2;
    protected FingerIdData data;
    protected boolean showComputeButton;
    protected int returnState = CANCELED;
    protected FingerIDComputationPanel dbForm;
    protected CSIFingerIdComputation storage;

    public FingerIdDialog(Frame owner, CSIFingerIdComputation storage, FingerIdData data, boolean showComputeButton) {
        super(owner, "Search with CSI:FingerId", true);
        this.storage = storage;
        this.dbForm = new FingerIDComputationPanel(this.storage.enforceBio);
        this.data = data;
        this.showComputeButton = showComputeButton;
        setLocationRelativeTo(owner);
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

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
        this.add(southPanel,BorderLayout.SOUTH);

        if (showComputeButton) {
            final JButton computeAll = new JButton("Search all");
            computeAll.setToolTipText("Search ALL items with CSI:FingerID");
            computeAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    storage.setEnforceBio(dbForm.biodb.isSelected());
                    storage.configured = true;
                    returnState = COMPUTE_ALL;
                    dispose();
                }
            });
            southPanel.add(computeAll);
        }

        JButton approve = new JButton("Search");
        approve.setToolTipText("Search SELECTED items with CSI:FingerID");
        approve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                storage.setEnforceBio(dbForm.biodb.isSelected());
                storage.configured = true;
                returnState = COMPUTE;
                dispose();
            }
        });
        final JButton abort = new JButton("Abort");
        abort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        southPanel.add(approve);
        southPanel.add(abort);
        pack();
        setVisible(true);
    }

    public boolean isBio() {
        return dbForm.biodb.isSelected();
    }
}
