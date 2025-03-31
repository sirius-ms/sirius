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

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
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
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.SiriusCachedFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
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
                exp.setSource(new MsFileSource(Path.of(s.getSource()).toUri()));
            else
                exp.setSource(new SpectrumFileSource(Path.of(s.getSource()).toUri()));
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
            List<ProcessedPeak> tmpSpec = null;
            try {
                tmpSpec = sirius.getMs2Preprocessor().preprocess(exp).getMergedPeaks();
            } catch (Exception e) {
                // when we get data from third party tools it sometimes happens that adduct/mass/formula are
                // contradictory usually due to a false formula annotation. instead of throwing the data away we try
                // to import it without formula an unknown adduct.
                log.warn("Error preprocessing feature at rt={}, mz={}, name={}. Retry without formula and with unknown adduct. Cause: {}",
                        exp.getAnnotation(RetentionTime.class).map(Objects::toString).orElse("N/A"), Math.round(exp.getIonMass()), exp.getName(), e.getMessage());
                ((MutableMs2Experiment) exp).setPrecursorIonType(PrecursorIonType.unknown(exp.getPrecursorIonType().getCharge()));
                ((MutableMs2Experiment) exp).setMolecularFormula(null);
                exp.removeAnnotation(InChI.class);
                exp.removeAnnotation(Smiles.class);
                tmpSpec = sirius.getMs2Preprocessor().preprocess(exp).getMergedPeaks();
            }
            builder.mergedMSnSpectrum(Spectrums.from(tmpSpec.stream()
                    .map(p -> new SimplePeak(p.getMass(), p.getSumIntensity())).toList()));
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
                .retentionTime(exp.getAnnotation(RetentionTime.class).orElse(null))
                .averageMass(exp.getMs2Spectra().stream().mapToDouble(Ms2Spectrum::getPrecursorMz).average().orElse(Double.NaN))
                .charge((byte) charge)
                .build();

        AlignedFeatures alignedFeature = AlignedFeatures.singleton(feature, msData);
        alignedFeature.setDataSource(DataSource.fromPath(exp.getSourceString()));
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

        //add empty
        List<DetectedAdduct> featureAdducts = new ArrayList<>();
        for (Map.Entry<de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source, PossibleAdducts> adductsEntry : adducts.entrySet()) {
            if (adductsEntry.getValue() == null || adductsEntry.getValue().isEmpty()){
                featureAdducts.add(DetectedAdduct.empty(adductsEntry.getKey()));
            }else {
                adductsEntry.getValue().forEach(precursorIonType ->
                        featureAdducts.add(DetectedAdduct.builder().adduct(precursorIonType).source(adductsEntry.getKey()).build()));
            }
        }
        DetectedAdducts featureDetectedAdducts = new DetectedAdducts();
        featureDetectedAdducts.addAll(featureAdducts);
        return featureDetectedAdducts;
    }

    public static de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts toMs2ExpAnnotation(@Nullable DetectedAdducts adducts) {
        if (adducts == null)
            return null;
        de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts dA = new de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts();

        Map<de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source, List<PrecursorIonType>> adductsBySource =
                adducts.getDetectedAdductsStr().collect(Collectors.groupingBy(
                        DetectedAdduct::getSource, Collectors.mapping(DetectedAdduct::getAdduct, Collectors.toList())));

        adductsBySource.forEach((s, v) -> dA.put(s, new PossibleAdducts(v.stream().filter(Objects::nonNull).distinct().toArray(PrecursorIonType[]::new))));
        return dA;
    }
}
