package de.unijena.bioinf.GibbsSampling;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.ChemistryBase.ms.ft.ZodiacScore;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.jjobs.MasterJJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class Zodiac {
    private final Logger Log;
    List<ExperimentResult> experimentResults;
    List<LibraryHit> anchors;
    NodeScorer[] nodeScorers;
    EdgeScorer<FragmentsCandidate>[] edgeScorers;
    EdgeFilter edgeFilter;
    int maxCandidates; //todo always use all!?
    private final boolean clusterCompounds;
    private final boolean runTwoStep;

    MasterJJob masterJJob;

    String[] ids;
    FragmentsCandidate[][] candidatesArray;

    Map<String, String[]> representativeToCluster;

    public Zodiac(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, MasterJJob masterJJob) throws ExecutionException {
        this(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, clusterCompounds, true, masterJJob);
    }

    public Zodiac(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, boolean runTwoStep , MasterJJob masterJJob) throws ExecutionException {
        this.experimentResults = experimentResults;
        this.anchors = anchors==null?Collections.emptyList():anchors;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.maxCandidates = maxCandidates;
        this.masterJJob = masterJJob;
        this.clusterCompounds = clusterCompounds;
        this.runTwoStep = runTwoStep;
        this.Log = masterJJob!=null?masterJJob.LOG():LoggerFactory.getLogger(Zodiac.class);
    }

    public Zodiac(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds) throws ExecutionException {
        this(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, clusterCompounds, true, null);
    }

    public Zodiac(List<ExperimentResult> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, boolean runTwoStep) throws ExecutionException {
        this(experimentResults, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, clusterCompounds, runTwoStep, null);
    }

    public ZodiacResultsWithClusters compute(int iterationSteps, int burnIn, int repetitions) throws ExecutionException {
        init();
        if (ids.length==0){
            Log.error("Cannot run ZODIAC. No/empty SIRIUS input provided.");
            return null;
        } else if (ids.length==1){
            Log.error("Don't run ZODIAC. Only a single compound in SIRIUS input.");
            return createOneCompoundOutput();
        }

        ZodiacResult<FragmentsCandidate> zodiacResult;
        if (runTwoStep){
            TwoPhaseGibbsSampling<FragmentsCandidate> twoPhaseGibbsSampling = new TwoPhaseGibbsSampling<>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, repetitions, FragmentsCandidate.class);
            twoPhaseGibbsSampling.setIterationSteps(iterationSteps, burnIn);
            if (masterJJob!=null) masterJJob.submitSubJob(twoPhaseGibbsSampling);
            else SiriusJobs.getGlobalJobManager().submitJob(twoPhaseGibbsSampling);

            zodiacResult = twoPhaseGibbsSampling.awaitResult();
        } else {
            zodiacResult = runOneStepZodiacOnly(iterationSteps, burnIn, repetitions);
        }

        CompoundResult<FragmentsCandidate>[] result = zodiacResult.getResults();

        addZodiacScoreToIdentificationResult(result, experimentResults);

        if (clusterCompounds) zodiacResult = includedAllClusterInstances(zodiacResult);
        else zodiacResult = new ZodiacResultsWithClusters(ids, zodiacResult.getGraph(), zodiacResult.getResults(), getSelfMapping(ids));

        return (ZodiacResultsWithClusters)zodiacResult;
    }

    private ZodiacResult<FragmentsCandidate> runOneStepZodiacOnly(int iterationSteps, int burnIn, int repetitions) throws ExecutionException {
        GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);

        Graph<FragmentsCandidate> graph;
        if (masterJJob!=null) graph = (Graph<FragmentsCandidate>)masterJJob.submitSubJob(graphBuilder).awaitResult();
        else graph = SiriusJobs.getGlobalJobManager().submitJob(graphBuilder).awaitResult();

        try {
            Graph.validateAndThrowError(graph, Log);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }


        GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel<>(graph, repetitions);
        gibbsParallel.setIterationSteps(iterationSteps, burnIn);

        if (masterJJob!=null) masterJJob.submitSubJob(gibbsParallel);
        else SiriusJobs.getGlobalJobManager().submitJob(gibbsParallel);

        CompoundResult<FragmentsCandidate>[] results = gibbsParallel.awaitResult();

        return new ZodiacResult<>(ids, graph, results);
    }

    private ZodiacResultsWithClusters createOneCompoundOutput(){
        if (ids.length!=1) throw new NoSuchMethodError("This method must only be used to output results of a single compound");

        Graph<FragmentsCandidate> graph = GraphBuilder.createGraph(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, null);


        Scored<FragmentsCandidate>[] possibleFormulasArray = graph.getPossibleFormulas(0);
        double[] scores = new double[possibleFormulasArray.length];
        double sum = 0;
        double maxLog = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < possibleFormulasArray.length; j++) {
            double score = possibleFormulasArray[j].getScore();
            if (score>maxLog) maxLog = score;
        }

        for (int j = 0; j < possibleFormulasArray.length; j++) {
            final double score = Math.exp(possibleFormulasArray[j].getScore()-maxLog);
            scores[j] = score;
            sum += score;
        }

        for (int j = 0; j < scores.length; j++) {
            scores[j] = scores[j]/sum;
        }


        Scored<FragmentsCandidate>[] results = new Scored[possibleFormulasArray.length];
        for (int i = 0; i < results.length; i++) {
            FragmentsCandidate result = possibleFormulasArray[i].getCandidate();
            results[i] = new Scored<>(result, scores[i]);
        }
        
        
        
        CompoundResult<FragmentsCandidate> compoundResult = new CompoundResult<>(ids[0], results);
        compoundResult.addAnnotation(Connectivity.class, new Connectivity(0));

        return new ZodiacResultsWithClusters(ids, graph, new CompoundResult[]{compoundResult}, getSelfMapping(ids));
    }

    private Map<String, String[]> getSelfMapping(String[] strings){
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            map.put(string, new String[]{string});
        }
        return map;
    }

    private ZodiacResultsWithClusters includedAllClusterInstances(ZodiacResult<FragmentsCandidate> zodiacResult){
        CompoundResult<FragmentsCandidate>[] results = zodiacResult.getResults();
        List<String> allIds = new ArrayList<>();
        List<CompoundResult<FragmentsCandidate>> allCandidates = new ArrayList<>();
        for (CompoundResult<FragmentsCandidate> result : results) {
            String repId = result.getId();
            String[] cluster = representativeToCluster.get(repId); //also contains rep

            for (String c : cluster) {
                allIds.add(c);
                allCandidates.add(result.withNewId(c));
            }
        }
        return new ZodiacResultsWithClusters(allIds.toArray(new String[0]), zodiacResult.getGraph(), allCandidates.toArray(new CompoundResult[0]), representativeToCluster);
    }


    private void addZodiacScoreToIdentificationResult(CompoundResult<FragmentsCandidate>[] result, List<ExperimentResult> experimentResults){
        //todo add score to FTree not IdentificationResult?!?!!?!?!
        Map<String, CompoundResult<FragmentsCandidate>> idToCompoundResult = createInstanceMap(result);//contains all compounds (even all clustered)
        for (ExperimentResult experimentResult : experimentResults) {
            List<IdentificationResult> identificationResults = experimentResult.getResults();
            if (identificationResults.size()==0) continue;

            Ms2Experiment experiment = experimentResult.getExperiment();
            String id = experiment.getName();
            CompoundResult<FragmentsCandidate> compoundResult = idToCompoundResult.get(id);
            if (compoundResult==null){
                //some ExperimentResult with no results was provided
                Log.warn("no Zodiac result for compound with id "+id+".");
                continue;
            }
            Scored<FragmentsCandidate>[] zodiacResults = compoundResult.getCandidates();
            Map<MolecularFormula, IdentificationResult> idResultMap = createIdentificationResultMap(identificationResults);
            for (Scored<FragmentsCandidate> zodiacResult : zodiacResults) {
                if (zodiacResult.getCandidate() instanceof DummyFragmentCandidate) continue;
                ZodiacScore zodiacScore = new ZodiacScore(zodiacResult.getScore());
                MolecularFormula mf = zodiacResult.getCandidate().getFormula();
                IdentificationResult identificationResult = idResultMap.get(mf);
                if (identificationResult==null){
                    //formula not found: might happen for clustered compounds
//                    if (zodiacScore.getProbability()>0){
//                        Log.warn("could not match Zodiac result to Sirius results");
//                    }

                    if (zodiacScore.getProbability()>0 && clusterCompounds && representativeToCluster.containsKey(id)){
                        Log.error("Zodiac results and Sirius results contain different molecular formula candiates for compoumound "+id+".");
                    } else if (zodiacScore.getProbability()>0.01){
                        Log.warn("A high scoring ZODIAC molecular formula candidate is not contained in SIRIUS top hits.\n" +
                                "This might occur if clustered commpounds possess different SIRIUS molecular formula candidates.\n" +
                                "You might increase the number of SIRIUS output candidates or disable clustering in ZODIAC. Compound id: "+id);
                    }
                } else {
                    identificationResult.setAnnotation(ZodiacScore.class, zodiacScore);
                    identificationResult.getResolvedTree().setAnnotation(ZodiacScore.class, zodiacScore);
                }
            }
        }
    }

    private Map<String, CompoundResult<FragmentsCandidate>> createInstanceMap(CompoundResult<FragmentsCandidate>[] result) {
        if (clusterCompounds) return createInstanceMapClusters(result);
        return createInstanceMapNoClusters(result);
    }

    private Map<String, CompoundResult<FragmentsCandidate>> createInstanceMapClusters(CompoundResult<FragmentsCandidate>[] result) {
        Map<String, CompoundResult<FragmentsCandidate>> idToCompoundResult = new HashMap<>();
        Map<String, CompoundResult<FragmentsCandidate>> repIdToCompoundResult = new HashMap<>();
        for (int i = 0; i < result.length; i++) {
            CompoundResult<FragmentsCandidate> compoundResult = result[i];
            String id = compoundResult.getId();
            assert !repIdToCompoundResult.containsKey(id);
            repIdToCompoundResult.put(id, compoundResult);
        }
        for (Map.Entry<String, String[]> stringEntry : representativeToCluster.entrySet()) {
            String rep = stringEntry.getKey();
            String[] compounds = stringEntry.getValue();
            for (String compound : compounds) {
                assert !idToCompoundResult.containsKey(compound);
                idToCompoundResult.put(compound, repIdToCompoundResult.get(rep));
            }
        }
        return idToCompoundResult;
    }

    private Map<String, CompoundResult<FragmentsCandidate>> createInstanceMapNoClusters(CompoundResult<FragmentsCandidate>[] result) {
        Map<String, CompoundResult<FragmentsCandidate>> idToCompoundResult = new HashMap<>();
        for (int i = 0; i < result.length; i++) {
            CompoundResult<FragmentsCandidate> compoundResult = result[i];
            String id = compoundResult.getId();
            idToCompoundResult.put(id, compoundResult);
        }
        return idToCompoundResult;
    }

    private TObjectIntHashMap<MolecularFormula> createIndexMap(Scored<FragmentsCandidate>[] result) {
        TObjectIntHashMap<MolecularFormula> indexMap = new TObjectIntHashMap<>(result.length, 0.75f, -1);
        for (int i = 0; i < result.length; i++) {
            indexMap.put(result[i].getCandidate().getFormula(), i);
        }
        return indexMap;
    }

    private Map<MolecularFormula, IdentificationResult> createIdentificationResultMap(List<IdentificationResult> result) {
        Map<MolecularFormula, IdentificationResult> resultMap = new HashMap<>(result.size(), 0.75f);
        for (IdentificationResult identificationResult : result) {
            final MolecularFormula mf = identificationResult.getMolecularFormula();
            assert !resultMap.containsKey(mf);
            resultMap.put(mf, identificationResult);
        }
        return resultMap;
    }


    private void init(){
        Map<String, List<FragmentsCandidate>> candidatesMap = new HashMap<>();
        Set<String> experimentIDSet = new HashSet<>();
        for (ExperimentResult result : experimentResults) {
            List<FTree> trees = new ArrayList<>();
            for (IdentificationResult identificationResult : result.getResults()) {
//                trees.add(identificationResult.getRawTree()); //changed do we want to include H2O and similar in-source losses? What about adducts?
                trees.add(identificationResult.getResolvedTree()); //todo use rawTree or resolvedTree?!
            }


            Ms2Experiment experiment = result.getExperiment();
            List<FragmentsCandidate> candidates = FragmentsCandidate.createAllCandidateInstances(trees, experiment);

            Collections.sort(candidates);
            if (candidates.size() > 0) candidatesMap.put(experiment.getName(), candidates);
            experimentIDSet.add(experiment.getName());
        }

        for (LibraryHit anchor : anchors) {
            String id = anchor.getQueryExperiment().getName();
            List<FragmentsCandidate> candidatesList = candidatesMap.get(id);

            if (!experimentIDSet.contains(id)) {
                //library hits found in mgf. But there are no candidates (ExperimentResult) available.
                Log.warn("No compound in SIRIUS workspace found which corresponds to spectral library hit with id "+id+".");
            }
            if (candidatesList==null) continue;

            for (FragmentsCandidate candidate : candidatesList) {
                candidate.setLibraryHit(anchor);
            }
        }



        //parse reactions
        Reaction[] reactions = ZodiacUtils.parseReactions(1);
        Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
        for (Reaction reaction : reactions) {
            netSingleReactionDiffs.add(reaction.netChange());
        }

        //set 'correct' hits
        setKnownCompounds(candidatesMap, netSingleReactionDiffs);


        //add dummy
        ZodiacUtils.addNotExplainableDummy(candidatesMap, maxCandidates, Log);


        //cluster compounds
        if (clusterCompounds){
            representativeToCluster = ZodiacUtils.clusterCompounds(candidatesMap,Log);
            candidatesMap = ZodiacUtils.mergeCluster(candidatesMap, representativeToCluster);
            Log.info("Generated " + candidatesMap.size()+" compound clusters from "+experimentResults.size()+" compounds.");
        }



        ids = candidatesMap.keySet().toArray(new String[0]);
        candidatesArray = new FragmentsCandidate[ids.length][];

        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            candidatesArray[i] = candidatesMap.get(id).toArray(new FragmentsCandidate[0]);
        }

    }




    private void setKnownCompounds(Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences) {
        //create fragment candidates and match library hits
        Set<String> ids = candidatesMap.keySet();
        for (String id : ids) {
            final List<FragmentsCandidate> candidateList = candidatesMap.get(id);
            if (!candidateList.get(0).hasLibraryHit()) continue;

            final LibraryHit libraryHit = candidateList.get(0).getLibraryHit();

            //todo at least 5 peaks match, no cosine threshold?
            if (libraryHit.getSharedPeaks() < 5) continue;

            MolecularFormula correctMF = libraryHit.getMolecularFormula();
            List<FragmentsCandidate> candidates = candidatesMap.get(id);

            //todo does the ionization of library hit and compound have to match!?
            for (FragmentsCandidate candidate : candidates) {
                boolean matches = candidate.getFormula().equals(correctMF);
                if (!matches) {
                    MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                    if (diff.getMass() < 0) diff = diff.negate();
                    matches = allowedDifferences.contains(diff);
                }
                if (matches) {
                    candidate.setCorrect(true);
                    Log.info("Compound " + id + " has library hit. Candidate MF is " + candidate.getFormula() + ". Library hit is " + correctMF+".");
                }
                candidate.setInTrainingSet(true);


            }
        }
    }


    /**
     * A map of each cluster's representative to the cluster itself
     * @return
     */
    public Map<String, String[]> getClusterRepresentatives() {
        return representativeToCluster;
    }


    public Map<String, String> getInstanceToClusterRepresentative() {
        Map<String, String> instanceToCluster = new HashMap<>();
        for (Map.Entry<String, String[]> stringEntry : representativeToCluster.entrySet()) {
            String rep = stringEntry.getKey();
            String[] compounds = stringEntry.getValue();
            for (String compound : compounds) {
                assert !instanceToCluster.containsKey(compound);
                instanceToCluster.put(compound, rep);
            }
        }
        return instanceToCluster;
    }


    public List<ExperimentResult> getAnnotatedExperimentResults(){
        return experimentResults;
    }

}
