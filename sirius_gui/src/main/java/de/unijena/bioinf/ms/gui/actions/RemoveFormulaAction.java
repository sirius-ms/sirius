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

package de.unijena.bioinf.ms.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class RemoveFormulaAction extends AbstractAction {

    public RemoveFormulaAction() {
        super("Remove Formula");
        putValue(Action.SMALL_ICON, Icons.REMOVE_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "Remove the preset molecular formula from the selected data.");

        setEnabled(SiriusActions.notComputingOrEmptyFirst(MF.getCompoundListSelectionModel()));

        MF.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
                setEnabled(SiriusActions.notComputingOrEmptyFirst(selection));
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Jobs.runInBackgroundAndLoad(MF, "Removing Formulas...", new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() throws Exception {
                int progress = 0;
                updateProgress(0, 100, progress++, "Loading Compounds...");
                final List<InstanceBean> toModify = new ArrayList<>(MF.getCompoundList().getCompoundListSelectionModel().getSelected());
                updateProgress(0, toModify.size(), progress++, "Removing " + (progress-1) + "/" + toModify.size() );
                for (InstanceBean instance : toModify) {
                    instance.set().setMolecularFormula(null).apply();
                    updateProgress(0, toModify.size(), progress++, "Removing " + (progress-1) + "/" + toModify.size() );
                }
                return null;
            }
        });
    }
}
