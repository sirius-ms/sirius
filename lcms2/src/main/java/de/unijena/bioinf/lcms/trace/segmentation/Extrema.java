package de.unijena.bioinf.lcms.trace.segmentation;

public class Extrema {

    protected final int[] extrema;
    protected final boolean[] maximum;

    public Extrema(int[] extrema, boolean[] maximum) {
        this.extrema = extrema;
        this.maximum = maximum;
    }

    public int getExtremumAt(int index) {
        return extrema[index];
    }

    public boolean isMaximum(int index) {
        return maximum[index];
    }

    public int size() {
        return extrema.length;
    }
}
