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

public class MissingPeakScorer implements IsotopePatternScorer {

    protected double lambda = 50;
    protected double threshold = 0.05;

    public MissingPeakScorer() {
    }

    @Override
    public void score(double[] scoreUptoKPeaks, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
        if (usedNormalization.getBase() != 1 || usedNormalization.getMode() != NormalizationMode.MAX) {
            theoreticalSpectrum = Spectrums.getNormalizedSpectrum(theoreticalSpectrum, Normalization.Max(1));
        }
        double score = 0d;
        for (int k=theoreticalSpectrum.size()-1; k >= 0; --k) {
            final double intensity = theoreticalSpectrum.size() > k ? (theoreticalSpectrum.getIntensityAt(k)) : 0d;
            if (intensity >= threshold) {
                if (k < scoreUptoKPeaks.length) scoreUptoKPeaks[k] += score;
                score -= intensity*lambda;
            }

        }

    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary,"lambda"))
            this.lambda = document.getDoubleFromDictionary(dictionary, "lambda");
        if (document.hasKeyInDictionary(dictionary,"threshold"))
            this.threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "lambda", lambda);
        document.addToDictionary(dictionary, "threshold", threshold);
    }
}
