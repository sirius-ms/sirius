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

package de.unijena.bioinf.babelms.intermediate;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.Tagging;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ExperimentDataParser {

    protected ExperimentData data;
    protected MutableMs2Experiment experiment;

    public MutableMs2Experiment parse(ExperimentData data) {
        this.data = data;
        experiment = new MutableMs2Experiment();

        addSpectrum();
        addAnnotations();

        return experiment;
    }

    protected void addSpectrum() {
        SimpleSpectrum spectrum = data.getSpectrum();
        if (spectrum == null) {
            log.warn("Spectrum is missing in record " + data.getId() + ".");
            return;
        }

        String spectrumLevel = data.getSpectrumLevel();

        if ("MS1".equalsIgnoreCase(spectrumLevel) || "1".equals(spectrumLevel)) {
            experiment.getMs1Spectra().add(data.getSpectrum());
        } else if ("MS2".equalsIgnoreCase(spectrumLevel) || "MSMS".equalsIgnoreCase(spectrumLevel) || "2".equals(spectrumLevel)) {
            double precursorMz = getPrecursorMz().orElseGet(() -> {
                log.warn("Precursor m/z is not set for MS2 record " + data.getId() + ", setting to 0.");
                return 0d;
            });
            MutableMs2Spectrum ms2Spectrum = new MutableMs2Spectrum(data.getSpectrum(), precursorMz, getCollisionEnergy().orElse(null), 2);
            getIonization().ifPresent(ms2Spectrum::setIonization);
            experiment.getMs2Spectra().add(ms2Spectrum);
        } else {
            throw new RuntimeException("Unsupported ms level " + spectrumLevel + " in record " + data.getId() + ". Expecting MS1 or MS2.");
        }
    }

    protected void addAnnotations() {
        getPrecursorMz().ifPresent(experiment::setIonMass);
        getPrecursorIonType().ifPresent(experiment::setPrecursorIonType);

        getCompoundName().ifPresent(experiment::setName);
        getInstrumentation().ifPresent(instrumentation -> experiment.setAnnotation(MsInstrumentation.class, instrumentation));
        getMolecularFormula().ifPresent(experiment::setMolecularFormula);
        getInchi().ifPresent(experiment::annotate);
        getSmiles().ifPresent(experiment::annotate);
        getSplash().ifPresent(experiment::annotate);
        getRetentionTime().ifPresent(experiment::annotate);

        if (data.getTags() != null && !data.getTags().isEmpty()) {
            experiment.annotate(new Tagging(data.getTags().toArray(new String[0])));
        }
    }

    protected Optional<Double> getPrecursorMz() {
        return Optional.ofNullable(data.getPrecursorMz()).map(Utils::parseDoubleWithUnknownDezSep);
    }

    protected Optional<CollisionEnergy> getCollisionEnergy() {
        return Optional.ofNullable(data.getCollisionEnergy()).map(CollisionEnergy::fromStringOrNull);
    }

    protected Optional<PrecursorIonType> getPrecursorIonType() {
        return Optional.ofNullable(data.getPrecursorIonType()).map(PrecursorIonType::fromString);
    }

    protected Optional<Ionization> getIonization() {
        return getPrecursorIonType().map(PrecursorIonType::getIonization);
    }

    protected Optional<MsInstrumentation> getInstrumentation() {
        return Optional.ofNullable(data.getInstrumentation())
                .map(MsInstrumentation::getBestFittingInstrument)
                .filter(t -> !MsInstrumentation.Unknown.equals(t));
    }

    protected Optional<String> getCompoundName() {
        return Optional.ofNullable(data.getCompoundName());
    }

    protected Optional<MolecularFormula> getMolecularFormula() {
        return Optional.ofNullable(data.getMolecularFormula()).map(MolecularFormula::parseOrNull);
    }

    protected Optional<InChI> getInchi() {
        if (data.getInchi() != null || data.getInchiKey() != null) {
            return Optional.of(InChIs.newInChI(data.getInchiKey(), data.getInchi()));
        }
        return Optional.empty();
    }

    protected Optional<Smiles> getSmiles() {
        return Optional.ofNullable(data.getSmiles()).map(Smiles::new);
    }

    protected Optional<Splash> getSplash() {
        return Optional.ofNullable(data.getSplash()).map(Splash::new);
    }

    protected Optional<RetentionTime> getRetentionTime() {
        return Optional.ofNullable(data.getRetentionTime()).flatMap(RetentionTime::tryParse);
    }
}
