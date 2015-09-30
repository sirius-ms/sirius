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
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

public class MassDifferenceDeviationScorer implements IsotopePatternScorer {

    private final static double root2 = Math.sqrt(2d);
    private IntensityDependency dependency;

    public MassDifferenceDeviationScorer() {
        this(1.5d);
    }

    public MassDifferenceDeviationScorer(double lowestIntensityAccuracy) {
        this(new LinearIntensityDependency(1d, lowestIntensityAccuracy));
    }

    public MassDifferenceDeviationScorer(IntensityDependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public double score(Spectrum<Peak> measured, Spectrum<Peak> theoretical, Normalization norm, MsExperiment experiment) {
        final double mz0 = measured.getMzAt(0);
        final double thMz0 = theoretical.getMzAt(0);
        double score = 0d;
        for (int i=0; i < measured.size(); ++i) {
            final double mz = measured.getMzAt(i) - (i==0 ? 0 : measured.getMzAt(i-1));
            final double thMz = theoretical.getMzAt(i) - (i==0 ? 0 : theoretical.getMzAt(i-1));
            final double intensity = measured.getIntensityAt(i);
            // TODO: thMz hier richtig?
            final double sd = experiment.getMeasurementProfile().getStandardMassDifferenceDeviation().absoluteFor(measured.getMzAt(i)) * dependency.getValueAt(intensity);
            score += Math.log(Erf.erfc(Math.abs(thMz - mz)/(root2*sd)));
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.dependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "intensityDependency", helper.wrap(document,dependency));
    }
}
