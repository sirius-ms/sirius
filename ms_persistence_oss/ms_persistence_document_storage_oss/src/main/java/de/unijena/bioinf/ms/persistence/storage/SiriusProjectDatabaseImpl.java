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
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectMaximumIntensities;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            ProjectMaximumIntensities derived = new ProjectMaximumIntensities();
            try (Stream<Feature> features = getStorage().findAllStr(Feature.class)) {
                features.forEach(f -> derived.fold(List.of(f)));
            }
            upsertProjectMaximumIntensities(derived);
            maxima = derived;
            maximaAwaitDerivation = false;
            return derived;
        }
    }

    private Set<Class<?>> getRelatedToAF() {
        if (relatedToAF == null) {
            relatedToAF = getStorage().getAllRegisteredClasses().stream().filter(
                    clazz -> FieldUtils.getAllFieldsList(clazz).stream().anyMatch(field -> field.getName().equals("alignedFeatureId"))
            ).collect(Collectors.toSet());
        }
        return relatedToAF;
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
        total += getStorage().removeAll(Filter.where("compoundId").eq(compoundId), Compound.class);
        return total;
    }

    @Override
    public long cascadeDeleteAlignedFeatures(long alignedFeatureId) throws IOException {
        return this.getStorage().write(() -> {
            long count = 0;
            for (Class<?> clazz : getRelatedToAF()) {
                count += getStorage().removeAll(Filter.where("alignedFeatureId").eq(alignedFeatureId), clazz);
            }
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
                for (Class<?> clazz : getRelatedToAF()) {
                    for (Long afid : ascendingIds)
                        count += getStorage().removeAll(Filter.where("alignedFeatureId").eq(afid), clazz);
                }
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
