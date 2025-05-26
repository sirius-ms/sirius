/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.features;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.storage.StorageUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.merging.HighIntensityMsMsMerger;
import de.unijena.bioinf.sirius.merging.Ms1Merging;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrEmpty;

@Slf4j
public class FeatureImports {

    private static String preprocessAdductString(@Nullable String s) {
        if (s == null)
            return null;
        if (!s.contains("[") && !s.contains("]"))
            log.warn("Adduct notation '{}' does not contain any square bracket '[,]'. This might cause inaccurate parsing of the adduct (e.g. dimeres and multiple charges).", s);

        if (s.contains("(") || s.contains(")")) {
            log.warn("Adduct notation '{}' contains unsupported parenthesis '(,)'. Try to remove them before parsing.", s);
            s = s.replaceAll("[()]", "");
        }
        return s;
    }

    public static boolean endsWithDigit(@NotNull String str) {
        if (str.isBlank())
            return false;

        return Character.isDigit(str.charAt(str.length() - 1));
    }

    @NotNull
    public static de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts extractDetectedAdducts(FeatureImport featureImport) throws IllegalArgumentException {
        Set<String> detAdducts = featureImport.getDetectedAdducts();
        if (detAdducts != null && !detAdducts.isEmpty()) {
            if (detAdducts.size() == 1) {
                String adductString = preprocessAdductString(detAdducts.iterator().next());
                if (Utils.notNullOrBlank((adductString))) {
                    try {
                        if (endsWithDigit(adductString))
                            throw new IllegalArgumentException("The provided adduct ends with a digit indicating an Isotope peak which is not yet supported. Feature will not be imported!");

                        PrecursorIonType adduct = PrecursorIonType.fromString(adductString);
                        if (adduct.getCharge() != featureImport.getCharge())
                            throw new IllegalArgumentException("The provided adduct has a different charge than the charge provided for the FeatureImport. Feature will not be imported!");

                        return de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts
                                .singleton(DetectedAdducts.Source.INPUT_FILE, adduct);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("The provided adduct in FeatureImport is not supported. Feature will not be imported!", e);
                    }
                }
            } else {
                List<PrecursorIonType> adducts = featureImport.getDetectedAdducts()
                        .stream()
                        .filter(Utils::notNullOrBlank)
                        .map(FeatureImports::preprocessAdductString)
                        .filter(s -> !endsWithDigit(s))
                        .map(PrecursorIonType::parsePrecursorIonType)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(adduct -> adduct.getCharge() == featureImport.getCharge())
                        .distinct()
                        .toList();

                if (!adducts.isEmpty()) {
                    de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts da = new de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts();
                    adducts.forEach(ionType -> da.addAll(DetectedAdduct.builder().adduct(ionType).source(DetectedAdducts.Source.INPUT_FILE).build()));
                    return da;
                }
            }
        }
        return new de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts();
    }

    @NotNull
    public static MSData extractMsData(FeatureImport featureImport, @Nullable InstrumentProfile profile) {
        MSData.MSDataBuilder msDataBuilder = MSData.builder();

        // build MS1 data
        if (featureImport.getMergedMs1() != null || notNullOrEmpty(featureImport.getMs1Spectra())) {
            MS1MassDeviation ms1dev = profile == InstrumentProfile.ORBITRAP
                    ? new MS1MassDeviation(new Deviation(5), new Deviation(5), new Deviation(2))
                    : PropertyManager.DEFAULTS.createInstanceWithDefaults((MS1MassDeviation.class));

            SimpleSpectrum mergeMs1 = featureImport.getMergedMs1() != null
                    ? new SimpleSpectrum(featureImport.getMergedMs1())
                    : Ms1Merging.mergeIfMoreThanOneSpectrum(featureImport.getMs1Spectra(), ms1dev.allowedMassDeviation).mergedSpectrum;
            msDataBuilder.mergedMs1Spectrum(
                    StorageUtils.cleanMergedMs1DataForImport(mergeMs1)
            );

            SimpleSpectrum isotopePattern = Spectrums.extractIsotopePattern(mergeMs1, ms1dev, featureImport.getIonMass(), featureImport.getCharge(), true);
            if (isotopePattern != null)
                msDataBuilder.isotopePattern(new IsotopePattern(isotopePattern, IsotopePattern.Type.AVERAGE));
        }


        if (featureImport.getMs2Spectra() != null && !featureImport.getMs2Spectra().isEmpty()) {
            List<MutableMs2Spectrum> msnSpectra = new ArrayList<>();
            final Charge c = new Charge(featureImport.getCharge());
            DoubleList pmz = new DoubleArrayList();
            for (int i = 0; i < featureImport.getMs2Spectra().size(); i++) {
                BasicSpectrum spectrum = featureImport.getMs2Spectra().get(i);
                MutableMs2Spectrum mutableMs2 = new MutableMs2Spectrum(spectrum);
                mutableMs2.setMsLevel(spectrum.getMsLevel());
                if (spectrum.getScanNumber() != null) {
                    mutableMs2.setScanNumber(spectrum.getScanNumber());
                }
                if (spectrum.getCollisionEnergy() != null) {
                    mutableMs2.setCollisionEnergy(spectrum.getCollisionEnergy());
                }
                if (spectrum.getPrecursorMz() != null) {
                    mutableMs2.setPrecursorMz(spectrum.getPrecursorMz());
                    pmz.add(spectrum.getPrecursorMz());
                }
                mutableMs2.setIonization(c);

                msnSpectra.add(mutableMs2);
            }

            msDataBuilder.msnSpectra(msnSpectra.stream()
                    .map(StorageUtils::cleanMsnDataForImport)
                    .map(MergedMSnSpectrum::fromMs2Spectrum).toList());

            SimpleSpectrum merged = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getNormalizedSpectrum(
                    StorageUtils.cleanMergedMsnDataForImport(
                            HighIntensityMsMsMerger.mergePeaks(msnSpectra, featureImport.getIonMass(), new Deviation(10), false, true)
                    ), Normalization.Sum);
            msDataBuilder.mergedMSnSpectrum(merged);
        }

        return msDataBuilder.build();
    }
}
