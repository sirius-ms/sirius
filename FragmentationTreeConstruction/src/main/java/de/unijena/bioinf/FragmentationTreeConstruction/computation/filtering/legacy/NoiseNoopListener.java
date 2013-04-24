package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.legacy;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;

/**
 * @author Kai Dührkop
 */
public class NoiseNoopListener implements NoisePeakCallback {
    @Override
    public void reportNoise(MS2Peak peak, String reason) {
    }
}
