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
package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Ms2ExperimentValidator;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphReduction;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SimpleReduction;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SubFormulaGraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.PeakMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ExtractedIsotopePattern;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ScoredFormulaMap;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Scoring;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.PeakAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TLongProcedure;

import java.util.*;

/**
 * FragmentationPatternAnalysis contains the pipeline for computing fragmentation trees
 * This is done using the following steps:
 * 1. Validate input and correct/fillin missing values
 * 2. Preprocess input (add baseline, noise filters and so on)
 * 3. Merge peaks within the same spectra, compute relative intensitities
 * 4. Merge peaks from different spectra, compute a flat peak list
 * 5. Postprocessing (delete peaks with low intensitities and so on)
 * 6. Detect parent peak
 * 7. Decompose each peak
 * 8. Postprocesing
 * 9. Score each peak and each pair of peaks
 *
 * Steps 1-9 are bundled within the method FragmentationPatternAnalysis#preprocessing
 *
 * Now for each Molecular formula candidate:
 * 10. Compute Fragmentation Graph
 * 11. Score losses and vertices in the graph
 * 12. Compute Fragmentation tree
 * 13. Recalibrate tree
 * 14. Might repeat all steps with recalibration function
 * 15. Postprocess tree
 *
 * Steps 10-15 are bundled within the method FragmentationPatternAnalysis#computeTree
 *
 * You can run each step individually. However, you have to run step 1. first to get a ProcessedInput object.
 * This object contains the Ms2Experiment input and stores all intermediate values during the computation.
 *
 * Some (or, honestly, most) steps rely on certain properties and intermediate values computed in previous steps.
 * So you have to be very careful when running a step separately. The recommended way is to run the whole pipeline.
 *
 *
 */
public class FragmentationPatternAnalysis implements Parameterized, Cloneable {
    private List<Ms2ExperimentValidator> inputValidators;
    private Warning validatorWarning;
    private boolean repairInput;
    private NormalizationType normalizationType;
    private PeakMerger peakMerger;
    private DecomposerCache decomposers;
    private List<DecompositionScorer<?>> decompositionScorers;
    private List<DecompositionScorer<?>> rootScorers;
    private List<LossScorer> lossScorers;
    private List<PeakPairScorer> peakPairScorers;
    private List<PeakScorer> fragmentPeakScorers;
    private GraphBuilder graphBuilder;
    private List<Preprocessor> preprocessors;
    private List<PostProcessor> postProcessors;
    private TreeBuilder treeBuilder;
    private GraphReduction reduction;
    private IsotopePatternInMs2Scorer isoInMs2Scorer;
    private IsotopeInMs2Handling isotopeInMs2Handling;

    private static ParameterHelper parameterHelper = ParameterHelper.getParameterHelper();



    private final HashMap<Class<?>, SiriusPlugin> siriusPlugins = new HashMap<>();

    public void registerPlugin(SiriusPlugin plugin) {
        if (!siriusPlugins.containsKey(plugin.getClass())) {
            siriusPlugins.put((plugin.getClass(), plugin);
            plugin.initializePlugin(new SiriusPlugin.PluginInitializer(this));
        }
    }

    /**
     * Adds all annotations to ProcessedInput which are necessary for graph building
     */
    @Provides(DecompositionList.class)
    @Provides(Scoring.class)
    public ProcessedInput prepareGraphBuilding(ProcessedInput input) {
        return performPeakScoring(performDecomposition(input));
    }


    /**
     * Step 6: Decomposition
     * Decompose each peak as well as the parent peak
     */
    public ProcessedInput performDecomposition(ProcessedInput input) {
        final PeriodicTable PT = PeriodicTable.getInstance();
        final Whiteset whiteset = input.getAnnotation(Whiteset.class, null);
        final FormulaConstraints constraints = input.getAnnotation(FormulaConstraints.class, null);
        final Ms2Experiment experiment = input.getExperimentInformation();
        final Deviation parentDeviation = input.getAnnotation(MS2MassDeviation.class, null).allowedMassDeviation;
        // sort again...
        final ArrayList<ProcessedPeak> processedPeaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size() - 1);
        // decompose peaks
        final List<IonMode> ionModes = input.getAnnotationOrThrow(PossibleIonModes.class).getIonModesWithProbabilityAboutZero();
        final PeakAnnotation<DecompositionList> decompositionList = input.getOrCreatePeakAnnotation(DecompositionList.class);
        final MassToFormulaDecomposer decomposer = decomposers.getDecomposer(constraints.getChemicalAlphabet());
        final Deviation fragmentDeviation = input.getAnnotation(MS2MassDeviation.class, null).allowedMassDeviation;

        final List<MolecularFormula> pmds;
        final List<Decomposition> decomps = new ArrayList<>();

        if (input.getOriginalInput().getMolecularFormula()!=null){
            //always use formula. don't look at mass dev.
            final MolecularFormula formula = input.getOriginalInput().getMolecularFormula();
            final PrecursorIonType ionType = experiment.getPrecursorIonType();
            Decomposition decomposition = new Decomposition(ionType.neutralMoleculeToMeasuredNeutralMolecule(formula), ionType.getIonization(), 0d);
            decomps.add(decomposition);
            pmds = new ArrayList<>();
            pmds.add(decomposition.getCandidate());

            if (!parentDeviation.inErrorWindow(parentPeak.getMass(), ionType.neutralMassToPrecursorMass(formula.getMass()))){
                validatorWarning.warn("Specified precursor molecular formula does not fall into given m/z error window. "
                        +formula.formatByHill()+" for m/z "+parentPeak.getMass()+" and ionization "+ionType);
            }
        } else if (whiteset != null && !whiteset.getFormulas().isEmpty()) {
            final Collection<PrecursorIonType> ionTypes;
            if (experiment.getPrecursorIonType().isIonizationUnknown())
                ionTypes = experiment.getAnnotationOrThrow(PossibleAdducts.class).getAdducts();
            else ionTypes = Arrays.asList(experiment.getPrecursorIonType());
            decomps.addAll(whiteset.resolve(parentPeak.getMass(), parentDeviation, ionTypes));
            pmds = new ArrayList<>();
            for (Decomposition d : decomps) pmds.add(d.getCandidate());
        } else if (!experiment.getPrecursorIonType().isIonizationUnknown()) {
            // use given ionization
            final PrecursorIonType ionType = experiment.getPrecursorIonType();
            final List<MolecularFormula> forms = decomposer.decomposeToFormulas(ionType.precursorMassToNeutralMass(parentPeak.getMass()), parentDeviation.absoluteFor(parentPeak.getMass()), constraints);
            pmds = new ArrayList<>();
            for (MolecularFormula f : forms)  {
                final MolecularFormula neutralMeasuredFormula = ionType.neutralMoleculeToMeasuredNeutralMolecule(f);
                if (neutralMeasuredFormula.isAllPositiveOrZero()) {
                    decomps.add(new Decomposition(neutralMeasuredFormula, ionType.getIonization() , 0d));
                    pmds.add(neutralMeasuredFormula);
                }
            }
        } else {

            pmds = new ArrayList<>();
            for (Ionization ion : ionModes) {
                final List<MolecularFormula> forms = decomposer.decomposeToFormulas(ion.subtractFromMass(parentPeak.getMass()), parentDeviation.absoluteFor(parentPeak.getMass()), constraints);
                pmds.addAll(forms);
                for (MolecularFormula f : forms) decomps.add(new Decomposition(f, ion, 0d));
            }
        }


        //todo ? always allow M+H+ for fragments
        //add IonModes which are possible for fragments due to adduct switch
        PossibleAdductSwitches possibleAdductSwitches = input.getAnnotation(PossibleAdductSwitches.class);
        Set<IonMode> ionModeSet = new HashSet<>();
        if (possibleAdductSwitches!=null) {
            while (true) {
                Set<IonMode> newIonModes = new HashSet<>();
                for (IonMode ionMode : ionModes) {
                    newIonModes.addAll(possibleAdductSwitches.getPossibleIonizations(ionMode));
                }
                if (ionModeSet.size()==newIonModes.size()){
                    break;
                }
                ionModeSet = newIonModes;
            }
            ionModes.clear();
        }
        for (IonMode ionization : ionModeSet) {
            ionModes.add(ionization);
        }

        // may split pmds if multiple alphabets are present
        final List<MassToFormulaDecomposer> decomposers = new ArrayList<>();
        final List<FormulaConstraints> constraintList = new ArrayList<>();
        getDecomposersFor(pmds, constraints, decomposers, constraintList);

        decompositionList.set(parentPeak, new DecompositionList(decomps));
        int j = 0;
        for (ProcessedPeak peak : processedPeaks.subList(0, processedPeaks.size() - 1)) {
            peak.setIndex(j++);
            final List<Decomposition> decompositions = new ArrayList<>();
            final double mz = peak.getMass();
            for (Ionization ion : ionModes) {
                final double mass = ion.subtractFromMass(mz);
                if (mass > 0) {
                    final HashSet<MolecularFormula> formulas = new HashSet<>();
                    for (int D=0; D < decomposers.size(); ++D) {
                        formulas.addAll(decomposers.get(D).decomposeToFormulas(mass, fragmentDeviation.absoluteFor(peak.getMass()), constraintList.get(D)));
                    }
                    for (MolecularFormula f : formulas) decompositions.add(new Decomposition(f, ion, 0d));
                }
            }
            decompositionList.set(peak, new DecompositionList(decompositions));
        }
        parentPeak.setIndex(processedPeaks.size() - 1);
        assert parentPeak == processedPeaks.get(processedPeaks.size() - 1);
        // important: for each two peaks which are within 2*massrange:
        //  => make decomposition list disjoint
        final SpectralRecalibration recalibration = input.getAnnotation(SpectralRecalibration.class, SpectralRecalibration::none);
        final double[] recalibrated = new double[processedPeaks.size()];
        for (int i=0; i < recalibrated.length; ++i) recalibrated[i] = recalibration.recalibrate(input.getMergedPeaks().get(i));
        final Deviation window = fragmentDeviation.multiply(2);
        for (int i = 1; i < processedPeaks.size() - 1; ++i) {
            if (window.inErrorWindow(recalibrated[i], recalibrated[i-1])) {
                decompositionList.get(processedPeaks.get(i-1)).disjoin(decompositionList.get(processedPeaks.get(i)), recalibrated[i-1], recalibrated[i]);
            }
        }
        input.setAnnotation(DecompositionList.class, decompositionList.get(parentPeak));
        return postProcess(PostProcessor.Stage.AFTER_DECOMPOSING, input);
    }

    private void getDecomposersFor(List<MolecularFormula> pmds, FormulaConstraints constraint, List<MassToFormulaDecomposer> decomposers, List<FormulaConstraints> constraintList) {
        pmds = new ArrayList<>(pmds);
        final TObjectLongHashMap<Element> elementMap = new TObjectLongHashMap<>(10, 0.75f, -1);
        final TLongObjectHashMap<MassToFormulaDecomposer> decomposerMap = new TLongObjectHashMap<>(10);
        final long[] buf = new long[2];
        Collections.sort(pmds, new Comparator<MolecularFormula>() {
            @Override
            public int compare(MolecularFormula o1, MolecularFormula o2) {
                return o2.getNumberOfElements()-o1.getNumberOfElements();
            }
        });
        for (MolecularFormula formula : pmds) {
            buf[0] = 0l;
            formula.visit(new FormulaVisitor<Object>() {
                @Override
                public Object visit(Element element, int amount) {
                    long val = elementMap.get(element);
                    if (val < 0) {
                        val =  1L << elementMap.size();
                        elementMap.put(element,val);
                    }
                    buf[0] |= val;
                    return null;
                }
            });
            buf[1] = -1L;
            if (!decomposerMap.containsKey(buf[0])) {
                decomposerMap.forEachKey(new TLongProcedure() {
                    @Override
                    public boolean execute(long value) {
                        if ((value & buf[0]) == buf[0]) {
                            buf[1] = 1L;
                            return false;
                        } else return true;
                    }
                });
                if (buf[1] < 0) {
                    MassToFormulaDecomposer newDecomposer = getDecomposerCache().getDecomposer(new ChemicalAlphabet(formula.elementArray()));
                    decomposerMap.put(buf[0], newDecomposer);
                }
            }
        }
        for (MassToFormulaDecomposer decomposer : decomposerMap.valueCollection()) {
            final FormulaConstraints cs = constraint.intersection(new FormulaConstraints(decomposer.getChemicalAlphabet()));
            constraintList.add(cs);
            decomposers.add(decomposer);
        }
    }

    /**
     * Step 7: Peak Scoring
     * Scores each peak. Expects a decomposition list
     */
    public ProcessedInput performPeakScoring(ProcessedInput input) {
        final List<ProcessedPeak> processedPeaks = input.getMergedPeaks();
        final ProcessedPeak parentPeak = input.getParentPeak();
        final int n = processedPeaks.size();
        input.computeAnnotationIfAbsent(Scoring.class).initializeScoring(n);
        // score peak pairs
        final double[][] peakPairScores = input.getAnnotationOrThrow(Scoring.class).getPeakPairScores();
        for (PeakPairScorer scorer : peakPairScorers) {
            scorer.score(processedPeaks, input, peakPairScores);
        }
        // score fragment peaks
        final double[] peakScores = input.getAnnotationOrThrow(Scoring.class).getPeakScores();
        for (PeakScorer scorer : fragmentPeakScorers) {
            scorer.score(processedPeaks, input, peakScores);
        }

        final PeakAnnotation<DecompositionList> decomp = input.getPeakAnnotationOrThrow(DecompositionList.class);

        // dont score parent peak
        peakScores[peakScores.length - 1] = 0d;


        // score peaks
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(decompositionScorers.size());
            for (DecompositionScorer<?> scorer : decompositionScorers) preparations.add(scorer.prepare(input));
            for (int i = 0; i < processedPeaks.size() - 1; ++i) {
                final DecompositionList decomps = decomp.get(processedPeaks.get(i));
                final ArrayList<Decomposition> scored = new ArrayList<Decomposition>(decomps.getDecompositions().size());
                for (Decomposition f : decomps.getDecompositions()) {
                    double score = 0d;
                    int k = 0;
                    for (DecompositionScorer<?> scorer : decompositionScorers) {
                        score += ((DecompositionScorer<Object>) scorer).score(f.getCandidate(),f.getIon(), processedPeaks.get(i), input, preparations.get(k++));
                    }
                    scored.add(new Decomposition(f.getCandidate(),f.getIon(), score));
                }
                decomp.set(processedPeaks.get(i), new DecompositionList(scored));
            }
        }
        // same with root
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(rootScorers.size());
            for (DecompositionScorer<?> scorer : rootScorers) preparations.add(scorer.prepare(input));
            final ArrayList<Decomposition> scored = new ArrayList<>(decomp.get(parentPeak).getDecompositions());
            for (int j=0; j < scored.size(); ++j) {
                double score = 0d;
                int k = 0;
                final Decomposition f = scored.get(j);
                for (DecompositionScorer<?> scorer : rootScorers) {
                    score += ((DecompositionScorer<Object>) scorer).score(f.getCandidate(),f.getIon(), input.getParentPeak(), input, preparations.get(k++));
                }
                scored.set(j, new Decomposition(scored.get(j).getCandidate(), scored.get(j).getIon(), score));

            }
            Collections.sort(scored, Collections.reverseOrder());
            decomp.set(parentPeak, new DecompositionList(scored));
            input.setAnnotation(DecompositionList.class, decomp.get(parentPeak));
        }
        // set peak indizes
        for (int i = 0; i < processedPeaks.size(); ++i) processedPeaks.get(i).setIndex(i);

        return input;
    }

    /**
     *
     */
    public FragmentationPatternAnalysis() {
        this.decomposers = new DecomposerCache();
        setInitial();
    }

    public static <G, D, L> FragmentationPatternAnalysis loadFromProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        if (!document.hasKeyInDictionary(dict, "FragmentationPatternAnalysis"))
            throw new IllegalArgumentException("No field 'FragmentationPatternAnalysis' in profile");
        final FragmentationPatternAnalysis analyzer = (FragmentationPatternAnalysis) helper.unwrap(document,
                document.getFromDictionary(dict, "FragmentationPatternAnalysis"));
        analyzer.initialize();

        return analyzer;
    }

    /**
     * Construct a new FragmentationPatternAnaylsis with default scorers
     */
    public static FragmentationPatternAnalysis defaultAnalyzer() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();

        // peak pair scorers
        final LossSizeScorer lossSize = new LossSizeScorer(new LogNormalDistribution(4d, 1d), -5d);/*LossSizeScorer.LEARNED_DISTRIBUTION, LossSizeScorer.LEARNED_NORMALIZATION*/
        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        peakPairScorers.add(lossSize);

        // loss scorers
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());
        //lossScorers.add(new StrangeElementScorer());
        lossScorers.add(CommonLossEdgeScorer.getLossSizeCompensationForExpertList(lossSize, 0.75d).addImplausibleLosses(Math.log(0.01)));

        // peak scorers
        final List<PeakScorer> peakScorers = new ArrayList<PeakScorer>();
        peakScorers.add(new PeakIsNoiseScorer());
        peakScorers.add(new TreeSizeScorer(0d));

        // root scorers
        final List<DecompositionScorer<?>> rootScorers = new ArrayList<DecompositionScorer<?>>();
        rootScorers.add(new ChemicalPriorScorer());
        rootScorers.add(new MassDeviationVertexScorer());

        // fragment scorers
        final List<DecompositionScorer<?>> fragmentScorers = new ArrayList<DecompositionScorer<?>>();
        fragmentScorers.add(new MassDeviationVertexScorer());
        fragmentScorers.add(new CommonFragmentsScore(new HashMap<MolecularFormula, Double>()));

        // setup
        analysis.setLossScorers(lossScorers);
        analysis.setRootScorers(rootScorers);
        analysis.setDecompositionScorers(fragmentScorers);
        analysis.setFragmentPeakScorers(peakScorers);
        analysis.setPeakPairScorers(peakPairScorers);

        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.005d));
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());

        analysis.initialize();

        return analysis;
    }

    private void initialize() {
        for (Object o : this.inputValidators) {
            initialize(o);
        }
        initialize(this.peakMerger);
        for (Object o : this.decompositionScorers) {
            initialize(o);
        }
        for (Object o : this.rootScorers) {
            initialize(o);
        }
        for (Object o : this.lossScorers) {
            initialize(o);
        }
        for (Object o : this.peakPairScorers) {
            initialize(o);
        }
        for (Object o : this.fragmentPeakScorers) {
            initialize(o);
        }
        initialize(graphBuilder);
        for (Object o : this.preprocessors) {
            initialize(o);
        }
        for (Object o : this.postProcessors) {
            initialize(o);
        }
        //initialize(treeBuilder);
        initialize(reduction);
        initialize(isoInMs2Scorer);
    }
    private void initialize(Object o)  {
        if (o==null) return;
        if (o instanceof Initializable) {
            ((Initializable)o).initialize(this);
        }
    }



    /**
     * Helper function to change the parameters of a specific scorer
     * <code>
     * analyzer.getByClassName(MassDeviationScorer.class, analyzer.getDecompositionScorers).setMassPenalty(4d);
     * </code>
     *
     * @param klass
     * @param list
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T) elem;
        return null;
    }

    public static <S, T extends S> T getOrCreateByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T) elem;
        try {
            final T obj = klass.newInstance();
            list.add(obj);
            return obj;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <G, D, L> void writeToProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        final D fpa = document.newDictionary();
        exportParameters(helper, document, fpa);
        document.addToDictionary(fpa, "$name", helper.toClassName(getClass()));
        document.addDictionaryToDictionary(dict, "FragmentationPatternAnalysis", fpa);
    }

    /**
     * Remove all scorers and set analyzer to clean state
     */
    public void setInitial() {
        this.inputValidators = new ArrayList<>();
        this.validatorWarning = new Warning.Noop();
        this.normalizationType = NormalizationType.GLOBAL;
        this.peakMerger = new HighIntensityMerger();
        this.repairInput = true;
        this.decompositionScorers = new ArrayList<>();
        this.preprocessors = new ArrayList<>();
        this.postProcessors = new ArrayList<>();
        this.rootScorers = new ArrayList<>();
        this.peakPairScorers = new ArrayList<>();
        this.fragmentPeakScorers = new ArrayList<>();
        this.graphBuilder = new SubFormulaGraphBuilder();
        this.lossScorers = new ArrayList<>();
        isoInMs2Scorer = new IsotopePatternInMs2Scorer();
        isotopeInMs2Handling = PropertyManager.DEFAULTS.createInstanceWithDefaults(IsotopeInMs2Handling.class);
        this.reduction = new SimpleReduction();
    }

    /**
     * add annotations to the tree class
     * basically, the annotation system allows you to put arbitrary meta information into the tree
     * However, many of these information might be required by other algorithms (like the Peak annotation).
     * Therefore, this method tries to add as much metainformation as possible into the tree.
     * Only annotations that are registered in DescriptorRegistry (package BabelMs) will be serialized together
     * with the tree. Feel free to add "temporary non-persistent" annotations. Annotations should be
     * final immutable pojos.
     * @param originalGraph
     * @param tree
     */
    public void addTreeAnnotations(ProcessedInput pinput, FGraph originalGraph, FTree tree) {
        //final ProcessedInput pinput = originalGraph.getAnnotationOrNull(ProcessedInput.class);
        //tree.setAnnotation(ProcessedInput.class, pinput);
        PrecursorIonType ionType = originalGraph.getAnnotationOrThrow(PrecursorIonType.class);
        if (ionType.isIonizationUnknown()) {
            // use ionization instead
            for (Fragment root : originalGraph.getRoot().getChildren()) {
                if (root.getFormula().equals(tree.getRoot().getFormula())) {
                    Ionization ionMode = originalGraph.getFragmentAnnotationOrThrow(Ionization.class).get(root);
                    ionType = PrecursorIonType.getPrecursorIonType(ionMode);
                }
            }
        }
        // tree annotations
        tree.setAnnotation(PrecursorIonType.class, ionType);

        final FragmentAnnotation<Ms2IsotopePattern> msIsoAno = tree.getOrCreateFragmentAnnotation(Ms2IsotopePattern.class);

        // fragment annotations
        final FragmentAnnotation<AnnotatedPeak> peakAnnotation = tree.getOrCreateFragmentAnnotation(AnnotatedPeak.class);
        final FragmentAnnotation<Peak> simplePeakAnnotation = tree.getOrCreateFragmentAnnotation(Peak.class);
        // non-persistent fragment annotation
        final FragmentAnnotation<ProcessedPeak> peakAno = tree.getOrCreateFragmentAnnotation(ProcessedPeak.class);

        final TCustomHashMap<Fragment, Fragment> graphFragmentMap = Fragment.newFragmentWithIonMap();
        for (Fragment f : tree) {
            graphFragmentMap.put(f, f);
        }
        for (Fragment f : originalGraph) {
            final MolecularFormula form = f.getFormula();
            //todo still test for null formula or use all?!
            if (form != null && graphFragmentMap.containsKey(f))
                graphFragmentMap.put(f, f);
        }

        // remove pseudo nodes

        if (originalGraph.getFragmentAnnotationOrNull(IsotopicMarker.class)!=null) {
            final FragmentAnnotation<IsotopicMarker> marker = tree.getOrCreateFragmentAnnotation(IsotopicMarker.class);
            final FragmentAnnotation<Ms2IsotopePattern> msIsoAnoG = originalGraph.getOrCreateFragmentAnnotation(Ms2IsotopePattern.class);
            final ArrayList<Fragment> subtreesToDelete = new ArrayList<Fragment>();
            for (Fragment f : tree) {
                if (msIsoAnoG.get(graphFragmentMap.get(f))!=null) {
                    // find isotope chain
                    double score = 0d;
                    int count = 1;
                    for (Fragment fchild : f.getChildren()) {
                        if (fchild.getFormula().isEmpty()) {
                            Fragment child = fchild;
                            while (true) {
                                ++count;
                                score += child.getIncomingEdge().getWeight();
                                marker.set(child, new IsotopicMarker());
                                if (child.isLeaf()) break;
                                else child = child.getChildren(0);
                            }
                            subtreesToDelete.add(fchild);
                            break;
                        }
                    }
                    if (count > 1) {
                        final Ms2IsotopePattern origPattern = msIsoAnoG.get(graphFragmentMap.get(f));
                        final Peak[] shortened = Arrays.copyOf(origPattern.getPeaks(), count);
                        // TODO: what happens if second peak is below threshold but third peak not?
                        msIsoAno.set(f, new Ms2IsotopePattern(shortened, score));
                    }
                }
            }
            for (Fragment f : subtreesToDelete) tree.deleteSubtree(f);
        }



        final FragmentAnnotation<ProcessedPeak> graphPeakAno = originalGraph.getFragmentAnnotationOrThrow(ProcessedPeak.class);

        // TODO: we need a more general way to transfer annotations from graph nodes to tree nodes!

        final FragmentAnnotation<Ms1IsotopePattern> ms1IsoAno = originalGraph.getFragmentAnnotationOrNull(Ms1IsotopePattern.class);
        final FragmentAnnotation<Ms1IsotopePattern> treeMs1IsoAno = (ms1IsoAno == null && pinput.getAnnotation(ExtractedIsotopePattern.class) == null) ? null : tree.addFragmentAnnotation(Ms1IsotopePattern.class);

        // check for MS1 isotope scores
        for (Fragment treeFragment : tree) {
            final Fragment graphFragment = graphFragmentMap.get(treeFragment);
            if (graphFragment==null)
                throw new NullPointerException("do not find graph fragment with formula " + treeFragment.getFormula());
            final ProcessedPeak graphPeak = graphPeakAno.get(graphFragment);
            if (graphPeak==null)
                throw new NullPointerException("graph node has no associated peak");
            peakAno.set(treeFragment, graphPeak);

            simplePeakAnnotation.set(treeFragment, graphPeak);
            //todo do I have to change everything to PrecursorIonType?
            peakAnnotation.set(treeFragment, graphPeak.toAnnotatedPeak(treeFragment.getFormula(), PrecursorIonType.getPrecursorIonType(treeFragment.getIonization()), originalGraph.getAnnotation(SpectralRecalibration.class, SpectralRecalibration::none)));

            if (ms1IsoAno!=null && ms1IsoAno.get(graphFragment) != null) {
                treeMs1IsoAno.set(treeFragment, ms1IsoAno.get(graphFragment));
            }

        }

        // TODO: HIER STIMMT WAS NICHT!!!!!!!!!!!!!!!!!!!
        System.out.println("TODO: HIER STIMMT WAS NICHT!!!!!!!!!!!!!!!!!!!s");
        // add isotopes
        ExtractedIsotopePattern extr = pinput.getAnnotation(ExtractedIsotopePattern.class, null);
        double rootIso = 0d;
        if (extr!=null) {
            final Fragment f = tree.getRoot();
            final IsotopePattern p = extr.getExplanations().get(f.getFormula());
            if (p!=null) {
                treeMs1IsoAno.set(f, new Ms1IsotopePattern(p.getPattern(), p.getScore()));
                treeScoring.setIsotopeMs1Score(treeScoring.getIsotopeMs1Score() + p.getScore());
                rootIso = p.getScore();
                tree.setAnnotation(IsotopePattern.class, p);
            }
        }

        treeScoring.setRootScore(originalGraph.getLoss(originalGraph.getRoot(), tree.getRoot().getFormula()).getWeight() - rootIso);
        treeScoring.setOverallScore(treeScoring.getRootScore() + overallScore + rootIso);
        treeScoring.setIsotopeMs1Score(rootIso);
        // add statistics
        treeScoring.setExplainedIntensity(getIntensityRatioOfExplainedPeaks(tree));
        treeScoring.setExplainedIntensityOfExplainablePeaks(getIntensityRatioOfExplainablePeaks(tree));
        treeScoring.setRatioOfExplainedPeaks((double)tree.numberOfVertices() / (double)originalGraph.getAnnotationOrThrow(ProcessedInput.class).getMergedPeaks().size());


    }

     public GraphReduction getReduction() {
        return reduction;
    }

    public void setReduction(GraphReduction reduction) {
        this.reduction = reduction;
    }

    public FGraph buildGraphWithoutReduction(ProcessedInput input, Decomposition candidate) {
        return buildGraphWithoutReduction(input,candidate,true);
    }

    private FGraph buildGraphWithoutReduction(ProcessedInput input, Decomposition candidate, boolean topologicalSort) {
        // build Graph
        FGraph graph = graphBuilder.fillGraph(
                graphBuilder.addRoot(graphBuilder.initializeEmptyGraph(input),
                        input.getParentPeak(), Collections.singletonList(candidate)));
        graph.addAliasForFragmentAnnotation(ProcessedPeak.class, Peak.class);
        graph = performGraphScoring(graph);
        if (topologicalSort) {
            graph.sortTopological();
        }
        return graph;
    }

    public FGraph buildGraph(ProcessedInput input, Decomposition candidate) {
        return performGraphReduction(buildGraphWithoutReduction(input,candidate,reduction==null),0d);
    }

    public FGraph performGraphReduction(FGraph fragments, double lowerbound) {
        if(reduction==null) return fragments;
        return reduction.reduce(fragments, lowerbound);
    }

    /*
    public FGraph buildGraph(ProcessedInput input, List<ProcessedPeak> parentPeaks, List<List<Scored<MolecularFormula>>> candidatesPerParentPeak) {
        throw new RuntimeException("Not supported yet"); // implement the possibility to allow several ion types per
        // within the same graph

        // build Graph
        FGraph graph = graphBuilder.initializeEmptyGraph(input);
        for (int i = 0; i < parentPeaks.size(); ++i) {
            graph = graphBuilder.addRoot(graph, parentPeaks.get(i), candidatesPerParentPeak.get(i));
        }
        return performGraphReduction(performGraphScoring(graphBuilder.fillGraph(graph)));

    }
*/
    /**
     * @return the relative amount of intensity that is explained by this tree, considering only
     * peaks that have an explanation for the hypothetical precursor ion of the tree
     */
    public double getIntensityRatioOfExplainablePeaks(FTree tree) {
        double treeIntensity = 0d, maxIntensity = 0d;
        final FragmentAnnotation<ProcessedPeak> pp = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        for (Fragment f : tree.getFragmentsWithoutRoot()) treeIntensity += pp.get(f).getRelativeIntensity();
        final ProcessedInput input = tree.getAnnotationOrThrow(ProcessedInput.class);
        final PeakAnnotation<DecompositionList> decomp = input.getPeakAnnotationOrThrow(DecompositionList.class);
        final MolecularFormula parent = tree.getRoot().getFormula();
        eachPeak:
        for (ProcessedPeak p : input.getMergedPeaks())
            if (p != input.getParentPeak()) {
                for (Scored<MolecularFormula> f : decomp.get(p).getDecompositions()) {
                    if (parent.isSubtractable(f.getCandidate())) {
                        maxIntensity += p.getRelativeIntensity();
                        continue eachPeak;
                    }
                }
            }
        if (maxIntensity==0) return 0;
        return treeIntensity / maxIntensity;
    }

    /**
     * @return the relative amount of intensity that is explained by this tree
     */
    public double getIntensityRatioOfExplainedPeaks(ProcessedInput input, FTree tree) {
        double treeIntensity = 0d, maxIntensity = 0d;
        FragmentAnnotation<ProcessedPeak> pp = tree.getFragmentAnnotationOrNull(ProcessedPeak.class);
        if (pp==null) {
            pp = tree.getOrCreateFragmentAnnotation(ProcessedPeak.class);
            for (Fragment f : tree) {
                pp.set(f, input.getMergedPeaks().get(f.getColor()));
            }
        }
        for (Fragment f : tree.getFragmentsWithoutRoot()) {
            // just for the case that spectrum parent != tree root (e.g. in-source fragments)
            if (pp.get(f)!=input.getParentPeak())
                treeIntensity += pp.get(f).getRelativeIntensity();
        }
        final PeakAnnotation<DecompositionList> decomp = input.getPeakAnnotationOrThrow(DecompositionList.class);
        final MolecularFormula parent = tree.getRoot().getFormula();
        eachPeak:
        for (ProcessedPeak p : input.getMergedPeaks())
            if (p != input.getParentPeak()) {
                maxIntensity += p.getRelativeIntensity();
            }
        if (maxIntensity==0) return 0;
        return treeIntensity / maxIntensity;
    }
    public double getIntensityRatioOfExplainedPeaksFromUnanotatedTree(ProcessedInput input, FTree tree) {
        final double[] fragmentMasses = new double[tree.numberOfVertices()];
        int k=0;
        for (Fragment f : tree) {
            fragmentMasses[k++] = f.getIonization().addToMass(f.getFormula().getMass());
        }
        Arrays.sort(fragmentMasses);
        double explainedIntensity = 0d, totalIntensity = 0d;
        for (ProcessedPeak p : input.getMergedPeaks()) {
            if (p == input.getParentPeak()) continue;
            totalIntensity += p.getRelativeIntensity();
            int index = Arrays.binarySearch(fragmentMasses, p.getMass());
            if (index < 0) {
                index = -(index+1);
                double diff1,diff2,diff3;
                if (index < fragmentMasses.length) {
                    diff1 = Math.abs(fragmentMasses[index] - p.getMass());
                    diff2 = index<=0 ? Double.POSITIVE_INFINITY :  Math.abs(fragmentMasses[index-1] - p.getMass());
                    diff3 = index+1>=fragmentMasses.length ? Double.POSITIVE_INFINITY : Math.abs(fragmentMasses[index+1] - p.getMass());
                    if (diff2 < diff1 && diff2 <= diff3) --index;
                    else if (diff3 < diff1 && diff3 <= diff2) ++index;
                } else if (--index < 0)
                    continue;
                if (input.getAnnotation(MS2MassDeviation.class, null).allowedMassDeviation.inErrorWindow(p.getMass(), fragmentMasses[index])) {
                    explainedIntensity += p.getRelativeIntensity();
                }
            }
        }
        if (totalIntensity==0) return 0;
        return explainedIntensity/totalIntensity;
    }
    public double getIntensityRatioOfExplainedPeaks(FTree tree) {
        final ProcessedInput input = tree.getAnnotationOrThrow(ProcessedInput.class);
        return getIntensityRatioOfExplainedPeaks(input,tree);
    }

    /**
     * Adds Score annotations to vertices and losses of the tree for every scoring method.
     * As the single scores are forgotten during tree computation, they have to be computed again.
     * @param tree
     */
    public double recalculateScores(ProcessedInput input, FTree tree) {
        final Iterator<Loss> edges = tree.lossIterator();
        final FragmentAnnotation<ProcessedPeak> peakAno = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);

        final Object[] preparedLoss = new Object[lossScorers.size()];
        final Object[] preparedFrag = new Object[decompositionScorers.size()];
        final PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);
        final FragmentAnnotation<Ms2IsotopePattern> msMsIso = tree.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        final FragmentAnnotation<Ms1IsotopePattern> msIso1 = tree.getFragmentAnnotationOrNull(Ms1IsotopePattern.class);
        final String[] fragmentScores;
        final String[] lossScores;
        final String[] rootScores;

        final Score.HeaderBuilder fragmentHeader = Score.defineScoring(), lossHeader = Score.defineScoring(), rootHeader = Score.defineScoring();
        {
            final ArrayList<String> fragScores = new ArrayList<String>();
            for (PeakScorer peakScorer : this.fragmentPeakScorers) {
                fragScores.add(fragmentHeader.define(getScoringMethodName(peakScorer)));
            }
            int i=0;
            for (DecompositionScorer peakScorer : this.decompositionScorers) {
                fragScores.add(fragmentHeader.define(getScoringMethodName(peakScorer)));
                preparedFrag[i++] = peakScorer.prepare(input);
            }
            if (msMsIso!=null) fragScores.add(fragmentHeader.define(getScoringMethodName(new IsotopePatternInMs2Scorer())));
            fragmentScores = fragScores.toArray(new String[fragScores.size()]);
        }
        {
            final ArrayList<String> lScores = new ArrayList<String>();
            for (PeakPairScorer lossScorer : this.peakPairScorers) {
                lScores.add(lossHeader.define(getScoringMethodName(lossScorer)));
            }
            int i=0;
            for (LossScorer lossScorer : this.lossScorers) {
                lScores.add(lossHeader.define(getScoringMethodName(lossScorer)));
                preparedLoss[i++] = lossScorer.prepare(input);
            }
            lossScores = lScores.toArray(new String[lScores.size()]);
        }
        {
            final ArrayList<String> fragScores = new ArrayList<String>();
            for (DecompositionScorer peakScorer : this.rootScorers) {
                fragScores.add(rootHeader.define(getScoringMethodName(peakScorer)));
            }
            rootScores = fragScores.toArray(new String[fragScores.size()]);
        }

        final FragmentAnnotation<Score> fAno = tree.getOrCreateFragmentAnnotation(Score.class);
        final LossAnnotation<Score> lAno = tree.getOrCreateLossAnnotation(Score.class);
        final LossAnnotation<InsourceFragmentation> isInsource = tree.getOrCreateLossAnnotation(InsourceFragmentation.class);
        final double[][] pseudoMatrix = new double[2][2];
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            if (isInsource.get(loss)!= null && isInsource.get(loss).isInsource()) continue;
            final Fragment u = loss.getSource();
            final Fragment v = loss.getTarget();

            // add loss scores
            final Score.ScoreAssigner lscore = lossHeader.score();
            int k=0;
            for (int i=0; i < peakPairScorers.size(); ++i) {
                pseudoMatrix[0][0]=pseudoMatrix[0][1]=pseudoMatrix[1][0]=pseudoMatrix[1][1]=0.0d;
                peakPairScorers.get(i).score(Arrays.asList(peakAno.get(v), peakAno.get(u)), input,pseudoMatrix);
                lscore.set(lossScores[k++], pseudoMatrix[1][0]);
            }
            for (int i=0; i < lossScorers.size(); ++i) {
                lscore.set(lossScores[k++], lossScorers.get(i).score(loss, input, preparedLoss[i]));
            }
            lAno.set(loss, lscore.done());

            // add fragment scores
            final Score.ScoreAssigner fscore = fragmentHeader.score;;
            k=0;
            for (int i=0; i < fragmentPeakScorers.size(); ++i) {
                pseudoMatrix[0][0]=pseudoMatrix[0][1]=pseudoMatrix[1][0]=pseudoMatrix[1][1]=0.0d;
                fragmentPeakScorers.get(i).score(Arrays.asList(peakAno.get(v)), input,pseudoMatrix[0]);
                fscore.set(fragmentScores[k++], pseudoMatrix[0][0]);
            }
            for (int i=0; i < decompositionScorers.size(); ++i) {
                fscore.set(fragmentScores[k++], ((DecompositionScorer<Object>) decompositionScorers.get(i)).score(v.getFormula(),v.getIonization(), peakAno.get(v), input, preparedFrag[i]));
            }

            double isoScore = 0d;
            if (msMsIso!=null && msMsIso.get(v)!=null) {
                isoScore += msMsIso.get(v).getScore();
            }
            if (isoScore>0)
                fscore.set(fragmentScores[k++], isoScore);

            fAno.set(v, fscore.done());
        }
        // set root
        Fragment root = tree.getRoot();
        if (root.getOutDegree()==1 && isInsource.get(root.getOutgoingEdge(0))!=null && isInsource.get(root.getOutgoingEdge(0)).isInsource()) {
            root = root.getChildren(0);
        }
        final Score.ScoreAssigner rootScore = rootHeader.score();
        for (int k=0; k < rootScorers.size(); ++k) {
            final Object prepared = rootScorers.get(k).prepare(input);
            final double score = ((DecompositionScorer<Object>)rootScorers.get(k)).score(root.getFormula(),root.getIonization(), peakAno.get(root), input, prepared);
            rootScore.set(rootScores[k], score);
        }
        fAno.set(root, rootScore.done());

        // check scoreSum
        double scoreSum = 0d;
        for (Loss l : tree.losses()) {
            Score s = lAno.get(l);
            if (s==null) continue;
            scoreSum += s.sum();
            s = fAno.get(l.getTarget());
            scoreSum += s.sum();
        }
        scoreSum += fAno.get(root).sum();
        return scoreSum;

    }

    public static String getScoringMethodName(Object instance) {
        Class<? extends Object> someClass = instance.getClass();
        if (someClass.isAnnotationPresent(Called.class)) {
            return someClass.getAnnotation(Called.class).value();
        } else return parameterHelper.toClassName(someClass);
    }

    public FGraph performGraphScoring(ProcessedInput input, FGraph graph) {
        // score graph
        final Iterator<Loss> edges = graph.lossIterator();
        final Scoring scoring = input.getAnnotationOrThrow(Scoring.class);
        final double[] peakScores = scoring.getPeakScores();
        final double[][] peakPairScores = scoring.getPeakPairScores();
        final LossScorer[] lossScorers = this.lossScorers.toArray(new LossScorer[this.lossScorers.size()]);
        final Object[] precomputeds = new Object[lossScorers.length];
        final ScoredFormulaMap map = graph.getAnnotationOrThrow(ScoredFormulaMap.class);
        final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        for (int i = 0; i < precomputeds.length; ++i) precomputeds[i] = lossScorers[i].prepare(input);
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            final Fragment u = loss.getSource();
            final Fragment v = loss.getTarget();
            // take score of molecular formula
            double score = map.get(new IonizedMolecularFormula(v.getFormula(), v.getIonization()));
            assert !Double.isInfinite(score);
            // add it to score of the peak
            score += peakScores[peakAno.get(v).getIndex()];
            assert !Double.isInfinite(score);
            // add it to the score of the peak pairs
            if (!u.isRoot())
                score += peakPairScores[peakAno.get(u).getIndex()][peakAno.get(v).getIndex()]; // TODO: Umdrehen!
            assert !Double.isInfinite(score);
            // add the score of the loss
            if (!u.isRoot())
                for (int i = 0; i < lossScorers.length; ++i)
                    score += lossScorers[i].score(loss, input, precomputeds[i]);
            assert !Double.isInfinite(score);
            loss.setWeight(score);
        }
        scoreIsotopesInMs2(input, graph);

        return graph;
    }

    public boolean isScoringIsotopes(ProcessedInput input) {
        final boolean isBrukerMaxis = input.getAnnotation(MsInstrumentation.class, () -> MsInstrumentation.Unknown).hasIsotopesInMs2();
        switch (isotopeInMs2Handling) {
            case IGNORE: return false;
            case IF_NECESSARY:
                IsolationWindow isolationWindow = input.getAnnotation(IsolationWindow.class, null);
                if (isolationWindow!=null && isolationWindow.getRightBorder()>0.9){
                    //isolation window includes the +1 peak
                    return true;
                }
            case BRUKER_IF_NECESSARY:
                throw new UnsupportedOperationException("Not supported yet");
            case BRUKER_ONLY:
                if (!isBrukerMaxis) return false;
            case ALWAYS:
            default:
        }
        return true;
    }

    private void scoreIsotopesInMs2(ProcessedInput input, FGraph graph) {

//        isoInMs2Scorer.scoreFromMs1(input, graph);
        if (isScoringIsotopes(input))
            isoInMs2Scorer.score(input, graph);

    }

    private void addSyntheticParent(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks, double parentmass) {
        final ProcessedPeak syntheticParent = new ProcessedPeak();
        syntheticParent.setMass(parentmass);
        processedPeaks.add(syntheticParent);
    }

    public void setIsotopeHandling(IsotopeInMs2Handling handling) {
        this.isotopeInMs2Handling = handling;
    }

    /*

    Merging:
        - 1. lösche alle Peaks die zu nahe an einem anderen Peak im selben Spektrum sind un geringe Intensität
        - 2. der Peakmerger bekommt nur Peak aus unterschiedlichen Spektren und mergt diese
        - 3. Nach der Decomposition läuft man alle peaks in der Liste durch. Wenn zwischen zwei
             Peaks der Abstand zu klein wird, werden diese Peaks disjunkt, in dem die doppelt vorkommenden
             Decompositions auf einen peak (den mit der geringeren asseabweichung) eindeutig verteilt werden.

     */


    ProcessedInput postProcess(PostProcessor.Stage stage, ProcessedInput input) {
        for (PostProcessor proc : postProcessors) {
            if (proc.getStage() == stage) {
                input = proc.process(input);
            }
        }
        return input;
    }

    //////////////////////////////////////////
    //        GETTER/SETTER
    //////////////////////////////////////////


    public List<Ms2ExperimentValidator> getInputValidators() {
        return inputValidators;
    }

    public void setInputValidators(List<Ms2ExperimentValidator> inputValidators) {
        this.inputValidators = inputValidators;
    }

    public Warning getValidatorWarning() {
        return validatorWarning;
    }

    public void setValidatorWarning(Warning validatorWarning) {
        this.validatorWarning = validatorWarning;
    }

    public boolean isRepairInput() {
        return repairInput;
    }

    public void setRepairInput(boolean repairInput) {
        this.repairInput = repairInput;
    }

    public NormalizationType getNormalizationType() {
        return normalizationType;
    }

    public void setNormalizationType(NormalizationType normalizationType) {
        this.normalizationType = normalizationType;
    }

    public PeakMerger getPeakMerger() {
        return peakMerger;
    }

    public void setPeakMerger(PeakMerger peakMerger) {
        this.peakMerger = peakMerger;
    }

    public List<DecompositionScorer<?>> getDecompositionScorers() {
        return decompositionScorers;
    }

    public void setDecompositionScorers(List<DecompositionScorer<?>> decompositionScorers) {
        this.decompositionScorers = decompositionScorers;
    }

    public List<DecompositionScorer<?>> getRootScorers() {
        return rootScorers;
    }

    public void setRootScorers(List<DecompositionScorer<?>> rootScorers) {
        this.rootScorers = rootScorers;
    }

    public List<LossScorer> getLossScorers() {
        return lossScorers;
    }

    public void setLossScorers(List<LossScorer> lossScorers) {
        this.lossScorers = lossScorers;
    }

    public List<PeakPairScorer> getPeakPairScorers() {
        return peakPairScorers;
    }

    public void setPeakPairScorers(List<PeakPairScorer> peakPairScorers) {
        this.peakPairScorers = peakPairScorers;
    }

    public List<PeakScorer> getFragmentPeakScorers() {
        return fragmentPeakScorers;
    }

    public void setFragmentPeakScorers(List<PeakScorer> fragmentPeakScorers) {
        this.fragmentPeakScorers = fragmentPeakScorers;
    }

    public List<Preprocessor> getPreprocessors() {
        return preprocessors;
    }

    public void setPreprocessors(List<Preprocessor> preprocessors) {
        this.preprocessors = preprocessors;
    }

    public List<PostProcessor> getPostProcessors() {
        return postProcessors;
    }

    public void setPostProcessors(List<PostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    public TreeBuilder getTreeBuilder() {
        if (treeBuilder==null) {
            setTreeBuilder(TreeBuilderFactory.getInstance().getTreeBuilder());
        }
        return treeBuilder;
    }

    public void setTreeBuilder(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    public MassToFormulaDecomposer getDecomposerFor(ChemicalAlphabet alphabet) {
        return decomposers.getDecomposer(alphabet);
    }

    public DecomposerCache getDecomposerCache() {
        return decomposers;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        setInitial();
        fillList(preprocessors, helper, document, dictionary, "preProcessing");
        fillList(postProcessors, helper, document, dictionary, "postProcessing");
        fillList(rootScorers, helper, document, dictionary, "rootScorers");
        fillList(decompositionScorers, helper, document, dictionary, "fragmentScorers");
        fillList(fragmentPeakScorers, helper, document, dictionary, "peakScorers");
        fillList(peakPairScorers, helper, document, dictionary, "peakPairScorers");
        fillList(lossScorers, helper, document, dictionary, "lossScorers");
        if (document.hasKeyInDictionary(dictionary, "isotopesInMs2")) {
            this.isoInMs2Scorer = (IsotopePatternInMs2Scorer) helper.unwrap(document, document.getFromDictionary(dictionary,"isotopesInMs2"));
        }
        peakMerger = (PeakMerger) helper.unwrap(document, document.getFromDictionary(dictionary, "merge"));
    }

    private <T, G, D, L> void fillList(List<T> list, ParameterHelper helper, DataDocument<G, D, L> document, D dictionary, String keyName) {
        if (!document.hasKeyInDictionary(dictionary, keyName)) return;
        Iterator<G> ls = document.iteratorOfList(document.getListFromDictionary(dictionary, keyName));
        while (ls.hasNext()) {
            final G l = ls.next();
            list.add((T) helper.unwrap(document, l));
        }
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (Preprocessor p : preprocessors) document.addToList(list, helper.wrap(document, p));
        document.addListToDictionary(dictionary, "preProcessing", list);
        list = document.newList();
        for (PostProcessor p : postProcessors) document.addToList(list, helper.wrap(document, p));
        document.addListToDictionary(dictionary, "postProcessing", list);
        list = document.newList();
        for (DecompositionScorer s : rootScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "rootScorers", list);
        list = document.newList();
        for (DecompositionScorer s : decompositionScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "fragmentScorers", list);
        list = document.newList();
        for (PeakScorer s : fragmentPeakScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "peakScorers", list);
        list = document.newList();
        for (PeakPairScorer s : peakPairScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "peakPairScorers", list);
        list = document.newList();
        for (LossScorer s : lossScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "lossScorers", list);
        document.addToDictionary(dictionary, "isotopesInMs2", helper.wrap(document, isoInMs2Scorer));
        document.addToDictionary(dictionary, "merge", helper.wrap(document, peakMerger));
    }
}
