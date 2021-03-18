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

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by fleisch on 15.05.17.
 */
public abstract class ActionList<E extends SiriusPCS, D> implements ActiveElements<E, D> {
    public enum DataSelectionStrategy {ALL, FIRST_SELECTED, ALL_SELECTED}

    public enum ViewState {NOT_COMPUTED, EMPTY, DATA}

    private final Queue<ActiveElementChangedListener<E, D>> listeners = new ConcurrentLinkedQueue<>();

    protected ObservableElementList<E> elementList;
    protected DefaultEventSelectionModel<E> selectionModel;

    private final ArrayList<E> elementData = new ArrayList<>();
    private final BasicEventList<E> basicElementList = new BasicEventList<>(elementData);

    protected D data = null;
    public final DataSelectionStrategy selectionType;

    public ActionList(Class<E> cls) {
        this(cls, DataSelectionStrategy.FIRST_SELECTED);
    }

    public ActionList(Class<E> cls, DataSelectionStrategy strategy) {
        selectionType = strategy;
        elementList = new ObservableElementList<>(basicElementList, GlazedLists.beanConnector(cls));
        selectionModel = new DefaultEventSelectionModel<>(elementList);
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);


        selectionModel.addListSelectionListener(e -> {
            if (!selectionModel.getValueIsAdjusting()) {
                if (selectionModel.isSelectionEmpty() || elementList == null || elementList.isEmpty())
                    notifyListeners(data, null, elementList, selectionModel);
                else
                    notifyListeners(data, elementList.get(selectionModel.getMinSelectionIndex()), elementList, selectionModel);
            }
        });

        elementList.addListEventListener(listChanges -> {
            if (!selectionModel.getValueIsAdjusting()) {
                if (!selectionModel.isSelectionEmpty() && elementList != null && !elementList.isEmpty()) {
                    while (listChanges.next()) {
                        if (selectionModel.getMinSelectionIndex() == listChanges.getIndex()) {
                            notifyListeners(data, elementList.get(selectionModel.getMinSelectionIndex()), elementList, selectionModel);
                            return;
                        }
                    }
                }
            }
        });
    }

    protected boolean refillElementsEDT(final Collection<E> toFillIn) throws InvocationTargetException, InterruptedException {
        AtomicBoolean ret = new AtomicBoolean();
        Jobs.runEDTAndWait(() -> ret.set(refillElements(toFillIn)));
        return ret.get();
    }

    protected boolean refillElements(final Collection<E> toFillIn) {
        if (SiriusGlazedLists.refill(basicElementList, elementData, toFillIn)) {
            notifyListeners(data, null, elementList, getResultListSelectionModel());
            return true;
        }
        return false;
    }

    public D getData() {
        return data;
    }

    public ObservableElementList<E> getElementList() {
        return elementList;
    }

    public DefaultEventSelectionModel<E> getResultListSelectionModel() {
        return selectionModel;
    }

    public void addActiveResultChangedListener(ActiveElementChangedListener<E, D> listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveElementChangedListener<E, D> listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(D data, E element, List<E> sre, ListSelectionModel selections) {
        for (ActiveElementChangedListener<E, D> listener : listeners) {
            listener.resultsChanged(data, element, sre, selections);
        }
    }
}
