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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentpeak;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

/**
 * Detects the parent peak in the given experiment.
 * - if the parent peak does not exist, create a synthetic one
 * - if there are multiple peaks that origin the parent peak, decide for one (or merge them). The real peak merging
 * is later done by the MergeStrategy
 * - if there are multiple peaks with different ionizations, decide for one ionization (usually preferring [M+H]+)
 */
public interface ParentPeakDetector {

    ProcessedPeak detectParentPeak(Ms2Experiment experiment);

}
