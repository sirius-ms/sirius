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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabases;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FingerIdDialog extends JDialog {

    public static final int COMPUTE = 1, CANCELED = 0, COMPUTE_ALL = 2;

    protected boolean showComputeButton;
    protected int returnState = CANCELED;
    protected FingerIDComputationPanel dbForm;
    protected CSIFingerIDComputation storage;
    private String buttonSuffix = "compounds";

    public FingerIdDialog(Frame owner, CSIFingerIDComputation storage, boolean showComputeButton, boolean local) {
        super(owner, "Search with CSI:FingerID", true);
        this.storage = storage;
        dbForm = new FingerIDComputationPanel(SearchableDatabases.getAvailableDatabases());
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
            computeAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    returnState = COMPUTE_ALL;
                    dispose();
                }
            });
            southPanel.add(computeAll);
        }

        JButton approve = new JButton("Search selected");
        approve.setToolTipText("Search SELECTED " + buttonSuffix + " with CSI:FingerID");
        approve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnState = COMPUTE;
                dispose();
            }
        });

        final JButton abort = new JButton("Close");
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

    public SearchableDatabase getSearchDb() {
        return dbForm.dbSelectionOptions.getDb();
    }
}
