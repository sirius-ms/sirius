package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.gui.table.ActionListDetailView;
import de.unijena.bioinf.ms.gui.table.ActionTable;
import de.unijena.bioinf.ms.gui.table.BarTableCellRenderer;
import de.unijena.bioinf.ms.gui.table.SiriusResultTableCellRenderer;
import de.unijena.bioinf.projectspace.FormulaResultBean;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class CompoundClassTableView extends ActionListDetailView<ClassyfirePropertyBean, FormulaResultBean, CompoundClassList> {

    protected SortedList<ClassyfirePropertyBean> sortedSource;
    protected ActionTable<ClassyfirePropertyBean> actionTable;
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
        actionTable.getColumnModel().getColumn(7).setCellRenderer(new BarTableCellRenderer(-1,0,1,false));
    }

    @Override
    protected JToolBar getToolBar() {
        final JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        return tb;
    }


    @Override
    protected FilterList<ClassyfirePropertyBean> configureFiltering(EventList<ClassyfirePropertyBean> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

    @Override
    protected EventList<MatcherEditor<ClassyfirePropertyBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new TextMatcher(searchField.textField)
        );
    }

    protected static class TextMatcher extends TextComponentMatcherEditor<ClassyfirePropertyBean> {

        public TextMatcher(JTextComponent textComponent) {
            super(textComponent, (baseList, element) -> {
                baseList.add(element.getMolecularProperty().getName());
                baseList.add(element.getMolecularProperty().getChemontIdentifier());
                baseList.add(element.getMolecularProperty().getParent().getChemontIdentifier());
                baseList.add(element.getMolecularProperty().getDescription());
            });
        }
    }
}
