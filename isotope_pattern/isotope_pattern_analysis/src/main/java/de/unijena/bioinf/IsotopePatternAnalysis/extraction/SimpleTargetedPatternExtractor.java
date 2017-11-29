package de.unijena.bioinf.IsotopePatternAnalysis.extraction;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.ArrayList;

public class SimpleTargetedPatternExtractor implements TargetedPatternExtraction {

    @Override
    public SimpleSpectrum extractSpectrum(Ms2Experiment experiment, MeasurementProfile profile) {

        if (experiment.getIonMass()<=0 || Double.isNaN(experiment.getIonMass()) )
            throw new IllegalArgumentException("ion mass is not set.");

        final SimpleSpectrum ms1;
        if (experiment.getMergedMs1Spectrum()==null || experiment.getMergedMs1Spectrum().size()>0) {
            ms1 = merge(experiment,profile);
        } else ms1 = experiment.getMergedMs1Spectrum();

        if (ms1==null) return null;


        final ChemicalAlphabet stdalphabet = profile.getFormulaConstraints().getExtendedConstraints(new FormulaConstraints(ChemicalAlphabet.getExtendedAlphabet())).getChemicalAlphabet();
        final Spectrum<Peak> massOrderedSpectrum = Spectrums.getMassOrderedSpectrum(ms1);
        final ArrayList<SimpleSpectrum> patterns = new ArrayList<SimpleSpectrum>();
        final int index = Spectrums.mostIntensivePeakWithin(massOrderedSpectrum, experiment.getIonMass(), profile.getAllowedMassDeviation());
        if (index < 0) return null;
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        spec.addPeak(massOrderedSpectrum.getPeakAt(index));
        // add additional peaks
        final PeriodicTable T = PeriodicTable.getInstance();
        for (int k=1; k <= 5; ++k) {
            final Range<Double> nextMz = T.getIsotopicMassWindow(stdalphabet, profile.getAllowedMassDeviation(), spec.getMzAt(0), k);
            final double a = nextMz.lowerEndpoint();
            final double b = nextMz.upperEndpoint();
            final double startPoint = a - profile.getStandardMassDifferenceDeviation().absoluteFor(a);
            final double endPoint = b + profile.getStandardMassDifferenceDeviation().absoluteFor(b);
            final int nextIndex = Spectrums.indexOfFirstPeakWithin(massOrderedSpectrum, startPoint, endPoint);
            if (nextIndex < 0) break;
            double mzBuffer = 0d;
            double intensityBuffer = 0d;
            for (int i=nextIndex; i < massOrderedSpectrum.size(); ++i) {
                final double mz = massOrderedSpectrum.getMzAt(i);
                if (mz > endPoint) break;
                final double intensity = massOrderedSpectrum.getIntensityAt(i);
                mzBuffer += mz*intensity;
                intensityBuffer += intensity;
            }
            mzBuffer /= intensityBuffer;
            spec.addPeak(mzBuffer, intensityBuffer);
        }
        return new SimpleSpectrum(spec);
    }

    private SimpleSpectrum merge(Ms2Experiment experiment, MeasurementProfile profile) {
        if (experiment.getMs1Spectra().size()>0) {
            return Spectrums.mergeSpectra(experiment.<Spectrum<Peak>>getMs1Spectra());
        } else return null;
    }
}
