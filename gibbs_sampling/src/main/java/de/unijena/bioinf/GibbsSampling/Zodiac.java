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

package de.unijena.bioinf.GibbsSampling;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.InterruptionCheck;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.MasterJJob;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Zodiac {
    private final Logger Log;
    Map<Ms2Experiment, List<FTree>> siriusScoredTrees;
    Map<Ms2Experiment, Map<FTree, ZodiacScore>> zodiacScoredTrees; //todo should this be part of the ZodiacResult?

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

    public Zodiac(List<Ms2Experiment> experiments, List<List<FTree>> trees, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, MasterJJob masterJJob) throws ExecutionException {
        this(experiments, trees, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, clusterCompounds, true, masterJJob);
    }

    public Zodiac(List<Ms2Experiment> experiments, List<List<FTree>> trees, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds) throws ExecutionException {
        this(experiments, trees, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, clusterCompounds, true, null);
    }

    public Zodiac(List<Ms2Experiment> experiments, List<List<FTree>> trees, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, boolean runTwoStep) throws ExecutionException {
        this(experiments, trees, anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, clusterCompounds, runTwoStep, null);
    }

    public Zodiac(List<Ms2Experiment> experiments, List<List<FTree>> trees, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, boolean runTwoStep, MasterJJob masterJJob) throws ExecutionException {
        this(IntStream.range(0, experiments.size()).boxed().collect(Collectors.toMap(experiments::get, trees::get)), anchors, nodeScorers, edgeScorers, edgeFilter, maxCandidates, clusterCompounds, runTwoStep, masterJJob);
    }

    public Zodiac(Map<Ms2Experiment, List<FTree>> experimentResults, List<LibraryHit> anchors, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int maxCandidates, boolean clusterCompounds, boolean runTwoStep, MasterJJob masterJJob) throws ExecutionException {
        this.siriusScoredTrees = experimentResults;
        this.anchors = anchors == null ? Collections.emptyList() : anchors;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.maxCandidates = maxCandidates;
        this.masterJJob = masterJJob;
        this.clusterCompounds = clusterCompounds;
        this.runTwoStep = runTwoStep;
        this.Log = masterJJob != null ? LoggerFactory.getLogger(masterJJob.loggerKey()) : LoggerFactory.getLogger(Zodiac.class); //todo this is a bit ugly, we should use log through the job API instead (correct Job ID prefixing).
    }

    public JJob<ZodiacResultsWithClusters> makeComputeJob(final int iterationSteps, final int burnIn, final int repetitions) {
        return new BasicMasterJJob<ZodiacResultsWithClusters>(JJob.JobType.CPU) {
            @Override
            protected ZodiacResultsWithClusters compute() throws Exception {
                masterJJob = this;
                init();
                if (ids.length<=1) {
                    Log.error("Cannot run ZODIAC. SIRIUS input consists of " + ids.length + " instances. More are needed for running a network analysis.");
                    return null;
                }

                checkForInterruption();

                ZodiacResult<FragmentsCandidate> zodiacResult;
                if (runTwoStep){
                    TwoPhaseGibbsSampling<FragmentsCandidate> twoPhaseGibbsSampling = new TwoPhaseGibbsSampling<>(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, repetitions, FragmentsCandidate.class);
                    twoPhaseGibbsSampling.setIterationSteps(iterationSteps, burnIn);
                    if (masterJJob!=null) masterJJob.submitSubJob(twoPhaseGibbsSampling);
                    else this.submitSubJob(twoPhaseGibbsSampling);
                    zodiacResult = twoPhaseGibbsSampling.awaitResult();
                } else {
                    zodiacResult = runOneStepZodiacOnly(iterationSteps, burnIn, repetitions, this::checkForInterruption);
                }

                checkForInterruption();
                zodiacScoredTrees = mapZodiacScoresToFTrees(zodiacResult.getResults());
                checkForInterruption();
                if (clusterCompounds) zodiacResult = includedAllClusterInstances(zodiacResult);
                else zodiacResult = new ZodiacResultsWithClusters(ids, zodiacResult.getGraph(), zodiacResult.getResults(), getSelfMapping(ids));
                return (ZodiacResultsWithClusters)zodiacResult;
            }
        };
    }

    private ZodiacResult<FragmentsCandidate> runOneStepZodiacOnly(int iterationSteps, int burnIn, int repetitions, @NotNull InterruptionCheck interruption) throws ExecutionException, InterruptedException {
        Log.info("ZODIAC: Graph building.");
        GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);

        Graph<FragmentsCandidate> graph;
        if (masterJJob!=null) graph = masterJJob.submitSubJob(graphBuilder).awaitResult();
        else graph = SiriusJobs.getGlobalJobManager().submitJob(graphBuilder).awaitResult();

        interruption.check();

        try {
            Graph.validateAndThrowError(graph, Log::warn);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }

        interruption.check();

        Log.info("ZODIAC: run sampling.");
        GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel<>(graph, repetitions);
        gibbsParallel.setIterationSteps(iterationSteps, burnIn);

        if (masterJJob!=null) masterJJob.submitSubJob(gibbsParallel);
        else SiriusJobs.getGlobalJobManager().submitJob(gibbsParallel);

        CompoundResult<FragmentsCandidate>[] results = gibbsParallel.awaitResult();

        interruption.check();

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


    private Map<Ms2Experiment, Map<FTree, ZodiacScore>> mapZodiacScoresToFTrees(CompoundResult<FragmentsCandidate>[] result) {
        final Map<String, CompoundResult<FragmentsCandidate>> idToCompoundResult = createInstanceMap(result);//contains all compounds (even all clustered)
        final Map<Ms2Experiment, Map<FTree, ZodiacScore>> zodiacScoredTrees = new HashMap<>(siriusScoredTrees.size());

        for (Map.Entry<Ms2Experiment, List<FTree>> experimentResult : siriusScoredTrees.entrySet()) {
            if (experimentResult.getValue().size() == 0) continue;

            Ms2Experiment experiment = experimentResult.getKey();
            String id = getUniqueExperimentId(experiment);
            CompoundResult<FragmentsCandidate> compoundResult = idToCompoundResult.get(id);
            if (compoundResult==null){
                //some ExperimentResult with no results was provided
                Log.warn("no Zodiac result for compound with id "+id+".");
                continue;
            }
            Scored<FragmentsCandidate>[] zodiacResults = compoundResult.getCandidates();
            Map<MolecularFormula, FTree> idResultMap = createIdentificationResultMap(experimentResult.getValue());
            for (Scored<FragmentsCandidate> zodiacResult : zodiacResults) {
                if (zodiacResult.getCandidate() instanceof DummyFragmentCandidate) continue;
                final ZodiacScore zodiacScore = new ZodiacScore(zodiacResult.getScore());
                MolecularFormula mf = zodiacResult.getCandidate().getFormula();
                FTree ftree = idResultMap.get(mf);
                if (ftree == null) {
                    //formula not found: might happen for clustered compounds
//                    if (zodiacScore.getProbability()>0){
//                        Log.warn("could not match Zodiac result to Sirius results");
//                    }
                    if (zodiacScore.score() > 0 && clusterCompounds && representativeToCluster.containsKey(id)) {
                        Log.error("Zodiac results and Sirius results contain different molecular formula candiates for compoumound "+id+".");
                    } else if (zodiacScore.score() > 0.01) {
                        Log.warn("Instance " + id + ": The high scoring ZODIAC molecular formula " + mf +  " with score " + zodiacScore.toString() +
                                " is not contained in SIRIUS top hits.\n" +
                                "This might occur if clustered commpounds possess different SIRIUS molecular formula candidates.\n" +
                                "You might increase the number of SIRIUS output candidates or disable clustering in ZODIAC. Compound id: "+id + " and cluster is " + Arrays.toString(representativeToCluster.get(id)));
                    }
                } else {
                    zodiacScoredTrees.computeIfAbsent(experimentResult.getKey(), (key) -> new HashMap<>(experimentResult.getValue().size()))
                            .put(ftree, zodiacScore);
                }
            }
        }
        return zodiacScoredTrees;
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

    // TODO: Kaidu: PLEASE change this. It is soo annoying to do these kind of workarounds.
    private String getUniqueExperimentId(Ms2Experiment experiment) {
        return experiment.getName() + "_" + Objects.hashCode(experiment); //hash code should be unique. name just for readability
    }

    private Map<MolecularFormula, FTree> createIdentificationResultMap(List<FTree> result) {
        Map<MolecularFormula, FTree> resultMap = new HashMap<>(result.size(), 0.75f);
        for (FTree identificationResult : result) {
            final MolecularFormula mf = identificationResult.getRoot().getFormula();
            assert !resultMap.containsKey(mf);
            resultMap.put(mf, identificationResult);
        }
        return resultMap;
    }


    private void init(){
        Map<String, List<FragmentsCandidate>> candidatesMap = new HashMap<>();
        Set<String> experimentIDSet = new HashSet<>();
        for (Map.Entry<Ms2Experiment, List<FTree>> result : siriusScoredTrees.entrySet()) {
            Ms2Experiment experiment = result.getKey();
            List<FTree> trees = new ArrayList<>(result.getValue());
            /*for (IdentificationResult identificationResult : result.getResults()) {
//                trees.add(identificationResult.getRawTree()); //changed do we want to include H2O and similar in-source losses? What about adducts?
                trees.add(identificationResult.getResolvedTree()); //todo use rawTree or resolvedTree?!
            }*/

            List<FragmentsCandidate> candidates;
            try {
                 candidates = FragmentsCandidate.createAllCandidateInstances(trees, experiment);
            } catch (Exception e){
                Log.error("Error in ZODIAC processing: Could not create FragmentsCandidate object for compound {} from source {}", experiment.getName(), experiment.getSourceString());
                throw e;
            }


            Collections.sort(candidates);
            if (candidates.size() > 0) candidatesMap.put(getUniqueExperimentId(experiment), candidates);
            experimentIDSet.add(getUniqueExperimentId(experiment));
        }

        for (LibraryHit anchor : anchors) {
            String id = getUniqueExperimentId(anchor.getQueryExperiment());
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
        ZodiacUtils.addNotExplainableDummyAndTruncateCandidateList(candidatesMap, maxCandidates, Log);


        //cluster compounds
        if (clusterCompounds){
            representativeToCluster = ZodiacUtils.clusterCompounds(candidatesMap,Log);
            candidatesMap = ZodiacUtils.mergeCluster(candidatesMap, representativeToCluster);
            Log.info("Generated " + candidatesMap.size() + " compound clusters from " + siriusScoredTrees.size() + " compounds.");
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

    public Map<Ms2Experiment, Map<FTree, ZodiacScore>> getZodiacScoredTrees() {
        return zodiacScoredTrees;
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

}
