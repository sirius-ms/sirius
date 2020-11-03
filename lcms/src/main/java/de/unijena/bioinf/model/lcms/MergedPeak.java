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

package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.Comparator;

public class MergedPeak implements Peak {

    private ScanPoint[] sourcePeaks;
    private float mass,intensity;

    public MergedPeak(ScanPoint single) {
        this.mass = (float)single.getMass();
        this.intensity = (float)single.getIntensity();
        this.sourcePeaks = new ScanPoint[]{single};
    }

    public MergedPeak(ScanPoint[] list) {
        if (list.length==0)
            throw new IllegalArgumentException("Empty merged peak.");
        ScanPoint best = (Arrays.stream(list).max(Comparator.comparingDouble(Peak::getIntensity)).get());
        this.mass = (float)best.getMass();
        this.intensity = (float)best.getIntensity();
        this.sourcePeaks = list;
        assert allFromDifferentScans(sourcePeaks);
    }

    public MergedPeak(MergedPeak left, MergedPeak right) {
        Peak h = getHightestPeak(left,right);
        this.sourcePeaks = Arrays.copyOf(left.sourcePeaks, left.sourcePeaks.length+right.sourcePeaks.length);
        System.arraycopy(right.sourcePeaks, 0, sourcePeaks, left.sourcePeaks.length, right.sourcePeaks.length);
        Arrays.sort(sourcePeaks, Comparator.comparingInt(ScanPoint::getScanNumber));
        this.mass = (float)h.getMass();
        this.intensity = (float)h.getIntensity();
        assert allFromDifferentScans(sourcePeaks);
    }

    private static boolean allFromDifferentScans(ScanPoint[] xs) {
        final TIntHashSet set = new TIntHashSet();
        for (ScanPoint p : xs) {
            if (!set.add(p.getScanNumber())) {
                return false;
            }
        }
        return true;

    }

    protected double correlation(TDoubleArrayList bufferLeft,TDoubleArrayList bufferRight, MergedPeak other) {
        int i=0,j=0;
        bufferLeft.clearQuick(); bufferRight.clearQuick();
        while (i < sourcePeaks.length && j < other.sourcePeaks.length) {
            if (sourcePeaks[i].getScanNumber()==other.sourcePeaks[j].getScanNumber()) {
                bufferLeft.add(sourcePeaks[i].getIntensity());
                bufferRight.add(other.sourcePeaks[j].getIntensity());
                ++i;++j;
            } else if (sourcePeaks[i].getScanNumber()<other.sourcePeaks[j].getScanNumber()) {
                ++i;
            } else {
                ++j;
            }
        }
        if (bufferLeft.size()>=3 && bufferRight.size()>=3) {
            return Statistics.pearson(bufferLeft.toArray(), bufferRight.toArray());
        } else return 0d;
    }

    private static Peak getHightestPeak(MergedPeak left, MergedPeak right) {
        Peak highest = left.sourcePeaks[0];
        for (Peak l : left.sourcePeaks) {
            if (l.getIntensity() > highest.getIntensity())
                highest = l;
        }
        for (Peak l : right.sourcePeaks) {
            if (l.getIntensity() > highest.getIntensity())
                highest = l;
        }
        return highest;
    }

    public ScanPoint[] getSourcePeaks() {
        return sourcePeaks;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

    public double getHighestIntensity() {
        return intensity;
    }

    public double getRobustAverageMass(double noiseLevel) {
        if (sourcePeaks.length==1) return sourcePeaks[0].getMass();
        ScanPoint[] copy = sourcePeaks.clone();
        Arrays.sort(copy,Comparator.comparingDouble(ScanPoint::getIntensity).reversed());
        double threshold = Math.min(4*noiseLevel, copy[0].getIntensity()*0.5d);
        int i=0;
        for (; i < copy.length; ++i)
            if (copy[i].getIntensity()<threshold) {
                break;
            }
        if (i<=3) return weightedAverage(copy,i);
        Arrays.sort(copy,0,i, Comparator.comparingDouble(ScanPoint::getMass));
        int perc = (int)Math.floor(i*0.25);
        double avg = 0d, ints=0d;
        for (int k=perc; k < i-perc; ++k) {
            avg += copy[k].getMass()*copy[k].getIntensity();
            ints += copy[k].getIntensity();
        }
        assert ints>0;
        return avg/(ints);

    }

    public double weightedAverage() {
        return weightedAverage(sourcePeaks,sourcePeaks.length);
    }

    private double weightedAverage(ScanPoint[] xs, int n) {
        double m=0d, i=0d;
        for (int k=0; k < n; ++k) {
            m += xs[k].getIntensity()*xs[k].getMass();
            i+=xs[k].getIntensity();
        }
        return m/i;
    }

    public double sumIntensity() {
        double i = 0d;
        for (Peak p : sourcePeaks)
            i += p.getIntensity();
        return i;
    }

    @Override
    public String toString(){
        return this.getMass() + " m/z\t" + this.getIntensity() + "  (" + this.sourcePeaks.length + " peaks)";
    }
}
