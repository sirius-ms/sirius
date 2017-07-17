package de.unijena.bioinf.ChemistryBase.ms;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ge28quv on 07/07/17.
 */
public class SimpleIsolationWindow extends IsolationWindow {

    private double[] relMz;
    private double[] filterRatio;



    public SimpleIsolationWindow(double maxWindowSize, double massShift) {
        super(maxWindowSize,massShift);
    }

    public SimpleIsolationWindow(double maxWindowSize) {
        super(maxWindowSize);
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
        relMz = isotopeRatioInformation.getPosToMedian().keys();
        Arrays.sort(relMz);
        filterRatio = new double[relMz.length];
        for (int i = 0; i < relMz.length; i++) {
            filterRatio[i] = isotopeRatioInformation.getPosToMedian().get(relMz[i]);
        }
    }

    @Override
    public double getEstimatedWindowSize() {
        return relMz[relMz.length]-relMz[0]+0.2;//or bigger?
    }
}
