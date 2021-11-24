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

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.fingerid.CandidateListTableView;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fleisch on 15.05.17.
 */
public class EpimetheusPanel extends JPanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>EPIMETHEUS - Sub-stucture annotations</b>"
                +"<br>"
                + "CSI:FingerID db search results with Epimetheus sub-structure annotations for all molecular formulas that had been searched."
                + "<br>"
                + "For the selected candidate structure, the bottom panel shows the spectrum with Epimetheus substructure annotations."/* and corresponding fragmentation tree*/
                + "</html>";
    }

    protected final StructureList structureList;
    protected final CandidateListTableView candidateTable;
    public EpimetheusPanel(final StructureList structureList) {
        super(new BorderLayout());
        this.structureList = structureList;
        this.candidateTable = new CandidateListTableView(structureList);
//        final TreeVisualizationPanel overviewTVP = new TreeVisualizationPanel();
        final SpectraVisualizationPanel overviewSVP = new SpectraVisualizationPanel(SpectraVisualizationPanel.MS2_DISPLAY, false);
        this.structureList.getTopLevelSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Class to synchronize selected peak/node
//        VisualizationPanelSynchronizer synchronizer = new VisualizationPanelSynchronizer(overviewTVP, overviewSVP);
 
//
        candidateTable.getFilteredSelectionModel().addListSelectionListener(e -> {
            DefaultEventSelectionModel<FingerprintCandidateBean> selections = (DefaultEventSelectionModel<FingerprintCandidateBean>) e.getSource();
            FingerprintCandidateBean sre = selections.getSelected().stream().findFirst().orElse(null);
            FormulaResultBean form = sre != null ? sre.getFormulaResult() : null;
            InstanceBean inst = form != null ? form.getInstance() : null;
//            overviewTVP.resultsChanged(inst, form, null, null);
            overviewSVP.resultsChanged(inst, form, sre != null ? sre.getFingerprintCandidate() : null);
        });

//        JSplitPane south = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, overviewSVP, overviewTVP);
//        south.setDividerLocation(.5d);
//        south.setResizeWeight(.5d);
        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, candidateTable, overviewSVP);
        major.setDividerLocation(250);
        add(major, BorderLayout.CENTER);
    }

    public StructureList getStructureList() {
        return structureList;
    }

    public CandidateListTableView getCandidateTable() {
        return candidateTable;
    }
}