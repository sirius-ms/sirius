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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.filter.Filter;
import de.unijena.bioinf.lcms.trace.filter.GaussFilter;
import de.unijena.bioinf.lcms.trace.filter.NoFilter;
import de.unijena.bioinf.lcms.trace.filter.WaveletFilter;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
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

    /**
     * Feature intensity must be large than {@code noiseCoefficient * noiseLevel}
     */
    private final double noiseCoefficient;

    /**
     * Keep only features with more than {@code persistencecoefficent * max intensity} persistence
     */
    private final double persistenceCoefficient;

    /**
     * Merge neighboring features with less than {@code mergeCoefficient * feature intensity}.
     */
    private final double mergeCoefficient;

    /**
     * Trim features to {@code trim * standard deviation}. (Assumes features to be Gaussian-like.)
     */
    private final double trim = 3d;

    public PersistentHomology() {
        //this(new NoFilter(), 2.0, 0.1, 0.8);
        this(null, 3, 0d, 0.8);
    }

    public PersistentHomology(Filter filter) {
        this.filter = filter;
        this.noiseCoefficient = 3;
        this.persistenceCoefficient = 0.01;
        this.mergeCoefficient = 0.8;
    }

    public PersistentHomology(Filter filter, double noiseCoefficient, double persistenceCoefficient, double mergeCoefficient) {
        this.filter = filter;

        if (noiseCoefficient < 0)
            throw new IllegalArgumentException("noiseCoefficient must be >= 0! (was " + noiseCoefficient + ")");
        if (persistenceCoefficient < 0 || persistenceCoefficient > 1)
            throw new IllegalArgumentException("persistenceCoefficient must be >= 0 and <= 1! (was " + persistenceCoefficient + ")");
        if (mergeCoefficient < 0 || mergeCoefficient > 1)
            throw new IllegalArgumentException("mergeCoefficient must be >= 0 and <= 1! (was " + mergeCoefficient + ")");

        this.noiseCoefficient = noiseCoefficient;
        this.persistenceCoefficient = persistenceCoefficient;
        this.mergeCoefficient = mergeCoefficient;
    }

    public PersistentHomology(boolean highPrecisionMode) {
        //this(new NoFilter(), highPrecisionMode ? 1.0 : 2.0, highPrecisionMode ? 0.01 : 0.1, highPrecisionMode ? 0.95 : 0.8);
        this();
    }

    @Getter
    private static class Segment {

        @Setter
        private int left, right;

        private Segment leftNeighbour, rightNeighbour;
        private Valley leftValley, rightValley;
        private double noiseEstimate, averageApexHeight;
        private int noiseEstimateNormalization;


        private int bornTime, deadTime, mergedTo;

        private final int born;

        private boolean hasPointOfInterest;

        @Setter
        private int died = Integer.MIN_VALUE;

        public Segment(int idx) {
            this.born = this.left = this.right = idx;
            mergedTo=-1;
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
        private final BitSet pointsOfInterest, features;
        private final Filter filter;

        private final float[] intensities;
        private final int offset;

        @Getter
        private float apexIntensity;

        public TraceIntensityArray(Trace trace, int[] pointsOfInterest, Filter filter) {
            this.trace = trace;
            this.offset = trace.startId();
            this.filter = filter;
            this.intensities = filter();
            if (intensities != null) {
                this.apexIntensity = 0;
                for (float i : intensities) {
                    apexIntensity = Math.max(apexIntensity, i);
                }
            } else {
                this.apexIntensity = trace.apexIntensity();
            }
            this.pointsOfInterest = new BitSet(trace.length());
            this.features = new BitSet(trace.length());
            for (int poi : pointsOfInterest) {
                int poic = poi-this.offset;
                if (poic>=0 && poic<trace.length()) {
                    this.pointsOfInterest.set(poi-this.offset);
                }
            }
        }

        private float[] filter() {
            float[] vec;
            if (filter instanceof NoFilter) {
                final float[] ints = new float[size()];
                for (int k=0; k < size(); ++k) ints[k] = trace.intensity(k+offset);
                vec=ints;
            } else {
                final double[] ints = new double[size()];
                for (int k=0; k < size(); ++k) ints[k] = trace.intensity(k+offset);
                vec = MatrixUtils.double2float(filter.apply(ints));
            }
            // add tie breaker
            for (int i=1; i < vec.length; ++i) {
                if (vec[i]==vec[i-1]) {
                    vec[i] = Float.intBitsToFloat(Float.floatToRawIntBits(vec[i])+1);
                }
            }
            return vec;
        }

        public boolean isOfInterest(int index) {
            return pointsOfInterest.get(index);
        }

        public float get(int index) {
            if (intensities!=null) return intensities[index];
            return trace.intensity(index+offset);
        }

        public float unfiltered(int index) {
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
        public double getAverageRt(Segment segment, double noiseLevel) {
            double sum = trace.intensity(segment.born+offset) * trace.retentionTime(segment.born+offset);
            double norm = trace.intensity(segment.born+offset);
            for (int i = segment.born-1; i >= segment.left; --i) {
                final int j = i+offset;
                if (trace.intensity(j)<noiseLevel) break;
                sum += trace.intensity(j) * trace.retentionTime(j);
                norm += trace.intensity(j);
            }
            for (int i = segment.born+1; i <= segment.right; ++i) {
                final int j = i+offset;
                if (trace.intensity(j)<noiseLevel) break;
                sum += trace.intensity(j) * trace.retentionTime(j);
                norm += trace.intensity(j);
            }
            return sum / norm;
        }
        public double getStdRT(Segment segment, double mean, double noiseLevel) {
            double intSum = trace.intensity(segment.born+offset);
            double var = 0d;
            int mindex=segment.born, maxdex=segment.born;
            for (mindex = segment.born-1; mindex >= segment.left; --mindex) {
                final int j = mindex+offset;
                if (trace.intensity(j)<noiseLevel) break;
                intSum += trace.intensity(j);
            }
            for (maxdex = segment.born+1; maxdex <= segment.right; ++maxdex) {
                final int j = maxdex+offset;
                if (trace.intensity(j)<noiseLevel) break;
                intSum += trace.intensity(j);
            }
            ++mindex;

            for (int i = mindex; i < maxdex; i++) {
                final int j = i+offset;
                var += (trace.intensity(j) / intSum) * Math.pow((trace.retentionTime(j) - mean), 2);
            }
            return Math.sqrt(var);
        }
// modell peak als gaussian, cut bei 3*sigma
        private Segment trimToXStdRT(Segment peak, double x) {
            double mean = getAverageRt(peak);
            double std = getStdRT(peak,mean);
            for (int i = peak.left; i < peak.born; i++) {
                if ((trace.retentionTime(i+offset) > mean - x * std) && !pointsOfInterest.get(i) || features.get(i))
                    break;
                peak.setLeft(i);
            }
            for (int i = peak.right; i > peak.born ; i--) {
                if ((trace.retentionTime(i+offset) < mean + x * std)  && !pointsOfInterest.get(i) || features.get(i))
                    break;
                peak.setRight(i);
            }
            return peak;
        }
        private Segment trimToXStdRT(Segment peak, double x, double noiseLevel) {
            double mean = getAverageRt(peak, noiseLevel);
            double std = getStdRT(peak,mean, noiseLevel);
            for (int i = peak.left; i < peak.born; i++) {
                if ((trace.retentionTime(i+offset) > mean - x * std) || pointsOfInterest.get(i) || features.get(i))
                    break;
                peak.setLeft(i);
            }
            for (int i = peak.right; i > peak.born ; i--) {
                if ((trace.retentionTime(i+offset) < mean + x * std)  || pointsOfInterest.get(i) || features.get(i))
                    break;
                peak.setRight(i);
            }
            return peak;
        }

        public int size() {
            return trace.length();
        }

        public double stdInt() {

            final int windowLen = Math.max(3, trace.length()/30);
            final int stepSize = windowLen/3;
            double varmean=0d;
            int c=0;
            for (int i=0; i < trace.length()-windowLen; i+=stepSize) {
                double mean=0d, var=0d;
                for (int j=i; j < i+windowLen; ++j) {
                    mean += get(j);
                }
                mean /= windowLen;
                for (int j=i; j < i+windowLen; ++j) {
                    var += Math.pow(get(j)-mean,2);
                }
                var /= windowLen;
                varmean += Math.sqrt(var);
                ++c;
            }
            varmean /= c;
            return varmean;
        }

        public double sizeOf(int a, int b) {
            final double rtA = trace.retentionTime(a+offset);
            final double rtB = trace.retentionTime(b+offset);
            return Math.abs(rtA-rtB);
        }
    }

    List<Segment> persistentHom(Trace trace, Filter filter, double _noiseLevel, double expectedPeakWidth, int[] pointsOfInterestArray) {
        // if ANY point of interest lies within this trace, we want to keep it, even if intensity is super low
        if (Arrays.stream(pointsOfInterestArray).noneMatch(trace::inRange) &&  trace.apexIntensity() < noiseCoefficient * _noiseLevel) {
            return Collections.emptyList();
        }

        final TraceIntensityArray seq = new TraceIntensityArray(trace, pointsOfInterestArray, filter);

        // noise level has to be always larger than variance of data
        if (trace.length()>=256) {
            _noiseLevel = Math.max(seq.stdInt() * 3, _noiseLevel);
        } else if (trace.length()>=32) {
            _noiseLevel = Math.max(seq.stdInt() * 2, _noiseLevel);
        } else {
            _noiseLevel = Math.max(seq.stdInt(), _noiseLevel);
        }
        final double noiseLevel = _noiseLevel;

        List<Segment> peaks = new ArrayList<>();
        int[] idx2Peak = new int[seq.size()];
        Arrays.fill(idx2Peak, -1);
        IntList indices = new IntArrayList(IntStream.range(0, seq.size()).toArray());
        indices.sort((a, b) -> Double.compare(seq.get(b), seq.get(a)));

        for (int I=0; I < indices.size(); ++I) {
            int idx = indices.getInt(I);
            boolean leftDone = (idx > 0 && idx2Peak[idx - 1] > -1);
            boolean rightDone = (idx < seq.size() - 1 && idx2Peak[idx + 1] > -1);

            int il = leftDone ? idx2Peak[idx - 1] : -1;
            int ir = rightDone ? idx2Peak[idx + 1] : -1;

            // new peak born
            if (!leftDone && !rightDone) {
                Segment s = new Segment(idx);
                s.bornTime = I;
                peaks.add(s);
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
                    peaks.get(ir).deadTime = I;
                    peaks.get(il).setRight(peaks.get(ir).getRight());
                    idx2Peak[peaks.get(il).getRight()] = idx2Peak[idx] = il;
                } else {
                    peaks.get(il).setDied(idx);
                    peaks.get(il).deadTime = I;
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
        return peaks;
    }

    private List<Segment> computePersistentHomology(Trace trace, Filter filter, double _noiseLevel, double expectedPeakWidth, int[] pointsOfInterestArray) {
        // if ANY point of interest lies within this trace, we want to keep it, even if intensity is super low
        if (Arrays.stream(pointsOfInterestArray).noneMatch(trace::inRange) &&  trace.apexIntensity() < noiseCoefficient * _noiseLevel) {
            return Collections.emptyList();
        }

        final TraceIntensityArray seq = new TraceIntensityArray(trace, pointsOfInterestArray, filter);

        // noise level has to be always larger than variance of data

        if (trace.length()>=256) {
            _noiseLevel = Math.max(seq.stdInt() * 3, _noiseLevel);
        } else if (trace.length()>=32) {
            _noiseLevel = Math.max(seq.stdInt() * 2, _noiseLevel);
        } else {
            _noiseLevel = Math.max(seq.stdInt(), _noiseLevel);
        }
        final double noiseLevel = _noiseLevel;

        List<Segment> peaks = new ArrayList<>();
        int[] idx2Peak = new int[seq.size()];
        Arrays.fill(idx2Peak, -1);
        IntList indices = new IntArrayList(IntStream.range(0, seq.size()).toArray());
        indices.sort((a, b) -> Double.compare(seq.get(b), seq.get(a)));

        for (int idx : indices) {
            boolean leftDone = (idx > 0 && idx2Peak[idx - 1] > -1);
            boolean rightDone = (idx < seq.size() - 1 && idx2Peak[idx + 1] > -1);

            int il = leftDone ? idx2Peak[idx - 1] : -1;
            int ir = rightDone ? idx2Peak[idx + 1] : -1;

            // new peak born
            if (!leftDone && !rightDone) {
                Segment s = new Segment(idx);
                peaks.add(s);
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
            final int gapLength = last.getRight() - current.getLeft();
            // 2. the valley intensity is low (either below the int threshold or mergeCoeff * max int)
            final double valleyIntensity = Math.min(seq.get(last.born), seq.get(current.getBorn())) - seq.get(current.getLeft());
            final double relativeValleyIntensity = seq.get(current.getLeft()) / Math.min(seq.get(last.born), seq.get(current.getBorn()));
            boolean properValley;
            if (expectedPeakWidth>0) {
                final double valleySize = ((seq.sizeOf(current.left, current.born)) / expectedPeakWidth);
                properValley = ((mergeCoefficient - relativeValleyIntensity) + valleySize) > 0.33;
            } else {
                properValley = relativeValleyIntensity < mergeCoefficient;
            }

            /*
            System.out.println(((gapLength > 0 || (valleyIntensity > noiseLevel && properValley)) ? "split " : "merge ") +
                    last.born + "\t" + current.born + ": valley intensity = " + valleyIntensity + ", noise estimate = " + noiseLevel +
                    ", relative valley = " + relativeValleyIntensity + ", gapLength = " + gapLength);
            */
            if (gapLength > 0 || (valleyIntensity > noiseLevel && properValley)) {
                merged.add(current); // don't merge
            } else {
                // merge last with current
                if (seq.get(last.born) >= seq.get(current.born)) {
                    last.setRight(current.getRight());
                } else {
                    merged.remove(merged.size() - 1);
                    current.setLeft(last.getLeft());
                    merged.add(current);
                }
            }
        }
        // sort peaks by persistence
        merged.sort((a, b) -> Double.compare(getPersistence(b, seq), getPersistence(a, seq)));

        // remember which peaks contain my point of interests - these peaks cannot be deleted!
        int[] assigned = new int[pointsOfInterestArray.length];
        Arrays.fill(assigned,-1);
        for (int i=0; i  < merged.size(); ++i) {
            final Segment s = merged.get(i);
            for (int j=0; j < pointsOfInterestArray.length; ++j) {
                if (pointsOfInterestArray[j] >= (s.left+seq.offset) && pointsOfInterestArray[j] <= (s.right+seq.offset)) {
                    if (assigned[j]>=0) {
                        if (seq.get(merged.get(assigned[j]).getBorn()) < seq.get(s.getBorn())) {
                            assigned[j] = i;
                        }
                    } else {
                        assigned[j] = i;
                    }
                }
            }
        }
        for (int assignment : assigned) {
            if (assignment >= 0) {
                merged.get(assignment).hasPointOfInterest = true;
            }
        }

        // delete all peaks which have a persistence lower the persistence threshold
        if (merged.size()>1) merged.removeIf(x -> !x.hasPointOfInterest && getPersistence(x, seq) < persistenceCoefficient * seq.get(indices.getInt(0)));
        // delete all peaks below the intensity threshold
        merged.removeIf(x -> !x.hasPointOfInterest &&  IntStream.range(x.getLeft(), x.getRight() + 1).mapToDouble(seq::unfiltered).max().orElse(0) < noiseCoefficient * noiseLevel);

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
    public List<TraceSegment> detectSegments(Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest, int[] features) {
        final int offset=trace.startId();
        /*
        return computePersistentHomology(trace, filter, noiseLevel, expectedPeakWidth, pointsOfInterest).stream().map(seg->
                TraceSegment.createSegmentFor(trace, seg.left+offset, seg.right+offset)
        ).toList();
        */
        Filter f = (filter==null) ? getGaussianFilter(trace, expectedPeakWidth) : filter;
        return computePersistentHomologyHierarchical(trace, f, noiseLevel, expectedPeakWidth, pointsOfInterest, features).stream().map(seg->
                TraceSegment.createSegmentFor(trace, seg.left+offset, seg.right+offset)
        ).toList();

    }

    public static Filter getGaussianFilter(Trace t, double w) {
        if (w<=0) return new NoFilter();
        if (t.length()<=250) return new NoFilter();
        double binwidth = (t.retentionTime(t.endId())-t.retentionTime(t.startId()))/(t.endId()-t.startId());
        int bins = (int)Math.round(w/binwidth);
        if (bins<=1) return new NoFilter();
        else return new GaussFilter(bins);

    }

    public List<TraceSegment> detectSegmentsOld(Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest, int[] features) {
        final int offset=trace.startId();
        return computePersistentHomology(trace, filter, noiseLevel, expectedPeakWidth, pointsOfInterest).stream().map(seg->
                TraceSegment.createSegmentFor(trace, seg.left+offset, seg.right+offset)
        ).toList();

    }

    public String debugJson(Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest){
        return debugJson(trace,noiseLevel,expectedPeakWidth,pointsOfInterest,null);
    }
    public String debugJson(Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest, int[] apexesOfParent) {
        DebugModel model = new DebugModel(this, trace, noiseLevel, expectedPeakWidth, pointsOfInterest,apexesOfParent);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<TraceSegment> fromDebugModel(String json) {
        ObjectMapper m = new ObjectMapper();
        try {
            DebugModel debugModel = m.readValue(json, DebugModel.class);
            return new PersistentHomology(new NoFilter(), debugModel.noiseCoefficient, debugModel.persistenceCoefficient, debugModel.mergeCoefficient).detectSegments(
                    debugModel, debugModel.noiseLevel, debugModel.expectedPeakWidth, debugModel.pointsOfInterest
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DebugModel implements Trace {

        @JsonProperty double noiseCoefficient;
        @JsonProperty double persistenceCoefficient;
        @JsonProperty double mergeCoefficient;
        @JsonProperty double noiseLevel;
        @JsonProperty double expectedPeakWidth;
        @JsonProperty int[] pointsOfInterest;
        @JsonProperty double[] mz;
        @JsonProperty double[] rt;
        @JsonProperty float[] intensity;
        @JsonProperty int[] scanIds;
        @JsonProperty int startId;
        @JsonProperty int endId;
        @JsonProperty int apexId;
        @JsonProperty(required = false) int[] features;

        public DebugModel() {
        }

        public DebugModel(PersistentHomology pers, Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest, int[] features) {
            this.pointsOfInterest=pointsOfInterest;
            this.expectedPeakWidth=expectedPeakWidth;
            this.noiseLevel=noiseLevel;
            this.noiseCoefficient=pers.noiseCoefficient;
            this.persistenceCoefficient=pers.persistenceCoefficient;
            this.mergeCoefficient=pers.mergeCoefficient;
            this.startId=trace.startId();
            this.endId=trace.endId();
            this.apexId=trace.apex();
            this.features = features;
            this.mz = new double[trace.endId()-trace.startId()+1];
            this.rt = new double[trace.endId()-trace.startId()+1];
            this.intensity = new float[trace.endId()-trace.startId()+1];
            this.scanIds = new int[trace.endId()-trace.startId()+1];
            for (int i=trace.startId(); i <= trace.endId(); ++i) {
                int j = i-trace.startId();
                mz[j] = trace.mz(i);
                rt[j] = trace.retentionTime(i);
                intensity[j] = trace.intensityOrZero(i);
                scanIds[j] = trace.scanId(i);
            }
        }

        @Override
        public int startId() {
            return startId;
        }

        @Override
        public int endId() {
            return endId;
        }

        @Override
        public int apex() {
            return apexId;
        }

        @Override
        public double mz(int index) {
            return mz[index-startId];
        }

        @Override
        public double averagedMz() {
            return Arrays.stream(mz).average().orElse(0d);
        }

        @Override
        public double minMz() {
            return Arrays.stream(mz).min().orElse(0d);
        }

        @Override
        public double maxMz() {
            return Arrays.stream(mz).max().orElse(0d);
        }

        @Override
        public float intensity(int index) {
            return intensity[index-startId];
        }

        @Override
        public int scanId(int index) {
            return scanIds[index-startId];
        }

        @Override
        public double retentionTime(int index) {
            return rt[index-startId];
        }

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container C = new Container();
        frame.add(C);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static class Container extends JPanel {
        int pos;
        boolean newMode=false;
        File[] files;
        boolean dofilter = false;
        JLabel title;
        GraphCanvas inner;
        public Container() {
            this.files  = Arrays.stream(new File("/home/kaidu/analysis/debug/many").listFiles()).filter(x->x.getName().endsWith(".json")/* && x.getName().contains("merged")*/).toArray(File[]::new);
            setLayout(new BorderLayout());
            JButton before = new JButton(new AbstractAction("<") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    --pos;
                    if (pos<0) pos = files.length-1;
                    updateCanvas();
                }
            });
            JButton next = new JButton(new AbstractAction(">") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ++pos;
                    if (pos>=files.length) pos=0;
                    updateCanvas();
                }
            });
            JButton old = new JButton(new AbstractAction("old") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    newMode=false;
                    updateCanvas();
                }
            });
            // Now setting up the global hotkey

            JButton newmode = new JButton(new AbstractAction("new") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    newMode=true;
                    updateCanvas();
                }
            });
            JCheckBox filter = new JCheckBox("filter,", dofilter);
            filter.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    dofilter = filter.isSelected();
                    updateCanvas();
                }
            });
            add(before, BorderLayout.WEST);
            add(next, BorderLayout.EAST);
            title = new JLabel("");
            add(title, BorderLayout.NORTH);
            Box horizontalBox = Box.createHorizontalBox();
            horizontalBox.add(old);
            horizontalBox.add(newmode);
            horizontalBox.add(filter);
            add(horizontalBox, BorderLayout.SOUTH);
            pos=0;
            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            kfm.addKeyEventDispatcher( new KeyEventDispatcher() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                    if (keyStroke.equals(KeyStroke.getKeyStroke("A"))) {
                        newMode=false;
                        updateCanvas();
                    } else if (keyStroke.equals(KeyStroke.getKeyStroke("D"))) {
                        newMode=true;
                        updateCanvas();
                    }
                    if (keyStroke.equals(KeyStroke.getKeyStroke("LEFT"))) {
                        --pos;
                        if (pos<0) pos = files.length-1;
                        updateCanvas();
                    } else if (keyStroke.equals(KeyStroke.getKeyStroke("RIGHT"))) {
                        ++pos;
                        if (pos>=files.length) pos=0;
                        updateCanvas();
                    } else if (keyStroke.equals(KeyStroke.getKeyStroke("SPACE"))) {
                        System.out.println(files[pos]);
                    }
                    return true;
                }
            });

            updateCanvas();
        }

        private void updateCanvas() {
            File filename = files[pos];
            try {
                ObjectMapper m = new ObjectMapper();
                DebugModel debugModel = m.readValue(FileUtils.read(filename), DebugModel.class);
                GraphCanvas c = new GraphCanvas(debugModel, newMode, dofilter);
                if (inner!=null)remove(inner);
                inner=c;
                add(inner, BorderLayout.CENTER);
                title.setText(filename.getName());
                updateUI();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static class GraphCanvas extends Canvas {
        DebugModel m;
        double rtStart, rtEnd;
        double intEnd;
        double[] filtered;
        int margin;
        int width, height;
        boolean filter;
        List<TraceSegment> segments;
        List<Segment> pers;
        Color[] pastelColors;
        public GraphCanvas(DebugModel m, boolean newMode, boolean filter) {
            this.m = m;
            m.expectedPeakWidth = 4;
            this.filter=filter;
            intEnd = 0d;
            for (float f : m.intensity) intEnd = Math.max(intEnd, f);
            rtStart = m.rt[0];
            rtEnd = m.rt[m.rt.length-1];
            margin = 32;
            width = 1800;
            height = 896;
            setPreferredSize(new Dimension(width+4*margin,height+4*margin));
            int scale = (int)Math.ceil(m.expectedPeakWidth / (m.retentionTime(m.apexId)-m.retentionTime(m.apexId-1)));
            if (m.features==null) m.features=new int[0];
            Filter F = filter ? new GaussFilter(3) : new NoFilter();
            if (filter) {
                filtered = MatrixUtils.float2double(m.intensity);
                filtered = F.apply(filtered);
            } else {
                filtered=null;
            }
            if (newMode) {
                /*
                m.noiseCoefficient=2;
                m.mergeCoefficient=0.8;
                if(m.expectedPeakWidth<=0) m.expectedPeakWidth = 4;
                */
                segments = new PersistentHomology(F, m.noiseCoefficient, m.persistenceCoefficient, m.mergeCoefficient).detectSegments(
                        m, m.noiseLevel, m.expectedPeakWidth, new int[0]
                );
                pers = new PersistentHomology(F, m.noiseCoefficient, m.persistenceCoefficient, m.mergeCoefficient).persistentHom(
                        m, F, m.noiseLevel, m.expectedPeakWidth, new int[0]
                );
            } else {
                segments = new PersistentHomology(F, m.noiseCoefficient, m.persistenceCoefficient, m.mergeCoefficient).detectSegments(
                        m, m.noiseLevel, m.expectedPeakWidth, m.pointsOfInterest, m.features
                );
            }
            segments = segments.stream().sorted(Comparator.comparingInt(x->x.apex)).toList();
            m.pointsOfInterest = Arrays.stream(m.pointsOfInterest).filter(x-> x>= m.startId && x <= m.endId).toArray();
            /*
            pers = new PersistentHomology(F, m.noiseCoefficient, m.persistenceCoefficient, m.mergeCoefficient).persistentHom(
                    m, new NoFilter(), m.noiseLevel, m.expectedPeakWidth, m.pointsOfInterest
            );
             */

            List<Color> pastelColors = new ArrayList<>();
            pastelColors.add(new Color(255, 179, 186)); // Pastel Pink
            pastelColors.add(new Color(255, 223, 186)); // Pastel Peach
            pastelColors.add(new Color(255, 255, 186)); // Pastel Yellow
            pastelColors.add(new Color(186, 255, 201)); // Pastel Mint
            pastelColors.add(new Color(186, 225, 255)); // Pastel Light Blue
            pastelColors.add(new Color(201, 186, 255)); // Pastel Lavender
            pastelColors.add(new Color(255, 186, 244)); // Pastel Pinkish Purple
            pastelColors.add(new Color(255, 207, 186)); // Pastel Coral
            pastelColors.add(new Color(230, 230, 250)); // Pastel Lavender Blue
            pastelColors.add(new Color(204, 229, 255)); // Pastel Light Sky Blue
            this.pastelColors = pastelColors.toArray(Color[]::new);

            //List<Segment> ord = pers.stream().sorted(Comparator.comparingInt(x -> x.bornTime)).toList();
            //for (int i=0; i < ord.size(); ++i) ord.get(i).bornTime=i;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D G = (Graphics2D) g;
            G.drawRect(margin+1, margin+1, width+2*margin - 1, height+2*margin - 1);
            int xxxx=0;
            for (TraceSegment s : segments) {
                double x1 = m.retentionTime(s.leftEdge);
                double x3 = m.retentionTime(s.rightEdge);
                G.setColor(pastelColors[xxxx++ % pastelColors.length]);
                G.fillRect(x(x1), y(intEnd), x(x3)-x(x1), y(0)-y(intEnd));;
            }
            xxxx=0;
            for (TraceSegment s : segments) {
                double x1 = m.retentionTime(s.leftEdge);
                double x3 = m.retentionTime(s.rightEdge);
                G.setColor(pastelColors[xxxx++ % pastelColors.length]);
                G.drawRect(x(x1), y(intEnd), x(x3)-x(x1), y(0)-y(intEnd));;
            }

            G.setStroke(new BasicStroke(2));

            G.setColor(Color.GRAY);
            G.drawLine(x(rtStart), y(m.noiseLevel), x(rtEnd), y(m.noiseLevel));


            G.setColor(Color.RED);
            // draw data points
            ;

            for (int k=m.startId; k < m.endId; ++k) {
                G.drawLine(x(m.retentionTime(k)), y(m.intensity(k)), x(m.retentionTime(k+1)), y(m.intensity(k+1)));
            }
            G.setColor(Color.BLACK);
            if (filtered!=null) {
                for (int k=m.startId; k < m.endId; ++k) {
                    G.drawLine(x(m.retentionTime(k)), y(filtered[k-m.startId]), x(m.retentionTime(k+1)), y(filtered[k+1-m.startId]));
                }
            }
            G.setColor(Color.RED);
            for (int p : m.pointsOfInterest) {
                int xx = x(m.retentionTime(p));
                int yy = y(m.intensity(p));
                G.drawOval(xx-5, yy-5, 10, 10);
            }

            if (pers!=null) {
                for (int i = 0; i < pers.size(); ++i) {
                    int xx = x(m.retentionTime(pers.get(i).born + m.startId));
                    int yy = y(m.intensity(pers.get(i).born + m.startId));
                    G.drawString(String.valueOf(pers.get(i).born), xx, yy - 10);
                }
            }


            for (TraceSegment s : segments) {
                double x1 = m.retentionTime(s.leftEdge);
                double x2 = m.retentionTime(s.apex);
                double x3 = m.retentionTime(s.rightEdge);
                G.setStroke(new BasicStroke(2));
                G.setColor(Color.BLACK);
                G.drawLine(x(x2), y(0), x(x2), y(m.intensity(s.apex)));
                final BasicStroke dashed =
                        new BasicStroke(2.0f,
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER,
                                10.0f, new float[]{10.0f}, 0.0f);
                G.setStroke(dashed);
                G.setColor(Color.BLACK);
                G.drawLine(x(x1), y(intEnd), x(x1), y(0));
                G.drawLine(x(x3), y(intEnd), x(x3), y(0));
                G.drawString(String.valueOf(s.apex), x(x2), y(m.intensity(s.apex))-10);
            }
            if (m.expectedPeakWidth>0) {
                for (double f = rtStart; f < rtEnd; f += m.expectedPeakWidth){
                    final int y1 = y(0)+margin-16;
                    final int y2 = y1 +4;
                    G.drawLine(x(f),y1,x(f),y2);
                }
            }
            final int stepsize = Math.max(1, (m.endId-m.startId)/5);
            for (int j=m.startId; j < m.endId; j += stepsize) {
                final double rt = m.retentionTime(j);
                final int x = x(rt);
                final int y = y(0);
                G.drawString((int)rt + " s", x, y + margin - 4);
            }


        }

        private int y(double v) {
            return (int)((height+2*margin) - (v / intEnd * height));
        }
        private int x(double v) {
            return (int)(margin + (v-rtStart)/(rtEnd-rtStart) * width);
        }
    }



    private List<TraceSegment> computeTraceSegmentsWithPersistentHomologyHierarchical(Trace trace, double _noiseLevel, double expectedPeakWidth, int[] pointsOfInterestArray) {
        final int offset = trace.startId();
        return computePersistentHomologyHierarchical(trace, filter, _noiseLevel, expectedPeakWidth, pointsOfInterestArray, new int[0]).stream().map(seg->
                TraceSegment.createSegmentFor(trace, seg.left+offset, seg.right+offset)
        ).toList();
    }

    private List<Segment> computePersistentHomologyHierarchical(Trace trace, Filter filter, double _noiseLevel, double expectedPeakWidth, int[] pointsOfInterestArray_, int[] features_) {
        // if ANY point of interest lies within this trace, we want to keep it, even if intensity is super low
        // otherwise, if even the largest intensity of this trace is below noise intensity, reject it
        if (Arrays.stream(pointsOfInterestArray_).noneMatch(trace::inRange) &&  trace.apexIntensity() < noiseCoefficient * _noiseLevel) {
            return Collections.emptyList();
        }
        final int[] pointsOfInterestArray = Arrays.stream(pointsOfInterestArray_).filter(x->x>=trace.startId() && x <= trace.endId()).sorted().toArray();
        final int[] features = Arrays.stream(features_).filter(x->x>=trace.startId() && x <= trace.endId()).sorted().toArray();

        // the original trace array is a bit complicated with its index offset and so on. We wrap it into a separate
        // trace object that transparently handles smoothing and index operations from zero offset
        final TraceIntensityArray seq = new TraceIntensityArray(trace, pointsOfInterestArray, filter);
        // spread points of interests to their closest apex
        for (int point : pointsOfInterestArray) {
            final int p = point-seq.offset;
            int j;
            for (j=p+1; j < seq.size(); ++j) {
                if (seq.get(j)<=seq.get(j-1)) break;
            }
            --j;
            seq.pointsOfInterest.set(j);
            for (j=p-1; j > 0; --j) {
                if (seq.get(j)<=seq.get(j+1)) break;
            }
            ++j;
            seq.pointsOfInterest.set(j);
        }
        // same with features
        Int2IntOpenHashMap peakidx2featureidx = new Int2IntOpenHashMap();
        peakidx2featureidx.defaultReturnValue(-1);
        for (int point : features) {
            final int p = point-seq.offset;
            int j;
            for (j=p+1; j < seq.size(); ++j) {
                if (seq.get(j)<=seq.get(j-1)) break;
            }
            --j;
            final int apexRight = j;
            for (j=p-1; j >= 0; --j) {
                if (seq.get(j)<=seq.get(j+1)) break;
            }
            ++j;
            final int apexLeft = j;
            if (apexLeft==apexRight && apexLeft==p) { // case 1: point is maximum itself
                peakidx2featureidx.put(p, point);
            } else if (apexLeft != p && ((p-apexLeft) < (apexRight-p) ) || apexRight==p) { // case 2: left peak is closest maximum
                peakidx2featureidx.put(apexLeft, point);
            } else { // right peak is closest maximum
                peakidx2featureidx.put(apexRight, point);
            }
        }
        // will contain all maxima from the sequence
        List<Segment> peaks = new ArrayList<>();
        // maps each data point to the index of its corresponding maximum in the peaks array
        int[] idx2Peak = new int[seq.size()];
        Arrays.fill(idx2Peak, -1);
        // first sort all data points by their intensity
        IntList indices = new IntArrayList(IntStream.range(0, seq.size()).toArray());
        indices.sort((a, b) -> Double.compare(seq.get(b), seq.get(a)));
        // add some kind of baseline to noise
        final double givenNoiseLevel = _noiseLevel;
        _noiseLevel += seq.get(indices.getInt((int)(indices.size()*0.9)))/5d;
        final double traceNoiseLevel = Math.min(seq.get(indices.getInt((int)(indices.size()*0.75))), givenNoiseLevel);
        final double noiseLevel = _noiseLevel;
        // now iterate data points, starting from the highest intensity
        for (int idx : indices) {
            // leftDone is true when the peak before has larger intensity
            boolean leftDone = (idx > 0 && idx2Peak[idx - 1] > -1);
            // analog, rightDone. If both are true, we found a minimum
            boolean rightDone = (idx < seq.size() - 1 && idx2Peak[idx + 1] > -1);
            // rank of neighbouring peaks
            int il = leftDone ? idx2Peak[idx - 1] : -1;
            int ir = rightDone ? idx2Peak[idx + 1] : -1;

            // new peak born - we found a MAXIMUM
            if (!leftDone && !rightDone) {
                Segment s = new Segment(idx);
                peaks.add(s);
                idx2Peak[idx] = peaks.size() - 1;
            }

            // merge to next peak left - we found a left flank of a maximum
            Segment leftPeak = il >= 0 ? peaks.get(il) : null;
            if (leftDone && !rightDone) {
                leftPeak.setRight(leftPeak.getRight() + 1);
                idx2Peak[idx] = il;
            }

            // merge to next peak right - we found a right flank of a maximum
            Segment rightPeak = ir >= 0 ? peaks.get(ir) : null;
            if (!leftDone && rightDone) {
                rightPeak.setLeft(rightPeak.getLeft() - 1);
                idx2Peak[idx] = ir;
            }

            // merge left and right peaks - we found a MINIMUM
            if (leftDone && rightDone) {
                final Segment leftApex = leftPeak;
                final Segment rightApex = rightPeak;
                final float minimum = seq.get(idx);
                // 2. the valley intensity is low (either below the int threshold or mergeCoeff * max int)
                final double valleyIntensity = Math.min(seq.get(leftApex.born)  - minimum, seq.get(rightApex.born) -  minimum);
                final double relativeValleyIntensity = 1d-Math.sqrt((1d-minimum/(seq.get(leftApex.born))) * (1d-minimum/seq.get(rightApex.born)));
                final boolean poi = (seq.get(leftApex.born)<traceNoiseLevel && seq.pointsOfInterest.get(leftApex.born)) ||
                        (seq.get(rightApex.born)<traceNoiseLevel && seq.pointsOfInterest.get(rightApex.born));
                final boolean properValley = valleyIntensity > traceNoiseLevel && relativeValleyIntensity <= mergeCoefficient;
                final boolean largeDistance = expectedPeakWidth > 0 && seq.sizeOf(rightApex.born, leftApex.born) > 6 * expectedPeakWidth;
                // special rule (argh): if one of the two peaks is in noise but a point of interest, do not merge

                // yet another special rule: if both apexes belong to a different feature, ALWAYS SPLIT THEM
                final boolean differentFeatures = peakidx2featureidx.get(leftApex.born)!=peakidx2featureidx.get(rightApex.born) && peakidx2featureidx.get(leftApex.born)>=0 &&
                        peakidx2featureidx.get(rightApex.born)>=0;

                if (poi || properValley || largeDistance || differentFeatures) {
                    // split both peaks
                    if (seq.get(leftPeak.getBorn()) > seq.get(rightPeak.getBorn())) {
                        rightPeak.setDied(idx);
                    } else {
                        leftPeak.setDied(idx);
                    }
                    leftPeak.right = idx;
                    rightPeak.left = idx;
                } else {
                    // merge both peaks
                    // left was born earlier: merge right to left
                    if (seq.get(leftPeak.getBorn()) > seq.get(rightPeak.getBorn())) {
                        rightPeak.setDied(idx);
                        leftPeak.noiseEstimate += rightPeak.noiseEstimate;
                        leftPeak.averageApexHeight += rightPeak.averageApexHeight;
                        leftPeak.noiseEstimate += Math.min(seq.get(rightPeak.born)-seq.get(idx),seq.get(leftPeak.born)-seq.get(idx));
                        leftPeak.averageApexHeight += seq.get(rightPeak.born);
                        leftPeak.noiseEstimateNormalization += 1 + rightPeak.noiseEstimateNormalization;
                        if (peakidx2featureidx.get(rightPeak.born)>=0) peakidx2featureidx.put(leftPeak.born, peakidx2featureidx.get(rightPeak.born));
                        leftPeak.setRight(rightPeak.getRight());
                        idx2Peak[leftPeak.getRight()] = idx2Peak[idx] = il;
                        rightPeak.mergedTo = il;

                    } else {
                        rightPeak.noiseEstimate += leftPeak.noiseEstimate;
                        rightPeak.averageApexHeight += leftPeak.averageApexHeight;
                        rightPeak.noiseEstimate += Math.min(seq.get(leftPeak.born)-seq.get(idx),seq.get(rightPeak.born)-seq.get(idx));
                        rightPeak.averageApexHeight += seq.get(leftPeak.born);
                        rightPeak.noiseEstimateNormalization += 1 + leftPeak.noiseEstimateNormalization;
                        if (peakidx2featureidx.get(leftPeak.born)>=0) peakidx2featureidx.put(rightPeak.born, peakidx2featureidx.get(leftPeak.born));
                        leftPeak.setDied(idx);
                        rightPeak.setLeft(leftPeak.getLeft());
                        idx2Peak[rightPeak.getLeft()] = idx2Peak[idx] = ir;
                        leftPeak.mergedTo = ir;
                    }
                }
            }
        }

        // now merge peaks together if
        // a.) there variance is larger than the valley intensity
        // b.)
        peaks.removeIf(x->x.mergedTo>=0);
        //if(true) return peaks;
        peaks.sort(Comparator.comparingInt(x->x.born));
        PriorityQueue<Valley> valleys = new PriorityQueue<>(Comparator.comparingInt(x->x.width));
        for (int i=1; i < peaks.size(); ++i) {
            Segment before = peaks.get(i-1);
            Segment next = peaks.get(i);
            before.rightNeighbour=next;
            next.leftNeighbour=before;
            Valley v = new Valley(before.right, before, next, seq);
            before.rightValley = v;
            next.leftValley = v;
            valleys.offer(v);
        }
        // add pseudo values left and right
        while (!valleys.isEmpty()) {
            Valley v = valleys.poll();
            if (v.outdated) continue;
            int len = v.right.right - v.left.left + 1;
            boolean merge;
            if (len >= 8) {
                final double noiseEstimate = getNoiseEstimate(seq, v.left, v.right, v, 0.25f);
                final double valleyintensity = Math.min(seq.get(v.left.born), seq.get(v.right.born)) - seq.get(v.idx);
                final double relativeValleyIntensity = valleyintensity/Math.sqrt(seq.get(v.left.born) * seq.get(v.right.born));
                //final boolean gap = seq.get(v.idx) < _noiseLevel && Math.min(seq.get(v.left.born), seq.get(v.right.born)) > _noiseLevel*noiseCoefficient;
                final boolean gap = seq.get(v.idx) < traceNoiseLevel && Math.max((double)seq.get(v.left.born), seq.get(v.right.born)) > traceNoiseLevel*noiseCoefficient;

                boolean peakIsTooLarge = false;
                boolean peakIsTooShort = false;
                //String peakWidthInfo = "";
                if (expectedPeakWidth>0) {
                    int[] peakLeftRange = getFwhmRange(seq, v.left);
                    int[] peakRightRange = getFwhmRange(seq, v.right);
                    final double peakLeft = seq.sizeOf(peakLeftRange[0], peakLeftRange[1]) / expectedPeakWidth;
                    final double peakRight = seq.sizeOf(peakRightRange[0], peakRightRange[1]) / expectedPeakWidth;
                    final double combinedPeakWidth = seq.sizeOf(peakLeftRange[0], peakRightRange[1]) / expectedPeakWidth;
                    // wenn ein Peak deutlich zu groß und der andere Peak groß genug ist, merge nicht
                    if (combinedPeakWidth>=5 && Math.min(peakLeft,peakRight)>=1) {
                        peakIsTooLarge = true;
                    } else if (combinedPeakWidth >= 1 && Math.max(peakLeft, peakRight) <= 1) {
                        peakIsTooShort = true;
                    }
                    //peakWidthInfo = " combined peakwidth = " + combinedPeakWidth + ", left = " + peakLeft + ", right = " + peakRight + ", " + (peakIsTooLarge ? " too large!" : "") +
                    //        (peakIsTooShort ? " too short!" : "");
                } else {
                    //peakWidthInfo = "";
                }
                final boolean properValley;
                if (peakIsTooLarge) {
                    // we want to split except the valley is really non-existant
                    properValley = (valleyintensity > Math.min(traceNoiseLevel,noiseEstimate) && relativeValleyIntensity>=0.1);
                } else if (peakIsTooShort) {
                    // we want to merge, except the valley is really strong
                    properValley = (valleyintensity > (traceNoiseLevel+4*noiseEstimate)  && relativeValleyIntensity>=0.25);
                } else {
                    // default
                    properValley = (valleyintensity > 2*Math.min(traceNoiseLevel,noiseEstimate) && relativeValleyIntensity>=0.15);
                }

                // yet another special rule: if both apexes belong to a different feature, ALWAYS SPLIT THEM
                final boolean differentFeatures = peakidx2featureidx.get(v.left.born)!=peakidx2featureidx.get(v.right.born) && peakidx2featureidx.get(v.left.born)>=0 &&
                        peakidx2featureidx.get(v.right.born)>=0;


                merge = !differentFeatures && (relativeValleyIntensity<0.1 || (!properValley && !gap));
                //System.out.println((merge ? "merge " : "split ") + v.left.born + "\t" + v.right.born + ": valley intensity = " + valleyintensity + ", relative = " + relativeValleyIntensity + ", noise estimate = "+ noiseEstimate + ", " + (gap ? "with gap" : "no gap") + peakWidthInfo);
            } else {
                merge=true;
            }
            if (merge) {
                if (seq.get(v.right.born)>seq.get(v.left.born)) {
                    // merge the left peak into the right peak, the right peak remains
                    Segment merged = v.right;
                    v.outdated = true;
                    merged.left = v.left.left;
                    merged.leftNeighbour = v.left.leftNeighbour;
                    merged.averageApexHeight += v.left.averageApexHeight + seq.get(v.left.born);
                    merged.noiseEstimate += v.right.noiseEstimate;
                    merged.noiseEstimate += Math.min(seq.get(v.right.born)-seq.get(v.idx),seq.get(v.left.born)-seq.get(v.idx));
                    merged.noiseEstimateNormalization += 1 + v.right.noiseEstimateNormalization;
                    if (peakidx2featureidx.get(v.left.born)>=0) peakidx2featureidx.put(v.right.born, peakidx2featureidx.get(v.left.born));
                    if (merged.leftNeighbour!=null) {
                        v.left.leftValley.outdated=true;
                        merged.leftValley = new Valley(v.left.leftValley.idx, merged.leftNeighbour, merged, seq);
                        merged.leftNeighbour.rightValley = merged.leftValley;
                        merged.leftNeighbour.rightNeighbour=merged;
                        valleys.offer(merged.leftValley);
                    }
                    v.left.mergedTo=v.right.born;
                } else {
                    // merge the right peak into the left peak, the left peak remains
                    Segment merged = v.left;
                    v.outdated=true;
                    merged.right = v.right.right;
                    merged.rightNeighbour = v.right.rightNeighbour;
                    merged.noiseEstimate += v.left.noiseEstimate;
                    merged.averageApexHeight += v.right.averageApexHeight + seq.get(v.right.born);
                    merged.noiseEstimate += Math.min(seq.get(v.left.born)-seq.get(v.idx), seq.get(v.right.born)-seq.get(v.idx));
                    merged.noiseEstimateNormalization += 1 + v.left.noiseEstimateNormalization;
                    if (peakidx2featureidx.get(v.right.born)>=0) peakidx2featureidx.put(v.left.born, peakidx2featureidx.get(v.right.born));
                    if (merged.rightNeighbour!=null) {
                        v.right.rightValley.outdated=true;
                        merged.rightValley = new Valley(v.right.rightValley.idx, merged, merged.rightNeighbour, seq);
                        merged.rightNeighbour.leftValley = merged.rightValley;
                        merged.rightNeighbour.leftNeighbour=merged;
                        valleys.offer(merged.rightValley);
                    }
                    v.right.mergedTo=v.left.born;
                }
            }
        }
        peaks.removeIf(x->x.mergedTo>=0);
        //if(true)return peaks;
        // finally, remove peaks that have no point of interest and too low intensity
        peaks.removeIf(x->{
            final boolean closeToNoise = seq.get(x.born) <= noiseCoefficient*Math.max((x.noiseEstimateNormalization>0 ? x.noiseEstimate/x.noiseEstimateNormalization : noiseLevel), noiseLevel);
            if (!closeToNoise) return false;
            if (peakidx2featureidx.get(x.born)>=0) return false;
            int j = Arrays.binarySearch(pointsOfInterestArray, x.left+trace.startId());
            if (j < 0) {
                j = -j;
                --j;
            }
            for (; j < pointsOfInterestArray.length; ++j) {
                final int q = pointsOfInterestArray[j]-trace.startId();
                if ( q >= x.left && q <= x.right  ) return false;
                else if (q > x.right) break;
            }
            return true;
        });
        //if(true)return peaks;
        // trim peaks
        peakidx2featureidx.forEach((x,y)->seq.features.set(x));
        return peaks.stream().map(peak -> seq.get(peak.born)<noiseLevel ? seq.trimToXStdRT(peak, trim) : seq.trimToXStdRT(peak, trim, noiseLevel)).toList();

    }

    private double getFwhm(TraceIntensityArray seq, Segment s) {
        int l=s.born, r=s.born;
        double threshold = seq.get(s.born)/2d;
        for (; l > s.left; --l) {
            if (seq.get(l)<threshold)break;
        }
        for (; r < s.right; ++r) {
            if (seq.get(r)<threshold)break;
        }
        return seq.sizeOf(l,r);
    }
    private int[] getFwhmRange(TraceIntensityArray seq, Segment s) {
        int l=s.born, r=s.born;
        double threshold = seq.get(s.born)/2d;
        for (; l > s.left; --l) {
            if (seq.get(l)<threshold)break;
        }
        for (; r < s.right; ++r) {
            if (seq.get(r)<threshold)break;
        }
        return new int[]{l,r};
    }

    private double getNoiseEstimate(TraceIntensityArray seq, Segment left, Segment right, Valley valley, float relativeMargin) {
        final double n = (left.noiseEstimateNormalization+right.noiseEstimateNormalization);
        return n <= 2 ? 0 :  (left.noiseEstimate+right.noiseEstimate)/n;
    }

    private static class Valley {
        int idx;
        Segment left; Segment right; int width; boolean outdated;
        public Valley(int idx, Segment left, Segment right, TraceIntensityArray seq) {
            this.idx = idx;
            this.left = left;
            this.right = right;
            this.width = (left==null || right==null) ? Integer.MAX_VALUE : right.born - left.born;
            this.outdated=left==null || right==null;
            if (seq.get(left.born) <= seq.get(idx) || seq.get(right.born) <= seq.get(idx)) {
                throw new RuntimeException("WTF?");
            }
        }
    }


}
