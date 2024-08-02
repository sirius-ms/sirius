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

package de.unijena.bioinf.lcms.detection;

import de.unijena.bioinf.ms.persistence.model.core.Trace;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * <p>
 *     A class to find and compute the significance of each peak in a list of values. Algorithm sourced from
 *     <a href="https://www.sthu.org/blog/13-perstopology-peakdetection/index.html">Stefan Huber, FH Salzburg</a>.
 * </p>
 * <p>
 *     The idea of the algorithm is:
 * <pre>
 *     Consider a water level starting above the global maximum that continuously lowers its level. Whenever the water
 *     level reaches a local maximum a new island is born. Whenever it reaches a local minimum two islands merge, and we
 *     consider the lower island to be merged into the higher island. This gives a precise notion of a lifespan of an
 *     island; it is the significance we mentioned above, or the so-called persistence in persistent homology.
 * </pre>
 * </p>
 *
 * @see <a href="https://www.math.uri.edu/~thoma/comp_top__2018/stag2016.pdf">Fugacci et al., 2016</a>
 * @see <a href="https://www.sthu.org/research/publications/files/Hub20.pdf">Huber, 2020</a>
 */
public class PersistentHomology {

    public static List<ChromatographicPeak> computePersistentHomology(Trace trace, DoubleList seq, double intensityThreshold, double trim) {
        List<ChromatographicPeak> peaks = new ArrayList<>();

        int[] idx2Peak = new int[seq.size()];
        Arrays.fill(idx2Peak, -1);

        IntList indices = new IntArrayList(IntStream.range(0, seq.size()).toArray());
        indices.sort((a, b) -> Double.compare(seq.getDouble(b), seq.getDouble(a)));

        for (int idx : indices) {
            boolean leftDone = (idx > 0 && idx2Peak[idx - 1] > -1);
            boolean rightDone = (idx < seq.size() - 1 && idx2Peak[idx + 1] > -1);

            int il = leftDone ? idx2Peak[idx - 1] : -1;
            int ir = rightDone ? idx2Peak[idx + 1] : -1;

            // new peak born
            if (!leftDone && !rightDone) {
                peaks.add(new ChromatographicPeak(idx, trace));
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
                if (seq.getDouble(peaks.get(il).getBorn()) > seq.getDouble(peaks.get(ir).getBorn())) {
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
        peaks.sort(Comparator.comparingInt(ChromatographicPeak::getBorn));
        IntList valleys = new IntArrayList(peaks.stream().mapToInt(ChromatographicPeak::getDied).filter(died -> died > Integer.MIN_VALUE).sorted().toArray());
        valleys.add(0, 0);
        valleys.add(seq.size() - 1);

        for (int i = 0; i < peaks.size(); i++) {
            peaks.get(i).setLeft(valleys.getInt(i));
            peaks.get(i).setRight(valleys.getInt(i + 1));
        }

        peaks = peaks.stream().map(peak -> trimToXStdRT(peak, trim)).toList();

        if (peaks.isEmpty())
            return peaks;

        List<ChromatographicPeak> merged = new ArrayList<>();
        merged.add(peaks.get(0));

        for (int i = 1; i < peaks.size(); i++) {
            ChromatographicPeak current = peaks.get(i);
            ChromatographicPeak last = merged.get(merged.size() - 1);
            // do not merge two consecutive peaks if:
            // 1. there is a gap between both peaks
            // 2. the valley intensity is low (either below the int threshold or half max)
            if (last.getRight() < current.getLeft() ||
                    current.getTrace().getIntensities().getDouble(current.getLeft()) < intensityThreshold ||
                    current.getTrace().getIntensities().getDouble(current.getLeft()) < 0.5 * current.getTrace().getIntensities().getDouble(current.getBorn())
            ) {
                merged.add(current);
            } else {
                last.setRight(current.getRight());
            }
        }

        // sort peaks by persistence
        merged.sort((a, b) -> Double.compare(getPersistence(b, seq), getPersistence(a, seq)));
        return merged;
    }

    private static ChromatographicPeak trimToXStdRT(ChromatographicPeak peak, double x) {
        double mean = peak.getAverageRT();
        double std = peak.getStdRT(mean);
        for (int i = 0; i < peak.getTrace().getRts().size(); i++) {
            if (peak.getTrace().getRts().getDouble(i) < mean - x * std)
                continue;
            peak.setLeft(Math.max(peak.getLeft(), i));
            break;
        }
        for (int i = peak.getTrace().getRts().size() - 1; i >= 0 ; i--) {
            if (peak.getTrace().getRts().getDouble(i) > mean + x * std)
                continue;
            peak.setRight(Math.min(peak.getRight(), i));
            break;
        }
        return peak;
    }

    private static double getPersistence(ChromatographicPeak peak, DoubleList seq) {
        if (peak.getDied() > Integer.MIN_VALUE) {
            return seq.getDouble(peak.getBorn()) - seq.getDouble(peak.getDied());
        } else {
            return seq.getDouble(peak.getBorn());
        }
    }

}
