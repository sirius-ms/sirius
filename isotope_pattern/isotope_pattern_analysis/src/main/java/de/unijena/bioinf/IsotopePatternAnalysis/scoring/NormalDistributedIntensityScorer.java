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

    public boolean LOGODDS = true;



    public static void main(String[] args) {
        final double sigmaA = 0.02, sigmaR = 0.08;
        final double theoreticalIntensity = 0.4;
        for (double measuredIntensity : new double[]{0.01, 0.1, 0.2, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6,0.7,0.9}) {
            final double delta = measuredIntensity-theoreticalIntensity;

            final double peakPropbability = Math.exp(-(delta*delta)/(2*(sigmaA*sigmaA + theoreticalIntensity*theoreticalIntensity*sigmaR*sigmaR)))/(2*Math.PI*theoreticalIntensity*sigmaR*sigmaA);
            final double sigma = theoreticalIntensity+ theoreticalIntensity*sigmaR + sigmaA;
            final double sigmaDelta = sigma-theoreticalIntensity;
            final double score = Math.log(peakPropbability);
            final double normscore = Math.log(Math.exp(-(sigmaDelta*sigmaDelta)/(2*(sigmaA*sigmaA + theoreticalIntensity*theoreticalIntensity*sigmaR*sigmaR)))/(2*Math.PI*theoreticalIntensity*sigmaR*sigmaA));
            System.out.println("y = " + measuredIntensity  + ", delta = " + delta + ", density = " + peakPropbability + ", score = " + score + ", normscore = " + (score-normscore));
        }
    }

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

    public double getSigmaA() {
        return sigmaA;
    }

    public void setSigmaA(double sigmaA) {
        this.sigmaA = sigmaA;
    }

    public double getSigmaR() {
        return sigmaR;
    }

    public void setSigmaR(double sigmaR) {
        this.sigmaR = sigmaR;
    }

    @Override
    public void score(double[] scores, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
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
