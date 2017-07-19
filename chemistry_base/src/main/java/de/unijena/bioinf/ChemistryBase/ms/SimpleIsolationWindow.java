package de.unijena.bioinf.ChemistryBase.ms;

import java.util.Arrays;

/**
 * Created by ge28quv on 07/07/17.
 */
public class SimpleIsolationWindow extends IsolationWindow {

    protected double[] relMz;
    protected double[] filterRatio;

    public SimpleIsolationWindow(double maxWindowSize) {
        super(maxWindowSize);
    }

    public SimpleIsolationWindow(double maxWindowSize, double massShift, boolean estimateSize) {
        super(maxWindowSize, massShift, estimateSize);
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
            leftMz = relMz[0]-0.5;
            rightMz = relMz[0];
            leftIntensity = 0d;
            rightIntensity = filterRatio[0];
        } else {
            leftMz = relMz[relMz.length-1];
            leftIntensity = filterRatio[filterRatio.length-1];
            rightMz = leftMz+0.5; //todo how to end filter?
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
        double[] positions = isotopeRatioInformation.getPosToMedianIntensity().keys();
        Arrays.sort(positions);
        relMz = new double[positions.length];
        filterRatio = new double[positions.length];
        for (int i = 0; i < positions.length; i++) {
            filterRatio[i] = isotopeRatioInformation.getPosToMedianIntensity().get(positions[i]);
            relMz[i] = isotopeRatioInformation.getPosToMedianMz().get(positions[i]);
        }
    }

    @Override
    public double getEstimatedWindowSize() {
        return relMz[relMz.length-1]-relMz[0]+0.2;//or bigger?
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
