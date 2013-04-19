package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.NormalizationType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.Normalizer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.Detection;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.ParentPeakDetector;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2InputImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.ArrayList;
import java.util.List;

public class FragmentationPatternAnalysis {

    private List<InputValidator> inputValidators;
    private ParentPeakDetector parentPeakDetector;
    private Warning validatorWarning;
    private boolean repairInput;
    private NormalizationType normalizationType;
    private NormalizationMode normalizationMode;

    public FragmentationPatternAnalysis() {
        this.inputValidators = new ArrayList<InputValidator>();
        this.validatorWarning = new Warning.Noop();
        this.repairInput = true;
    }

    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        // use a mutable experiment, such that we can easily modify it
        Ms2InputImpl input = wrapInput(validate(input));
        final Detection detectedParentPeak = detectParentPeak(experiment);
        input = normalize(input);
        merge(experiment);

    }

    public ProcessedInput normalize(Ms2Experiment experiment) {
        final Ms2InputImpl input = wrapInput(experiment);
        final List<Spectrum<Peak>> normalizedSpectra = new ArrayList<Spectrum<Peak>>();
        // local normalization
        double maxIntensity = 0d;
        for (Ms2Spectrum spectrum : experiment.getMs2Spectra()) {
            maxIntensity= Math.max(maxIntensity, Spectrums.getMaximalIntensity(spectrum));
            normalizedSpectra.add(Spectrums.getNormalizedSpectrum(spectrum, Normalization.))
        }

    }

    public Ms2Experiment validate(Ms2Experiment experiment) {
        for (InputValidator validator : inputValidators) {
            experiment = validator.validate(experiment, validatorWarning, repairInput);
        }
        return experiment;
    }

    public Detection detectParentPeak(Ms2Experiment experiment) {
        return parentPeakDetector.detectParentPeak(experiment);
    }

    private Ms2InputImpl wrapInput(Ms2Experiment exp) {
        if (exp instanceof Ms2InputImpl) return (Ms2InputImpl) exp;
        else return new Ms2InputImpl(exp);
    }


}
