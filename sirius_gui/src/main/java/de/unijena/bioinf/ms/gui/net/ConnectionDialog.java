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

package de.unijena.bioinf.ms.gui.net;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.settings.SettingsDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import io.sirius.ms.sdk.model.ConnectionCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marcus Ludwig
 */
public final class ConnectionDialog extends JDialog implements ActionListener {
    private final static String name = "Webservice Connection";
    private JButton network;
    private JButton account;

    private static ConnectionDialog instance;

    private final SiriusGui gui;
    private ConnectionCheckPanel connectionCheck;

    public static synchronized ConnectionDialog of(SiriusGui gui) {
        return of(gui, null);
    }

    public static synchronized ConnectionDialog of(SiriusGui gui, @Nullable ConnectionCheck check) {
        if (instance != null)
            instance.dispose();
        instance = new ConnectionDialog(gui, check);
        return instance;
    }

    private ConnectionDialog(@NotNull SiriusGui gui, @Nullable ConnectionCheck check) {
        super(gui.getMainFrame(), name, ModalityType.APPLICATION_MODAL);
        this.gui = gui;
        initDialog(check);
    }

    private void initDialog(@Nullable ConnectionCheck check) {
        setLayout(new BorderLayout());

        //header
        JPanel header = new DialogHeader(Icons.NET.derive(64,64));
        add(header, BorderLayout.NORTH);

        connectionCheck = new ConnectionCheckPanel(this, gui, check);
        connectionCheck.setBackground(getBackground());
        gui.getConnectionMonitor().addConnectionListener(connectionCheck);
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


        pack();
        setMinimumSize(new Dimension(525,575));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    @Override
    public void dispose() {
        if (instance == this)
            instance = null;
        if (connectionCheck != null)
            gui.getConnectionMonitor().removePropertyChangeListener(connectionCheck);
        super.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.dispose();
        if (e.getSource().equals(network))
            Jobs.runEDTLater(() -> new SettingsDialog(gui, 2));
        if (e.getSource().equals(account))
            Jobs.runEDTLater(() -> mf().getToolbar().getAccount().getAction().actionPerformed(e));
    }

    private MainFrame mf(){
        return (MainFrame) getOwner();
    }

}
