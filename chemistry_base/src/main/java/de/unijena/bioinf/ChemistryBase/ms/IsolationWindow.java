package de.unijena.bioinf.ChemistryBase.ms;

import com.google.common.collect.Range;

import java.util.Locale;

public class IsolationWindow {

    protected final double windowOffset;
    protected final double windowWidth;


    public IsolationWindow(double windowOffset, double windowWidth) {
        this.windowOffset = windowOffset;
        this.windowWidth = windowWidth;
    }

    public double getWindowOffset() {
        return windowOffset;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public Range<Double> getWindowFor(double peak) {
        return Range.closed(peak+windowOffset-windowWidth/2d, peak+windowOffset+windowWidth/2d);
    }

    public static IsolationWindow fromOffsets(double lowerOffset, double higherOffset) {
        double width = lowerOffset + higherOffset;
        double offset = higherOffset - lowerOffset;
        return new IsolationWindow(offset, width);
    }

    public boolean isUndefined() {
        return windowWidth<=0 || !Double.isFinite(windowWidth);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "IsolationWindow: offset = %f, width=%f.",windowOffset,windowWidth);
    }
}
