/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.merging.HighIntensityMsMsMerger;
import de.unijena.bioinf.sirius.merging.Ms2Merger;
import de.unijena.bioinf.sirius.peakprocessor.MergedSpectrumProcessor;
import de.unijena.bioinf.sirius.peakprocessor.NoiseIntensityThresholdFilter;
import de.unijena.bioinf.sirius.peakprocessor.NormalizeToSumPreprocessor;
import de.unijena.bioinf.sirius.peakprocessor.UnmergedSpectrumProcessor;
import de.unijena.bioinf.sirius.validation.Ms2Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ms2Preprocessor extends Ms1Preprocessor {

    protected Ms2Merger ms2Merger = new HighIntensityMsMsMerger();
    protected List<MergedSpectrumProcessor> postProcessors;
    protected List<UnmergedSpectrumProcessor> preprocessors;

    public Ms2Preprocessor() {
        super();
        this.validator = new Ms2Validator();
        this.preprocessors = new ArrayList<>(Arrays.asList(
                new NormalizeToSumPreprocessor()
        ));
        this.postProcessors = new ArrayList<>(Arrays.asList(
           new NoiseIntensityThresholdFilter()
        ));
    }

    @Override
    public ProcessedInput preprocess(Ms2Experiment experiment) {
        final ProcessedInput processedInput = super.preprocess(experiment);
        preProcessMsMs(processedInput);
        mergePeaks(processedInput);
        renormalizeSpectrum(processedInput);
        postProcessMsMs(processedInput);
        replacePrecursorPeakByIsotopePeak(processedInput);
        return processedInput;
    }

    private void replacePrecursorPeakByIsotopePeak(ProcessedInput input) {
        Ms1IsotopePattern annotation = input.getAnnotation(Ms1IsotopePattern.class, Ms1IsotopePattern::none);
        if (!annotation.isEmpty()) {

            int i = Spectrums.mostIntensivePeakWithin(annotation.getSpectrum(), input.getParentPeak().getMass(), new Deviation(20, 0.1));
            if (i>=0) {
                input.getParentPeak().setMass(annotation.getSpectrum().getMzAt(i));
            }
        }
    }

    private void renormalizeSpectrum(ProcessedInput processedInput) {
        List<ProcessedPeak> mergedPeaks = processedInput.getMergedPeaks();
        double maxIntensity = 0d;
        for (ProcessedPeak peak : mergedPeaks) {
            maxIntensity = Math.max(peak.getRelativeIntensity(), maxIntensity);
        }
        for (int k = 0; k < mergedPeaks.size(); ++k) {
            final ProcessedPeak peak = mergedPeaks.get(k);
            peak.setRelativeIntensity(peak.getRelativeIntensity()/maxIntensity);
            peak.setIndex(k);
        }

    }

    private void preProcessMsMs(ProcessedInput processedInput) {
        for (UnmergedSpectrumProcessor p : preprocessors) {
            p.process(processedInput.getExperimentInformation());
        }
    }

    private void postProcessMsMs(ProcessedInput processedInput) {
        for (MergedSpectrumProcessor p : postProcessors) {
            p.process(processedInput);
        }
    }

    protected void mergePeaks(ProcessedInput processedInput) {
        ms2Merger.merge(processedInput);
    }

}
