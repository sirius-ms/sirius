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

    public double getAverageMass() {
        final double[] xs = new double[sourcePeaks.length];
        for (int k=0; k < sourcePeaks.length; ++k) xs[k] = sourcePeaks[k].getMass();
        return Statistics.robustAverage(xs);
    }
}
