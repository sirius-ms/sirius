package de.unijena.bioinf.model.lcms;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * Sample datapoints from scans.
 * Use a shifting window
 */
public class QuantileShiftingWindowNoiseModel {

    float[][] noiseMatrix;

    public QuantileShiftingWindowNoiseModel(LCMSRun lcmsRun) {
        final TDoubleArrayList intensities = new TDoubleArrayList();
        // we divide the run in 20 parts, and the spectrum in 20 parts of m/z 100 windows



    }

}
