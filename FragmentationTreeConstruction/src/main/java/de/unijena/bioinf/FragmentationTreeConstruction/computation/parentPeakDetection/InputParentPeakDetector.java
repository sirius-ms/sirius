package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * Parent Peak detection is done in two steps:
 * 1. guess the parent mass
 * 2. search for a peak with high intensity (>10%) which has minimal mass deviation to the parentmass
 */
public class InputParentPeakDetector implements ParentPeakDetector {

    private final double minimalIntensity;

    public InputParentPeakDetector() {
        this(0.1d);
    }

    public InputParentPeakDetector(double minimalIntensity) {
        this.minimalIntensity = minimalIntensity;
    }

    public double getMinimalIntensity() {
        return minimalIntensity;
    }

    @Override
    public Detection detectParentPeak(Ms2Experiment experiment) {
        final double parentMass = detectParentMass(experiment);
        return detectParentPeakByParentMass(experiment, parentMass);
    }

    protected Detection detectParentPeakByParentMass(final Ms2Experiment experiment, final double parentMass) {
        Peak bestChoice = null;
        final Deviation allowedDeviation = experiment.getMeasurementProfile().getExpectedIonMassDeviation();
        // search for peak with the correct mass (within the given mass deviation) and with at least <minimalIntensity>
        // intensity. If multiple peaks are found, take the one with the lowest intensity
        for (Ms2Spectrum spectrum : experiment.getMs2Spectra()) {
            final double minIntensity = Spectrums.getMaximalIntensity(spectrum)*minimalIntensity;
            for (int i=0; i < spectrum.size(); ++i) {
                if (allowedDeviation.inErrorWindow(parentMass, spectrum.getMzAt(i)) && spectrum.getIntensityAt(i) >= minIntensity) {
                    // take the peak with the higher intensity
                    if (bestChoice == null || spectrum.getIntensityAt(i) > bestChoice.getIntensity()) {
                        bestChoice = spectrum.getPeakAt(i);
                    }
                }
            }
        }
        if (bestChoice==null) {
            // ouch! There is no parent peak
            // Create a synthetic one
            return new Detection(new Peak(parentMass, 0d), true);
        } else {
            return new Detection(bestChoice, false);
        }
    }

    protected double detectParentMass(Ms2Experiment experiment) {
        // a valid input has an ion mass. Yeah!
        return experiment.getIonMass();
    }
}
