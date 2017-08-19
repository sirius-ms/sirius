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

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformation;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.SupportVectorMolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

public class Sirius {

    private static final double MAX_TREESIZE_INCREASE = 3d;
    private static final double TREE_SIZE_INCREASE = 1d;
    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 15;
    private static final double MIN_EXPLAINED_INTENSITY = 0.7d;
    private static final int MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY = 5;

    protected Profile profile;
    protected ElementPredictor elementPrediction;
    protected Progress progress;
    protected PeriodicTable table;
    protected boolean autoIonMode;

    public static void main(String[] args) {
        System.out.println(new ParetoDistribution.EstimateByMedian(0.0025).extimateByMedian(0.005));System.exit(0);

        final File F = new File("/home/kaidu/data/ms/demo-data/ms/Bicuculline.ms");
        try {
            Sirius s = new Sirius("qtof_fixed");
            Ms2Experiment exp = s.parseExperiment(F).next();
            final List<IdentificationResult> results = s.identify(exp, 100, true, IsotopePatternHandling.omit);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void mainElem(String[] args)  {
        Sirius s = new Sirius();
        ElementPredictor predictor = s.getElementPrediction();
        final HashMap<String, File> doof = new HashMap<>();
        for (File f : new File("/home/kaidu/data/datasets/casmi_ms1/MS1_positive/").listFiles()) {
            doof.put(f.getName().split("_")[1], f);
        }
        try {
            final List<String> lines = Files.readAllLines(new File("/home/kaidu/data/datasets/casmi_ms1/pos.csv").toPath(), Charset.forName("UTF-8"));

            for (String line : lines.subList(1,lines.size())) {
                final String[] tabs = line.split("\t");
                final String id = tabs[0];
                final MolecularFormula formula = MolecularFormula.parse(tabs[4]);
                if (doof.containsKey(id)) {
                    final File f = doof.get(id);
                    final List<String> xs = Files.readAllLines(f.toPath(), Charset.forName("UTF-8"));
                    final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(lines.size());
                    for (String l : xs) {
                        String[] vls = l.split("\\s+");
                        spec.addPeak(Double.parseDouble(vls[0]), Double.parseDouble(vls[1]));
                    }
                    final MutableMs2Experiment experiment = new MutableMs2Experiment();
                    final PrecursorIonType hplus = PrecursorIonType.getPrecursorIonType("[M+H]+");
                    experiment.setIonMass(hplus.neutralMassToPrecursorMass(formula.getMass()));
                    experiment.setMergedMs1Spectrum(new SimpleSpectrum(spec));

                    SimpleSpectrum extracted = s.getMs1Analyzer().extractPattern(experiment, experiment.getIonMass());

                    final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
                    // pick 3 peaks
                    if (extracted.size() < 3) continue;
                    buf.addPeak(extracted.getPeakAt(0));
                    buf.addPeak(extracted.getPeakAt(1));
                    buf.addPeak(extracted.getPeakAt(2));
                    // add additional 2 peaks if intensity is decreasing
                    for (int i=3; i <= Math.min(4, extracted.size()-1); ++i) {
                        if (extracted.getPeakAt(i).getIntensity()>= buf.getIntensityAt(buf.size()-1)) break;
                        buf.addPeak(extracted.getPeakAt(i));
                    }

                    extracted = new SimpleSpectrum(buf);

                    final FormulaConstraints constraints = predictor.predictConstraints(extracted);
                    if (!constraints.isSatisfied(formula)) {
                        System.out.println(f.getName() + "\t" + formula + "\t" + constraints + "\t" + experiment.getIonMass());
                        System.out.println(Spectrums.getNormalizedSpectrum(extracted, Normalization.Sum(100d)));
                    }

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public final static String ISOTOPE_SCORE = "isotope";

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

    /**
     * set new constraints for the molecular formulas that should be considered by Sirius
     * constraints consist of a set of allowed elements together with upperbounds for this elements
     * You can set constraints as String with a format like "CHNOP[7]" where each bracket contains the upperbound
     * for the preceeding element. Elements without upperbound are unbounded.
     * <p>
     * The elemens CHNOPS will always be contained in the element set. However, you can change their upperbound which
     * is unbounded by default.
     *
     * @param newConstraints
     */
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
     * @param constraints
     */
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


    public Progress getProgress() {
        return progress;
    }

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
        if (elementPrediction==null) {
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
     * @param experiment
     * @param candidateIonizations array of possible ionizations (lots of different adducts very likely make no sense!)
     * @return
     */
    public PrecursorIonType[] guessIonization(Ms2Experiment experiment, PrecursorIonType[] candidateIonizations){
        Spectrum<Peak> spec = experiment.getMergedMs1Spectrum();
        if (spec==null) spec = experiment.getMs1Spectra().get(0);

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
     * @return a list of identified molecular formulas together with their tree
     */
    public List<IdentificationResult> identify(Ms2Experiment uexperiment) {
        return identify(uexperiment, 5, true, IsotopePatternHandling.both, Collections.<MolecularFormula>emptySet());
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
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, Set<MolecularFormula> whiteList) {
        if (whiteList == null || whiteList.isEmpty()) {
            return identify(uexperiment, numberOfCandidates, recalibrating, deisotope);
        }
        // fix parentmass
        final MutableMs2Experiment exp = detectParentPeakFromWhitelist(uexperiment, whiteList);
        // split whitelist into sublists matching a certain ionization and alphabet
        final List<IonWhitelist> subsets = splitWhitelistByIonizationAndAlphabet(exp, whiteList);
        // now compute each subset separately...
        // first MS
        final HashMap<MolecularFormula, IsotopePattern> isoScores = handleIsoAnalysisWithWhitelist(deisotope, exp, subsets);
        // then MS/MS
        final DoubleEndWeightedQueue treeSet = new DoubleEndWeightedQueue(numberOfCandidates);
        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer != null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize + MAX_TREESIZE_INCREASE;
        int SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS=MIN_NUMBER_OF_EXPLAINED_PEAKS;
        try {
            final ArrayList<FTree> computedTrees = new ArrayList<FTree>();
            final FeedbackFlag feedback = new FeedbackFlag();
            outerLoop:
            while (true) {
                progress.init(whiteList.size());
                int counter = 0;
                for (IonWhitelist wl : subsets) {
                    final MutableMs2Experiment specificExp = exp.clone();
                    specificExp.setPrecursorIonType(wl.ionization);
                    final FormulaConstraints constraints = FormulaConstraints.allSubsetsOf(wl.whitelist);
                    final ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(specificExp, constraints);
                    SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS = Math.min(pinput.getMergedPeaks().size()-2, MIN_NUMBER_OF_EXPLAINED_PEAKS);
                    MultipleTreeComputation trees = profile.fragmentationPatternAnalysis.computeTrees(pinput).withoutRecalibration().inParallel();
                    if (!isoScores.isEmpty()) {
                        trees = trees.onlyWith(isoScores.keySet());
                    } else {
                        trees = trees.onlyWith(wl.whitelist);
                    }

                    final TreeIterator iter = trees.iterator(true);
                    while (iter.hasNext()) {
                        final FTree tree = iter.next();
                        if (tree != null) {
                            if (deisotope.isScoring()) addIsoScore(isoScores, tree);
                            treeSet.add(tree);
                        }
                        if (iter.lastGraph() != null) {
                            progress.update(++counter, whiteList.size(), iter.lastGraph().getRoot().getChildren(0).getFormula().toString() + " " + wl.ionization.toString(), feedback);
                        }
                        if (feedback.getFlag() == FeedbackFlag.Flag.CANCEL) {
                            return null;
                        } else if (feedback.getFlag() == FeedbackFlag.Flag.STOP) {
                            computedTrees.addAll(treeSet.getTrees());
                            break outerLoop;
                        }
                    }
                }
                progress.finished();
                boolean satisfied = treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE;
                if (!satisfied) {
                    final Iterator<FTree> treeIterator = treeSet.iterator();
                    for (int k = 0; k < MIN_NUMBER_OF_TREES_CHECK_FOR_INTENSITY; ++k) {
                        if (treeIterator.hasNext()) {
                            final FTree tree = treeIterator.next();
                            final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainedPeaks(tree);
                            if (tree.numberOfVertices() >= SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS && intensity >= MIN_EXPLAINED_INTENSITY) {
                                satisfied = true;
                                break;
                            }
                        } else break;
                    }
                }
                if (satisfied) {
                    computedTrees.addAll(treeSet.getTrees());
                    break;
                } else {
                    progress.info("Not enough peaks were explained. Repeat computation with less restricted constraints.");
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                    computedTrees.clear();
                    treeSet.clear();
                }
            }
            feedback.clear();
            // recalibrate trees
            double maximalPossibleScoreByRecalibration=0d;
            if (recalibrating) {
                // now recalibrate the trees and recompute them another time...
                progress.info("recalibrate trees");
                progress.init(computedTrees.size());
                for (int k = 0; k < computedTrees.size(); ++k) {
                    final FTree recalibratedTree = profile.fragmentationPatternAnalysis.recalibrate(computedTrees.get(k), true);
                    maximalPossibleScoreByRecalibration = addRecalibrationPenalty(computedTrees.get(k), k+1, maximalPossibleScoreByRecalibration);
                    if (deisotope.isScoring()) addIsoScore(isoScores, recalibratedTree);
                    computedTrees.set(k, recalibratedTree);
                    progress.update(k + 1, computedTrees.size(), "recalibrate " + recalibratedTree.getRoot().getFormula().toString(), feedback);
                    if (feedback.getFlag() == FeedbackFlag.Flag.CANCEL) return null;
                    else if (feedback.getFlag() == FeedbackFlag.Flag.STOP) break;
                }
                progress.finished();
            }

            Collections.sort(computedTrees, Collections.reverseOrder(TREE_SCORE_COMPARATOR));


            final ArrayList<IdentificationResult> list = new ArrayList<IdentificationResult>(numberOfCandidates);
            for (int k = 0; k < Math.min(numberOfCandidates, computedTrees.size()); ++k) {
                final FTree tree = computedTrees.get(k);
                profile.fragmentationPatternAnalysis.recalculateScores(tree);
                list.add(new IdentificationResult(tree, k + 1));
            }

            return list;
        } finally {
            treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
    }

    private double addRecalibrationPenalty(FTree result, int rank, double maximalPossibleScoreByRecalibration) {
        if (rank == 10) {
            maximalPossibleScoreByRecalibration = result.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
        } else if (rank > 10) {
            TreeScoring scoring = result.getAnnotationOrThrow(TreeScoring.class);
            if (scoring.getOverallScore() > maximalPossibleScoreByRecalibration) {
                scoring.setRecalibrationPenalty(maximalPossibleScoreByRecalibration-scoring.getOverallScore());
                scoring.setOverallScore(maximalPossibleScoreByRecalibration);
            }
        }
        return maximalPossibleScoreByRecalibration;
    }

    private List<IonWhitelist> splitWhitelistByIonizationAndAlphabet(Ms2Experiment exp, Set<MolecularFormula> whiteList){

        final HashMap<Element, Integer> elementMap = new HashMap<>();
        for (Element e : MolecularFormula.parse("CHNOPS").elementArray()) {
            elementMap.put(e, elementMap.size());
        }
        for (MolecularFormula f : whiteList) {
            f.visit(new FormulaVisitor<Object>() {
                @Override
                public Object visit(Element element, int amount) {
                    if (!elementMap.containsKey(element))
                        elementMap.put(element, elementMap.size());
                    return null;
                }
            });
        }

        // split whitelist into sublists matching a certain ionization and alphabet
        final HashMap<PrecursorIonType, IonWhitelist> subsets = new HashMap<PrecursorIonType, IonWhitelist>();
        final double absoluteError = profile.fragmentationPatternAnalysis.getDefaultProfile().getAllowedMassDeviation().absoluteFor(exp.getIonMass());
        for (MolecularFormula f : whiteList) {
            final PrecursorIonType usedIonType = getIonization(exp,  f, absoluteError);
            if (usedIonType != null) {
                IonWhitelist iw = subsets.get(usedIonType);
                if (iw == null) {
                    iw = new IonWhitelist(usedIonType);
                    subsets.put(usedIonType, iw);
                }
                iw.whitelist.add(f);
            }
        }
        // now split each of these subsets into alphabets

        final List<IonWhitelist> whitelists = new ArrayList<>();
        final HashMap<BitSet, IonWhitelist> subsets2 = new HashMap<>();
        final BitSet defaultSet = new BitSet();
        for (Element e : MolecularFormula.parse("CHNOPS").elementArray()) {
            defaultSet.set(elementMap.get(e));
        }
        for (IonWhitelist list : subsets.values()) {
            subsets2.clear();

            for (MolecularFormula f : list.whitelist) {
                final BitSet elems = (BitSet)defaultSet.clone();
                f.visit(new FormulaVisitor<Object>() {
                    @Override
                    public Object visit(Element element, int amount) {
                        elems.set(elementMap.get(element));
                        return null;
                    }
                });
                IonWhitelist iw = subsets2.get(elems);
                if (iw == null) {
                    iw = new IonWhitelist(list.ionization);
                    subsets2.put(elems, iw);
                }
                iw.whitelist.add(f);
            }
            for (IonWhitelist iw : subsets2.values()) {
                whitelists.add(iw);
            }
        }
        return whitelists;
    }

    public List<IdentificationResult> identifyByIsotopePattern(Ms2Experiment experiment, int numberOfCandidates) {
        return identifyByIsotopePattern(experiment, numberOfCandidates, null);
    }


    public List<IdentificationResult> identifyByIsotopePattern(Ms2Experiment experiment, int numberOfCandidates, Set<MolecularFormula> whiteList) {
        final List<IsotopePattern> patterns;
        if (whiteList == null || whiteList.isEmpty()) {
            patterns = profile.isotopePatternAnalysis.deisotope(experiment, profile.isotopePatternAnalysis.getDefaultProfile());
        } else {
            patterns = new ArrayList<>();
            // fix parentmass
            final MutableMs2Experiment exp = detectParentPeakFromWhitelist(experiment, whiteList);
            // split whitelist into sublists matching a certain ionization
            final List<IonWhitelist> subsets = splitWhitelistByIonizationAndAlphabet(exp, whiteList);

            final PrecursorIonType before = exp.getPrecursorIonType();
            try {
                for (IonWhitelist wl : subsets) {
                    final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(wl.whitelist);
                    exp.setPrecursorIonType(wl.ionization);
                    patterns.addAll(profile.isotopePatternAnalysis.deisotope(exp, profile.isotopePatternAnalysis.getDefaultProfile(), formulas));
                }
            } finally {
                exp.setPrecursorIonType(before);
            }
        }
        Collections.sort(patterns, Collections.<IsotopePattern>reverseOrder());

        final double absoluteError = profile.fragmentationPatternAnalysis.getDefaultProfile().getAllowedMassDeviation().absoluteFor(experiment.getIonMass());
        final ArrayList<IdentificationResult> list = new ArrayList<IdentificationResult>(numberOfCandidates);
        getProgress().info("Perform isotope pattern analysis:");
        for (int i = 0; i < Math.min(numberOfCandidates, patterns.size()); ++i) {
            final IsotopePattern pattern = patterns.get(i);
            getProgress().info("1.) ");
            final TreeScoring scoring = new TreeScoring();
            scoring.setOverallScore(0);
            scoring.addAdditionalScore(ISOTOPE_SCORE, pattern.getScore());
            final FTree dummyTree = new FTree(experiment.getPrecursorIonType().neutralMoleculeToMeasuredNeutralMolecule(pattern.getCandidate()));
            dummyTree.setAnnotation(TreeScoring.class, scoring);
            dummyTree.getOrCreateLossAnnotation(Score.class);
            dummyTree.getOrCreateFragmentAnnotation(Score.class);
            dummyTree.addAnnotation(PrecursorIonType.class, getIonization(experiment, pattern.getCandidate(), absoluteError));
            dummyTree.addAnnotation(IsotopePattern.class, pattern);
            dummyTree.addFragmentAnnotation(AnnotatedPeak.class);
            dummyTree.addFragmentAnnotation(Peak.class);
            Peak parent = new Peak(pattern.getPattern().getPeakAt(0));
            dummyTree.getFragmentAnnotationOrNull(Peak.class).set(dummyTree.getRoot(), parent);
            dummyTree.getFragmentAnnotationOrNull(AnnotatedPeak.class).set(dummyTree.getRoot(), new AnnotatedPeak(pattern.getCandidate(), pattern.getMonoisotopicMass(), pattern.getMonoisotopicMass(), parent.getIntensity(), experiment.getPrecursorIonType().getIonization(), new Peak[]{parent}, new CollisionEnergy[]{CollisionEnergy.none()}));
            list.add(new IdentificationResult(dummyTree, i + 1));
        }

        return list;
    }

    private PrecursorIonType getIonization(Ms2Experiment exp,  MolecularFormula mf, double absoluteError){
        final PrecursorIonType usedIonType;
        if (exp.getPrecursorIonType().isIonizationUnknown()) {
            final double modification = exp.getIonMass() - mf.getMass();
            usedIonType = PeriodicTable.getInstance().ionByMass(modification, absoluteError, exp.getPrecursorIonType().getCharge());
        } else if (Math.abs(exp.getPrecursorIonType().precursorMassToNeutralMass(exp.getIonMass()) - mf.getMass()) <= absoluteError) {
            usedIonType = exp.getPrecursorIonType();
        } else usedIonType = null;
        return usedIonType;
    }

    private HashMap<MolecularFormula, IsotopePattern> handleIsoAnalysisWithWhitelist(IsotopePatternHandling deisotope, MutableMs2Experiment exp, List<IonWhitelist> subsets) {
        final HashMap<MolecularFormula, IsotopePattern> isoScores = new HashMap<>();
        if (deisotope != IsotopePatternHandling.omit) {
            final List<IsotopePattern> patterns = new ArrayList<>();
            double bestScore = 0d;
            final PrecursorIonType before = exp.getPrecursorIonType();
            try {
                for (IonWhitelist wl : subsets) {
                    final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>(wl.whitelist);
                    exp.setPrecursorIonType(wl.ionization);
                    patterns.addAll(profile.isotopePatternAnalysis.deisotope(exp, profile.isotopePatternAnalysis.getDefaultProfile(), formulas));
                }
            } finally {
                exp.setPrecursorIonType(before);
            }
            Collections.sort(patterns, Collections.<IsotopePattern>reverseOrder());
            filterCandidateList(patterns, isoScores, deisotope);
        }
        return isoScores;
    }

    private static final class IonWhitelist {
        private final PrecursorIonType ionization;
        private final HashSet<MolecularFormula> whitelist;

        private IonWhitelist(PrecursorIonType ionization) {
            this.ionization = ionization;
            this.whitelist = new HashSet<MolecularFormula>();
        }
    }

    private MutableMs2Experiment detectParentPeakFromWhitelist(Ms2Experiment uexperiment, Set<MolecularFormula> whiteList) {
        final MutableMs2Experiment exp = new MutableMs2Experiment(uexperiment);
        final double pmz;
        if (uexperiment.getIonMass() > 0) {
            pmz = uexperiment.getIonMass();
        } else if (uexperiment.getPrecursorIonType().isIonizationUnknown() && autoIonMode) {
            // try every ionization mode
            double mz = 0;
            for (PrecursorIonType ionType : PeriodicTable.getInstance().getKnownLikelyPrecursorIonizations(uexperiment.getPrecursorIonType().getCharge())) {
                exp.setPrecursorIonType(ionType);
                mz = detectParentPeakByWhitelistAndIonization(exp, whiteList);
                if (mz > 0) {
                    break;
                }
            }
            pmz = mz;
        } else {
            // the ionmass is not given. Search for a appropiate ionmass in the spectrum
            // with at least 5% relative intensity in MS/MS or MS
            pmz = detectParentPeakByWhitelistAndIonization(uexperiment, whiteList);
        }
        if (pmz <= 0d) {
            throw new IllegalArgumentException("Please provide mass of parent peak.");
        }
        exp.setIonMass(pmz);
        return exp;
    }

    private double detectParentPeakByWhitelistAndIonization(Ms2Experiment uexperiment, Set<MolecularFormula> whiteList) {
        // the ionmass is not given. Search for a appropiate ionmass in the spectrum
        // with at least 5% relative intensity in MS/MS or MS
        final SimpleSpectrum mergedms1 = uexperiment.getMergedMs1Spectrum();
        final double[] rel = new double[uexperiment.getMs2Spectra().size()];
        for (int k = 0; k < uexperiment.getMs2Spectra().size(); ++k) {
            rel[k] = Spectrums.getMaximalIntensity(uexperiment.getMs2Spectra().get(k));
        }
        final double relms1 = mergedms1 == null ? 0 : Spectrums.getMaximalIntensity(mergedms1);
        PrecursorIonType iontype = uexperiment.getPrecursorIonType();
        final Deviation dev = profile.fragmentationPatternAnalysis.getDefaultProfile().getAllowedMassDeviation();
        int k = 0;
        for (MolecularFormula f : whiteList) {
            final double mz = iontype.neutralMassToPrecursorMass(f.getMass());
            if (mergedms1 != null) {
                k = Spectrums.mostIntensivePeakWithin(mergedms1, mz, dev);
                if (k >= 0 && mergedms1.getIntensityAt(k) / relms1 >= 0.05)
                    return mz;
            }
            for (int i = 0; i < uexperiment.getMs2Spectra().size(); ++i) {
                final Ms2Spectrum<? extends Peak> spec = uexperiment.getMs2Spectra().get(i);
                k = Spectrums.mostIntensivePeakWithin(spec, mz, dev);
                if (k >= 0 && spec.getIntensityAt(k) / rel[i] >= 0.05)
                    return mz;
            }
        }
        // cannot find ParentPeak
        return 0d;
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
        ProcessedInput pinput = profile.fragmentationPatternAnalysis.performValidation(uexperiment);
        if (formulaConstraints!=null) pinput.getMeasurementProfile().setFormulaConstraints(formulaConstraints);
        // first check if MS data is present;
        final List<IsotopePattern> candidates = lookAtMs1(pinput, deisotope != IsotopePatternHandling.omit);
        final HashMap<MolecularFormula, IsotopePattern> isoFormulas = new HashMap<>();
        filterCandidateList(candidates, isoFormulas, deisotope);
        int maxNumberOfFormulas = 0;
        pinput = profile.fragmentationPatternAnalysis.preprocessing(pinput.getOriginalInput(), pinput.getMeasurementProfile());
        if (candidates.size() > 0 && deisotope.isFiltering()) {
            maxNumberOfFormulas = isoFormulas.size();
        } else {
            maxNumberOfFormulas = pinput.getPeakAnnotationOrThrow(DecompositionList.class).get(pinput.getParentPeak()).getDecompositions().size();
        }

        final int outputSize = Math.min(maxNumberOfFormulas, numberOfCandidates);
        final int computeNTrees = Math.max(5, outputSize);
        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer != null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize + MAX_TREESIZE_INCREASE;

        final ArrayList<FTree> computedTrees = new ArrayList<FTree>();
        final FeedbackFlag feedback = new FeedbackFlag();
        final int SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS = Math.min(pinput.getMergedPeaks().size()-2, MIN_NUMBER_OF_EXPLAINED_PEAKS);

        try {
            outerLoop:
            while (true) {
                MultipleTreeComputation trees = profile.fragmentationPatternAnalysis.computeTrees(pinput);
                trees = trees.inParallel(3);
                if (isoFormulas.size() > 0 && deisotope.isFiltering()) {
                    trees = trees.onlyWith(isoFormulas.keySet());
                }
                trees = trees.computeMaximal(computeNTrees).withoutRecalibration();

                final DoubleEndWeightedQueue treeSet = new DoubleEndWeightedQueue(numberOfCandidates);

                final TreeIterator iter = trees.iterator(true);
                progress.init(maxNumberOfFormulas);
                int counter = 0;
                while (iter.hasNext()) {
                    final FTree tree = iter.next();
                    if (deisotope.isScoring()) addIsoScore(isoFormulas, tree);

                    if (tree != null) {
                        treeSet.add(tree);
                    }
                    if (iter.lastGraph() != null)
                        progress.update(++counter, maxNumberOfFormulas, iter.lastGraph().getRoot().getChildren(0).getFormula().toString(), feedback);
                    if (feedback.getFlag() == FeedbackFlag.Flag.CANCEL) return null;
                    if (feedback.getFlag() == FeedbackFlag.Flag.STOP) {
                        computedTrees.addAll(treeSet.getTrees());
                        break outerLoop;
                    }
                }
                progress.finished();

                // check if at least one of the best N trees satisfies the tree-rejection-condition
                boolean satisfied = treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE;
                if (!satisfied) {
                    final Iterator<FTree> treeIterator = treeSet.iterator();
                    for (int k = 0; k < computeNTrees; ++k) {
                        if (treeIterator.hasNext()) {
                            final FTree tree = treeIterator.next();
                            final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainedPeaks(tree);
                            if (tree.numberOfVertices() >= SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS && intensity >= MIN_EXPLAINED_INTENSITY) {
                                satisfied = true;
                                break;
                            }
                        } else break;
                    }
                }
                if (satisfied) {
                    computedTrees.addAll(treeSet.getTrees());
                    break;
                } else {
                    progress.info("Not enough peaks were explained. Repeat computation with less restricted constraints.");
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                    computedTrees.clear();
                    treeSet.clear();
                    // TODO!!!! find a smarter way to do this -_-
                    pinput = profile.fragmentationPatternAnalysis.preprocessing(pinput.getOriginalInput(), pinput.getMeasurementProfile());
                }
            }
            feedback.clear();
            if (recalibrating) {
                double maximalPossibleScoreByRecalibration = 0d;
                // now recalibrate the trees and recompute them another time...
                progress.info("recalibrate trees");
                progress.init(computedTrees.size());
                for (int k = 0; k < computedTrees.size(); ++k) {
                    final FTree recalibratedTree = profile.fragmentationPatternAnalysis.recalibrate(computedTrees.get(k), true);
                    maximalPossibleScoreByRecalibration = addRecalibrationPenalty(computedTrees.get(k), k+1, maximalPossibleScoreByRecalibration);
                    if (deisotope.isScoring()) addIsoScore(isoFormulas, recalibratedTree);
                    computedTrees.set(k, recalibratedTree);
                    progress.update(k + 1, computedTrees.size(), "recalibrate " + recalibratedTree.getRoot().getFormula().toString(), feedback);
                    if (feedback.getFlag() == FeedbackFlag.Flag.CANCEL) return null;
                    else if (feedback.getFlag() == FeedbackFlag.Flag.STOP) break;
                }
                progress.finished();
            }

            Collections.sort(computedTrees, Collections.reverseOrder(TREE_SCORE_COMPARATOR));

            final ArrayList<IdentificationResult> list = new ArrayList<IdentificationResult>(outputSize);
            for (int k = 0; k < Math.min(outputSize, computedTrees.size()); ++k) {
                final FTree tree = computedTrees.get(k);
                profile.fragmentationPatternAnalysis.recalculateScores(tree);
                list.add(new IdentificationResult(tree, k + 1));
            }

            return list;
        } finally {
            if (treeSizeScorer != null)
                treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
    }

    public List<IdentificationResult> identifyPrecursorAndIonization(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope) {
        return identifyPrecursorAndIonization(uexperiment, numberOfCandidates, recalibrating, deisotope, null);
    }

    /**
     * Identify the molecular formula of the measured compound as well as the ionization mode.
     * This method behaves like identify, but will try different ion modes and return the best trees over all different
     * ionizations. This method does not accept a whitelist, as for a neutral molecular formula candidate it is always possible to determine the
     * ionization mode.
     */
    public List<IdentificationResult> identifyPrecursorAndIonization(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, FormulaConstraints formulaConstraints) {
        ProcessedInput validatedInput = profile.fragmentationPatternAnalysis.performValidation(uexperiment);
        if (formulaConstraints!=null) validatedInput.getMeasurementProfile().setFormulaConstraints(formulaConstraints);

        final MutableMs2Experiment experiment = validatedInput.getExperimentInformation();
        // first check if MS data is present;
        final List<IsotopePattern> candidates = lookAtMs1(validatedInput, deisotope != IsotopePatternHandling.omit);
        int maxNumberOfFormulas = 0;
        final HashMap<MolecularFormula, IsotopePattern> isoFormulas = new HashMap<>();
        final double optIsoScore = filterCandidateList(candidates, isoFormulas, deisotope);
        if (isoFormulas.size()>0 && deisotope.isFiltering()) {
            return identify(uexperiment, numberOfCandidates, recalibrating, deisotope, isoFormulas.keySet());
        }
        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer != null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize + MAX_TREESIZE_INCREASE;

        final ArrayList<FTree> computedTrees = new ArrayList<FTree>();

        final ArrayList<Ionization> possibleIonModes = new ArrayList<Ionization>();
        final PrecursorIonType vion = validatedInput.getExperimentInformation().getPrecursorIonType();
        for (Ionization ionMode : PeriodicTable.getInstance().getKnownIonModes(vion.getCharge())) {
            possibleIonModes.add(ionMode);
        }

        final DoubleEndWeightedQueue treeSet = new DoubleEndWeightedQueue(numberOfCandidates);
        final FeedbackFlag feedback = new FeedbackFlag();
        int SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS=MIN_NUMBER_OF_EXPLAINED_PEAKS;
        try {
            outerLoop:
            while (true) {

                // try every ionization
                for (Ionization ionization : possibleIonModes) {

                    final PrecursorIonType ionType = (vion.isIonizationUnknown() ? PrecursorIonType.getPrecursorIonType(ionization) : vion);
                    experiment.setPrecursorIonType(ionType);
                    assert experiment.getMolecularFormula() == null;
                    experiment.setMoleculeNeutralMass(0d); // previous neutral mass is not correct anymore
                    final ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment.clone());
                    SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS = Math.min(pinput.getMergedPeaks().size()-2, MIN_NUMBER_OF_EXPLAINED_PEAKS);
                    MultipleTreeComputation trees = profile.fragmentationPatternAnalysis.computeTrees(pinput);
                    trees = trees.inParallel(3);
                    if (isoFormulas.size() > 0) {
                        trees = trees.onlyWithIons(isoFormulas.keySet());
                    }
                    trees = trees.computeMaximal(numberOfCandidates).withoutRecalibration();

                    final TreeIterator iter = trees.iterator(true);
                    progress.init(maxNumberOfFormulas);
                    int counter = 0;
                    while (iter.hasNext()) {
                        final FTree tree = iter.next();
                        if (deisotope.isScoring()) addIsoScore(isoFormulas, tree);

                        if (tree != null) {
                            treeSet.add(tree);
                        }
                        if (iter.lastGraph() != null)
                            progress.update(++counter, maxNumberOfFormulas, iter.lastGraph().getRoot().getChildren(0).getFormula().toString() + " " + ionType.toString(), feedback);
                        if (feedback.getFlag() == FeedbackFlag.Flag.CANCEL) return null;
                        else if (feedback.getFlag() == FeedbackFlag.Flag.STOP) {
                            computedTrees.addAll(treeSet.getTrees());
                            break outerLoop;
                        }
                    }
                    progress.finished();

                }

                // check if at least one of the best N trees satisfies the tree-rejection-condition
                boolean satisfied = treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE;
                if (!satisfied) {
                    final Iterator<FTree> treeIterator = treeSet.iterator();
                    for (int k = 0; k < numberOfCandidates; ++k) {
                        if (treeIterator.hasNext()) {
                            final FTree tree = treeIterator.next();
                            final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainedPeaks(tree);
                            if (tree.numberOfVertices() >= SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS && intensity >= MIN_EXPLAINED_INTENSITY) {
                                satisfied = true;
                                break;
                            }
                        } else break;
                    }
                }
                if (satisfied) {
                    computedTrees.addAll(treeSet.getTrees());
                    break;
                } else {
                    progress.info("Not enough peaks were explained. Repeat computation with less restricted constraints.");
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                    treeSet.clear();
                    computedTrees.clear();
                }
            }
            feedback.clear();
            if (recalibrating) {
                double maximalPossibleScoreByRecalibration=0d;
                // now recalibrate the trees and recompute them another time...
                progress.info("recalibrate trees");
                progress.init(computedTrees.size());
                for (int k = 0; k < computedTrees.size(); ++k) {
                    final FTree recalibratedTree = profile.fragmentationPatternAnalysis.recalibrate(computedTrees.get(k), true);
                    maximalPossibleScoreByRecalibration = addRecalibrationPenalty(computedTrees.get(k), k+1, maximalPossibleScoreByRecalibration);
                    if (deisotope.isScoring()) addIsoScore(isoFormulas, recalibratedTree);
                    computedTrees.set(k, recalibratedTree);
                    progress.update(k + 1, computedTrees.size(), "recalibrate " + recalibratedTree.getRoot().getFormula().toString(), feedback);
                    if (feedback.getFlag() == FeedbackFlag.Flag.STOP) break;
                    else if (feedback.getFlag() == FeedbackFlag.Flag.CANCEL) return null;
                }
                progress.finished();
            }

            Collections.sort(computedTrees, Collections.reverseOrder(TREE_SCORE_COMPARATOR));


            final ArrayList<IdentificationResult> list = new ArrayList<IdentificationResult>(Math.min(numberOfCandidates, computedTrees.size()));
            for (int k = 0; k < Math.min(numberOfCandidates, computedTrees.size()); ++k) {
                final FTree tree = computedTrees.get(k);
                profile.fragmentationPatternAnalysis.recalculateScores(tree);
                list.add(new IdentificationResult(tree, k + 1));
            }

            return list;
        } finally {
            if (treeSizeScorer != null) treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
    }

    public FormulaConstraints predictElementsFromMs1(Ms2Experiment experiment) {
        final SimpleSpectrum pattern = getMs1Analyzer().extractPattern(experiment, experiment.getIonMass());
        if (pattern==null) return null;
        return getElementPrediction().predictConstraints(pattern);
    }

    boolean predictElements(ProcessedInput input) {
        if (getElementPrediction() != null) {
            final FormulaConstraints prediction = predictElementsFromMs1(input.getExperimentInformation());
            if (prediction==null) return false;
            input.getMeasurementProfile().setFormulaConstraints(prediction);
            return true;
        } else return false;
    }

    /**
     * check MS spectrum. If an isotope pattern is found, check it's monoisotopic mass and update the ionmass field
     * if this field is null yet
     * If deisotope is set, start isotope pattern analysis
     *
     * @return
     */
    protected List<IsotopePattern> lookAtMs1(ProcessedInput pinput, boolean deisotope) {
        final MutableMs2Experiment experiment = pinput.getExperimentInformation();
        if (experiment.getIonMass() == 0) {
            if (experiment.getMs1Spectra().size() == 0)
                throw new RuntimeException("Please provide the parentmass of the measured compound");
            List<IsotopePattern> candidates = profile.isotopePatternAnalysis.deisotope(experiment, pinput.getMeasurementProfile());
            experiment.setIonMass(candidates.get(0).getMonoisotopicMass());
            return deisotope ? filterIsotopes(candidates) : Collections.<IsotopePattern>emptyList();
        }
        return deisotope ? filterIsotopes(profile.isotopePatternAnalysis.deisotope(experiment,pinput.getMeasurementProfile())) : Collections.<IsotopePattern>emptyList();
    }

    /**
     * check if at least one isotope pattern contains more than one peak and has a score above zero. Otherwise,
     * omit isotopes.
     */
    private List<IsotopePattern> filterIsotopes(List<IsotopePattern> deisotope) {
        for (IsotopePattern pattern : deisotope) {
            if (pattern.getPattern().size()>1 && pattern.getScore()>0) return deisotope;
        }
        return Collections.emptyList();
    }

    protected void addIsoScore(HashMap<MolecularFormula, IsotopePattern> isoFormulas, FTree tree) {
        final TreeScoring sc = tree.getAnnotationOrThrow(TreeScoring.class);
        final IsotopePattern pat = isoFormulas.get(tree.getAnnotationOrThrow(PrecursorIonType.class).measuredNeutralMoleculeToNeutralMolecule(tree.getRoot().getFormula()));
        if (pat != null) {
            sc.addAdditionalScore(ISOTOPE_SCORE, pat.getScore());
            tree.setAnnotation(IsotopePattern.class, pat);
        }
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
        ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment, FormulaConstraints.allSubsetsOf(formula));
        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer != null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize + MAX_TREESIZE_INCREASE;
        final int SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS = Math.min(pinput.getMergedPeaks().size()-2, MIN_NUMBER_OF_EXPLAINED_PEAKS);

        FTree tree = null;
        try {
            while (true) {
                tree = profile.fragmentationPatternAnalysis.computeTrees(pinput).withRecalibration(recalibrating).onlyWith(Arrays.asList(formula)).optimalTree();
                if (tree == null) return new IdentificationResult(null, 1);
                final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainablePeaks(tree);
                if (treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE || (tree.numberOfVertices() >= SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS && intensity >= MIN_EXPLAINED_INTENSITY)) {
                    break;
                } else {
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                    // TODO!!!! find a smarter way to do this -_-
                    pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);
                }
            }
            profile.fragmentationPatternAnalysis.recalculateScores(tree);
        } finally {
            treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
        return new IdentificationResult(tree, 1);
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
        final MolecularFormula formula;
        final IonTreeUtils.Type type =tree.getAnnotationOrNull(IonTreeUtils.Type.class);
        if (type == IonTreeUtils.Type.RESOLVED) {
            formula = tree.getRoot().getFormula();
        } else if (type == IonTreeUtils.Type.IONIZED) {
            formula = tree.getAnnotationOrThrow(PrecursorIonType.class).precursorIonToNeutralMolecule(tree.getRoot().getFormula());
        } else {
            formula = tree.getAnnotationOrThrow(PrecursorIonType.class).measuredNeutralMoleculeToNeutralMolecule(tree.getRoot().getFormula());
        }

        final MutableMs2Experiment mutableMs2Experiment = new MutableMs2Experiment(experiment);
        mutableMs2Experiment.setMolecularFormula(formula);
        mutableMs2Experiment.setMoleculeNeutralMass(0d);
        mutableMs2Experiment.setPrecursorIonType(tree.getAnnotationOrNull(PrecursorIonType.class));

        ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(mutableMs2Experiment.clone(), FormulaConstraints.allSubsetsOf(formula));
        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        if (treeSizeScorer==null) return null;
        final double originalTreeSize = treeSizeScorer.getTreeSizeScore();

        double modifiedTreeSizeScore = originalTreeSize;
        //try to find used treeSize score
        if (tree.numberOfVertices()>1){
            FragmentAnnotation<Score> anno = tree.getFragmentAnnotationOrNull(Score.class);
            ParameterHelper parameterHelper = ParameterHelper.getParameterHelper();
            if (anno!=null){
                Double treeSizeScore = anno.get(tree.getFragmentAt(1)).get(parameterHelper.toClassName(TreeSizeScorer.class));
                if (treeSizeScore!=null){
                    modifiedTreeSizeScore = treeSizeScore;
                }
            }
        }

        final double MAX_TREESIZE_SCORE = originalTreeSize + MAX_TREESIZE_INCREASE;
        final int SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS = Math.min(pinput.getMergedPeaks().size()-2, MIN_NUMBER_OF_EXPLAINED_PEAKS);

        FTree beautifulTree = new FTree(tree);
        int iteration = 0;
        try {
            while (true) {
                final double intensity = (beautifulTree==null ? 0 : beautifulTree.getAnnotationOrThrow(TreeScoring.class).getExplainedIntensityOfExplainablePeaks());
//                final double intensity = (beautifulTree==null ? 0 : profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainablePeaks(beautifulTree));
                if (modifiedTreeSizeScore >= MAX_TREESIZE_SCORE){
                    if (beautifulTree!=null){
                        if (iteration>0) profile.fragmentationPatternAnalysis.recalculateScores(beautifulTree);
                        return beautifulTree;
                    } else {
                        return null;
                    }
                }else if (beautifulTree!=null && (beautifulTree.numberOfVertices() >= SPECIFIC_MIN_NUMBER_OF_EXPLAINED_PEAKS && intensity >= MIN_EXPLAINED_INTENSITY)) {
                    if (iteration>0) profile.fragmentationPatternAnalysis.recalculateScores(beautifulTree);
                    return beautifulTree;
                } else {
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                    // TODO!!!! find a smarter way to do this -_-
                    pinput = profile.fragmentationPatternAnalysis.preprocessing(mutableMs2Experiment.clone(), FormulaConstraints.allSubsetsOf(formula));
                }

                beautifulTree = profile.fragmentationPatternAnalysis.computeTrees(pinput).withRecalibration(recalibrating).onlyWith(Arrays.asList(formula)).withBackbones(beautifulTree).optimalTree();
                ++iteration;
            }

        } catch (Exception e){
            e.printStackTrace();
            return null;
        } finally {
            treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
    }


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
}
