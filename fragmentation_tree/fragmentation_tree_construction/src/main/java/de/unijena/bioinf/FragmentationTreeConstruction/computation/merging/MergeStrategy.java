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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * A strategy to merge peaks from different spectra. Merging is done by picking all peaks in the same merge window
 * and calculate a new mass which represents them all. Their intensities are summed up by the merger.
 *
 * It is allowed to modify the parent peak and add new originalPeaks to it. But the mass of the parent peak should
 * not be modified. Furthermore, the intensity of the parent usually doesn't matter.
 *
 * ParentPeak may be null if no parent peak was found or if the parent peak detection should happen later
 *
 * Returns the list of merged peaks
 */
public interface MergeStrategy extends Parameterized {
    public List<ProcessedPeak> mergePeaks(Ms2Experiment experiment, ProcessedPeak parentPeak, List<List<ProcessedPeak>> peaks);
}
