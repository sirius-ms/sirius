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

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public abstract class ActionList<E extends SiriusPCS, D> implements ActiveElements<E, D> {
    public enum DataSelectionStrategy {ALL, FIRST_SELECTED, ALL_SELECTED}

    public enum ViewState {NOT_COMPUTED, EMPTY, DATA, LOADING}

    private final Queue<ActiveElementChangedListener<E, D>> listeners = new ConcurrentLinkedQueue<>();

    @Getter
    protected final ObservableElementList<E> elementList;
    @Getter
    protected final DefaultEventSelectionModel<E> elementListSelectionModel;
    private final ArrayList<E> elementData = new ArrayList<>();
    private final BasicEventList<E> basicElementList = new BasicEventList<>(elementData);

    // data object shall be read and written only from EDT
    private D data = null;

    public final DataSelectionStrategy selectionType;

    public ActionList(Class<E> cls) {
        this(cls, DataSelectionStrategy.FIRST_SELECTED);
    }

    public ActionList(Class<E> cls, DataSelectionStrategy strategy) {
        selectionType = strategy;
        elementList = new ObservableElementList<>(basicElementList, GlazedLists.beanConnector(cls));
        elementListSelectionModel = new DefaultEventSelectionModel<>(elementList);
        elementListSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);


        elementListSelectionModel.addListSelectionListener(e -> {
            //runs in edt
            if (!SwingUtilities.isEventDispatchThread())
                log.warn("WARNING: Data access not int EDT!!!");
            if (!elementListSelectionModel.getValueIsAdjusting()) {
                if (elementListSelectionModel.isSelectionEmpty() || elementList == null || elementList.isEmpty())
                    notifyListenersEDT(null, elementList, elementListSelectionModel);
                else
                    notifyListenersEDT(elementList.get(elementListSelectionModel.getMinSelectionIndex()), elementList, elementListSelectionModel);
            }
        });

        elementList.addListEventListener(listChanges -> {
            //runs in edt
            if (!SwingUtilities.isEventDispatchThread())
                log.warn("WARNING: Data access not int EDT!!!");
            if (!elementListSelectionModel.getValueIsAdjusting()) {
                if (!elementListSelectionModel.isSelectionEmpty() && elementList != null && !elementList.isEmpty()) {
                    while (listChanges.next()) {
                        if (elementListSelectionModel.getMinSelectionIndex() == listChanges.getIndex()) {
                            notifyListenersEDT(elementList.get(listChanges.getIndex()), elementList, elementListSelectionModel);
                            return;
                        }
                    }
                }
            }
        });
    }

    protected boolean refillElementsEDT(D parentDataObject, final Collection<E> toFillIn) throws InvocationTargetException, InterruptedException {
        AtomicBoolean ret = new AtomicBoolean();
        Jobs.runEDTAndWait(() -> {
            try {
                this.data = parentDataObject;
                elementListSelectionModel.setValueIsAdjusting(true);
                elementListSelectionModel.clearSelection();
                ret.set(SiriusGlazedLists.refill(basicElementList, elementData, toFillIn));
                if (!elementList.isEmpty())
                    try { // should not happen
                        elementListSelectionModel.setSelectionInterval(0, 0);
                    } catch (Exception e) {
                        LoggerFactory.getLogger(getClass()).warn("Error when resetting selection for elementList");
                    }
            } finally {
                elementListSelectionModel.setValueIsAdjusting(false);
                if (ret.get())
                    notifyListenersEDT(getSelectedElement(), elementList, elementListSelectionModel);
            }
        });
        return ret.get();
    }

    protected boolean refillElements(D parentDataObject, final Collection<E> toFillIn) {
        if (!SwingUtilities.isEventDispatchThread())
            log.warn("refillElements() Must be called from EventDispatchThread!");
        data = parentDataObject;
        if (SiriusGlazedLists.refill(basicElementList, elementData, toFillIn)) {
            notifyListenersEDT(getSelectedElement(), elementList, elementListSelectionModel);
            return true;
        }
        return false;
    }

    @NotNull
    public List<E> getSelectedElements() {
        return elementListSelectionModel.isSelectionEmpty() ? List.of() : elementListSelectionModel.getSelected();
    }

    @Nullable
    public E getSelectedElement() {
        return elementListSelectionModel.isSelectionEmpty() ? null : elementList.get(elementListSelectionModel.getMinSelectionIndex());
    }

    public void addActiveResultChangedListener(ActiveElementChangedListener<E, D> listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveElementChangedListener<E, D> listener) {
        listeners.remove(listener);
    }

    protected void notifyListenersEDT(E element, List<E> sre, ListSelectionModel selections) {
        Jobs.runEDTAndWaitLazy(() -> {
            for (ActiveElementChangedListener<E, D> listener : listeners) {
                listener.resultsChanged(data, element, sre, selections);
            }
        });
    }

    public void readDataByConsumer(Consumer<D> readData) {
        Jobs.runEDTAndWaitLazy(() -> readData.accept(data));
    }

    public void setDataEDT(D data) {
        Jobs.runEDTAndWaitLazy(() -> this.data = data);
    }
}
