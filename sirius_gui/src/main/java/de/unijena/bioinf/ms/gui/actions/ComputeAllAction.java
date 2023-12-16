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

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.nightsky.sdk.model.BackgroundComputationsStateEvent;
import de.unijena.bioinf.sse.DataEventType;
import de.unijena.bioinf.sse.DataObjectEvent;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeAllAction extends AbstractGuiAction {
    private final static AtomicBoolean isActive = new AtomicBoolean(false);

    public ComputeAllAction(SiriusGui gui) {
        super(gui);
        computationCanceled();

        //filtered Workspace Listener
        this.mainFrame.getCompoundList().getCompoundList().addListEventListener(listChanges ->
                setEnabled(listChanges.getSourceList().size() > 0));

        setEnabled(!mainFrame.getCompoundList().getCompoundList().isEmpty());

        //Listen if there are active gui jobs
        gui.withSiriusClient((pid, client) -> client.addEventListener(evt -> {
            DataObjectEvent<BackgroundComputationsStateEvent> eventData = ((DataObjectEvent<BackgroundComputationsStateEvent>) evt.getNewValue());
            if (eventData.getData().getNumberOfRunningJobs() > 0) {
                computationStarted();
            } else {
                computationCanceled();
            }
        }, pid, DataEventType.BACKGROUND_COMPUTATIONS_STATE));

        gui.withSiriusClient((pid, client) -> client.jobs().hasJobs(pid, false));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isActive.get()) {
            Jobs.runInBackgroundAndLoad(mainFrame, "Canceling Jobs...", () -> gui.getSiriusClient().jobs()
                    .deleteJobs(gui.getProjectId(), true, true));
        } else {
            if (mainFrame.getCompounds().isEmpty()){
                LoggerFactory.getLogger(getClass()).warn("Not instances to compute! Closing Compute Dialog...");
                return;
            }
            new BatchComputeDialog(gui, List.copyOf(mainFrame.getCompounds()));
        }
    }

    private void computationCanceled() {
        setEnabled(true);
        isActive.set(false);
        putValue(Action.NAME, "Compute All");
        putValue(Action.LARGE_ICON_KEY, Icons.RUN_32);
        putValue(Action.SMALL_ICON, Icons.RUN_16);
        putValue(Action.SHORT_DESCRIPTION, "Compute all compounds");
        setEnabled(!mainFrame.getCompoundList().getCompoundList().isEmpty());
    }

    private void computationStarted() {
        setEnabled(true);
        isActive.set(true);
        putValue(Action.NAME, "Cancel All");
        putValue(Action.LARGE_ICON_KEY, Icons.CANCEL_32);
        putValue(Action.SMALL_ICON, Icons.CANCEL_16);
        putValue(Action.SHORT_DESCRIPTION, "Cancel all running computations");
        setEnabled(!mainFrame.getCompoundList().getCompoundList().isEmpty());
    }

}
