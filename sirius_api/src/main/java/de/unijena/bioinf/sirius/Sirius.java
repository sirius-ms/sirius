/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai DÃ¼hrkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformation;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.SupportVectorMolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.AbstractTreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FasterTreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ExtractedIsotopePattern;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.MasterJJob;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ionGuessing.GuessIonizationFromMs1Result;
import de.unijena.bioinf.sirius.ionGuessing.IonGuesser;
import de.unijena.bioinf.sirius.ionGuessing.IonGuessingMode;
import de.unijena.bioinf.sirius.ionGuessing.IonGuessingSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;


//todo we should cleanup the api methods, proof which should be private and which are no longer needed, or at least change them, so that they use the identification job
public class Sirius {
    protected Profile profile;
    protected ElementPredictor elementPrediction;
    protected IonGuesser ionGuessing;
    protected PeriodicTable table;
    /**
     * for internal use to easily switch and experiment with implementation details
     */
    protected boolean useFastMode = true;
    public void setFastMode(boolean useFastMode) {
        this.useFastMode = useFastMode;
    }


    public Sirius(@NotNull String profile) {
        this(Profile.fromString(profile));
    }

    public Sirius(@NotNull Profile profile) {
        this(profile, PeriodicTable.getInstance());
    }

    public Sirius() {
        this.profile = PropertyManager.DEFAULTS.createInstanceWithDefaults(Profile.class);
    }

    public Sirius(@NotNull Profile profile, @NotNull PeriodicTable table) {
        this.profile = profile;
        this.table = table;
        this.ionGuessing = new IonGuesser();
    }

    public FragmentationPatternAnalysis getMs2Analyzer() {
        return profile.fragmentationPatternAnalysis;
    }

    public IsotopePatternAnalysis getMs1Analyzer() {
        return profile.isotopePatternAnalysis;
    }

    public ElementPredictor getElementPrediction() {
        if (elementPrediction == null) {
            DNNRegressionPredictor defaultPredictor = new DNNRegressionPredictor();
            defaultPredictor.disableSilicon();
            elementPrediction = defaultPredictor;
        }
        return elementPrediction;
    }

    public void setElementPrediction(ElementPredictor elementPrediction) {
        this.elementPrediction = elementPrediction;
    }

    public void enableAutomaticElementDetection(@NotNull Ms2Experiment experiment, boolean enabled) {
        FormulaSettings current = experiment.getAnnotationOrDefault(FormulaSettings.class);
        if (enabled) {
            experiment.setAnnotation(FormulaSettings.class, current.autoDetect(getElementPrediction().getChemicalAlphabet().getElements().toArray(new Element[0])));
        } else {
            disableElementDetection(experiment, current);
        }
    }

    protected AbstractTreeComputationInstance getTreeComputationImplementation(FragmentationPatternAnalysis analyzer, Ms2Experiment input, int numberOfResultsToKeep, int numberOfResultsToKeepPerIonization) {
        if (useFastMode)
            return new FasterTreeComputationInstance(analyzer, input, numberOfResultsToKeep, numberOfResultsToKeepPerIonization);
        else {
            if (numberOfResultsToKeepPerIonization > 0)
                throw new RuntimeException("TreeComputationInstance does not support parameter 'numberOfResultsToKeepPerIonization'");
            return new TreeComputationInstance(analyzer, input, numberOfResultsToKeep);
        }
    }

    public void detectPossibleIonModesFromMs1(ProcessedInput processedInput) {
        final List<PrecursorIonType> ionTypes = new ArrayList<>();
        for (Ionization ionMode : table.getKnownIonModes(processedInput.getExperimentInformation().getPrecursorIonType().getCharge())) {
            ionTypes.add(PrecursorIonType.getPrecursorIonType(ionMode));
        }
        detectPossibleIonModesFromMs1(processedInput, ionTypes.toArray(new PrecursorIonType[ionTypes.size()]));
    }

    public void detectPossibleIonModesFromMs1(ProcessedInput processedInput, PrecursorIonType... allowedIonModes) {
        final IonGuessingMode gm = processedInput.getAnnotationOrDefault(IonGuessingMode.class);
        //if disabled, do not guess ionization
        if (!gm.isEnabled())
            return;

        final PossibleIonModes pim = processedInput.getAnnotation(PossibleIonModes.class, () -> new PossibleIonModes());
        final GuessIonizationFromMs1Result guessIonization = ionGuessing.guessIonization(processedInput.getExperimentInformation(), allowedIonModes);

        if (guessIonization.guessedIonTypes.length > 0) {
            if (gm.equals(IonGuessingMode.SELECT)) {
                //fully trust any ionization prediction
                gm.updateGuessedIons(pim, guessIonization.guessedIonTypes);

            } else {

                if (guessIonization.getGuessingSource() == IonGuessingSource.MergedMs1Spectrum
                ) {
                    //don't fully trust guessing from simple (non-merged) MS1 spectrum.
                    double[] probabilities = new double[allowedIonModes.length];
                    Arrays.fill(probabilities, 0.01);
                    PrecursorIonType[] guessedIonizations = guessIonization.guessedIonTypes;
                    for (int i = 0; i < allowedIonModes.length; i++) {
                        PrecursorIonType allowedIonMode = allowedIonModes[i];
                        for (int j = 0; j < guessedIonizations.length; j++) {
                            PrecursorIonType guessedIonization = guessedIonizations[j];
                            if (guessedIonization.equals(allowedIonMode)) {
                                probabilities[i] = 1d;
                                break;
                            }
                        }
                    }
                    gm.updateGuessedIons(pim, guessIonization.candidateIonTypes, probabilities);
                } else {
                    //merged MS1 should be more reliable (probably openMS features or something like that), so we trust more
                    gm.updateGuessedIons(pim, guessIonization.guessedIonTypes);
                }
                gm.updateGuessedIons(pim, guessIonization.guessedIonTypes);
            }
        }
        processedInput.setAnnotation(PossibleIonModes.class, pim);
        //also update PossibleAdducts
        final PossibleAdducts pa = processedInput.computeAnnotationIfAbsent(PossibleAdducts.class, PossibleAdducts::new);
        pa.update(pim);
    }

    /**
     * Identify the molecular formula of the measured compound using the provided MS and MSMS data
     *
     * @param experiment input data
     * @return the top tree
     */
    @Deprecated
    public IdentificationResult identify(Ms2Experiment experiment) {
        return identify(experiment, 1).get(0);
    }

    /**
     * Identify the molecular formula of the measured compound using the provided MS and MSMS data
     *
     * @param experiment        input data
     * @param numberOfCandidates number of top candidates to return
     * @return a list of identified molecular formulas together with their tree
     */
    @Deprecated
    public List<IdentificationResult> identify(Ms2Experiment experiment, int numberOfCandidates) {
        final AbstractTreeComputationInstance instance = getTreeComputationImplementation(getMs2Analyzer(), experiment, numberOfCandidates, -1);
        final ProcessedInput pinput = instance.validateInput();
        performMs1Analysis(instance);
        SiriusJobs.getGlobalJobManager().submitJob(instance);
        AbstractTreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = createIdentificationResults(fr, instance);//postprocess results
        return irs;
    }

    @Deprecated
    public List<IdentificationResult> identifyPrecursorAndIonization(Ms2Experiment experiment,
                                                                     int numberOfCandidates, IsotopePatternHandling iso) {
        final MutableMs2Experiment exp = new MutableMs2Experiment(experiment);
        exp.setAnnotation(PossibleIonModes.class, PossibleIonModes.defaultFor(experiment.getPrecursorIonType().getCharge()));
        return identify(exp, numberOfCandidates, true, iso);
    }

    /**
     * Identify the molecular formula of the measured compound by combining an isotope pattern analysis on MS data with a fragmentation pattern analysis on MS/MS data
     *
     * @param experiment        input data
     * @param numberOfCandidates number of candidates to output
     * @param recalibrating      true if spectra should be recalibrated during tree computation
     * @param deisotope          set this to 'omit' to ignore isotope pattern, 'filter' to use it for selecting molecular formula candidates or 'score' to rerank the candidates according to their isotope pattern
     * @param whiteList          restrict the analysis to this subset of molecular formulas. If this set is empty, consider all possible molecular formulas
     * @return a list of identified molecular formulas together with their tree
     */
    @Deprecated
    public List<IdentificationResult> identify(Ms2Experiment experiment, int numberOfCandidates,
                                               boolean recalibrating, IsotopePatternHandling deisotope, Set<MolecularFormula> whiteList) {
        final AbstractTreeComputationInstance instance = getTreeComputationImplementation(getMs2Analyzer(), experiment, numberOfCandidates, -1);
        final ProcessedInput pinput = instance.validateInput();
        pinput.setAnnotation(ForbidRecalibration.class, recalibrating ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
        if (whiteList != null) pinput.setAnnotation(Whiteset.class, new Whiteset(whiteList));
        pinput.setAnnotation(IsotopeSettings.class, new IsotopeSettings(deisotope.isFiltering(), deisotope.isScoring() ? 1 : 0));
        performMs1Analysis(instance);
        SiriusJobs.getGlobalJobManager().submitJob(instance);
        AbstractTreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = createIdentificationResults(fr, instance);//postprocess results
        return irs;
    }

    protected List<IdentificationResult> createIdentificationResults(AbstractTreeComputationInstance.FinalResult
                                                                             fr, AbstractTreeComputationInstance computationInstance) {
        addScoreThresholdOnUnconsideredCandidates(fr, computationInstance.precompute());

        final List<IdentificationResult> irs = new ArrayList<>();
        int k = 0;
        for (FTree tree : fr.getResults()) {
            IdentificationResult result = new IdentificationResult(tree, ++k);
            irs.add(result);

        }
        return irs;
    }

    private static void addScoreThresholdOnUnconsideredCandidates(AbstractTreeComputationInstance.FinalResult
                                                                          fr, ProcessedInput processedInput) {
        //add annotation of score bound on unconsidered instances
        int numberOfResults = fr.getResults().size();
        if (numberOfResults == 0) return;
        int numberOfDecompositions = processedInput.getAnnotationOrThrow(DecompositionList.class).getDecompositions().size();
        int numberOfUnconsideredCandidates = numberOfDecompositions - numberOfResults;
        //trees should be sorted by score
        double lowestConsideredCandidatesScore = fr.getResults().get(numberOfResults - 1).getAnnotationOrThrow(TreeScoring.class).getOverallScore();
        UnconsideredCandidatesUpperBound unconsideredCandidatesUpperBound = new UnconsideredCandidatesUpperBound(numberOfUnconsideredCandidates, lowestConsideredCandidatesScore);
        for (FTree tree : fr.getResults()) {
            tree.addAnnotation(UnconsideredCandidatesUpperBound.class, unconsideredCandidatesUpperBound);
        }
    }

    public List<IdentificationResult> identify(Ms2Experiment experiment, int numberOfCandidates,
                                               boolean recalibrating, IsotopePatternHandling deisotope) {
        return identify(experiment, numberOfCandidates, recalibrating, deisotope, (FormulaConstraints) null);
    }

    /**
     * Identify the molecular formula of the measured compound by combining an isotope pattern analysis on MS data with a fragmentation pattern analysis on MS/MS data
     *
     * @param experiment        input data
     * @param numberOfCandidates number of candidates to output
     * @param recalibrating      true if spectra should be recalibrated during tree computation
     * @param deisotope          set this to 'omit' to ignore isotope pattern, 'filter' to use it for selecting molecular formula candidates or 'score' to rerank the candidates according to their isotope pattern
     * @param formulaConstraints use if specific constraints on the molecular formulas shall be imposed (may be null)
     * @return a list of identified molecular formulas together with their tree
     */
    public List<IdentificationResult> identify(Ms2Experiment experiment, int numberOfCandidates,
                                               boolean recalibrating, IsotopePatternHandling deisotope, FormulaConstraints formulaConstraints) {
        return identify(experiment, numberOfCandidates, -1, recalibrating, deisotope, formulaConstraints);
    }

    /**
     * Identify the molecular formula of the measured compound by combining an isotope pattern analysis on MS data with a fragmentation pattern analysis on MS/MS data
     *
     * @param experiment                     input data
     * @param numberOfCandidates              number of candidates to output
     * @param numberOfCandidatesPerIonization minimum number of candidates to output per ionization
     * @param recalibrating                   true if spectra should be recalibrated during tree computation
     * @param deisotope                       set this to 'omit' to ignore isotope pattern, 'filter' to use it for selecting molecular formula candidates or 'score' to rerank the candidates according to their isotope pattern
     * @param formulaConstraints              use if specific constraints on the molecular formulas shall be imposed (may be null)
     * @return a list of identified molecular formulas together with their tree
     */
    public List<IdentificationResult> identify(@NotNull Ms2Experiment experiment, int numberOfCandidates,
                                               int numberOfCandidatesPerIonization, boolean recalibrating, @NotNull IsotopePatternHandling deisotope, @Nullable FormulaConstraints
                                                       formulaConstraints) {
        final AbstractTreeComputationInstance instance = getTreeComputationImplementation(getMs2Analyzer(), experiment, numberOfCandidates, numberOfCandidatesPerIonization);
        final ProcessedInput pinput = instance.validateInput();
        pinput.setAnnotation(IsotopeSettings.class, new IsotopeSettings(deisotope.isFiltering(), deisotope.isScoring() ? 1 : 0));
        pinput.setAnnotation(ForbidRecalibration.class, recalibrating ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
        pinput.setAnnotation(FormulaSettings.class, new FormulaSettings(formulaConstraints, getElementPrediction().getChemicalAlphabet(), FormulaConstraints.empty()));

        performMs1Analysis(instance);
        SiriusJobs.getGlobalJobManager().submitJob(instance);
        AbstractTreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = createIdentificationResults(fr, instance);//postprocess results
        return irs;
    }

    public FormulaConstraints predictElementsFromMs1(Ms2Experiment experiment) {
        final SimpleSpectrum pattern = getMs1Analyzer().extractPattern(experiment, experiment.getIonMass());
        if (pattern == null) return null;
        return getElementPrediction().predictConstraints(pattern);
    }

    public IdentificationResult compute(@NotNull Ms2Experiment experiment, MolecularFormula formula) {
        return compute(experiment, formula, true);
    }

    public BasicJJob<IdentificationResult> makeComputeJob(@NotNull Ms2Experiment experiment, MolecularFormula
            formula) {
        final AbstractTreeComputationInstance instance = getTreeComputationImplementation(getMs2Analyzer(), experiment, 1, -1);
        final ProcessedInput pinput = instance.validateInput();
        pinput.setAnnotation(Whiteset.class, Whiteset.of(formula));
        pinput.setAnnotation(ForbidRecalibration.class, ForbidRecalibration.ALLOWED);
        return instance.wrap((f) -> new IdentificationResult(f.getResults().get(0), 1));
    }

    /**
     * Compute a fragmentation tree for the given MS/MS data using the given neutral molecular formula as explanation for the measured compound
     *
     * @param experiment    input data
     * @param formula       neutral molecular formula of the measured compound
     * @param recalibrating true if spectra should be recalibrated during tree computation
     * @return A single instance of IdentificationResult containing the computed fragmentation tree
     */
    public IdentificationResult compute(@NotNull Ms2Experiment experiment, MolecularFormula formula,
                                        boolean recalibrating) {
        final AbstractTreeComputationInstance instance = getTreeComputationImplementation(getMs2Analyzer(), experiment, 1, -1);
        final ProcessedInput pinput = instance.validateInput();
        pinput.setAnnotation(Whiteset.class, Whiteset.of(formula));
        pinput.setAnnotation(ForbidRecalibration.class, recalibrating ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
        SiriusJobs.getGlobalJobManager().submitJob(instance);
        final IdentificationResult ir = new IdentificationResult(instance.takeResult().getResults().get(0), 1);
        // tree is always beautyfied
        if (recalibrating) ir.setBeautifulTree(ir.getRawTree());
        return ir;

    }


    public boolean beautifyTree(IdentificationResult result, Ms2Experiment experiment) {
        return beautifyTree(null, result, experiment, true);
    }

    /**
     * compute and set the beautiful version of the {@link IdentificationResult}s {@link FTree}.
     * Aka: try to find a {@link FTree} with the same root molecular formula which explains the desired amount of the spectrum - if necessary by increasing the tree size scorer.
     *
     * @param result
     * @param experiment
     * @return true if a beautiful tree was found
     */
    public boolean beautifyTree(IdentificationResult result, Ms2Experiment experiment, boolean recalibrating) {
        return beautifyTree(null, result, experiment, recalibrating);
    }

    public boolean beautifyTree(MasterJJob<?> master, IdentificationResult result, Ms2Experiment experiment,
                                boolean recalibrating) {
        if (result.getBeautifulTree() != null) return true;
        FTree beautifulTree = beautifyTree(master, result.getStandardTree(), experiment, recalibrating);
        if (beautifulTree != null) {
            result.setBeautifulTree(beautifulTree);
            return true;
        }
        return false;
    }

    public FTree beautifyTree(FTree tree, Ms2Experiment experiment, boolean recalibrating) {
        return beautifyTree(null, tree, experiment, recalibrating);
    }

    public FTree beautifyTree(MasterJJob<?> master, FTree tree, Ms2Experiment experiment, boolean recalibrating) {
        if (tree.getAnnotation(Beautified.class, Beautified.IS_UGGLY).isBeautiful()) return tree;
        final PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);
        final MutableMs2Experiment mexp = new MutableMs2Experiment(experiment);
        mexp.setPrecursorIonType(ionType);
        final MolecularFormula formula;
        switch (tree.getAnnotation(IonTreeUtils.Type.class, IonTreeUtils.Type.RAW)) {
            case RESOLVED:
                if (ionType.isIntrinsicalCharged())
                    formula = ionType.measuredNeutralMoleculeToNeutralMolecule(tree.getRoot().getFormula());
                else
                    formula = tree.getRoot().getFormula();
                break;
            case IONIZED:
                formula = ionType.precursorIonToNeutralMolecule(tree.getRoot().getFormula());
                break;
            case RAW:
            default:
                formula = ionType.measuredNeutralMoleculeToNeutralMolecule(tree.getRoot().getFormula());
                ;
                break;
        }
        //todo remove when cleaning up the api
        final FTree btree;
        if (master != null) {
            btree = master.submitSubJob(FasterTreeComputationInstance.beautify(getMs2Analyzer(), tree)).takeResult().getResults().get(0);
        } else {
            btree = SiriusJobs.getGlobalJobManager().submitJob(FasterTreeComputationInstance.beautify(getMs2Analyzer(), tree)).takeResult().getResults().get(0);
        }


        if (!btree.getAnnotation(Beautified.class, Beautified.IS_UGGLY).isBeautiful()) {
            LoggerFactory.getLogger(Sirius.class).warn("Tree beautification annotation is not properly set.");
            btree.setAnnotation(Beautified.class, Beautified.IS_BEAUTIFUL);
        }
        return btree;
    }


    //////////////////////////////////// STATIC API METHODS ////////////////////////////////////////////////////////////
    public static void setAnnotations(@NotNull Ms2Experiment
                                              experiment, @NotNull Annotated<Ms2ExperimentAnnotation> annotations) {
        experiment.setAnnotationsFrom(annotations);
    }


    public static MutableMs2Experiment makeMutable(@NotNull Ms2Experiment experiment) {
        if (experiment instanceof MutableMs2Experiment) return (MutableMs2Experiment) experiment;
        else return new MutableMs2Experiment(experiment);
    }

    public static void setAllowedMassDeviationMs1(@NotNull MutableMs2Experiment experiment, Deviation fragmentMassDeviation) {
        setAllowedMassDeviation(experiment, fragmentMassDeviation, MS1MassDeviation.class);
    }

    public static void setAllowedMassDeviationMs2(@NotNull MutableMs2Experiment experiment, Deviation fragmentMassDeviation) {
        setAllowedMassDeviation(experiment, fragmentMassDeviation, MS2MassDeviation.class);
    }

    public static <T extends MassDeviation> void setAllowedMassDeviation(@NotNull MutableMs2Experiment experiment, Deviation fragmentMassDeviation, Class<T> deviationType) {
        experiment.setAnnotation(deviationType, experiment.getAnnotationOrDefault(deviationType).withAllowedMassDeviation(fragmentMassDeviation));
    }


    public static void setAllowedIonModes(@NotNull Ms2Experiment experiment, Ionization... ionModes) {
        final PossibleIonModes pa = new PossibleIonModes();
        for (Ionization ion : ionModes) {
            pa.add(ion, 1d);
        }
        setAllowedIonModes(experiment, pa);
    }

    public static void setAllowedIonModes(@NotNull Ms2Experiment experiment, PossibleIonModes ionModes) {
        experiment.setAnnotation(PossibleIonModes.class, ionModes);
    }

    public static void setAllowedIonModes(@NotNull ProcessedInput experiment, Ionization... ionModes) {
        final PossibleIonModes pa = new PossibleIonModes();
        for (Ionization ion : ionModes) {
            pa.add(ion, 1d);
        }
        setAllowedIonModes(experiment, ionModes);
    }

    public static void setAllowedIonModes(@NotNull ProcessedInput experiment, PossibleIonModes ionModes) {
        experiment.setAnnotation(PossibleIonModes.class, ionModes);

    }

    public static void setIonModeWithProbability(@NotNull Ms2Experiment experiment, Ionization ion,
                                                 double probability) {
        final PossibleIonModes pa = experiment.getAnnotation(PossibleIonModes.class, PossibleIonModes::new);
        pa.add(ion, probability);
        experiment.setAnnotation(PossibleIonModes.class, pa);
    }

    public static void setAllowedAdducts(@NotNull Ms2Experiment experiment, PrecursorIonType... adducts) {
        setAllowedAdducts(experiment, new PossibleAdducts(adducts));
    }

    public static void setAllowedAdducts(@NotNull Ms2Experiment experiment, PossibleAdducts adducts) {
        experiment.setAnnotation(PossibleAdducts.class, adducts);
    }

    public static void setAllowedAdducts(@NotNull ProcessedInput processedInput, PrecursorIonType... adducts) {
        setAllowedAdducts(processedInput, new PossibleAdducts(adducts));
    }

    public static void setAllowedAdducts(@NotNull ProcessedInput processedInput, PossibleAdducts adducts) {
        processedInput.setAnnotation(PossibleAdducts.class, adducts);
    }

    public static void setFormulaSearchList(@NotNull Ms2Experiment experiment, MolecularFormula... formulas) {
        setFormulaSearchList(experiment, Arrays.asList(formulas));
    }

    public static void setFormulaSearchList(@NotNull Ms2Experiment
                                                    experiment, Iterable<MolecularFormula> formulas) {
        final HashSet<MolecularFormula> fs = new HashSet<MolecularFormula>();
        for (MolecularFormula f : formulas) fs.add(f);
        final Whiteset whiteset = new Whiteset(fs);
        experiment.setAnnotation(Whiteset.class, whiteset);
    }

    public static void enableRecalibration(@NotNull MutableMs2Experiment experiment, boolean enabled) {
        experiment.setAnnotation(ForbidRecalibration.class, enabled ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
    }

    public static void setIsotopeMode(@NotNull MutableMs2Experiment experiment, IsotopeSettings isotopeSettings) {
        experiment.setAnnotation(IsotopeSettings.class, isotopeSettings);
    }

    public static void setAutomaticElementDetectionFor(@NotNull Ms2Experiment experiment, Element elements) {
        FormulaSettings current = experiment.getAnnotationOrDefault(FormulaSettings.class);
        experiment.setAnnotation(FormulaSettings.class, current.withoutAutoDetect().autoDetect(elements));
    }

    @Deprecated
    public static void setFormulaConstraints(@NotNull Ms2Experiment experiment, FormulaConstraints constraints) {
        //experiment.setAnnotation(FormulaConstraints.class, constraints);
        setFormulaSettings(experiment, experiment.getAnnotationOrDefault(FormulaSettings.class).enforce(constraints));
    }

    public static void setFormulaSettings(@NotNull Ms2Experiment experiment, FormulaSettings settings) {
        experiment.setAnnotation(FormulaSettings.class, settings);
    }

    public static void setIsolationWindow(@NotNull MutableMs2Experiment experiment, IsolationWindow isolationWindow) {
        experiment.setAnnotation(IsolationWindow.class, isolationWindow);
    }

    public static void setTimeout(@NotNull MutableMs2Experiment experiment, int timeoutPerInstanceInSeconds,
                                  int timeoutPerDecompositionInSeconds) {
        experiment.setAnnotation(Timeout.class, Timeout.newTimeout(timeoutPerInstanceInSeconds, timeoutPerDecompositionInSeconds));
    }

    public static void disableTimeout(@NotNull MutableMs2Experiment experiment) {
        experiment.setAnnotation(Timeout.class, Timeout.NO_TIMEOUT);
    }

    public static void disableElementDetection(@NotNull Ms2Experiment experiment) {
        disableElementDetection(experiment, experiment.getAnnotationOrDefault(FormulaSettings.class));
    }

    public static void disableElementDetection(@NotNull Ms2Experiment experiment, FormulaSettings current) {
        experiment.setAnnotation(FormulaSettings.class, current.withoutAutoDetect());
    }

    public static void setNumberOfCandidates(@NotNull Ms2Experiment experiment, NumberOfCandidates value) {
        experiment.setAnnotation(NumberOfCandidates.class, value);
    }

    public static void setNumberOfCandidatesPerIon(@NotNull Ms2Experiment experiment, NumberOfCandidatesPerIon value) {
        experiment.setAnnotation(NumberOfCandidatesPerIon.class, value);
    }

    /*
    remove all but the most intense ms2
    todo this is more a hack for bad data. maybe remove if data quality stuff is done
     */
    public static void onlyKeepMostIntenseMS2(MutableMs2Experiment experiment) {
        if (experiment == null || experiment.getMs2Spectra().size() == 0) return;
        double precursorMass = experiment.getIonMass();
        int mostIntensiveIdx = -1;
        double maxIntensity = -1d;
        int pos = -1;
        if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()) {
            //one ms1 corresponds to one ms2. we take ms2 with most intense ms1 precursor peak
            for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
                ++pos;
                Deviation dev = new Deviation(100);
                int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, dev);
                if (idx < 0) continue;
                double intensity = spectrum.getIntensityAt(idx);
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    mostIntensiveIdx = pos;
                }
            }
        }
        if (mostIntensiveIdx < 0) {
            //take ms2 with highest summed intensity
            pos = -1;
            for (Spectrum<Peak> spectrum : experiment.getMs2Spectra()) {
                ++pos;
                final int n = spectrum.size();
                double sumIntensity = 0d;
                for (int i = 0; i < n; ++i) {
                    sumIntensity += spectrum.getIntensityAt(i);
                }
                if (sumIntensity > maxIntensity) {
                    maxIntensity = sumIntensity;
                    mostIntensiveIdx = pos;
                }
            }
        }

        List<SimpleSpectrum> ms1List = new ArrayList<>();
        List<MutableMs2Spectrum> ms2List = new ArrayList<>();
        if (experiment.getMs1Spectra().size() == experiment.getMs2Spectra().size()) {
            ms1List.add(experiment.getMs1Spectra().get(mostIntensiveIdx));
        } else {
            ms1List.addAll(experiment.getMs1Spectra());
        }
        ms2List.add(experiment.getMs2Spectra().get(mostIntensiveIdx));
        experiment.setMs1Spectra(ms1List);
        experiment.setMs2Spectra(ms2List);
    }
    ////////////////////////////////////////////////////////////////////////////////



    /*
        DATA STRUCTURES API CALLS
     */

    /**
     * Wraps an array of m/z values and and array of intensity values into a spectrum object that can be used by the SIRIUS library. The resulting spectrum is a lightweight view on the array, so changes in the array are reflected in the spectrum. The spectrum object itself is immutable.
     *
     * @param mz          mass to charge ratios
     * @param intensities intensity values. Can be normalized or absolute - SIRIUS will performNormalization them itself at later point
     * @return view on the arrays implementing the Spectrum interface
     */
    public Spectrum<Peak> wrapSpectrum(double[] mz, double[] intensities) {
        return Spectrums.wrap(mz, intensities);
    }

    /**
     * Lookup the symbol in the periodic table and returns the corresponding Element object or null if no element with this symbol exists.
     *
     * @param symbol symbol of the element, e.g. H for hydrogen or Cl for chlorine
     * @return instance of Element class
     */
    public Element getElement(String symbol) {
        return table.getByName(symbol);
    }

    /**
     * Lookup the ionization name and returns the corresponding ionization object or null if no ionization with this name is registered. The name of an ionization has the syntax [M+ADDUCT]CHARGE, for example [M+H]+ or [M-H]-.
     * <p>
     * Deprecated: Ionization is now for the ion-mode (protonation or deprotonation, number of charges, ...). Use
     * getPrecursorIonType to get a PrecursorIonType object that contains adducts and in-source fragmentation as well as
     * the ion mode of the precursor ion
     *
     * @param name name of the ionization
     * @return adduct object
     */
    @Deprecated
    public Ionization getIonization(String name) {
        return getPrecursorIonType(name).getIonization();
    }

    /**
     * Lookup the ionization name and returns the corresponding ionization object or null if no ionization with this name is registered. The name of an ionization has the syntax [M+ADDUCT]CHARGE, for example [M+H]+ or [M-H]-.
     *
     * @param name name of the ionization
     * @return adduct object
     */
    public PrecursorIonType getPrecursorIonType(String name) {
        return table.ionByName(name);
    }


    /**
     * Charges are subclasses of Ionization. So they can be used everywhere as replacement for ionizations. A charge is very similar to the [M]+ and [M]- ionizations. However, the difference is that [M]+ describes an intrinsically charged compound where the Charge +1 describes an compound with unknown adduct.
     *
     * @param charge either 1 for positive or -1 for negative charges.
     * @return
     */
    public Charge getCharge(int charge) {
        if (charge != -1 && charge != 1)
            throw new IllegalArgumentException("SIRIUS does not support multiple charged compounds");
        return new Charge(charge);
    }

    /**
     * Creates a Deviation object that describes a mass deviation as maximum of a relative term (in ppm) and an absolute term. Usually, mass accuracy is given as relative term in ppm, as measurement errors increase with higher masses. However, for very small compounds (and fragments!) these relative values might overestimate the mass accurary. Therefore, an absolute value have to be given.
     *
     * @param ppm mass deviation as relative value (in ppm)
     * @param abs mass deviation as absolute value (m/z)
     * @return Deviation object
     */
    public Deviation getMassDeviation(int ppm, double abs) {
        return new Deviation(ppm, abs);
    }

    /**
     * Creates a Deviation object with the given relative term. The absolute term is implicitly given by applying the relative term on m/z 100.
     *
     * @param ppm
     * @return
     */
    public Deviation getMassDeviation(int ppm) {
        return new Deviation(ppm);
    }

    /**
     * Parses a molecular formula from the given string
     *
     * @param f molecular formula (e.g. in Hill notation)
     * @return immutable molecular formula object
     */
    public MolecularFormula parseFormula(String f) {
        return MolecularFormula.parse(f);
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param formula neutral molecular formula of the compound
     * @param ion     ionization mode (can be an instance of Charge if the exact adduct is unknown)
     * @param ms1     the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2     a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(MolecularFormula formula, Ionization ion, Spectrum<Peak> ms1, Spectrum...
            ms2) {
        return getMs2Experiment(formula, PrecursorIonType.getPrecursorIonType(ion), ms1, ms2);
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param formula neutral molecular formula of the compound
     * @param ion     PrecursorIonType (contains ion mode as well as adducts and in-source fragmentations of the precursor ion)
     * @param ms1     the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2     a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(MolecularFormula formula, PrecursorIonType
            ion, Spectrum<Peak> ms1, Spectrum... ms2) {
        final MutableMs2Experiment exp = (MutableMs2Experiment) getMs2Experiment(ion.neutralMassToPrecursorMass(formula.getMass()), ion, ms1, ms2);
        exp.setMolecularFormula(formula);
        return exp;
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param parentMass the measured mass of the precursor ion. Can be either the MS peak or (if present) a MS/MS peak
     * @param ion        PrecursorIonType (contains ion mode as well as adducts and in-source fragmentations of the precursor ion)
     * @param ms1        the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2        a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(double parentMass, PrecursorIonType ion, Spectrum<Peak> ms1, Spectrum...
            ms2) {
        final MutableMs2Experiment mexp = new MutableMs2Experiment();
        mexp.setPrecursorIonType(ion);
        mexp.setIonMass(parentMass);
        for (Spectrum<Peak> spec : ms2) {
            mexp.getMs2Spectra().add(new MutableMs2Spectrum(spec, mexp.getIonMass(), CollisionEnergy.none(), 2));
        }
        return mexp;
    }

    /**
     * Creates a Ms2Experiment object from the given MS and MS/MS spectra. A Ms2Experiment is NOT a single run or measurement, but a measurement of a concrete compound. So a MS spectrum might contain several Ms2Experiments. However, each MS/MS spectrum should have on precursor or parent mass. All MS/MS spectra with the same precursor together with the MS spectrum containing this precursor peak can be seen as one Ms2Experiment.
     *
     * @param parentMass the measured mass of the precursor ion. Can be either the MS peak or (if present) a MS/MS peak
     * @param ion        ionization mode (can be an instance of Charge if the exact adduct is unknown)
     * @param ms1        the MS spectrum containing the isotope pattern of the measured compound. Might be null
     * @param ms2        a list of MS/MS spectra containing the fragmentation pattern of the measured compound
     * @return a MS2Experiment instance, ready to be analyzed by SIRIUS
     */
    public Ms2Experiment getMs2Experiment(double parentMass, Ionization ion, Spectrum<Peak> ms1, Spectrum... ms2) {
        return getMs2Experiment(parentMass, PrecursorIonType.getPrecursorIonType(ion), ms1, ms2);
    }

    /**
     * Formula Constraints consist of a chemical alphabet (a subset of the periodic table, determining which elements might occur in the measured compounds) and upperbounds for each of this elements. A formula constraint can be given like a molecular formula. Upperbounds are written in square brackets or omitted, if any number of this element should be allowed.
     *
     * @param constraints string representation of the constraint, e.g. "CHNOP[5]S[20]"
     * @return formula constraint object
     */
    public FormulaConstraints getFormulaConstraints(String constraints) {
        return new FormulaConstraints(constraints);
    }

    /**
     * Decomposes a mass and return a list of all molecular formulas which ionized mass is near the measured mass.
     * The maximal distance between the neutral mass of the measured ion and the theoretical mass of the decomposed formula depends on the chosen profile. For qtof it is 10 ppm, for Orbitrap and FTICR it is 5 ppm.
     *
     * @param mass   mass of the measured ion
     * @param ion    ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
     * @param constr the formula constraints, defining the allowed elements and their upperbounds
     * @return list of molecular formulas which theoretical ion mass is near the given mass
     */
    public List<MolecularFormula> decompose(double mass, Ionization ion, FormulaConstraints constr) {
        return decompose(mass, ion, constr, PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation);
    }

    /**
     * Decomposes a mass and return a list of all molecular formulas which ionized mass is near the measured mass
     *
     * @param mass   mass of the measured ion
     * @param ion    ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
     * @param constr the formula constraints, defining the allowed elements and their upperbounds
     * @param dev    the allowed mass deviation of the measured ion from the theoretical ion masses
     * @return
     */
    public List<MolecularFormula> decompose(double mass, Ionization ion, FormulaConstraints constr, Deviation dev) {
        return getMs2Analyzer().getDecomposerFor(constr.getChemicalAlphabet()).decomposeToFormulas(ion.subtractFromMass(mass), dev, constr);
    }

    /**
     * Applies a given biotransformation on a given Molecular formular and return the transformed formula(s)
     *
     * @param source         source formula for transformation
     * @param transformation to that will be applied to given Formula    ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
     * @return transformed MolecularFormulas
     */
    public List<MolecularFormula> bioTransform(MolecularFormula source, BioTransformation transformation) {
        return BioTransformer.transform(source, transformation);
    }


    /**
     * Applies all known biotransformation on a given Molecular formular and returns the transformed formula(s)
     *
     * @param source source formula for transformation
     * @return transformed MolecularFormulas
     */
    public List<MolecularFormula> bioTransform(MolecularFormula source) {
        return BioTransformer.getAllTransformations(source);
    }

    /**
     * Simulates an isotope pattern for the given molecular formula and the chosen ionization
     *
     * @param compound neutral molecular formula
     * @param ion      ionization mode (might be a Charge)
     * @return spectrum containing the theoretical isotope pattern of this compound
     */
    public Spectrum<Peak> simulateIsotopePattern(MolecularFormula compound, Ionization ion) {
        return getMs1Analyzer().getPatternGenerator().simulatePattern(compound, ion);
    }

    /**
     * Simulates an isotope pattern for the given molecular formula and the chosen ionization
     *
     * @param compound      neutral molecular formula
     * @param ion           ionization mode (might be a Charge)
     * @param numberOfPeaks number of peaks in simulated pattern
     * @return spectrum containing the theoretical isotope pattern of this compound
     */
    public Spectrum<Peak> simulateIsotopePattern(MolecularFormula compound, Ionization ion, int numberOfPeaks) {
        IsotopePatternGenerator gen = getMs1Analyzer().getPatternGenerator();
        gen.setMaximalNumberOfPeaks(numberOfPeaks);
        return gen.simulatePattern(compound, ion);
    }

    /**
     * depending on the isotope pattern policy this method is
     * - omit: doing nothing
     * - scoring: adds all isotope pattern candidates with their score into the hashmap
     * - filtering: adds only a subset of isotope pattern candidates with good scores into the hashmap
     *
     * @return score of the best isotope candidate
     */
    private double filterCandidateList
    (List<IsotopePattern> candidates, HashMap<MolecularFormula, IsotopePattern> formulas, IsotopePatternHandling
            handling) {
        if (handling == IsotopePatternHandling.omit) {
            return 0d;
        }
        if (candidates.size() == 0) return 0d;
        {
            double opt = Double.NEGATIVE_INFINITY;
            final SupportVectorMolecularFormulaScorer formulaScorer = new SupportVectorMolecularFormulaScorer();
            for (IsotopePattern p : candidates) {
                opt = Math.max(opt, p.getScore() + formulaScorer.score(p.getCandidate()));
            }
            if (opt < 0) {
                for (IsotopePattern p : candidates)
                    formulas.put(p.getCandidate(), new IsotopePattern(p.getCandidate(), 0d, p.getPattern()));
                return candidates.get(0).getScore();
            }
        }
        final double optscore = candidates.get(0).getScore();
        if (!handling.isFiltering()) {
            for (IsotopePattern p : candidates) formulas.put(p.getCandidate(), p);
            return candidates.get(0).getScore();
        }
        formulas.put(candidates.get(0).getCandidate(), candidates.get(0));
        int n = 1;
        for (; n < candidates.size(); ++n) {
            final double score = candidates.get(n).getScore();
            final double prev = candidates.get(n - 1).getScore();
            if (((optscore - score) > 5) && (score <= 0 || score / optscore < 0.5 || score / prev < 0.5)) break;
        }
        for (int i = 0; i < n; ++i) formulas.put(candidates.get(i).getCandidate(), candidates.get(i));
        return optscore;
    }

    //todo this is from ms2datapreprocessor. not sure if this is really neeed -> changed to the version below
    /*private SimpleSpectrum extractIsotopePattern(Ms2Experiment experiment) {

        Ms2Experiment experiment2;
        if (experiment.getMergedMs1Spectrum()!=null) experiment2 = experiment;
        else {
            experiment2 = new MutableMs2Experiment(experiment);
            if (experiment2.getMs1Spectra().size() > 0) {
                ((MutableMs2Experiment)experiment2).setMergedMs1Spectrum(Spectrums.mergeSpectra(experiment2.<Spectrum<Peak>>getMs1Spectra()));
            } else {
                return new SimpleSpectrum(new double[0], new double[0]);
            }

        }

        return getMs1Analyzer().extractPattern(experiment2, experiment2.getIonMass());
    }
*/
    public ExtractedIsotopePattern extractedIsotopePattern(@NotNull ProcessedInput pinput) {
        ExtractedIsotopePattern pat = pinput.getAnnotation(ExtractedIsotopePattern.class, null);
        if (pat == null) {
            final MutableMs2Experiment experiment = pinput.getExperimentInformation();
            pat = extractedIsotopePattern(experiment);
            pinput.setAnnotation(ExtractedIsotopePattern.class, pat);
        }
        return pat;
    }

    public ExtractedIsotopePattern extractedIsotopePattern(@NotNull Ms2Experiment experiment) {
        MS1MassDeviation ms1dev = experiment.getAnnotationOrDefault(MS1MassDeviation.class);

        SimpleSpectrum mergedMS1Pattern = null;
        if (experiment.getMergedMs1Spectrum() != null) {
            mergedMS1Pattern = Spectrums.extractIsotopePattern(experiment.getMergedMs1Spectrum(), ms1dev, experiment.getIonMass(), experiment.getPrecursorIonType().getCharge(), true);
        }

        SimpleSpectrum ms1SpectraPattern = null;
        if (experiment.getMs1Spectra().size() > 0) {
            ms1SpectraPattern = Spectrums.extractIsotopePatternFromMultipleSpectra(experiment.getMs1Spectra(), ms1dev, experiment.getIonMass(), experiment.getPrecursorIonType().getCharge(), true, 0.66);
        }


        if (mergedMS1Pattern != null) {
            if (ms1SpectraPattern != null) {
                final SimpleSpectrum extendedPattern = Spectrums.extendPattern(mergedMS1Pattern, ms1SpectraPattern, 0.02);
                return new ExtractedIsotopePattern(extendedPattern);
            } else {
                return new ExtractedIsotopePattern(mergedMS1Pattern);
            }
        } else if (ms1SpectraPattern != null) {
            return new ExtractedIsotopePattern(ms1SpectraPattern);
        }

        return null;
    }

    private SimpleSpectrum mergeMs1Spec(ProcessedInput pinput) {
        final MutableMs2Experiment experiment = pinput.getExperimentInformation();
        if (experiment.getMergedMs1Spectrum() != null) return experiment.getMergedMs1Spectrum();
        else if (experiment.getMs1Spectra().size() > 0) {
            experiment.setMergedMs1Spectrum(Spectrums.mergeSpectra(experiment.<Spectrum<Peak>>getMs1Spectra()));
            return experiment.getMergedMs1Spectrum();
        } else return new SimpleSpectrum(new double[0], new double[0]);
    }

    /*
    TODO: We have to move this at some point back into the FragmentationPatternAnalysis pipeline -_-
     */
    protected boolean performMs1Analysis(AbstractTreeComputationInstance instance) {
        final ProcessedInput input = instance.validateInput();
        IsotopeSettings isotopeSettings = input.getAnnotation(IsotopeSettings.class);
        FormulaSettings formulaSettings = input.getAnnotation(FormulaSettings.class);

        if (!isotopeSettings.isFiltering() && !isotopeSettings.isScoring() && !formulaSettings.isElementDetectionEnabled())
            return false;

        final ExtractedIsotopePattern pattern = extractedIsotopePattern(input);
        if (pattern == null || !pattern.hasPatternWithAtLeastTwoPeaks()) {
            input.setAnnotation(FormulaConstraints.class, formulaSettings.getEnforcedAlphabet().getExtendedConstraints(formulaSettings.getFallbackAlphabet()));
            return false; // we cannot do any analysis without isotope information
        }
        // step 1: automatic element detection
        performAutomaticElementDetection(input, pattern.getPattern());

        // step 2: adduct type search
        PossibleIonModes pim = input.getAnnotation(PossibleIonModes.class, null);
        IonGuessingMode gm = input.getAnnotationOrDefault(IonGuessingMode.class);
        if (pim == null)
            detectPossibleIonModesFromMs1(input);
        else if (gm.isEnabled()) {
            detectPossibleIonModesFromMs1(input, pim.getIonModesAsPrecursorIonType().toArray(new PrecursorIonType[0]));
        }
        // step 3: Isotope pattern analysis
        if (!isotopeSettings.isScoring())
            return false;
        final DecompositionList decompositions = instance.precompute().getAnnotationOrThrow(DecompositionList.class);
        final IsotopePatternAnalysis an = getMs1Analyzer();
        for (Map.Entry<Ionization, List<MolecularFormula>> entry : decompositions.getFormulasPerIonMode().entrySet()) {
            for (IsotopePattern pat : an.scoreFormulas(pattern.getPattern(), entry.getValue(), input.getExperimentInformation(), PrecursorIonType.getPrecursorIonType(entry.getKey()))) {
                pattern.getExplanations().put(pat.getCandidate(), pat);
            }
        }
        int isoPeaks = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        boolean doFilter = false;
        double scoreThresholdForFiltering = 0d;
        for (IsotopePattern pat : pattern.getExplanations().values()) {
            maxScore = Math.max(pat.getScore(), maxScore);
            final int numberOfIsoPeaks = pat.getPattern().size() - 1;
            if (pat.getScore() >= 2 * numberOfIsoPeaks) {
                isoPeaks = Math.max(pat.getPattern().size(), isoPeaks);
                scoreThresholdForFiltering = isoPeaks * 1d;
                doFilter = true;
            }
        }
        //doFilter = doFilter && pattern.getExplanations().size() > 100;
        // step 3: apply filtering and/or scoring
        if (doFilter && maxScore >= scoreThresholdForFiltering) {
            if (isotopeSettings.isFiltering()) {
                //final Iterator<Map.Entry<MolecularFormula, IsotopePattern>> iter = pattern.getExplanations().entrySet().iterator();
                final Iterator<Decomposition> iter = decompositions.getDecompositions().iterator();
                while (iter.hasNext()) {
                    final Decomposition d = iter.next();
                    final IsotopePattern p = pattern.getExplanations().get(d.getCandidate());
                    if (p == null || p.getScore() < scoreThresholdForFiltering) {
                        iter.remove();
                    }
                }
            }
        }
        final Iterator<Map.Entry<MolecularFormula, IsotopePattern>> iter = pattern.getExplanations().entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<MolecularFormula, IsotopePattern> val = iter.next();
            val.setValue(val.getValue().withScore(isotopeSettings.isScoring() ? Math.max(val.getValue().getScore()*isotopeSettings.getMultiplier(), 0d) : 0d));
        }

        return true;
    }

    private void performAutomaticElementDetection(ProcessedInput input, SimpleSpectrum extractedPattern) {
        final FormulaSettings settings = input.getAnnotationOrDefault(FormulaSettings.class);
        if (settings.isElementDetectionEnabled()) {
            final ElementPredictor predictor = getElementPrediction();
            final FormulaConstraints constraints = settings.getEnforcedAlphabet().getExtendedConstraints(predictor.predictConstraints(extractedPattern).intersection(new FormulaConstraints(settings.getAutoDetectionAlphabet())));
            input.setAnnotation(FormulaConstraints.class, constraints);
        }
    }

    public Sirius.SiriusIdentificationJob makeIdentificationJob(final Ms2Experiment experiment) {
        return makeIdentificationJob(experiment, true);
    }

    public Sirius.SiriusIdentificationJob makeIdentificationJob(final Ms2Experiment experiment, final boolean beautifyTrees) {
        return new SiriusIdentificationJob(experiment, beautifyTrees);
    }

    public GuessIonizationFromMs1Result guessIonization(Ms2Experiment experiment, PrecursorIonType[] ionTypes) {
        return ionGuessing.guessIonization(experiment, ionTypes);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////CLASSES/////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public class SiriusIdentificationJob extends BasicMasterJJob<List<IdentificationResult>> {
        private final Ms2Experiment experiment;
        private final boolean beautifyTrees;

        public SiriusIdentificationJob(Ms2Experiment experiment, boolean beautifyTrees) {
            super(JobType.CPU);
            this.experiment = experiment;
            this.beautifyTrees = beautifyTrees;
        }

        @Override
        protected List<IdentificationResult> compute() throws Exception {
            final AbstractTreeComputationInstance instance = getTreeComputationImplementation(getMs2Analyzer(), experiment, experiment.getAnnotation(NumberOfCandidates.class, () -> NumberOfCandidates.ONE).value, experiment.getAnnotation(NumberOfCandidatesPerIon.class, () -> NumberOfCandidatesPerIon.MIN_VALUE).value);
            instance.addPropertyChangeListener(JobProgressEvent.JOB_PROGRESS_EVENT, evt -> updateProgress(0, 105, (int) evt.getNewValue()));
            final ProcessedInput pinput = instance.validateInput();
            performMs1Analysis(instance);
            submitSubJob(instance);
            AbstractTreeComputationInstance.FinalResult fr = instance.awaitResult();

            List<IdentificationResult> r = createIdentificationResults(fr, instance);//postprocess results
            return r;
        }

        private List<IdentificationResult> createIdentificationResults(AbstractTreeComputationInstance.FinalResult fr, AbstractTreeComputationInstance computationInstance) {
            addScoreThresholdOnUnconsideredCandidates(fr, computationInstance.precompute());

            final List<IdentificationResult> irs = new ArrayList<>();
            int k = 0;
            for (FTree tree : fr.getResults()) {
                IdentificationResult result = new IdentificationResult(tree, ++k);
                irs.add(result);

                //beautify tree (try to explain more peaks)
                if (beautifyTrees)
                    beautifyTree(this, result, experiment, experiment.getAnnotation(ForbidRecalibration.class, () -> ForbidRecalibration.ALLOWED) == ForbidRecalibration.ALLOWED);
                final ProcessedInput processedInput = result.getStandardTree().getAnnotationOrNull(ProcessedInput.class);
                if (processedInput != null)
                    result.setAnnotation(Ms2Experiment.class, processedInput.getExperimentInformation());
                else result.setAnnotation(Ms2Experiment.class, experiment);
            }
            return irs;
        }


        public Ms2Experiment getExperiment() {
            return experiment;
        }
    }


}
