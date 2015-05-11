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
 * A strategy to merge peaks from different spectra. It is guaranteed, that all peaks which are in the merge window are
 * from different spectra. Merging is done by picking all peaks in the same merge window and calculate a new mass
 * which represents them all. Their intensities are summed up by the merger.
 *
 * Important: Be careful with the parent peak! Use {{@link Ms2Experiment#getIonMass}} to get the parent mass.
 * Don't merge the best fitting parent peak away!
 */
public interface PeakMerger extends Parameterized {

    public void mergePeaks(List<ProcessedPeak> peaks, Ms2Experiment experiment, Deviation mergeWindow,  Merger merger);

}
