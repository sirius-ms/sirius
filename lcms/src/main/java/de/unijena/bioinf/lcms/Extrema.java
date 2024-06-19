/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;

public class Extrema {

    protected final TDoubleArrayList extrema;
    protected final TIntArrayList indizes;

    public Extrema() {
        this.extrema = new TDoubleArrayList();
        this.indizes = new TIntArrayList();
    }

    /**
     * When using a smoothing average window approach, all extrema are shifted to the right by design.
     * This function moves them back to the left where they belong to.
     */
    public void fixSmoothed(double[] unsmoothedIntensities) {

        final boolean[] extremaType = new boolean[extrema.size()];
        boolean change;
        do {
            change = false;
            for (int k = 0; k < extremaType.length; ++k) extremaType[k] = isMinimum(k);
            for (int k = 0; k < indizes.size(); ++k) {
                final int now = indizes.get(k);
                final int before = k == 0 ? 0 : indizes.get(k - 1);
                final int after = k + 1 >= indizes.size() ? unsmoothedIntensities.length : indizes.get(k + 1);
                if (!extremaType[k]) {
                    int max = now;
                    for (int j = before + 1; j < after; ++j) {
                        if (unsmoothedIntensities[j] > unsmoothedIntensities[max]) max = j;
                    }
                    if (max != now) {
                        indizes.set(k, max);
                        change = true;
                    }
                    extrema.set(k, unsmoothedIntensities[max]);
                }
            }
            for (int k = 0; k < indizes.size(); ++k) {
                final int now = indizes.get(k);
                final int before = k == 0 ? 0 : indizes.get(k - 1);
                final int after = k + 1 >= indizes.size() ? unsmoothedIntensities.length : indizes.get(k + 1);
                if (extremaType[k]) {
                    int min = now;
                    for (int j = before + 1; j < after; ++j) {
                        if (unsmoothedIntensities[j] < unsmoothedIntensities[min]) min = j;
                    }
                    if (min!=now) {
                        indizes.set(k, min);
                        change=true;
                    }
                    extrema.set(k, unsmoothedIntensities[min]);
                }
            }
        } while (change);
    }

    public int getIndexAt(int k)  {
        return indizes.getQuick(k);
    }

    public boolean valid() {
        boolean minimum = isMinimum(0);
        for (int k=1; k < indizes.size(); ++k) {
            if (minimum) {
                if (extrema.getQuick(k) < extrema.getQuick(k-1)) return false;
            } else {
                if (extrema.getQuick(k) > extrema.getQuick(k-1)) return false;
            }
            minimum = !minimum;
        }
        return true;
    }

    public void smoothoutExtrema(double[] intensities) {
        TDoubleArrayList medianSlopes = new TDoubleArrayList();
        while(indizes.size() > 3) {
            medianSlopes.clear();
            // find extremum with smallest intensity
            double f = Double.POSITIVE_INFINITY;
            int smalli = -1;
            for (int k=0; k < extrema.size(); ++k) {
                if (!isMinimum(k)) {
                    if (intensities[indizes.get(k)] < f) {
                        f =intensities[indizes.get(k)];
                        smalli = k;
                    }
                }
            }
            if (smalli>=0) {
                int apex = indizes.get(smalli);
                int from = (smalli==0 ? 0 : indizes.get(smalli-1));
                int to = (smalli+1>=indizes.size() ? intensities.length-1 : indizes.get(smalli+1));
                boolean delete=false;
                if (to-from < 3) {
                    delete=true;
                } else {
                    double signum = Math.signum(intensities[from+1]-intensities[from]);
                    int prev=from;
                    for (int i=from+1; i <= to; ++i) {
                        double signum2 = Math.signum(intensities[i]-intensities[i-1]);
                        if (signum2!=signum) {
                            signum=signum2;
                            medianSlopes.add(Math.abs(intensities[prev]-intensities[i-1]));
                            prev=i-1;
                        }
                    }
                    medianSlopes.add(Math.abs(intensities[to]-intensities[prev]));
                    if (medianSlopes.size()<2) break;
                    medianSlopes.sort();
                    // remove main peak
                    medianSlopes.removeAt(medianSlopes.size()-1);
                    medianSlopes.removeAt(medianSlopes.size()-1);
                    double medianSlope = medianSlopes.isEmpty() ? 0 : medianSlopes.get(medianSlopes.size()/2);
                    double slope = (intensities[apex]-intensities[from]) + (intensities[apex]-intensities[to]);
                    delete = slope < 5*medianSlope && intensities[apex] < 5*Math.min(intensities[from], intensities[to]);
                }
                if (delete) {
                    // find smallest intensity within region
                    int p = from;
                    for (int k=from; k < to; ++k) {
                        if (intensities[k] < intensities[p]) p = k;
                    }
                    if (smalli == 0 || smalli == extrema.size()-1) {
                        // just remove it and we are done
                        extrema.removeAt(smalli);
                        indizes.removeAt(smalli);
                    } else {
                        extrema.removeAt(smalli-1);
                        extrema.removeAt(smalli-1);
                        indizes.removeAt(smalli-1);
                        indizes.removeAt(smalli-1);
                        indizes.set(smalli-1, p);
                        extrema.set(smalli-1, intensities[p]);
                        System.out.println("Removed an extremum");
                    }
                } else break;
            } else break;
        }
    }

    protected void removeMaximumAt(int k) {
        if (isMinimum(k)) throw new IllegalArgumentException("index " + k + " points to a minimum");
        //
    }

    // remove all maxima which are outside of the given indizes (inclusive)
    public void trimToRegion(int from, int to) {
        if (indizes.isEmpty()) return;
        while (!indizes.isEmpty()) {
            int j=indizes.size()-1;
            boolean isMaximum = !isMinimum(j);
            if (!isMaximum) --j;
            if (j<0) break;
            if (indizes.getQuick(j) > to) {
                indizes.remove(j, indizes.size()-j+1);
                extrema.remove(j, indizes.size()-j+1);
            } else break;
        }
        while (!indizes.isEmpty()) {
            int j=0;
            boolean isMaximum = !isMinimum(j);
            if (!isMaximum) ++j;
            if (j>= indizes.size()) break;
            if (indizes.getQuick(j) < from) {
                indizes.remove(0, j+1);
                extrema.remove(0, j+1);
            } else break;
        }
    }

    public void addExtremum(int index, double intensity) {
        this.extrema.add(intensity);
        this.indizes.add(index);
    }

    void replaceLastExtremum(int index, double intensity) {
        extrema.set(extrema.size()-1, intensity);
        indizes.set(indizes.size()-1, index);
    }

    public double lastExtremumIntensity() {
        if (extrema.isEmpty()) return 0d;
        return extrema.getQuick(extrema.size()-1);
    }
    public int lastExtremum() {
        return indizes.getQuick(extrema.size()-1);
    }


    public int numberOfExtrema() {
        return extrema.size();
    }

    public boolean isMinimum(int k) {
        double v = extrema.getQuick(k);
        double w = extrema.getQuick(k > 0 ? k-1 : k+1);
        return v < w;
    }

    // search for the second biggest extremum next to the given one
    // returns {apex, bestMinimum, secondApex}
    public int[] findSecondApex(int apex) {
        final double apexIntensity = extrema.get(apex);
        int bestIndex = -1, bestMinimum=-1;
        double bestScore = 0d;
        // search left
        for (int j=apex-2; j >= 0; j -= 2) {
            double intens = extrema.getQuick(j);
            // find lowest minimum
            for (int k=j+1; k < apex; k += 2) {
                double minInt = extrema.getQuick(k);
                double score = intens-minInt;
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = j;
                    bestMinimum=k;
                }
            }
        }
        // search right
        for (int j=apex+2; j < extrema.size(); j += 2) {
            double intens = extrema.getQuick(j);
            // find lowest minimum
            for (int k=j-1; k > apex; k -= 2) {
                double minInt = extrema.getQuick(k);
                double score = intens-minInt;
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = j;
                    bestMinimum=k;
                }
            }
        }
        if (bestIndex<0 || bestMinimum<0) return new int[0];
        // return best one
        return new int[]{
                indizes.getQuick(apex),
                indizes.getQuick(bestMinimum),
                indizes.getQuick(bestIndex)
        };
    }

    private double geom(double a, double b) {
        return Math.sqrt(a*b);
    }

    public int globalMaximum() {
        double max = 0d;
        int bestIndex = -1;
        for (int j=0; j < extrema.size(); ++j) {
            if (extrema.getQuick(j)>max) {
                max=extrema.getQuick(j);
                bestIndex = j;
            }
        }
        return bestIndex;
    }

    public boolean isValid(double[] intensities) {
        if (indizes.size()<=1) return false;
        boolean lastExtremumIsMinimum;
        int j;
        if (indizes.get(0)==0) {
            lastExtremumIsMinimum=isMinimum(0);
            j=1;
        } else {
            lastExtremumIsMinimum = !isMinimum(0);
            j=0;
        }
        double p = lastExtremumIsMinimum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        double lastExtremumIntensity = p;
        for (int i=0; i < intensities.length; ++i) {
            if (lastExtremumIsMinimum) {
                p = Math.max(p, intensities[i]);
                if (intensities[i] < lastExtremumIntensity) {
                    LoggerFactory.getLogger(Extrema.class).error("position " + i + " has smaller intensity (" +
                            intensities[i] + ") than last extremum with intensity " + lastExtremumIntensity);
                    return false;
                }
                if (j < indizes.size() && indizes.get(j) == i) {
                    lastExtremumIntensity = intensities[i];
                    if ((intensities[i] - p + 1e-12 < 0) ) {
                        LoggerFactory.getLogger(Extrema.class).error("previous position with intensity  " + p +
                                " has larger intensity than next extremum at "+ i + " with intensity " +
                                intensities[i]);
                        return false;
                    }
                    lastExtremumIsMinimum = false;
                    ++j;
                }
            } else {
                p = Math.min(p, intensities[i]);
                if (intensities[i] >lastExtremumIntensity) {
                    LoggerFactory.getLogger(Extrema.class).error("position " + i + " has larger intensity (" +
                            intensities[i] + ") than last extremum with intensity " + lastExtremumIntensity);
                    return false;
                }
                if (j < indizes.size() &&  indizes.get(j) == i) {
                    lastExtremumIntensity = intensities[i];
                    if ((p - intensities[i] + 1e-12 < 0) ) {
                        LoggerFactory.getLogger(Extrema.class).error("previous position with intensity  " + p +
                                " has smaller intensity than next extremum at "+ i + " with intensity " +
                                intensities[i]);
                        return false;
                    }
                    lastExtremumIsMinimum = true;
                    ++j;
                }

            }
        }
        return true;
    }

    public static interface TIntDoubleFunction {
        public double call(int i);
    }

    public static Extrema detectExtrema(double[] rawIntensities, double intensityThreshold) {
        SavitzkyGolayFilter filter = getProposedFilter3(rawIntensities);
        double[] smoothedSignal = filter==null ? rawIntensities.clone() : filter.applyExtended(rawIntensities);
        normalizeAndShiftSignal(smoothedSignal);
        final double medianSlopes = getMedianSlopes(smoothedSignal);
        Extrema e = detectExtrema(smoothedSignal, (i)->Math.max(intensityThreshold, 3*medianSlopes),(i)->1.05);
        if (!e.isValid(smoothedSignal))
            throw new RuntimeException();
        Extrema f = e.clone();
        e.fixSmoothed(rawIntensities);
        if (!e.isValid(rawIntensities)) {// disable filter
            LoggerFactory.getLogger(Extrema.class).warn("SavitzkyGolayFilter leads to invalid extrema detection.");
            e = detectExtrema(rawIntensities, (i) -> Math.max(intensityThreshold, 3 * medianSlopes), (i) -> 1.05);
        }
        if (e.indizes.size() > 3) e.smoothoutExtrema(rawIntensities);
        if (!e.isValid(rawIntensities))
            throw new RuntimeException();
        return e;
    }

    public Extrema clone() {
        Extrema e = new Extrema();
        e.extrema.addAll(extrema);
        e.indizes.addAll(indizes);
        return e;
    }

    public static Extrema detectExtremaOld(double[] intensityValues, TIntDoubleFunction intensityThreshold, TIntDoubleFunction slopeThreshold) {
        final Extrema extrema = new Extrema();
        extrema.addExtremum(0, intensityValues[0]);
        boolean minimum = intensityValues[0] < intensityValues[1];
        for (int k=1; k < intensityValues.length-1; ++k) {
            final double a = intensityValues[k-1];
            final double b = intensityValues[k];
            final double c = intensityValues[k+1];
            if ((b - a) < 0 && (b - c) < 0) {
                // minimum
                if (minimum) {
                    if (extrema.lastExtremumIntensity() > b)
                        extrema.replaceLastExtremum(k, b);
                } else if (extrema.lastExtremumIntensity() - b >= intensityThreshold.call(extrema.lastExtremum()) && extrema.lastExtremumIntensity()/b >= slopeThreshold.call(extrema.lastExtremum())) {
                    extrema.addExtremum(k, b);
                    minimum = true;
                }
            } else if ((b - a) > 0 && (b - c) > 0) {
                // maximum
                if (minimum) {
                    if (b - extrema.lastExtremumIntensity() >= intensityThreshold.call(k) && b/extrema.lastExtremumIntensity() >= slopeThreshold.call(k)) {
                        extrema.addExtremum(k, b);
                        minimum = false;
                    }
                } else {
                    if (extrema.lastExtremumIntensity() < b) {
                        extrema.replaceLastExtremum(k, b);
                    }
                }
            }
        }
        // check last index
        if (minimum) {
            if (intensityValues[intensityValues.length-1] < extrema.lastExtremumIntensity()) {
                extrema.replaceLastExtremum(intensityValues.length-1, intensityValues[intensityValues.length-1]);
            }
        } else {
            if (intensityValues[intensityValues.length-1] > extrema.lastExtremumIntensity()) {
                extrema.replaceLastExtremum(intensityValues.length-1, intensityValues[intensityValues.length-1]);
            }
        }


        if (extrema.indizes.size()<=1) {
            int prevdex = extrema.indizes.get(0);
            extrema.indizes.clear();extrema.extrema.clear();
            // there should be always at least one minimum and maximum
            if (minimum) {
                // set maximum peak to maximum
                int mindex=0;
                for (int i=0; i < intensityValues.length; ++i) {
                    if (intensityValues[mindex] < intensityValues[i]) mindex=i;
                }
                if (mindex<prevdex) {
                    extrema.addExtremum(mindex, intensityValues[mindex]);
                    extrema.addExtremum(prevdex, intensityValues[prevdex]);
                } else {
                    extrema.addExtremum(prevdex, intensityValues[prevdex]);
                    extrema.addExtremum(mindex, intensityValues[mindex]);
                }

            } else {
                // set minimum peak to minimum
                int mindex=0;
                for (int i=0; i < intensityValues.length; ++i) {
                    if (intensityValues[mindex] > intensityValues[i]) mindex=i;
                }
                if (mindex<prevdex) {
                    extrema.addExtremum(mindex, intensityValues[mindex]);
                    extrema.addExtremum(prevdex, intensityValues[prevdex]);
                } else {
                    extrema.addExtremum(prevdex, intensityValues[prevdex]);
                    extrema.addExtremum(mindex, intensityValues[mindex]);
                }
            }
        }
        return extrema;
    }

    public static Extrema detectExtrema(double[] intensityValues, TIntDoubleFunction intensityThreshold, TIntDoubleFunction slopeThreshold) {
        final Extrema extrema = new Extrema();

        // start with maximum intensity
        int maxi=0;
        for (int k=0; k < intensityValues.length; ++k) {
            if (intensityValues[k]>intensityValues[maxi]) maxi=k;
        }
        extrema.addExtremum(0, maxi);


        boolean minimum = intensityValues[0] < intensityValues[1];
        for (int k=1; k < intensityValues.length-1; ++k) {
            final double a = intensityValues[k-1];
            final double b = intensityValues[k];
            final double c = intensityValues[k+1];
            if ((b - a) < 0 && (b - c) < 0) {
                // minimum
                if (minimum) {
                    if (extrema.lastExtremumIntensity() > b)
                        extrema.replaceLastExtremum(k, b);
                } else if (extrema.lastExtremumIntensity() - b >= intensityThreshold.call(extrema.lastExtremum()) && extrema.lastExtremumIntensity()/b >= slopeThreshold.call(extrema.lastExtremum())) {
                    extrema.addExtremum(k, b);
                    minimum = true;
                }
            } else if ((b - a) > 0 && (b - c) > 0) {
                // maximum
                if (minimum) {
                    if (b - extrema.lastExtremumIntensity() >= intensityThreshold.call(k) && b/extrema.lastExtremumIntensity() >= slopeThreshold.call(k)) {
                        extrema.addExtremum(k, b);
                        minimum = false;
                    }
                } else {
                    if (extrema.lastExtremumIntensity() < b) {
                        extrema.replaceLastExtremum(k, b);
                    }
                }
            }
        }
        // check last index
        if (minimum) {
            if (intensityValues[intensityValues.length-1] < extrema.lastExtremumIntensity()) {
                extrema.replaceLastExtremum(intensityValues.length-1, intensityValues[intensityValues.length-1]);
            }
        } else {
            if (intensityValues[intensityValues.length-1] > extrema.lastExtremumIntensity()) {
                extrema.replaceLastExtremum(intensityValues.length-1, intensityValues[intensityValues.length-1]);
            }
        }

        return extrema;
    }


    private static void normalizeAndShiftSignal(double[] signal) {
        double maximum=0d;
        // shift such that all peaks are strictly positive
        double min=0;
        for (int k=0; k< signal.length; ++k) {
            min = Math.min(signal[k], min);
        }
        for (int k=0; k< signal.length; ++k) {
            signal[k] -= min;
        }
    }

    public static double getMedianSlopes(double[] xs) {
        double[] ys = new double[xs.length-1];
        for (int k=1; k < xs.length; ++k) {
            ys[k-1] = Math.abs(xs[k]-xs[k-1]);
        }
        Arrays.sort(ys);
        return ys[Math.max(0,(ys.length-10)/2)];
    }


    public static SavitzkyGolayFilter getProposedFilter3(double[] peak) {
        if (peak.length<=10) return null;
        SavitzkyGolayFilter[] filters = new SavitzkyGolayFilter[]{
                SavitzkyGolayFilter.Window1Polynomial1,
                SavitzkyGolayFilter.Window2Polynomial2,
                SavitzkyGolayFilter.Window3Polynomial2,
                SavitzkyGolayFilter.Window4Polynomial2,
                SavitzkyGolayFilter.Window8Polynomial2,
                SavitzkyGolayFilter.Window16Polynomial2,
                SavitzkyGolayFilter.Window32Polynomial3
        };
        int p = estimatedScanPointsPerPeak(peak);
        SavitzkyGolayFilter largestFilter = null;
        for (SavitzkyGolayFilter filter : filters) {
            if (3*filter.windowSize >= p) break;
            largestFilter = filter;
        }
        return largestFilter;
    }


    private static int estimatedScanPointsPerPeak(double[] peak) {
        // find maximum and minimum intensity
        int mxi = 0, mni = 0;
        for (int k=0; k < peak.length; ++k) {
            double f = peak[k];
            if (f > peak[mxi]) mxi=k;
            else if (f < peak[mni]) mni=k;
        }
        final double threshold = Math.max( peak[mni]*5, peak[mxi]/2);
        boolean brk=true;
        int leftj,rightj;
        for (rightj=1; rightj+mxi < peak.length; ++rightj) {
            double f = peak[mxi + rightj];
            if (f < threshold) {
                break;
            }
        }
        // extend
        for (; rightj+mxi < peak.length; ++rightj) {
            if (peak[mxi+rightj] > peak[mxi+rightj-1]) break;
        }
        for (leftj=1; mxi-leftj >=0; ++leftj) {
            double f = peak[mxi - leftj];
            if (f < threshold) {
                break;
            }
        }
        for (; mxi-leftj >=0; ++leftj) {
            if (peak[mxi-leftj] > peak[mxi-leftj+1]) break;
        }
        return Math.min(leftj,rightj)*2;
    }

}
