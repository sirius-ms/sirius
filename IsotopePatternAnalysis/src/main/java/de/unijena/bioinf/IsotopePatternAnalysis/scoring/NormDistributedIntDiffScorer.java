package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.util.FixedIntensity;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import org.apache.commons.math3.special.Erf;

public class NormDistributedIntDiffScorer implements IsotopePatternScorer {

    private IntensityDependency intensityDependency;
    private double intensityPenalty;
    private final static double root2div2 = Math.sqrt(2);

    public NormDistributedIntDiffScorer() {
        this(new FixedIntensity(0.09));
    }

    public NormDistributedIntDiffScorer(IntensityDependency intensityDependency) {
        this(3, intensityDependency);
    }

    public NormDistributedIntDiffScorer(double penalty, IntensityDependency intensityDependency) {
        this.intensityPenalty = penalty;
        this.intensityDependency = intensityDependency;
    }

    public double getIntensityPenalty() {
        return intensityPenalty;
    }

    public IntensityDependency getIntensityDependency() {
        return intensityDependency;
    }

    @Override
    public double score(Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoretical, Normalization usedNormalization, MsExperiment experiment) {
        if (measuredSpectrum.size() > theoretical.size())
            throw new IllegalArgumentException("Theoretical spectrum is smaller than measured spectrum");
        // remove peaks from theoretical pattern until the length of both spectra is equal
        final MutableSpectrum<Peak> theoreticalSpectrum = new SimpleMutableSpectrum(theoretical);
        while (measuredSpectrum.size() < theoreticalSpectrum.size()) {
            theoreticalSpectrum.removePeakAt(theoreticalSpectrum.size()-1);
        }
        Spectrums.normalize(theoreticalSpectrum, Normalization.Sum(1));
        final double maxIntensity = Spectrums.getMaximalIntensity(measuredSpectrum);
        double score = 0d;
        for (int i=0; i < theoreticalSpectrum.size(); ++i) {
            final double measuredIntensity = measuredSpectrum.getIntensityAt(i);
            final double theoreticalIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double sd = 1d/intensityPenalty * intensityDependency.getValueAt(measuredIntensity);
            score += Math.log(Erf.erfc(Math.abs(measuredIntensity - theoreticalIntensity)/(root2div2*sd)));
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.intensityDependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));
        this.intensityPenalty = document.getDoubleFromDictionary(dictionary, "intensityPenalty");

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "intensityDependency", helper.wrap(document,intensityDependency));
        document.addToDictionary(dictionary, "intensityPenalty", intensityPenalty);
    }
}
