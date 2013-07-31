package de.unijena.bioinf.IsotopePatternAnalysis.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.LinearIntensityDependency;
import org.apache.commons.math3.special.Erf;

public class MassDeviationScorer implements IsotopePatternScorer {

    private final static double root2 = Math.sqrt(2d);

    private IntensityDependency intensityDependency;

    public MassDeviationScorer() {
        this(1.5d);
    }

    public MassDeviationScorer(double lowestIntensityAccuracy) {
        this(new LinearIntensityDependency(1d, lowestIntensityAccuracy));
    }

    public MassDeviationScorer(IntensityDependency intensityDependency) {
        this.intensityDependency  = intensityDependency;
    }

    @Override
    public double score(Spectrum<Peak> measured, Spectrum<Peak> theoretical, Normalization norm, MsExperiment experiment) {
        if (measured.size() > theoretical.size()) throw new IllegalArgumentException("Theoretical spectrum is smaller than measured spectrum");
        // remove peaks from theoretical pattern until the length of both spectra is equal
        final MutableSpectrum<Peak> theoreticalSpectrum = new SimpleMutableSpectrum(theoretical);
        while (measured.size() < theoreticalSpectrum.size()) {
            theoreticalSpectrum.removePeakAt(theoreticalSpectrum.size()-1);
        }
        // re-normalize
        Spectrums.normalize(theoreticalSpectrum, Normalization.Sum(1));
        final double mz0 = measured.getMzAt(0);
        final double thMz0 = theoreticalSpectrum.getMzAt(0);
        final double int0 = norm.rescale(measured.getIntensityAt(0));
        double score = Math.log(Erf.erfc(Math.abs(thMz0 - mz0)/
                (root2*(experiment.getMeasurementProfile().getStandardMs1MassDeviation().absoluteFor(mz0) *  intensityDependency.getValueAt(int0)))));
        for (int i=1; i < measured.size(); ++i) {
            final double mz = measured.getMzAt(i) - mz0;
            final double thMz = theoreticalSpectrum.getMzAt(i) - thMz0;
            final double thIntensity = norm.rescale(measured.getIntensityAt(i));
            // TODO: thMz hier richtig?
            final double sd = experiment.getMeasurementProfile().getStandardMassDifferenceDeviation().absoluteFor(measured.getMzAt(i)) * intensityDependency.getValueAt(thIntensity);
            score += Math.log(Erf.erfc(Math.abs(thMz - mz)/(root2*sd)));
        }
        return score;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.intensityDependency = (IntensityDependency)helper.unwrap(document, document.getFromDictionary(dictionary, "intensityDependency"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "intensityDependency", helper.wrap(document, intensityDependency));
    }
}
