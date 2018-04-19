package de.unijena.bioinf.IsotopePatternAnalysis.scoring;


import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

import static java.lang.Math.*;

@Deprecated
public class LogNormDistributedIntensityScorer implements IsotopePatternScorer {

    private final static double root2 = sqrt(2d);

    private  double intensityDeviationPenalty;
    private  IntensityDependency intensityDependency;

    public LogNormDistributedIntensityScorer(double intensityDeviationPenalty, IntensityDependency intensityDependency) {
        this.intensityDeviationPenalty = intensityDeviationPenalty;
        this.intensityDependency = intensityDependency;
    }

    public LogNormDistributedIntensityScorer(double intensityDeviationPenalty, double fullIntensityPrecision, double minIntensityPrecision) {
        this(intensityDeviationPenalty, new LinearIntensityDependency(fullIntensityPrecision, minIntensityPrecision));
    }

    public LogNormDistributedIntensityScorer() {
        this(3, 0.1, 0.9);
    }

    public LogNormDistributedIntensityScorer(double fullIntensityPrecision, double minIntensityPrecision) {
        this(3, fullIntensityPrecision, minIntensityPrecision);
    }


    @Override
    public void score(double[] scoreUptoKPeaks, Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalSpectrum, Normalization usedNormalization, Ms2Experiment experiment, MeasurementProfile profile) {
        // score
        double score = 0d;
        for (int i=0; i < measuredSpectrum.size(); ++i) {
            final double intensity = measuredSpectrum.getIntensityAt(i);
            final double thIntensity = theoreticalSpectrum.getIntensityAt(i);
            final double sd = 1d/intensityDeviationPenalty * log(1 + intensityDependency.getValueAt(intensity));
            score += log(Erf.erfc(abs(log(thIntensity / intensity)/(root2*sd))));
            scoreUptoKPeaks[i] += score;
        }
    }


    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.intensityDependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
