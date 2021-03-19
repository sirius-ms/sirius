
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.recal.MzRecalibration;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

/**
 * Recommended recalibration strategy.
 */
public class LeastSquare extends AbstractRecalibrationStrategy {

    public LeastSquare() {
        super();
    }

    public LeastSquare(Deviation epsilon, int minNumberOfPeaks, double threshold) {
        super(epsilon, minNumberOfPeaks, threshold);
    }

    @Override
    public UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum) {
        spectrum = new SimpleMutableSpectrum(spectrum);
        SimpleMutableSpectrum refSpectrum = new SimpleMutableSpectrum(referenceSpectrum);
        preprocess(spectrum, refSpectrum);
        final double[] eps = new double[spectrum.size()];
        for (int k=0; k < eps.length; ++k) eps[k] = this.epsilon.absoluteFor(spectrum.getMzAt(k));
        final double[][] values = MzRecalibration.maxIntervalStabbing(spectrum, refSpectrum, eps, threshold);
        if (values[0].length<minNumberOfPeaks) return new Identity();

        if (forceParentPeakIn) forceParentPeakInRecalibration(spectrum,referenceSpectrum,values);


        final UnivariateFunction recalibration = MzRecalibration.getLinearRecalibration(values[0], values[1]);
        MzRecalibration.recalibrate(spectrum, recalibration);
        return recalibration;
    }
}
