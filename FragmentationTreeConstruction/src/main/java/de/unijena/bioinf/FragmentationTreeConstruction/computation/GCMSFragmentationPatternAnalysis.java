package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.Preprocessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SubFormulaGraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.GCMSMissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.DPTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

import java.util.*;

public class GCMSFragmentationPatternAnalysis extends FragmentationPatternAnalysis {
    /*
    This is somehow a reimplementation of the GCMSTool core
    important differences here and in scorings, filters... are:
        - normalisation to max=1d not 100d -> adjust scorings
        - MostRelevantPeakfilter: in step 2 peaks are really ordered by mass*logInt and not mass*log(int*100)
        - isTrueSubsetOf in GCMSTool is now isSubstractable: implemented a bit different because was not sure about GCMSTool thing
        - TmsToDMsLossScorer: Scoring in GCMSTool assumed that there is just one TMS (no DMS) in head and one DMS in the tail of a loss -> this is changed
        - now scoring the root is possible. this wasn't necessary by now because either the parent formula was known or a dummy was created wich we don't want to score
          --> score root always the same way as other nodes or is there some reasonable difference possible?
          (root scoring would only influence results if 1. we would know parent peak but have more than one MolecularFormula
           or 2. if we buid a dummy graph and want the best x solutions
        - ...
        - and all other differences I forgot
     */

    /*
    whats still todo
        - ilp: - get different solutions
               - get best solution not necessarily rooted in parent / dummy
               - TMS/PFB stuff: just one edge out of dummy
        - introduce extra tms/pfb scoring for nodes directly below dummy ??? (if only one -> = root?))
            -> would be not as strict as the above one in ilp (if we don't know whether parent present?)
        - isotopes: - better selection
                    - integrate in scoring
        - currently using FractionOfParent LossScorer which scores loss in comparison to parent size -> better learn loss size distribution
        - currently using rdbeScore<0 strict filter for decomposition --> change to scoring? because ...
          "bis(pentafluorosulfur)methane CH2F10S2 has a negative ring double bond equivalent of -4. There are hundreds of such examples, especially from organic compounds containing halogenes (F,Cl,Br,I) together with sulfur, nitrogen and phosphorous."
        - learn common losses and scoring. //ASK: are there disadvantages if we allow more than one recombination of the common losses?
     */
    //todo problems?
    //todo for computation with dummy and mostRelevantPeaks=5 gcmstool with the rule 'only 1 edge to a node with Tms' has much better score : maybe because of  TMS-Root = score*2? but graph seems also to be bigger


    /*
    something to ask
        - what was the intention of the unused 'public void mergeDecompositions(Peak mergePeak, Parameters param)'  in GCMSTool?
          why merge peaks in EI?
        - why the different approaches for the most relevant peaks in GCMSTool for dummy root and normal graph (just chosen by relInt)?
          -> currently we don't differentiate between this cases.
        - if we assume that the deviation is dependent on the intensity why we take relative intensities? the deviation of one peak shouldn't change because of another peak
        - concerning TmsToDMsLossScorer: Is it uncommon, that TMS looses its CH3 group? If yes, does this scoring of -0.1 influence anything if CH3 as common loss scores log(100)~=4,6..
     */

    /*
    ask kai
        - TreeAnnotation -> change to FragmentationPathway + set ProcessedInput??
        - in DP //error thrown when using dummy -> difference in backtrack score?  69-72-7.txt, all scores, 32.14562512636295 is not equal to computed score 32.145625126362944
            //obviously only a rounding problem -> compare with a certain accuracy or is this unwanted for some reason?
            //    if (!isEqual(scoreSum, Math.max(tables[vertexId].bestScore(), 0) )) {
            //        throw new RuntimeException("Critical Error: Backtracked score " + scoreSum +
            //                " is not equal to computed score " + tables[vertexId].bestScore());
            //    }
        - MassDeviationVertexScorer scores unmodified masses not ions??
        - no massPenalty option anymore. Why? //todo massPenalty required for gcms?
        - //todo equals problem with mutable immutable MolecularFormula different amounts [3,1,0,0] vs. [3,1] -> is this desired or change it?
        - //todo error in DP backtrack?  tested with new and old version, and tree size
          -> edges in graph loss:    <0:C32767H32767Cl32767Dms32767K32767N32767Na32767O32767P32767Pfb32767S32767Tms32767 (0,000)> -> <15:C10H11 (-0,121)> [:0.0]
                            and :    <35:C10H13 (-3,058)> -> <15:C10H11 (-0,121)> [H2:0.2426246210745533]
                            are combined in tree to <0:C32767H32767Cl32767Dms32767K32767N32767Na32767O32767P32767Pfb32767S32767Tms32767 (0,000)> -> <15:C10H11 (-0,121)> [H2:0.2426246210745533]
                            problem with reconnecting edges? not yet fully implemented?


     */
    private static final boolean VERBOSE = true;

    private boolean moleculePeakKnown;
    private boolean moleculePeakPresent;

    private boolean useHalogens;
    private boolean useChlorine;

    //derivates
    private boolean useDerivates;
    private boolean usePFB;
    private boolean useTMS;
    /*
    is...Compound just to increase scoring for DP? //todo what about usage in ilp
     */
    private boolean isPFBcompound;
    private boolean isTMScompound;

    private boolean removeIsotopePeaks;
    private static final double NEUTRON_MASS = 1.00866491;
    private static final double ELECTRON_MASS = 0.00054857990946;
    private MolecularFormula dummyFormula;



    @Override
    public void setInitial() {
        this.setInputValidators(new ArrayList<InputValidator>());
        getInputValidators().add(new GCMSMissingValueValidator());
        this.setValidatorWarning(new Warning.Noop());
        this.setRepairInput(true);
        this.setDecompositionScorers(new ArrayList<DecompositionScorer<?>>());
        this.setPreprocessors(new ArrayList<Preprocessor>());
        this.setPostProcessors(new ArrayList<PostProcessor>());
        this.setRootScorers(new ArrayList<DecompositionScorer<?>>());
        this.setPeakPairScorers(new ArrayList<PeakPairScorer>());
        this.setFragmentPeakScorers(new ArrayList<PeakScorer>());
        this.setGraphBuilder(new SubFormulaGraphBuilder());
        this.setLossScorers(new ArrayList<LossScorer>());
        this.setTreeBuilder(new DPTreeBuilder(16));
    }


    @Override
    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        final ProcessedInput processedInput = preprocessWithoutDecomposing(experiment);
        // decompose and score all peaks
        //todo split decomposeAndScore method to differentiate in scoring whether molecule peak present or not
        if (moleculePeakPresent){
            return decomposeAndScore(processedInput.getExperimentInformation(), processedInput.getMergedPeaks());
        } else {
            //todo return a decomposeAndScore without parent
            return null;
        }

    }

    @Override
    ProcessedInput preprocessWithoutDecomposing(Ms2Experiment experiment) {
        //is molecule peak known or unknown?
        if (experiment.getIonMass()==0) moleculePeakKnown = false;
        else moleculePeakKnown = true;
        // first of all: insert default profile if no profile is given
        Ms2ExperimentImpl input = wrapInput(experiment);
        if (input.getMeasurementProfile()==null) input.setMeasurementProfile(getDefaultProfile());
        // use a mutable experiment, such that we can easily modify it. Validate and preprocess input
        input = wrapInput(preProcess(validate(experiment)));
        List<ProcessedPeak> peaks = normalize(input);

        testForDerivatization(input, peaks);
        peaks = postProcess(PostProcessor.Stage.AFTER_NORMALIZING, new ProcessedInput(input, peaks, null, null)).getMergedPeaks();
        if (removeIsotopePeaks){
            peaks = removeIsotopePeaks(input, peaks);
            if (VERBOSE && moleculePeakKnown) System.out.println("ionMass: "+ input.getIonMass()+", largest peak mass: "+peaks.get(peaks.size()-1).getMz());
        }
        //todo after isotopeStuff

        final ProcessedPeak parentPeak = selectMoleculePeakAndCleanSpectrum(input, peaks);
        //todo after parentPeak selection the molecule peak has to be last in list (assumed in decomposeAndScore)
        if (moleculePeakPresent) assert parentPeak.equals(peaks.get(peaks.size()-1)) : "parent is last in peak list";
        if (moleculePeakPresent){
            //todo some PostProcesses assume that parent peak present -> create new Stage to differentiate between methods which need parent or don't need? or make all methods also work without
            //PostProcessor.Stage.AFTER_MERGING ist z.B. NoiseThreshold
            peaks = postProcess(PostProcessor.Stage.AFTER_MERGING, new ProcessedInput(input, peaks, parentPeak, null)).getMergedPeaks();
        }

        return new ProcessedInput(input, peaks, parentPeak, null);
    }

    @Override
    ArrayList<ProcessedPeak> normalize(Ms2Experiment experiment) {
        //todo delete peaks near high intensity peaks like in MS2 version?
        //normalize relative intensities in merged MS1 spectrum
        final ArrayList<ProcessedPeak> peakList = new ArrayList<ProcessedPeak>();
        final Spectrum<Peak> ms1spectrum = experiment.getMergedMs1Spectrum();
        final Ionization ion = experiment.getIonization();
        MutableSpectrum<Peak> mutableSpectrum = new SimpleMutableSpectrum(ms1spectrum);
        Spectrums.normalize(mutableSpectrum, Normalization.Max(1d));

        for (int i = 0; i < mutableSpectrum.size(); i++) {
            ProcessedPeak processedPeak = new ProcessedPeak();
            processedPeak.setIntensity(ms1spectrum.getIntensityAt(i));
            processedPeak.setMz(ms1spectrum.getMzAt(i));
            processedPeak.setRelativeIntensity(mutableSpectrum.getIntensityAt(i));
            processedPeak.setIon(ion);
            //insert a synthetic as original Peak, because some scorings decide if it is a synthetic peak on whether he has a original MS2Peak! peak in list.
            processedPeak.setOriginalPeaks(Collections.singletonList(new MS2Peak(new Ms2SpectrumImpl(new CollisionEnergy(70, 70), 0), ms1spectrum.getMzAt(i), ms1spectrum.getIntensityAt(i))));
            peakList.add(processedPeak);
        }
        return peakList;
    }

    @Override
    ProcessedInput decomposeAndScore(Ms2Experiment ms2Experiment, List<ProcessedPeak> processedPeaks) {
        Ms2ExperimentImpl experiment = new Ms2ExperimentImpl(ms2Experiment);
        final Deviation parentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        // sort again...
        processedPeaks = new ArrayList<ProcessedPeak>(processedPeaks);
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size()-1);
        // decompose peaks
        final FormulaConstraints constraints = experiment.getMeasurementProfile().getFormulaConstraints();
        final MassToFormulaDecomposer decomposer = getDecomposerFor(constraints.getChemicalAlphabet());
        final Ionization ion = experiment.getIonization();
        final Deviation fragmentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        final List<MolecularFormula> pmds = decomposer.decomposeToFormulas(parentPeak.getUnmodifiedMass(), intensityDeviation(parentPeak.getRelativeIntensity(), parentDeviation), constraints);
        final ArrayList<List<MolecularFormula>> decompositions = new ArrayList<List<MolecularFormula>>(processedPeaks.size());
        int j=0;
        for (ProcessedPeak peak : processedPeaks.subList(0, processedPeaks.size()-1)) {
            peak.setIndex(j++);
            decompositions.add(decomposer.decomposeToFormulas(peak.getUnmodifiedMass(), intensityDeviation(peak.getRelativeIntensity(), fragmentDeviation), constraints));
        }
        parentPeak.setIndex(processedPeaks.size()-1);
        assert parentPeak == processedPeaks.get(processedPeaks.size()-1);
        // important: for each two peaks which are within 2*massrange:
        //  => make decomposition list disjoint
        //changed use maximum intensity deviation?????
        final EIIntensityDeviation window = (EIIntensityDeviation)(fragmentDeviation.multiply(2));
        window.setRelIntensity(1d);
        //changed.......................
        for (int i=1; i < processedPeaks.size()-1; ++i) {
            if (window.inErrorWindow(processedPeaks.get(i).getMz(), processedPeaks.get(i-1).getMz())) {
                final HashSet<MolecularFormula> right = new HashSet<MolecularFormula>(decompositions.get(i));
                final ArrayList<MolecularFormula> left = new ArrayList<MolecularFormula>(decompositions.get(i-1));
                final double leftMass = ion.subtractFromMass(processedPeaks.get(i-1).getMass());
                final double rightMass = ion.subtractFromMass(processedPeaks.get(i).getMass());
                final Iterator<MolecularFormula> leftIter = left.iterator();
                while (leftIter.hasNext()) {
                    final MolecularFormula leftFormula = leftIter.next();
                    if (right.contains(leftFormula)) {
                        //because case is so rare don't use something fancy with intensities
                        if (Math.abs(leftFormula.getMass()-leftMass) < Math.abs(leftFormula.getMass()-rightMass)) {
                            right.remove(leftFormula);
                        } else {
                            leftIter.remove();
                        }
                    }
                }
                decompositions.set(i-1, left);
                decompositions.set(i, new ArrayList<MolecularFormula>(right));
            }
        }
        final ProcessedInput preprocessed = new ProcessedInput(experiment, processedPeaks, parentPeak, null);
        final int n = processedPeaks.size();
        // score peak pairs
        final double[][] peakPairScores = new double[n][n];
        for (PeakPairScorer scorer : super.getPeakPairScorers()) {
            scorer.score(processedPeaks, preprocessed, peakPairScores);
        }
        // score fragment peaks
        final double[] peakScores = new double[n];
        for (PeakScorer scorer : super.getFragmentPeakScorers()) {
            scorer.score(processedPeaks, preprocessed, peakScores);
        }
        // dont score parent peak
        peakScores[peakScores.length-1]=0d;
        // score peaks
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(super.getDecompositionScorers().size());
            for (DecompositionScorer<?> scorer : super.getDecompositionScorers()) preparations.add(scorer.prepare(preprocessed));
            for (int i=0; i < processedPeaks.size()-1; ++i) {
                final List<MolecularFormula> decomps = decompositions.get(i);
                final ArrayList<ScoredMolecularFormula> scored = new ArrayList<ScoredMolecularFormula>(decomps.size());
                for (MolecularFormula f : decomps) {
                    double score = 0d;
                    int k=0;
                    setIntensityInMeasurmentProfileDeviations(experiment, processedPeaks.get(i).getRelativeIntensity());
                    for (DecompositionScorer<?> scorer : super.getDecompositionScorers()) {
                        score += ((DecompositionScorer<Object>)scorer).score(f, processedPeaks.get(i), preprocessed, preparations.get(k++));
                    }
                    scored.add(new ScoredMolecularFormula(f, score));
                }
                processedPeaks.get(i).setDecompositions(scored);
            }
        }
        // same with root
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(super.getRootScorers().size());
            for (DecompositionScorer<?> scorer : super.getRootScorers()) preparations.add(scorer.prepare(preprocessed));
            final ArrayList<ScoredMolecularFormula> scored = new ArrayList<ScoredMolecularFormula>(pmds.size());
            for (MolecularFormula f : pmds) {
                double score = 0d;
                int k=0;
                setIntensityInMeasurmentProfileDeviations(experiment, parentPeak.getRelativeIntensity());
                for (DecompositionScorer<?> scorer : super.getRootScorers()) {
                    score += ((DecompositionScorer<Object>)scorer).score(f, parentPeak, preprocessed, preparations.get(k++));
                }
                scored.add(new ScoredMolecularFormula(f, score));

            }
            parentPeak.setDecompositions(scored);
        }
        // set peak indizes
        for (int i=0; i < processedPeaks.size(); ++i) processedPeaks.get(i).setIndex(i);

        final ProcessedInput processedInput =
                new ProcessedInput(experiment, processedPeaks, parentPeak, parentPeak.getDecompositions(), peakScores, peakPairScores);
        // final processing
        return postProcess(PostProcessor.Stage.AFTER_DECOMPOSING, processedInput);
    }


    @Override
    public FragmentationGraph buildGraph(ProcessedInput input, ScoredMolecularFormula candidate) {
        assert moleculePeakPresent; //only works with present molecule peak;
        if (VERBOSE){
            System.out.println("buildGraph");
            System.out.println("mergedPeaks:"+input.getMergedPeaks().size());
            int withDecomp = 0;
            int totalDecomp = 0;
            for (ProcessedPeak processedPeak : input.getMergedPeaks()) {
                if (processedPeak.getDecompositions().size()>0){
                    withDecomp++;
                    totalDecomp += processedPeak.getDecompositions().size();
                }
            }
            System.out.println("withDecomp:"+withDecomp+" total:"+totalDecomp);
        }


        if (useTMS){
            final List<ProcessedPeak> processedPeakList = input.getMergedPeaks();
            for (ProcessedPeak processedPeak : processedPeakList) {
                List<ScoredMolecularFormula> decompositions = processedPeak.getDecompositions();
                List<ScoredMolecularFormula> derivatesDecompositions = new ArrayList<ScoredMolecularFormula>(decompositions.size());
                for (ScoredMolecularFormula decomposition : decompositions) {
                    derivatesDecompositions.add(new ScoredMolecularFormula(new DerivatesMolecularFormula(decomposition.getFormula()), decomposition.getScore()));
                }
                processedPeak.setDecompositions(derivatesDecompositions);
            }
            candidate = new ScoredMolecularFormula(new DerivatesMolecularFormula(candidate.getFormula()), candidate.getScore());
        }

        return super.buildGraph(input, candidate);
    }

    /**
     * build a FragmentationGraph using a dummy as root. No knonw parent is needed.
     * @param input
     * @return
     */
    public FragmentationGraph buildDummyGraph(ProcessedInput input){
//        assert input.getParentPeak()==null; //todo at least throw warning
        if (VERBOSE) System.out.println("build dummy");

        ProcessedPeak dummy = new ProcessedPeak();
        dummy.setMz(Double.MAX_VALUE); //always in most right position (MassComparator)
        dummy.setIon(new ElectronIonization()); //to make getUnmodifiedMass work
        dummy.setIndex(input.getMergedPeaks().size());

        TableSelection selection = input.getExperimentInformation().getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet().getTableSelection();
        short[] amounts = new short[selection.size()];
        Arrays.fill(amounts, Short.MAX_VALUE);
        dummyFormula = new DerivatesMolecularFormula(selection.toFormula(amounts));
        dummy.setDecompositions(Collections.singletonList(new ScoredMolecularFormula(dummyFormula, 0)));

        ArrayList<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        mergedPeaks.add(dummy);

        //extend peak(pair)score arrays
        double[] newPeakScores = Arrays.copyOf(input.getPeakScores(), input.getPeakScores().length+1);
        double[][] peakPairScores = input.getPeakPairScores();
        double[][] newPeakPairScores = new double[peakPairScores.length+1][peakPairScores.length+1];
        for (int i = 0; i < peakPairScores.length; i++) {
            newPeakPairScores[i] = Arrays.copyOf(peakPairScores[i], peakPairScores.length+1);
        }
        Arrays.fill(newPeakPairScores[newPeakPairScores.length-1], 0d);

        ProcessedInput processedInputForGraphBuilding = new ProcessedInput(input.getExperimentInformation(), mergedPeaks, dummy, dummy.getDecompositions(), newPeakScores, newPeakPairScores);
        FragmentationGraph graph = buildGraph(processedInputForGraphBuilding, dummy.getDecompositions().get(0));
        //set unwanted weights of dummy outgoing edges to 0
        //todo learn cool weights
        for (Loss loss : graph.getRoot().getOutgoingEdges()) {
            loss.setWeight(0);
        }
        graph.setRootScore(0);

        return graph;
    }


    @Override
    public FragmentationTree computeTree(FragmentationGraph graph) {
        if (VERBOSE) System.out.println("...computeTree...");
        if (VERBOSE) System.out.println("root:"+graph.getRoot());
        return super.computeTree(graph);
    }

    public List<FragmentationTree> computeMultipleTrees(FragmentationGraph graph, double lowerbound, int maxTrees) {
        List<FragmentationTree> trees = getTreeBuilder().buildMultipleTrees(graph.getProcessedInput(), graph, lowerbound);
        List<FragmentationTree> rescoredTrees = new ArrayList<FragmentationTree>(trees.size());
        PeriodicTable periodicTable = PeriodicTable.getInstance();
        final Element tmsElement = periodicTable.getByName("Tms");
        final Element pfbElement = periodicTable.getByName("Pfb");
        //todo score roots with normal scorers or with rootScorers?
        final double[] peakScores = graph.getProcessedInput().getPeakScores();
        for (FragmentationTree tree : trees) {
            final TreeFragment root = tree.getRoot();
            if (isValidMolecularFormula(root.getFormula()) || isDummy(root)){
                double rootScore = root.getDecomposition().getScore();
                rootScore += peakScores[root.getPeak().getIndex()];
                double overallScore = tree.getScore();
                if (Double.isNaN(tree.getRootScore())){
                    //for all but the real graph root the tree rootScore has to be added
                    overallScore += rootScore;
                }
                if (isTMScompound && root.getFormula().numberOf(tmsElement)>0){
                    overallScore *= 2;
                }
                if (isPFBcompound && root.getFormula().numberOf(pfbElement)>0){
                    overallScore *= 2;
                }
                if (overallScore>lowerbound){
                    tree.setRootScore(rootScore);
                    tree.setScore(overallScore);
                    rescoredTrees.add(tree);
                }
            }
        }
        Collections.sort(rescoredTrees, Collections.reverseOrder());
        if (maxTrees>=rescoredTrees.size()) return rescoredTrees;
        else return rescoredTrees.subList(0, maxTrees);
    }


    public List<FragmentationTree> computeMultipleTrees(FragmentationGraph graph, int maxTrees) {
        return computeMultipleTrees(graph, Double.NEGATIVE_INFINITY, maxTrees);
    }

    /**
     * changes Formula and mz of dummy node to smaller values for a visual nicer dot root node
     * @param tree
     */
    public void setNiceDummyNodeAnnotation(FragmentationTree tree){
        if (!isDummy(tree.getRoot())) return;
        //just for nicer treeAnnotation
        DerivatesMolecularFormula rootFormula = (DerivatesMolecularFormula)tree.getRoot().getFormula();
        for (Element element : rootFormula) {
            rootFormula.set(element, 1);
        }
        tree.getRoot().getPeak().setMz(0.0);
    }

    private boolean isValidMolecularFormula(MolecularFormula root){
        PeriodicTable periodicTable = PeriodicTable.getInstance();

        if (useTMS){
            Element dmsElement = periodicTable.getByName("Dms");
            if (root.numberOf(dmsElement)>0) return false;
        }


        int hal=0;
        int nrTms =0;
        int nrPfb=0;
        int nrN;
        int nrH;
        int nrC;

        //todo define elements once for whole class?
        Element chlorineElement = periodicTable.getByName("Cl");
        if (useChlorine && !useHalogens){
            hal= root.numberOf(chlorineElement);
        }
        if(useHalogens){
            hal = root.numberOf(periodicTable.getByName("F"))+root.numberOf(periodicTable.getByName("Br"))+root.numberOf(periodicTable.getByName("I"))+root.numberOf(chlorineElement);
        }
        if (useTMS){

            nrTms = root.numberOf(periodicTable.getByName("Tms"));
        }
        if (usePFB){
            nrPfb = root.numberOf(periodicTable.getByName("Pfb"));
        }

        nrN=root.numberOf(periodicTable.getByName("N"));
        nrH=root.numberOfHydrogens()+9*nrTms+2*nrPfb;
        nrC=root.numberOfCarbons()+3*nrTms+7*nrPfb;
        hal+=5*nrPfb;


        //odd electron rule
        double electronRule = nrC-0.5*(nrH+hal)+0.5*nrN+1;
        if ((electronRule % 1) != 0) {
            // an even electron cannot be mol peak
            return false;
        }

        //nitrogen rule
        boolean nitrogenRule = false;
        if (nrN%2==0 && (root.getIntMass()) % 2==0)nitrogenRule=true;
        else if(nrN%2!=0 && (root.getIntMass()) % 2!=0) nitrogenRule=true;

        return nitrogenRule;
    }

    private boolean isDummy(TreeFragment root){
        //of better test for maximal formula? C32xxxH32xxx....Tms....
        return root.getPeak().getMz()==Double.MAX_VALUE;
    }




    /**
     * under construction
     * always return true
     * predict whether molecule peak is present. And even predict mass ...
     * @param experiment
     * @param processedPeaks
     * @return
     */
    boolean isMoleculePeakPresent(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks) {
        //todo implement
        return true;
    }



    ProcessedPeak selectMoleculePeakAndCleanSpectrum(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks){
        if (moleculePeakKnown){
            //search with max intensity for max deviation
            ((EIIntensityDeviation)experiment.getMeasurementProfile().getAllowedMassDeviation()).setRelIntensity(1.0);

            //todo split method? change: take peak closest to parentmass
            moleculePeakPresent = true; //synthetic peak introduced if peak not present
            return selectParentPeakAndCleanSpectrum(experiment, processedPeaks);
        } else {
            //molecule peak not known
            //maybe later guessing whether it's in there and which peak it is -->
            moleculePeakPresent = isMoleculePeakPresent(experiment, processedPeaks);
            //but until now: just take heaviest
            //hopefully all isotopes are removed
            double currentMax = Double.NEGATIVE_INFINITY;
            ProcessedPeak moleculePeak = null;
            for (ProcessedPeak processedPeak : processedPeaks) {
                if (processedPeak.getMz()>currentMax){
                    moleculePeak = processedPeak;
                    currentMax = processedPeak.getMz();
                }
            }
            return moleculePeak;
        }
    }

    @Override
    ProcessedPeak selectParentPeakAndCleanSpectrum(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks) {
        // and sort the resulting peaklist by mass
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        // now search the parent peak. If it is not contained in the spectrum: create one!
        // delete all peaks behind the parent, such that the parent is the heaviest peak in the spectrum
        // Now we can access the parent peak by peaklist[peaklist.size-1]
        final double parentmass = experiment.getIonMass();
        final Deviation parentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        double minDiff = Double.MAX_VALUE;
        int parentPos = -1;
        for (int i=processedPeaks.size()-1; i >= 0; --i) {
            final double diff = Math.abs(processedPeaks.get(i).getMz()-parentmass);
            //take the peak closest to parentmass if it is inErrorWindow
            if (diff<minDiff){
                minDiff = diff;
                parentPos = i;
                //remove peaks above parent
                if (i < processedPeaks.size()-1) processedPeaks.remove(i+1);
            }
        }

        if (!parentDeviation.inErrorWindow(parentmass, processedPeaks.get(parentPos).getMz())) {
            if (processedPeaks.get(parentPos).getMz()-parentmass > 0) processedPeaks.remove(parentPos); //closest peak heavier than real peak;
            // parent peak is not contained. Create a synthetic one
            final ProcessedPeak syntheticParent = new ProcessedPeak();
            syntheticParent.setIon(experiment.getIonization());
            syntheticParent.setMz(parentmass);
            processedPeaks.add(syntheticParent);
            if (VERBOSE) System.out.println("synthetic peak created");
        }

        assert parentDeviation.inErrorWindow(parentmass, processedPeaks.get(processedPeaks.size()-1).getMz()) : "heaviest peak is parent peak";
        // the heaviest fragment that is possible is M - H
        // everything which is heavier is noise
        final double threshold = parentmass + experiment.getMeasurementProfile().getAllowedMassDeviation().absoluteFor(parentmass) - PeriodicTable.getInstance().getByName("H").getMass();
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size()-1);
        // delete all peaks between parentmass-H and parentmass except the parent peak itself
        for (int i = processedPeaks.size()-2; i >= 0; --i) {
            if (processedPeaks.get(i).getMz() <= threshold) break;
            processedPeaks.set(processedPeaks.size() - 2, parentPeak);
            processedPeaks.remove(processedPeaks.size()-1);
        }
        return parentPeak;
    }


    private void testForDerivatization(Ms2ExperimentImpl experiment, List<ProcessedPeak> processedPeaks){
        final double massDeviationPenalty = 3.0;
        //test for derivatization
        if (useDerivates){
            if (VERBOSE) System.out.println("Derivatization: ");

            for (ProcessedPeak p : processedPeaks){
                if (p.getMass()>181 && p.getMass()<182){
                    double score = MathUtils.erfc(Math.abs(p.getMass() - (181.007665 - ELECTRON_MASS)) * massDeviationPenalty / getErrorForMass(experiment, p.getMass(), p.getRelativeIntensity()) / Math.sqrt(2));
                    if (score>0.3){
                        usePFB=true;
                        isPFBcompound =true;
                        if (VERBOSE) System.out.println("increase PFB score");
                    }
                } else if (p.getMass()>73 && p.getMass()<74){
                    double score = MathUtils.erfc(Math.abs(p.getMass()-(73.047352-ELECTRON_MASS))*massDeviationPenalty/ getErrorForMass(experiment, p.getMass(), p.getRelativeIntensity())/Math.sqrt(2));
                    if (score>0.3){
                        useTMS=true;
                        isTMScompound =true;
                        if (VERBOSE) System.out.println("increase TMS score");
                    }
                }
            }
            if(!usePFB && !useTMS){
                usePFB=true;
                useTMS=true;
            }
        }

        PeriodicTable periodicTable = PeriodicTable.getInstance();
        boolean tmsAlreadyKnown = false;
        boolean pfbAlreadyKnown = false;
        List<Element> usedElements = experiment.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet().getElements();
        for (Element usedElement : usedElements) {
            if (usePFB){
                if (usedElement.equals(periodicTable.getByName("Tms"))) tmsAlreadyKnown = true;
            }
            if (useTMS){
                if (usedElement.equals(periodicTable.getByName("Pfb"))) pfbAlreadyKnown = true;
            }
        }

        List<Element> newElementsList = new ArrayList<Element>(usedElements);
        if (!tmsAlreadyKnown) newElementsList.add(periodicTable.getByName("Tms"));
        if (!pfbAlreadyKnown) newElementsList.add(periodicTable.getByName("Pfb"));
        if (!tmsAlreadyKnown || !pfbAlreadyKnown) {
            List<FormulaFilter> filters = experiment.getMeasurementProfile().getFormulaConstraints().getFilters();
            ChemicalAlphabet alphabet = new ChemicalAlphabet(newElementsList.toArray(new Element[0]));
            FormulaConstraints constraints = new FormulaConstraints(alphabet);
            for (FormulaFilter filter : filters) {
                constraints.addFilter(filter);
            }
            MutableMeasurementProfile measurementProfile = new MutableMeasurementProfile(experiment.getMeasurementProfile());
            measurementProfile.setFormulaConstraints(constraints);
            experiment.setMeasurementProfile(measurementProfile);
        }
    }


    /**
     * remove isotope peaks from spectrum.
     * @param experiment
     * @param processedPeaks
     * @return
     */
    private List<ProcessedPeak> removeIsotopePeaks(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks){
        //todo so far isotopes are only removed but not scored -> first improve isotope detection by simulating these
        List<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(processedPeaks);

        //todo already sorted?
        Collections.sort(peaks, new ProcessedPeak.MassComparator());

        double cl =0;
        if (useHalogens || useChlorine){
            cl = 36.96590259 - 34.96885268;
        }

        int isotopePeaks=0;
        ProcessedPeak mono, iso;
        int monoCounter =0;
        int isoCounter =1;

        Map<ProcessedPeak, List<ProcessedPeak>> isotopesMap = new HashMap<ProcessedPeak, List<ProcessedPeak>>();

        while(monoCounter<peaks.size() && isoCounter<peaks.size()){
            mono = peaks.get(monoCounter);
            isotopesMap.put(mono, new ArrayList<ProcessedPeak>());
            isoCounter=monoCounter+1;
            while (isoCounter<peaks.size()){
                iso = peaks.get(isoCounter);
                double diffToMono=0;
                diffToMono =  iso.getMass() - mono.getMass();

                double error = getErrorForMass(experiment, iso.getMz(), iso.getRelativeIntensity());
                if (diffToMono< NEUTRON_MASS-error){
                    isoCounter++;
                }else if (diffToMono >= NEUTRON_MASS-error && diffToMono <= NEUTRON_MASS+error && (0.3*mono.getRelativeIntensity()>iso.getRelativeIntensity())){      //todo 0.3 well-founded or just tested???
                    isotopesMap.get(mono).add(iso);
                    peaks.remove(iso);
                    ++isotopePeaks;
                }else if ((useHalogens || useChlorine) && diffToMono>= cl-error && diffToMono<= cl+error && mono.getRelativeIntensity()> iso.getRelativeIntensity()){  // cl isotope
                    isotopesMap.get(mono).add(iso);
                    peaks.remove(iso);
                    ++isotopePeaks;
                }else if (diffToMono > NEUTRON_MASS+error && diffToMono > cl+error){
                    ++monoCounter;
                    break;
                }else isoCounter++;
            }
        }

        if (VERBOSE) System.out.println("Found "+isotopePeaks+" isotope peaks out of "+processedPeaks.size());
        return peaks;
    }

    /**
     * set the relative intensity in all experimental deviations
     * @param experiment
     * @param relIntensity
     */
    private void setIntensityInMeasurmentProfileDeviations(Ms2ExperimentImpl experiment, double relIntensity){
        MeasurementProfile currentProfile = experiment.getMeasurementProfile();
        MutableMeasurementProfile mutableProfile = new MutableMeasurementProfile(currentProfile);
        EIIntensityDeviation deviation = (EIIntensityDeviation)currentProfile.getAllowedMassDeviation();
        if (deviation!=null) deviation.setRelIntensity(relIntensity);
        mutableProfile.setAllowedMassDeviation(deviation);
        deviation = (EIIntensityDeviation)currentProfile.getStandardMs1MassDeviation();
        if (deviation!=null) deviation.setRelIntensity(relIntensity);
        mutableProfile.setStandardMs1MassDeviation(deviation);
        deviation = (EIIntensityDeviation)currentProfile.getStandardMs2MassDeviation();
        if (deviation!=null) deviation.setRelIntensity(relIntensity);
        mutableProfile.setStandardMs2MassDeviation(deviation);
        deviation = (EIIntensityDeviation)currentProfile.getStandardMassDifferenceDeviation();
        if (deviation!=null) deviation.setRelIntensity(relIntensity);
        mutableProfile.setStandardMassDifferenceDeviation(deviation);
        experiment.setMeasurementProfile(mutableProfile);
    }

    private Deviation intensityDeviation(double intensity, Deviation deviation){
        final EIIntensityDeviation intensityDeviation = (EIIntensityDeviation)deviation;
        intensityDeviation.setRelIntensity(intensity);
        return intensityDeviation;
    }

    private double getErrorForMass(Ms2Experiment experiment, double center, double intensity){
        return intensityDeviation(intensity, experiment.getMeasurementProfile().getAllowedMassDeviation()).absoluteFor(center);
    }


    public boolean isUseHalogens() {
        return useHalogens;
    }

    public void setUseHalogens(boolean useHalogens) {
        this.useHalogens = useHalogens;
    }

    public boolean isUseChlorine() {
        return useChlorine;
    }

    public void setUseChlorine(boolean useChlorine) {
        this.useChlorine = useChlorine;
    }

    public boolean isRemoveIsotopePeaks() {
        return removeIsotopePeaks;
    }

    public void setRemoveIsotopePeaks(boolean removeIsotopePeaks) {
        this.removeIsotopePeaks = removeIsotopePeaks;
    }

    public boolean isUseDerivates() {
        return useDerivates;
    }

    public void setUseDerivates(boolean useDerivates) {
        this.useDerivates = useDerivates;
    }

    public boolean isUsePFB() {
        return usePFB;
    }

    public void setUsePFB(boolean usePFB) {
        this.usePFB = usePFB;
    }

    public boolean isUseTMS() {
        return useTMS;
    }

    public void setUseTMS(boolean useTMS) {
        this.useTMS = useTMS;
    }

    /**
     * this MolecularFormula implementation is needed to cover the special TMS --(CH3-Loss)--> DMS case
     */
    class DerivatesMolecularFormula extends MutableMolecularFormula {
        private Element tmsElement = PeriodicTable.getInstance().getByName("Tms");
        private Element dmsElement = PeriodicTable.getInstance().getByName("Dms");
        private Element carbon = PeriodicTable.getInstance().getByName("C");
        private Element hydrogen = PeriodicTable.getInstance().getByName("H");

        DerivatesMolecularFormula(MolecularFormula formula){
            super(formula);
        }

        @Override
        public MolecularFormula subtract(MolecularFormula other) {
            if (this.equals(dummyFormula)) return MolecularFormula.parse(""); //no loss known

            if (this.numberOf(tmsElement)>0 && other.numberOf(dmsElement)>0){
                MutableMolecularFormula substract = new MutableMolecularFormula(super.subtract(other));
                final int substractDmsAmount = substract.numberOf(dmsElement);
                if (substractDmsAmount<0) {
                    substract.set(carbon, substract.numberOfCarbons()-substractDmsAmount);
                    substract.set(hydrogen, substract.numberOfHydrogens()-3*substractDmsAmount);
                    substract.set(tmsElement, substract.numberOf(tmsElement)+substractDmsAmount);
                    substract.set(dmsElement, 0);
                }
                //todo problem with equals of mutable formulas (only to immutable?): different amount-array sizes:
                //todo [3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] and immutable [3, 1]
                //todo desired??
                //return substract;
                return MolecularFormula.from(substract);

            }

            return super.subtract(other);
        }

        @Override
        public boolean isSubtractable(MolecularFormula other) {
            if (this.equals(dummyFormula)) return true; //anything is subset of dummy
            //Tms can loose a CH3 which leads to Dms. To cover this case ->
            if (this.numberOf(tmsElement)>0 && other.numberOf(dmsElement)>0){
                final int thisTmsAmount = this.numberOf(tmsElement);
                final int thisDmsAmount = this.numberOf(dmsElement);
                final int otherTmsAmount = other.numberOf(tmsElement);

                final int difference = thisTmsAmount-otherTmsAmount;
                if (difference<0) return false;

                if (thisDmsAmount+difference<=Short.MAX_VALUE){
                    this.set(dmsElement, thisDmsAmount+difference);
                } else {
                    System.err.println("element amount in isSubtractable calculation exceeds Short.MAX_VALUE for "+this+" and "+other);
                }


                final boolean isSubtractable = super.isSubtractable(other);
                this.set(dmsElement, thisDmsAmount);

//                //GCMSTool version
//                //
//                final int otherDmsAmount = other.numberOf(dmsElement);
//                this.set(tmsElement, 0);
//                ((DerivatesMolecularFormula)other).set(dmsElement, 0);
//
//                final boolean isSubtractable = super.isSubtractable(other);
//                ((DerivatesMolecularFormula)other).set(dmsElement, otherDmsAmount);
//                this.set(tmsElement, thisTmsAmount);
                return isSubtractable;
            }
            return super.isSubtractable(other);
        }
    }
}
