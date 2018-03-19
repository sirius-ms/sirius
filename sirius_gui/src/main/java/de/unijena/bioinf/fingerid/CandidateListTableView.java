package de.unijena.bioinf.fingerid;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateListTableView extends CandidateListView implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {

    private final ActionTable<CompoundCandidate> table;
    private SortedList<CompoundCandidate> sortedSource;

    public CandidateListTableView(final CandidateList list) {
        super(list);


        final CandidateTableFormat tf = new CandidateTableFormat(source.scoreStats);
        this.table = new ActionTable<>(filteredSource, sortedSource, tf);

        table.setSelectionModel(filteredSelectionModel);
        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer(tf.highlightColumnIndex()));

        table.getColumnModel().getColumn(5).setCellRenderer(new ListStatBarTableCellRenderer(tf.highlightColumnIndex(), source.scoreStats, false, false, null));
        table.getColumnModel().getColumn(6).setCellRenderer(new BarTableCellRenderer(tf.highlightColumnIndex(), 0f, 1f, true));


        this.add(
                new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );


    }

    @Override
    protected FilterList<CompoundCandidate> configureFiltering(EventList<CompoundCandidate> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {
        //not used
    }
}
