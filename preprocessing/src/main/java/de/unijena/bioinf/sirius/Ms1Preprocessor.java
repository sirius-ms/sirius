package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.FormulaFilter;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.*;
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

import java.util.ArrayList;
import java.util.List;
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
        adjustValenceFilter(pinput);

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
            pinput.setAnnotation(FormulaConstraints.class, settings.getEnforcedAlphabet().getExtendedConstraints(settings.getFallbackAlphabet()));
        } else {
            pinput.setAnnotation(FormulaConstraints.class, fc);
        }
    }

    /**
     * Detect ion mode based on MS spectrum
     * @param pinput
     */


    @Provides(PossibleAdducts.class)
    public void adductDetection(ProcessedInput pinput) {
        //todo we need to write to the original data here. to keep the predicted adducts. maybe this should be part of the IDResult instead????
        final Ms2Experiment exp = pinput.getOriginalInput();

        // if input file contains an adduct annotation, disable adduct detection
        if (!exp.getPrecursorIonType().isIonizationUnknown()) {
            pinput.setAnnotation(PossibleAdducts.class, new PossibleAdducts(exp.getPrecursorIonType()));
            return;
        }

        if (pinput.hasAnnotation(PossibleAdducts.class)) return;

        final DetectedAdducts detAdds = exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new);
        if (!detAdds.containsKey(DetectedAdducts.Keys.MS1_PREPROCESSOR)) {
            final int charge = exp.getPrecursorIonType().getCharge();

            final AdductSettings settings = pinput.getAnnotationOrDefault(AdductSettings.class);
            final PossibleAdducts ionModes = ionModeDetection.detect(pinput, settings.getDetectable(charge));

            if (ionModes != null)
                detAdds.put(DetectedAdducts.Keys.MS1_PREPROCESSOR, new PossibleAdducts(ionModes.getAdducts()));
        }

        pinput.setAnnotation(PossibleAdducts.class, exp.getPossibleAdductsOrFallback());
    }

    @Requires(FormulaConstraints.class)
    @Requires(PossibleAdducts.class)
    @Requires(AdductSettings.class)
    public void adjustValenceFilter(ProcessedInput pinput) {
        ;
        final PossibleAdducts possibleAdducts = pinput.getAnnotationOrThrow(PossibleAdducts.class);

        Set<PrecursorIonType> usedIonTypes;
        final AdductSettings adductSettings = pinput.getAnnotationOrNull(AdductSettings.class);
        if (adductSettings != null && possibleAdducts.hasOnlyPlainIonizationsWithoutModifications()) {
            //todo check if it makes sense to use the detectables
            usedIonTypes = adductSettings.getDetectable(possibleAdducts.getIonModes());
        } else {
            //there seem to be some information from the preprocessing
            usedIonTypes = possibleAdducts.getAdducts();
        }

        List<FormulaFilter> newFilters = new ArrayList<>();
        final FormulaConstraints fc = pinput.getAnnotationOrThrow(FormulaConstraints.class);
        for (FormulaFilter filter : fc.getFilters()) {
            if (filter instanceof ValenceFilter) {
                newFilters.add(new ValenceFilter(((ValenceFilter) filter).getMinValence(), usedIonTypes));
            } else {
                newFilters.add(filter);
            }
        }
        pinput.setAnnotation(FormulaConstraints.class, fc.withNewFilters(newFilters));
    }

    public Set<Element> getSetOfPredictableElements() {
        return elementDetection.getPredictableElements();
    }

}
