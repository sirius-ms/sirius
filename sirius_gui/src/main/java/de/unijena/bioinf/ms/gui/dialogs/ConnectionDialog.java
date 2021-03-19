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

import de.unijena.bioinf.webapi.WebAPI;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionCheckPanel;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Marcus Ludwig on 17.11.16.
 */
public class ConnectionDialog extends JDialog implements ActionListener {
    private final static String name = "Webservice Connection";
    private JButton proxy;
    private ConnectionCheckPanel connectionCheck;


    public ConnectionDialog(Frame owner, int state, @Nullable WorkerList workerList) {
        super(owner, name, ModalityType.APPLICATION_MODAL);
        initDialog(state, workerList);
    }

    private void initDialog(int state, @Nullable WorkerList workerList) {
        setLayout(new BorderLayout());

        //header
        JPanel header = new DialogHaeder(Icons.NET_64);
        add(header, BorderLayout.NORTH);

        connectionCheck = new ConnectionCheckPanel(state, workerList);
        add(connectionCheck, BorderLayout.CENTER);


        //south
        proxy = new JButton("Open proxy settings");
        proxy.addActionListener(this);

        JButton ok = new JButton("Ok");
        ok.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(proxy);
        buttons.add(ok);

        add(buttons, BorderLayout.SOUTH);


        setMinimumSize(new Dimension(350, getPreferredSize().height));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
        if (state > WebAPI.MAX_STATE)
            if (getParent() instanceof Dialog) {
                new ErrorReportDialog((Dialog) getParent(), "An unknown Network Error occurred!");
            } else {
                new ErrorReportDialog((Frame) getParent(), "An unknown Network Error occurred!");
            }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        this.dispose();
        if (e.getSource().equals(proxy)) {
            new SettingsDialog(MainFrame.MF, 2);
        }
    }
}
