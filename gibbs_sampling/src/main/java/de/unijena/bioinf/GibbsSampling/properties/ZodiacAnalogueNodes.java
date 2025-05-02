package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import lombok.AllArgsConstructor;

/**
 * Contains parameters about how to use spectral library analog search as additional node in
 * the zodiac network.
 */
@AllArgsConstructor
public class ZodiacAnalogueNodes implements Ms2ExperimentAnnotation {

    /**
     * Specifies whether adding analog library search based nodes is used or not.
     */
    @DefaultProperty
    public final boolean enabled;

    /**
     * Minimal modified cosine needed to add the analog hit separate node to the network.
     */
    @DefaultProperty
    public final double minSimilarity;

    /**
     * Minimal number of share peaks needed to dd the analog hit separate node to the network.
     */
    @DefaultProperty
    public final int minSharedPeaks;

    protected ZodiacAnalogueNodes() {
        this.enabled = false;
        this.minSimilarity = 0;
        this.minSharedPeaks = 0;
    }
}
