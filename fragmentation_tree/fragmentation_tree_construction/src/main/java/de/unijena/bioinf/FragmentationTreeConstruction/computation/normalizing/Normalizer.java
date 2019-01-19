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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.List;

/**
 * Gets an Ms2Experiment with Ms2 spectra as input and returns a list of normalized peaks from each spectrum
 *
 * NormalizationType: The normalizer have to calculate local AND global intensities. The normalization type defines which
 * of both should be written in the relativeIntensity field
 */
public interface Normalizer {

    /**
     *
     * @param experiment input data
     * @param type The normalizer have to calculate local AND global intensities. The normalization type defines which
     *             of both should be written in the relativeIntensity field
     * @return a list for each spectrum containing a list of processed peaks. Each peak should have a backreference to
     *         the original peak as well as local, global and (according to NormalizationType) relative intensities
     */
    List<List<ProcessedPeak>> normalize(Ms2Experiment experiment, NormalizationType type);

}
