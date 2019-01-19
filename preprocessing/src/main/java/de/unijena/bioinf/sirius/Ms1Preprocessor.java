package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MergedMs1Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Ms2ExperimentValidator;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.deisotope.IsotopePatternDetection;
import de.unijena.bioinf.sirius.deisotope.TargetedIsotopePatternDetection;
import de.unijena.bioinf.sirius.elementdetection.DeepNeuralNetworkElementDetector;
import de.unijena.bioinf.sirius.elementdetection.ElementDetection;
import de.unijena.bioinf.sirius.iondetection.AdductDetection;
import de.unijena.bioinf.sirius.iondetection.DetectIonsFromMs1;
import de.unijena.bioinf.sirius.merging.Ms1Merging;
import de.unijena.bioinf.sirius.validation.Ms1Validator;

import java.util.HashSet;
import java.util.Set;

/**
 * Performs element detection, adduct detection, isotope pattern merging. But NOT MS/MS spectrum merging
 */
public class Ms1Preprocessor implements SiriusPreprocessor {

    protected Ms2ExperimentValidator validator = new Ms1Validator();
    protected ElementDetection elementDetection = new DeepNeuralNetworkElementDetector();
    protected AdductDetection ionModeDetection = new DetectIonsFromMs1();
    protected Ms1Merging ms1Merging = new Ms1Merging();
    protected IsotopePatternDetection deisotoper = new TargetedIsotopePatternDetection();


    @Override
    public ProcessedInput preprocess(Ms2Experiment experiment) {
        final MutableMs2Experiment validated = new MutableMs2Experiment(experiment);
        validateInput(validated);
        final ProcessedInput pinput = new ProcessedInput(validated, experiment);
        ms1Merging(pinput);
        isotopePatternDetection(pinput);
        elementDetection(pinput);
        adductDetection(pinput);
        return pinput;
    }

    private boolean validateInput(MutableMs2Experiment experiment) {
        return validator.validate(experiment, Warning.Logger, true);
    }

    /**
     * if no merged MS is given, merge the MS1 spectra into one merged MS
     */
    @Provides(MergedMs1Spectrum.class)
    public void ms1Merging(ProcessedInput pinput) {
        ms1Merging.merge(pinput);
    }

    /**
     * Search in the MS for isotope pattern
     */
    @Requires(MergedMs1Spectrum.class)
    @Provides(Ms1IsotopePattern.class)
    public void isotopePatternDetection(ProcessedInput pinput) {
        deisotoper.detectIsotopePattern(pinput);
    }

    /**
     * Detect elements based on MS spectrum
     * @param pinput
     */
    @Requires(Ms1IsotopePattern.class)
    @Provides(FormulaConstraints.class)
    public void elementDetection(ProcessedInput pinput) {
        final FormulaSettings settings = pinput.getAnnotationOrDefault(FormulaSettings.class);
        final FormulaConstraints fc = elementDetection.detect(pinput);
        if (fc==null) {
            pinput.setAnnotation(FormulaConstraints.class, settings.getFallbackAlphabet());
        } else {
            pinput.setAnnotation(FormulaConstraints.class, settings.getEnforcedAlphabet().getExtendedConstraints(fc));
        }
    }

    /**
     * Detect ion mode based on MS spectrum
     * @param pinput
     */
    public void adductDetection(ProcessedInput pinput) {
        final int charge = pinput.getExperimentInformation().getPrecursorIonType().getCharge();
        final AdductSettings settings = pinput.getAnnotation(AdductSettings.class);
        final PossibleAdducts ionModes = ionModeDetection.detect(pinput, settings.getDetectable(charge));

        final HashSet<PrecursorIonType> set = new HashSet<>(settings.getEnforced(charge));

        if (ionModes==null) set.addAll(settings.getFallback(charge));
        else set.addAll(ionModes.getAdducts());
        pinput.setAnnotation(PossibleAdducts.class, new PossibleAdducts(set));
    }


    public Set<Element> getSetOfPredictableElements() {
        return elementDetection.getPredictableElements();
    }
}
