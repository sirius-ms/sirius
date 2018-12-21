package de.unijena.bioinf.sirius.preprocessing;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.ms.MergedMs1Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.PossibleIonModes;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IonModeSettings;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.preprocessing.deisotope.IsotopePatternDetection;
import de.unijena.bioinf.sirius.preprocessing.deisotope.TargetedIsotopePatternDetection;
import de.unijena.bioinf.sirius.preprocessing.elems.ElementDetection;
import de.unijena.bioinf.sirius.preprocessing.ions.IonModeDetection;
import de.unijena.bioinf.sirius.preprocessing.merging.Ms1Merging;

import java.util.HashSet;

/**
 * Performs element detection, adduct detection, isotope pattern merging. But NOT MS/MS spectrum merging
 */
public class Ms1Preprocessor implements SiriusPreprocessor {

    protected MissingValueValidator fillMissingValues = new MissingValueValidator();
    protected ElementDetection elementDetection;
    protected IonModeDetection ionModeDetection ;
    protected Ms1Merging ms1Merging = new Ms1Merging();
    protected IsotopePatternDetection deisotoper = new TargetedIsotopePatternDetection();


    @Override
    public ProcessedInput preprocess(Ms2Experiment experiment) {
        final MutableMs2Experiment validated = fillMissingValues.validate(experiment, Warning.Logger, true);
        final ProcessedInput pinput = new ProcessedInput(validated, experiment);
        ms1Merging(pinput);
        isotopePatternDetection(pinput);
        elementDetection(pinput);
        ionModeDetection(pinput);
        return pinput;
    }

    /**
     * if no merged MS is given, merge the MS1 spectra into one merged MS
     */
    @Provides(MergedMs1Spectrum.class)
    protected void ms1Merging(ProcessedInput pinput) {
        ms1Merging.merge(pinput);
    }

    /**
     * Search in the MS for isotope pattern
     */
    @Requires(MergedMs1Spectrum.class)
    @Provides(Ms1IsotopePattern.class)
    protected void isotopePatternDetection(ProcessedInput pinput) {
        deisotoper.detectIsotopePattern(pinput);
    }

    /**
     * Detect elements based on MS spectrum
     * @param pinput
     */
    @Requires(Ms1IsotopePattern.class)
    @Provides(FormulaConstraints.class)
    private void elementDetection(ProcessedInput pinput) {
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
    private void ionModeDetection(ProcessedInput pinput) {
        final int charge = pinput.getExperimentInformation().getPrecursorIonType().getCharge();
        final IonModeSettings settings = pinput.getAnnotation(IonModeSettings.class);
        final PossibleIonModes ionModes = ionModeDetection.detect(pinput, settings.getDetectable(charge));

        final HashSet<IonMode> set = new HashSet<>(settings.getEnforced(charge));

        if (ionModes==null) set.addAll(settings.getFallback(charge));
        else set.addAll(ionModes.getIonModesWithProbabilityAboutZero());
        pinput.setAnnotation(PossibleIonModes.class, PossibleIonModes.uniformlyDistributed(set));
    }
}
