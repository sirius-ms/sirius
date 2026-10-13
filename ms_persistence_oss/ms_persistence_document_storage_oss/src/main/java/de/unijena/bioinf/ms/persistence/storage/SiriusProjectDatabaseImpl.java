/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.CorrelatedIonPair;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SiriusProjectDatabaseImpl<Storage extends Database<?>> implements SiriusProjectDocumentDatabase<Storage>, Closeable, AutoCloseable {
    public static Metadata buildMetadata() {
        Metadata metadata = Metadata.build();
        MsProjectDocumentDatabase.buildMetadata(metadata);
        NetworkingProjectDocumentDatabase.buildMetadata(metadata);
        StatsAndTaggingSupport.buildMetadata(metadata);
        SiriusProjectDocumentDatabase.buildMetadata(metadata);
        return metadata;
    }

    protected final Storage storage;

    private static Set<Class<?>> relatedToAF = null;

    public SiriusProjectDatabaseImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }

    @Override
    public void close() throws IOException {
        storage.close();
    }

    private Set<Class<?>> getRelatedToAF() {
        if (relatedToAF == null) {
            relatedToAF = getStorage().getAllRegisteredClasses().stream().filter(
                    clazz -> FieldUtils.getAllFieldsList(clazz).stream().anyMatch(field -> field.getName().equals("alignedFeatureId"))
            ).collect(Collectors.toSet());
        }
        return relatedToAF;
    }

    /**
     * Deletes the compound if the delete that just happened left it with no feature to reference it any more,
     * taking its CorrelatedIonPairs down with it - a correlation has no meaning once its compound is gone.
     * Folds both counts into the result.
     */
    private long deleteIfOrphaned(long compoundId) throws IOException {
        if (getStorage().count(Filter.where("compoundId").eq(compoundId), AlignedFeatures.class) == 0) {
            long count = getStorage().removeAll(Filter.where("compoundId").eq(compoundId), CorrelatedIonPair.class);
            return count + getStorage().removeByPrimaryKey(compoundId, Compound.class);
        }
        return 0;
    }

    /**
     * Deletes the CorrelatedIonPairs naming this feature on either side. Two indexed point deletes, not one
     * `or` filter: an `or` is what forces Nitrite off the indexed path (see the comment on the batch overload
     * of cascadeDeleteAlignedFeatures below).
     */
    private long deleteCorrelatedIonPairsOf(long alignedFeatureId) throws IOException {
        long count = getStorage().removeAll(Filter.where("alignedFeatureId1").eq(alignedFeatureId), CorrelatedIonPair.class);
        return count + getStorage().removeAll(Filter.where("alignedFeatureId2").eq(alignedFeatureId), CorrelatedIonPair.class);
    }

    @Override
    public long cascadeDeleteCompound(long compoundId) throws IOException {
        long total = this.getStorage().write(() -> {
            long count = 0;
            for (AlignedFeatures f : getStorage().find(Filter.where("compoundId").eq(compoundId), AlignedFeatures.class)) {
                count += cascadeDeleteAlignedFeatures(f.getAlignedFeatureId());
            }
            return count;
        });
        // Still needed even though deleteIfOrphaned above already deletes the compound once its last feature
        // goes: a compound with no features to begin with never drives that check, so it needs its own cleanup.
        total += getStorage().removeAll(Filter.where("compoundId").eq(compoundId), CorrelatedIonPair.class);
        total += getStorage().removeAll(Filter.where("compoundId").eq(compoundId), Compound.class);
        return total;
    }

    @Override
    public long cascadeDeleteAlignedFeatures(long alignedFeatureId) throws IOException {
        return this.getStorage().write(() -> {
            // Read before the delete loop removes the feature's own row and takes compoundId down with it.
            Long compoundId = getStorage().getByPrimaryKey(alignedFeatureId, AlignedFeatures.class)
                    .map(AlignedFeatures::getCompoundId).orElse(null);
            long count = deleteCorrelatedIonPairsOf(alignedFeatureId);
            for (Class<?> clazz : getRelatedToAF()) {
                count += getStorage().removeAll(Filter.where("alignedFeatureId").eq(alignedFeatureId), clazz);
            }
            if (compoundId != null)
                count += deleteIfOrphaned(compoundId);
            return count;
        });
    }


    @Override
    public long cascadeDeleteAlignedFeatures(List<Long> alignedFeatureIds) throws IOException {
        if (alignedFeatureIds.isEmpty())
            return 0;
        if (alignedFeatureIds.size() == 1)
            return cascadeDeleteAlignedFeatures(alignedFeatureIds.getFirst());

        // Nitrite's remove(filter) runs readOperations.find(filter); for `in`/`or` filters the optimizer picks NO
        // index (indexDescriptor==null -> nitriteMap.entries() full scan + per-doc deserialization). A single `eq`
        // on an indexed field DOES take the IndexedStream path (the same fast path the GUI uses to load a feature's
        // results), so remove per id with `eq` instead of one `in`/`or` over the whole selection.
        // Ascending, because neighbouring ids share index and document pages: deleting them one after the
        // other pays for each page once instead of coming back to it after the commit buffer has written it
        // out. Measured on 5000 features (88,597 documents): 15.0 s and 75.3 MiB of growth unsorted, 11.5 s
        // and 54.0 MiB sorted. The nesting below is already the right way round for the same reason - one
        // collection at a time, rather than one feature across all of them.
        List<Long> ascendingIds = new ArrayList<>(alignedFeatureIds);
        ascendingIds.sort(null);
        return this.getStorage().write(() -> {
            long count = 0;
            try {
                // Read every feature's compoundId before the delete loop below removes the feature rows that
                // carry it - a primitive long set, not boxed, since this scales with the input, not a constant.
                LongOpenHashSet compoundIds = new LongOpenHashSet();
                for (Long afid : ascendingIds) {
                    getStorage().getByPrimaryKey(afid, AlignedFeatures.class)
                            .map(AlignedFeatures::getCompoundId)
                            .ifPresent(compoundIds::add);
                    count += deleteCorrelatedIonPairsOf(afid);
                }
                for (Class<?> clazz : getRelatedToAF()) {
                    for (Long afid : ascendingIds)
                        count += getStorage().removeAll(Filter.where("alignedFeatureId").eq(afid), clazz);
                }
                LongIterator it = compoundIds.iterator();
                while (it.hasNext())
                    count += deleteIfOrphaned(it.nextLong());
            } finally {
                // Commit and GC once for the whole cascade, not per class: the entire write() runs under the
                // storage write lock as a single transaction, so a per-class commit buys no consistency and only
                // adds flush overhead. Doing it in finally still persists whatever was removed if the cascade
                // fails or is cancelled part way through, and lets MVStore reclaim the superseded copy-on-write
                // chunks accumulated during the bulk delete.
                getStorage().flush();
                System.gc();
            }
            return count;
        });
    }
}
