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
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.massbank.MassbankFormat;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.nitrite.SpectralNitriteDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpectralNoSQLDBs extends ChemDBs {

    public static SpectralLibrary getLocalSpectralLibrary(Path file) throws IOException {
        return new SpectralNitriteDatabase(file);
    }

    public static int importSpectraFromMs2Experiments(SpectralNoSQLDatabase<?> database, Iterable<Ms2Experiment> experiments, int chunkSize) throws ChemicalDatabaseException {
        return importSpectraFromMs2Experiments(database.storage, experiments, chunkSize);
    }

    public static int importSpectraFromMs2Experiments(Database<?> database, Iterable<Ms2Experiment> experiments, int chunkSize) throws ChemicalDatabaseException {
        List<Ms2ReferenceSpectrum> spectra = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            if (!(experiment instanceof MutableMs2Experiment)) {
                throw new ChemicalDatabaseException(experiment.getClass() + " is not supported.");
            }
            spectra.addAll(ms2ExpToMs2Ref((MutableMs2Experiment) experiment));
        }

        return importSpectra(database, spectra, chunkSize);
    }
    public static int importSpectra(Database<?> database, Iterable<Ms2ReferenceSpectrum> spectra, int chunkSize) throws ChemicalDatabaseException {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String dbDate = df.format(new Date());

            if (database.count(Filter.build().eq("key", ChemDbTags.TAG_DATE), SpectralNoSQLDatabase.Tag.class) > 0) {
                SpectralNoSQLDatabase.Tag tag = database.find(Filter.build().eq("key", ChemDbTags.TAG_DATE), SpectralNoSQLDatabase.Tag.class).iterator().next();
                tag.setValue(dbDate);
                database.upsert(tag);
            } else {
                database.insert(SpectralNoSQLDatabase.Tag.of(ChemDbTags.TAG_DATE, dbDate));
            }
            return StreamSupport.stream(Iterables.partition(spectra, chunkSize).spliterator(), false).mapToInt(chunk -> {
                try {
                    List<Ms2ReferenceSpectrum> data = chunk.stream().filter(reference -> {
                        if (reference.getSmiles() == null) {
                            LoggerFactory.getLogger(SpectralNoSQLDBs.class).warn(reference.getName() + " has no SMILES. Skipping import.");
                        }
                        if (reference.getPrecursorIonType() == null) {
                            LoggerFactory.getLogger(SpectralNoSQLDBs.class).warn(reference.getName() + " has no precursor ion type. Skipping import.");
                        }
                        if (reference.getCandidateInChiKey() == null) {
                            LoggerFactory.getLogger(SpectralNoSQLDBs.class).warn(reference.getName() + " has no candidate InChI key. Skipping import.");
                        }
                        return reference.getSmiles() != null && reference.getCandidateInChiKey() != null;
                    }).map(reference -> {
                        if (reference.getFormula() == null) {
                            try {
                                reference.setFormula(InChISMILESUtils.formulaFromSmiles(reference.getSmiles()));
                            } catch (InvalidSmilesException | UnknownElementException e) {
                                LoggerFactory.getLogger(SpectralNoSQLDBs.class).error("Error converting SMILES to MolecularFormula for " + reference.getName());
                                return Optional.<Ms2ReferenceSpectrum>empty();
                            }
                        }
                        return Optional.of(reference);
                    }).filter(Optional::isPresent).map(opt -> {
                        Ms2ReferenceSpectrum reference = opt.get();
                        if (reference.getPrecursorMz() <= 0) {
                            double pmz = reference.getPrecursorIonType().addIonAndAdduct(reference.getFormula().getMass());
                            reference.setPrecursorMz(pmz);
                        }
                        return reference;
                    }).toList();
                    return database.insertAll(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).sum();
        } catch (RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    private static List<Ms2ReferenceSpectrum> ms2ExpToMs2Ref(MutableMs2Experiment experiment) {
        return experiment.getMs2Spectra().stream().map(s -> {
            Ms2ReferenceSpectrum.Ms2ReferenceSpectrumBuilder b = Ms2ReferenceSpectrum.builder()
                    .formula(experiment.getMolecularFormula())
                    .ionMass(experiment.getIonMass())
                    .name(experiment.getName())
                    .collisionEnergy(s.getCollisionEnergy())
                    .msLevel(s.getMsLevel())
                    .precursorMz(s.getPrecursorMz())
                    .precursorIonType(experiment.getPrecursorIonType())
                    .spectrum(new SimpleSpectrum(s));
            experiment.getAnnotation(Splash.class).map(Splash::getSplash).ifPresent(b::splash);
            experiment.getAnnotation(Smiles.class).map(Smiles::toString).ifPresent(b::smiles);
            experiment.getAnnotation(InChI.class).map(inchi -> (inchi.key != null) ? inchi.key2D() : null).ifPresent(b::candidateInChiKey);
            experiment.getAnnotation(MsInstrumentation.class).ifPresent(b::instrumentation);
            //todo parse nist msp id output
            s.getAnnotation(AdditionalFields.class).ifPresent(fields -> {
                if (fields.containsKey(MassbankFormat.ACCESSION.k())) {
                    b.libraryName(DataSource.MASSBANK.realName);
                    b.libraryId(fields.get(MassbankFormat.ACCESSION.k()));
                }
            });
            return b.build();
        }).collect(Collectors.toList());
    }
}
