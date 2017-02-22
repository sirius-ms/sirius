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

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeaklistSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A peak merger with the following strategy:
 * 1. sort the peaks in descending intensity and iterate them
 * 2. for each peak, merge all peaks within its mass range
 * 3. merging is done by "deleting" all merged peaks but the most intensive one.
 *
 * The advantage of this merger is that the mz values of the spectra does not change.
 */
public class HighIntensityMerger implements PeakMerger {

    private double minMergeDistance;

    public HighIntensityMerger(double minMergeDistance) {
        this.minMergeDistance = minMergeDistance;
    }

    public HighIntensityMerger() {
        this(0d);
    }

    @Override
    public void mergePeaks(List<ProcessedPeak> peaks, Ms2Experiment experiment, Deviation mergeWindow, Merger merger) {
        mergeWindow = new Deviation(mergeWindow.getPpm(), Math.max(mergeWindow.getAbsolute(), minMergeDistance));
        final ProcessedPeak[] mzArray = peaks.toArray(new ProcessedPeak[peaks.size()]);
        final ProcessedPeak.MassComparator massComparator = new ProcessedPeak.MassComparator();
        Arrays.sort(mzArray, new ProcessedPeak.MassComparator());
        final Spectrum<ProcessedPeak> massOrderedSpectrum = new PeaklistSpectrum<ProcessedPeak>(Arrays.asList(mzArray));
        int n = mzArray.length;
        // first: Merge parent peak!!!!
        int parentIndex = mergeParentPeak(experiment, mergeWindow, merger, mzArray, massOrderedSpectrum);
        // after this you can merge the other peaks. Ignore all peaks near the parent peak
        final double parentMass = experiment.getIonMass();
        for (; parentIndex > 0 && mzArray[parentIndex-1].getMz()+0.1d >= parentMass; --parentIndex);
        n = parentIndex;
        final ProcessedPeak[] parray = Arrays.copyOf(mzArray, parentIndex);
        Arrays.sort(parray, Collections.reverseOrder(new ProcessedPeak.RelativeIntensityComparator()));
        for (int i=0; i < parray.length; ++i) {
            final ProcessedPeak p = parray[i];
            final int index = Arrays.binarySearch(mzArray, 0, n, p, massComparator);
            if (index < 0) continue;
            final double error = mergeWindow.absoluteFor(p.getMz());
            final double min = p.getMz() - error;
            final double max = p.getMz() + error;
            int minIndex = index;
            while (minIndex >= 0 && mzArray[minIndex].getMz() >= min) --minIndex;
            ++minIndex;
            int maxIndex = index;
            while (maxIndex < n && mzArray[maxIndex].getMz() <= max) ++maxIndex;
            merger.merge(new ArrayList<ProcessedPeak>(Arrays.asList(mzArray).subList(minIndex, maxIndex)), index-minIndex, p.getMz());
            System.arraycopy(mzArray, maxIndex, mzArray, minIndex, n-maxIndex);
            n -= (maxIndex - minIndex);
        }
    }

    protected int mergeParentPeak(Ms2Experiment experiment, Deviation mergeWindow, Merger merger, ProcessedPeak[] mzArray, Spectrum<ProcessedPeak> massOrderedSpectrum) {
        final double parentMass = experiment.getIonMass();
        final int properParentPeak = Spectrums.search(massOrderedSpectrum, parentMass, mergeWindow);
        if (properParentPeak < 0) {
            // there is no parent peak in spectrum
            // therefore it is save to merge all peaks
            return mzArray.length;
        }
        int lowerBound = properParentPeak;
        int upperBound = properParentPeak;
        double hightestIntensity = mzArray[properParentPeak].getIntensity();
        int intensiveIndex = properParentPeak;
        int secondIntensiveIndex = properParentPeak;
        // merge now all peaks in its neighbourhood
        for (int j = properParentPeak-1; j >= 0 && mergeWindow.inErrorWindow(parentMass, mzArray[j].getMz()); --j ) {
            lowerBound=j;
            if (hightestIntensity < mzArray[j].getIntensity()) {
                secondIntensiveIndex = intensiveIndex;
                hightestIntensity = mzArray[j].getIntensity();
                intensiveIndex = j;
            }
        }
        for (int j = properParentPeak+1; j < mzArray.length && mergeWindow.inErrorWindow(parentMass, mzArray[j].getMz()); ++j ) {
            upperBound=j;
            if (hightestIntensity < mzArray[j].getIntensity()) {
                hightestIntensity = mzArray[j].getIntensity();
                secondIntensiveIndex = intensiveIndex;
                intensiveIndex = j;
            }
        }
        // merge from lowerBound to upperBound
        // main peak is: take the two most intensive peaks with at least 10% intensity and select the peak that is
        // nearest to the parent mass
        final int mainIndex;
        if (mzArray[secondIntensiveIndex].getIntensity() < 0.1 ) mainIndex = intensiveIndex;
        else if (Math.abs(mzArray[secondIntensiveIndex].getMz() - parentMass) < Math.abs(mzArray[intensiveIndex].getMz() - parentMass))
            mainIndex = secondIntensiveIndex;
        else mainIndex = intensiveIndex;
        final ProcessedPeak[] subset = new ProcessedPeak[upperBound-lowerBound+1];
        System.arraycopy(mzArray, lowerBound, subset, 0, upperBound-lowerBound+1);
        merger.merge(Arrays.asList(subset), mainIndex-lowerBound, mzArray[mainIndex].getMz() );
        return lowerBound;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        minMergeDistance = document.getDoubleFromDictionary(dictionary, "minDistance");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "minDistance", minMergeDistance);
    }
}
