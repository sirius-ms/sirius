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

    /**
     * The ion count of the spectra this was merged from, over their raw intensities and before any weighting or
     * normalisation.
     * <p>
     * Meaningful for any acquisition - it is how much signal the fragmentation produced - and necessary for a data
     * independent one, where it is the amount of measured intensity that correlated with the precursor well enough to
     * be attributed to it. The peaks themselves cannot answer that: a merged peak's intensity is weighted by the
     * merge score, and the spectrum a reader finally sees is normalised, so a spectrum assembled from signal barely
     * above the noise looks exactly like one assembled from strong peaks.
     * <p>
     * Null for spectra from projects written before this was recorded.
     */
    private Double totalIonCount;

    /**
     * Whether the fragments were acquired data independently, so that the assignment of peaks to this precursor is a
     * computation rather than a measurement and its peaks may belong to a co-eluting neighbour.
     * <p>
     * A flag rather than something inferred from the isolation window's width, so that quality checks specific to
     * this failure mode apply exactly where they should. Null for projects written before this was recorded.
     */
    private Boolean dataIndependent;

    private SimpleSpectrum peaks;

    /** the ion count of a spectrum as it stands, for the paths that build one from a plain MS/MS */
    private static double totalIonCountOf(MutableMs2Spectrum spec) {
        double total = 0d;
        for (int k = 0; k < spec.size(); ++k) total += spec.getIntensityAt(k);
        return total;
    }

    /**
     * This method can be used to transform ms2spectra within an MsExperiment object into a MergedMsnSpectrum
     * and vice versa
     */
    public static MergedMSnSpectrum fromMs2Spectrum(MutableMs2Spectrum spec) {
        return new MergedMSnSpectrum(
                spec.getMsLevel(),
                spec.getIonization().getCharge(),
                spec.getCollisionEnergy(),
                spec.getPrecursorMz(),
                null,
                new long[0],
                new int[][]{new int[]{spec.getScanNumber()}},
                new int[0][],
                new int[0][],
                new double[0],
                null,
                totalIonCountOf(spec),
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
