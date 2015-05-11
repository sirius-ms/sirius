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
import org.apache.commons.math3.special.Erf;

public class MissingPeakScorer implements IsotopePatternScorer {

    private double lambda;
    private Normalization normalization;
    private final static double sqrt2 = Math.sqrt(2);

    public MissingPeakScorer(NormalizationMode normalization, double lambda) {
        this.lambda = lambda;
        this.normalization = new Normalization(normalization, 1);
    }

    public MissingPeakScorer(double lambda) {
        this(NormalizationMode.SUM, lambda);
    }

    public MissingPeakScorer() {
        this(1);
    }

    @Override
    public double score(Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization norm, MsExperiment experiment) {
        final Spectrum<? extends Peak> measured, theoretical;
        if (normalization.getMode()!=null && !norm.equals(normalization)) {
            measured = normalization.call(measuredSpectrum);
            theoretical = normalization.call(theoreticalSpectrum);
        } else {
            measured = measuredSpectrum;
            theoretical = theoreticalSpectrum;
        }
        double score = 0;
        final double standardDeviation = 0.03;
        for (int i=measured.size(); i < theoretical.size(); ++i) {
            final double diff = theoretical.getIntensityAt(i);
            score += Math.log(Erf.erfc(diff / (sqrt2 * standardDeviation)));
            //score -=  lambda*theoretical.getIntensityAt(i);
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "normalization"))
            this.normalization = (Normalization)helper.unwrap(document, document.getFromDictionary(dictionary, "normalization"));
        this.lambda = document.getDoubleFromDictionary(dictionary, "lambda");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "normalization", helper.wrap(document, normalization));
        document.addToDictionary(dictionary, "lambda", lambda);
    }
}
