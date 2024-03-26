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
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.JSONReader;
import de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction;
import de.unijena.bioinf.ms.persistence.model.sirius.CsiPrediction;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CanopusPredictionDeserializer;
import de.unijena.bioinf.ms.persistence.model.sirius.serializers.CsiPredictionDeserializer;
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
import java.util.function.Function;


public class NitriteSirirusProject extends SiriusProjectDatabaseImpl<NitriteDatabase> {

    public NitriteSirirusProject(Path location) throws IOException {
        Metadata metadata = SiriusProjectDocumentDatabase.buildMetadata();
        setStorage(new NitriteDatabase(location, metadata));

        FingerIdData csiPos = findFpData(FingerIdData.class, 1, FpDataDocs::toFingerIdData);
        FingerIdData csiNeg = findFpData(FingerIdData.class, -1, FpDataDocs::toFingerIdData);

        CanopusCfData cfPos = findFpData(CanopusCfData.class, 1, FpDataDocs::toCanopusCfData);
        CanopusCfData cfNeg = findFpData(CanopusCfData.class, -1, FpDataDocs::toCanopusCfData);

        CanopusNpcData npcPos = findFpData(CanopusNpcData.class, 1, FpDataDocs::toCanopusNpcData);
        CanopusNpcData npcNeg = findFpData(CanopusNpcData.class, -1, FpDataDocs::toCanopusNpcData);

        ((JSONReader.FingerprintCandidateDeserializer) metadata.deserializers.get(FingerprintCandidate.class))
                .setVersion(csiPos.getBaseFingerprintVersion());

        ((CsiPredictionDeserializer) metadata.deserializers.get(CsiPrediction.class)).setVersions(
                csiPos.getFingerprintVersion(), csiNeg.getFingerprintVersion());

        ((CanopusPredictionDeserializer) metadata.deserializers.get(CanopusPrediction.class)).setVersions(
                cfPos.getFingerprintVersion(), cfNeg.getFingerprintVersion(),
                npcPos.getFingerprintVersion(), npcNeg.getFingerprintVersion());


    }

    private <T extends FingerprintData<?>> T findFpData(@NotNull Class<T> clzz, int charge, @NotNull Function<Document, T> expander) throws IOException {
        return getStorage().findStr(FP_DATA_COLLECTION, Filter.and(
                        Filter.where("type").eq(clzz.getSimpleName()),
                        Filter.where("charge").eq(charge)))
                .findFirst().map(expander).orElseThrow();
    }


}
