
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.recal.MzRecalibration;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

import java.util.Arrays;

public class AbstractRecalibrationStrategy implements RecalibrationStrategy, Parameterized {

    protected Deviation epsilon;
    protected int minNumberOfPeaks;
    protected double minIntensity, threshold;
    protected Deviation maxDeviation;
    protected boolean forceParentPeakIn;

    public AbstractRecalibrationStrategy() {
        this(new Deviation(4, 0.001), 10, 0.1);
    }

    public AbstractRecalibrationStrategy(Deviation epsilon, int minNumberOfPeaks, double threshold) {
        this.epsilon = epsilon;
        this.minNumberOfPeaks = minNumberOfPeaks;
        this.minIntensity = 0d;
        this.maxDeviation = new Deviation(10, 1e-3);
        this.threshold = threshold;
        this.forceParentPeakIn = false;
    }

    public boolean isForceParentPeakIn() {
        return forceParentPeakIn;
    }

    public void setForceParentPeakIn(boolean forceParentPeakIn) {
        this.forceParentPeakIn = forceParentPeakIn;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public Deviation getMaxDeviation() {
        return maxDeviation;
    }

    public void setMaxDeviation(Deviation maxDeviation) {
        this.maxDeviation = maxDeviation;
    }

    public int getMinNumberOfPeaks() {
        return minNumberOfPeaks;
    }

    public void setEpsilon(Deviation epsilon) {
        this.epsilon = epsilon;
    }

    public void setMinNumberOfPeaks(int minNumberOfPeaks) {
        this.minNumberOfPeaks = minNumberOfPeaks;
    }

    public double getMinIntensity() {
        return minIntensity;
    }

    public void setMinIntensity(double minIntensity) {
        this.minIntensity = minIntensity;
    }

    public Deviation getEpsilon() {
        return epsilon;
    }


    @Override
    public UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum) {
        spectrum = new SimpleMutableSpectrum(spectrum);
        final SimpleMutableSpectrum ref = new SimpleMutableSpectrum(referenceSpectrum);

        preprocess(spectrum, ref);
        final double[][] values = MzRecalibration.maxIntervalStabbing(spectrum, referenceSpectrum, new UnivariateFunction() {
            @Override
            public double value(double x) {
                return epsilon.absoluteFor(x);
            }
        }, threshold);
        if (values[0].length < minNumberOfPeaks) return new Identity();
        final UnivariateFunction recalibration = MzRecalibration.getMedianLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }

    protected void preprocess(MutableSpectrum<? extends Peak> spectrum, MutableSpectrum<? extends Peak> ref) {
        int i = 0;
        final double parentmz = spectrum.getMzAt(Spectrums.getIndexOfPeakWithMaximalMass(spectrum));
        while (i < ref.size()) {
            if ((spectrum.getMzAt(i) < (parentmz - 0.5d)) && (spectrum.getIntensityAt(i) < minIntensity || !maxDeviation.inErrorWindow(spectrum.getMzAt(i), ref.getMzAt(i)))) {
                ref.removePeakAt(i);
                spectrum.removePeakAt(i);
            } else ++i;
        }
    }

    protected void forceParentPeakInRecalibration(final Spectrum<Peak> spectrum, final Spectrum<Peak> referenceSpectrum, final double[][] values) {
        double parentmz = spectrum.getMzAt(Spectrums.getIndexOfPeakWithMaximalMass(spectrum));
        double refmz = referenceSpectrum.getMzAt(Spectrums.getIndexOfPeakWithMaximalMass(referenceSpectrum));
        boolean found = false;
        for (int k = 0; k < values[0].length; ++k)
            if (Math.abs(parentmz - values[0][k]) < 1e-5 && Math.abs(refmz - values[1][k]) < 1e-5) {
                found = true;
                break;
            }
        if (!found) {
            values[0] = Arrays.copyOf(values[0], values[0].length + 1);
            values[0][values[0].length - 1] = parentmz;
            values[1] = Arrays.copyOf(values[1], values[1].length + 1);
            values[1][values[1].length - 1] = refmz;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "epsilon"))
            epsilon = Deviation.fromString(document.getStringFromDictionary(dictionary, "epsilon"));
        if (document.hasKeyInDictionary(dictionary, "minNumberOfPeaks"))
            minNumberOfPeaks = (int) document.getIntFromDictionary(dictionary, "minNumberOfPeaks");
        if (document.hasKeyInDictionary(dictionary, "threshold"))
            threshold = document.getDoubleFromDictionary(dictionary, "threshold");
        if (document.hasKeyInDictionary(dictionary, "forceParentPeakIn"))
            forceParentPeakIn = document.getBooleanFromDictionary(dictionary, "forceParentPeakIn");
        if (document.hasKeyInDictionary(dictionary, "maxDeviation"))
            maxDeviation = Deviation.fromString(document.getStringFromDictionary(dictionary, "maxDeviation"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "epsilon", epsilon.toString());
        document.addToDictionary(dictionary, "minNumberOfPeaks", minNumberOfPeaks);
        document.addToDictionary(dictionary, "threshold", threshold);
        document.addToDictionary(dictionary, "forceParentPeakIn", forceParentPeakIn);
        document.addToDictionary(dictionary, "maxDeviation", maxDeviation.toString());
    }


}
