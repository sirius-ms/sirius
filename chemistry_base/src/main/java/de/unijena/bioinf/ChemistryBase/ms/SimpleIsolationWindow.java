package de.unijena.bioinf.ChemistryBase.ms;

import java.util.Arrays;

/**
 * Created by ge28quv on 07/07/17.
 */
public class SimpleIsolationWindow extends IsolationWindow {

    protected double[] relMz;
    protected double[] filterRatio;
    protected double outerRimSize = 0.5;
    protected double minWindowSize = 1.0;

    public SimpleIsolationWindow(double maxWindowSize) {
        super(maxWindowSize);
    }

    /**
     * minimum estimated size is 1Da; if intensity outside filter window shall be estimated the edge is interpolated with 0 intensity in distance 0.5
     * @param maxWindowSize
     * @param massShift
     * @param estimateSize
     * @param findMs1PeakDeviation
     */
    public SimpleIsolationWindow(double maxWindowSize, double massShift, boolean estimateSize, Deviation findMs1PeakDeviation) {
        super(maxWindowSize, massShift, estimateSize, findMs1PeakDeviation);
    }

    @Override
    public double getLeftBorder() {
        return getMassShift()-getEstimatedWindowSize()/2-minWindowSize;
    }

    @Override
    public double getRightBorder() {
        return getMassShift()+getEstimatedWindowSize()/2+minWindowSize;
    }


    @Override
    public double getIntensityRatio(double precursorMz, double targetMz) {
        final double diff = targetMz-precursorMz;
        int idx = Arrays.binarySearch(relMz, diff);
        if (idx<0) idx = -(idx+1);

        final double leftMz, rightMz, leftIntensity, rightIntensity;

        if (diff>relMz[0] && diff<=relMz[relMz.length-1]){
            leftMz = relMz[idx-1];
            rightMz = relMz[idx];
            leftIntensity = filterRatio[idx-1];
            rightIntensity = filterRatio[idx];
        } else if (diff<=relMz[0]) {
            leftMz = relMz[0]-outerRimSize;
            rightMz = relMz[0];
            leftIntensity = 0d;
            rightIntensity = filterRatio[0];
        } else {
            leftMz = relMz[relMz.length-1];
            leftIntensity = filterRatio[filterRatio.length-1];
            rightMz = leftMz+outerRimSize; //todo how to end filter?
            rightIntensity = 0d;

        }

        if (diff<leftMz || diff>rightMz){
            return 0d;
        }

        final double diff2 = rightMz-leftMz;
        return leftIntensity*(rightMz-diff)/diff2+rightIntensity*(diff-leftMz)/diff2;
    }

    @Override
    public double getIntensity(double targetIntensity, double precursorMz, double targetMz) {
        return targetIntensity*getIntensityRatio(precursorMz, targetMz);
    }

    @Override
    protected void estimateDistribution(IsotopeRatioInformation isotopeRatioInformation) {
        double[] positions = isotopeRatioInformation.getPositionsWithMedianIntensity();
        Arrays.sort(positions);
        relMz = new double[positions.length];
        filterRatio = new double[positions.length];
        for (int i = 0; i < positions.length; i++) {
            filterRatio[i] = isotopeRatioInformation.getMedianIntensityRatio(positions[i]);
            relMz[i] = isotopeRatioInformation.getMedianRelMz(positions[i]);
        }
    }

    @Override
    public double getEstimatedWindowSize() {
        return Math.max(minWindowSize, relMz[relMz.length-1]-relMz[0]);//+0.2);//todo or bigger?
    }

    @Override
    public double getEstimatedMassShift() {
        return (relMz[relMz.length-1]-relMz[0])/2+relMz[0];
    }

    public double[] getFilterMassValues(){
        return Arrays.copyOf(relMz, relMz.length);
    }

    public double[] getFilterIntensityRatios(){
        return Arrays.copyOf(filterRatio, filterRatio.length);
    }
}
