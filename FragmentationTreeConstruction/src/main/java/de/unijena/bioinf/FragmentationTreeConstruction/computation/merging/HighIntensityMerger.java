package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

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

    public HighIntensityMerger() {
    }

    @Override
    public void mergePeaks(List<ProcessedPeak> peaks, Ms2Experiment experiment, Deviation mergeWindow, Merger merger) {
        final ProcessedPeak[] mzArray = peaks.toArray(new ProcessedPeak[peaks.size()]);
        final ProcessedPeak.MassComparator massComparator = new ProcessedPeak.MassComparator();
        Arrays.sort(mzArray, new ProcessedPeak.MassComparator());
        final Spectrum<ProcessedPeak> massOrderedSpectrum = new PeaklistSpectrum<ProcessedPeak>(Arrays.asList(mzArray));
        int n = mzArray.length;
        // first: Merge parent peak!!!!
        int parentIndex = mergeParentPeak(experiment, mergeWindow, merger, mzArray, massOrderedSpectrum);
        // after this you can merge the other peaks. Ignore all peaks near the parent peak
        final double parentMass = experiment.getIonMass();
        for (; parentIndex >= 0 && mzArray[parentIndex].getMz()+0.1d >= parentMass; --parentIndex);
        final ProcessedPeak[] parray = Arrays.copyOf(mzArray, parentIndex);
        Arrays.sort(parray, Collections.reverseOrder(new ProcessedPeak.RelativeIntensityComparator()));
        for (int i=0; i < parray.length; ++i) {
            final ProcessedPeak p = parray[i];
            final int index = Arrays.<ProcessedPeak>binarySearch(mzArray, 0, n, p, massComparator);
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
        int lowerBound = properParentPeak;
        int upperBound = properParentPeak;
        double hightestIntensity = mzArray[properParentPeak].getIntensity();
        int intensiveIndex = properParentPeak;
        // merge now all peaks in its neighbourhood
        for (int j = properParentPeak-1; j >= 0 && mergeWindow.inErrorWindow(parentMass, mzArray[j].getMz()); --j ) {
            lowerBound=j;
            if (hightestIntensity < mzArray[j].getIntensity()) {
                hightestIntensity = mzArray[j].getIntensity();
                intensiveIndex = j;
            }
        }
        for (int j = properParentPeak+1; j < mzArray.length && mergeWindow.inErrorWindow(parentMass, mzArray[j].getMz()); ++j ) {
            upperBound=j;
            if (hightestIntensity < mzArray[j].getIntensity()) {
                hightestIntensity = mzArray[j].getIntensity();
                intensiveIndex = j;
            }
        }
        // merge from lowerBound to upperBound using highest intensive peak as main
        final ProcessedPeak[] subset = new ProcessedPeak[upperBound-lowerBound+1];
        System.arraycopy(mzArray, lowerBound, subset, 0, upperBound-lowerBound+1);
        merger.merge(Arrays.asList(mzArray), intensiveIndex-lowerBound, mzArray[intensiveIndex].getMz() );
        return lowerBound;
    }
}
