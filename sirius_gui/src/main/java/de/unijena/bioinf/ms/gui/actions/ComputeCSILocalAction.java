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
 * 30.01.17.
 */

import ca.odell.glazedlists.EventList;
import de.unijena.bioinf.ms.gui.compute.FingerIdDialog;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.projectspace.InstanceBean;

import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeCSILocalAction extends ComputeCSIAction {

    public ComputeCSILocalAction() {
        super();
        putValue(LARGE_ICON_KEY, Icons.FINGER_64);
        putValue(SHORT_DESCRIPTION, "Search molecular formulas with CSI:FingerID");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (CheckConnectionAction.isNotConnectedAndLoad())
            return;
        
        //calculate csi
        final FingerIdDialog dialog = new FingerIdDialog(MF, CheckConnectionAction.isConnectedAndLoad(),  true);
        final int returnState = dialog.run();

        if (returnState != FingerIdDialog.CANCELED) {
            InstanceBean ec = null;
            EventList<InstanceBean> l = MF.getCompoundListSelectionModel().getSelected();
            if (l != null && !l.isEmpty()) {
                ec = l.get(0);
            }

            if (returnState == FingerIdDialog.COMPUTE_ALL) {
                System.out.println("Compute ALL local CSI not implemented yet!");

//                MF.getCsiFingerId().compute(ec, dialog.getSearchDb());
            } else {
                System.out.println("Compute local CSI not implemented yet!");

//                java.util.List<FormulaResultBean> selected = MF.getFormulaList().getSelectedValues();
//                java.util.List<FingerIdTask> tasks = new ArrayList<>(selected.size());
                /*for (FormulaResultBean element : selected) {
                    if (element.getCharge() > 0 || element.getResult().getResolvedTree().numberOfEdges() > 0)
                        tasks.add(new FingerIdTask(dialog.getSearchDb(), ec, element));
                }
                MF.getCsiFingerId().computeAll(tasks);*/
            }
        }
    }
}
