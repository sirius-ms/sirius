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

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs a GUI update on the EDT <b>after</b> the event that requested it has been dispatched, coalescing a burst
 * of requests into a single update that sees the final state.
 * <p>
 * This exists because GlazedLists notifies selection listeners from <i>inside</i> its list-event dispatch. Reading
 * the live selection views (or, worse, mutating the source list) from such a listener sees half-updated state, and
 * an exception escaping the listener aborts the dispatch and leaves the {@code ListSelection} barcode permanently
 * out of sync with the list - after which every further click on the list fails. Deferring the reaction guarantees
 * it runs on a consistent list, and swallowing (logging) failures keeps a single bad update from killing the list.
 *
 * @see de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList
 */
@Slf4j
public class CoalescingEdtUpdater {
    private final String name;
    private final Runnable update;
    // true while an update is queued on the EDT but has not started yet; further requests are then redundant
    // because the queued update reads the current state anyway.
    private final AtomicBoolean queued = new AtomicBoolean(false);

    public CoalescingEdtUpdater(@NotNull String name, @NotNull Runnable update) {
        this.name = name;
        this.update = update;
    }

    /**
     * Requests the update. Never runs it inline: it is always executed later on the EDT, so it is safe to call
     * this from within an event dispatch. Redundant while an update is already queued.
     */
    public void request() {
        if (queued.compareAndSet(false, true))
            Jobs.runEDTLater(this::runNow);
    }

    /** Package-private for testing; run only via {@link #request()} in production. */
    void runNow() {
        // reset before running so an update requested by the update itself is not swallowed
        queued.set(false);
        try {
            update.run();
        } catch (RuntimeException e) {
            log.warn("Error during coalesced GUI update '{}'. Skipping it.", name, e);
        }
    }
}
