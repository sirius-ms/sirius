package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

/**
 * Flags that indicate good or bad quality of the input data
 */
public interface QualityAnnotation extends DataAnnotation  {

    public Quality getQuality();

}
