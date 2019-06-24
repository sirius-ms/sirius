package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.apache.commons.math3.analysis.UnivariateFunction;

public class Feature implements Annotated<DataAnnotation> {

    protected final LCMSRun origin;
    protected final double mz, intensity;
    protected final ScanPoint[] trace;
    protected final SimpleSpectrum[] correlatedFeatures;
    protected final SimpleSpectrum[] ms2Spectra;
    protected final PrecursorIonType ionType;
    protected final UnivariateFunction rtRecalibration;
    protected Annotated.Annotations<DataAnnotation> annotations = new Annotations<>();

    // quality terms
    protected final Quality peakShapeQuality, ms1Quality, ms2Quality;

    // debug
    public ScanPoint[] completeTraceDebug;

    public Feature(LCMSRun origin, double mz, double intensity, ScanPoint[] trace, SimpleSpectrum[] correlatedFeatures, SimpleSpectrum[] ms2Spectra, PrecursorIonType ionType, UnivariateFunction rtRecalibration,Quality peakShapeQuality, Quality ms1Quality, Quality ms2Quality) {
        this.origin = origin;
        this.mz = mz;
        this.intensity = intensity;
        this.trace = trace;
        this.correlatedFeatures = correlatedFeatures;
        this.ms2Spectra = ms2Spectra;
        this.ionType = ionType;
        this.rtRecalibration = rtRecalibration;
        this.peakShapeQuality = peakShapeQuality;
        this.ms1Quality = ms1Quality;
        this.ms2Quality = ms2Quality;
    }

    public Quality getPeakShapeQuality() {
        return peakShapeQuality;
    }

    public Quality getMs1Quality() {
        return ms1Quality;
    }

    public Quality getMs2Quality() {
        return ms2Quality;
    }

    public UnivariateFunction getRtRecalibration() {
        return rtRecalibration;
    }

    public LCMSRun getOrigin() {
        return origin;
    }

    public double getMz() {
        return mz;
    }

    public double getIntensity() {
        return intensity;
    }

    public ScanPoint[] getTrace() {
        return trace;
    }

    public SimpleSpectrum[] getCorrelatedFeatures() {
        return correlatedFeatures;
    }

    public SimpleSpectrum[] getMs2Spectra() {
        return ms2Spectra;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }
}
