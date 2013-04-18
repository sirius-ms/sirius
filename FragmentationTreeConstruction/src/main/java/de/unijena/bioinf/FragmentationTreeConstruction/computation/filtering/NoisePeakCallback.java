package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;

public interface NoisePeakCallback {

    /**
     * Callback which is called for noise peaks
     * @param peak
     * @param reason
     */
    public void reportNoise(MS2Peak peak, String reason);

}
