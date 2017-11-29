/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
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
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.ms.ft.UnregardedCandidatesUpperBound;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Sirius {

    private static final double MAX_TREESIZE_INCREASE = 3d;
    private static final double TREE_SIZE_INCREASE = 1d;
    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 15;
    private static final double MIN_EXPLAINED_INTENSITY = 0.7d;
    private static final int MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY = 5;

    private static final double MINIMAL_SCORE_FOR_APPLY_FILTER = 10d;
    private static final double ISOTOPE_SCORE_FILTER_THRESHOLD = 2.5d;

    protected Profile profile;
    protected ElementPredictor elementPrediction;
    protected Progress progress;
    protected PeriodicTable table;
    protected boolean autoIonMode;
    protected JobManager jobManager;

    //public final static String ISOTOPE_SCORE = "isotope";

    public Sirius(String profileName) throws IOException {
        profile = new Profile(profileName);
        loadMeasurementProfile();
        this.progress = new Progress.Quiet();
    }

    public Sirius() {
        try {
            profile = new Profile("default");
            loadMeasurementProfile();
            this.progress = new Progress.Quiet();
        } catch (IOException e) { // should be in classpath
            throw new RuntimeException(e);
        }
    }

    // TODO: add progress bar
    public BasicJJob<List<IdentificationResult>> makeIdentificationJob(final Ms2Experiment experiment, final int numberOfResultsToReport) {
        return new BasicJJob<List<IdentificationResult>>() {
            @Override
            protected List<IdentificationResult> compute() throws Exception {
                return identify(experiment, numberOfResultsToReport);
            }
        };
    }

    // TODO: add progress bar
    public BasicJJob<IdentificationResult> makeTreeComputationJob(final Ms2Experiment experiment, final MolecularFormula formula) {
        return new BasicJJob<IdentificationResult>() {
            @Override
            protected IdentificationResult compute() throws Exception {
                return Sirius.this.compute(experiment, formula);
            }
        };
    }

    /**
     * set new constraints for the molecular formulas that should be considered by Sirius
     * constraints consist of a set of allowed elements together with upperbounds for this elements
     * You can set constraints as String with a format like "CHNOP[7]" where each bracket contains the upperbound
     * for the preceeding element. Elements without upperbound are unbounded.
     * <p>
     * The elemens CHNOPS will always be contained in the element set. However, you can change their upperbound which
     * is unbounded by default.
     *
     *@Deprecated Formula Constraits are now set per input instance via {@link #setFormulaConstraints(MutableMs2Experiment, FormulaConstraints)}
     * @param newConstraints
     */
    @Deprecated
    public void setFormulaConstraints(String newConstraints) {
        setFormulaConstraints(new FormulaConstraints(newConstraints));
    }

    /**
     * set new constraints for the molecular formulas that should be considered by Sirius
     * constraints consist of a set of allowed elements together with upperbounds for this elements
     * <p>
     * The elemens CHNOPS will always be contained in the element set. However, you can change their upperbound which
     * is unbounded by default.
     *
     * @Deprecated Formula Constraits are now set per input instance via {@link #setFormulaConstraints(MutableMs2Experiment, FormulaConstraints)}
     * @param constraints
     */
    @Deprecated
    public void setFormulaConstraints(FormulaConstraints constraints) {
        final PeriodicTable tb = PeriodicTable.getInstance();
        final Element[] chnop = new Element[]{tb.getByName("C"), tb.getByName("H"), tb.getByName("N"), tb.getByName("O"), tb.getByName("P")};
        final FormulaConstraints fc = constraints.getExtendedConstraints(chnop);
        getMs1Analyzer().getDefaultProfile().setFormulaConstraints(fc);
        getMs2Analyzer().getDefaultProfile().setFormulaConstraints(fc);
    }

    /**
     * parses a file and return an iterator over all MS/MS experiments contained in this file
     * An experiment consists of all MS and MS/MS spectra belonging to one feature (=compound).
     * <p>
     * Supported file formats are .ms and .mgf
     * <p>
     * The returned iterator supports the close method to close the input stream. The stream is closed automatically,
     * after the last element is iterated. However, it is recommendet to use the following syntax (since java 7):
     * <p>
     * <pre>
     * {@code
     * try ( CloseableIterator<Ms2Experiment> iter = sirius.parse(myfile) ) {
     *   while (iter.hasNext()) {
     *      Ms2Experiment experiment = iter.next();
     *      // ...
     *   }
     * }}
     * </pre>
     *
     * @param file
     * @return
     * @throws IOException
     */
    public CloseableIterator<Ms2Experiment> parseExperiment(File file) throws IOException {
        return new MsExperimentParser().getParser(file).parseFromFileIterator(file);
    }

    /**
     * @Deprecated Progress handling should be done via Job API
     */
    @Deprecated
    public Progress getProgress() {
        return progress;
    }

    /**
     * @Deprecated Progress handling should be done via Job API
     */
    @Deprecated
    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public FragmentationPatternAnalysis getMs2Analyzer() {
        return profile.fragmentationPatternAnalysis;
    }

    public IsotopePatternAnalysis getMs1Analyzer() {
        return profile.isotopePatternAnalysis;
    }

    private void loadMeasurementProfile() {
        this.table = PeriodicTable.getInstance();
        // make mutable
        profile.fragmentationPatternAnalysis.setDefaultProfile(new MutableMeasurementProfile(profile.fragmentationPatternAnalysis.getDefaultProfile()));
        profile.isotopePatternAnalysis.setDefaultProfile(new MutableMeasurementProfile(profile.isotopePatternAnalysis.getDefaultProfile()));
        this.elementPrediction = null;
        this.autoIonMode = false;
    }

    public ElementPredictor getElementPrediction() {
        if (elementPrediction == null) {
            /*
            DNNElementPredictor defaultPredictor = new DNNElementPredictor();
            defaultPredictor.setThreshold(0.05);
            defaultPredictor.setThreshold("S", 0.1);
            defaultPredictor.setThreshold("Si", 0.8);
            elementPrediction = defaultPredictor;
            */
            DNNRegressionPredictor defaultPredictor = new DNNRegressionPredictor();
            defaultPredictor.disableSilicon();
            elementPrediction = defaultPredictor;
        }
        return elementPrediction;
    }

    public void setElementPrediction(ElementPredictor elementPrediction) {
        this.elementPrediction = elementPrediction;
    }

    public boolean isAutoIonMode() {
        return autoIonMode;
    }

    public void setAutoIonMode(boolean autoIonMode) {
        this.autoIonMode = autoIonMode;
    }

    /**
     * try to guess ionization from MS1. multiple  suggestions possible. In doubt [M]+ is ignored (cannot distinguish from isotope pattern)!
     *
     * @param experiment
     * @param candidateIonizations array of possible ionizations (lots of different adducts very likely make no sense!)
     * @return
     */
    public PrecursorIonType[] guessIonization(Ms2Experiment experiment, PrecursorIonType[] candidateIonizations) {
        Spectrum<Peak> spec = experiment.getMergedMs1Spectrum();
        if (spec == null) spec = experiment.getMs1Spectra().get(0);

        SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(spec);
        Spectrums.normalizeToMax(mutableSpectrum, 100d);
        Spectrums.applyBaseline(mutableSpectrum, 1d);

        PrecursorIonType[] ionType = Spectrums.guessIonization(mutableSpectrum, experiment.getIonMass(), profile.fragmentationPatternAnalysis.getDefaultProfile().getAllowedMassDeviation(), candidateIonizations);
        return ionType;
    }

    /**
     * Identify the molecular formula of the measured compound using the provided MS and MSMS data
     *
     * @param uexperiment input data
     * @return the top tree
     */
    public IdentificationResult identify(Ms2Experiment uexperiment) {
        return identify(uexperiment, 1).get(0);
    }

    /**
     * Identify the molecular formula of the measured compound using the provided MS and MSMS data
     *
     * @param uexperiment input data
     * @param numberOfCandidates number of top candidates to return
     * @return a list of identified molecular formulas together with their tree
     */
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates) {
        final TreeComputationInstance instance = new TreeComputationInstance(jobManager, getMs2Analyzer(), uexperiment, numberOfCandidates);
        final ProcessedInput pinput = instance.validateInput();
        performMs1Analysis(instance, IsotopePatternHandling.both);
        jobManager.submitSubJob(instance);
        TreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = new ArrayList<>();
        int k=0;
        for (FTree tree : fr.getResults()) {
            irs.add(new IdentificationResult(tree, ++k));
        }
        return irs;
    }
    /**
     * Identify the molecular formula of the measured compound by combining an isotope pattern analysis on MS data with a fragmentation pattern analysis on MS/MS data
     *
     * @param uexperiment        input data
     * @param numberOfCandidates number of candidates to output
     * @param recalibrating      true if spectra should be recalibrated during tree computation
     * @param deisotope          set this to 'omit' to ignore isotope pattern, 'filter' to use it for selecting molecular formula candidates or 'score' to rerank the candidates according to their isotope pattern
     * @param whiteList          restrict the analysis to this subset of molecular formulas. If this set is empty, consider all possible molecular formulas
     * @return a list of identified molecular formulas together with their tree
     */
    @Deprecated
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, Set<MolecularFormula> whiteList) {
        final TreeComputationInstance instance = new TreeComputationInstance(jobManager, getMs2Analyzer(), uexperiment, numberOfCandidates);
        final ProcessedInput pinput = instance.validateInput();
        pinput.setAnnotation(ForbidRecalibration.class, recalibrating ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
        pinput.setAnnotation(Whiteset.class, new Whiteset(whiteList));
        performMs1Analysis(instance, deisotope);
        jobManager.submitSubJob(instance);
        TreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = new ArrayList<>();
        int k=0;
        for (FTree tree : fr.getResults()) {
            irs.add(new IdentificationResult(tree, ++k));
        }
        return irs;
    }

    private void addScoreThresholdOnUnconsideredCandidates(List<IdentificationResult> identificationResults, int numberOfCandidates) {
        double lowestConsideredCandidatesScore = Double.POSITIVE_INFINITY;
        int numberOfUnconsideredCandidates = numberOfCandidates - identificationResults.size();
        for (IdentificationResult identificationResult : identificationResults) {
            final double score = identificationResult.getScore();
            if (score < lowestConsideredCandidatesScore) lowestConsideredCandidatesScore = score;
        }

        for (IdentificationResult identificationResult : identificationResults) {
            FTree tree = identificationResult.getStandardTree();
            FTree beautifulTree = identificationResult.getBeautifulTree();
            UnregardedCandidatesUpperBound unregardedCandidatesUpperBound = new UnregardedCandidatesUpperBound(numberOfUnconsideredCandidates, lowestConsideredCandidatesScore);

            tree.addAnnotation(UnregardedCandidatesUpperBound.class, unregardedCandidatesUpperBound);
            if (beautifulTree != null)
                beautifulTree.addAnnotation(UnregardedCandidatesUpperBound.class, unregardedCandidatesUpperBound);
        }
    }

    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope) {
        return identify(uexperiment, numberOfCandidates, recalibrating, deisotope, (FormulaConstraints)null);
    }


    /**
     * Identify the molecular formula of the measured compound by combining an isotope pattern analysis on MS data with a fragmentation pattern analysis on MS/MS data
     *
     * @param uexperiment        input data
     * @param numberOfCandidates number of candidates to output
     * @param recalibrating      true if spectra should be recalibrated during tree computation
     * @param deisotope          set this to 'omit' to ignore isotope pattern, 'filter' to use it for selecting molecular formula candidates or 'score' to rerank the candidates according to their isotope pattern
     * @param formulaConstraints use if specific constraints on the molecular formulas shall be imposed (may be null)
     * @return a list of identified molecular formulas together with their tree
     */
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, FormulaConstraints formulaConstraints) {
        final TreeComputationInstance instance = new TreeComputationInstance(jobManager, getMs2Analyzer(), uexperiment, numberOfCandidates);
        final ProcessedInput pinput = instance.validateInput();
        pinput.setAnnotation(ForbidRecalibration.class, recalibrating ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
        if (formulaConstraints!=null) pinput.getMeasurementProfile().setFormulaConstraints(formulaConstraints);
        performMs1Analysis(instance, deisotope);
        jobManager.submitSubJob(instance);
        TreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = new ArrayList<>();
        int k=0;
        for (FTree tree : fr.getResults()) {
            irs.add(new IdentificationResult(tree, ++k));
        }
        return irs;
    }

    public FormulaConstraints predictElementsFromMs1(Ms2Experiment experiment) {
        final SimpleSpectrum pattern = getMs1Analyzer().extractPattern(experiment, experiment.getIonMass());
        if (pattern==null) return null;
        return getElementPrediction().predictConstraints(pattern);
    }

    public IdentificationResult compute(Ms2Experiment experiment, MolecularFormula formula) {
        return compute(experiment, formula, true);
    }

    /**
     * Compute a fragmentation tree for the given MS/MS data using the given neutral molecular formula as explanation for the measured compound
     *
     * @param experiment    input data
     * @param formula       neutral molecular formula of the measured compound
     * @param recalibrating true if spectra should be recalibrated during tree computation
     * @return A single instance of IdentificationResult containing the computed fragmentation tree
     */
    public IdentificationResult compute(Ms2Experiment experiment, MolecularFormula formula, boolean recalibrating) {
        final TreeComputationInstance instance = new TreeComputationInstance(jobManager, getMs2Analyzer(), experiment, 1);
        final ProcessedInput pinput = instance.validateInput();
        pinput.setAnnotation(Whiteset.class, new Whiteset(new HashSet<MolecularFormula>(Arrays.asList(formula))));
        pinput.setAnnotation(ForbidRecalibration.class, recalibrating ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
        return new IdentificationResult(instance.takeResult().getResults().get(0), 1);

    }


    public boolean beautifyTree(IdentificationResult result, Ms2Experiment experiment){
        return beautifyTree(result, experiment, true);
    }

    /**
     * compute and set the beautiful version of the {@link IdentificationResult}s {@link FTree}.
     * Aka: try to find a {@link FTree} with the same root molecular formula which explains the desired amount of the spectrum - if necessary by increasing the tree size scorer.
     * @param result
     * @param experiment
     * @return true if a beautiful tree was found
     */
    public boolean beautifyTree(IdentificationResult result, Ms2Experiment experiment, boolean recalibrating){
        if (result.getBeautifulTree()!=null) return true;
        FTree beautifulTree = beautifyTree(result.getStandardTree(), experiment, recalibrating);
        if (beautifulTree!=null){
            result.setBeautifulTree(beautifulTree);
            return true;
        }
        return false;
    }

    public FTree beautifyTree(FTree tree, Ms2Experiment experiment, boolean recalibrating){
        final IdentificationResult ir = compute(experiment, tree.getRoot().getFormula(), recalibrating);
        return ir.getRawTree();
    }


    ////////////////////////////////////////////////////////////////////////////////

    public MutableMs2Experiment makeMutable(Ms2Experiment experiment) {
        if (experiment instanceof MutableMs2Experiment) return (MutableMs2Experiment) experiment;
        else return new MutableMs2Experiment(experiment);
    }

    public void setAllowedIonModes(Ms2Experiment experiment, Ionization... ionModes) {
        final PossibleAdductTypes pa = new PossibleAdductTypes();
        for (Ionization ion : ionModes) {
            pa.add(ion, 1d);
        }
        experiment.setAnnotation(PossibleAdductTypes.class, pa);
    }

    public void setIonModeWithProbability(Ms2Experiment experiment, Ionization ion, double probability) {
        final PossibleAdductTypes pa = experiment.getAnnotation(PossibleAdductTypes.class, new PossibleAdductTypes());
        pa.add(ion, probability);
        experiment.setAnnotation(PossibleAdductTypes.class, pa);
    }

    public void setFormulaSearchList(Ms2Experiment experiment, MolecularFormula... formulas) {
        setFormulaSearchList(experiment, Arrays.asList(formulas));
    }

    public void setFormulaSearchList(Ms2Experiment experiment, Iterable<MolecularFormula> formulas) {
        final HashSet<MolecularFormula> fs = new HashSet<MolecularFormula>();
        for (MolecularFormula f : formulas) fs.add(f);
        final Whiteset whiteset = new Whiteset(fs);
        experiment.setAnnotation(Whiteset.class, whiteset);
    }

    public void enableRecalibration(MutableMs2Experiment experiment, boolean enabled) {
        experiment.setAnnotation(ForbidRecalibration.class, enabled ? ForbidRecalibration.ALLOWED : ForbidRecalibration.FORBIDDEN);
    }

    public void setAutomaticElementDetectionFor(MutableMs2Experiment experiment, Element elements) {
        FormulaSettings current = experiment.getAnnotation(FormulaSettings.class, FormulaSettings.defaultWithMs2Only());
        experiment.setAnnotation(FormulaSettings.class, current.withoutAutoDetect().autoDetect(elements));
    }

    public void setFormulaConstraints(MutableMs2Experiment experiment, FormulaConstraints constraints) {
        FormulaSettings current = experiment.getAnnotation(FormulaSettings.class, FormulaSettings.defaultWithMs2Only());
        experiment.setAnnotation(FormulaSettings.class, current.withConstraints(constraints));
    }

    public void enableAutomaticElementDetection(MutableMs2Experiment experiment, boolean enabled) {
        FormulaSettings current = experiment.getAnnotation(FormulaSettings.class, FormulaSettings.defaultWithMs2Only());
        if (enabled) {
            experiment.setAnnotation(FormulaSettings.class, current.autoDetect(getElementPrediction().getChemicalAlphabet().getElements().toArray(new Element[0])));
        } else {
            experiment.setAnnotation(FormulaSettings.class,current.withoutAutoDetect());
        }
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
    public Ms2Experiment getMs2Experiment(MolecularFormula formula, Ionization ion, Spectrum<Peak> ms1, Spectrum... ms2) {
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
    public Ms2Experiment getMs2Experiment(MolecularFormula formula, PrecursorIonType ion, Spectrum<Peak> ms1, Spectrum... ms2) {
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
    public Ms2Experiment getMs2Experiment(double parentMass, PrecursorIonType ion, Spectrum<Peak> ms1, Spectrum... ms2) {
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
        return decompose(mass, ion, constr, getMs2Analyzer().getDefaultProfile().getAllowedMassDeviation());
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
     * @param source   source formula for transformation
     * @param transformation to that will be applied to given Formula    ionization mode (might be a Charge, in which case the decomposer will enumerate the ion formulas instead of the neutral formulas)
     * @return transformed MolecularFormulas
     */
    public List<MolecularFormula> bioTransform(MolecularFormula source, BioTransformation transformation) {
        return BioTransformer.transform(source,transformation);
    }


    /**
     * Applies all known biotransformation on a given Molecular formular and returns the transformed formula(s)
     *
     * @param source   source formula for transformation
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
     * @return score of the best isotope candidate
     */
    private double filterCandidateList(List<IsotopePattern> candidates, HashMap<MolecularFormula, IsotopePattern> formulas, IsotopePatternHandling handling) {
        if (handling==IsotopePatternHandling.omit) {
            return 0d;
        }
        if (candidates.size() == 0) return 0d;
        {
            double opt=Double.NEGATIVE_INFINITY;
            final SupportVectorMolecularFormulaScorer formulaScorer = new SupportVectorMolecularFormulaScorer();
            for (IsotopePattern p : candidates) {
                opt = Math.max(opt, p.getScore() + formulaScorer.score(p.getCandidate()));
            }
            if (opt < 0) {
                for (IsotopePattern p : candidates) formulas.put(p.getCandidate(), new IsotopePattern(p.getCandidate(), 0d, p.getPattern()));
                return  candidates.get(0).getScore();
            }
        }
        final double optscore = candidates.get(0).getScore();
        if (!handling.isFiltering()) {
            for (IsotopePattern p : candidates) formulas.put(p.getCandidate(), p);
            return  candidates.get(0).getScore();
        }
        formulas.put(candidates.get(0).getCandidate(), candidates.get(0));
        int n = 1;
        for (; n < candidates.size(); ++n) {
            final double score = candidates.get(n).getScore();
            final double prev = candidates.get(n - 1).getScore();
            if (((optscore-score) > 5) && (score <= 0 || score / optscore < 0.5 || score / prev < 0.5)) break;
        }
        for (int i = 0; i < n; ++i) formulas.put(candidates.get(i).getCandidate(), candidates.get(i));
        return optscore;
    }

    private static Comparator<FTree> TREE_SCORE_COMPARATOR = new Comparator<FTree>() {
        @Override
        public int compare(FTree o1, FTree o2) {
            return Double.compare(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore(), o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
        }
    };


    private ExtractedIsotopePattern extractedIsotopePattern(ProcessedInput pinput) {
        ExtractedIsotopePattern pat = pinput.getAnnotation(ExtractedIsotopePattern.class, null);
        if (pat==null){
            final SimpleSpectrum spectrum = getMs1Analyzer().extractPattern(mergeMs1Spec(pinput), pinput.getMeasurementProfile(), pinput.getExperimentInformation().getIonMass());
            pat = new ExtractedIsotopePattern(spectrum);
            pinput.setAnnotation(ExtractedIsotopePattern.class, pat);
        }
        return pat;
    }

    private SimpleSpectrum mergeMs1Spec(ProcessedInput pinput) {
        final MutableMs2Experiment experiment = pinput.getExperimentInformation();
        if (experiment.getMergedMs1Spectrum()!=null) return experiment.getMergedMs1Spectrum();
        else if (experiment.getMs1Spectra().size()>0) {
            experiment.setMergedMs1Spectrum( Spectrums.mergeSpectra(experiment.<Spectrum<Peak>>getMs1Spectra()));
            return experiment.getMergedMs1Spectrum();
        } else return new SimpleSpectrum(new double[0], new double[0]);
    }

    /*
    TODO: We have to move this at some point back into the FragmentationPatternAnalysis pipeline -_-
     */
    protected boolean performMs1Analysis(TreeComputationInstance instance, IsotopePatternHandling handling) {
        if (handling == IsotopePatternHandling.omit) return false;
        final ProcessedInput input = instance.validateInput();
        final ExtractedIsotopePattern pattern = extractedIsotopePattern(input);
        if (!pattern.hasPatternWithAtLeastTwoPeaks())
            return false; // we cannot do any analysis without isotope information
        // step 1: automatic element detection
        performAutomaticElementDetection(input, pattern.getPattern());

        // step 2: Isotope pattern analysis
        final DecompositionList decompositions = instance.precompute().getAnnotationOrThrow(DecompositionList.class);
        final IsotopePatternAnalysis an = getMs1Analyzer();
        for (Map.Entry<Ionization, List<MolecularFormula>> entry : decompositions.getFormulasPerIonMode().entrySet()) {
            for (IsotopePattern pat : an.scoreFormulas(pattern.getPattern(), entry.getValue(), input.getExperimentInformation(), input.getMeasurementProfile(), PrecursorIonType.getPrecursorIonType(entry.getKey()))) {
                pattern.getExplanations().put(pat.getCandidate(), pat);
            }
        }
        int isoPeaks = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        for (IsotopePattern pat : pattern.getExplanations().values()) {
            maxScore = Math.max(pat.getScore(), maxScore);
            isoPeaks = Math.max(pat.getPattern().size(), isoPeaks);
        }
        // step 3: apply filtering and/or scoring
        if (maxScore >= MINIMAL_SCORE_FOR_APPLY_FILTER) {
            if (handling.isFiltering()) {
                final Iterator<Map.Entry<MolecularFormula, IsotopePattern>> iter = pattern.getExplanations().entrySet().iterator();
                while (iter.hasNext()) {
                    if (iter.next().getValue().getScore() < ((isoPeaks*ISOTOPE_SCORE_FILTER_THRESHOLD))) {
                        iter.remove();
                    }
                }
            }
        }
        final Iterator<Map.Entry<MolecularFormula, IsotopePattern>> iter = pattern.getExplanations().entrySet().iterator();
        while (iter.hasNext())  {
            final Map.Entry<MolecularFormula, IsotopePattern> val = iter.next();
            val.setValue(val.getValue().withScore(handling.isScoring() ? Math.max(val.getValue().getScore(),0d) : 0d));
        }
        return true;
    }

    private void performAutomaticElementDetection(ProcessedInput input, SimpleSpectrum extractedPattern) {
        final FormulaSettings settings = input.getAnnotation(FormulaSettings.class, FormulaSettings.defaultWithMs1());
        if (settings.isElementDetectionEnabled()) {
            final ElementPredictor predictor = getElementPrediction();
            final HashSet<Element> allowedElements = new HashSet<>(input.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet().getElements());
            final HashSet<Element> auto = settings.getAutomaticDetectionEnabled();
            allowedElements.addAll(auto);
            Iterator<Element> e = allowedElements.iterator();
            final FormulaConstraints constraints = predictor.predictConstraints(extractedPattern);
            while (e.hasNext()) {
                final Element detectable = e.next();
                if (auto.contains(detectable) && getElementPrediction().isPredictable(detectable) && constraints.getUpperbound(detectable) <= 0)
                    e.remove();
            }
            final FormulaConstraints revised = settings.getConstraints().getExtendedConstraints(allowedElements.toArray(new Element[allowedElements.size()]));
            for (Element det : auto) {
                revised.setUpperbound(det, constraints.getUpperbound(det));
            }
            input.getMeasurementProfile().setFormulaConstraints(revised);
        }
    }
}
