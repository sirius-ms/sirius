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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LimitNumberOfPeaksFilter implements PostProcessor {

    private int limit;

    public LimitNumberOfPeaksFilter() {
        this(Integer.MAX_VALUE);
    }

    public LimitNumberOfPeaksFilter(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        if (limit > input.getMergedPeaks().size()) return input;
        final ProcessedPeak[] peaks = input.getMergedPeaks().toArray(new ProcessedPeak[input.getMergedPeaks().size()]);
        Arrays.sort(peaks, Collections.reverseOrder(new ProcessedPeak.RelativeIntensityComparator()));
        final List<ProcessedPeak> filtered = new ArrayList<ProcessedPeak>(Arrays.asList(peaks).subList(0, limit));
        // !!! don't delete the parent peak !!!
        // window may be wide, but that doesn't matter as other peaks near the parent should be already removed
        final Deviation parentPeakDeviation = new Deviation(1, 0.1);
        final double parentMz = input.getExperimentInformation().getIonMass();
        for (int i = limit; i < peaks.length; ++i) {
            if (parentPeakDeviation.inErrorWindow(parentMz, peaks[i].getMz())) filtered.add(peaks[i]);
        }

        input.setMergedPeaks(filtered);
        return input;
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        limit = (int) document.getIntFromDictionary(dictionary, "limit");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "limit", limit);
    }
}
