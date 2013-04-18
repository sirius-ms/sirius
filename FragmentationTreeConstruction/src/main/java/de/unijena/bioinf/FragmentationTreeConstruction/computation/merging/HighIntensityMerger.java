package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class HighIntensityMerger implements PeakMerger {

    private Double mergeTreshold;

    public HighIntensityMerger() {
        this(null);
    }

    public HighIntensityMerger(Double mergeTreshold) {
        this.mergeTreshold = mergeTreshold;
    }

    @Override
    public void mergePeaks(List<ProcessedPeak> peaks, ProcessedInput input, Merger merger) {
        final ProcessedPeak[] parray = peaks.toArray(new ProcessedPeak[peaks.size()]);
        final ProcessedPeak[] mzArray = Arrays.copyOf(parray, parray.length);
        final Deviation idev = input.getExperimentInformation().getMassError();
        final Deviation deviation = mergeTreshold == null ? idev.multiply(2)
                    : new Deviation(idev.getPpm(), Math.max(idev.getAbsolute(), mergeTreshold.doubleValue()));
        Arrays.sort(parray, Collections.reverseOrder(new ProcessedPeak.RelativeIntensityComparator()));
        Arrays.sort(mzArray, new ProcessedPeak.MassComparator());
        int n = mzArray.length;
        for (int i=0; i < parray.length; ++i) {
            final ProcessedPeak p = parray[i];
            final int index = Arrays.<ProcessedPeak>binarySearch(mzArray, 0, n, p, new ProcessedPeak.MassComparator());
            if (index < 0) continue;
            final double error = deviation.absoluteFor(p.getMz());
            final double min = p.getMz() - error;
            final double max = p.getMz() + error;
            int minIndex = index;
            while (minIndex >= 0 && mzArray[minIndex].getMz() >= min) --minIndex;
            ++minIndex;
            int maxIndex = index;
            while (maxIndex < n && mzArray[maxIndex].getMz() <= max) ++maxIndex;
            merger.merge(new ArrayList<ProcessedPeak>(Arrays.asList(mzArray).subList(minIndex, maxIndex)), index-minIndex, p.getMz(),
                    p.getGlobalRelativeIntensity());
            System.arraycopy(mzArray, maxIndex, mzArray, minIndex, n-maxIndex);
            n -= (maxIndex - minIndex);
        }
    }
}
