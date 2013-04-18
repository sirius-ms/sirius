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
public class KMeansMerger implements PeakMerger {

    private Double mergeTreshold;

    public KMeansMerger() {
        this(null);
    }

    public KMeansMerger(Double mergeTreshold) {
        this.mergeTreshold = mergeTreshold;
    }

    @Override
    public void mergePeaks(List<ProcessedPeak> peaks, ProcessedInput input, Merger merger) {
        final ToMerge[] toMerge = new ToMerge[peaks.size()];
        {
            int k=0;
            for (ProcessedPeak p : peaks)  {
                toMerge[k++] = new ToMerge(Collections.singletonList(p), 0, p.getMz(), p.getRelativeIntensity());
            }
        }
        Arrays.sort(toMerge, Collections.reverseOrder());
        final Deviation idev = input.getExperimentInformation().getMassError();
        final Deviation deviation = mergeTreshold == null ? idev.multiply(2)
                : new Deviation(idev.getPpm(), Math.max(idev.getAbsolute(), mergeTreshold.doubleValue()));
        int index = 0;
        int n = toMerge.length;
        do {
            double minDistance = Double.POSITIVE_INFINITY;
            index = 0;
            for (int j=1; j < n; ++j) {
                final double dist = Math.abs(toMerge[j].mz - toMerge[j - 1].mz);
                final double maxDist = deviation.absoluteFor(toMerge[j].mz);
                if (dist <= maxDist && dist < minDistance) {
                    minDistance = dist;
                    index = j;
                }
            }
            if (index > 0) {
                toMerge[index-1] = toMerge[index].merge(toMerge[index-1]);
                removeAt(toMerge, index, 0, n--);
            }
        } while (index > 0);
        for (int i=0; i < n; ++i) {
            final ToMerge m = toMerge[i];
            merger.merge(m.peaks, m.index, m.mz, m.intensity);
        }
    }

    private <T> void removeAt(T[] array, int index, int offset, int length ) {
        final int realIndex = index + offset;
        System.arraycopy(array, realIndex+1, array, realIndex, length-index-1);
        array[offset+length-1] = null;
    }

    private final static class ToMerge implements Comparable<ToMerge> {
        private final List<ProcessedPeak> peaks;
        private final int index;
        private final double mz;
        private final double intensity;

        private ToMerge(List<ProcessedPeak> peaks, int index, double mz, double intensity) {
            this.peaks = peaks;
            this.index = index;
            this.mz = mz;
            this.intensity = intensity;
        }

        private ToMerge merge(ToMerge other) {
            final ArrayList<ProcessedPeak> pks = new ArrayList<ProcessedPeak>(peaks);
            pks.addAll(other.peaks);
            final double newMz = (mz * intensity + other.mz * other.intensity) / (intensity + other.intensity);
            if (intensity > other.intensity) {
                return new ToMerge(pks, index, newMz, intensity);
            } else {
                return new ToMerge(pks, peaks.size() + other.index, newMz, other.intensity);
            }
        }


        @Override
        public int compareTo(ToMerge o) {
            return Double.compare(mz, o.mz);
        }

        public String toString() {
            return peaks.get(index).toString();
        }
    }

}
