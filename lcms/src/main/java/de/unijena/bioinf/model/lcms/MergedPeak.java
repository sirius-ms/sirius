package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.Arrays;
import java.util.Comparator;

public class MergedPeak extends Peak {

    private ScanPoint[] sourcePeaks;

    public MergedPeak(ScanPoint single) {
        super(single.getMass(), single.getIntensity());
        this.sourcePeaks = new ScanPoint[]{single};
    }

    public MergedPeak(ScanPoint[] list) {
        super(Arrays.stream(list).max(Comparator.comparingDouble(Peak::getIntensity)).get());
        this.sourcePeaks = list;
        if (sourcePeaks.length==0)
            throw new IllegalArgumentException("Empty merged peak.");
    }

    public MergedPeak(MergedPeak left, MergedPeak right) {
        super(getHightestPeak(left,right));
        this.sourcePeaks = Arrays.copyOf(left.sourcePeaks, left.sourcePeaks.length+right.sourcePeaks.length);
        System.arraycopy(right.sourcePeaks, 0, sourcePeaks, left.sourcePeaks.length, right.sourcePeaks.length);
        Arrays.sort(sourcePeaks, Comparator.comparingInt(ScanPoint::getScanNumber));
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
}
