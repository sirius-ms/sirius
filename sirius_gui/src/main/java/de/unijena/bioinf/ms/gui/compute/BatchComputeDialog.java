

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

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.net.ConnectionChecks;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.ConnectionCheck;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

@Slf4j
public class BatchComputeDialog extends JDialog {
    private final SiriusGui gui;

    private ComputePresetAndBannerPanel presetPanel;
    private ComputeToolPanel toolsPanel;
    private ComputeActionsPanel actionsPanel;

    private PropertyChangeListener connectionListener;
    private BatchComputeController batchComputeController;

    public BatchComputeDialog(SiriusGui gui, List<InstanceBean> compoundsToProcess) {
        super(gui.getMainFrame(), compoundsToProcess.isEmpty() ? "Edit Presets" : "Compute", true);
        gui.getConnectionMonitor().checkConnectionInBackground();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        this.gui = gui;
        JPanel main = new JPanel(new BorderLayout());
        LoadablePanel loadableMainWrapper = new LoadablePanel(main, "Initializing...");
        loadableMainWrapper.setLoading(true, true);
        add(loadableMainWrapper, BorderLayout.CENTER);

        // build panel in background with loading animation
        loadableMainWrapper.runInBackgroundAndLoad(() -> {
            boolean ms2 = compoundsToProcess.stream().anyMatch(InstanceBean::hasMsMs) || compoundsToProcess.isEmpty();  // Empty compounds if the dialog is opened to edit presets, ms2 UI should be active

            //make north panel
            presetPanel = new ComputePresetAndBannerPanel(gui, compoundsToProcess.size() == 1, ms2);
            main.add(presetPanel, BorderLayout.NORTH);

            //make scrollable center panel
            toolsPanel = new ComputeToolPanel(gui, compoundsToProcess, ms2);

            final JScrollPane mainSP = new JScrollPane(toolsPanel);
            mainSP.setBorder(BorderFactory.createEtchedBorder());
            mainSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            mainSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            mainSP.getVerticalScrollBar().setUnitIncrement(16);

            main.add(mainSP, BorderLayout.CENTER);

            // make south panel with Recompute/Compute/Abort
            actionsPanel = new ComputeActionsPanel(compoundsToProcess.size());
            main.add(actionsPanel, BorderLayout.SOUTH);


            configureControls(compoundsToProcess);

            presetPanel.reloadPresets();
            presetPanel.selectDefaultPreset(true);

            connectionListener = evt -> {
                if (evt instanceof ConnectionMonitor.ConnectionEvent stateEvent)
                    Jobs.runEDTLater(() -> presetPanel.updateConnectionBanner(stateEvent.getConnectionCheck()));
            };
            gui.getConnectionMonitor().addConnectionListener(connectionListener);


            // perform initial connection check in to have a proper state on start up
            @Nullable ConnectionCheck checkResult = gui.getConnectionMonitor().getCurrentCheckResult();
            if (ConnectionChecks.isInternet(checkResult) && !ConnectionChecks.isLoggedIn(checkResult)) {
                SiriusActions.SIGN_IN.getInstance(gui, true).actionPerformed(null);
                checkResult = gui.getConnectionMonitor().checkConnection();
            }
            presetPanel.updateConnectionBanner(checkResult);
        });

        //finalize panel build and make the dialog visible
        setResizable(false);
        setMaximumSize(GuiUtils.getEffectiveScreenSize(getGraphicsConfiguration()));
        setPreferredSize(new Dimension(1050, 970));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
            if (connectionListener != null)
                gui.getConnectionMonitor().removePropertyChangeListener(connectionListener);
        } finally {
            toolsPanel.destroy();
        }
    }


    private void configureControls(List<InstanceBean> compoundsToProcess) {
        batchComputeController = new BatchComputeController(gui, this, toolsPanel, presetPanel, actionsPanel, compoundsToProcess);

        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
        String enterAction = "compute";
        String escAction = "abort";
        inputMap.put(enterKey, enterAction);
        inputMap.put(escKey, escAction);
        getRootPane().getActionMap().put(enterAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                batchComputeController.startComputing();
            }
        });
        getRootPane().getActionMap().put(escAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
}