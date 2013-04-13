package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternScorer;

import static java.lang.Math.abs;

public class IntensityDiffScorer<P extends Peak, T extends Spectrum<P>> implements IsotopePatternScorer<P, T> {

    private final IsotopePatternScorer<Peak, Spectrum<Peak>> usedScoring;

    public IntensityDiffScorer(IsotopePatternScorer<Peak, Spectrum<Peak>> usedScoring) {
        this.usedScoring = usedScoring;
    }

    @Override
    public double score(T measuredSpectrum, T theoreticalSpectrum, Normalization mode) {
        if (measuredSpectrum.size() <= 1 || theoreticalSpectrum.size() <= 1) return 0;
        final SimpleMutableSpectrum s = new SimpleMutableSpectrum();
        final SimpleMutableSpectrum t = new SimpleMutableSpectrum();
        for (int i=1; i < measuredSpectrum.size(); ++i) {
            s.addPeak(new Peak(0, abs(measuredSpectrum.getIntensityAt(i-1)-measuredSpectrum.getIntensityAt(i))));
            t.addPeak(new Peak(0, abs(theoreticalSpectrum.getIntensityAt(i-1)-theoreticalSpectrum.getIntensityAt(i))));
        }
        final Normalization sum1 = Normalization.Sum(1);
        Spectrums.normalize(s, sum1);
        Spectrums.normalize(t, sum1);
        return usedScoring.score(s, t, sum1);
    }
}
