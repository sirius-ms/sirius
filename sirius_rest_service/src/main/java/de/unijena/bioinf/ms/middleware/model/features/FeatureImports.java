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

package de.unijena.bioinf.ms.middleware.model.features;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class FeatureImports {

    public static Ms2Experiment toExperiment(FeatureImport feature) {
        MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setIonMass(feature.getIonMass());
        exp.setPrecursorIonType(PrecursorIonType.fromString(feature.getAdduct()));
        exp.setName(feature.getName());
        exp.setFeatureId(feature.getFeatureId());

        RetentionTime rt = null;
        if (feature.rtStartSeconds == null || feature.rtStartSeconds.isNaN()) {
            if (feature.rtEndSeconds != null && !feature.rtEndSeconds.isNaN())
                rt = new RetentionTime(feature.rtEndSeconds);
        } else {
            if (feature.rtEndSeconds == null || feature.rtEndSeconds.isNaN())
                rt = new RetentionTime(feature.rtStartSeconds);
            else
                rt = new RetentionTime(feature.rtStartSeconds, feature.rtEndSeconds);
        }
        exp.setAnnotation(RetentionTime.class, rt);

        exp.setMs1Spectra(feature.getMs1Spectra().stream().map(SimpleSpectrum::new).toList());
        exp.setMs2Spectra(feature.getMs2Spectra().stream().map(s ->
                new MutableMs2Spectrum(s, s.getPrecursorMz(), s.getCollisionEnergy(),
                        s.getMsLevel() == 0 ? 2 : s.getMsLevel(),
                        s.getScanNumber() == null ? -1 : s.getScanNumber())).toList());
        if (feature.getMergedMs1() != null)
            exp.setMergedMs1Spectrum(new SimpleSpectrum(feature.getMergedMs1()));
        return exp;
    }

    public static List<Ms2Experiment> toExperiments(Collection<FeatureImport> features) {
        return toExperimentsStr(features).toList();
    }

    public static Stream<Ms2Experiment> toExperimentsStr(Collection<FeatureImport> features) {
        return features.stream().map(FeatureImports::toExperiment);
    }
}
