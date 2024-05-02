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

package de.unijena.bioinf.ms.gui.fingerid;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.MatcherEditor;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.CandidateStringMatcherEditor;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.DatabaseFilterMatcherEditor;
import de.unijena.bioinf.ms.gui.table.ActionListDetailView;
import de.unijena.bioinf.ms.gui.table.FilterRangeSlider;
import de.unijena.bioinf.ms.gui.table.MinMaxMatcherEditor;
import de.unijena.bioinf.ms.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;

public class CandidateListView extends ActionListDetailView<FingerprintCandidateBean, InstanceBean, StructureList> {

//    private FilterRangeSlider<StructureList, FingerprintCandidateBean, InstanceBean> logPSlider;
    private DBFilterPanel dbFilterPanel;


    public CandidateListView(StructureList source) {
        super(source);
    }


    @Override
    protected JPanel getNorth() {
        final JPanel north = super.getNorth();
        north.add(dbFilterPanel, BorderLayout.CENTER);
        final JPanel south = new JPanel();
        final BoxLayout southLayout = new BoxLayout(south, BoxLayout.PAGE_AXIS);
        south.setLayout(southLayout);
        south.add(new LipidLabel(source));
        if (!source.isDenovoStructureCandidates()) south.add(new ExpansiveSearchLabel(source));
        north.add(south, BorderLayout.SOUTH);
        return north;
    }

    @Override
    protected JToolBar getToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        tb.setLayout(new WrapLayout(FlowLayout.LEFT, 0, 0));

//        logPSlider = new FilterRangeSlider<>(source, source.logPStats);

        dbFilterPanel = new DBFilterPanel(source);
        dbFilterPanel.toggle();

//        tb.add(new NameFilterRangeSlider("XLogP:", logPSlider));
//        tb.addSeparator();
        tb.addSeparator();


        final JToggleButton filter = new ToolbarToggleButton(Icons.FILTER_DOWN_24, "show filter");
        filter.addActionListener(e -> {
            if (dbFilterPanel.toggle()) {
                filter.setIcon(Icons.FILTER_UP_24);
                filter.setToolTipText("Hide source filters");
            } else {
                filter.setIcon(Icons.FILTER_DOWN_24);
                filter.setToolTipText("Show source filters");
            }
        });
        tb.add(filter);
        tb.addSeparator();

        final JToggleButton loadAll = new ToolbarToggleButton(Icons.LOAD_ALL_24, "Load all Candidates (Might be many!).");
        loadAll.addActionListener(e -> source.reloadData(loadAll.isSelected()));
        tb.add(loadAll);

        filter.doClick();

        return tb;
    }

    @Override
    protected EventList<MatcherEditor<FingerprintCandidateBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new CandidateStringMatcherEditor(searchField.textField)
//                ,new MinMaxMatcherEditor<>(logPSlider, (baseList, element) ->
//                        element.getXLogPOpt().ifPresentOrElse(baseList::add, () -> baseList.add(null)))
               , new DatabaseFilterMatcherEditor(dbFilterPanel)
        );
    }
}
