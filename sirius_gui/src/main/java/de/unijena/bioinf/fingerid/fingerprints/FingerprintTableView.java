package de.unijena.bioinf.fingerid.fingerprints;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.*;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.sirius.gui.utils.NameFilterRangeSlider;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class FingerprintTableView extends ActionListDetailView<MolecularPropertyTableEntry, SiriusResultElement, FingerprintTable> {

    protected SortedList<MolecularPropertyTableEntry> sortedSource;
    protected ActionTable<MolecularPropertyTableEntry> actionTable;
    protected FingerprintTableFormat format;
    protected int maxAtomSize;

    protected FilterRangeSlider probabilitySlider, atomSizeSlider;

    private DoubleListStats __atomsizestats__;

    public FingerprintTableView(FingerprintTable table) {
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
        this.add(
                new JScrollPane(actionTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );

        // set small width for ID column
        actionTable.getColumnModel().getColumn(0).setMaxWidth(50);

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


    }



    public void addSelectionListener(ListSelectionListener listener) {
        getFilteredSelectionModel().addListSelectionListener(listener);
    }

    @Override
    protected FilterList<MolecularPropertyTableEntry> configureFiltering(EventList<MolecularPropertyTableEntry> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

    @Override
    protected JToolBar getToolBar() {
        __atomsizestats__ = new DoubleListStats(new double[]{0,5});
        JToolBar tb =  new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        final DoubleListStats stats1 = new DoubleListStats(new double[]{0,1});
        probabilitySlider = new FilterRangeSlider<>(source,stats1);
        atomSizeSlider = new FilterRangeSlider<>(source, __atomsizestats__,false, FilterRangeSlider.DEFAUTL_INT_FORMAT);


        tb.add(new NameFilterRangeSlider("Probability:", probabilitySlider));
        tb.addSeparator();
        tb.add(new NameFilterRangeSlider("Number of heavy atoms:", atomSizeSlider));
        tb.addSeparator();
        return tb;
    }

    @Override
    protected EventList<MatcherEditor<MolecularPropertyTableEntry>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                (MatcherEditor<MolecularPropertyTableEntry>) new TextMatcher(searchField.textField),
                new MinMaxMatcherEditor<>(probabilitySlider, new Filterator<Double, MolecularPropertyTableEntry>() {
                    @Override
                    public void getFilterValues(java.util.List<Double> baseList, MolecularPropertyTableEntry element) {
                        baseList.add(element.getProbability());
                    }
                }),
                new MinMaxMatcherEditor<>(atomSizeSlider, new Filterator<Double, MolecularPropertyTableEntry>() {
                    @Override
                    public void getFilterValues(java.util.List<Double> baseList, MolecularPropertyTableEntry element) {
                        baseList.add((double)element.getMatchSize());
                    }
                })
        );
    }

    protected static class TextMatcher extends TextComponentMatcherEditor<MolecularPropertyTableEntry> {

        public TextMatcher(JTextComponent textComponent) {
            super(textComponent, new TextFilterator<MolecularPropertyTableEntry>() {
                @Override
                public void getFilterStrings(java.util.List<String> baseList, MolecularPropertyTableEntry element) {
                    baseList.add(element.getFingerprintTypeName());
                    baseList.add(element.getMolecularProperty().getDescription());
                }
            });
        }
    }
}
