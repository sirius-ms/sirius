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
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.CloseDialogNoSaveReturnValue;
import de.unijena.bioinf.ms.gui.dialogs.CloseDialogReturnValue;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class DeleteExperimentAction extends AbstractMainFrameAction {
    public static final String NEVER_ASK_AGAIN_KEY = PropertyManager.PROPERTY_BASE + ".sirius.dialog.delete_experiment_action.ask_again";
    private final List<InstanceBean> toRemove;

    public DeleteExperimentAction(List<InstanceBean> toRemove, MainFrame mainFrame) {
        super(mainFrame);
        this.toRemove = toRemove;
    }

    public DeleteExperimentAction(MainFrame mainFrame) {
        super("Delete", mainFrame);
        toRemove = null;
        putValue(Action.SMALL_ICON, Icons.REMOVE_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "Delete the selected data");


        setEnabled(SiriusActions.notComputingOrEmptySelected(this.MF.getCompoundListSelectionModel()));

        this.MF.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
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
        deleteCompounds();
    }

    public void deleteCompounds() {
        if (!PropertyManager.getBoolean(NEVER_ASK_AGAIN_KEY, false)) {
            CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(MF, "When removing the selected compound(s) you will loose all computed identification results?", NEVER_ASK_AGAIN_KEY);
            CloseDialogReturnValue val = diag.getReturnValue();
            if (val == CloseDialogReturnValue.abort) return;
        }

        //use provided list or remove selected.
        List<InstanceBean> toRemove = this.toRemove != null ? new ArrayList<>(this.toRemove) : new ArrayList<>(MF.getCompoundList().getCompoundListSelectionModel().getSelected());
        MF.getCompoundList().getCompoundListSelectionModel().clearSelection();
        MF.ps().deleteCompounds(toRemove, MF);
    }
}
