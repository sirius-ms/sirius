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

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CoalescingEdtUpdaterTest {

    /** Lets everything that is already queued on the EDT run to completion. */
    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    @Test
    void requestDoesNotRunTheUpdateInline() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingEdtUpdater updater = new CoalescingEdtUpdater("test", runs::incrementAndGet);

        // this mimics the situation we actually care about: the request happens on the EDT from inside a
        // GlazedLists dispatch, where touching the list/selection views is not safe.
        SwingUtilities.invokeAndWait(() -> {
            updater.request();
            assertEquals(0, runs.get(), "update must not run inside the event that requested it");
        });

        flushEdt();
        assertEquals(1, runs.get());
    }

    @Test
    void burstOfRequestsCollapsesIntoASingleUpdate() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingEdtUpdater updater = new CoalescingEdtUpdater("test", runs::incrementAndGet);

        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < 5; i++)
                updater.request();
        });

        flushEdt();
        assertEquals(1, runs.get(), "a burst must collapse into one update reading the final state");
    }

    @Test
    void aLaterRequestSchedulesAnotherUpdate() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingEdtUpdater updater = new CoalescingEdtUpdater("test", runs::incrementAndGet);

        SwingUtilities.invokeAndWait(updater::request);
        flushEdt();
        SwingUtilities.invokeAndWait(updater::request);
        flushEdt();

        assertEquals(2, runs.get());
    }

    @Test
    void requestingFromInsideTheUpdateSchedulesAFollowUp() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        AtomicBoolean reRequested = new AtomicBoolean(false);
        CoalescingEdtUpdater[] holder = new CoalescingEdtUpdater[1];
        holder[0] = new CoalescingEdtUpdater("test", () -> {
            runs.incrementAndGet();
            if (reRequested.compareAndSet(false, true))
                holder[0].request();
        });

        SwingUtilities.invokeAndWait(holder[0]::request);
        flushEdt();
        flushEdt();

        assertEquals(2, runs.get(), "an update requested while updating must not be swallowed");
    }

    @Test
    void updateRunsOnTheEdtEvenWhenRequestedFromABackgroundThread() throws Exception {
        AtomicBoolean onEdt = new AtomicBoolean(false);
        AtomicInteger runs = new AtomicInteger();
        CoalescingEdtUpdater updater = new CoalescingEdtUpdater("test", () -> {
            onEdt.set(SwingUtilities.isEventDispatchThread());
            runs.incrementAndGet();
        });

        Thread t = new Thread(updater::request);
        t.start();
        t.join();
        flushEdt();
        flushEdt();

        assertEquals(1, runs.get());
        assertTrue(onEdt.get());
    }

    @Test
    void exceptionsNeverEscapeTheUpdate() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingEdtUpdater updater = new CoalescingEdtUpdater("test", () -> {
            runs.incrementAndGet();
            throw new IndexOutOfBoundsException("Index 64 out of bounds for length 63");
        });

        // the runnable handed to the EDT must never throw: in production it would escape into the
        // GlazedLists dispatch that triggered it and corrupt the list/selection state for good.
        assertDoesNotThrow(updater::runNow);
        assertEquals(1, runs.get());

        // a failed update must not wedge the updater
        SwingUtilities.invokeAndWait(updater::request);
        flushEdt();
        assertEquals(2, runs.get());
    }
}
