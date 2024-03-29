/*
 *
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

import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.StandardFingerprintData;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.JSONReader;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CanopusPredictionDeserializer;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CsiPredictionDeserializer;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

public interface SiriusProjectDocumentDatabase<Storage extends Database<?>> extends NetworkingProjectDocumentDatabase<Storage> {
    String SIRIUS_PROJECT_SUFFIX = ".sirius";
    String FP_DATA_COLLECTION = "FP_DATA";

    static Metadata buildMetadata() throws IOException {
        return buildMetadata(Metadata.build());
    }

    //todo merge msNovelist candidate with normal candidates so they can be stored together
    //todo update links of existing candidates with msnovelist flag!
    //todo import data from MsExperiment
    //todo provide feature as MsExperiment for computation
    static Metadata buildMetadata(@NotNull Metadata sourceMetadata) throws IOException {
        NetworkingProjectDocumentDatabase.buildMetadata(sourceMetadata)
                .addCollection(FP_DATA_COLLECTION, Index.unique("type", "charge"))

                .addRepository(Parameters.class, Index.unique("alignedFeatureId", "type"))

                .addRepository(FTreeResult.class,
                        Index.unique("formulaId"),
                        Index.nonUnique("alignedFeatureId")) //todo needed? //if we want to search for a formulaId the alignedFeatureId is implicitly known.

                .addRepository(CsiPrediction.class,
                        Index.unique("formulaId"),
                        Index.nonUnique("alignedFeatureId"))  //todo needed? //if we want to search for a formulaId the alignedFeatureId is implicitly known.
                .addDeserializer(CsiPrediction.class,
                        new CsiPredictionDeserializer())

                .addRepository(CanopusPrediction.class,
                        Index.unique("formulaId"),
                        Index.nonUnique("alignedFeatureId"))  //todo needed? //if we want to search for a formulaId the alignedFeatureId is implicitly known.
                .addDeserializer(CanopusPrediction.class,
                        new CanopusPredictionDeserializer())

                .addRepository(CsiStructureSearchResult.class, "alignedFeatureId")

                .addRepository(CsiStructureMatch.class,
                        Index.nonUnique("formulaId"),
                        Index.nonUnique("alignedFeatureId"))

                .addRepository(DenovoStructureMatch.class,
                        Index.nonUnique("formulaId"),
                        Index.nonUnique("alignedFeatureId"))

                .addRepository(SpectraMatch.class, "uuid",
                        Index.nonUnique("candidateInChiKey"),
                        Index.nonUnique("alignedFeatureId"))

                .addRepository(FingerprintCandidate.class)
                .addSerialization(FingerprintCandidate.class,
                        new FingerprintCandidate.Serializer(),
                        new JSONReader.FingerprintCandidateDeserializer(null)) //will be added later because it has to be read from project

                .addSerialization(ProbabilityFingerprint.class,
                        new ProbabilityFingerprint.Serializer(),
                        new ProbabilityFingerprint.Deserializer()) // version needs to be added later
        ;

        return sourceMetadata;
    }

    void insertFingerprintData(StandardFingerprintData<?> fpData, int charge);

    void insertFingerprintData(FingerIdData fpData, int charge);

    <T extends FingerprintData<?>> Optional<T> findFingerprintData(Class<T> dataClazz, int charge);

    default Optional<CsiStructureSearchResult> findCsiStructureSearchResult(long alignedFeatureId, boolean includeStructureMatches) {
        try {
            Optional<CsiStructureSearchResult> result = getStorage().getByPrimaryKey(alignedFeatureId, CsiStructureSearchResult.class);
            if (includeStructureMatches && result.isPresent())
                getStorage().fetchAllChildren(result.get(), "alignedFeatureId", "matches", CsiStructureMatch.class);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
