/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.db.nosql.nitrite;

import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreTool;

import java.nio.file.Path;
import java.util.Map;

/**
 * Rewrites an MVStore so that its B-tree pages are split against the page budget
 * the store is opened with, rather than the one it was written with.
 * <p>
 * A page holds as many records as fit its byte budget, so a store written with a
 * budget too small for its records ends up with one record per page. That is
 * permanent: {@link MVStoreTool#compact} copies live pages verbatim, so it
 * reclaims garbage and keeps the layout. Only writing every record into a fresh
 * store re-splits the pages, which is what this does.
 *
 * @see NitriteDatabase#DEFAULT_PAGE_SPLIT_SIZE_BYTE
 */
@Slf4j
public class PageLayout {

    /**
     * Unsaved pages MVStore may hold while a rewrite fills the target, in KiB.
     * <p>
     * This is what bounds the rewrite's footprint: {@code MVStore.beforeWrite} stores
     * inline once the unsaved memory passes this, so a store of tens of GiB is written
     * as a sequence of chunks rather than accumulated in the heap. Set explicitly
     * because it has to be a deliberate number here - {@code autoCommitDisabled()} only
     * stops the background writer thread, it does not disable this buffer (its
     * {@code autoCommitBufferSize} line is commented out upstream), so leaving it
     * unset would silently bound the rewrite by MVStore's own ~19 MiB default.
     */
    private static final int REWRITE_COMMIT_BUFFER_KIB = 32 * 1024;

    private PageLayout() {
    }

    /**
     * Rewrite every map of {@code source} into a new store at {@code target}, so its
     * pages are split against {@code budgetByte} rather than copied as they are.
     * <p>
     * Mirrors what {@link MVStore#compact(String, String, boolean, org.h2.mvstore.FileStore)}
     * does, except that records are {@code put} into the target instead of having
     * their pages copied - which is the whole point, and also why this cannot be
     * expressed as an option to the tool. Two things h2's own copy does are not
     * reachable from outside the package and are therefore not reproduced: the source's
     * commit version ({@code MVStore.setCurrentVersion}), so the rewritten store starts
     * counting again - see {@link NitriteDatabase#compact()} for who has to care - and
     * the per-map metadata entry, which does not matter here because Nitrite opens every
     * map with the default {@link MVMap.Builder} and so does this.
     */
    public static void rewrite(Path source, Path target, int budgetByte, boolean compress, int cacheSizeMiB) {
        long entries = 0;
        long start = System.currentTimeMillis();
        MVStore.Builder targetBuilder = new MVStore.Builder()
                .fileName(target.toString())
                .pageSplitSize(budgetByte)
                .cacheSize(Math.max(1, cacheSizeMiB))
                .autoCommitBufferSize(REWRITE_COMMIT_BUFFER_KIB)
                .autoCommitDisabled();
        if (compress) targetBuilder = targetBuilder.compress();

        try (MVStore in = new MVStore.Builder().fileName(source.toString()).readOnly()
                .cacheSize(Math.max(1, cacheSizeMiB)).open();
             MVStore out = targetBuilder.open()) {
            // do not let MVStore collect chunks while the file is still being filled
            out.setReuseSpace(false);

            // carry over the store settings, but not the map ids - those are assigned as maps are opened
            MVMap<String, String> sourceMeta = in.getMetaMap();
            MVMap<String, String> targetMeta = out.getMetaMap();
            for (Map.Entry<String, String> e : sourceMeta.entrySet()) {
                if (!e.getKey().startsWith("map.") && !e.getKey().startsWith("name.")) {
                    targetMeta.put(e.getKey(), e.getValue());
                }
            }

            for (String name : in.getMapNames()) {
                MVMap<Object, Object> from = in.openMap(name);
                MVMap<Object, Object> to = out.openMap(name);
                for (Map.Entry<Object, Object> e : from.entrySet()) {
                    to.put(e.getKey(), e.getValue());
                    entries++;
                }
            }

            // Record the budget the target actually got, which is the point of the rewrite and has to be
            // set after the settings above: the copy loop carries the source's marker over, and the
            // effective budget may be below the requested one anyway (see DEFAULT_PAGE_SPLIT_SIZE_BYTE).
            out.setStoreVersion(NitriteDatabase.effectivePageBudget(out));
            out.commit();
        }
        log.info("Rewrote {} records of '{}' with a {} byte page budget in {} ms",
                entries, source.getFileName(), budgetByte, System.currentTimeMillis() - start);
    }
}
