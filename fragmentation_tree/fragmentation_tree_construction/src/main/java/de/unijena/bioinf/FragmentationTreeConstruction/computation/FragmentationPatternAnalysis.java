
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
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
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Scoring;
import de.unijena.bioinf.IsotopePatternAnalysis.ExtractedIsotopePattern;
import de.unijena.bioinf.MassDecomposer.Chemistry.AddDeNovoDecompositionsToWhiteset;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.NonEmptyFormulaValidator;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.PeakAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TLongProcedure;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
 * <p>
 * Steps 1-9 are bundled within the method FragmentationPatternAnalysis#preprocessing
 * <p>
 * Now for each Molecular formula candidate:
 * 10. Compute Fragmentation Graph
 * 11. Score losses and vertices in the graph
 * 12. Compute Fragmentation tree
 * 13. Recalibrate tree
 * 14. Might repeat all steps with recalibration function
 * 15. Postprocess tree
 * <p>
 * Steps 10-15 are bundled within the method FragmentationPatternAnalysis#computeTree
 * <p>
 * You can run each step individually. However, you have to run step 1. first to get a ProcessedInput object.
 * This object contains the Ms2Experiment input and stores all intermediate values during the computation.
 * <p>
 * Some (or, honestly, most) steps rely on certain properties and intermediate values computed in previous steps.
 * So you have to be very careful when running a step separately. The recommended way is to run the whole pipeline.
 */
public class FragmentationPatternAnalysis implements Parameterized, Cloneable {
    private List<Ms2ExperimentValidator> inputValidators;
    private Warning validatorWarning;
    private boolean repairInput;
    private DecomposerCache decomposers;
    private List<DecompositionScorer<?>> decompositionScorers;
    private List<DecompositionScorer<?>> rootScorers;
    private List<LossScorer> lossScorers;
    private List<PeakPairScorer> peakPairScorers;
    private List<PeakScorer> fragmentPeakScorers;

    private List<FragmentScorer<?>> fragmentScorers;
    private List<GeneralGraphScorer> generalGraphScorers;

    private GraphBuilder graphBuilder;
    private TreeBuilder treeBuilder;
    private GraphReduction reduction;

    private static ParameterHelper parameterHelper = ParameterHelper.getParameterHelper();



    private final LinkedHashMap<Class<?>, SiriusPlugin> siriusPlugins = new LinkedHashMap<>();

    public void registerPlugin(SiriusPlugin plugin) {
        if (!siriusPlugins.containsKey(plugin.getClass())) {
            siriusPlugins.put(plugin.getClass(), plugin);
            plugin.initializePlugin(new SiriusPlugin.PluginInitializer(this));
        }
    }

    /**
     * this removes the plugin but not any scorers the plugin added to the {@link FragmentationPatternAnalysis}.
     * @param pluginClass
     * @return true if a plugin of this class existed
     */
    public boolean unregisterPlugin(Class<? extends SiriusPlugin> pluginClass) {
        return siriusPlugins.remove(pluginClass) != null;
    }

    public boolean hasPlugin(Class<? extends SiriusPlugin> pluginClass) {
        return siriusPlugins.containsKey(pluginClass);
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
        siriusPlugins.values().forEach(p->p.afterPreprocessing(input));
        siriusPlugins.values().forEach(p->p.beforeDecomposing(input));

        final PeriodicTable PT = PeriodicTable.getInstance();
        FormulaConstraints constraints = input.getAnnotationOrNull(FormulaConstraints.class);
        final Ms2Experiment experiment = input.getExperimentInformation();
        PossibleAdducts possibleAdducts = input.getAnnotationOrThrow(PossibleAdducts.class);
        final double parentMass;
        // if parent peak stems from MS1, use MS1 mass deviation instead
        final Deviation parentDeviation;
        if (input.getAnnotation(ExtractedIsotopePattern.class, ExtractedIsotopePattern::none).hasPattern()) {
            parentDeviation = input.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
            parentMass = input.getAnnotation(ExtractedIsotopePattern.class, ExtractedIsotopePattern::none).getPattern().getMzAt(0);
        } else {
            parentDeviation = input.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;
            parentMass = input.getParentPeak().getMass();
        }

        Whiteset whiteset = input.getAnnotationOrNull(Whiteset.class);
        if (whiteset == null || (!whiteset.isFinalized() && whiteset.stillRequiresDeNovoToBeAdded())) {
            //should only trigger if MS1 analysis has not already been performed (MS2 data only).
            //Hence, these de novo decompositions are not influcends by "beforeDecomposing" such as the filter step in IsotopePatternInMs1Plugin
            whiteset = AddDeNovoDecompositionsToWhiteset.createNewWhitesetWithDenovoAdded(whiteset, parentMass, parentDeviation, possibleAdducts, constraints, decomposers);
            input.setAnnotation(Whiteset.class, whiteset);
            //after this step, the whiteset should be fixed. All plugins were run,
        }

        // sort again...
        final ArrayList<ProcessedPeak> processedPeaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size() - 1);
        // decompose peaks
        final PeakAnnotation<DecompositionList> decompositionList = input.getOrCreatePeakAnnotation(DecompositionList.class);
        final Deviation fragmentDeviation = input.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;

        final List<MolecularFormula> pmds;
        final List<Decomposition> decomps = new ArrayList<>();

        // we use "ORIGINAL" input here, such that we do not use molecular formula information from InChI
        // this is just for evaluation purpose! We want to be able to evaluate the formula identification rate
        // without having to remove any field that is not directly related to formula but helps to infer the formula.
        if (input.getOriginalInput().getMolecularFormula()!=null){ //todo ElementFilter: can we get rid of this?
            //always use formula. don't look at mass dev.
            final MolecularFormula formula = input.getExperimentInformation().getMolecularFormula();
            final PrecursorIonType ionType = experiment.getPrecursorIonType();

            // HACKY. We have to get that right at some point! :(
            //Decomposition decomposition = Whiteset.ofMeasuredOrNeutral(Set.of(formula)).applyIonizationBothWays(experiment.getPrecursorIonType()).resolve(parentPeak.getMass(), parentDeviation, Arrays.asList(ionType)).get(0);

            Decomposition decomposition = new Decomposition(ionType.neutralMoleculeToMeasuredNeutralMolecule(formula), ionType.getIonization(), 0d);
            decomps.add(decomposition);
            pmds = new ArrayList<>();
            pmds.add(decomposition.getCandidate());

            if (!parentDeviation.inErrorWindow(parentPeak.getMass(), ionType.neutralMassToPrecursorMass(formula.getMass()))){
                validatorWarning.warn("Specified precursor molecular formula does not fall into given m/z error window. "
                        +formula.formatByHill()+" for m/z "+parentPeak.getMass()+" and ionization "+ionType);
            }
        } else  {
            pmds = new ArrayList<>();
            final Collection<PrecursorIonType> ionTypes;
            if (experiment.getPrecursorIonType().isIonizationUnknown()) {
                ionTypes = input.getAnnotationOrThrow(PossibleAdducts.class).getAdducts();
            } else {
                ionTypes = Arrays.asList(experiment.getPrecursorIonType()); //todo ElementFilter: can we remove this special case?
            }
            List<Decomposition> forms = whiteset.resolve(parentMass, parentDeviation, ionTypes);
            decomps.addAll(forms);
            for (Decomposition d : decomps) pmds.add(d.getCandidate());
            // extend formula constraints such that we can decompose fragment peaks
            constraints = constraints.getExtendedConstraints(FormulaConstraints.allSubsetsOf(forms.stream().map(SScored::getCandidate).collect(Collectors.toList())));
        }

        // remove duplicate pmds
        {
            Set<Decomposition> usedDecomps = new HashSet<>();
            Iterator<Decomposition> decompositionIterator = decomps.iterator();
            while( decompositionIterator.hasNext()) {
                if (!usedDecomps.add(decompositionIterator.next())) {
                    decompositionIterator.remove();
                }
            }
        }

        //extract the used ion modes
        //todo ElementFilter: check new way of retrieving ionMdoes. Not using the Ionmodes you get from PossibleAdducts anymore. Because filtering (ElGordo, isotope pattern) may remove all formulas of a specific io mod
        final Set<IonMode> ionModes = decomps.stream().filter(d -> d.getIon() instanceof IonMode).map(d -> (IonMode) d.getIon()).collect(Collectors.toSet());

        // may split pmds if multiple alphabets are present
        final List<MassToFormulaDecomposer> decomposers = new ArrayList<>();
        final List<FormulaConstraints> constraintList = new ArrayList<>();
        getDecomposersFor(pmds, constraints, decomposers, constraintList);

        Set<Ionization> ionModeSet = new HashSet<>(ionModes);


        for (IonMode pmdIonMode : ionModes.toArray(new IonMode[0])) {
            for (SiriusPlugin plugin : siriusPlugins.values()) {
                plugin.addPossibleIonModesToGraph(input, pmdIonMode, ionModeSet);
            }
        }

        final SpectralRecalibration recalibration = input.getAnnotation(SpectralRecalibration.class,SpectralRecalibration::none);

        int j = 0;
        for (ProcessedPeak peak : processedPeaks.subList(0, processedPeaks.size() - 1)) {
            peak.setIndex(j++);
            final List<Decomposition> decompositions = new ArrayList<>();
            final double mz = recalibration.recalibrate(peak);
            for (Ionization ion : ionModeSet) {
                final double mass = ion.subtractFromMass(mz);
                if (mass > 0) {
                    final HashSet<MolecularFormula> formulas = new HashSet<>();
                    for (int D=0; D < decomposers.size(); ++D) {
                        formulas.addAll(decomposers.get(D).decomposeToFormulas(mz, ion, fragmentDeviation.absoluteFor(peak.getMass()), constraintList.get(D)));
                    }
                    for (MolecularFormula f : formulas){
                        decompositions.add(new Decomposition(f, ion, 0d));
                    }
                }
            }

            DecompositionList peakDecompList = new DecompositionList(decompositions);
            for (SiriusPlugin plugin : siriusPlugins.values()) {
                peakDecompList = plugin.transformDecompositionList(input, peak, peakDecompList);
            }

            decompositionList.set(peak, peakDecompList);
        }
        parentPeak.setIndex(processedPeaks.size() - 1);
        decompositionList.set(parentPeak, new DecompositionList(decomps));
        assert parentPeak == processedPeaks.get(processedPeaks.size() - 1);
        // important: for each two peaks which are within 2*massrange:
        //  => make decomposition list disjoint
        final double[] recalibrated = new double[processedPeaks.size()];
        for (int i=0; i < recalibrated.length; ++i) recalibrated[i] = recalibration.recalibrate(input.getMergedPeaks().get(i));
        final Deviation window = fragmentDeviation.multiply(2);
        for (int i = 1; i < processedPeaks.size() - 1; ++i) {
            if (window.inErrorWindow(recalibrated[i], recalibrated[i-1])) {
                decompositionList.get(processedPeaks.get(i-1)).disjoin(decompositionList.get(processedPeaks.get(i)), recalibrated[i-1], recalibrated[i]);
            }
        }
        input.setAnnotation(DecompositionList.class, decompositionList.get(parentPeak));
        return input;
    }

    private void getDecomposersFor(List<MolecularFormula> pmds, FormulaConstraints constraint, List<MassToFormulaDecomposer> decomposers, List<FormulaConstraints> constraintList) {
        if (pmds.size()==1) {
            FormulaConstraints fc = FormulaConstraints.allSubsetsOf(pmds.get(0));
            fc.addFilter(new NonEmptyFormulaValidator());
            constraintList.add(fc);
            decomposers.add(getDecomposerFor(new ChemicalAlphabet(pmds.get(0).elementArray())));
            return;
        }
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
            cs.addFilter(new NonEmptyFormulaValidator());
            constraintList.add(cs);
            decomposers.add(decomposer);
        }
    }

    /**
     * Step 7: Peak Scoring
     * Scores each peak. Expects a decomposition list
     */
    public ProcessedInput performPeakScoring(ProcessedInput input) {
        siriusPlugins.values().forEach(p->p.beforePeakScoring(input));
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
                        assert Double.isFinite(score) : scorer.getClass().getSimpleName();
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
                    if (!Double.isFinite(score)) {
                        throw new RuntimeException(score + " is not finite. For root " + f.getCandidate() + " with m/z = " + input.getParentPeak().getMass() + ".\nWhiteset = " + input.getAnnotation(Whiteset.class, Whiteset::empty).toString());
                    }
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

        analysis.generalGraphScorers = new ArrayList<>();
        analysis.fragmentScorers = new ArrayList<>();


        return analysis;
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
        this.repairInput = true;
        this.decompositionScorers = new ArrayList<>();
        this.rootScorers = new ArrayList<>();
        this.peakPairScorers = new ArrayList<>();
        this.fragmentPeakScorers = new ArrayList<>();
        this.graphBuilder = new SubFormulaGraphBuilder();
        this.lossScorers = new ArrayList<>();
        this.fragmentScorers = new ArrayList<>();
        this.generalGraphScorers = new ArrayList<>();
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
     */
    @Provides(PrecursorIonType.class)
    @Provides(SpectralRecalibration.class)
    @Provides(value=AnnotatedPeak.class, in=Fragment.class)
    @Requires(value=DecompositionList.class, in = ProcessedPeak.class)
    private void transferDefaultAnotationsFromGraphToTree(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2tree) {

        // Ionization
        tree.setAnnotation(PrecursorIonType.class, graph.getAnnotationOrNull(PrecursorIonType.class));

        // Recalibration
        final SpectralRecalibration recalibration = input.getAnnotation(SpectralRecalibration.class, SpectralRecalibration::none);

        // Peak information
        for (Fragment treeFragment : tree) {
            final Fragment graphFragment = graph2tree.mapRightToLeft(treeFragment);
            final ProcessedPeak peak = input.getMergedPeaks().get(graphFragment.getPeakId());
            tree.setAnnotation(AnnotatedPeak.class, peak.toAnnotatedPeak(graphFragment.getFormula(), PrecursorIonType.getPrecursorIonType(graphFragment.getIonization()), recalibration));
        }
    }


/*
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
*/


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
        siriusPlugins.values().forEach(p->p.beforeGraphBuilding(input));
        final LossValidator validator = new LossValidator.Combination(siriusPlugins.values().stream().map(p->p.filterLossesInGraph(input, candidate)).filter(p->p!=null).collect(Collectors.toList()));
        final HashSet<Ionization> ionModes = new HashSet<>();
        ionModes.add(candidate.getIon());
        for (SiriusPlugin plugin : siriusPlugins.values()) {
            plugin.addPossibleIonModesToGraph(input, candidate.getIon(), ionModes);
        }
        final FGraph graph = graphBuilder.fillGraph(input,
                graphBuilder.addRoot(graphBuilder.initializeEmptyGraph(input),
                        input.getParentPeak(), Collections.singletonList(candidate)),ionModes,validator);
        graph.setAnnotation(PrecursorIonType.class, PrecursorIonType.getPrecursorIonType(candidate.getIon()));
        siriusPlugins.values().forEach(p->p.afterGraphBuilding(input,graph));
        siriusPlugins.values().forEach(p->p.transferAnotationsFromInputToGraph(input,graph));
        final FGraph scoredGraph = performGraphScoring(input, graph);
        siriusPlugins.values().forEach(p->p.afterGraphScoring(input,scoredGraph));
        if (topologicalSort) {
            ensureTopologicalOrder(input, graph);
        }
        return scoredGraph;
    }

    protected void ensureTopologicalOrder(ProcessedInput input, FGraph graph) {
         final List<Fragment> fs = new ArrayList<>(graph.getFragments());
         final List<ProcessedPeak> peaks = input.getMergedPeaks();
         final Fragment root = graph.getRoot();
         fs.sort(Comparator.comparingDouble(f->f==root ? Double.NEGATIVE_INFINITY : -peaks.get(f.getPeakId()).getMass()));
         graph.reorderVertices(fs);
         int color = 0;
         int previousColor = -2;
         for (Fragment f : fs) {
            if (f.getColor() != previousColor) {
                previousColor = f.getColor();
                ++color;
            }
            f.setColor(color);
         }
        /*
         final FragmentAnnotation<IsotopicMarker> marker = graph.getFragmentAnnotationOrNull(IsotopicMarker.class);

        final Fragment[] fs = graph.getFragments().toArray(new Fragment[graph.numberOfVertices()]);
        Arrays.sort(fs, new Comparator<Fragment>() {
            @Override
            public int compare(Fragment fragment, Fragment fragment2) {
                if (fragment == graph.getRoot()) return -1;
                else if (fragment2 == graph.getRoot()) return 1;
                else return Double.compare(input.getMergedPeaks().get(assignment)peak.get(fragment2).getMass(), peak.get(fragment).getMass());
            }
        });

        fragments.clear();
        // now assign colors to all fragments based on their mass
        int color = 0;
        int previousColor = -2;
        for (Fragment f : fs) {
            if (f.color != previousColor) {
                previousColor = f.color;
                ++color;
            }
            f.setColor(color);
        }



        // now reinsert fragments into the fragment list. But be careful that isotope peaks are inserted in reverse
        // order
        if (marker == null) {
            fragments.addAll(Arrays.asList(fs));
        } else {
            final TIntIntHashMap isoBuf = new TIntIntHashMap(16,0.75f,-1,-1);
            for (int i=0; i < fs.length; ++i) {
                final boolean isIsotope = marker.get(fs[i])!=null;
                if (isIsotope) {
                    // ensure linear chain structure of isotopes
                    if (fs[i].getOutDegree() > 1 || fs[i].getInDegree() != 1){
                        throw new RuntimeException("Bug in MSMS isotope analysis: Isotope peak should form a chain, but has in degree of " + fs[i].getInDegree() + " and out degree of " + fs[i].getOutDegree());
                    }
                    final Fragment g = fs[i].getParent();
                    if (marker.get(g)==null) {
                        // we found monoisotopic peak
                        isoBuf.put(g.vertexId, i);
                    }
                } else {
                    fragments.add(fs[i]);
                    if (!isoBuf.isEmpty()) {
                        final int fi = isoBuf.get(fs[i].vertexId);
                        if (fi >= 0) {
                            // add isotopes below this fragment
                            Fragment chain = fs[fi];
                            while (true) {
                                fragments.add(chain);
                                if (chain.outDegree==0) break;
                                else chain = chain.getChildren(0);
                            }
                            isoBuf.remove(fs[i].vertexId);
                        }
                    }
                }
            }
        }
        int id=0;
        for (Fragment f : fragments) {
            f.setVertexId(id++);
        }
        */
    }

    public FGraph buildGraph(ProcessedInput input, Decomposition candidate) {
        return performGraphReduction(buildGraphWithoutReduction(input,candidate,true),0d);
    }

    public FGraph performGraphReduction(FGraph fragments, double lowerbound) {
        // remove unsupported edges an fragments first
        removeNegativeInfinityEdges(fragments);

        boolean reduce = true;
        if (reduction == null) reduce = false;
        else {
            for (SiriusPlugin plugin : siriusPlugins.values()) {
                if (plugin.isGraphReductionForbidden(fragments)) {
                    reduce = false;
                    break;
                }
            }
        }

        if (reduce)
            fragments = reduction.reduce(fragments, lowerbound);

        return fragments;
    }

    private FGraph removeNegativeInfinityEdges(final FGraph fragments) {
        final ArrayList<Loss> todelete = new ArrayList<>();
        for (Loss l : fragments.losses()) {
            if (!Double.isFinite(l.getWeight())) {
                if (l.getWeight() > 0) {
                    LoggerFactory.getLogger(getClass()).warn("We do not support edges with infinite score. Delete edge " + l + ", even though it has positive infinite score.");
                }
                todelete.add(l);
            }
        }

        Set<Fragment> fragmentsToDelete = new HashSet<>();
        for (Loss l : todelete) {
            fragments.deleteLoss(l);
            if (l.getTarget().getInDegree() <= 0) {
                fragmentsToDelete.add(l.getTarget());
            }
        }

        while (!fragmentsToDelete.isEmpty()) {
            fragments.deleteFragmentsKeepTopologicalOrder(fragmentsToDelete);
            fragmentsToDelete = fragments.getFragments().stream()
                    .filter(f -> f.getInDegree() <= 0)
                    .filter(f -> !fragments.getRoot().equals(f))
                    .collect(Collectors.toSet());
        }
        return fragments;
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
                if (input.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation.inErrorWindow(p.getMass(), fragmentMasses[index])) {
                    explainedIntensity += p.getRelativeIntensity();
                }
            }
        }
        if (totalIntensity==0) return 0;
        return explainedIntensity/totalIntensity;
    }

    /**
     * Adds Score annotations to vertices and losses of the tree for every scoring method.
     * As the single scores are forgotten during tree computation, they have to be computed again.
     * @param tree
     */
    public double recalculateScores(ProcessedInput input, FTree tree) {
        final Iterator<Loss> edges = tree.lossIterator();
        final List<ProcessedPeak> peaks = input.getMergedPeaks();
        final Object[] preparedLoss = new Object[lossScorers.size()];
        final Object[] preparedFrag = new Object[decompositionScorers.size()];
        final Object[] preparedFragScores = new Object[fragmentScorers.size()];
        final PrecursorIonType ionType = tree.getAnnotationOrThrow(PrecursorIonType.class);
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
            i=0;
            for (FragmentScorer<?> fragmentScorer : fragmentScorers) {
                fragScores.add(fragmentHeader.define(getScoringMethodName(fragmentScorer)));
                preparedFragScores[i++] = fragmentScorer.prepare(input, tree);
            }

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
                preparedLoss[i++] = lossScorer.prepare(input,tree);
            }
            lossScores = lScores.toArray(new String[lScores.size()]);
        }
        {
            final ArrayList<String> fragScores = new ArrayList<String>();
            for (DecompositionScorer peakScorer : this.rootScorers) {
                fragScores.add(rootHeader.define(getScoringMethodName(peakScorer)));
            }
            for (FragmentScorer<?> fragmentScorer : fragmentScorers) {
                fragScores.add(rootHeader.define(getScoringMethodName(fragmentScorer)));
            }
            for (GeneralGraphScorer generalGraphScorer : this.generalGraphScorers) {
                fragScores.add(rootHeader.define(getScoringMethodName(generalGraphScorer)));
            }
            rootScores = fragScores.toArray(new String[fragScores.size()]);
        }

        final FragmentAnnotation<Score> fAno = tree.getOrCreateFragmentAnnotation(Score.class);
        final LossAnnotation<Score> lAno = tree.getOrCreateLossAnnotation(Score.class);
        final LossAnnotation<LossType> lossType = tree.getOrCreateLossAnnotation(LossType.class);
        final double[][] pseudoMatrix = new double[2][2];
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            if (lossType.get(loss).isInSource()) continue;
            final Fragment u = loss.getSource();
            final Fragment v = loss.getTarget();

            // add loss scores
            final Score.ScoreAssigner lscore = lossHeader.score();
            int k=0;
            for (int i=0; i < peakPairScorers.size(); ++i) {
                pseudoMatrix[0][0]=pseudoMatrix[0][1]=pseudoMatrix[1][0]=pseudoMatrix[1][1]=0.0d;
                peakPairScorers.get(i).score(Arrays.asList(peaks.get(v.getPeakId()), peaks.get(u.getPeakId())), input,pseudoMatrix);
                lscore.set(lossScores[k++], lossShouldBeScoredbyPeakPairScorers(loss) ? pseudoMatrix[1][0] : 0d);
            }
            for (int i=0; i < lossScorers.size(); ++i) {
                if (!loss.isArtificial() || lossScorers.get(i).processArtificialEdges()) {
                    lscore.set(lossScores[k++], lossScorers.get(i).score(loss, input, preparedLoss[i]));
                }
            }
            lAno.set(loss, lscore.done());

            // add fragment scores
            final Score.ScoreAssigner fscore = fragmentHeader.score();
            k=0;
            for (int i=0; i < fragmentPeakScorers.size(); ++i) {
                pseudoMatrix[0][0]=pseudoMatrix[0][1]=pseudoMatrix[1][0]=pseudoMatrix[1][1]=0.0d;
                fragmentPeakScorers.get(i).score(Arrays.asList(peaks.get(v.getPeakId())), input,pseudoMatrix[0]);
                fscore.set(fragmentScores[k++], pseudoMatrix[0][0]);
            }
            for (int i=0; i < decompositionScorers.size(); ++i) {
                fscore.set(fragmentScores[k++], ((DecompositionScorer<Object>) decompositionScorers.get(i)).score(v.getFormula(),v.getIonization(), peaks.get(v.getPeakId()), input, preparedFrag[i]));
            }
            for (int i=0; i < fragmentScorers.size(); ++i) {
                fscore.set(fragmentScores[k++], ((FragmentScorer<Object>)fragmentScorers.get(i)).score(v, peaks.get(v.getPeakId()), false, preparedFragScores[i]));
            }

            fAno.set(v, fscore.done());
        }
        // set root
        Fragment root = tree.getRoot();
        if (root.getOutDegree()==1 && lossType.get(root.getOutgoingEdge(0)).isInSource()) {
            root = root.getChildren(0);
        }
        final Score.ScoreAssigner rootScore = rootHeader.score();
        int rootscoreIndex = 0;
        for (int k=0; k < rootScorers.size(); ++k) {
            final Object prepared = rootScorers.get(k).prepare(input);
            final double score = ((DecompositionScorer<Object>)rootScorers.get(k)).score(root.getFormula(),root.getIonization(), peaks.get(root.getPeakId()), input, prepared);
            rootScore.set(rootScores[rootscoreIndex++], score);
        }
        for (int k=0; k < fragmentScorers.size(); ++k) {
            final Object prepared = preparedFragScores[k];
            final double score = ((FragmentScorer<Object>)fragmentScorers.get(k)).score(root, peaks.get(root.getPeakId()), true, prepared);
            rootScore.set(rootScores[rootscoreIndex++], score);
        }
        for (int k=0; k < generalGraphScorers.size(); ++k) {
            final double score =generalGraphScorers.get(k).score(tree, input);
            rootScore.set(rootScores[rootscoreIndex++], score);
        }


        fAno.set(root, rootScore.done());

        // check scoreSum
        double scoreSum = 0d;
        for (Loss l : tree.losses()) {
            double lossScore = 0d;
            Score s = lAno.get(l);
            if (s==null) continue;
            lossScore += s.sum();
            s = fAno.get(l.getTarget());
            lossScore += s.sum();
            scoreSum += lossScore;
            if (Math.abs(lossScore-l.getWeight()) > 1e-4) {
                LoggerFactory.getLogger(FragmentationPatternAnalysis.class).trace("Score difference: loss " + l + " (" + l.getSource().getFormula().toString() + " -> " + l.getTarget().getFormula().toString() + ") should have score " + lossScore + " but edge is weighted with " + l.getWeight() + ", loss is " + lAno.get(l) + " and fragment is " + fAno.get(l.getTarget()));
            }
        }
        scoreSum += fAno.get(root).sum();
        return scoreSum;

    }

    public static String getScoringMethodName(Object instance) {
        Class<? extends Object> someClass = instance instanceof Class ? (Class<? extends Object>)instance : instance.getClass();
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
        final Object[] precomputedForFragmentScorer = new Object[fragmentScorers.size()];
        for (int k=0; k < fragmentScorers.size(); ++k) {
            precomputedForFragmentScorer[k] = fragmentScorers.get(k).prepare(input,graph);
        }
        final FragmentAnnotation<Decomposition> decompositionFragmentAnnotation = graph.getFragmentAnnotationOrThrow(Decomposition.class);
        //final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        for (int i = 0; i < precomputeds.length; ++i) precomputeds[i] = lossScorers[i].prepare(input,graph);
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            final boolean isArtificial = loss.isArtificial();
            final Fragment u = loss.getSource();
            final Fragment v = loss.getTarget();
            // take score of molecular formula
            final Decomposition decomp = decompositionFragmentAnnotation.get(v);
            double score = decomp==null ? 0d : decomp.getScore();
            assert !Double.isInfinite(score);
            // add it to score of the peak
            score += peakScores[v.getColor()];//peakScores[peakAno.get(v).getIndex()];
            assert !Double.isInfinite(score);
            // add it to the score of the peak pairs
            if (!u.isRoot() && lossShouldBeScoredbyPeakPairScorers(loss))
                score +=  peakPairScores[u.getColor()][v.getColor()];//peakPairScores[peakAno.get(u).getIndex()][peakAno.get(v).getIndex()]; // TODO: Umdrehen!
            assert !Double.isInfinite(score);
            // add the score of the loss
            if (!u.isRoot()) {
                for (int i = 0; i < lossScorers.length; ++i) {
                    if (!isArtificial || lossScorers[i].processArtificialEdges()) {
                        score += lossScorers[i].score(loss, input, precomputeds[i]);
                        assert !Double.isInfinite(score) : lossScorers[i].getClass().getSimpleName();
                    }
                }
            }
            // add score of the fragment
            ProcessedPeak correspondingPeak = input.getMergedPeaks().get(v.getPeakId());
            for (int k=0; k < fragmentScorers.size(); ++k) {
                final FragmentScorer<Object> scorer = (FragmentScorer<Object>)fragmentScorers.get(k);
                score += scorer.score(v, correspondingPeak, v.isRoot(), precomputedForFragmentScorer[k]);
            }
            assert !Double.isInfinite(score);
            loss.setWeight(score);
        }
        // add graph scores
        for (GeneralGraphScorer gen : generalGraphScorers) {
            double score = gen.score(graph, input);
            for (int l=0; l < graph.getRoot().getOutDegree(); ++l) {
                Loss outgoingEdge = graph.getRoot().getOutgoingEdge(l);
                outgoingEdge.setWeight(outgoingEdge.getWeight()+score);
            }
        }
        return graph;
    }

    private boolean lossShouldBeScoredbyPeakPairScorers(Loss loss) {
        return !loss.isArtificial();
    }


    private void addSyntheticParent(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks, double parentmass) {
        final ProcessedPeak syntheticParent = new ProcessedPeak();
        syntheticParent.setMass(parentmass);
        processedPeaks.add(syntheticParent);
    }

    /*

    Merging:
        - 1. lösche alle Peaks die zu nahe an einem anderen Peak im selben Spektrum sind un geringe Intensität
        - 2. der Peakmerger bekommt nur Peak aus unterschiedlichen Spektren und mergt diese
        - 3. Nach der Decomposition läuft man alle peaks in der Liste durch. Wenn zwischen zwei
             Peaks der Abstand zu klein wird, werden diese Peaks disjunkt, in dem die doppelt vorkommenden
             Decompositions auf einen peak (den mit der geringeren asseabweichung) eindeutig verteilt werden.

     */


    /**
     * is called immediately after computing the tree
     */
    public void postProcessTree(ProcessedInput input, FGraph graph, FTree tree) {
        for (SiriusPlugin plugin : siriusPlugins.values()) {
            plugin.afterTreeComputation(input,graph,tree);
        }
    }

    /**
     * is called if the tree should be reported to the user, BEFORE recalculating the scores
     */
    public void makeTreeReleaseReady(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graphFragmentMap) {
        addPeakAnnotationToTree(input, graph, tree, graphFragmentMap);
        final LossAnnotation<LossType> lossType = tree.addLossAnnotation(LossType.class, LossType::regular);
        for (Loss l : tree.losses()) {
            lossType.set(l,LossType.regular());
        }
        transferDefaultAnotationsFromGraphToTree(input,graph,tree,graphFragmentMap);
        for (SiriusPlugin plugin : siriusPlugins.values()) {
            plugin.transferAnotationsFromGraphToTree(input,graph,tree, graphFragmentMap);
        }
        for (SiriusPlugin plugin : siriusPlugins.values()) {
            plugin.releaseTreeToUser(input,graph,tree);
        }

        tree.normalizeStructure();
    }

    private void addPeakAnnotationToTree(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2tree) {
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.addFragmentAnnotation(AnnotatedPeak.class,AnnotatedPeak::none);
        final SpectralRecalibration rec = graph.getAnnotation(SpectralRecalibration.class, SpectralRecalibration::none);
        for (Fragment u : tree.getFragments()) {
            final Fragment v = graph2tree.mapRightToLeft(u);
            if (v!=null) {
                final int peakIndex = v.getPeakId();
                u.setPeakId(peakIndex);
                if (peakIndex>=0) {
                    peakAno.set(u, input.getMergedPeaks().get(peakIndex).toAnnotatedPeak(u.getFormula(),PrecursorIonType.getPrecursorIonType(u.getIonization()), rec));
                } else {
                    peakAno.set(u, AnnotatedPeak.artificial(u.getFormula(), u.getIonization()));
                }
            } else {
                System.err.println("unknown node " + u);
            }
        }
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

    public List<FragmentScorer<?>> getFragmentScorers() {
        return fragmentScorers;
    }

    public void setFragmentScorers(List<FragmentScorer<?>> fragmentScorers) {
        this.fragmentScorers = fragmentScorers;
    }

    public List<GeneralGraphScorer> getGeneralGraphScorers() {
        return generalGraphScorers;
    }

    public void setGeneralGraphScorers(List<GeneralGraphScorer> generalGraphScorers) {
        this.generalGraphScorers = generalGraphScorers;
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
        fillList(rootScorers, helper, document, dictionary, "rootScorers");
        fillList(fragmentScorers, helper, document, dictionary, "fragmentScorers");
        fillList(decompositionScorers, helper, document, dictionary, "decompositionScorers");
        fillList(fragmentPeakScorers, helper, document, dictionary, "peakScorers");
        fillList(peakPairScorers, helper, document, dictionary, "peakPairScorers");
        fillList(lossScorers, helper, document, dictionary, "lossScorers");
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
        list = document.newList();
        for (DecompositionScorer s : rootScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "rootScorers", list);
        list = document.newList();
        for (FragmentScorer s : fragmentScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "fragmentScorers", list);
        list = document.newList();
        for (DecompositionScorer s : decompositionScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "decompositionScorers", list);
        list = document.newList();
        for (PeakScorer s : fragmentPeakScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "peakScorers", list);
        list = document.newList();
        for (PeakPairScorer s : peakPairScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "peakPairScorers", list);
        list = document.newList();
        for (LossScorer s : lossScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "lossScorers", list);
    }
}
