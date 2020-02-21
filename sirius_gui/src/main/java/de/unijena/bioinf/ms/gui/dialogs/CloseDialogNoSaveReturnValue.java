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

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CloseDialogNoSaveReturnValue extends JDialog implements ActionListener {

    private CloseDialogReturnValue rv;

    private JButton delete, abort;
    private JCheckBox dontaskagain;
    private final String dontaskagainKey;

    public CloseDialogNoSaveReturnValue(Frame owner, String question, String dontaskagainKey) {
        super(owner, true);
        rv = CloseDialogReturnValue.abort;
        this.dontaskagainKey = dontaskagainKey;

        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        Icon icon = UIManager.getIcon("OptionPane.questionIcon");
        northPanel.add(new JLabel(icon));
        northPanel.add(new JLabel(question));
        this.add(northPanel, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        dontaskagain = new JCheckBox();
        dontaskagain.setSelected(false);
        south.add(dontaskagain);
        south.add(new JLabel("Do not ask again"));


        delete = new JButton("Delete compound");
        delete.addActionListener(this);
        abort = new JButton("Abort");
        abort.addActionListener(this);
        south.add(delete);
        south.add(abort);
        this.add(south, BorderLayout.SOUTH);
        this.pack();
        setLocationRelativeTo(getParent());
        this.setVisible(true);
    }

    public CloseDialogReturnValue getReturnValue() {
        return rv;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == delete) {
            rv = CloseDialogReturnValue.delete;
        } else if (e.getSource() == abort) {
            rv = CloseDialogReturnValue.abort;
        } else return;

        Jobs.runInBackground(() -> SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(dontaskagainKey, String.valueOf(dontaskagain.isSelected())));
        this.dispose();
    }
}
