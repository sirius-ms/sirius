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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by fleisch on 15.05.17.
 */
public abstract class ActionList<E extends SiriusPCS, D> implements ActiveElements<E, D> {
    public enum DataSelectionStrategy {ALL, FIRST_SELECTED, ALL_SELECTED}

    public enum ViewState {NOT_COMPUTED, EMPTY, DATA}

    private final Queue<ActiveElementChangedListener<E, D>> listeners = new ConcurrentLinkedQueue<>();

    protected ObservableElementList<E> elementList;
    protected DefaultEventSelectionModel<E> elementListSelectionModel;
    protected DefaultEventSelectionModel<E> topLevelSelectionModel;

    private final ArrayList<E> elementData = new ArrayList<>();
    private final BasicEventList<E> basicElementList = new BasicEventList<>(elementData);

    private final ReadWriteLock dataLock = new ReentrantReadWriteLock();
    private D data = null;

    public final DataSelectionStrategy selectionType;

    public ActionList(Class<E> cls) {
        this(cls, DataSelectionStrategy.FIRST_SELECTED);
    }

    public ActionList(Class<E> cls, DataSelectionStrategy strategy) {
        selectionType = strategy;
        elementList = new ObservableElementList<>(basicElementList, GlazedLists.beanConnector(cls));
        elementListSelectionModel = new DefaultEventSelectionModel<>(elementList);
        topLevelSelectionModel = elementListSelectionModel;
        elementListSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);


        elementListSelectionModel.addListSelectionListener(e -> {
            if (!elementListSelectionModel.getValueIsAdjusting()) {
                if (elementListSelectionModel.isSelectionEmpty() || elementList == null || elementList.isEmpty())
                    readDataByConsumer(data -> notifyListeners(data, null, elementList, elementListSelectionModel));
                else
                    readDataByConsumer(data -> notifyListeners(data, elementList.get(elementListSelectionModel.getMinSelectionIndex()), elementList, elementListSelectionModel));
            }
        });

        elementList.addListEventListener(listChanges -> {
            if (!elementListSelectionModel.getValueIsAdjusting()) {
                if (!elementListSelectionModel.isSelectionEmpty() && elementList != null && !elementList.isEmpty()) {
                    while (listChanges.next()) {
                        if (elementListSelectionModel.getMinSelectionIndex() == listChanges.getIndex()) {
                            readDataByConsumer(data -> notifyListeners(data, elementList.get(listChanges.getIndex()), elementList, elementListSelectionModel));
                            return;
                        }
                    }
                }
            }
        });
    }

    protected boolean refillElementsEDT(final Collection<E> toFillIn) throws InvocationTargetException, InterruptedException {
        AtomicBoolean ret = new AtomicBoolean();
        Jobs.runEDTAndWait(() -> {
            ret.set(refillElements(toFillIn));
            if (!toFillIn.isEmpty())
                try { // dirty hack to ensure this does not crash
                    topLevelSelectionModel.setSelectionInterval(0, 0);
                } catch (Exception e) {
                    topLevelSelectionModel.clearSelection();
                    //ignore
                }
        });
        return ret.get();
    }

    protected boolean refillElements(final Collection<E> toFillIn) {
        if (SiriusGlazedLists.refill(basicElementList, elementData, toFillIn)) {
            readDataByConsumer(data -> notifyListeners(data, getSelectedElement(), elementList, elementListSelectionModel));
            return true;
        }
        return false;
    }

    public void setTopLevelSelectionModel(DefaultEventSelectionModel<E> topLevelSelectionModel) {
        this.topLevelSelectionModel = topLevelSelectionModel;
    }

    public DefaultEventSelectionModel<E> getTopLevelSelectionModel() {
        return topLevelSelectionModel;
    }

    @NotNull
    public List<E> getSelectedElements() {
        return elementListSelectionModel.isSelectionEmpty() ? List.of() : elementListSelectionModel.getSelected();
    }

    @Nullable
    public E getSelectedElement() {
        return elementListSelectionModel.isSelectionEmpty() ? null : elementList.get(elementListSelectionModel.getMinSelectionIndex());
    }

    public ObservableElementList<E> getElementList() {
        return elementList;
    }

    public DefaultEventSelectionModel<E> getElementListSelectionModel() {
        return elementListSelectionModel;
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

    public void readDataByConsumer(Consumer<D> readData){
        dataLock.readLock().lock();
        try{
            readData.accept(data);
        }finally {
            dataLock.readLock().unlock();
        }
    }

    public <R> R readDataByFunction(Function<D,R> readData){
        dataLock.readLock().lock();
        try{
            return readData.apply(data);
        }finally {
            dataLock.readLock().unlock();
        }
    }

    public void writeData(Consumer<D> writeData){
        dataLock.writeLock().lock();
        try{
            writeData.accept(data);
        }finally {
            dataLock.writeLock().unlock();
        }
    }

    protected void setData(D data) {
        dataLock.writeLock().lock();
        try{
            this.data = data;
        }finally {
            dataLock.writeLock().unlock();
        }
    }

    public boolean hasData() {
        return data != null;
    }
}
