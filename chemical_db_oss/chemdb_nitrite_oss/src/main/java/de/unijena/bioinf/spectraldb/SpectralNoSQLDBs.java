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

package de.unijena.bioinf.spectraldb;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.babelms.massbank.MassbankFormat;
import de.unijena.bioinf.chemdb.ChemDBs;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bioinf.spectraldb.nitrite.SpectralNitriteDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpectralNoSQLDBs extends ChemDBs {

    public static SpectralLibrary getLocalSpectralLibrary(Path file) throws IOException {
        return new SpectralNitriteDatabase(file);
    }

    public static void importSpectraFromMs2Experiments(SpectralNoSQLDatabase<?> database, Iterable<Ms2Experiment> experiments, int chunkSize) throws ChemicalDatabaseException {
        importSpectraFromMs2Experiments(database.storage, experiments, chunkSize);
    }
    public static void importSpectraFromMs2Experiments(Database<?> database, Iterable<Ms2Experiment> experiments, int chunkSize) throws ChemicalDatabaseException {
        List<Pair<Ms2SpectralMetadata, Ms2SpectralData>> pairs = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            if (!(experiment instanceof MutableMs2Experiment)) {
                throw new ChemicalDatabaseException(experiment.getClass() + " is not supported.");
            }
            pairs.addAll(ms2ExpToMsDataPair((MutableMs2Experiment) experiment));
        }

        importSpectra(database, pairs, chunkSize);
    }
    public static void importSpectra(Database<?> database, Iterable<Pair<Ms2SpectralMetadata, Ms2SpectralData>> spectra, int chunkSize) throws ChemicalDatabaseException {
        try {
            StreamSupport.stream(Iterables.partition(spectra, chunkSize).spliterator(), false).forEach(chunk -> {
                List<Ms2SpectralMetadata> metadata = chunk.stream().map(Pair::getLeft).collect(Collectors.toList());
                List<Ms2SpectralData> data = chunk.stream().map(Pair::getRight).collect(Collectors.toList());

                try {
                    database.insertAll(metadata);
                    for (Pair<Ms2SpectralMetadata, Ms2SpectralData> pair : chunk) {
                        pair.getRight().setMetaId(pair.getLeft().getId());
                    }
                    database.insertAll(data);
                    for (Pair<Ms2SpectralMetadata, Ms2SpectralData> pair : chunk) {
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

    private static List<Pair<Ms2SpectralMetadata, Ms2SpectralData>> ms2ExpToMsDataPair(MutableMs2Experiment experiment) {
        return experiment.getMs2Spectra().stream().map(s -> {
            Ms2SpectralMetadata.Ms2SpectralMetadataBuilder b = Ms2SpectralMetadata.builder()
                    .formula(experiment.getMolecularFormula())
                    .ionMass(experiment.getIonMass())
                    .name(experiment.getName())
                    .collisionEnergy(s.getCollisionEnergy())
                    .msLevel(s.getMsLevel())
                    .precursorMz(s.getPrecursorMz());
            experiment.getAnnotation(Splash.class).map(Splash::toString).ifPresent(b::splash);
            experiment.getAnnotation(Smiles.class).map(Smiles::toString).ifPresent(b::smiles);
            experiment.getAnnotation(InChI.class).map(InChI::key2D).ifPresent(b::candidateInChiKey);
            experiment.getAnnotation(MsInstrumentation.class).ifPresent(b::instrumentation);
            //todo parse nist msp id output
            s.getAnnotation(AdditionalFields.class).ifPresent(fields -> {
                if (fields.containsKey(MassbankFormat.ACCESSION.k())) {
                    b.libraryId(fields.get(MassbankFormat.ACCESSION.k()));
                }
            });
            return Pair.of(b.build(), new Ms2SpectralData(s));
        }).collect(Collectors.toList());
    }
}
