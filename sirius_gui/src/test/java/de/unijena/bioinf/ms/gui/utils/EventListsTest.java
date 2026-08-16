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

package de.unijena.bioinf.ms.gui.utils;

import ca.odell.glazedlists.*;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EventListsTest {

    /** Minimal observable list element, like InstanceBean firing "instance.updated" from a background thread. */
    public static class Bean {
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        private final int id;
        private volatile int revision = 0;

        Bean(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void touch() {
            revision++;
            pcs.firePropertyChange("revision", revision - 1, revision);
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
            pcs.addPropertyChangeListener(l);
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
            pcs.removePropertyChangeListener(l);
        }
    }

    private static List<Bean> beans(int from, int count) {
        List<Bean> beans = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            beans.add(new Bean(from + i));
        return beans;
    }

    /** The page swap of the feature list: discard everything, then add the freshly loaded page. */
    private static void swapContent(EventList<Bean> list, List<Bean> newContent) {
        EventLists.writeLocked(list, () -> {
            list.clear();
            list.addAll(newContent);
        });
    }

    @Test
    void writeLockedSwapReplacesTheWholeContent() {
        EventList<Bean> list = new BasicEventList<>();
        list.addAll(beans(0, 3));

        List<Bean> fresh = beans(100, 5);
        swapContent(list, fresh);

        assertEquals(fresh, new ArrayList<>(list));
    }

    @Test
    void removeRemovesTheElement() {
        EventList<Bean> list = new BasicEventList<>();
        List<Bean> content = beans(0, 3);
        list.addAll(content);

        assertTrue(EventLists.remove(list, content.get(1)));
        assertEquals(List.of(content.get(0), content.get(2)), new ArrayList<>(list));
        assertFalse(EventLists.remove(list, new Bean(42)));
    }

    @Test
    void readLockedReturnsTheReadValue() {
        EventList<Bean> list = new BasicEventList<>();
        list.addAll(beans(0, 4));
        assertEquals(4, (int) EventLists.readLocked(list, list::size));
    }

    /**
     * Regression test for the feature list dying with
     * {@code NullPointerException: "this.subjectsAndListenersForCurrentEvent" is null} /
     * {@code IndexOutOfBoundsException} inside the GlazedLists event publisher: a page swap of the source list
     * must not be able to dispatch concurrently with an element change fired from another thread (which is what
     * {@code ObservableElementList} does when a bean notifies from a background thread).
     */
    @Test
    void listSwapDoesNotDispatchConcurrentlyWithElementChanges() throws Exception {
        EventList<Bean> source = new BasicEventList<>();
        ObservableElementList<Bean> observable =
                new ObservableElementList<>(source, GlazedLists.beanConnector(Bean.class));
        SortedList<Bean> sorted = new SortedList<>(observable, Comparator.comparingInt(Bean::getId));
        // a downstream listener, so dispatching actually does some work (as the real pipeline does)
        List<Integer> sizes = new ArrayList<>();
        sorted.addListEventListener(e -> sizes.add(e.getSourceList().size()));

        List<Throwable> failures = new CopyOnWriteArrayList<>();
        AtomicBoolean stop = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);

        // keeps firing element changes on beans that are currently in the list
        Thread toucher = new Thread(() -> {
            started.countDown();
            while (!stop.get()) {
                try {
                    for (Bean b : EventLists.readLocked(source, () -> new ArrayList<>(source)))
                        b.touch();
                } catch (Throwable t) {
                    failures.add(t);
                }
            }
        }, "element-toucher");
        toucher.setDaemon(true);

        swapContent(source, beans(0, 64));
        toucher.start();
        started.await();

        try {
            for (int round = 0; round < 300; round++) {
                List<Bean> page = beans(round * 100, 63 + (round % 3));
                swapContent(source, page);
                assertEquals(page.size(), EventLists.readLocked(sorted, sorted::size),
                        "transformed list lost track of the source content");
            }
        } finally {
            stop.set(true);
            toucher.join(5000);
        }

        assertEquals(List.of(), failures, "concurrent element change corrupted the event pipeline");
    }
}
