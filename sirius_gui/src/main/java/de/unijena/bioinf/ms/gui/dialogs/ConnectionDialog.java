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

package de.unijena.bioinf.ms.gui.dialogs;

import com.google.common.collect.Multimap;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionCheckPanel;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.rest.ConnectionError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Marcus Ludwig on 17.11.16.
 */
public final class ConnectionDialog extends JDialog implements ActionListener {
    private final static String name = "Webservice Connection";
    private JButton network;
    private JButton account;
    private ConnectionCheckPanel connectionCheck;


    private static ConnectionDialog instance;

    public static synchronized ConnectionDialog of(Frame owner, @NotNull Multimap<ConnectionError.Klass, ConnectionError> errors, @Nullable WorkerList workerList, @NotNull LicenseInfo license) {
        if (instance != null)
            instance.dispose();
        instance = new ConnectionDialog(owner, errors, workerList, license);
        return instance;
    }

    private ConnectionDialog(Frame owner, @NotNull Multimap<ConnectionError.Klass, ConnectionError> errors, @Nullable WorkerList workerList,  @NotNull LicenseInfo license) {
        super(owner, name, ModalityType.APPLICATION_MODAL);
        initDialog(errors, workerList, license);
    }

    private void initDialog(@NotNull Multimap<ConnectionError.Klass, ConnectionError> errors, @Nullable WorkerList workerList,  @NotNull LicenseInfo license) {
        setLayout(new BorderLayout());

        //header
        JPanel header = new DialogHeader(Icons.NET_64);
        add(header, BorderLayout.NORTH);

        connectionCheck = new ConnectionCheckPanel(this, errors, workerList, license);
        add(connectionCheck, BorderLayout.CENTER);


        //south
        network = new JButton("Network Settings");
        network.addActionListener(this);

        account = new JButton("Account");
        account.addActionListener(this);

        JButton ok = new JButton("Close");
        ok.addActionListener(this);

        Box buttons = Box.createHorizontalBox();
        buttons.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        buttons.add(account);
        buttons.add(network);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(ok);

        add(buttons, BorderLayout.SOUTH);


        setMinimumSize(new Dimension(350, getPreferredSize().height));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    @Override
    public void dispose() {
        if (instance == this)
            instance = null;
        super.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.dispose();
        if (e.getSource().equals(network))
            Jobs.runEDTLater(() -> new SettingsDialog(MainFrame.MF, 2));
        if (e.getSource().equals(account))
            Jobs.runEDTLater(() -> SiriusActions.SHOW_ACCOUNT.getInstance().actionPerformed(e));
    }
}
