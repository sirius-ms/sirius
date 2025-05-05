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

package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.table.*;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.ms.gui.utils.NameFilterRangeSlider;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class FingerprintTableView extends ActionListDetailView<FingerIdPropertyBean, FormulaResultBean, FingerprintList> {

    protected SortedList<FingerIdPropertyBean> sortedSource;
    protected ActionTable<FingerIdPropertyBean> actionTable;
    protected FingerprintTableFormat format;
    protected int maxAtomSize;

    protected FilterRangeSlider<FingerprintList, FingerIdPropertyBean, FormulaResultBean> probabilitySlider, atomSizeSlider;

    private DoubleListStats __atomsizestats__;

    public FingerprintTableView(FingerprintList table) {
        super(table,true);
        this.format = new FingerprintTableFormat(table);
        this.maxAtomSize = 5;
        for (FingerprintVisualization v : table.visualizations)
            if (v != null)
                this.maxAtomSize = Math.max(this.maxAtomSize, v.numberOfMatchesAtoms);
        __atomsizestats__.addValue(this.maxAtomSize);
        this.actionTable = new ActionTable<>(filteredSource, sortedSource, format);
        TableComparatorChooser.install(actionTable, sortedSource, AbstractTableComparatorChooser.SINGLE_COLUMN);
        actionTable.setSelectionModel(getFilteredSelectionModel());
        actionTable.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer(-1));
        JScrollPane scrollPane = new JScrollPane(actionTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Fingerprint_Predictions);
        this.add(
                scrollPane,
                BorderLayout.CENTER
        );

        // set small width for ID column
        actionTable.getColumnModel().getColumn(0).setMaxWidth(50);
        // write "undefined" for undefined atom size
        actionTable.getColumnModel().getColumn(3).setCellRenderer(new SiriusResultTableCellRenderer(-1){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String newValue;
                if (value instanceof Integer) {
                    if (((Integer) value).intValue() <= 0) newValue = "undefined";
                    else newValue = value.toString();
                } else {
                    newValue = value.toString();
                }
                return super.getTableCellRendererComponent(table, newValue, isSelected, hasFocus, row, column);
            }
        });
        // display color bar for posterior probability
        BarTableCellRenderer barRenderer = BarTableCellRenderer.newProbabilityBar(-1);
        barRenderer.setTwoWayBar(true);
        actionTable.getColumnModel().getColumn(2).setCellRenderer(barRenderer);
        actionTable.getColumnModel().getColumn(6).setCellRenderer(BarTableCellRenderer.newProbabilityBar(-1));
    }



    public void addSelectionListener(ListSelectionListener listener) {
        getFilteredSelectionModel().addListSelectionListener(listener);
    }

    @Override
    protected FilterList<FingerIdPropertyBean> configureFiltering(EventList<FingerIdPropertyBean> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

    @Override
    protected JToolBar getToolBar() {
        __atomsizestats__ = new DoubleListStats(new double[]{0,5});
        JToolBar tb =  new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        tb.setLayout(new WrapLayout(FlowLayout.LEFT, 0, 0));
        final DoubleListStats stats1 = new DoubleListStats(new double[]{0,1});
        probabilitySlider = new FilterRangeSlider<>(source,stats1);
        atomSizeSlider = new FilterRangeSlider<>(source, __atomsizestats__,false, FilterRangeSlider.DEFAUTL_INT_FORMAT);


        tb.add(new NameFilterRangeSlider("Probability:", probabilitySlider));
        tb.addSeparator();
        tb.add(new NameFilterRangeSlider("Number of heavy atoms:", atomSizeSlider));
        tb.addSeparator();
        tb.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Fingerprint_Filter);
        return tb;
    }

    @Override
    protected EventList<MatcherEditor<FingerIdPropertyBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new TextMatcher(searchField),
                new MinMaxMatcherEditor<>(probabilitySlider, (baseList, element) -> baseList.add(element.getProbability())),
                new MinMaxMatcherEditor<>(atomSizeSlider, (baseList, element) -> baseList.add((double) element.getMatchSize()))
        );
    }

    protected static class TextMatcher extends TextComponentMatcherEditor<FingerIdPropertyBean> {

        public TextMatcher(JTextComponent textComponent) {
            super(textComponent, (baseList, element) -> {
                baseList.add(element.getFingerprintTypeName());
                baseList.add(element.getMolecularProperty().getDescription());
            });
        }
    }
}
