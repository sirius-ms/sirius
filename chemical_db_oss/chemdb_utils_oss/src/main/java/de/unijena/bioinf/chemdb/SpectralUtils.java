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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.MutableMs2SpectrumWithAdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.massbank.MassbankFormat;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import edu.ucdavis.fiehnlab.spectra.hash.core.SplashFactory;
import edu.ucdavis.fiehnlab.spectra.hash.core.types.Ion;
import edu.ucdavis.fiehnlab.spectra.hash.core.types.SpectraType;
import edu.ucdavis.fiehnlab.spectra.hash.core.types.SpectrumImpl;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SpectralUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpectralUtils.class);

    private static final edu.ucdavis.fiehnlab.spectra.hash.core.Splash SPLASH_ALGO = SplashFactory.create();

    public static int importSpectraFromMs2Experiments(WriteableSpectralLibrary library, Iterable<Ms2Experiment> experiments, int chunkSize) throws ChemicalDatabaseException {
        List<Ms2ReferenceSpectrum> spectra = new ArrayList<>();
        for (Ms2Experiment experiment : experiments) {
            if (!(experiment instanceof MutableMs2Experiment)) {
                throw new ChemicalDatabaseException(experiment.getClass() + " is not supported.");
            }
            spectra.addAll(ms2ExpToMs2Ref((MutableMs2Experiment) experiment));
        }

        return importSpectra(library, spectra, chunkSize);
    }

    public static int importSpectra(WriteableSpectralLibrary library, Iterable<Ms2ReferenceSpectrum> spectra, int chunkSize) throws ChemicalDatabaseException {
        try {
            return Partition.ofSize(spectra, chunkSize).stream().mapToInt(chunk -> {
                try {
                    List<Ms2ReferenceSpectrum> data = SpectralUtils.validateSpectra(chunk);
                    return library.upsertSpectra(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).sum();
        } catch (RuntimeException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    public static List<Ms2ReferenceSpectrum> ms2ExpToMs2Ref(MutableMs2Experiment experiment) {
        return experiment.getMs2Spectra().stream().map(s -> {
            Ms2ReferenceSpectrum.Ms2ReferenceSpectrumBuilder b = Ms2ReferenceSpectrum.builder()
                    .formula(experiment.getMolecularFormula())
                    .exactMass(experiment.getIonMass())
                    .name(experiment.getName())
                    .collisionEnergy(s.getCollisionEnergy())
                    .msLevel(s.getMsLevel())
                    .precursorMz(s.getPrecursorMz())
                    .precursorIonType(experiment.getPrecursorIonType())
                    .spectrum(new SimpleSpectrum(s));

            experiment.getAnnotation(Splash.class).ifPresentOrElse((splash -> b.splash(splash.getSplash())), () -> {
                final edu.ucdavis.fiehnlab.spectra.hash.core.Spectrum spectrum = new SpectrumImpl(StreamSupport.stream(s.spliterator(), false).map(peak -> new Ion(peak.getMass(), peak.getIntensity())).toList(), SpectraType.MS);
                b.splash(SPLASH_ALGO.splashIt(spectrum));
            });

            experiment.getAnnotation(Smiles.class).map(Smiles::toString).ifPresent(b::smiles);
            experiment.getAnnotation(InChI.class).map(inchi -> (inchi.key != null) ? inchi.key2D() : null).ifPresent(b::candidateInChiKey);
            experiment.getAnnotation(MsInstrumentation.class).ifPresent(b::instrumentation);
            experiment.getAnnotation(RetentionTime.class).ifPresent(rt -> b.retentionTime(rt.getMiddleTime()));

            if (s instanceof MutableMs2SpectrumWithAdditionalFields) {
                AdditionalFields fields = ((MutableMs2SpectrumWithAdditionalFields) s).additionalFields();
                    if (fields.containsKey(MassbankFormat.ACCESSION.k())) {
                        b.libraryId(fields.get(MassbankFormat.ACCESSION.k()));
                    }
                
            }

            return b.build();
        }).collect(Collectors.toList());
    }

    public static List<Ms2ReferenceSpectrum> validateSpectra(List<Ms2ReferenceSpectrum> data) {
        return data.stream().filter(reference -> {
            if (reference.getSmiles() == null && reference.getCandidateInChiKey() == null) {
                LOGGER.error(reference.getName() + " has no SMILES or InChI key.");
            } else if (reference.getSmiles() == null) {
                try {
                    reference.setSmiles(InChISMILESUtils.getSmiles(InChISMILESUtils.getAtomContainerFromInchi(reference.getCandidateInChiKey())));
                } catch (CDKException e) {
                    LOGGER.error(reference.getName() + " has malformed InChi key.", e);
                    return false;
                }
            } else if (reference.getCandidateInChiKey() == null) {
                try {
                    reference.setCandidateInChiKey(InChISMILESUtils.getInchiFromSmiles(reference.getSmiles(), false).key2D());
                } catch (CDKException e) {
                    LOGGER.error(reference.getName() + " has malformed SMILES.", e);
                    return false;
                }
            } else if (!InChIs.isInchiKey(reference.getCandidateInChiKey())) {
                try {
                    LOGGER.error(reference.getName() + " has malformed InChI key, trying to replace.");
                    reference.setCandidateInChiKey(InChISMILESUtils.getInchiFromSmiles(reference.getSmiles(), false).key2D());
                } catch (CDKException e) {
                    LOGGER.error(reference.getName() + " has malformed SMILES.", e);
                    return false;
                }
            }
            if (reference.getPrecursorIonType() == null) {
                LOGGER.error(reference.getName() + " has no precursor ion type.");
                return false;
            }
            return reference.getSmiles() != null && reference.getCandidateInChiKey() != null;
        }).map(reference -> {
            if (reference.getFormula() == null) {
                try {
                    reference.setFormula(InChISMILESUtils.formulaFromSmiles(reference.getSmiles()));
                } catch (InvalidSmilesException | UnknownElementException e) {
                    LOGGER.error("Error converting SMILES to MolecularFormula for " + reference.getName());
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
    }
    
}
