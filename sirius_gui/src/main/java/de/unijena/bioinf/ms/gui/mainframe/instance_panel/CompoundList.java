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
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.AbstractMatcherEditorListenerSupport;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.AdvancedListSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.dialogs.filter.FeatureFilterOptionsDialog;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.search.LuceneSearchBar;
import de.unijena.bioinf.ms.gui.utils.search.PanelFilterTerms;
import de.unijena.bioinf.ms.gui.utils.loading.LazyLoadingPanel;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.projectspace.GuiProjectManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
@Slf4j
public class CompoundList {

    final LuceneSearchBar searchBar;

    final JButton openFilterPanelButton;
    final ObservableElementList<InstanceBean> observableScource;
    @Getter
    final SortedList<InstanceBean> sortedSource;
    @Getter
    final EventList<InstanceBean> compoundList;

    @Getter
    final AdvancedListSelectionModel<InstanceBean> compoundListSelectionModel;

    private final Queue<ExperimentListChangeListener> listeners = new ConcurrentLinkedQueue<>();

    private final Color defaultOpenFilterPanelButtonColor;

    @Getter
    private @NotNull SiriusGui gui;
    private final GuiProjectManager projectManager;
    private final FeatureFilterModel filterModel;

    public CompoundList(@NotNull SiriusGui gui) {
        this.gui = gui;
        this.projectManager = gui.getProjectManager();
        //additional filter based on specific parameters
        filterModel = projectManager.getFeatureFilterModel();
        // lucene query search bar: collapsed summary here, chip-based query builder with
        // autocompletion in an overlay expanding over the result view
        searchBar = new LuceneSearchBar(gui.getSiriusClient(), projectManager.getProjectId(), filterModel,
                () -> PanelFilterTerms.of(filterModel, gui.getProperties().getConfidenceDisplayMode()),
                term -> new FeatureFilterOptionsDialog(gui, filterModel, this, term.id()));

        observableScource = new ObservableElementList<>(gui.getProjectManager().INSTANCE_LIST, GlazedLists.beanConnector(InstanceBean.class));
        sortedSource = new SortedList<>(observableScource, Comparator.comparing(InstanceBean::getRTOrMissing));
        compoundList = GlazedListsSwing.swingThreadProxyList(sortedSource);

        //filter dialog
        openFilterPanelButton = new JButton("...");
        openFilterPanelButton.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.OpenFilterPanelButton);
        openFilterPanelButton.setToolTipText("Open filter panel");
        defaultOpenFilterPanelButtonColor = openFilterPanelButton.getBackground();

        openFilterPanelButton.addActionListener(e -> new FeatureFilterOptionsDialog(gui, filterModel, this));

        compoundListSelectionModel = new DefaultEventSelectionModel<>(compoundList);


        // data change listener needs to operate on unfiltered list as well to notice add or removal on filtered elements
        compoundList.addListEventListener(this::notifyListenerDataChange);

        //init filters
        filterModel.addUpdateCompleteListener(evt -> colorByActiveFilter());
        filterModel.updateAdducts(projectManager.getDetectedAdducts());
        filterModel.fireUpdateCompleted();
    }


    private boolean selectionListenerRegistered = false;
    public synchronized void initializedSelectionListener(@NotNull LazyLoadingPanel<ResultPanel> resultPanelProvider){
        if (!selectionListenerRegistered) {
            compoundListSelectionModel.addListSelectionListener(e -> {
                final Component c = resultPanelProvider.getContentPanelIfReady().map(ResultPanel::getSelectedComponent)
                        .orElse(null);
                if (c instanceof Loadable l)
                    l.setLoading(true, true);

                if (!e.getValueIsAdjusting()) {
                    //we only enable listener for first selected because this is the one where results are visible.
                    compoundListSelectionModel.getDeselected().forEach(InstanceBean::disableProjectSpaceListener);
                    compoundListSelectionModel.getSelected().stream().skip(1).forEach(InstanceBean::disableProjectSpaceListener);
                    if (!compoundListSelectionModel.isSelectionEmpty()){
                        InstanceBean selected = compoundListSelectionModel.getSelected().getFirst();
                        selected.enableProjectSpaceListener();
                        projectManager.removeTemporaryJumpToFeatureIfNotSelected(selected.getFeatureId());
                    }
                    notifyListenerSelectionChange();
                }
            });
            selectionListenerRegistered = true;
        }
    }

    private void colorByActiveFilter() {
        //is any filtering option active (despite the text filter which is visible all the time)
        if (filterModel.isActive()) {
            if (filterModel.isInverted()) {
                openFilterPanelButton.setBackground(Colors.Menu.FILTER_BUTTON_INVERTED);
                openFilterPanelButton.setForeground(Colors.Menu.FILTER_BUTTON_INVERTED_TEXT);
            } else {
                openFilterPanelButton.setBackground(Colors.Menu.FILTER_BUTTON);
                openFilterPanelButton.setForeground(Colors.Menu.FILTER_BUTTON_TEXT);
            }
        } else {
            openFilterPanelButton.setBackground(defaultOpenFilterPanelButtonColor);
            openFilterPanelButton.setForeground(Colors.FOREGROUND_DATA);
        }
    }

    public void orderBy(@NotNull final Comparator<InstanceBean> comp) {
        sortedSource.setComparator(comp);
    }

    public void resetFilter() {
        //filtering consists of the text filter and the filter model
        filterModel.resetFilter(); //also clears the shared search text document
        searchBar.refreshSummary();
        colorByActiveFilter();
    }

    private void notifyListenerDataChange(ListEvent<InstanceBean> event) {
        //copy event is hell important to reset the iterator
        long total = projectManager.getTotalInstances();
        for (ExperimentListChangeListener l : listeners)
            l.listChanged(event.copy(), compoundListSelectionModel, total);
    }

    private void notifyListenerSelectionChange() {
        final java.util.List<InstanceBean> selected = Collections.unmodifiableList(compoundListSelectionModel.getSelected());
        final java.util.List<InstanceBean> deselected = Collections.unmodifiableList(compoundListSelectionModel.getDeselected());
        long total = projectManager.getTotalInstances();
        for (ExperimentListChangeListener l : listeners)
            l.listSelectionChanged(compoundListSelectionModel, selected, deselected, total);
    }

    //API methods
    public void addChangeListener(ExperimentListChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ExperimentListChangeListener l) {
        listeners.remove(l);
    }

    public int getFullSize() {
        return sortedSource.size();
    }

    /**
     * Selects an InstanceBean in the list based on its featureId.
     * If the instance is currently filtered out (i.e., not in {@link #compoundList} but present in {@link #sortedSource}),
     * the featureId will be added to the filter model to ensure the feature is shown.
     * Then, the instance will be selected in the UI.
     *
     * @param featureId The non-null featureId of the InstanceBean to find and select.
     */
    public boolean selectInstanceByFeatureId(@NotNull String featureId) {
        // 1. Search for the InstanceBean in the complete list (sortedSource).
        InstanceBean targetInstance = sortedSource.stream()
                .filter(bean -> bean.getFeatureId().equals(featureId))
                .findAny().orElse(null);

        // 2. if not in list assume its filter by lucene search and try loading it from api
        if (targetInstance == null)
            targetInstance = projectManager.findAndAddTemporaryJumpToFeature(featureId);

        // 3. If still null feature ID does not exist in project. Ignore feature jump
        if (targetInstance == null) {
            log.warn("Feature with featureId '" + featureId + "' not found in the GUI feature list.");
            return false;
        }

        // 4. jump to feature
        // Ensure this runs on EDT.
        final InstanceBean finalTargetInstance = targetInstance;
        Jobs.runEDTLater(() -> {
            int indexInView = compoundList.indexOf(finalTargetInstance);
            if (indexInView != -1) {
                compoundListSelectionModel.setSelectionInterval(indexInView, indexInView);
                gui.getMainFrame().ensureCompoundIsVisible(indexInView);
            } else {
                // Should not happen if contains is true.
                log.warn("Feature with featureId '" + finalTargetInstance.getFeatureId() + "' exists in the full list but index retrieval failed.");
            }
        });
        return true;
    }
}
