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
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeAction extends AbstractGuiAction {

    public ComputeAction(SiriusGui gui) {
        super("Compute", gui);
        putValue(Action.SMALL_ICON, Icons.RUN.derive(16,16));
        putValue(Action.LARGE_ICON_KEY, Icons.RUN.derive(32,32));
        putValue(Action.SHORT_DESCRIPTION, "Compute selected compound(s)");

        setEnabled(SiriusActions.notComputingOrEmptySelected(this.mainFrame.getCompoundListSelectionModel()));

        this.mainFrame.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection, int fullSize) {}

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection, List<InstanceBean> selected, List<InstanceBean> deselected, int fullSize) {
                setEnabled(SiriusActions.notComputingOrEmpty(selected));
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!mainFrame.getCompoundListSelectionModel().isSelectionEmpty()) {
            List<InstanceBean> l = List.copyOf(mainFrame.getCompoundListSelectionModel().getSelected());
            if (l.stream().noneMatch(InstanceBean::isComputing))
                new BatchComputeDialog(gui, l);
        }
    }
}