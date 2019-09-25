package de.unijena.bioinf.model.lcms;

public interface CorrelatedChromatographicPeak extends ChromatographicPeak {

    public ChromatographicPeak getCorrelatedPeak();
    public double getCorrelation();
    public int getCorrelationStartPoint();
    public int getCorrelationEndPoint();

}
