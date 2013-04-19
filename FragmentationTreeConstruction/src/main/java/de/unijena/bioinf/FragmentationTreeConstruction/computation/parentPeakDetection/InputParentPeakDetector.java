package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

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

    @Override
    public Detection detectParentPeak(Ms2Experiment experiment) {
        final double parentMass = detectParentMass(experiment);
        return detectParentPeakByParentMass(experiment, parentMass);
    }

    protected Detection detectParentPeakByParentMass(Ms2Experiment experiment, double parentMass) {

        for (Ms2Spectrum spectrum : experiment.getMS2Spectra()) {

        }
    }

    protected double detectParentMass(Ms2Experiment experiment) {
        // use ion mass from input
        final double m = experiment.getIonMass();
        if (m != 0 && !Double.isNaN(m)) {
            return m;
        }
        // if no ion mass is given, use mass from sum formula
        final MolecularFormula f = experiment.getMolecularFormula();
        if (f != null) {
            final Ionization z = experiment.getIonization();
            return f.getMass();
        }

    }
}
