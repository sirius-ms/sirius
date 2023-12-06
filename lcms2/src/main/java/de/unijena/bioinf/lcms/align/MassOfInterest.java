package de.unijena.bioinf.lcms.align;

import org.apache.commons.math3.analysis.UnivariateFunction;

import java.io.Serializable;
import java.util.Locale;

public class MassOfInterest implements Serializable {

    protected double mz, rt, rtorig;

    /**
     * width of the mass trace left and right
     */
    protected double widthLeft, widthRight;
    protected float mzDevLeft, mzDevRight;
    protected float relativeIntensity;
    private final int traceId, scanId, sampleId;



    protected byte qualityLevel;

    public MassOfInterest(double mz, double rt, double widthLeft, double widthRight, double minMz, double maxMz, float relativeIntensity, int traceId, int scanId, int sampleId, int qualityLevel) {
        this.mz = mz;
        this.rt = rt;
        this.rtorig = rt;
        this.widthLeft =widthLeft;
        this.widthRight =widthRight;
        this.mzDevLeft = (float)(mz-minMz);
        this.mzDevRight = (float)(maxMz-mz);
        this.relativeIntensity = relativeIntensity;
        this.traceId = traceId;
        this.scanId = scanId;
        this.sampleId = sampleId;
        this.qualityLevel = (byte)qualityLevel;
    }

    public float getMzDevLeft() {
        return mzDevLeft;
    }

    public float getMzDevRight() {
        return mzDevRight;
    }

    public double getMinMz() {
        return mz - mzDevLeft;
    }

    public double getMaxMz() {
        return mz + mzDevRight;
    }

    public double getWidthLeft() {
        return widthLeft;
    }

    public double getWidthRight() {
        return widthRight;
    }

    protected void normalize(IntensityNormalization.Normalizer normalizer) {
        this.relativeIntensity = normalizer.normalize(this.relativeIntensity);
    }

    protected void recalibrate(UnivariateFunction f) {
        this.rt = f.value(rtorig);
    }

    public double getUncalibratedRetentionTime() {
        return rtorig;
    }

    public byte getQualityLevel() {
        return qualityLevel;
    }

    public void setQualityLevel(int qualityLevel) {
        this.qualityLevel = (byte)qualityLevel;
    }

    public double getMz() {
        return mz;
    }

    public double getRt() {
        return rt;
    }

    public float getRelativeIntensity() {
        return relativeIntensity;
    }

    public int getTraceId() {
        return traceId;
    }

    public int getScanId() {
        return scanId;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "<moi: mz=%.4f, rt = %.1f, intensity = %.3f>", mz, rt, relativeIntensity);
    }
}
