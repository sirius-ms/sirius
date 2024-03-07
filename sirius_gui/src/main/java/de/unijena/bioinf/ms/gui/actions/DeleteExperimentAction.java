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

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.CloseDialogNoSaveReturnValue;
import de.unijena.bioinf.ms.gui.dialogs.CloseDialogReturnValue;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Markus Fleischauer
 */
public class DeleteExperimentAction extends AbstractGuiAction {
    public static final String NEVER_ASK_AGAIN_KEY = PropertyManager.PROPERTY_BASE + ".sirius.dialog.delete_experiment_action.ask_again";

    public DeleteExperimentAction(SiriusGui gui) {
        super("Delete", gui);
        putValue(Action.SMALL_ICON, Icons.REMOVE_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "Delete the selected data");


        setEnabled(SiriusActions.notComputingOrEmptySelected(this.mainFrame.getCompoundListSelectionModel()));

        this.mainFrame.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
                setEnabled(SiriusActions.notComputingOrEmptySelected(selection));
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!PropertyManager.getBoolean(NEVER_ASK_AGAIN_KEY, false)) {
            CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(mainFrame, "When removing the selected compound(s) you will loose all computed identification results?", NEVER_ASK_AGAIN_KEY);
            CloseDialogReturnValue val = diag.getReturnValue();
            if (val == CloseDialogReturnValue.abort) return;
        }
        deleteCompounds(new ArrayList<>(mainFrame.getCompoundList().getCompoundListSelectionModel().getSelected()));
    }

    public void deleteCompounds(List<InstanceBean> toRemove) {
        if (toRemove == null || toRemove.isEmpty())
            return;

        //clear selection to prevent EventList from going crazy.
        mainFrame.getCompoundList().getCompoundListSelectionModel().clearSelection();

        Jobs.runInBackgroundAndLoad(mainFrame, "Deleting Compounds...", false, new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {
                synchronized (this) {
                    final AtomicInteger pro = new AtomicInteger(0);
                    updateProgress(0, toRemove.size(), pro.get(), "Deleting...");

                    gui.acceptSiriusClient((client, pid) ->
                            toRemove.forEach(feature -> {
                                try {
                                    if (!feature.isComputing())
                                        client.features().deleteAlignedFeature(pid, feature.getFeatureId());
                                    else
                                        LoggerFactory.getLogger(getClass()).warn("Cannot delete compound '" + feature.getFeatureId() + "' because it is currently computing. Skipping!");
                                } catch (Exception e) {
                                    LoggerFactory.getLogger(getClass()).error("Could not delete Compound: " + feature.getFeatureId(), e);
                                } finally {
                                    updateProgress(0, toRemove.size(), pro.incrementAndGet(), "Deleting...");
                                }
                            }));
                    return true;
                }
            }
        });
    }
}
