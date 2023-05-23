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

package de.unijena.bioinf.spectraldb.nitrite;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.nitrite.ChemicalNitriteDatabase;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bioinf.spectraldb.entities.SpectralData;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.apache.commons.lang3.tuple.Pair;
import org.dizitart.no2.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpectralNitriteDatabase extends SpectralNoSQLDatabase<Document> {

    public SpectralNitriteDatabase(Path file) throws IOException {
        super(new NitriteDatabase(file, SpectralNoSQLDatabase.initMetadata(CdkFingerprintVersion.getDefault())));
    }

    public SpectralNitriteDatabase(Path file, FingerprintVersion version) throws IOException {
        super(new NitriteDatabase(file, SpectralNoSQLDatabase.initMetadata(version)));
    }

    @Override
    public <C extends CompoundCandidate> void importCompoundsAndFingerprints(MolecularFormula key, Iterable<C> candidates) throws ChemicalDatabaseException {
        ChemicalNitriteDatabase.importCompoundsAndFingerprints(this.database, key, candidates);
    }

    @Override
    public void importSpectra(Iterable<Ms2Experiment> experiments) throws ChemicalDatabaseException {
        List<Pair<Ms2SpectralMetadata, SpectralData>> pairs = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            if (!(experiment instanceof MutableMs2Experiment)) {
                throw new ChemicalDatabaseException(experiment.getClass() + " is not supported.");
            }
            pairs.addAll(Ms2SpectralMetadata.fromMutableMs2Experiment((MutableMs2Experiment) experiment));
        }

        try {
            StreamSupport.stream(Iterables.partition(pairs, 100).spliterator(), false).forEach(chunk -> {
                List<Ms2SpectralMetadata> metadata = chunk.stream().map(Pair::getLeft).collect(Collectors.toList());
                List<SpectralData> data = chunk.stream().map(Pair::getRight).collect(Collectors.toList());

                try {
                    database.insertAll(metadata);
                    for (Pair<Ms2SpectralMetadata, SpectralData> pair : chunk) {
                        pair.getRight().setMetaId(pair.getLeft().getId());
                    }
                    database.insertAll(data);
                    for (Pair<Ms2SpectralMetadata, SpectralData> pair : chunk) {
                        pair.getLeft().setPeaksId(pair.getRight().getId());
                    }
                    database.upsertAll(metadata);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            throw new ChemicalDatabaseException(e);
        }
    }
}
