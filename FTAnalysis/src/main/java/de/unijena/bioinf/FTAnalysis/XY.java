package de.unijena.bioinf.FTAnalysis;

final class XY implements Comparable<XY> {
    double x;
    double y;
    XY(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XY xy = (XY) o;

        if (Double.compare(xy.x, x) != 0) return false;
        if (Double.compare(xy.y, y) != 0) return false;

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
        return result;
    }

    @Override
    public int compareTo(XY o) {
        return Double.compare(x, o.x);
    }
}
