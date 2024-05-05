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
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.nightsky.sdk.model.StructureCandidateFormula;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;


public class EpimetheusPanel extends JPanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>EPIMETHEUS - Substructure annotations</b>"
                +"<br>"
                + "CSI:FingerID db search results Epimetheus with substructure annotations from combinatorial fragmentation for all molecular formulas that had been searched."
                + "<br>"
                + "For the selected candidate structure in the upper panel, the bottom panel shows the source spectrum annotated with substructures computed by combinatorial fragmentation (Epimetheus)."
                + "</html>";
    }

    protected final StructureList structureList;
    protected final EpimetheusPanelCandidateListTableView candidateTable;
    public EpimetheusPanel(final StructureList structureList) {
        super(new BorderLayout());
        this.structureList = structureList;
        this.candidateTable = new EpimetheusPanelCandidateListTableView(structureList);
        final SpectraVisualizationPanel overviewSVP = new SpectraVisualizationPanel(SpectraVisualizationPanel.MS2_DISPLAY);

        candidateTable.getFilteredSelectionModel().addListSelectionListener(e -> {
            DefaultEventSelectionModel<FingerprintCandidateBean> selections = (DefaultEventSelectionModel<FingerprintCandidateBean>) e.getSource();
            Optional<FingerprintCandidateBean> sre = selections.getSelected().stream().findFirst();
            structureList.readDataByConsumer(d -> overviewSVP.resultsChanged(d,
                    sre.map(FingerprintCandidateBean::getCandidate).map(StructureCandidateFormula::getFormulaId).orElse(null),
                    sre.map(FingerprintCandidateBean::getSmiles).orElse(null)));
        });

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

    protected class EpimetheusPanelCandidateListTableView extends CandidateListTableView {
        JCheckBox showMSNovelist;

        public EpimetheusPanelCandidateListTableView(StructureList list) {
            super(list);
        }

        @Override
        protected JToolBar getToolBar() {
            JToolBar tb = super.getToolBar();
            JCheckBox showMSNovelist = new JCheckBox("Include de novo structures", true);
            tb.add(showMSNovelist, 0);

            showMSNovelist.addActionListener(e -> structureList.reloadData(loadAll.isSelected(), true, showMSNovelist.isSelected()));
            structureList.reloadData(loadAll.isSelected(), true, showMSNovelist.isSelected());

            return tb;
        }
    }
}