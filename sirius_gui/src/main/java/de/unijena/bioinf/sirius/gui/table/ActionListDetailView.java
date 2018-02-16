package de.unijena.bioinf.sirius.gui.table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.structure.AbstractEDTBean;
import de.unijena.bioinf.sirius.gui.utils.SearchTextField;
import org.jdesktop.beans.AbstractBean;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ActionListDetailView<E extends AbstractEDTBean, D, T extends ActionList<E, D>> extends ActionListView<T> {
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
        filteredSelectionModel = new DefaultEventSelectionModel<E>(filteredSource);
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
        FilterList<E> fl = new FilterList<E>(source,
                new CompositeMatcherEditor<>(getSearchFieldMatchers())
        );
        return fl;
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


