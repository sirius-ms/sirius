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

package de.unijena.bioinf.ms.gui.fingerid;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.ms.gui.table.*;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateListTableView extends CandidateListView implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    private final ActionTable<FingerprintCandidateBean> table;
    private SortedList<FingerprintCandidateBean> sortedSource;

    public CandidateListTableView(final StructureList list) {
        super(list);

        final CandidateTableFormat tf = new CandidateTableFormat(getSource().getBestFunc());
        this.table = new ActionTable<>(filteredSource, sortedSource, tf);

        table.setSelectionModel(filteredSelectionModel);
        final SiriusResultTableCellRenderer defaultRenderer = new SiriusResultTableCellRenderer(tf.highlightColumnIndex());
        table.setDefaultRenderer(Object.class, defaultRenderer);

        table.getColumnModel().getColumn(5).setCellRenderer(new ListStatBarTableCellRenderer<>(tf.highlightColumnIndex(), source.csiScoreStats, false, false, null));
        table.getColumnModel().getColumn(6).setCellRenderer(new BarTableCellRenderer(tf.highlightColumnIndex(), 0f, 1f, true));
        LinkedSiriusTableCellRenderer linkRenderer = new LinkedSiriusTableCellRenderer(defaultRenderer, (LinkedSiriusTableCellRenderer.LinkCreator<PubmedLinks>) s -> s == null ? null : s.getPubmedLink());
        linkRenderer.registerToTable(table, 7);
        this.add(
                new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );
    }

    @Override
    protected FilterList<FingerprintCandidateBean> configureFiltering(EventList<FingerprintCandidateBean> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        System.out.println("CandidateListTableView Changed!");
        //not used
    }
}
