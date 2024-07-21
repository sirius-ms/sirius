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

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.settings.AdductSettingsPanel;
import de.unijena.bioinf.ms.gui.settings.GerneralSettingsPanel;
import de.unijena.bioinf.ms.gui.settings.NetworkSettingsPanel;
import de.unijena.bioinf.ms.gui.settings.SettingsPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SettingsDialog extends JDialog implements ActionListener {
    private JButton discard, save;
    private final Properties nuProps;
    private AdductSettingsPanel addSettings;
    private NetworkSettingsPanel proxSettings;
    private GerneralSettingsPanel genSettings;
    private JTabbedPane settingsPane;

    private SiriusGui gui;

    public SettingsDialog(SiriusGui gui) {
        this(gui, -1);
    }

    public SettingsDialog(SiriusGui gui, int activeTab) {
        super(gui.getMainFrame(), true);
        this.gui = gui;
        setTitle("Settings");
        setLayout(new BorderLayout());
        nuProps = SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties();

//=============NORTH =================
        JPanel header = new DialogHeader(Icons.GEAR_64);
        add(header, BorderLayout.NORTH);

//============= CENTER =================
        settingsPane = new JTabbedPane();
        genSettings = new GerneralSettingsPanel(nuProps, gui);
        genSettings.addVerticalGlue();
        settingsPane.add(genSettings.name(), genSettings);

        addSettings = new AdductSettingsPanel(nuProps);
        settingsPane.add(addSettings.name(), addSettings);

        /*ilpSettings = new ILPSettings(nuProps);
        settingsPane.add(ilpSettings.name(),ilpSettings);*/

        proxSettings = new NetworkSettingsPanel(gui, nuProps);
        settingsPane.add(proxSettings.name(), proxSettings);

//        accountSettings = new AccountSettingsPanel(nuProps, ApplicationCore.WEB_API.getAuthService());
//        settingsPane.add(accountSettings.name(), accountSettings);

        if (activeTab >= 0 && activeTab < settingsPane.getTabCount())
            settingsPane.setSelectedIndex(activeTab);

        add(settingsPane, BorderLayout.CENTER);

//============= SOUTH =================
        discard = new JButton("Discard");
        discard.addActionListener(this);
        save = new JButton("Save");
        save.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(discard);

        add(buttons, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(350, getMinimumSize().height)); //todo use maximum size of tab panes?
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private boolean collectChangedProps() {
        boolean restartMessage = false;
        for (Component c : settingsPane.getComponents()) {
            if (c instanceof SettingsPanel) {
                ((SettingsPanel) c).saveProperties();
                restartMessage = restartMessage || ((SettingsPanel) c).restartRequired();
            }
        }

        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperties(nuProps);

        for (Component c : settingsPane.getComponents()) {
            if (c instanceof SettingsPanel) {
                ((SettingsPanel) c).reloadChanges();
            }
        }

        return restartMessage;
    }

    private MainFrame mf(){
        return (MainFrame) getOwner();
    }



    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == discard) {
            this.dispose();
        } else {
            final boolean rm = Jobs.runInBackgroundAndLoad(mf(), "Applying Changes...", () ->  {
                boolean restartMessage = collectChangedProps();
                Jobs.runInBackground(() -> {
                    LoggerFactory.getLogger(this.getClass()).info("Saving settings to properties File");
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
                    isConnected(CheckConnectionAction.checkConnectionAndLoad(gui));
                });
                return restartMessage;
            }).getResult();

            if (rm)
                new InfoDialog(this, "At least one change you made requires a restart of Sirius to take effect.");
            this.dispose();
        }
    }
}
