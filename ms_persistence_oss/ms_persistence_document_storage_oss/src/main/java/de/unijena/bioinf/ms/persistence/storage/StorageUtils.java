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

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StorageUtils {
    public static Ms2Experiment toMs2Experiment(@NotNull AlignedFeatures feature, @NotNull ParameterConfig config){
        MSData spectra = feature.getMSData().orElseThrow();

        MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.addAnnotationsFrom(config, Ms2ExperimentAnnotation.class);
        exp.setMs2Spectra(spectra.getMsnSpectra());
        exp.setMs1Spectra(Stream.of(spectra.getIsotopePattern(), spectra.getMergedMs1Spectrum())
                .filter(Objects::nonNull).collect(Collectors.toList()));
        exp.setName(feature.getName()); //todo not stored do we want this?
        exp.setFeatureId(feature.getExternalFeatureId());//todo not stored do we want this?
        exp.setPrecursorIonType(feature.getIonType());
        exp.setIonMass(feature.getAverageMass());
        exp.setMolecularFormula(feature.getMolecularFormula()); //todo not stored do we need this?
        feature.getDataSource().ifPresent(s -> {
           if (s.getFormat() == DataSource.Format.JENA_MS)
                exp.setSource(new MsFileSource(URI.create(s.getSource())));
           else
               exp.setSource(new SpectrumFileSource(URI.create(s.getSource())));
        });
        exp.setAnnotation(RetentionTime.class, feature.getRetentionTime());
        return exp;
    }
    public static AlignedFeatures fromMs2Experiment(Ms2Experiment exp){
        SimpleSpectrum mergedMs1 = exp.getMergedMs1Spectrum() != null
                ? (SimpleSpectrum) exp.getMergedMs1Spectrum()
                : Spectrums.mergeSpectra(exp.getMs1Spectra());

        SimpleSpectrum isotopePattern = Spectrums.extractIsotopePattern(
                mergedMs1,
                exp.getAnnotationOrDefault(MS1MassDeviation.class),
                exp.getIonMass(),
                exp.getPrecursorIonType().getCharge(),
                true);

        MergedMSnSpectrum mergedMsn = MergedMSnSpectrum.builder()
                .peaks(Spectrums.from(new Ms2Preprocessor().preprocess(exp).getMergedPeaks()))
                .percursorMzs(exp.getMs2Spectra().stream().mapToDouble(Ms2Spectrum::getPrecursorMz).toArray())
                .collisionEnergies(exp.getMs2Spectra().stream().map(Ms2Spectrum::getCollisionEnergy).toArray(CollisionEnergy[]::new))
                .isolationWindows(null)
                .build();

        MSData msData = MSData.builder()
                .isotopePattern(new IsotopePattern(isotopePattern, IsotopePattern.Type.MERGED_APEX))
                .mergedMs1Spectrum(mergedMs1)
                .mergedMSnSpectrum(mergedMsn)
                .msnSpectra(exp.getMs2Spectra())
                .build();

        Feature feature = Feature.builder()
                .dataSource(DataSource.fromPath(exp.getSourceString()))
                .ionType(exp.getPrecursorIonType())
                .retentionTime(exp.getAnnotation(RetentionTime.class).orElse(null))
                //todo @MEL: wir habe im modell kein MZ of interest, aber letztendlich ist das einfach average mz oder? Gibt ja nur ein window keine wirkliche mzofinterest
                .averageMass(Arrays.stream(mergedMsn.getPercursorMzs()).average().orElse(Double.NaN))
                //todo @MEL ich habe die mal als nullable wrapper objekte gemacht, da wir diese info fuer peak list daten nicht wirklich haben.
//                .apexIntensity()
//                .apexMass()
//                .snr()
                .build();

        AlignedFeatures alignedFeature = AlignedFeatures.singleton(feature, msData);
        alignedFeature.setName(exp.getName());
        alignedFeature.setExternalFeatureId(exp.getFeatureId());
        alignedFeature.setMolecularFormula(exp.getMolecularFormula());
        return alignedFeature;
    }
}
