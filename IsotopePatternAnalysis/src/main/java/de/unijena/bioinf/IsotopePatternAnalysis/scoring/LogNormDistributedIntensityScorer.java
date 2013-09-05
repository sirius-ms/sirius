package de.unijena.bioinf.IsotopePatternAnalysis.scoring;


import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

import static java.lang.Math.*;

public class LogNormDistributedIntensityScorer implements IsotopePatternScorer {

    private final static double root2 = sqrt(2d);

    private IntensityDependency intensityDependency;


    public LogNormDistributedIntensityScorer(IntensityDependency intensityDependency) {
        this.intensityDependency = intensityDependency;
    }

    public LogNormDistributedIntensityScorer(double fullIntensityPrecision, double minIntensityPrecision) {
        this(new LinearIntensityDependency(fullIntensityPrecision, minIntensityPrecision));
    }

    public LogNormDistributedIntensityScorer() {
        this(0.1, 0.9);
    }


    @Override
    public double score(Spectrum<Peak> measuredSpectrum, Spectrum<Peak> theoreticalPattern, Normalization norm, MsExperiment experiment) {
        // remove peaks from theoretical pattern until the length of both spectra is equal
        final MutableSpectrum<Peak> theoreticalSpectrum = new SimpleMutableSpectrum(theoreticalPattern);
        while (measuredSpectrum.size() < theoreticalSpectrum.size()) {
            theoreticalSpectrum.removePeakAt(theoreticalSpectrum.size()-1);
        }
        // re-normalize
        Spectrums.normalize(theoreticalSpectrum, norm);
        // score
        double score = 0d;
        final double intensityDeviation = experiment.getMeasurementProfile().getIntensityDeviation();
        for (int i=0; i < measuredSpectrum.size(); ++i) {
            final double intensity = measuredSpectrum.getIntensityAt(i);
            final double thIntensity = theoreticalSpectrum.getIntensityAt(i);
            // TODO: TEST!!!!!!!!!!!
            final double sd = log(intensity)-log(intensity+intensityDeviation*intensityDependency.getValueAt(intensity));//log(1 + intensityDeviation*intensityDependency.getValueAt(intensity));
            final double newScore = log(Erf.erfc(abs(log(thIntensity / intensity)/(root2*sd))));
            if (Double.isInfinite(newScore)) return newScore;
            score += newScore;
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.intensityDependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "intensityDependency", helper.wrap(document,intensityDependency));
    }
}
