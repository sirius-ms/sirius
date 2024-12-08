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

package de.unijena.bioinf.ms.gui.table;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import de.unijena.bioinf.ms.gui.utils.loading.SpinnerProgressPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer
 */
public abstract class ActionListDetailView<E extends SiriusPCS, D, T extends ActionList<E, D>> extends ActionListView<T> {

    protected final PlaceholderTextField searchField;
    protected final FilterList<E> filteredSource;
    protected final DefaultEventSelectionModel<E> filteredSelectionModel;
    protected final JToolBar toolBar;


    protected final CardLayout centerCard = new CardLayout();
    protected final JPanel centerCardPanel = new JPanel(centerCard);

    protected final Component firstGap = Box.createGlue();
    protected final Component secondGap = Box.createGlue();

    public ActionListDetailView(T source) {
        this(source, false);
    }

    public ActionListDetailView(T source, final boolean singleSelection) {
        super(source);
        setLayout(new BorderLayout());
        searchField = new PlaceholderTextField();
        searchField.setPlaceholder("Type to search");
        searchField.setPreferredSize(new Dimension(150, searchField.getPreferredSize().height));
        searchField.setMaximumSize(new Dimension(150, searchField.getPreferredSize().height));
        searchField.setToolTipText("Type text to perform a full text search on the data below.");

        this.toolBar = getToolBar();
        filteredSource = configureFiltering(source.elementList);
        filteredSelectionModel = new DefaultEventSelectionModel<>(filteredSource);
        if (singleSelection)
            filteredSelectionModel.setSelectionMode(DefaultEventSelectionModel.SINGLE_SELECTION);
        add(getNorth(), BorderLayout.NORTH);

        addToCenterCard(ActionList.ViewState.NOT_COMPUTED, GuiUtils.newNoResultsComputedPanel());
        addToCenterCard(ActionList.ViewState.EMPTY, GuiUtils.newEmptyResultsPanel());
        addToCenterCard(ActionList.ViewState.LOADING, new SpinnerProgressPanel());
        add(centerCardPanel, BorderLayout.CENTER);
        showCenterCard(ActionList.ViewState.NOT_COMPUTED);
    }

    protected void addToCenterCard(@NotNull ActionList.ViewState name, @NotNull JComponent component) {
        addToCenterCard(name.name(), component);
    }

    protected void addToCenterCard(@NotNull String name, @NotNull JComponent component) {
        centerCardPanel.add(name, component);
        showCenterCard(name);
    }

    public void showCenterCard(@NotNull ActionList.ViewState name) {
        showCenterCard(name.name());
    }

    public void showCenterCard(@NotNull String name) {
        centerCard.show(centerCardPanel, name);
    }


    protected abstract JToolBar getToolBar();

    protected int getIndexOfFirstGap(JToolBar toolBar){
        if (toolBar == null)
            return -1;
        return toolBar.getComponentIndex(firstGap);
    }
    protected int getIndexOfSecondGap(JToolBar toolBar){
        if (toolBar == null)
            return -1;
        return toolBar.getComponentIndex(secondGap);
    }

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

        if (toolBar != null) {
            toolBar.add(searchField, getIndexOfSecondGap(toolBar));
            north.add(toolBar, BorderLayout.NORTH);
        } else {
            north.add(searchField, BorderLayout.NORTH);
        }

        return north;
    }
}


