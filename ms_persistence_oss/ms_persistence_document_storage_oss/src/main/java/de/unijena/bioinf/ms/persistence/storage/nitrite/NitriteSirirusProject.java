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

package de.unijena.bioinf.ms.persistence.storage.nitrite;

import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.StandardFingerprintData;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiPrediction;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CanopusPredictionDeserializer;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CsiPredictionDeserializer;
import de.unijena.bioinf.chemdb.nitrite.serializers.NitriteCompoundSerializers;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.dizitart.no2.collection.Document;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;


public class NitriteSirirusProject extends SiriusProjectDatabaseImpl<NitriteDatabase> {
    //this is just needed to update json serializers with fingerprint data if it arrives
    private final Metadata metadata;
    public NitriteSirirusProject(@NotNull Path location) throws IOException {
        this(location, SiriusProjectDocumentDatabase.buildMetadata(), NitriteDatabase.MVStoreCompression.DEFLATE); //highest compression rate
    }
    private NitriteSirirusProject(@NotNull Path location, @NotNull final Metadata metadata, NitriteDatabase.MVStoreCompression compression) throws IOException {
        this(location, metadata, compression, 256, 65536); //64Kib
    }
    private NitriteSirirusProject(@NotNull Path location, @NotNull final Metadata metadata, NitriteDatabase.MVStoreCompression compression, int cacheSizeMiB, int commitBufferByte) throws IOException {
        super(new NitriteDatabase(location, metadata, compression, cacheSizeMiB, commitBufferByte));
        this.metadata = metadata;
        updateSerializers();
    }

    private void updateSerializers() {
        synchronized (metadata) {
            Optional<FingerIdData> csiPos = findFingerprintData(FingerIdData.class, 1);
            Optional<FingerIdData> csiNeg = findFingerprintData(FingerIdData.class, -1);

            Optional<CanopusCfData> cfPos = findFingerprintData(CanopusCfData.class, 1);
            Optional<CanopusCfData> cfNeg = findFingerprintData(CanopusCfData.class, -1);

            Optional<CanopusNpcData> npcPos = findFingerprintData(CanopusNpcData.class, 1);
            Optional<CanopusNpcData> npcNeg = findFingerprintData(CanopusNpcData.class, -1);

            csiPos.ifPresent(data ->
                    ((NitriteCompoundSerializers.FingerprintCandidateDeserializer) metadata.deserializers.get(FingerprintCandidate.class))
                            .setVersion(data.getFingerprintVersion()));

            ((CsiPredictionDeserializer) metadata.deserializers.get(CsiPrediction.class)).setVersions(
                    csiPos.map(FingerprintData::getFingerprintVersion).orElse(null),
                    csiNeg.map(FingerprintData::getFingerprintVersion).orElse(null));


            ((CanopusPredictionDeserializer) metadata.deserializers.get(CanopusPrediction.class)).setVersions(
                    cfPos.map(FingerprintData::getFingerprintVersion).orElse(null),
                    cfNeg.map(FingerprintData::getFingerprintVersion).orElse(null),
                    npcPos.map(FingerprintData::getFingerprintVersion).orElse(null),
                    npcNeg.map(FingerprintData::getFingerprintVersion).orElse(null));
        }
    }

    private <T extends FingerprintData<?>> Optional<T> findFpData(@NotNull Class<T> clzz, int charge, @NotNull Function<Document, T> expander) throws IOException {
        return getStorage().findStr(FP_DATA_COLLECTION, Filter.and(
                        Filter.where("type").eq(clzz.getSimpleName()),
                        Filter.where("charge").eq(charge)))
                .findFirst().map(expander);
    }

    @Override
    public <T extends FingerprintData<?>> Optional<T> findFingerprintData(Class<T> dataClazz, int charge) {
        try {
            return findFpData(dataClazz, charge, FpDataDocs.toDataFunction(dataClazz));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertFingerprintData(StandardFingerprintData<?> fpData, int charge) {
        // this should not be easy to update sind changing this data can make parts of the project unreadable
        // wie should provide a project update method that takes care of deleting/updating the related data.
        try {
            getStorage().insert(FP_DATA_COLLECTION, FpDataDocs.toDoc(fpData, charge));
            updateSerializers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void insertFingerprintData(FingerIdData fpData, int charge) {
        try {
            getStorage().upsert(FP_DATA_COLLECTION, FpDataDocs.toDoc(fpData, charge));
            updateSerializers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
