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
import ca.odell.glazedlists.swing.AdvancedListSelectionModel;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.CloseDialogNoSaveReturnValue;
import de.unijena.bioinf.ms.gui.dialogs.CloseDialogReturnValue;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Markus Fleischauer
 */
@Slf4j
public class DeleteExperimentAction extends AbstractGuiAction {
    public static final String NEVER_ASK_AGAIN_KEY = PropertyManager.PROPERTY_BASE + ".sirius.dialog.delete_experiment_action.ask_again";

    public DeleteExperimentAction(SiriusGui gui) {
        super("Delete", gui);
        putValue(Action.SMALL_ICON, Icons.REMOVE_DOC.derive(16, 16));
        putValue(Action.SHORT_DESCRIPTION, "Delete the selected data");


        setEnabled(SiriusActions.notComputingOrEmptySelected(this.mainFrame.getCompoundListSelectionModel()));

        this.mainFrame.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, AdvancedListSelectionModel<InstanceBean> selection, long totalElements) {
                setEnabled(SiriusActions.notComputingOrEmptySelected(selection));
            }

            @Override
            public void computeStateChanged(AdvancedListSelectionModel<InstanceBean> selection, long totalElements) {
                // one signal for a whole batch of features, so re-ask the same question the selection asks
                setEnabled(SiriusActions.notComputingOrEmptySelected(selection));
            }

            @Override
            public void listSelectionChanged(AdvancedListSelectionModel<InstanceBean> selection, List<InstanceBean> selected, List<InstanceBean> deselected, long totalElements) {
                setEnabled(SiriusActions.notComputingOrEmpty(selected));
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!PropertyManager.getBoolean(NEVER_ASK_AGAIN_KEY, false)) {
            CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(mainFrame, "When removing the selected feature(s) you will loose all computed results?", NEVER_ASK_AGAIN_KEY);
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

        Jobs.runInBackgroundAndLoad(mainFrame, "Deleting Data...", true, new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {
                // Skip features that are currently computing (can't be deleted), mirroring the previous behaviour.
                List<String> idsToDelete = new ArrayList<>(toRemove.size());
                for (InstanceBean feature : toRemove) {
                    if (feature.isComputing())
                        log.warn("Cannot delete '{}' because it is currently computing. Skipping!", feature.getFeatureId());
                    else
                        idsToDelete.add(feature.getFeatureId());
                }
                // One bulk server call; the manager suppresses the FEATURE_DELETED storm and rebuilds the list
                // once, authoritatively, afterwards (and reconciles list + counters even on failure/cancel).
                gui.getProjectManager().deleteAlignedFeaturesByIds(idsToDelete);
                return true;
            }
        });

        if (toRemove.size() >= 100 && JOptionPane.showConfirmDialog(mainFrame, "Compact project to reduce the project's file size? (may take some time)", null, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            gui.getProjectManager().compactWithLoading(mainFrame);
        }
    }
}
