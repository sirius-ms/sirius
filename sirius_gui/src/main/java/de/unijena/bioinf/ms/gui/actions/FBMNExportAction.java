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
import de.unijena.bioinf.ms.gui.dialogs.ExecutionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.subtools.export.mgf.MgfExporterConfigPanel;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;


public class FBMNExportAction extends AbstractMainFrameAction {

    public FBMNExportAction(MainFrame mainFrame) {
        super("FBMN Export", mainFrame);
        putValue(Action.LARGE_ICON_KEY, Icons.FBMN_32);
        putValue(Action.SHORT_DESCRIPTION, "Export project for GNPS Feature Based Molecular Networking.");
        initListeners();
    }

    protected void initListeners(){
        setEnabled(SiriusActions.notComputingOrEmpty(MF.getCompoundList().getCompoundList()));

        MF.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
                setEnabled(SiriusActions.notComputingOrEmpty(event.getSourceList()));
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {}
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Path p = MF.ps().projectSpace().getLocation();
        new ExecutionDialog<>(MF.getBackgroundRuns(), new MgfExporterConfigPanel(p.getParent().toString(), p.getFileName().toString()), List.copyOf(MF.getCompounds()), null, MF, "Export Project for GNPS FBMN", true).start();
    }
}
