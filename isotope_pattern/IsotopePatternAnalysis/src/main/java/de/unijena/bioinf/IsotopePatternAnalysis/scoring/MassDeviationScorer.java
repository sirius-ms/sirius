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
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

public class MassDeviationScorer implements IsotopePatternScorer {

    private final static double root2 = Math.sqrt(2d);

    private IntensityDependency intensityDependency;

    public MassDeviationScorer() {
        this(1.5d);
    }

    public MassDeviationScorer(double lowestIntensityAccuracy) {
        this(new LinearIntensityDependency(1d, lowestIntensityAccuracy));
    }

    public MassDeviationScorer(IntensityDependency intensityDependency) {
        this.intensityDependency  = intensityDependency;
    }

    @Override
    public double score(Spectrum<Peak> measured, Spectrum<Peak> theoretical, Normalization norm, Ms2Experiment experiment, MeasurementProfile profile) {
        if (measured.size() > theoretical.size())
            throw new IllegalArgumentException("Theoretical spectrum is smaller than measured spectrum");
        // remove peaks from theoretical pattern until the length of both spectra is equal
        final MutableSpectrum<Peak> theoreticalSpectrum = new SimpleMutableSpectrum(theoretical);
        while (measured.size() < theoreticalSpectrum.size()) {
            theoreticalSpectrum.removePeakAt(theoreticalSpectrum.size()-1);
        }
        // re-normalize
        Spectrums.normalize(theoreticalSpectrum, norm);
        final double mz0 = measured.getMzAt(0);
        final double thMz0 = theoreticalSpectrum.getMzAt(0);
        final double int0 = measured.getIntensityAt(0);
        double score = Math.log(Erf.erfc(Math.abs(thMz0 - mz0)/
                (root2*(profile.getStandardMs1MassDeviation().absoluteFor(mz0) *  intensityDependency.getValueAt(int0)))));
        for (int i=1; i < measured.size(); ++i) {
            final double mz = measured.getMzAt(i) - mz0;
            final double thMz = theoreticalSpectrum.getMzAt(i) - thMz0;
            final double thIntensity = measured.getIntensityAt(i);
            // TODO: thMz hier richtig?
            final double sd = profile.getStandardMassDifferenceDeviation().absoluteFor(measured.getMzAt(i)) * intensityDependency.getValueAt(thIntensity);
            score += Math.log(Erf.erfc(Math.abs(thMz - mz)/(root2*sd)));
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.intensityDependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "intensityDependency", helper.wrap(document, intensityDependency));
    }
}
