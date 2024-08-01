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
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

import static java.lang.Math.*;

@Deprecated
public class LogNormDistributedIntensityScorer implements IsotopePatternScorer {

    private final static double root2 = sqrt(2d);

    private  double intensityDeviationPenalty;
    private  IntensityDependency intensityDependency;

    public LogNormDistributedIntensityScorer(double intensityDeviationPenalty, IntensityDependency intensityDependency) {
        this.intensityDeviationPenalty = intensityDeviationPenalty;
        this.intensityDependency = intensityDependency;
    }

    public LogNormDistributedIntensityScorer(double intensityDeviationPenalty, double fullIntensityPrecision, double minIntensityPrecision) {
        this(intensityDeviationPenalty, new LinearIntensityDependency(fullIntensityPrecision, minIntensityPrecision));
    }

    public LogNormDistributedIntensityScorer() {
        this(3, 0.1, 0.9);
    }

    public LogNormDistributedIntensityScorer(double fullIntensityPrecision, double minIntensityPrecision) {
        this(3, fullIntensityPrecision, minIntensityPrecision);
    }


    @Override
    public void score(double[] scoreUptoKPeaks, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment) {
        // score
        double score = 0d;
        for (int i=0; i < measuredSpectrum.size(); ++i) {
            final double intensity = measuredSpectrum.getIntensityAt(i);
            final double thIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double sd = 1d/intensityDeviationPenalty * log(1 + intensityDependency.getValueAt(intensity));
            score += log(Erf.erfc(abs(log(thIntensity / intensity)/(root2*sd))));
            scoreUptoKPeaks[i] += score;
        }
    }


    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.intensityDependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
