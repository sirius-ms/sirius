/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

import de.unijena.bioinf.ms.persistence.model.Tag;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedRun;
import de.unijena.bioinf.ms.persistence.model.core.run.Run;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.IndexType;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface MsProjectDocumentDatabase<Storage extends Database<?>> {


    static Metadata buildMetadata() throws IOException {
        return buildMetadata(Metadata.build());
    }

    static Metadata buildMetadata(@NotNull Metadata sourceMetadata) throws IOException {
        MetadataUtils.addFasUtilCollectionSupport(sourceMetadata);
        return sourceMetadata
                .addRepository(Tag.class, new Index("name", IndexType.UNIQUE))

                .addRepository(Run.class,
                        new Index("name", IndexType.NON_UNIQUE),
                        new Index("runType", IndexType.NON_UNIQUE))

                .addRepository(MergedRun.class,
                        new Index("name", IndexType.NON_UNIQUE),
                        new Index("runType", IndexType.NON_UNIQUE))

                .addRepository(Scan.class,
                        new Index("runId", IndexType.NON_UNIQUE),
                        new Index("scanTime", IndexType.NON_UNIQUE))
                .setOptionalFields(Scan.class, "peaks")

                .addRepository(MSMSScan.class,
                        new Index("runId", IndexType.NON_UNIQUE),
                        new Index("scanTime", IndexType.NON_UNIQUE),
                        new Index("precursorScanId", IndexType.NON_UNIQUE))
                .setOptionalFields(MSMSScan.class, "peaks")

                .addRepository(MergedTrace.class)

                .addRepository(SourceTrace.class)

                .addRepository(Feature.class,
                        new Index("alignedFeatureId", IndexType.NON_UNIQUE),
                        new Index("averageMass", IndexType.NON_UNIQUE),
                        new Index("apexMass", IndexType.NON_UNIQUE),
                        new Index("retentionTime.start", IndexType.NON_UNIQUE),
                        new Index("retentionTime.end", IndexType.NON_UNIQUE))

                .addRepository(AlignedFeatures.class,
                        new Index("compoundId", IndexType.NON_UNIQUE),
                        new Index("averageMass", IndexType.NON_UNIQUE),
                        new Index("apexMass", IndexType.NON_UNIQUE),
                        new Index("retentionTime.start", IndexType.NON_UNIQUE),
                        new Index("retentionTime.end", IndexType.NON_UNIQUE))

                .addRepository(AlignedIsotopicFeatures.class,
                        new Index("alignedFeatureId", IndexType.NON_UNIQUE),
                        new Index("averageMass", IndexType.NON_UNIQUE),
                        new Index("apexMass", IndexType.NON_UNIQUE),
                        new Index("retentionTime.start", IndexType.NON_UNIQUE),
                        new Index("retentionTime.end", IndexType.NON_UNIQUE))

                .addRepository(CorrelatedIonPair.class,
                        new Index("alignedFeatureId1", IndexType.NON_UNIQUE),
                        new Index("alignedFeatureId2", IndexType.NON_UNIQUE),
                        new Index("type", IndexType.NON_UNIQUE))

                .addRepository(Compound.class,
                        new Index("name", IndexType.NON_UNIQUE),
                        new Index("neutralMass", IndexType.NON_UNIQUE),
                        new Index("rt.start", IndexType.NON_UNIQUE),
                        new Index("rt.end", IndexType.NON_UNIQUE));
    }

    default Stream<Compound> getAllCompounds() throws IOException {
        return getStorage().findAllStr(Compound.class);
    }

    default void fetchAdductFeatures(@NotNull final Compound compound) {
        try {
            getStorage().fetchAllChildren(compound, "compoundId", "adductFeatures", AlignedFeatures.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void fetchCorrelatedIonPairs(@NotNull final Compound compound) {
        try {
            getStorage().fetchAllChildren(compound, "compoundId", "correlatedIonPairs", CorrelatedIonPair.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default Stream<AlignedFeatures> getAllAlignedFeatures() throws IOException {
        return getStorage().findAllStr(AlignedFeatures.class);
    }

    default <A extends AbstractAlignedFeatures> void fetchFeatures(@NotNull final A alignedFeatures) {
        try {
            getStorage().fetchAllChildren(alignedFeatures, "alignedFeatureId", "features", Feature.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void importCompounds(List<Compound> compounds) throws IOException {
        getStorage().insertAll(compounds);

        for (Compound c : compounds) {
            c.setAdductFeatureIds(importOptionals(c.getAdductFeatures(), c.getCompoundId(), this::importAlignedFeatures));
            c.setCorrelatedIonPairIds(importOptionals(c.getCorrelatedIonPairs(), c.getCompoundId(), this::importCorrelatedIonPairs));
        }

        getStorage().upsertAll(compounds);
    }

    default LongList importCorrelatedIonPairs(List<CorrelatedIonPair> ionPairs, long parentId) throws IOException {
        List<CorrelatedIonPair> pairs = ionPairs.stream().filter(pair -> pair.getAlignedFeatures1().isPresent() && pair.getAlignedFeatures2().isPresent()).peek(pair -> {
            pair.setCompoundId(parentId);
            pair.setAlignedFeatureId1(pair.getAlignedFeatures1().get().getAlignedFeatureId());
            pair.setAlignedFeatureId2(pair.getAlignedFeatures2().get().getAlignedFeatureId());
        }).toList();
        getStorage().insertAll(pairs);
        return LongArrayList.toListWithExpectedSize(pairs.stream().mapToLong(CorrelatedIonPair::getIonPairId), pairs.size());
    }

    default LongList importAlignedFeatures(List<AlignedFeatures> featureAlignments, long parentId) throws IOException {
        for (AlignedFeatures f : featureAlignments) {
            f.setCompoundId(parentId);
        }
        return importAlignedFeatures(featureAlignments);
    }

    default LongList importAlignedFeatures(List<AlignedFeatures> featureAlignments) throws IOException {
        getStorage().insertAll(featureAlignments);

        for (AlignedFeatures f : featureAlignments) {
            f.setIsotopicFeaturesIds(importOptionals(f.getIsotopicFeatures(), f.getAlignedFeatureId(), this::importAlignedIsotopicFeatures));
            f.setFeatureIds(importOptionals(f.getFeatures(), f.getAlignedFeatureId(), this::importFeatures));
        }

        getStorage().upsertAll(featureAlignments);
        return LongArrayList.toListWithExpectedSize(featureAlignments.stream().mapToLong(AlignedFeatures::getAlignedFeatureId), featureAlignments.size());
    }

    default LongList importAlignedIsotopicFeatures(List<AlignedIsotopicFeatures> isotopicFeatureAlignments, long parentId) throws IOException {
        for (AlignedIsotopicFeatures f : isotopicFeatureAlignments) {
            f.setAlignedFeatureId(parentId);
        }
        getStorage().insertAll(isotopicFeatureAlignments);

        for (AlignedIsotopicFeatures f : isotopicFeatureAlignments) {
            f.setFeatureIds(importOptionals(f.getFeatures(), f.getAlignedIsotopeFeatureId(), this::importFeatures));
        }

        getStorage().upsertAll(isotopicFeatureAlignments);
        return LongArrayList.toListWithExpectedSize(isotopicFeatureAlignments.stream().mapToLong(AlignedIsotopicFeatures::getAlignedIsotopeFeatureId), isotopicFeatureAlignments.size());
    }

    default LongList importFeatures(List<Feature> features, long parentId) throws IOException {
        for (Feature f : features) {
            f.setAlignedFeatureId(parentId);
        }

        getStorage().insertAll(features);
        return LongArrayList.toListWithExpectedSize(features.stream().mapToLong(Feature::getFeatureId), features.size());
    }

    private <T> LongList importOptionals(Optional<List<T>> optionals, long parentId, IOThrowingBiFunction<List<T>, Long, LongList> importer) throws IOException {
        if (optionals.isPresent()) {
            return importer.apply(optionals.get(), parentId);
        } else {
            return LongList.of();
        }
    }

    @FunctionalInterface
    interface IOThrowingBiFunction<T, U, R> {

        R apply(T object1, U object2) throws IOException;

    }

    Storage getStorage();
}
