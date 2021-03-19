
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

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public interface IsotopePatternScorer extends Parameterized{

    /**
     * computes the log likelihood that the isotopic pattern spectrum explains the measured data. Both input spectra
     * should have the same normalization mode!
     * @param measuredSpectrum normalized measured spectrum
     * @param theoreticalSpectrum normalized theoretical spectrum
     * @param usedNormalization normalization mode which was applied to the spectra, or null, if both spectra is not normalized
     */
    void score(double[] scoreUptoKPeaks, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment);
	
	
}
