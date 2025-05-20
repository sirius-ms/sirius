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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.notNullOrEmpty;

public class FeatureImports {

    @NotNull
    public static de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts extractDetectedAdducts(FeatureImport featureImport) {
        if (featureImport.getDetectedAdducts() != null && !featureImport.getDetectedAdducts().isEmpty()) {
            de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts da = new de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts();
            featureImport.getDetectedAdducts().stream().map(PrecursorIonType::fromString).distinct().forEach(ionType ->
                    da.addAll(DetectedAdduct.builder().adduct(ionType).source(DetectedAdducts.Source.INPUT_FILE).build()));
            return da;
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


            Deviation ms2MergeDeviation;
            if (profile == InstrumentProfile.ORBITRAP) {
                ms2MergeDeviation = new Deviation(5);
            } else {
                MS2MassDeviation ms2dev =  PropertyManager.DEFAULTS.createInstanceWithDefaults((MS2MassDeviation.class));
                ms2MergeDeviation = ms2dev.allowedMassDeviation;
            }

            SimpleSpectrum merged = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getNormalizedSpectrum(
                    StorageUtils.cleanMergedMsnDataForImport(
                            HighIntensityMsMsMerger.mergePeaks(msnSpectra, featureImport.getIonMass(), ms2MergeDeviation, false, true)
                    ), Normalization.Sum);
            msDataBuilder.mergedMSnSpectrum(merged);
        }

        return msDataBuilder.build();
    }
}
