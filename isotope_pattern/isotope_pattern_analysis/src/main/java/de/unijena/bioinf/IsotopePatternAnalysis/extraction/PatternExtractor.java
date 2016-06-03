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
package de.unijena.bioinf.IsotopePatternAnalysis.extraction;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;

import java.util.List;

/**
 * Input: MS1 Spectrum with one or more isotopic patterns
 * Output: For each compound this algorithm should return a spectrum containing the isotopic peaks of this compound
 * For each nominal mass there have to be only ONE peak! So multiple peaks have to be merged
 */
public interface PatternExtractor extends Parameterized {

    public List<IsotopePattern> extractPattern(MeasurementProfile profile, Spectrum<Peak> spectrum);

    public List<IsotopePattern> extractPattern(MeasurementProfile profile, Spectrum<Peak> spectrum, double targetMz, boolean allowAdducts);

}
