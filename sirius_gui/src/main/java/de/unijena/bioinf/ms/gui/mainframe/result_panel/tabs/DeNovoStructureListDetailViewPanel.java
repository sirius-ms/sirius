

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
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.fingerid.CandidateListDetailView;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;

import javax.swing.*;
import java.awt.*;

public class DeNovoStructureListDetailViewPanel extends JPanel implements PanelDescription, Loadable {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>MSNovelist - De Novo Structure Generation</b>"
                +"<br>"
                + "De Novo structure generation results for all molecular formulas with a predicted fingerprint. Generated structure candidates have been ranked by CSI:FingerID scoring."
                + "<br>"
                + "For each candidate structure all present molecular properties are represented by squares."
                + "<br>"
                + "Click a square to highlight the molecular property in the structure."
                + "</html>";
    }

    protected CandidateListDetailView list;

    public DeNovoStructureListDetailViewPanel(ResultPanel resultPanel, StructureList sourceList, SiriusGui gui) {
        super(new BorderLayout());
        list = new DeNovoCandidateListDetailView(resultPanel, sourceList, gui);
        add(list, BorderLayout.CENTER);
    }

    protected class DeNovoCandidateListDetailView extends CandidateListDetailView {
        public DeNovoCandidateListDetailView(ResultPanel resultPanel, StructureList sourceList, SiriusGui gui) {
            super(resultPanel, sourceList, gui, true);
        }

        @Override
        protected JToolBar getToolBar() {
            JToolBar tb = super.getToolBar();
            ToolbarToggleButton showDatabaseHits = new ToolbarToggleButton(null, Icons.DB_LENS.derive(24,24), "Show CSI:FingerID structure database hits together with MsNovelist de novo structure candidates.");
            showDatabaseHits.setSelected(true);
            tb.add(showDatabaseHits, getIndexOfSecondGap(tb) + 1);

            loadAll.removeActionListener(loadDataActionListener);
            loadDataActionListener = e -> source.reloadData(loadAll.isSelected(), showDatabaseHits.isSelected(), true);
            showDatabaseHits.addActionListener(loadDataActionListener);
            loadAll.addActionListener(loadDataActionListener);

            return tb;
        }
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        if (loading)
            list.showCenterCard(ActionList.ViewState.LOADING);
        else
            list.showCenterCard(ActionList.ViewState.DATA);
        return loading;
    }
}
