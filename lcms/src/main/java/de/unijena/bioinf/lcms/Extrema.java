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

import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.BitSet;
import java.util.function.Function;

public class Extrema {

    protected final TDoubleArrayList extrema;
    protected final TIntArrayList indizes;

    public Extrema() {
        this.extrema = new TDoubleArrayList();
        this.indizes = new TIntArrayList();
    }

    public int getIndexAt(int k)  {
        return indizes.getQuick(k);
    }

    public boolean valid() {
        boolean minimum = false;
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
    public double lastExtremum() {
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

    /**
     * removes extrema which differences are below noise level. However, if after removal less than
     * minimumNumberExtrema is kept, the removal is canceled.
     *
     * There are two requirements for smoothing:
     * 1.) as higher the surrounding noise, as more is smoothed
     * 2.) as higher the intensity, as less is smoothed
     *
     * @return true, if there are more than minimumNumberExtrema after smoothing
     */
    boolean smooth(Function<Integer,Float> noiseLevelPerScan, ChromatographicPeak cpeak, double relativeSlopeThreshold, double noiseWeight) {
        if (extrema.size()<=1) return false;

        final BitSet delete = new BitSet(indizes.size());
        for (int k=1; k < indizes.size()-1; ++k) {
            double intensityLeft = extrema.getQuick(k-1);
            double intensityRight = extrema.getQuick(k+1);
            double peak = extrema.getQuick(k);
            int length = indizes.getQuick(k+1)-indizes.getQuick(k-1);
            boolean isMaximum = intensityLeft<peak || intensityRight<peak;
            double minSlope = geom(peak-intensityLeft,peak-intensityRight);//Math.min(Math.abs(peak-intensityLeft),Math.abs(peak-intensityRight));
            double relativeSlope = minSlope/Math.max(peak, Math.max(intensityLeft,intensityRight));
            if (relativeSlope < relativeSlopeThreshold && minSlope <= noiseWeight*noiseLevelPerScan.apply(indizes.getQuick(k)) && length <= 4){
                delete.set(k);
            }
        }
        int cardinality = delete.cardinality();
        if (indizes.size()-cardinality >= 1) {
            final double[] newExtrema = new double[indizes.size()-cardinality];
            final int[] newIndizes = new int[indizes.size()-cardinality];
            boolean minimum = true;
            int k=-1;
            for (int j=0; j < indizes.size(); ++j) {
                if (!delete.get(j)) {
                    double newval = extrema.getQuick(j);
                    double prevVal = (k<0 ? 0 : newExtrema[k]);
                    if (minimum) {
                        if (newval <= prevVal) {
                            newExtrema[k] = newval;
                            newIndizes[k] = indizes.getQuick(j);
                        } else {
                            ++k;
                            newExtrema[k] = newval;
                            newIndizes[k] = indizes.getQuick(j);
                            minimum = false;
                        }
                    } else {
                        if (newval >= prevVal) {
                            newExtrema[k] = newval;
                            newIndizes[k] = indizes.getQuick(j);
                        } else {
                            ++k;
                            newExtrema[k] = newval;
                            newIndizes[k] = indizes.getQuick(j);
                            minimum = true;
                        }
                    }
                }
            }
            extrema.clearQuick();
            extrema.add(newExtrema, 0, k);
            indizes.clearQuick();
            indizes.add(newIndizes, 0, k);
            return true;
        } else return cardinality==0;
    }

    private double geom(double a, double b) {
        return Math.sqrt(a*b);
    }

    private void calcWidthAt33Maximum(ChromatographicPeak cpeak, long[] widthAt33Maximum, int k) {
        double intensity = extrema.getQuick(k);
        final boolean minimum = (k == 0 && extrema.getQuick(k+1)>intensity)
                || (k>0 && extrema.getQuick(k-1)>intensity);
        if (minimum) {
            int i,j;
            // go left
            if (k>0) {
                i = indizes.getQuick(k);
                double threshold = cpeak.getIntensityAt(indizes.get(k-1))*0.33;
                while (i >= 0 && cpeak.getIntensityAt(i)< threshold) --i;
                i = Math.min(++i,indizes.getQuick(k));
            } else i = indizes.getQuick(k);
            // go right
            if (k+1<indizes.size()) {
                j = indizes.getQuick(k);
                double threshold = cpeak.getIntensityAt(indizes.get(k+1))*0.33;
                while (j < cpeak.numberOfScans() && cpeak.getIntensityAt(j) < threshold) ++j;
                j = Math.max(--j,indizes.getQuick(k));
            } else j = indizes.getQuick(k);
            widthAt33Maximum[k] = cpeak.getRetentionTimeAt(j)-cpeak.getRetentionTimeAt(i);

        } else {
            int i,j;
            // go left
            if (k>0) {
                i = indizes.getQuick(k);
                double threshold = cpeak.getIntensityAt(indizes.get(k))*0.33;
                while (i >= 0 && cpeak.getIntensityAt(i)> threshold) --i;
                i = Math.min(++i,indizes.getQuick(k));
            } else i = indizes.getQuick(k);
            // go right
            if (k+1<indizes.size()) {
                j = indizes.getQuick(k);
                double threshold = cpeak.getIntensityAt(indizes.get(k))*0.33;
                while (j < cpeak.numberOfScans() && cpeak.getIntensityAt(j) > threshold) ++j;
                j = Math.max(--j,indizes.getQuick(k));
            } else j = indizes.getQuick(k);
            widthAt33Maximum[k] = cpeak.getRetentionTimeAt(j)-cpeak.getRetentionTimeAt(i);

        }
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

    public void deleteExtremaOfSinglePeaks(double slopeThreshold) {
        while (true) {
            for (int k = 1; k < extrema.size() - 1; ++k) {
                final double m = extrema.getQuick(k);
                final double l = extrema.getQuick(k - 1), r = extrema.getQuick(k + 1);
                if (m > l && m > r && ((indizes.getQuick(k + 1) - indizes.getQuick(k) <= 1 && k +2 < extrema.size() && extrema.getQuick(k+2) / m > slopeThreshold ) || (indizes.getQuick(k) - indizes.getQuick(k - 1) <= 1 && k > 1 && extrema.getQuick(k-2) / m > slopeThreshold))) {
                    if (extrema.getQuick(k-1) < extrema.getQuick(k+1)) {
                        extrema.removeAt(k);
                        extrema.removeAt(k);
                        indizes.removeAt(k);
                        indizes.removeAt(k);
                        k=Math.max(k-2,0);
                    } else {
                        extrema.removeAt(k-1);
                        extrema.removeAt(k-1);
                        indizes.removeAt(k-1);
                        indizes.removeAt(k-1);
                        k=Math.max(k-2,0);
                    }
                }
            }
            break;
        }
    }
}
