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

package de.unijena.bioinf.ms.gui.mainframe.instance_panel;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.dialogs.CompoundFilterOptionsDialog;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.matchers.BackgroundJJobMatcheEditor;
import de.unijena.bioinf.ms.gui.utils.toggleswitch.toggle.JToggleSwitch;
import de.unijena.bioinf.projectspace.GuiProjectManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.DataQuality;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This is the main List of the SIRIUS UI.
 * It shows the main Instances (former Compounds or Experiments)
 * It is usually a singleton and backed by the INSTANCE_LIST of the  {@link GuiProjectManager}
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CompoundList {

    final PlaceholderTextField searchField;
    final JToggleSwitch adductToggleSwitch;
    final JToggleSwitch qualityToggleSwitch;
    final JToggleSwitch msMsToggleSwitch;


    final JButton openFilterPanelButton;
    final CompoundFilterModel compoundFilterModel;
    final ObservableElementList<InstanceBean> observableScource;
    @Getter
    final SortedList<InstanceBean> sortedSource;
    final FilterList<InstanceBean> filterList;
    @Getter
    final EventList<InstanceBean> compoundList; // wrapper for filteredList that executes events in swing edt

    final DefaultEventSelectionModel<InstanceBean> compountListSelectionModel;
    final BackgroundJJobMatcheEditor<InstanceBean> backgroundFilterMatcher;
    final private MatcherEditorWithOptionalInvert<InstanceBean> compoundListMatchEditor;

    private final Queue<ExperimentListChangeListener> listeners = new ConcurrentLinkedQueue<>();

    private final Color defaultOpenFilterPanelButtonColor;

    @Getter
    private @NotNull SiriusGui gui;

    public CompoundList(@NotNull SiriusGui gui) {
        this.gui = gui;
        //additional filter based on specific parameters
        compoundFilterModel = new CompoundFilterModel(gui);
        // filter based ion full text field
        searchField = new PlaceholderTextField();
        searchField.setPlaceholder("Type and hit enter to search");
        searchField.setToolTipText("Type text to perform a full text search on the data below. Hit enter to start searching.");

        adductToggleSwitch = makeAdductToggleSwitch(compoundFilterModel);
        qualityToggleSwitch = makeQualityToggleSwitch(compoundFilterModel);
        msMsToggleSwitch = makeMsMsToggleSwitch(compoundFilterModel);

        observableScource = new ObservableElementList<>(gui.getProjectManager().INSTANCE_LIST, GlazedLists.beanConnector(InstanceBean.class));
        sortedSource = new SortedList<>(observableScource, Comparator.comparing(InstanceBean::getRTOrMissing));
        compoundFilterModel.updateAdducts(sortedSource);

        //filters
        BasicEventList<MatcherEditor<InstanceBean>> listOfFilters = new BasicEventList<>();
        //text filter
        listOfFilters.add(new TextComponentMatcherEditor<>(searchField, (baseList, element) -> {
            baseList.add(element.getGUIName());
            baseList.add(element.getIonType().toString());
            baseList.add(String.valueOf(element.getIonMass()));
        }, false));

        listOfFilters.add(new CompoundFilterMatcherEditor(new CompoundFilterMatcher(gui.getProperties(), compoundFilterModel)));
        //combined filters
        CompositeMatcherEditor<InstanceBean> compositeMatcherEditor = new CompositeMatcherEditor<>(listOfFilters);
        compositeMatcherEditor.setMode(CompositeMatcherEditor.AND);

        compoundListMatchEditor = new MatcherEditorWithOptionalInvert<>(compositeMatcherEditor);
        backgroundFilterMatcher = new BackgroundJJobMatcheEditor<>(compoundListMatchEditor);
        filterList = new FilterList<>(sortedSource, backgroundFilterMatcher);
        compoundList = GlazedListsSwing.swingThreadProxyList(filterList);

        //filter dialog
        openFilterPanelButton = new JButton("...");
        openFilterPanelButton.setToolTipText("Open filter panel");
        defaultOpenFilterPanelButtonColor = openFilterPanelButton.getBackground();

        openFilterPanelButton.addActionListener(e -> new CompoundFilterOptionsDialog(gui, searchField, compoundFilterModel, this));
        compositeMatcherEditor.addMatcherEditorListener(evt -> {
            colorByActiveFilter();
            updateTogglesByActiveFilter();
        });

        compountListSelectionModel = new DefaultEventSelectionModel<>(compoundList);

        compountListSelectionModel.addListSelectionListener(e -> {
            final Component c = gui.getMainFrame().getResultsPanel().getSelectedComponent();
            if (c instanceof Loadable l)
                l.setLoading(true, true);

            if (!e.getValueIsAdjusting()) {
                compountListSelectionModel.getDeselected().forEach(InstanceBean::disableProjectSpaceListener);
                compountListSelectionModel.getSelected().forEach(InstanceBean::enableProjectSpaceListener);
                notifyListenerSelectionChange(e);
            }
        });

        // data change listener needs to operate on unfiltered list as well to notice add or removal on filtered elements
        sortedSource.addListEventListener(this::notifyListenerFullListDataChange);
        compoundList.addListEventListener(this::notifyListenerDataChange);

        //init filters
        compoundFilterModel.updateAdducts(sortedSource);
        compoundFilterModel.fireUpdateCompleted();
    }

    private void colorByActiveFilter() {
        //is any filtering option active (despite the text filter which is visible all the time)
        if (isFilterInverted()) {
            openFilterPanelButton.setBackground(Colors.Menu.FILTER_BUTTON_INVERTED);
            openFilterPanelButton.setForeground(Colors.Menu.FILTER_BUTTON_INVERTED_TEXT);
        } else if (compoundFilterModel.isActive() || !searchField.getText().isEmpty()) {
            openFilterPanelButton.setBackground(Colors.Menu.FILTER_BUTTON);
            openFilterPanelButton.setForeground(Colors.Menu.FILTER_BUTTON_TEXT);
        } else {
            openFilterPanelButton.setBackground(defaultOpenFilterPanelButtonColor);
            openFilterPanelButton.setForeground(Colors.FOREGROUND_DATA);
        }
    }

    protected void updateTogglesByActiveFilter() {
        msMsToggleSwitch.setSelected(!compoundFilterModel.isHasMsMs(), false, false);
        adductToggleSwitch.setSelected(compoundFilterModel.isMultiAdductsAllowed(), false, false);
        qualityToggleSwitch.setSelected(compoundFilterModel.getFeatureQualityFilter().isQualitySelected(DataQuality.BAD), false, false);
    }

    private static @NotNull JToggleSwitch makeAdductToggleSwitch(CompoundFilterModel model) {
        JToggleSwitch tSwitch = new JToggleSwitch();
        tSwitch.setSelected(model.isMultiAdductsAllowed(), false, false);
        tSwitch.addEventToggleSelected(selected -> {
            if (selected)
                model.addMultiAdducts();
            else
                model.removeMultiAdducts();
            model.fireUpdateCompleted();
        });
        return tSwitch;
    }

    private static @NotNull JToggleSwitch makeQualityToggleSwitch(CompoundFilterModel model) {
        final CompoundFilterModel.QualityFilter fqFilter = model.getFeatureQualityFilter();
        JToggleSwitch tSwitch = new JToggleSwitch();
        tSwitch.setSelected(fqFilter.isQualitySelected(DataQuality.BAD), false, false); //initialize from model
        tSwitch.addEventToggleSelected(selected -> {
            if (selected) {
                // we add only the bad ones when enabling
                fqFilter.addQuality(DataQuality.BAD);
            } else {
                fqFilter.removeQuality(DataQuality.LOWEST);
                fqFilter.removeQuality(DataQuality.BAD);
            }
            model.fireUpdateCompleted();
        });
        // ensure default value is propagated
        return tSwitch;
    }

    private static @NotNull JToggleSwitch makeMsMsToggleSwitch(CompoundFilterModel model) {
        JToggleSwitch tSwitch = new JToggleSwitch();
        tSwitch.setSelected(!model.isHasMsMs(), false, false); ///initialize from model
        tSwitch.addEventToggleSelected(selected -> {
            model.setHasMsMs(!selected);
            model.fireUpdateCompleted();
        });
        return tSwitch;
    }

    public void orderBy(@NotNull final Comparator<InstanceBean> comp) {
        sortedSource.setComparator(comp);
    }

    public boolean isFilterInverted() {
        return compoundListMatchEditor.isInverted();
    }

    public void toggleInvertFilter() {
        compoundListMatchEditor.setInverted(!compoundListMatchEditor.isInverted());
    }


    /**
     * Updates the  available filter options in the filter model.
     * Does not cause global re-filtering
     */
    public void updateFilter(@NotNull java.util.List<InstanceBean> instances) {
        compoundFilterModel.updateAdducts(instances);
        updateTogglesByActiveFilter();
    }
    public void resetFilter() {
        //filtering consists of the text filter, the filter model and the possible inversion using the MatcherEditor
        compoundFilterModel.resetFilter();
        compoundListMatchEditor.setInverted(false);
        searchField.setText("");
        searchField.postActionEvent();
        colorByActiveFilter();
        updateTogglesByActiveFilter();
    }

    private void notifyListenerFullListDataChange(ListEvent<InstanceBean> event) {
        //copy event is hell important to reset the iterator
        for (ExperimentListChangeListener l : listeners) {
            l.fullListChanged(event.copy(), compountListSelectionModel, compoundList.size());
        }
    }

    private void notifyListenerDataChange(ListEvent<InstanceBean> event) {
        //copy event is hell important to reset the iterator
        for (ExperimentListChangeListener l : listeners) {
            l.listChanged(event.copy(), compountListSelectionModel, sortedSource.size());
        }
    }

    private void notifyListenerSelectionChange(ListSelectionEvent event) {
        final java.util.List<InstanceBean> selected = Collections.unmodifiableList(compountListSelectionModel.getSelected());
        final java.util.List<InstanceBean> deselected = Collections.unmodifiableList(compountListSelectionModel.getDeselected());
        for (ExperimentListChangeListener l : listeners) {
            l.listSelectionChanged(compountListSelectionModel, selected, deselected, sortedSource.size());
        }
    }

    //API methods
    public void addChangeListener(ExperimentListChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ExperimentListChangeListener l) {
        listeners.remove(l);
    }

    public DefaultEventSelectionModel<InstanceBean> getCompoundListSelectionModel() {
        return compountListSelectionModel;
    }

    public int getFullSize() {
        return sortedSource.size();
    }
}
