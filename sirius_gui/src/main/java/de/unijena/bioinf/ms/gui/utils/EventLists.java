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

import ca.odell.glazedlists.EventList;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Mutations of a shared GlazedLists pipeline that honour its locking contract.
 * <p>
 * All lists of one pipeline share a single lock and a single event publisher, and every list transformation
 * dispatches events on the thread that mutated the source. Mutating without the write lock therefore lets two
 * threads dispatch into the same publisher concurrently, which corrupts its state
 * ({@code "subjectsAndListenersForCurrentEvent" is null}) and desynchronizes the swing thread proxy cache from
 * the selection model - after which the affected list is unusable. Note that the library itself mutates under
 * that lock (e.g. {@code ObservableElementList.elementChanged()} when a list element fires a property change
 * from a background thread), so our own mutations must do the same to be serialized against it.
 */
public class EventLists {
    private EventLists() {
    }

    /** Runs the given mutation under the list's write lock. */
    public static void writeLocked(@NotNull EventList<?> list, @NotNull Runnable mutation) {
        list.getReadWriteLock().writeLock().lock();
        try {
            mutation.run();
        } finally {
            list.getReadWriteLock().writeLock().unlock();
        }
    }

    /** Reads from the list under its read lock. */
    public static <R> R readLocked(@NotNull EventList<?> list, @NotNull Supplier<R> read) {
        list.getReadWriteLock().readLock().lock();
        try {
            return read.get();
        } finally {
            list.getReadWriteLock().readLock().unlock();
        }
    }

    /** Removes the given element from the list. */
    public static <T> boolean remove(@NotNull EventList<T> list, T element) {
        boolean[] removed = new boolean[1];
        writeLocked(list, () -> removed[0] = list.remove(element));
        return removed[0];
    }
}
