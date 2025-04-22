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

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.fingerid.CandidateListTableView;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;


public class EpimetheusPanel extends JPanel implements Loadable, PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>EPIMETHEUS - Substructure annotations</b>"
                +"<br>"
                + "Structure search results annotated with substructures from combinatorial fragmentation for all molecular formulas that had been searched."
                + "<br>"
                + "For the selected candidate structure in the upper panel, the bottom panel shows the source spectrum annotated with substructures computed by combinatorial fragmentation (Epimetheus)."
                + "</html>";
    }

    protected final StructureList structureList;
    protected final EpimetheusPanelCandidateListTableView candidateTable;
    private final LoadablePanel loadablePanel;

    public EpimetheusPanel(final StructureList structureList) {
        super(new BorderLayout());
        this.structureList = structureList;
        this.candidateTable = new EpimetheusPanelCandidateListTableView(structureList);

        SubstructurePanel substructurePanel = new SubstructurePanel(structureList.getGui());
        candidateTable.getFilteredSelectionModel().addListSelectionListener(substructurePanel);

        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, candidateTable, substructurePanel);
        major.setDividerLocation(250);
        loadablePanel = new LoadablePanel(major);
        add(loadablePanel, BorderLayout.CENTER);
        structureList.addActiveResultChangedListener((elementsParent, selectedElement, resultElements, selections) -> disableLoading());
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return loadablePanel.setLoading(loading, absolute);
    }

    public CandidateListTableView getCandidateTable() {
        return candidateTable;
    }

    protected class EpimetheusPanelCandidateListTableView extends CandidateListTableView {

        public EpimetheusPanelCandidateListTableView(StructureList list) {
            super(list);
            centerCardPanel.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Epimetheus_List);
        }

        @Override
        protected JToolBar getToolBar() {
            JToolBar tb = super.getToolBar();
            ToolbarToggleButton showMSNovelist = new ToolbarToggleButton(null, Icons.DENOVO.derive(24,24), "Show MSNovelist de novo structure candidates together with CSI:FingerID structure database hits.");
            showMSNovelist.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Epimetheus_DeNovoFilter);
            showMSNovelist.setSelected(true);
            tb.add(showMSNovelist, getIndexOfSecondGap(tb) + 1);

            loadAll.removeActionListener(loadDataActionListener);
            loadDataActionListener = e -> structureList.reloadData(loadAll.isSelected(), true, showMSNovelist.isSelected());
            showMSNovelist.addActionListener(loadDataActionListener);
            loadAll.addActionListener(loadDataActionListener);

            return tb;
        }
    }

    public void initSoftwareTour(GuiProperties guiProperties) {
        SoftwareTourUtils.checkAndInitTour(this, SoftwareTourInfoStore.EpimetheusTabTourKey, guiProperties);
    }
}