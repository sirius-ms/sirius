package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import lombok.Getter;
import org.slf4j.LoggerFactory;

@Getter
public class MsMsQuerySpectrum {

    SimpleSpectrum originalSpectrum;
    SimpleSpectrum chimericSpectrum;
    double chimericPollution;
    double ms1Intensity;
    Ms2SpectrumHeader header;
    double exactParentMass;
    CosineQuerySpectrum filteredSpectrum;

    int sampleId;

    double score;

    public MsMsQuerySpectrum(Ms2SpectrumHeader header, int sampleId, SimpleSpectrum originalSpectrum, SimpleSpectrum ms1Spectrum) {
        this.header = header;
        this.sampleId = sampleId;
        this.score = 1d;
        this.originalSpectrum = originalSpectrum;
        this.filteredSpectrum = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(10))).createQuery(filtered(originalSpectrum), header.getPrecursorMz(), true, false);
        IsolationWindow isolationWindow = header.getIsolationWindow().orElse(new IsolationWindow(0, 1d));
        final double L = header.getPrecursorMz() + isolationWindow.getLeftOffset();
        final double R = header.getPrecursorMz() + isolationWindow.getRightOffset();
        int target = Spectrums.mostIntensivePeakWithin(ms1Spectrum, header.getPrecursorMz(), new Deviation(10));
        if (target<0) {
            LoggerFactory.getLogger(MsMsQuerySpectrum.class).warn("Do not find any precursor peak for MsMs spectrum at the expected position.");
            target = Spectrums.mostIntensivePeakWithin(ms1Spectrum, L, R);
        }
        exactParentMass = ms1Spectrum.getMzAt(target);
        int left=target-1, right=target+1;
        while (left >= 0 && ms1Spectrum.getMzAt(left) >= L)
            --left;
        ++left;
        while (right < ms1Spectrum.size() && ms1Spectrum.getMzAt(right) <= R)
            ++right;
        --right;
        this.chimericSpectrum = Spectrums.subspectrum(ms1Spectrum, left, right-left+1);
        double chi = 0d;
        for (int k=0; k < chimericSpectrum.size(); ++k) {
            if (k!=(target-left)) chi += chimericSpectrum.getIntensityAt(k) * Math.exp(-Math.pow(chimericSpectrum.getMzAt(k)-(header.getTargetedMz()+isolationWindow.getWindowOffset()), 2)/(isolationWindow.getWindowWidth()*isolationWindow.getWindowWidth()));
        }
        this.chimericPollution = chi;
        this.ms1Intensity = ms1Spectrum.getIntensityAt(target);
    }

    private SimpleSpectrum filtered(SimpleSpectrum originalSpectrum) {
        // remove everything below 0.005% intensity and keep top 60 peaks
        Spectrum<Peak> intensityOrderedSpectrum = Spectrums.getIntensityOrderedSpectrum(originalSpectrum);
        final double threshold = intensityOrderedSpectrum.getIntensityAt(0)*0.005;
        int cutoff = Math.min(intensityOrderedSpectrum.size(),60);
        while (cutoff > 0 && intensityOrderedSpectrum.getIntensityAt(cutoff-1)<threshold) {
            --cutoff;
        }
        return new SimpleSpectrum(Spectrums.subspectrum(intensityOrderedSpectrum, 0, cutoff));

    }
}
