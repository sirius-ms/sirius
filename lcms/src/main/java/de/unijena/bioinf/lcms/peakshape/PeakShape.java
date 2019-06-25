package de.unijena.bioinf.lcms.peakshape;

import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

public interface PeakShape extends DataAnnotation {

    public double getScore();

    public double expectedIntensityAt(long rt);

    public double getLocation();

    public Quality getPeakShapeQuality();

}
