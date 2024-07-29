package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.align.RecalibrationFunction;
import org.dizitart.no2.mvstore.MVSpatialKey;
import org.h2.mvstore.rtree.Spatial;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class Rect implements Comparable<Rect>, Serializable {
    public float minMz, maxMz, minRt, maxRt;
    public double avgMz;
    public int id;

    public final static float FLOATING_POINT_TOLERANCE = 5e-5f;

    public Rect(Rect o) {
        this(o.minMz,o.maxMz,o.minRt,o.maxRt,o.avgMz,-1);
    }

    public Rect(double minMz, double maxMz, double minRt, double maxRt, double avgMz) {
        this((float)minMz,(float)maxMz,(float)minRt,(float)maxRt,avgMz, -1);
    }

    public Rect(float minMz, float maxMz, float minRt, float maxRt, double avgMz) {
        this(minMz,maxMz,minRt,maxRt,avgMz, -1);
    }

    public float[] toArray() {
        return new float[]{minMz,maxMz,minRt,maxRt};
    }

    public Rect(float minMz, float maxMz, float minRt, float maxRt, double avgMz, int id) {
        this.minMz = minMz;
        this.maxMz = maxMz;
        this.minRt = minRt;
        this.maxRt = maxRt;
        this.avgMz = avgMz;
        this.id = id;
    }

    public void upgrade(Rect other) {
        minMz = Math.min(minMz, other.minMz);
        maxMz = Math.max(maxMz, other.maxMz);
        minRt = Math.min(minRt, other.minRt);
        maxRt = Math.max(maxRt, other.maxRt);
        avgMz = (avgMz+other.avgMz)/2;
    }

    public boolean contains(Rect other) {
        return minMz <= (other.minMz+FLOATING_POINT_TOLERANCE) && (maxMz+FLOATING_POINT_TOLERANCE) >= other.maxMz && minRt <= (other.minRt+FLOATING_POINT_TOLERANCE) && (maxRt+FLOATING_POINT_TOLERANCE) >= other.maxRt;
    }
    public boolean contains(double mz, double rt) {
        return (minMz <= mz+FLOATING_POINT_TOLERANCE) && (maxMz+FLOATING_POINT_TOLERANCE) >= mz && minRt <= (rt+FLOATING_POINT_TOLERANCE) && (maxRt+FLOATING_POINT_TOLERANCE) >= rt;
    }

    @Override
    public int compareTo(@NotNull Rect o) {
        int c = Float.compare(minMz, o.minMz);
        if (c!=0) return c;
        c = Float.compare(maxMz, o.maxMz);
        if (c!=0) return c;
        c = Float.compare(minRt, o.minRt);
        if (c!=0) return c;
        c = Float.compare(maxRt, o.maxRt);
        if (c!=0) return c;
        else return Integer.compare(id, o.id);
    }

    @Override
    public String toString() {
        return id + ":(m/z = " + minMz + ".." + maxMz + ", rt = " + minRt + ".." + maxRt + ", avgMz = " + avgMz + ")" ;
    }

    public Spatial toKey() {
        return new MVSpatialKey(id, minMz-Rect.FLOATING_POINT_TOLERANCE, maxMz+Rect.FLOATING_POINT_TOLERANCE, minRt, maxRt);
    }

    public Rect recalibrateRt(RecalibrationFunction rtRecalibration) {
        return new Rect(minMz, maxMz, rtRecalibration.value(minRt), rtRecalibration.value(maxRt), avgMz);
    }

    public boolean containsRt(double rt) {
        return minRt<=(rt+FLOATING_POINT_TOLERANCE) && (maxRt+FLOATING_POINT_TOLERANCE)>=rt;
    }
}
