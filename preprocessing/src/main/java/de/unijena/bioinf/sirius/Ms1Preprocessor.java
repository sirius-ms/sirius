/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Ms2ExperimentValidator;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.deisotope.IsotopePatternDetection;
import de.unijena.bioinf.sirius.deisotope.TargetedIsotopePatternDetection;
import de.unijena.bioinf.sirius.elementdetection.DeepNeuralNetworkElementDetector;
import de.unijena.bioinf.sirius.elementdetection.DetectedFormulaConstraints;
import de.unijena.bioinf.sirius.elementdetection.ElementDetection;
import de.unijena.bioinf.sirius.elementdetection.TransformerElementDetector;
import de.unijena.bioinf.sirius.iondetection.DetectIonsFromMs1;
import de.unijena.bioinf.sirius.merging.Ms1Merging;
import de.unijena.bioinf.sirius.validation.Ms1Validator;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

/**
 * Performs element detection, adduct detection, isotope pattern merging. But NOT MS/MS spectrum merging
 */
public class Ms1Preprocessor implements SiriusPreprocessor {
    protected Ms2ExperimentValidator validator = new Ms1Validator();
    protected ElementDetection elementDetection = new TransformerElementDetector();
    protected DetectIonsFromMs1 ms1IonAdductDetection = new DetectIonsFromMs1();
    protected Ms1Merging ms1Merging = new Ms1Merging();
    protected IsotopePatternDetection deisotoper = new TargetedIsotopePatternDetection();


    @Override
    public ProcessedInput preprocess(Ms2Experiment experiment) {
        final MutableMs2Experiment validated = new MutableMs2Experiment(experiment);
        validateInput(validated);
        final ProcessedInput pinput = new ProcessedInput(validated, experiment);
        ms1Merging(pinput);
        isotopePatternDetection(pinput);
        adductDetection(pinput);
        elementDetection(pinput);
        adjustValenceFilter(pinput);
        createWhitesetFromCandidateList(pinput);

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
    @Requires(PossibleAdducts.class)
    @Provides(FormulaConstraints.class)
    public void elementDetection(ProcessedInput pinput) {
        final FormulaSettings settings = pinput.getAnnotationOrDefault(FormulaSettings.class);
        checkDetectableElementSettings(settings);

        final DetectedFormulaConstraints fc = elementDetection.detect(pinput);
        if (fc==null) {
            DetectedFormulaConstraints dfc = new DetectedFormulaConstraints(settings.getEnforcedAlphabet().getExtendedConstraints(settings.getFallbackAlphabet()), false);
            pinput.setAnnotation(FormulaConstraints.class, dfc); //to ensure backwards compatibility, still use FormulaConstraints.class as key
        } else {
            pinput.setAnnotation(FormulaConstraints.class, fc); //to ensure backwards compatibility, still use FormulaConstraints.class as key
        }
    }

    private void checkDetectableElementSettings(FormulaSettings settings) {
        Set<Element> supported = getSetOfPredictableElements();
        Set<Element> unsupported = settings.getAutoDetectionElements().stream().filter(e -> !supported.contains(e)).collect(Collectors.toSet());
        if (!unsupported.isEmpty()) {
            LoggerFactory.getLogger(this.getClass()).warn("The following elements were specified as detectables, but detection is not supported: "+unsupported.stream().map(Element::getSymbol).collect(Collectors.joining(",")));
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

        //todo this is contained of historical reasons. Remove if absolutely sure that this is not necessary anymore. Currently it still seems to be needed for detectElements() in the compute panel
        if (!exp.getPrecursorIonType().isIonizationUnknown()) {
            pinput.setAnnotation(PossibleAdducts.class, new PossibleAdducts(exp.getPrecursorIonType()));
            return;
        }

        if (pinput.hasAnnotation(PossibleAdducts.class)) return;

        final DetectedAdducts detAdds = exp.computeAnnotationIfAbsent(DetectedAdducts.class, DetectedAdducts::new);
        if (!detAdds.containsKey(DetectedAdducts.Source.MS1_PREPROCESSOR) && !detAdds.hasMoreImportantSource(DetectedAdducts.Source.MS1_PREPROCESSOR))  {
            //Source.MS1_PREPROCESSOR shall only be present for peaklist data for which adducts are not specified in input file
            final int charge = exp.getPrecursorIonType().getCharge();

            final AdductSettings settings = pinput.getAnnotationOrDefault(AdductSettings.class);
            final PossibleAdducts ionModes = ms1IonAdductDetection.detect(pinput, settings.getDetectable(charge).stream().filter(not(PrecursorIonType::isMultimere)).collect(Collectors.toSet()));

            if (ionModes != null)
                detAdds.put(DetectedAdducts.Source.MS1_PREPROCESSOR, new PossibleAdducts(ionModes.getAdducts()));
        }
        pinput.setAnnotation(PossibleAdducts.class, exp.getPossibleAdductsOrFallback());
    }

    @Requires(FormulaConstraints.class)
    @Requires(PossibleAdducts.class)
    @Requires(AdductSettings.class)
    public void adjustValenceFilter(ProcessedInput pinput) { //todo ElementFilter: check if this is still correct
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

    @Requires(FormulaConstraints.class)
    @Requires(PossibleAdducts.class)
    @Provides(Whiteset.class)
    private void createWhitesetFromCandidateList(ProcessedInput pinput) {
        Whiteset whiteset = pinput.computeAnnotationIfAbsent(Whiteset.class, Whiteset::empty); //should not exist in general. maybe just for some old code.
        FormulaSearchSettings formulaSettings = pinput.getAnnotation(FormulaSearchSettings.class, FormulaSearchSettings::deNovoOnly);
        FormulaConstraints formulaConstraints = pinput.getAnnotationOrThrow(FormulaConstraints.class);
        PossibleAdducts possibleAdducts = pinput.getAnnotationOrThrow(PossibleAdducts.class);

        final boolean formulaGiven = pinput.getOriginalInput().getMolecularFormula()!=null;

        if (formulaGiven) {
            //single formula given in input file or otherwise specified by the user. This part also ensure backwards compatibility with old code / evaluation scripts etc. this should not be necessary for SIRIUS frontend application
            Set<MolecularFormula> inputFormulaSingleton = Collections.singleton(pinput.getOriginalInput().getMolecularFormula());
            if (formulaSettings.prioritizeAndForceCandidatesFromUserInput) {
                //molecular formula given in by user / input file. Force it.
                //PossibleAdducts should always contain a matching adduct after Ms2Validator was run -> is this also true for MS1?
                warnIfFormulaCandidateWithoutMatchingAdduct(inputFormulaSingleton, possibleAdducts, pinput.getExperimentInformation().getIonMass());
                whiteset = Whiteset.ofNeutralizedFormulas(inputFormulaSingleton, Ms1Preprocessor.class).setRequiresDeNovo(false).setRequiresBottomUp(false).setIgnoreMassDeviationToResolveIonType(true).setFinalized(true);
                pinput.setAnnotation(Whiteset.class, whiteset);
                return;
            } else {
                //just add to whiteset. Still, it is kind of strange not to enforce it.
                whiteset = whiteset.addNeutral(inputFormulaSingleton, Ms1Preprocessor.class);
            }
        }

        if (pinput.hasAnnotation(CandidateFormulas.class) && pinput.getAnnotationOrThrow(CandidateFormulas.class).numberOfFormulas()>0) { //I think somehow we always have at least an empty based on DefaultInstanceProvider
            //CandidateFormulas may be set by user, database or from input files.
            //here we convert them to Whiteset to use and modify internally
            CandidateFormulas candidateFormulas = pinput.getAnnotationOrThrow(CandidateFormulas.class);
            if (formulaSettings.prioritizeAndForceCandidatesFromUserInput && candidateFormulas.hasUserInputProvider()) {
                //molecular formula candidate set given in input file. Force it.
                warnIfFormulaCandidateWithoutMatchingAdduct(candidateFormulas.getWhitesetOfUserInputCandidates().getNeutralFormulas(), possibleAdducts, pinput.getExperimentInformation().getIonMass());
                whiteset = candidateFormulas.getWhitesetOfUserInputCandidates().setRequiresDeNovo(false).setRequiresBottomUp(false).setIgnoreMassDeviationToResolveIonType(true).setFinalized(true);
                pinput.setAnnotation(Whiteset.class, whiteset);
                return;
            } else {
                Whiteset candidateWhiteset = candidateFormulas.toWhiteSet();
                if (formulaSettings.applyFormulaConstraintsToDatabaseCandidates) candidateWhiteset = candidateWhiteset.filter(formulaConstraints, possibleAdducts.getAdducts(), Ms1Preprocessor.class);

                if (candidateFormulas.hasSpectralLibraryMatchProvidersProvider()) {
                    warnIfFormulaCandidateWithoutMatchingAdduct(candidateFormulas.getCandidatesFromSpectralLibraryMatches(), possibleAdducts, pinput.getExperimentInformation().getIonMass());
                    candidateWhiteset = candidateWhiteset.addEnforedNeutral(candidateFormulas.getCandidatesFromSpectralLibraryMatches(), CandidateFormulas.class);
                }

                if (whiteset.isEmpty()) {
                    whiteset = candidateWhiteset;
                } else {
                    whiteset = whiteset.add(candidateWhiteset);
                }
                whiteset = whiteset.setIgnoreMassDeviationToResolveIonType(formulaSettings.ignoreMassDeviationForCandidateList);
            }
        }
        whiteset = whiteset
                .setRequiresDeNovo(formulaSettings.useDeNovoFor(pinput.getExperimentInformation().getIonMass()))
                .setRequiresBottomUp(formulaSettings.useBottomUpFor(pinput.getExperimentInformation().getIonMass()));

        pinput.setAnnotation(Whiteset.class, whiteset);
    }

    /**
     * check and warn for enforced molecular formulas
     * @param candidates
     * @param possibleAdducts
     * @param precursorMass
     * @return
     */
    private boolean warnIfFormulaCandidateWithoutMatchingAdduct(Set<MolecularFormula> candidates, PossibleAdducts possibleAdducts, double precursorMass) {
        Set<MolecularFormula> issues = candidates.stream().filter(mf -> possibleAdducts.getAdducts().stream().noneMatch(adduct -> adduct.isApplicableToNeutralFormula(mf) && Math.abs(adduct.neutralMassToPrecursorMass(mf.getMass())-precursorMass)<0.1)).collect(Collectors.toSet());
        if (!issues.isEmpty()) {
            LoggerFactory.getLogger(this.getClass()).warn("Enforced molecular formula has no matching adduct: "+issues.stream().map(MolecularFormula::toString).collect(Collectors.joining(",")) + ". Adducts are: "+possibleAdducts.getAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.joining(",")));
            return true;
        }
        return false;
    }

    public Set<Element> getSetOfPredictableElements() {
        return elementDetection.getPredictableElements();
    }

}
