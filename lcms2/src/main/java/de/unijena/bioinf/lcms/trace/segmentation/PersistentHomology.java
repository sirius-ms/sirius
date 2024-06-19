/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2017 Stefan Huber (Original version)
 * Copyright (C) 2023 Bright Giant GmbH (Modified version)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.filter.Filter;
import de.unijena.bioinf.lcms.trace.filter.NoFilter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.IntStream;

/**
 * <p>A class to find and compute the significance of each peak in a list of values. Algorithm sourced from
 * <a href="https://www.sthu.org/blog/13-perstopology-peakdetection/index.html">Stefan Huber, FH Salzburg</a>.</p>
 * <p> The idea of the algorithm is:
 * Consider a water level starting above the global maximum that continuously lowers its level. Whenever the water
 * level reaches a local maximum a new island is born. Whenever it reaches a local minimum two islands merge, and we
 * consider the lower island to be merged into the higher island. This gives a precise notion of a lifespan of an
 * island; it is the significance we mentioned above, or the so-called persistence in persistent homology.</p>
 *
 * {@see <a href="https://www.math.uri.edu/~thoma/comp_top__2018/stag2016.pdf">Fugacci et al., 2016</a>}
 * {@see <a href="https://www.sthu.org/research/publications/files/Hub20.pdf">Huber, 2020</a>}
 */
public class PersistentHomology implements TraceSegmentationStrategy {

    private final Filter filter;

    private final boolean highPrecisionMode;

    public PersistentHomology() {
        this(new NoFilter());
    }

    public PersistentHomology(Filter filter) {
        this.filter = filter;
        this.highPrecisionMode = false;
    }

    public PersistentHomology(boolean highPrecisionMode) {
        this.highPrecisionMode = highPrecisionMode;
        this.filter = new NoFilter();
    }

    @Getter
    private static class Segment {

        @Setter
        private int left, right;

        private final int born;

        @Setter
        private int died = Integer.MIN_VALUE;

        public Segment(int idx) {
            this.born = this.left = this.right = idx;
        }

        @Override
        public String toString() {
            return "Segment{" +
                    "left=" + left +
                    ", right=" + right +
                    ", born=" + born +
                    ", died=" + died +
                    '}';
        }
    }

    /**
     * Just a wrapper around a trace that is 0-offset. This way we avoid annoying indexing errors
     */
    private static class TraceIntensityArray {
        private final Trace trace;

        private final Filter filter;

        private final float[] intensities;
        private final int offset;

        public TraceIntensityArray(Trace trace, Filter filter) {
            this.trace = trace;
            this.offset = trace.startId();
            this.filter = filter;
            this.intensities = filter();
        }

        private float[] filter() {
            if (filter instanceof NoFilter) return null;
            final double[] ints = new double[size()];
            for (int k=0; k < size(); ++k) ints[k] = trace.intensity(k+offset);
            return MatrixUtils.double2float(filter.apply(ints));
        }

        public float get(int index) {
            if (intensities!=null) return intensities[index];
            return trace.intensity(index+offset);
        }

        private double percentile10() {
            return Quickselect.quickselectInplace(intensities.clone(), 0, intensities.length, (int)Math.ceil(intensities.length*0.1));
        }

        public double getAverageRt(Segment segment) {
            double sum = 0d;
            double norm = 0d;
            for (int i = segment.left; i <= segment.right; i++) {
                final int j = i+offset;
                sum += trace.intensity(j) * trace.retentionTime(j);
                norm += trace.intensity(j);
            }
            return sum / norm;
        }
        public double getStdRT(Segment segment, double mean) {
            double intSum = 0d;
            double var = 0d;
            for (int i = segment.left; i <= segment.right; i++) {
                intSum += trace.intensity(i+offset);
            }
            for (int i = segment.left; i <= segment.right; i++) {
                final int j = i+offset;
                var += (trace.intensity(j) / intSum) * Math.pow((trace.retentionTime(j) - mean), 2);
            }
            return Math.sqrt(var);
        }
// modell peak als gaussian, cut bei 3*sigma
        private Segment trimToXStdRT(Segment peak, double x) {
            double mean = getAverageRt(peak);
            double std = getStdRT(peak,mean);
            for (int i = 0; i < size(); i++) {
                if (trace.retentionTime(i+offset) < mean - x * std)
                    continue;
                peak.setLeft(Math.max(peak.getLeft(), i));
                break;
            }
            for (int i = size() - 1; i >= 0 ; i--) {
                if (trace.retentionTime(i+offset) > mean + x * std)
                    continue;
                peak.setRight(Math.min(peak.getRight(), i));
                break;
            }
            return peak;
        }

        public int size() {
            return trace.length();
        }
    }

    private static List<Segment> computePersistentHomology(Trace trace, Filter filter, double intensityThreshold, double persistenceThresholdValue, double noisePercentile, double slopeThreshold, double trim) {
        if (trace.apexIntensity() < intensityThreshold) return Collections.emptyList();
        final TraceIntensityArray seq = new TraceIntensityArray(trace, filter);
        List<Segment> peaks = new ArrayList<>();
        int[] idx2Peak = new int[seq.size()];
        Arrays.fill(idx2Peak, -1);
        IntList indices = new IntArrayList(IntStream.range(0, seq.size()).toArray());
        indices.sort((a, b) -> Double.compare(seq.get(b), seq.get(a)));
        // get 10% percentile
        final double persistenceThreshold = Math.max(persistenceThresholdValue, seq.get(indices.getInt((int)Math.floor(indices.size()*(1-noisePercentile)))));
        for (int idx : indices) {
            boolean leftDone = (idx > 0 && idx2Peak[idx - 1] > -1);
            boolean rightDone = (idx < seq.size() - 1 && idx2Peak[idx + 1] > -1);

            int il = leftDone ? idx2Peak[idx - 1] : -1;
            int ir = rightDone ? idx2Peak[idx + 1] : -1;

            // new peak born
            if (!leftDone && !rightDone) {
                peaks.add(new Segment(idx));
                idx2Peak[idx] = peaks.size() - 1;
            }

            // merge to next peak left
            if (leftDone && !rightDone) {
                peaks.get(il).setRight(peaks.get(il).getRight() + 1);
                idx2Peak[idx] = il;
            }

            // merge to next peak right
            if (!leftDone && rightDone) {
                peaks.get(ir).setLeft(peaks.get(ir).getLeft() - 1);
                idx2Peak[idx] = ir;
            }

            // merge left and right peaks
            if (leftDone && rightDone) {
                // left was born earlier: merge right to left
                if (seq.get(peaks.get(il).getBorn()) > seq.get(peaks.get(ir).getBorn())) {
                    peaks.get(ir).setDied(idx);
                    peaks.get(il).setRight(peaks.get(ir).getRight());
                    idx2Peak[peaks.get(il).getRight()] = idx2Peak[idx] = il;
                } else {
                    peaks.get(il).setDied(idx);
                    peaks.get(ir).setLeft(peaks.get(il).getLeft());
                    idx2Peak[peaks.get(ir).getLeft()] = idx2Peak[idx] = ir;
                }
            }
        }

        // set left and right borders of peaks to peak valleys
        peaks.sort(Comparator.comparingInt(Segment::getBorn));
        IntList valleys = new IntArrayList(peaks.stream().mapToInt(Segment::getDied).filter(died -> died > Integer.MIN_VALUE).sorted().toArray());
        valleys.add(0, 0);
        valleys.add(seq.size() - 1);

        for (int i = 0; i < peaks.size(); i++) {
            peaks.get(i).setLeft(valleys.getInt(i));
            peaks.get(i).setRight(valleys.getInt(i + 1));
        }

        peaks = peaks.stream().map(peak -> seq.trimToXStdRT(peak, trim)).toList();

        if (peaks.isEmpty())
            return peaks;

        List<Segment> merged = new ArrayList<>();
        merged.add(peaks.get(0));

        for (int i = 1; i < peaks.size(); i++) {
            Segment current = peaks.get(i);
            Segment last = merged.get(merged.size() - 1);
            // do not merge two consecutive peaks if:
            // 1. there is a gap between both peaks
            // 2. the valley intensity is low (either below the int threshold or half max)
            if (last.getRight() < current.getLeft() ||
                    seq.get(current.getLeft()) < intensityThreshold ||
                    seq.get(current.getLeft()) < slopeThreshold * seq.get(current.getBorn())
            ) {
                merged.add(current);
            } else {

                // merge last with current
                if (seq.get(last.born) >= seq.get(current.born)) {
                    last.setRight(current.getRight());
                } else {
                    merged.remove(merged.size()-1);
                    current.setLeft(last.getLeft());
                    merged.add(current);
                }
            }
        }
        // sort peaks by persistence
        merged.sort((a, b) -> Double.compare(getPersistence(b, seq), getPersistence(a, seq)));
        // delete all peaks which have a persistence lower the threshold
        if (persistenceThreshold > 0 && merged.size()>1) merged.removeIf(x->getPersistence(x, seq)<persistenceThreshold);
        return merged;
    }

    private static double getPersistence(Segment peak, TraceIntensityArray seq) {
        if (peak.getDied() > Integer.MIN_VALUE) {
            return seq.get(peak.getBorn()) - seq.get(peak.getDied());
        } else {
            return seq.get(peak.getBorn());
        }
    }

    @Override
    public List<TraceSegment> detectSegments(Trace trace, double intensityThreshold) {
        final int offset=trace.startId();
        return computePersistentHomology(trace, filter, highPrecisionMode ? intensityThreshold : 2*intensityThreshold, highPrecisionMode ? 2*intensityThreshold : 5*intensityThreshold,
                highPrecisionMode ? 0.01 : 0.1, highPrecisionMode ? 0.95 : 0.8, 3d).stream().map(seg->
                TraceSegment.createSegmentFor(trace, seg.left+offset, seg.right+offset)
        ).toList();
    }

    @Override
    public int[] detectMaxima(SampleStats stats, Trace trace) {
        return TraceSegmentationStrategy.super.detectMaxima(stats, trace);
    }

}
