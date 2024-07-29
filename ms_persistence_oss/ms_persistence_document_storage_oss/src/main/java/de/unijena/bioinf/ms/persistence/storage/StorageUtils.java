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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.SiriusCachedFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class StorageUtils {
    private static SiriusCachedFactory SIRIUS_PROVIDER;

    public synchronized static SiriusCachedFactory siriusProvider() {
        if (SIRIUS_PROVIDER == null)
            SIRIUS_PROVIDER = new SiriusCachedFactory();
        return SIRIUS_PROVIDER;
    }

    public static Ms2Experiment toMs2Experiment(@NotNull AlignedFeatures feature, @NotNull ParameterConfig config) {
        MSData spectra = feature.getMSData().orElseThrow();

        //should we copy or not?
        MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.addAnnotationsFrom(config, Ms2ExperimentAnnotation.class);
        exp.setMs2Spectra(spectra.getMsnSpectra() != null ? Collections.unmodifiableList(spectra.getMsnSpectra()).stream().map(MergedMSnSpectrum::toMs2Spectrum).toList() : List.of());
        exp.setMs1Spectra(Stream.of(spectra.getIsotopePattern(), spectra.getMergedMs1Spectrum())
                .filter(Objects::nonNull).collect(Collectors.toList()));
        exp.setMergedMs1Spectrum(spectra.getMergedMs1Spectrum());
        exp.setName(feature.getName()); //todo not stored do we want this?
        exp.setFeatureId(feature.getExternalFeatureId());//todo not stored do we want this?
        exp.setIonMass(feature.getAverageMass());
        exp.setMolecularFormula(feature.getMolecularFormula()); //todo not stored do we need this?

        exp.setPrecursorIonType(PrecursorIonType.unknown(feature.getCharge()));

        feature.getDataSource().ifPresent(s -> {
            if (s.getFormat() == DataSource.Format.JENA_MS)
                exp.setSource(new MsFileSource(URI.create(s.getSource())));
            else
                exp.setSource(new SpectrumFileSource(URI.create(s.getSource())));
        });
        exp.setAnnotation(RetentionTime.class, feature.getRetentionTime());
        exp.setAnnotation(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.class, toMs2ExpAnnotation(feature.getDetectedAdducts()));
        return exp;
    }

    public static AlignedFeatures fromMs2Experiment(Ms2Experiment exp) {
        Sirius sirius = siriusProvider().sirius(exp.getAnnotation(MsInstrumentation.class)
                .orElse(MsInstrumentation.Unknown)
                .getRecommendedProfile());

        SimpleSpectrum mergedMs1 = exp.getMergedMs1Spectrum() != null
                ? (SimpleSpectrum) exp.getMergedMs1Spectrum()
                : Spectrums.mergeSpectra(exp.getMs1Spectra());

        SimpleSpectrum isotopePattern = Spectrums.extractIsotopePattern(
                mergedMs1,
                exp.getAnnotationOrDefault(MS1MassDeviation.class),
                exp.getIonMass(),
                exp.getPrecursorIonType().getCharge(),
                true);

        MSData.MSDataBuilder builder = MSData.builder()
                .isotopePattern(isotopePattern != null ? new IsotopePattern(isotopePattern, IsotopePattern.Type.MERGED_APEX) : null)
                .mergedMs1Spectrum(mergedMs1);

        if (exp.getMs2Spectra() != null && !exp.getMs2Spectra().isEmpty()) {
            builder.mergedMSnSpectrum(Spectrums.from(sirius.getMs2Preprocessor().preprocess(exp).getMergedPeaks()));
            builder.msnSpectra(exp.getMs2Spectra().stream().map(StorageUtils::msnSpectrumFrom).toList());
        }

        MSData msData = builder.build();

        //detect adducts for the first time
        if (exp.getMs1Spectra() != null && !exp.getMs1Spectra().isEmpty())
            sirius.getMs1Preprocessor().preprocess(exp);
        de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts det = exp.getAnnotation(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.class).orElse(new de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts());
        if (!exp.getPrecursorIonType().isIonizationUnknown()) {
            PossibleAdducts inputFileAdducts = det.get(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.INPUT_FILE);
            PossibleAdducts ionTypeAdducts = new PossibleAdducts(exp.getPrecursorIonType());
            det.put(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.INPUT_FILE,
                    inputFileAdducts == null ? ionTypeAdducts : PossibleAdducts.union(inputFileAdducts, ionTypeAdducts));
        }

        int charge = exp.getPrecursorIonType().getCharge();
        if (msData.getMsnSpectra() != null) {
            for (MergedMSnSpectrum spec : msData.getMsnSpectra()) {
                spec.setCharge(charge);
            }
        }

        Feature feature = Feature.builder()
                .dataSource(DataSource.fromPath(exp.getSourceString()))
                .retentionTime(exp.getAnnotation(RetentionTime.class).orElse(null))
                .averageMass(exp.getMs2Spectra().stream().mapToDouble(Ms2Spectrum::getPrecursorMz).average().orElse(Double.NaN))
                .charge((byte) charge)
                //todo @MEL ich habe die mal als nullable wrapper objekte gemacht, da wir diese info fuer peak list daten nicht wirklich haben.
//                .apexIntensity()
//                .apexMass()
//                .snr()
                .build();

        AlignedFeatures alignedFeature = AlignedFeatures.singleton(feature, msData);
        alignedFeature.setName(exp.getName());
        alignedFeature.setExternalFeatureId(exp.getFeatureId());
        alignedFeature.setMolecularFormula(exp.getMolecularFormula());
        alignedFeature.setDetectedAdducts(StorageUtils.fromMs2ExpAnnotation(det));
        alignedFeature.setHasMs1(msData.getMergedMs1Spectrum() != null);
        alignedFeature.setHasMsMs((msData.getMsnSpectra() != null && !msData.getMsnSpectra().isEmpty()) || (msData.getMergedMSnSpectrum() != null));

        return alignedFeature;
    }

    private static MergedMSnSpectrum msnSpectrumFrom(Ms2Spectrum<Peak> ms2Spectrum) {
        final MergedMSnSpectrum msn = new MergedMSnSpectrum();
        if (ms2Spectrum.getIonization() != null) msn.setCharge(ms2Spectrum.getIonization().getCharge());
        msn.setPeaks(new SimpleSpectrum(ms2Spectrum));
        msn.setMsLevel(ms2Spectrum.getMsLevel());
        msn.setMergedCollisionEnergy(ms2Spectrum.getCollisionEnergy());
        msn.setMergedPrecursorMz(ms2Spectrum.getPrecursorMz());
        return msn;
    }

    public static DetectedAdducts fromMs2ExpAnnotation(@Nullable de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts adducts) {
        if (adducts == null)
            return null;
        List<DetectedAdduct> featureAdducts = adducts.entrySet().stream().flatMap(e -> e.getValue().getAdducts().stream()
                        .map(p -> DetectedAdduct.builder().adduct(p).source(e.getKey()).build()))
                .toList();

        DetectedAdducts featureDetectedAdducts = new DetectedAdducts();
        featureDetectedAdducts.add(featureAdducts);
        return featureDetectedAdducts;
    }

    public static de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts toMs2ExpAnnotation(@Nullable DetectedAdducts adducts) {
        if (adducts == null)
            return null;
        de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts dA = new de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts();

        Map<de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source, List<PrecursorIonType>> adductsBySource =
                adducts.asMap().values().stream().flatMap(Collection::stream).collect(Collectors.groupingBy(
                        d -> d.getSource(), Collectors.mapping(it -> it.getAdduct(), Collectors.toList())));

        adductsBySource.forEach((s, v) -> dA.put(s, new PossibleAdducts(v.toArray(PrecursorIonType[]::new))));
        return dA;
    }
}
