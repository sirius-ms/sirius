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
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class CandidateListView extends ActionListDetailView<FingerprintCandidateBean, InstanceBean, StructureList> {

//    private FilterRangeSlider<StructureList, FingerprintCandidateBean, InstanceBean> logPSlider;
    private DBFilterPanel dbFilterPanel;
    protected JToggleButton loadAll;
    protected ActionListener loadDataActionListener;



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
        if (!source.hasDenovoStructureCandidates()) south.add(new ExpansiveSearchLabel(source));
        north.add(south, BorderLayout.SOUTH);
        return north;
    }

    @Override
    protected JToolBar getToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        tb.setRollover(true);
//        tb.setLayout(new WrapLayout(FlowLayout.CENTER, 0, 0));

//        logPSlider = new FilterRangeSlider<>(source, source.logPStats);

        dbFilterPanel = new DBFilterPanel(source);
        dbFilterPanel.toggle();

//        tb.add(new NameFilterRangeSlider("XLogP:", logPSlider));
//        tb.addSeparator();
//        tb.addSeparator();


        final JToggleButton filter = new ToolbarToggleButton(Icons.FILTER_DOWN.derive(24,24), "Show database filters");
        filter.addActionListener(e -> {
            if (dbFilterPanel.toggle()) {
                filter.setIcon(Icons.FILTER_UP.derive(24,24));
                filter.setToolTipText("Hide database filters");
            } else {
                filter.setIcon(Icons.FILTER_DOWN.derive(24,24));
                filter.setToolTipText("Show database filters");
            }
        });
        tb.add(filter);
//        tb.addSeparator();

        loadAll = new ToolbarToggleButton(Icons.LOAD_ALL.derive(24,24), "Load all Candidates (Might be many!).");
        loadDataActionListener = e -> source.reloadData(loadAll.isSelected(), true, false);
        loadAll.addActionListener(loadDataActionListener);
        tb.add(firstGap);
        tb.add(secondGap);
        tb.add(loadAll);

        filter.setSelected(false);

        return tb;
    }

    @Override
    protected EventList<MatcherEditor<FingerprintCandidateBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new CandidateStringMatcherEditor(searchField)
//                ,new MinMaxMatcherEditor<>(logPSlider, (baseList, element) ->
//                        element.getXLogPOpt().ifPresentOrElse(baseList::add, () -> baseList.add(null)))
               , new DatabaseFilterMatcherEditor(dbFilterPanel)
        );
    }
}
