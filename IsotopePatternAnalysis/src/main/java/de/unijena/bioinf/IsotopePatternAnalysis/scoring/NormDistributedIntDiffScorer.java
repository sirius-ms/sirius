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
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.util.FixedIntensity;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import org.apache.commons.math3.special.Erf;

public class NormDistributedIntDiffScorer implements IsotopePatternScorer {

    private IntensityDependency intensityDependency;
    private final static double root2div2 = Math.sqrt(2);

    public NormDistributedIntDiffScorer() {
        this(new FixedIntensity(0.09));
    }

    public NormDistributedIntDiffScorer(IntensityDependency intensityDependency) {
        this.intensityDependency = intensityDependency;
    }

    public IntensityDependency getIntensityDependency() {
        return intensityDependency;
    }

    @Override
    public double score(Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoretical, Normalization usedNormalization, Ms2Experiment experiment, MeasurementProfile profile) {
        if (measuredSpectrum.size() > theoretical.size())
            throw new IllegalArgumentException("Theoretical spectrum is smaller than measured spectrum");
        // remove peaks from theoretical pattern until the length of both spectra is equal
        final MutableSpectrum<Peak> theoreticalSpectrum = new SimpleMutableSpectrum(theoretical);
        while (measuredSpectrum.size() < theoreticalSpectrum.size()) {
            theoreticalSpectrum.removePeakAt(theoreticalSpectrum.size()-1);
        }
        Spectrums.normalize(theoreticalSpectrum, usedNormalization);
        final double maxIntensity = Spectrums.getMaximalIntensity(measuredSpectrum);
        double score = 0d;
        for (int i=0; i < theoreticalSpectrum.size(); ++i) {
            final double measuredIntensity = measuredSpectrum.getIntensityAt(i);
            final double theoreticalIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double sd = intensityDependency.getValueAt(measuredIntensity);
            score += Math.log(Erf.erfc(Math.abs(measuredIntensity - theoreticalIntensity)/(root2div2*sd)));
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.intensityDependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "intensityDependency", helper.wrap(document,intensityDependency));
    }
}
