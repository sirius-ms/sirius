

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

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.CandidateListDetailView;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourUtils;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class CandidateListDetailViewPanel extends JPanel implements PanelDescription, Loadable {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>CSI:FingerID - Structure Database Search</b>"
                +"<br>"
                + "Structure DB search results for all selected molecular formulas with a predicted fingerprint that have been searched."
                + "<br>"
                + "For each candidate structure all present molecular properties are represented by squares."
                + "<br>"
                + "Click a square to highlight the molecular property in the structure."
                + "</html>";
    }

    protected CandidateListDetailView list;

    public CandidateListDetailViewPanel(ResultPanel resultPanel, StructureList sourceList, SiriusGui gui) {
        super(new BorderLayout());
        Function<InstanceBean, Boolean> wasComputed = instanceBean -> instanceBean.getComputedTools().isStructureSearch();
        list = new CandidateListDetailView(resultPanel, sourceList, gui, wasComputed);
        add(list, BorderLayout.CENTER);

        list.getSource().addActiveResultChangedListener((instanceBean, sre, resultElements, selections) -> {
            if (instanceBean != null && wasComputed.apply(instanceBean) && !resultElements.isEmpty()) {
                Jobs.runEDTLater(() -> initSoftwareTour(gui.getProperties()));
            }
        });
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
       if (loading)
           list.showCenterCard(ActionList.ViewState.LOADING);
       else
           list.showCenterCard(ActionList.ViewState.DATA);
       return loading;
    }

    public void initSoftwareTour(GuiProperties guiProperties) {
        SoftwareTourUtils.checkAndInitTour(this, SoftwareTourInfoStore.DatabaseSearchTabTourName, SoftwareTourInfoStore.DatabaseSearchTabTourKey, guiProperties);
    }
}
