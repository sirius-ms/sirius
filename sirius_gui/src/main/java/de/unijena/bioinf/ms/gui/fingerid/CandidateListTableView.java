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
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.table.*;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class CandidateListTableView extends CandidateListView {

    private SortedList<FingerprintCandidateBean> sortedSource;

    @Getter
    private ActionTable<FingerprintCandidateBean> table;

    public CandidateListTableView(final StructureList list) {
        super(list);

        getSource().addActiveResultChangedListener((instanceBean, sre, resultElements, selections) -> {
            try {
                filteredSelectionModel.setValueIsAdjusting(true);
                filteredSelectionModel.clearSelection();
                if (instanceBean == null || (Boolean.FALSE.equals(instanceBean.getComputedTools().isStructureSearch()) && Boolean.FALSE.equals(instanceBean.getComputedTools().isDeNovoSearch())))
                    showCenterCard(ActionList.ViewState.NOT_COMPUTED);
                else if (resultElements.isEmpty())
                    showCenterCard(ActionList.ViewState.EMPTY);
                else
                    showCenterCard(ActionList.ViewState.DATA);
                if (!getSource().getElementListSelectionModel().isSelectionEmpty())
                    filteredSelectionModel.setSelectionInterval(getSource().getElementListSelectionModel().getMinSelectionIndex(), getSource().getElementListSelectionModel().getMaxSelectionIndex());
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).warn("Error when resetting selection for elementList", e);
            } finally {
                filteredSelectionModel.setValueIsAdjusting(false);
            }
        });

        final CandidateTableFormat tf = new CandidateTableFormat(getSource().getBestFunc());
        table = new ActionTable<>(filteredSource, sortedSource, tf);

        table.setSelectionModel(filteredSelectionModel);
        final SiriusResultTableCellRenderer defaultRenderer = new SiriusResultTableCellRenderer(tf.highlightColumnIndex());
        table.setDefaultRenderer(Object.class, defaultRenderer);

        table.getTableHeader().setDefaultRenderer(new CandidateListTableHeaderRenderer());

        table.getColumnModel().getColumn(5).setCellRenderer(new ListStatBarTableCellRenderer<>(tf.highlightColumnIndex(), source.csiScoreStats, false, false, null));
        table.getColumnModel().getColumn(6).setCellRenderer(new BarTableCellRenderer(tf.highlightColumnIndex(), 0f, 1f, true));
        table.getColumnModel().getColumn(10).setCellRenderer(new SiriusResultTableCellRenderer(tf.highlightColumnIndex(), Fonts.FONT_DEJAVU_SANS.deriveFont((float) table.getFont().getSize())));
        table.getColumnModel().getColumn(11).setCellRenderer(new SiriusResultTableCellRenderer(tf.highlightColumnIndex(), Fonts.FONT_DEJAVU_SANS.deriveFont((float) table.getFont().getSize())));
        //todo nightsky: add pubmed link feature!
//        LinkedSiriusTableCellRenderer linkRenderer = new LinkedSiriusTableCellRenderer(defaultRenderer, (LinkedSiriusTableCellRenderer.LinkCreator<PubmedLinks>) s -> s == null ? null : s.getPubmedLink());
//        linkRenderer.registerToTable(table, 6);

        addToCenterCard(ActionList.ViewState.DATA, new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        showCenterCard(ActionList.ViewState.NOT_COMPUTED);
    }

    @Override
    protected FilterList<FingerprintCandidateBean> configureFiltering(EventList<FingerprintCandidateBean> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }
}
