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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.CorrelatedIonPair;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectMaximumIntensities;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // Deriving the maximum intensities and folding written features into them read the stored value, change it
    // and write it back. Both run under this lock, or two writers would each fold into the same old value and
    // the second write would erase the first one's increase.
    private final Object maximaLock = new Object();
    // The stored maxima, read once; null while they have not been derived. Replaced, never changed in place,
    // so a reader holding one never sees it change.
    private volatile ProjectMaximumIntensities maxima;
    // Guarded by maximaLock: whether the stored state has been read into the field above.
    private boolean maximaRead;
    // Guarded by maximaLock: the project had features before it tracked its maxima, so a write cannot start
    // them - it would leave out everything written earlier. They wait for getMaximumIntensities to be derived.
    private boolean maximaAwaitDerivation;

    private ProjectMaximumIntensities readMaximaLocked() {
        if (!maximaRead) {
            maxima = findProjectMaximumIntensities().orElse(null);
            maximaRead = true;
        }
        return maxima;
    }

    @Override
    public void featuresWritten(@NotNull List<Feature> features) throws IOException {
        if (features.isEmpty())
            return;
        synchronized (maximaLock) {
            ProjectMaximumIntensities current = readMaximaLocked();
            if (current == null) {
                if (maximaAwaitDerivation)
                    return;
                // Not tracked yet. A project whose only features are these is a new one, and starts tracking
                // with them - otherwise its first reader would pay the scan an old project pays.
                if (getStorage().countAll(Feature.class) != features.size()) {
                    maximaAwaitDerivation = true;
                    return;
                }
                current = new ProjectMaximumIntensities();
                current.fold(features);
                upsertProjectMaximumIntensities(current);
                maxima = current;
                return;
            }
            ProjectMaximumIntensities next = current.copy();
            if (next.fold(features)) {
                upsertProjectMaximumIntensities(next);
                maxima = next;
            }
        }
    }

    @SneakyThrows
    @Override
    public @NotNull ProjectMaximumIntensities getMaximumIntensities() {
        ProjectMaximumIntensities known = maxima;
        if (known != null)
            return known;
        synchronized (maximaLock) {
            known = readMaximaLocked();
            if (known != null)
                return known;
            ProjectMaximumIntensities derived = deriveMaxima();
            upsertProjectMaximumIntensities(derived);
            maxima = derived;
            maximaAwaitDerivation = false;
            return derived;
        }
    }

    /** How many alignments a worker takes from the shared cursor at a time. */
    private static final int ALIGNMENTS_PER_CHUNK = 128;

    /**
     * Folds every stored feature into fresh maxima, read alignment by alignment: the keys of the aligned and of
     * the isotopic-aligned features - whose run features are stored under the isotopic alignment's own id, and
     * are folded in on import like any other - handed out in chunks from a shared cursor to one
     * {@code JobType.CPU} sub-job per core, each fetching a chunk's run features over the alignedFeatureId index
     * and folding them into maxima of its own, merged at the end.
     * <p>
     * Alignments rather than the run features' own keys: there are far fewer of them, so reading the keys to hand
     * out costs a fraction. A shared cursor rather than one contiguous block of chunks per worker: workers taking
     * neighbouring chunks in order read neighbouring records and share the pages they decode, where a block per
     * worker has each of them decode pages of its own. Both were measured against the alternatives and against a
     * single-threaded walk of the collection, on a large LC-MS project read cold, and both win clearly. This runs
     * the first time an older project's maxima are asked for - which is while its search index is being built,
     * before anything is written to it.
     */
    private ProjectMaximumIntensities deriveMaxima() throws Exception {
        List<Object> keys = new ArrayList<>(getStorage().primaryKeys(AlignedFeatures.class));
        keys.addAll(getStorage().primaryKeys(AlignedIsotopicFeatures.class));
        if (keys.isEmpty())
            return new ProjectMaximumIntensities();
        int workers = Math.clamp(SiriusJobs.getGlobalJobManager().getCPUThreads(), 1,
                (keys.size() + ALIGNMENTS_PER_CHUNK - 1) / ALIGNMENTS_PER_CHUNK);
        AtomicInteger cursor = new AtomicInteger();

        return SiriusJobs.getGlobalJobManager().submitJob(new BasicMasterJJob<ProjectMaximumIntensities>(JJob.JobType.CPU) {
            @Override
            protected ProjectMaximumIntensities compute() throws Exception {
                List<BasicJJob<ProjectMaximumIntensities>> running = new ArrayList<>(workers);
                for (int worker = 0; worker < workers; worker++)
                    running.add(submitSubJob(new BasicJJob<>(JobType.CPU) {
                        @Override
                        protected ProjectMaximumIntensities compute() throws Exception {
                            ProjectMaximumIntensities part = new ProjectMaximumIntensities();
                            int from;
                            while ((from = cursor.getAndAdd(ALIGNMENTS_PER_CHUNK)) < keys.size()) {
                                checkForInterruption();
                                try (Stream<Feature> features = getStorage().findAllByIndexedFieldStr("alignedFeatureId",
                                        keys.subList(from, Math.min(keys.size(), from + ALIGNMENTS_PER_CHUNK)), Feature.class)) {
                                    features.forEach(f -> part.fold(List.of(f)));
                                }
                            }
                            return part;
                        }
                    }));
                ProjectMaximumIntensities derived = new ProjectMaximumIntensities();
                for (BasicJJob<ProjectMaximumIntensities> part : running)
                    derived.merge(part.awaitResult());
                return derived;
            }
        }).awaitResult();
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
