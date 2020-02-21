package de.unijena.bioinf.ms.gui.table;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.utils.SearchTextField;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ActionListDetailView<E extends SiriusPCS, D, T extends ActionList<E, D>> extends ActionListView<T> {
    protected final SearchTextField searchField;
    protected final FilterList<E> filteredSource;
    protected final DefaultEventSelectionModel<E> filteredSelectionModel;
    protected final JToolBar toolBar;

    public ActionListDetailView(T source){
        this(source,false);
    }
    public ActionListDetailView(T source, final boolean singleSelection) {
        super(source);
        setLayout(new BorderLayout());
        searchField = new SearchTextField();
        this.toolBar = getToolBar();
        filteredSource = configureFiltering(source.elementList);
        filteredSelectionModel = new DefaultEventSelectionModel<>(filteredSource);
        if (singleSelection)
            filteredSelectionModel.setSelectionMode(DefaultEventSelectionModel.SINGLE_SELECTION);
        add(getNorth(), BorderLayout.NORTH);

    }

    protected abstract JToolBar getToolBar();

    protected abstract EventList<MatcherEditor<E>> getSearchFieldMatchers();

    public FilterList<E> getFilteredSource() {
        return filteredSource;
    }

    public DefaultEventSelectionModel<E> getFilteredSelectionModel() {
        return filteredSelectionModel;
    }

    protected FilterList<E> configureFiltering(EventList<E> source) {
        return new FilterList<>(source,
                new CompositeMatcherEditor<>(getSearchFieldMatchers())
        );
    }

    protected JPanel getNorth() {
        final JPanel north = new JPanel(new BorderLayout());
        north.add(searchField, BorderLayout.EAST);

        if (toolBar != null) {
            north.add(toolBar, BorderLayout.CENTER);
        }

        return north;
    }
}


