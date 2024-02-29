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
import de.unijena.bioinf.ms.persistence.model.core.*;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.run.Run;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.storage.db.nosql.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
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
                        new Index("name", IndexType.UNIQUE),
                        new Index("runType", IndexType.NON_UNIQUE))

                .addRepository(Scan.class,
                        new Index("runId", IndexType.NON_UNIQUE),
                        new Index("scanTime", IndexType.NON_UNIQUE))
//                .setOptionalFields(Scan.class, "peaks") //todo needed as optional?

                .addRepository(MSMSScan.class,
                        new Index("runId", IndexType.NON_UNIQUE),
                        new Index("scanTime", IndexType.NON_UNIQUE),
//                        new Index("collisionEnergy.minEnergySource", IndexType.NON_UNIQUE), //todo needed?
//                        new Index("collisionEnergy.maxEnergySource", IndexType.NON_UNIQUE), //todo needed?
                        new Index("precursorScanId", IndexType.NON_UNIQUE))
//                .setOptionalFields(MSMSScan.class, "peaks")  //todo needed as optional?

                .addRepository(MergedTrace.class)

                .addRepository(SourceTrace.class)

                .addRepository(Feature.class,
                        new Index("alignedFeatureId", IndexType.NON_UNIQUE),
                        new Index("blank", IndexType.NON_UNIQUE))
                .setOptionalFields(Feature.class, "traceRefs")

                .addRepository(AlignedFeatures.class,
                        new Index("compoundId", IndexType.NON_UNIQUE),
                        new Index("mergedIonMass", IndexType.NON_UNIQUE)) //todo really needed?
//                        new Index("mergedRT.start", IndexType.NON_UNIQUE), //todo really needed?
//                        new Index("mergedRT.end", IndexType.NON_UNIQUE))  //todo really needed?
                .setOptionalFields(AlignedFeatures.class, "traceRefs")
//                .setOptionalFields(AlignedFeatures.class, "topAnnotation", "manualAnnotation")

                .addRepository(AlignedIsotopicFeatures.class)

                .addRepository(CorrelatedIonPair.class,
                        new Index("alignedFeatureId1", IndexType.NON_UNIQUE),
                        new Index("alignedFeatureId2", IndexType.NON_UNIQUE),
                        new Index("type", IndexType.NON_UNIQUE))

                .addRepository(Compound.class,
                        new Index("neutralMass", IndexType.NON_UNIQUE)); //todo really needed?
//                        new Index("mergedRT.start", IndexType.NON_UNIQUE), //todo really needed?
//                        new Index("mergedRT.end", IndexType.NON_UNIQUE))  //todo really needed?
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
            getStorage().fetchChildren(
                    compound, "correlatedIonPairs", Filter.build()
                            .inLong("ionPairId", compound.getCorrelatedIonPairIds().toLongArray()), CorrelatedIonPair.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default Stream<AlignedFeatures> getAllAlignedFeatures() throws IOException {
        return getStorage().findAllStr(AlignedFeatures.class);
    }

    default void fetchFeatures(@NotNull final AlignedFeatures alignedFeatures) {
        try {
            getStorage().fetchAllChildren(alignedFeatures, "alignedFeatureId", "features", OldFeature.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void fetchMsmsScans(@NotNull final AlignedFeatures alignedFeatures) {
        // FIXME
//        if (alignedFeatures.getFeatures().isEmpty())
//            fetchFeatures(alignedFeatures);
//
//        alignedFeatures.getFeatures().ifPresent(fs -> fs.stream().filter(f -> f.getMsms().isEmpty())
//                .forEach(this::fetchMsmsScans));
    }

    default void fetchMsmsScans(@NotNull final OldFeature feature) {
        try {
            getStorage().fetchAllChildren(feature, "featureId", "msms", MSMSScan.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void fetchApexScans(@NotNull final OldFeature feature) {
        try {
            getStorage().fetchAllChildren(feature, "apexScanId", "scanId", "apexScan", Scan.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void importCompounds(List<Compound> compounds) throws IOException {
        for (Compound c : compounds) {
            if (c.getCorrelatedIonPairs().isPresent()){
                getStorage().insertAll(c.getCorrelatedIonPairs().get());
                c.getCorrelatedIonPairs()
                        .map(l -> l.stream().map(CorrelatedIonPair::getIonPairId))
                        .map(s -> s.collect(Collectors.toCollection(LongArrayList::new)))
                        .ifPresent(c::setCorrelatedIonPairIds);
            }
        }

        getStorage().insertAll(compounds);

        List<AlignedFeatures> features = compounds.stream()
                .peek(c -> c.getAdductFeatures().ifPresent(fs -> fs.forEach(f -> f.setCompoundId(c.getCompoundId()))))
                .flatMap(f -> f.getAdductFeatures().stream().flatMap(List::stream)).toList();
        importAlignedFeatures(features);
    }

    default void importAlignedFeatures(List<AlignedFeatures> featureAlignments) throws IOException {
        // FIXME
//        getStorage().insertAll(featureAlignments);
//        List<Feature> features = featureAlignments.stream()
//                .peek(fa -> fa.getFeatures().ifPresent(fs -> fs.forEach(f -> f.setAlignedFeatureId(fa.getAlignedFeatureId()))))
//                .flatMap(f -> f.getFeatures().stream().flatMap(List::stream)).toList();
//        importFeatures(features);
    }

    default void importFeatures(List<OldFeature> features) throws IOException {
        for (OldFeature f : features) {
            if (f.getApexScan().isPresent()){
                getStorage().insert(f.getApexScan().get());
                f.getApexScan().map(Scan::getScanId).ifPresent(f::setApexScanId);
            }
        }

        getStorage().insertAll(features);

        List<MSMSScan> msmsToAdd = features.stream()
                .peek(f -> f.getMsms().ifPresent(msms -> msms.forEach(scan -> scan.setFeatureId(f.getFeatureId()))))
                .flatMap(f -> f.getMsms().stream().flatMap(List::stream)).toList();

        getStorage().insertAll(msmsToAdd);
    }

    Storage getStorage();
}
