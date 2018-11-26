/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.List;

public class NoiseThresholdFilter implements PostProcessor, Preprocessor {

    private double threshold;

    public NoiseThresholdFilter() {
        this(0d);
    }

    public NoiseThresholdFilter(double threshold) {
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        final List<ProcessedPeak> peaks = input.getMergedPeaks();
        final List<ProcessedPeak> filtered = new ArrayList<ProcessedPeak>(peaks.size());
        final ProcessedPeak parent = input.getParentPeak();
        for (ProcessedPeak p : peaks)
            if (p.getRelativeIntensity() >= threshold || p.isSynthetic() || p == parent)
                filtered.add(p);
        input.setMergedPeaks(filtered);
        return input;
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "threshold", threshold);
    }

    @Override
    public MutableMs2Experiment process(final MutableMs2Experiment experiment) {
        List<MutableMs2Spectrum> specs = experiment.getMs2Spectra();
        final ArrayList<MutableMs2Spectrum> newList = new ArrayList<MutableMs2Spectrum>();
        final Deviation allowedDev = experiment.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;
        final Deviation parentWindow = new Deviation(allowedDev.getPpm(), Math.min(allowedDev.getAbsolute(), 0.1d));
        for (MutableMs2Spectrum spec : specs) {
            Spectrums.filter(spec, new Spectrums.PeakPredicate() {
                @Override
                public boolean apply(double mz, double intensity) {
                    return intensity > threshold || parentWindow.inErrorWindow(experiment.getIonMass(), mz);
                }
            });
        }
        return experiment;
    }
}
