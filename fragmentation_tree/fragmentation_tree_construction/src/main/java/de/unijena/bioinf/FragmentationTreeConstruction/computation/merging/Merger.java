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

import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.List;

/*
    A merger which gets a list of peaks and merge them to a single peak. The implementation is given by
    FragmentationTreeAnalysis itself.
 */
public interface Merger {

    /**
     * Merge the given peaks to a single peak
     * @param peaks the peaks to merge. May be modified during the method call
     * @param index the index of the main peak which intensity is passed to the merged peak
     * @return the merged peak
     */
    ProcessedPeak merge(List<ProcessedPeak> peaks, int index, double newMz);

}
