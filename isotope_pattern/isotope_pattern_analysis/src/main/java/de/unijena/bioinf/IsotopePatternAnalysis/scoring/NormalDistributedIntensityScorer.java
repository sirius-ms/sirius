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

package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * Models isotope intensity deviations as normal distributions of absolute and relative errors
 * Takes two parameters: sigmaA for absolute deviations and sigmaR for relative deviations
 */
public class NormalDistributedIntensityScorer implements IsotopePatternScorer{

    private static final double SQRT2PI = Math.sqrt(2 * Math.PI);


    protected double sigmaA, sigmaR;

    public NormalDistributedIntensityScorer(double sigmaR, double sigmaA) {
        this.sigmaA = sigmaA;
        this.sigmaR = sigmaR;
    }

    public NormalDistributedIntensityScorer() {
        this.sigmaA = 0.03;
        this.sigmaR = 0.1;
    }

    @Override
    public void score(double[] scores, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment, MeasurementProfile profile) {
        if (usedNormalization.getBase() != 1 || usedNormalization.getMode() != NormalizationMode.MAX) {
            measuredSpectrum = Spectrums.getNormalizedSpectrum(measuredSpectrum, Normalization.Max(1));
            theoreticalSpectrum = Spectrums.getNormalizedSpectrum(theoreticalSpectrum, Normalization.Max(1));
        }
        double score = 0d;
        for (int i = 1; i < Math.min(measuredSpectrum.size(), theoreticalSpectrum.size()); ++i) {
            final double measuredIntensity = measuredSpectrum.getIntensityAt(i);
            final double theoreticalIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double delta = measuredIntensity - theoreticalIntensity;
            final double deltaZero = (sigmaR * sigmaR * delta * theoreticalIntensity) / (sigmaA * sigmaA +
                    sigmaR * sigmaR * theoreticalIntensity * theoreticalIntensity);
            final double epsilon = delta - deltaZero * theoreticalIntensity;
            final double probDelta = (1 / (SQRT2PI * sigmaR)) * (Math.pow(Math.E, ((deltaZero) * (deltaZero) / (-2 * sigmaR *
                    sigmaR))));
            final double probEpsilon = (1 / (SQRT2PI * sigmaA)) * (Math.pow(Math.E, (epsilon * epsilon / (-2 * sigmaA * sigmaA))));
            final double probability = probDelta * probEpsilon;
            score += Math.log(probability);
            scores[i] += score;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        sigmaR = document.getDoubleFromDictionary(dictionary, "relative_error");
        sigmaA = document.getDoubleFromDictionary(dictionary, "absolute_error");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "relative_error", sigmaR);
        document.addToDictionary(dictionary, "absolute_error", sigmaA);
    }
}
