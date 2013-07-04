package de.unijena.bioinf.FTAnalysis;

public class XYZ implements Comparable<XYZ> {
    final double x;
    final double y;
    final double z;
    XYZ(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XYZ xyz = (XYZ) o;

        if (Double.compare(xyz.x, x) != 0) return false;
        if (Double.compare(xyz.y, y) != 0) return false;
        if (Double.compare(xyz.z, z) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = x != +0.0d ? Double.doubleToLongBits(x) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = y != +0.0d ? Double.doubleToLongBits(y) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = z != +0.0d ? Double.doubleToLongBits(z) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public int compareTo(XYZ o) {
        return Double.compare(x, o.x);
    }
}
