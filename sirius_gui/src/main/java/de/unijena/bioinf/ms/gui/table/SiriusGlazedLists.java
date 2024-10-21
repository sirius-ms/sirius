/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventAssembler;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SiriusGlazedLists {

    public static <E> void allUpdate(EventList<E> list) {
        multiUpdate(list, null);
    }
    public static <E> void multiUpdate(EventList<E> list, @Nullable Set<E> elementsToUpdate) {
        list.getReadWriteLock().readLock().lock();
        try {
            final ListEventAssembler<E> eventAssembler = new ListEventAssembler<>(list, list.getPublisher());
            eventAssembler.beginEvent();
            for (int i = 0; i < list.size(); i++) {
                if (elementsToUpdate == null || elementsToUpdate.contains(list.get(i)))
                    eventAssembler.elementUpdated(i, ListEvent.unknownValue(), list.get(i));
            }
            eventAssembler.commitEvent();
        } finally {
            list.getReadWriteLock().readLock().unlock();
        }
    }

    public static <E> boolean multiAddRemove(@NotNull EventList<E> baseList, @NotNull List<Pair<E, Boolean>> elementsAddOrRemove) {
        try {
            baseList.getReadWriteLock().writeLock().lock();
                Set<E> toAdd = new LinkedHashSet<>();
                Set<E> toRemove = new LinkedHashSet<>();

                elementsAddOrRemove.forEach(p -> {
                    if (p.value()){
                        toAdd.add(p.key());
                        toRemove.remove(p.key());
                    }else {
                        toAdd.remove(p.key());
                        toRemove.add(p.key());
                    }
                });

                if (toAdd.isEmpty() && toRemove.isEmpty())
                    return true;

                if (!toAdd.isEmpty())
                    baseList.addAll(toAdd);
                if (!toRemove.isEmpty())
                    baseList.removeAll(toRemove);

                return true;
        } finally {
            baseList.getReadWriteLock().writeLock().unlock();
        }
    }

    public static <E> boolean refill(EventList<E> list, ArrayList<E> innerList, Collection<E> elementsToFillIn) {
        try {
            list.getReadWriteLock().writeLock().lock();

            if (elementsToFillIn == null || elementsToFillIn.isEmpty()) {
                list.clear();
                return true;
            } else if (innerList.equals(elementsToFillIn)) {
                return false;
            } else {
                try {
                    final ListEventAssembler<E> eventAssembler = new ListEventAssembler<>(list, list.getPublisher());
                    eventAssembler.beginEvent();
                    int index = 0;
                    for (E e : elementsToFillIn) {
                        if (index < innerList.size()) {
                            eventAssembler.elementUpdated(index, innerList.get(index), e);
                            innerList.set(index, e);
                        } else {
                            eventAssembler.elementInserted(index, e);
                            innerList.add(index, e);
                        }
                        index++;
                    }

                    for (int i = innerList.size() - 1; i >= index; i--) {
                        eventAssembler.elementDeleted(i, innerList.get(i));
                        innerList.remove(i);
                    }

                    eventAssembler.commitEvent();
                    return true;
                } catch (Exception e) {
                    LoggerFactory.getLogger(SiriusGlazedLists.class).error("Error during Event list Refill.", e);
                    return false;
                }
            }
        } finally {
            list.getReadWriteLock().writeLock().unlock();
        }
    }
}
