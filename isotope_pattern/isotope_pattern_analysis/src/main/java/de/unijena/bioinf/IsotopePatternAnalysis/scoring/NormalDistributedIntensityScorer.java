

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

package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopicIntensitySettings;

/**
 * Models isotope intensity deviations as normal distributions of absolute and relative errors
 * Takes two parameters: sigmaA for absolute deviations and sigmaR for relative deviations
 */
public class NormalDistributedIntensityScorer implements IsotopePatternScorer{

    public boolean LOGODDS = true;
   private static final double SQRT2PI = Math.sqrt(2 * Math.PI);

    public NormalDistributedIntensityScorer() {
    }

    @Override
    public void score(double[] scores, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
        final IsotopicIntensitySettings settings = experiment.getAnnotationOrDefault(IsotopicIntensitySettings.class);
        final double sigmaA = settings.absoluteIntensityError;
        final double sigmaR = settings.relativeIntensityError;

        if (usedNormalization.getBase() != 1 || usedNormalization.getMode() != NormalizationMode.MAX) {
            measuredSpectrum = Spectrums.getNormalizedSpectrum(measuredSpectrum, Normalization.Max(1));
            theoreticalSpectrum = Spectrums.getNormalizedSpectrum(theoreticalSpectrum, Normalization.Max(1));
        }

        double score = 0d;
        for (int i = 1; i < Math.min(measuredSpectrum.size(), theoreticalSpectrum.size()); ++i) {
            final double measuredIntensity = measuredSpectrum.getIntensityAt(i);
            final double theoreticalIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double delta = measuredIntensity-theoreticalIntensity;

            final double peakPropbability = Math.exp(-(delta*delta)/(2*(sigmaA*sigmaA + measuredIntensity*measuredIntensity*sigmaR*sigmaR)))/(2*Math.PI*measuredIntensity*sigmaR*sigmaR);

            score += Math.log(peakPropbability);

            if (LOGODDS) {
                final double sigma = measuredIntensity*2*sigmaR + 2*sigmaA;
                score -= Math.log(Math.exp(-(sigma*sigma)/(2*(sigmaA*sigmaA + measuredIntensity*measuredIntensity*sigmaR*sigmaR)))/(2*Math.PI*measuredIntensity*sigmaR*sigmaR));
            }

            scores[i] += score;
        }
    }
/*
    public void score2(double[] scores, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
        if (usedNormalization.getBase() != 1 || usedNormalization.getMode() != NormalizationMode.MAX) {
            measuredSpectrum = Spectrums.getNormalizedSpectrum(measuredSpectrum, Normalization.Max(1));
            theoreticalSpectrum = Spectrums.getNormalizedSpectrum(theoreticalSpectrum, Normalization.Max(1));
        }

        double score = 0d;
        for (int i = 1; i < Math.min(measuredSpectrum.size(), theoreticalSpectrum.size()); ++i) {
            final double measuredIntensity = measuredSpectrum.getIntensityAt(i);
            final double theoreticalIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double delta = measuredIntensity-theoreticalIntensity;

            final double peakPropbability = Math.exp(-(delta*delta)/(2*(sigmaA*sigmaA + theoreticalIntensity*theoreticalIntensity*sigmaR*sigmaR)))/(2*Math.PI*theoreticalIntensity*sigmaR*sigmaR);

            score += Math.log(peakPropbability);

            if (LOGODDS) {
                final double sigma = theoreticalIntensity*2*sigmaR + 2*sigmaA;
                score -= Math.log(Math.exp(-(sigma*sigma)/(2*(sigmaA*sigmaA + theoreticalIntensity*theoreticalIntensity*sigmaR*sigmaR)))/(2*Math.PI*theoreticalIntensity*sigmaR*sigmaR));
            }

            scores[i] += score;
        }
    }
    */

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
    }
}
