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
import de.unijena.bioinf.ms.gui.subtools.summaries.SummaryConfigPanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class SummarizeAllAction extends AbstractMainFrameAction {

    public SummarizeAllAction(MainFrame mainFrame) {
        super("Summaries", mainFrame);
        putValue(Action.LARGE_ICON_KEY, Icons.EXPORT_32);
        putValue(Action.SMALL_ICON, Icons.EXPORT_16);
        putValue(Action.SHORT_DESCRIPTION, "Write/Export Summary .tsv files.");

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
        run(List.copyOf(MF.getCompounds()), "Write Summaries for whole Project");
    }

    protected void run(@NotNull List<InstanceBean> compounds, String title) {
        ExecutionDialog<SummaryConfigPanel> d = new ExecutionDialog<>(MF.getBackgroundRuns(), new SummaryConfigPanel(Optional.ofNullable(MF.ps().projectSpace().getLocation().getParent()).map(Path::toString).orElse("")), compounds, null, MF, title, true);
        d.setIndeterminateProgress(false);
        d.start();
    }
}
