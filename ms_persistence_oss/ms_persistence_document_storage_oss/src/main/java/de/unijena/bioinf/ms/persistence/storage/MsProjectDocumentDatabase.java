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

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.persistence.model.Tag;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface MsProjectDocumentDatabase<Storage extends Database<?>> {


    static Metadata buildMetadata() throws IOException {
        return buildMetadata(Metadata.build());
    }

    static Metadata buildMetadata(@NotNull Metadata sourceMetadata) throws IOException {
        MetadataUtils.addFasUtilCollectionSupport(sourceMetadata);
        return sourceMetadata
                .addRepository(Tag.class, Index.unique("name"))

                .addRepository(LCMSRun.class,
                        Index.nonUnique("name"))

                .addRepository(MergedLCMSRun.class, // TODO: das ist doof -_- Wozu überhaupt zwischen beidem unterscheiden?
                        Index.nonUnique("name"))

                .addRepository(Scan.class,
                        Index.nonUnique("runId", "scanTime"))
                .setOptionalFields(Scan.class, "peaks")

                .addRepository(MSMSScan.class,
                        Index.nonUnique("runId", "scanTime"),
                        Index.nonUnique("precursorScanId"))
                .setOptionalFields(MSMSScan.class, "peaks")

                .addRepository(MergedTrace.class)

                .addRepository(SourceTrace.class)

                .addRepository(MSData.class)

                .addRepository(Feature.class,
                        Index.nonUnique("alignedFeatureId")
                )

                .addRepository(AlignedFeatures.class,
                        Index.nonUnique("compoundId"),
                        Index.nonUnique("averageMass"),
                        Index.nonUnique("retentionTime.middle")
                )

                .addRepository(AlignedIsotopicFeatures.class,
                        Index.nonUnique("alignedFeatureId")
                )

                .addRepository(CorrelatedIonPair.class,
                        Index.nonUnique("alignedFeatureId1"),
                        Index.nonUnique("alignedFeatureId2"),
                        Index.nonUnique("type")
                )

                .addRepository(Compound.class,
                        Index.nonUnique("name"),
                        Index.nonUnique("neutralMass"),
                        Index.nonUnique("rt.middle"))

                .addRepository(RetentionTimeAxis.class)

                .addRepository(QualityReport.class)

                ;


    }

    default <T> Stream<T> stream(Iterable<T> iterable){
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    default Stream<Compound> getAllCompounds() throws IOException {
        return getStorage().findAllStr(Compound.class);
    }

    @SneakyThrows
    default Compound fetchAdductFeatures(@NotNull final Compound compound) {
        getStorage().fetchAllChildren(compound, "compoundId", "adductFeatures", AlignedFeatures.class);
        return compound;
    }

    @SneakyThrows
    default Compound fetchCorrelatedIonPairs(@NotNull final Compound compound) {
        getStorage().fetchAllChildren(compound, "compoundId", "correlatedIonPairs", CorrelatedIonPair.class);
        return compound;
    }

    default Stream<AlignedFeatures> getAllAlignedFeatures() throws IOException {
        return getStorage().findAllStr(AlignedFeatures.class);
    }

    @SneakyThrows
    default <A extends AbstractAlignedFeatures> A fetchFeatures(@NotNull final A alignedFeatures) {
        getStorage().fetchAllChildren(alignedFeatures, "alignedFeatureId", "features", Feature.class);
        return alignedFeatures;
    }

    @SneakyThrows
    default <A extends AlignedFeatures> A fetchIsotopicFeatures(@NotNull final A alignedFeatures) {
        getStorage().fetchAllChildren(alignedFeatures, "alignedFeatureId", "alignedFeatureId", "isotopicFeatures", AlignedIsotopicFeatures.class);
        return alignedFeatures;
    }

    @SneakyThrows
    default <A extends AbstractAlignedFeatures> A fetchMsData(@NotNull final A alignedFeatures) {
        getStorage().fetchChild(alignedFeatures, "alignedFeatureId", "msData", MSData.class);
        return alignedFeatures;
    }

    @SneakyThrows
    default MergedLCMSRun fetchLCMSRuns(@NotNull final MergedLCMSRun run) {
        Storage storage = getStorage();
        run.setRuns(Arrays.stream(run.getRunIds()).mapToObj(x-> {
            try {
                return storage.getByPrimaryKey(x, LCMSRun.class).orElse(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList());
        return run;
    }

    default void importCompounds(List<Compound> compounds) throws IOException {
        getStorage().insertAll(compounds);

        for (Compound c : compounds) {
            importOptionals(c.getAdductFeatures(), c.getCompoundId(), this::importAlignedFeatures);
            importOptionals(c.getCorrelatedIonPairs(), c.getCompoundId(), this::importCorrelatedIonPairs);
        }
    }

    default void importCorrelatedIonPairs(List<CorrelatedIonPair> ionPairs, long parentId) throws IOException {
        List<CorrelatedIonPair> pairs = ionPairs.stream().filter(pair -> pair.getAlignedFeatures1().isPresent() && pair.getAlignedFeatures2().isPresent()).peek(pair -> {
            pair.setCompoundId(parentId);
            pair.setAlignedFeatureId1(pair.getAlignedFeatures1().get().getAlignedFeatureId());
            pair.setAlignedFeatureId2(pair.getAlignedFeatures2().get().getAlignedFeatureId());
        }).toList();
        getStorage().insertAll(pairs);
    }

    default void importAlignedFeatures(List<AlignedFeatures> featureAlignments, long parentId) throws IOException {
        for (AlignedFeatures f : featureAlignments) {
            f.setCompoundId(parentId);
        }
        importAlignedFeatures(featureAlignments);
    }

    default void importAlignedFeatures(List<AlignedFeatures> featureAlignments) throws IOException {
        getStorage().insertAll(featureAlignments);
        for (AlignedFeatures f : featureAlignments) {
            importOptionals(f.getFeatures(), f.getAlignedFeatureId(), this::importFeatures);
            importOptionals(f.getIsotopicFeatures(), f.getAlignedFeatureId(), this::importAlignedIsotopicFeatures);
            if (f.getMSData().isPresent()) {
                importMSData(f.getMSData().get(), f.getAlignedFeatureId());
            } else {
                LoggerFactory.getLogger(getClass()).warn("Importing AlignedFeatures without MSData! Feature: {}| RT: {}| M/Z: {}", f.getName(), f.getRetentionTime(), f.getAverageMass());
            }
        }
    }

    default void importAlignedIsotopicFeatures(List<AlignedIsotopicFeatures> isotopicFeatureAlignments, long parentId) throws IOException {
        for (AlignedIsotopicFeatures f : isotopicFeatureAlignments) {
            f.setAlignedFeatureId(parentId);
        }
        getStorage().insertAll(isotopicFeatureAlignments);
        for (AlignedIsotopicFeatures f : isotopicFeatureAlignments) {
            importOptionals(f.getFeatures(), f.getAlignedIsotopeFeatureId(), this::importFeatures);
            if (f.getMSData().isPresent()) {
                importMSData(f.getMSData().get(), f.getAlignedIsotopeFeatureId());
            }
        }
    }

    default void importMSData(MSData msData, long parentId) throws IOException {
        msData.setAlignedFeatureId(parentId);
        getStorage().insert(msData);
    }

    default void importFeatures(List<Feature> features, long parentId) throws IOException {
        for (Feature f : features) {
            f.setAlignedFeatureId(parentId);
        }

        getStorage().insertAll(features);
    }

    private <T> void importOptionals(Optional<List<T>> optionals, long parentId, IOFunctions.BiIOConsumer<List<T>, Long> importer) throws IOException {
        if (optionals.isPresent()) {
            importer.accept(optionals.get(), parentId);
        }
    }

    Storage getStorage();
}
