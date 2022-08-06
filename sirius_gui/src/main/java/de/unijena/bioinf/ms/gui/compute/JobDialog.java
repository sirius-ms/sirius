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

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.jjobs.JJobManagerPanel;
import de.unijena.bioinf.jjobs.JJobTable;
import de.unijena.bioinf.jjobs.JJobTableFormat;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.table.SiriusTableCellRenderer;
import de.unijena.bioinf.ms.gui.logging.JobLogDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JobDialog extends JDialog {

    protected JFrame owner;

    public JobDialog(JFrame owner) {
        super(owner, "Jobs", false);
        this.owner = owner;


        JJobManagerPanel managerPanel = createJobManagerPanel();

        add(managerPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(owner);
    }

    private JJobManagerPanel createJobManagerPanel() {
        //todo button enable disable stuff
        final JJobTable jobTable = new JJobTable(Jobs.MANAGER(), new JJobTableFormat(), new SiriusTableCellRenderer());
        jobTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // check if a double click
                    int row = jobTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        SwingJJobContainer c = jobTable.getAdvancedTableModel().getElementAt(row);
                        new JobLogDialog(JobDialog.this, c);
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelB = new JButton("Cancel");
        cancelB.setToolTipText("Tries to cancel selected jobs. Note that not all jobs will be cancelable immediately. Some jobs may not be cancelable at all.");
        cancelB.addActionListener(e -> {
            for (SwingJJobContainer c : jobTable.getAdvancedListSelectionModel().getSelected()) {
                c.getSourceJob().cancel();
            }
        });

        JButton openLogb = new JButton("Show log");
        openLogb.setToolTipText("Opens the log window for the selected Job.");
        openLogb.addActionListener(e -> {
            int row = jobTable.getSelectedRow();
            if (row >= 0) {
                SwingJJobContainer c = jobTable.getAdvancedTableModel().getElementAt(row);
                new JobLogDialog(JobDialog.this, c);
            }
        });

        jobTable.getSelectionModel().addListSelectionListener(e -> {
            final boolean enabled = e.getFirstIndex() >= 0;
            cancelB.setEnabled(enabled);
            openLogb.setEnabled(enabled);
        });

        final boolean enabled = jobTable.getSelectedRow() >= 0;
        cancelB.setEnabled(enabled);
        openLogb.setEnabled(enabled);

        buttonPanel.add(openLogb);
        buttonPanel.add(cancelB);


        JPanel cleaningButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearFailedB = new JButton("Clear");
        clearFailedB.setToolTipText("Remove all done/canceled/failed jobs from job list. This will also remove the logs");
        clearFailedB.addActionListener(e -> Jobs.MANAGER().clearFinished()); //todo this could be a global action


        cleaningButtonPanel.add(clearFailedB);


        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.EAST);
        southPanel.add(cleaningButtonPanel, BorderLayout.WEST);

        return new JJobManagerPanel(jobTable, null, southPanel);
    }
}
