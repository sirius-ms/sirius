package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.jjobs.BasicJJob;
import lombok.AllArgsConstructor;

/**
 * JJob class for executing one spectral alignment match between two spectra
 */
@AllArgsConstructor
public class SpectralMatchJJob extends BasicJJob<SpectralSimilarity> {

    private CosineQueryUtils queryUtils;
    private CosineQuerySpectrum left;
    private CosineQuerySpectrum right;

    @Override
    protected SpectralSimilarity compute() throws Exception {
        return queryUtils.cosineProduct(left, right);
    }

    @Override
    protected void cleanup() {
        queryUtils = null;
        left = null;
        right = null;
        super.cleanup();
    }
}
