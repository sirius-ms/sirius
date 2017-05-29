package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionListDetailView;
import de.unijena.bioinf.sirius.gui.table.ActionTable;

import javax.swing.*;
import java.awt.*;

public class FingerprintTableView extends ActionListDetailView<MolecularPropertyTableEntry, SiriusResultElement, FingerprintTable> {

    protected SortedList<MolecularPropertyTableEntry> sortedSource;
    protected ActionTable<MolecularPropertyTableEntry> actionTable;
    protected FingerprintTableFormat format;

    public FingerprintTableView(FingerprintTable table) {
        super(table);
        this.format = new FingerprintTableFormat(table);
        this.actionTable = new ActionTable<MolecularPropertyTableEntry>(filteredSource, sortedSource, format);

        TableComparatorChooser.install(actionTable, sortedSource, AbstractTableComparatorChooser.SINGLE_COLUMN);
        actionTable.setSelectionModel(filteredSelectionModel);

        this.add(
                new JScrollPane(actionTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );

        // set small width for ID column
        actionTable.getColumnModel().getColumn(0).setMaxWidth(50);


    }

    @Override
    protected FilterList<MolecularPropertyTableEntry> configureFiltering(EventList<MolecularPropertyTableEntry> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

    @Override
    protected JToolBar getToolBar() {
        return new JToolBar();
    }

    @Override
    protected EventList<MatcherEditor<MolecularPropertyTableEntry>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf();
    }
}
