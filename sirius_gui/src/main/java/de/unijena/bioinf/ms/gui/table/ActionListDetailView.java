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


