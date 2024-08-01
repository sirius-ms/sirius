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

package de.unijena.bioinf.ms.persistence.model.core.spectrum;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergedMSnSpectrum {
    private int msLevel;
    private int charge;
    private CollisionEnergy mergedCollisionEnergy;
    private double mergedPrecursorMz;
    private IsolationWindow[] isolationWindows;
    /*
     * a merged spectrum is merged from several spectra across samples. For each sample we have a sample ID
     * and an array of scan IDs and precursor scan IDs.
     */
    private long[] sampleIds;
    private int[][] ms2ScanIds; // scan ID of the MS/MS spectrum
    private int[][] rawPrecursorScanIds; // scan ID of the MS on which the MS/MS points to
    private int[][] projectedPrecursorScanIds; // projected scan ID of the MS

    private double[] percursorMzs;

    private Double chimericPollutionRatio;

    private SimpleSpectrum peaks;

    /**
     * This method can be used to transform ms2spectra within an MsExperiment object into a MergedMsnSpectrum
     * and vice versa
     */
    public static MergedMSnSpectrum fromMs2Spectrum(Ms2Spectrum<?> spec) {
        return new MergedMSnSpectrum(
                spec.getMsLevel(),
                spec.getIonization().getCharge(),
                spec.getCollisionEnergy(),
                spec.getPrecursorMz(),
                null,
                new long[0],
                new int[0][],
                new int[0][],
                new int[0][],
                new double[0],
                null,
                new SimpleSpectrum(spec)
        );
    }

    public MutableMs2Spectrum toMs2Spectrum() {
        MutableMs2Spectrum ms2 = new MutableMs2Spectrum(
                peaks, mergedPrecursorMz, mergedCollisionEnergy, msLevel);
        ms2.setIonization(new Charge(charge));
        return ms2;
    }

}
