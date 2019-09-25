package de.unijena.bioinf.lcms.peakshape;

import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;

public interface PeakShapeFitting<T extends PeakShape> {

    public T fit(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment);

}
