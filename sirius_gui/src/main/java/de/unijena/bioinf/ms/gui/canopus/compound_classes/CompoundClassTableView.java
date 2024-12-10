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

package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.gui.table.ActionListDetailView;
import de.unijena.bioinf.ms.gui.table.ActionTable;
import de.unijena.bioinf.ms.gui.table.BarTableCellRenderer;
import de.unijena.bioinf.ms.gui.table.SiriusResultTableCellRenderer;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;
import de.unijena.bioinf.projectspace.FormulaResultBean;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class CompoundClassTableView extends ActionListDetailView<CompoundClassBean, FormulaResultBean, CompoundClassList> {

    protected SortedList<CompoundClassBean> sortedSource;
    protected ActionTable<CompoundClassBean> actionTable;
    protected CompoundClassTableFormat format;


    public CompoundClassTableView(CompoundClassList sourceList) {
        super(sourceList, true);
        this.format = new CompoundClassTableFormat();

        this.actionTable = new ActionTable<>(filteredSource, sortedSource, format);
        TableComparatorChooser.install(actionTable, sortedSource, AbstractTableComparatorChooser.SINGLE_COLUMN);
        actionTable.setSelectionModel(getFilteredSelectionModel());
        actionTable.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer(-1));
        this.add(
                new JScrollPane(actionTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );

        // set small width for index column
        actionTable.getColumnModel().getColumn(0).setMaxWidth(50);
        // display color bar for posterior probability
        actionTable.getColumnModel().getColumn(2).setCellRenderer(new BarTableCellRenderer(-1, 0, 1, true));
        // display color bar for f1 score
//        actionTable.getColumnModel().getColumn(7).setCellRenderer(new BarTableCellRenderer(-1,0,1,false));
    }

    @Override
    protected JToolBar getToolBar() {
        final JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        tb.setLayout(new WrapLayout(FlowLayout.LEFT, 0, 0));
        return tb;
    }


    @Override
    protected FilterList<CompoundClassBean> configureFiltering(EventList<CompoundClassBean> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

    @Override
    protected EventList<MatcherEditor<CompoundClassBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new TextMatcher(searchField)
        );
    }

    protected static class TextMatcher extends TextComponentMatcherEditor<CompoundClassBean> {

        public TextMatcher(JTextComponent textComponent) {
            super(textComponent, (baseList, element) -> {
                baseList.add(element.getSourceClass().getName());
                baseList.add(element.getChemontIdentifier());
                if (!Utils.isNullOrBlank(element.getParentName()))
                    baseList.add(element.getParentName());
                baseList.add(element.getSourceClass().getDescription());
            });
        }
    }
}
