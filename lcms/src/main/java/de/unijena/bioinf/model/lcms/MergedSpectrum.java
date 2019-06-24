package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.*;
import de.unijena.bioinf.lcms.quality.Quality;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MergedSpectrum extends PeaklistSpectrum<MergedPeak> implements OrderedSpectrum<MergedPeak> {

    protected Precursor precursor;
    protected List<Scan> scans;
    protected double noiseLevel;

    public MergedSpectrum(Scan scan, Spectrum<? extends Peak> spectrum, Precursor precursor) {
        super(new ArrayList<>());
        for (int k=0; k < spectrum.size(); ++k) {
            peaks.add(new MergedPeak(new ScanPoint(scan, spectrum.getMzAt(k), spectrum.getIntensityAt(k))));
        }
        peaks.sort(Comparator.comparingDouble(Peak::getMass));
        this.precursor= precursor;
        scans = new ArrayList<>();
        scans.add(scan);
    }

    public MergedSpectrum(Precursor precursor, List<MergedPeak> peaks, List<Scan> scans) {
        super(peaks);
        this.peaks.sort(Comparator.comparingDouble(Peak::getMass));
        this.scans = scans;
        this.precursor=precursor;
    }

    // we have to do this. Otherwise, memory consumption is just too high
    public void applyNoiseFiltering() {
        int min = (int)Math.floor(scans.size()*0.2);
        this.peaks.removeIf(x->x.getIntensity()<noiseLevel || x.getSourcePeaks().length < min);
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(double noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public List<Scan> getScans() {
        return scans;
    }

    public double totalTic() {
        return Spectrums.calculateTIC(this);
    }

    public Precursor getPrecursor() {
        return precursor;
    }

    public SimpleSpectrum finishMerging() {
        final int n = scans.size();
        if (n >= 5) {
            int min = (int)Math.ceil(n*0.2);
            final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
            for (MergedPeak p : peaks) {
                if (p.getSourcePeaks().length >= min) {
                    buf.addPeak(p);
                }
            }
            Spectrums.applyBaseline(buf, noiseLevel);
            return new SimpleSpectrum(buf);
        } else {
            final SimpleMutableSpectrum buf = new SimpleMutableSpectrum(this);
            Spectrums.applyBaseline(buf, noiseLevel);
            return new SimpleSpectrum(buf);
        }
    }

    public Quality getQuality() {
        int peaksAboveNoise = 0;
        for (int k=0; k < peaks.size(); ++k) {
            if (peaks.get(k).getMass() < (getPrecursor().getMass()-20) && peaks.get(k).getIntensity() >= noiseLevel*5)
                ++peaksAboveNoise;
        }
        if (peaksAboveNoise >= 5) return Quality.GOOD;
        if (peaksAboveNoise >= 3) return Quality.DECENT;
        return Quality.BAD;
    }
}
