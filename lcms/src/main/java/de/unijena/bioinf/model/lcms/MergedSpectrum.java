package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeaklistSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MergedSpectrum extends PeaklistSpectrum<MergedPeak> implements OrderedSpectrum<MergedPeak> {

    protected Precursor precursor;
    protected List<Scan> scans;

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

    public List<Scan> getScans() {
        return scans;
    }

    public double totalTic() {
        return Spectrums.calculateTIC(this);
    }

    public Precursor getPrecursor() {
        return precursor;
    }
}
