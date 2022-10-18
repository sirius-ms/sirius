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
import de.unijena.bioinf.ms.gui.dialogs.CompoundFilterOptionsDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.matchers.BackgroundJJobMatcheEditor;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This is the main List of the SIRIUS UI.
 * It shows the main Instances (former Compounds or Experiments)
 * It is usually a singleton and backed by the INSTANCE_LIST of the  {@link GuiProjectSpaceManager}
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CompoundList {

    final SearchTextField searchField;
    final JButton openFilterPanelButton;
    final CompoundFilterModel compoundFilterModel;
    final ObservableElementList<InstanceBean> obsevableScource;
    final SortedList<InstanceBean> sortedScource;
    final EventList<InstanceBean> compoundList;
    final DefaultEventSelectionModel<InstanceBean> compountListSelectionModel;
    final BackgroundJJobMatcheEditor<InstanceBean> backgroundFilterMatcher;
    final private MatcherEditorWithOptionalInvert<InstanceBean> compoundListMatchEditor;

    private final Queue<ExperimentListChangeListener> listeners = new ConcurrentLinkedQueue<>();

    public CompoundList(@NotNull final GuiProjectSpaceManager ps) {
        searchField = new SearchTextField("Hit enter to search...");
        obsevableScource = new ObservableElementList<>(ps.INSTANCE_LIST, GlazedLists.beanConnector(InstanceBean.class));
        sortedScource = new SortedList<>(obsevableScource, Comparator.comparing(b -> b.getID().getCompoundIndex()));

        //filters
        BasicEventList<MatcherEditor<InstanceBean>> listOfFilters = new BasicEventList<>();
        //text filter
        listOfFilters.add(new TextComponentMatcherEditor<>(searchField.textField, (baseList, element) -> {
            baseList.add(element.getGUIName());
            baseList.add(element.getIonization().toString());
            baseList.add(String.valueOf(element.getIonMass()));
        }, false));
        //additional filter based on specific parameters
        compoundFilterModel = new CompoundFilterModel();
        listOfFilters.add(new CompoundFilterMatcherEditor(compoundFilterModel));
        //combined filters
        CompositeMatcherEditor<InstanceBean> compositeMatcherEditor = new CompositeMatcherEditor<>(listOfFilters);
        compositeMatcherEditor.setMode(CompositeMatcherEditor.AND);
        compoundListMatchEditor = new MatcherEditorWithOptionalInvert<>(compositeMatcherEditor);
        backgroundFilterMatcher = new BackgroundJJobMatcheEditor<>(compoundListMatchEditor);
        FilterList<InstanceBean> filterList = new FilterList<>(sortedScource, backgroundFilterMatcher);
        compoundList = GlazedListsSwing.swingThreadProxyList(filterList);

        //filter dialog
        openFilterPanelButton = new JButton("...");
        openFilterPanelButton.addActionListener(e -> {
            new CompoundFilterOptionsDialog(MainFrame.MF, searchField, compoundFilterModel, this);
            colorByActiveFilter(openFilterPanelButton, compoundFilterModel);
        });

        compountListSelectionModel = new DefaultEventSelectionModel<>(compoundList);

        compountListSelectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                compountListSelectionModel.getDeselected().forEach(InstanceBean::unregisterProjectSpaceListeners);
                compountListSelectionModel.getSelected().forEach(InstanceBean::registerProjectSpaceListeners);
                notifyListenerSelectionChange();
            }
        });
        compoundList.addListEventListener(this::notifyListenerDataChange);
    }

    private void colorByActiveFilter(JButton openFilterPanelButton, CompoundFilterModel compoundFilterModel) {
        //is any filtering option active (despite the text filter which is visible all the time)
        if (isFilterInverted()){
            openFilterPanelButton.setBackground(new Color(235, 94, 85));
        } else if (compoundFilterModel.isActive()){
            openFilterPanelButton.setBackground(new Color(49, 153, 187));
        }else {
            openFilterPanelButton.setBackground(Color.LIGHT_GRAY);
        }
    }

    public void orderBy(@NotNull final Comparator<InstanceBean> comp) {
        sortedScource.setComparator(comp);
    }

    public boolean isFilterInverted() {
        return compoundListMatchEditor.isInverted();
    }

    public void toggleInvertFilter() {
        compoundListMatchEditor.setInverted(!compoundListMatchEditor.isInverted());
    }

    public void resetFilter() {
        //filtering consists of the text filter, the filter model and the possible inversion using the MatcherEditor
        compoundFilterModel.resetFilter();
        compoundListMatchEditor.setInverted(false);
        searchField.textField.setText("");
        searchField.textField.postActionEvent();
        colorByActiveFilter(openFilterPanelButton, compoundFilterModel);
    }

    private void notifyListenerDataChange(ListEvent<InstanceBean> event) {
        for (ExperimentListChangeListener l : listeners) {
            event.reset();//this is hell important to reset the iterator
            l.listChanged(event, compountListSelectionModel);
        }
    }

    private void notifyListenerSelectionChange() {
        for (ExperimentListChangeListener l : listeners) {
            l.listSelectionChanged(compountListSelectionModel);
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

    public EventList<InstanceBean> getCompoundList() {
        return compoundList;
    }
}
