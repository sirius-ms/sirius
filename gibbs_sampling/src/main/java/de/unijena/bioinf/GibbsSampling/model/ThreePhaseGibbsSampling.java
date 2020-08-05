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

package de.unijena.bioinf.GibbsSampling.model;

/**
 * compute Gibbs Sampling first with good quality spectra. Then, insert other ones and compute again.
 */
public class ThreePhaseGibbsSampling {
//    private static final Logger LOG = LoggerFactory.getLogger(ThreePhaseGibbsSampling.class);
//    private String[] ids;
//    private FragmentsCandidate[][] possibleFormulas;
//    private NodeScorer<FragmentsCandidate>[] nodeScorers;
//    private EdgeScorer<FragmentsCandidate>[] edgeScorers;
//    private EdgeFilter edgeFilter;
//    private JobManager jobManager;
//    private int repetitions;
//    private Class<FragmentsCandidate> cClass;
//
//    private Scored<FragmentsCandidate>[][] results1;
//    private Scored<FragmentsCandidate>[][] results2;
//    private Scored<FragmentsCandidate>[][] combinedResult;
//    private String[] usedIds;
//
//    private Graph<FragmentsCandidate> graph;
//    private GibbsParallel<FragmentsCandidate> gibbsParallel;
//    private String[] firstRoundIds;
//    private TIntArrayList firstRoundCompoundsIdx;
//
//    private int numberOfCandidatesFirstRound;
//
//
//    //todo extend from TwoPhaseGibbsSampling
//    public ThreePhaseGibbsSampling(String[] ids, FragmentsCandidate[][] possibleFormulas, int numberOfCandidatesFirstRound, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, JobManager jobManager, int repetitions) throws ExecutionException {
//        this.ids = ids;
//        this.possibleFormulas = possibleFormulas;
//        this.nodeScorers = nodeScorers;
//        this.edgeScorers = edgeScorers;
//        this.edgeFilter = edgeFilter;
//        this.jobManager = jobManager;
//        this.repetitions = repetitions;
//        this.numberOfCandidatesFirstRound = numberOfCandidatesFirstRound;
//        assertInput();
//        init();
//    }
//
//    private void assertInput(){
//        for (int i = 0; i < possibleFormulas.length; i++) {
//            FragmentsCandidate[] candidates = possibleFormulas[i];
//            for (int j = 0; j < candidates.length; j++) {
//                if (DummyFragmentCandidate.isDummy(candidates[j])){
//                    if (j<candidates.length-1){
//                        throw new RuntimeException("dummy node must be at last position of candidate list");
//                    }
//                }
//
//            }
//        }
//    }
//
//    private void init() throws ExecutionException {
//        firstRoundCompoundsIdx = new TIntArrayList();
//        for (int i = 0; i < possibleFormulas.length; i++) {
//            FragmentsCandidate[] poss = possibleFormulas[i];
//            if (poss.length>0 && CompoundQuality.isNotBadQuality(poss[0].getExperiment())){
//                firstRoundCompoundsIdx.add(i);
//            }
//            if (cClass==null && poss.length>0) cClass = (Class<FragmentsCandidate>)poss[0].getClass();
//        }
//
//
//        FragmentsCandidate[][] firstRoundPossibleFormulas;
//        String[] firstRoundIds;
//
//        firstRoundPossibleFormulas = (FragmentsCandidate[][])Array.newInstance(cClass, firstRoundCompoundsIdx.size(), 1);
//        firstRoundIds = new String[firstRoundCompoundsIdx.size()];
//        for (int i = 0; i < firstRoundCompoundsIdx.size(); i++) {
//            FragmentsCandidate[] candidates = possibleFormulas[firstRoundCompoundsIdx.get(i)];
//            DummyFragmentCandidate dummy = extractDummy(candidates);
//            if (dummy==null) {
//                if (candidates.length>numberOfCandidatesFirstRound){
//                    candidates = Arrays.copyOfRange(candidates, 0, numberOfCandidatesFirstRound);
//                }
//            } else {
//                int maxCandidatesWithDummy = numberOfCandidatesFirstRound+1;
//                DummyFragmentCandidate newDummy = updateDummy(dummy, candidates, maxCandidatesWithDummy);
//                if (candidates.length>maxCandidatesWithDummy){
//                    candidates = Arrays.copyOfRange(candidates, 0, maxCandidatesWithDummy);
//                }
//                //dummy missing, add it
//                candidates[candidates.length-1] = newDummy;
//            }
//
//            firstRoundPossibleFormulas[i] = candidates;
//            firstRoundIds[i] = ids[firstRoundCompoundsIdx.get(i)];
//        }
//
//
//
//        LOG.info("run Zodiac on good quality compounds only. Use "+firstRoundIds.length+" of "+ids.length+" compounds.");
//        GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(firstRoundIds, firstRoundPossibleFormulas, nodeScorers, edgeScorers, edgeFilter, FragmentsCandidate.class);
//        jobManager.submitJob(graphBuilder); //todo might dead lock??
//        graph = graphBuilder.awaitResult();
//        gibbsParallel = new GibbsParallel<>(graph, repetitions);
//        graph = gibbsParallel.getGraph();
//    }
//
//    private DummyFragmentCandidate extractDummy(FragmentsCandidate[] candidates){
//        //todo always at last position?!?
////        for (C candidate : candidates) {
////            if (DummyFragmentCandidate.isDummy(candidate)) return (DummyFragmentCandidate)candidate;
////        }
//        if (DummyFragmentCandidate.isDummy(candidates[candidates.length-1])) return (DummyFragmentCandidate) candidates[candidates.length-1];
//        return null;
//    }
//
//    private DummyFragmentCandidate updateDummy(DummyFragmentCandidate dummy,FragmentsCandidate[] candidates, int maxCandidatesWithDummy){
//        if (maxCandidatesWithDummy>=candidates.length) return dummy;
//        int numberOfIgnored = dummy.getNumberOfIgnoredInstances();
//        numberOfIgnored += (candidates.length-maxCandidatesWithDummy);
//
//        double worstScore = candidates[maxCandidatesWithDummy-1].getScore();
//
//        return DummyFragmentCandidate.newDummy(worstScore, numberOfIgnored, dummy.getExperiment());
//
//    }
//
//    public void run(int maxSteps, final int burnIn) throws ExecutionException {
//        run(maxSteps, burnIn, jobManager);
//    }
//
//    public void run(int maxSteps, final int burnIn, JobManager jobManager) throws ExecutionException {
//
//        //1. run only on best quality spectra with restricted number of candidates
//        gibbsParallel.setIterationSteps(maxSteps, burnIn);
//        jobManager.submitJob(gibbsParallel);
//
//        gibbsParallel.awaitResult();
//        results1 = gibbsParallel.getChosenFormulas();
//
//        firstRoundIds = gibbsParallel.getGraph().getIds();
//
//
//        //2. now score 'all' candidates for each compound (rest of network is fixed on best only)
//        LOG.info("rerank candidates.");
//        //extract top hits;
//        Scored<FragmentsCandidate>[][] intermediateResults = results1.clone();
//        Scored<FragmentsCandidate>[][] scoredFixedCandidates = new Scored[firstRoundIds.length][];
//
//
//        Graph<FragmentsCandidate> graphWithFixedProbabilities = gibbsParallel.getGraph().replaceScoredCandidates(firstRoundIds, transformToLogScores(results1));
//        graphWithFixedProbabilities = graphWithFixedProbabilities.removeUnlikelyCandidates(0d);
//
//
//
//
//
//        //now replace each step one compound with all candidates and score them (not super efficient, but should play no big role compared to rest)
//        for (int i = 0; i < firstRoundCompoundsIdx.size(); i++) {
//            int firstRoundIdx = firstRoundCompoundsIdx.get(i);
//            FragmentsCandidate[] allCandidates = possibleFormulas[firstRoundIdx];
//            Scored<FragmentsCandidate>[] currentScoredCandidates = graph.getPossibleFormulas(i);
//
//            //changed comment out to test.
////            if (allCandidates.length==currentScoredCandidates.length) continue;
//
//            Scored<FragmentsCandidate>[] allScoredCandidates = new Scored[allCandidates.length];
//
//
//
//            //compute new node scores for all compound candidates
//            for (FragmentsCandidate candidate : allCandidates) {
//                candidate.clearNodeScores();
//            }
//            for (NodeScorer<FragmentsCandidate> nodeScorer : nodeScorers) {
//                nodeScorer.score(allCandidates);
//            }
//
//            for (int j = 0; j < allCandidates.length; j++) {
//                allScoredCandidates[j] = new Scored<>(allCandidates[j], allCandidates[j].getNodeLogProb());
//            }
//
//
//            Graph<FragmentsCandidate> oneCompoundOfInterestGraph = graphWithFixedProbabilities.extractOneCompound(i, allScoredCandidates, edgeScorers);
//
//
//            compareCompoundInteractions(graphWithFixedProbabilities, oneCompoundOfInterestGraph, i);
////            compareCompoundInteractions(gibbsParallel.getGraph(), oneCompoundOfInterestGraph, i);
////            compareCompoundCandidateScores(gibbsParallel.getGraph(), oneCompoundOfInterestGraph, i);
//
//
//            //todo is this working? (log odds vs log probs -> recalculate)
//            Scored<FragmentsCandidate>[] scoredCandidates = GibbsMFCorrectionNetwork.computeFromSnapshot(oneCompoundOfInterestGraph, i);
//
//
//            //test: compare sampling and not sampling
//            boolean error = false;
//            for (int j = 0; j < results1[i].length; j++) {
//                Scored<FragmentsCandidate> r1 = results1[i][j];
//                Scored<FragmentsCandidate> r2 = scoredCandidates[j];
//                double errorRate = Math.abs(r1.getScore()-r2.getScore()) / Math.max(r1.getScore(), 1e-3);
//                if (errorRate>1e-1 && Math.abs(r1.getScore()-r2.getScore())>0.03){
//                    error = true;
//                }
//            }
//            //todo for whatever reason errors improve if burn-in and iterations are increased!?!?!
//            if (error){
//                //maybe it is still correct
//                //this does not need to produce exactly same results.
//                System.out.println("big deviation");
//                scoredCandidates = GibbsMFCorrectionNetwork.computeFromSnapshot(oneCompoundOfInterestGraph, i);
//            } else {
//                System.out.println("great");
//            }
//
//            //update results with all candidates for compound i;
//            results1[i] = scoredCandidates;
//            //todo don't use new scores as the 'true' probabilities but only to spot missed candidates
//
//        }
//
//
//
//
//
//
//        //3. include bad quality spectra (could also do this in step 2) //todo use gibbs sampling??
//        if (firstRoundIds.length==possibleFormulas.length){
//            combinedResult = results1;
//            usedIds = gibbsParallel.getGraph().ids;
//            return;
//        }
//
//
//        LOG.info("score low quality compounds.");
//        //todo rather sample everything and just use results of low quality compounds? may there arise problems? in principle should not as we still sample all compounds (even 'fixed')
//        FragmentsCandidate[][] candidatesNewRound = combineNewAndOldAndSetFixedProbabilities(results1, firstRoundCompoundsIdx);
//        //todo this stupid thing creates a complete new graph.
//        TIntHashSet fixedIds = new TIntHashSet(firstRoundCompoundsIdx);
//        GraphBuilder<FragmentsCandidate> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesNewRound, nodeScorers, edgeScorers, edgeFilter, fixedIds, FragmentsCandidate.class);
//        jobManager.submitJob(graphBuilder); //todo might dead lock??
//        graph = graphBuilder.awaitResult();
//        gibbsParallel = new GibbsParallel<>(graph, repetitions, fixedIds);
//
//        gibbsParallel.setIterationSteps(maxSteps, burnIn);
//        jobManager.submitJob(gibbsParallel);
//
//        gibbsParallel.awaitResult();
//        results2 = gibbsParallel.getChosenFormulas();
//
//        usedIds = gibbsParallel.getGraph().ids;
//
//        combinedResult = combineResults(results1, firstRoundIds, results2, usedIds); //todo test. should not be necessary as all candidates are used anyways.
//
//    }
//
//    private void compareCompoundCandidateScores(Graph<FragmentsCandidate> graph1, Graph<FragmentsCandidate> graph2, int compoundIndex) {
//        Scored<FragmentsCandidate>[] scoredCandidates = graph1.getPossibleFormulas(compoundIndex);
//        Scored<FragmentsCandidate>[] scoredCandidates2 = graph2.getPossibleFormulas(compoundIndex);
//
//
//
//        for (int i = 0; i < scoredCandidates.length; i++) {
//            Scored<FragmentsCandidate> scoredCandidate = scoredCandidates[i];
//            boolean found = false;
//            for (int j = 0; j < scoredCandidates2.length; j++) {
//                Scored<FragmentsCandidate> scoredCandidate2 = scoredCandidates2[j];
//                if (scoredCandidate.getCandidate().equals(scoredCandidate2.getCandidate())){
//                    if (found){
//                        throw new RuntimeException("candidate is contained at least twice.");
//                    }
//                    found = true;
//
//                    //compare
//                    if (Math.abs(scoredCandidate.getScore()-scoredCandidate2.getScore())>1e-12){
//                        throw new RuntimeException("candidate scores differ.\n"+scoredCandidate.getCandidate()+
//                                "\n"+scoredCandidate.getCandidate().getFormula()+"\n"+scoredCandidate2.getCandidate()
//                        +"\nscore: "+scoredCandidate.getScore()+" vs "+scoredCandidate2.getScore());
//                    }
//
//                }
//
//            }
//            if (!found){
//                throw new RuntimeException("candidate not found");
//            }
//        }
//    }
//
//    private void compareCompoundInteractions(Graph<FragmentsCandidate> graph1, Graph<FragmentsCandidate> graph2, int compoundIndex) {
//        Scored<FragmentsCandidate>[] scoredCandidates = graph1.getPossibleFormulas(compoundIndex);
//        Scored<FragmentsCandidate>[] scoredCandidates2 = graph2.getPossibleFormulas(compoundIndex);
//
//
//
//        for (int i = 0; i < scoredCandidates.length; i++) {
//            Scored<FragmentsCandidate> scoredCandidate = scoredCandidates[i];
//            boolean found = false;
//            for (int j = 0; j < scoredCandidates2.length; j++) {
//                Scored<FragmentsCandidate> scoredCandidate2 = scoredCandidates2[j];
//                if (scoredCandidate.getCandidate().equals(scoredCandidate2.getCandidate())){
//                    if (found){
//                        throw new RuntimeException("candidate is contained at least twice.");
//                    }
//                    found = true;
//
//                    //compare
//                    int candIdx = graph1.getAbsoluteFormulaIdx(compoundIndex, i);
//                    int candIdx2 = graph2.getAbsoluteFormulaIdx(compoundIndex, j);
//
//                    int[] connections = graph1.getConnections(candIdx).clone();
//                    int[] connections2 = graph2.getConnections(candIdx2).clone();
//
//                    Arrays.sort(connections);
//                    Arrays.sort(connections2);
//
//                    if (connections.length!=connections2.length){
//                        throw new RuntimeException("different number of connections.");
//                    }
//
//                    for (int k = 0; k < connections.length; k++) {
//                        int c = connections[k];
//                        int c2 = connections2[k];
//
//                        if (!graph1.getPossibleFormulas1D(c).getCandidate().equals(graph2.getPossibleFormulas1D(c2).getCandidate())){
//                            throw new RuntimeException("connected candidates differ");
//                        }
//                        if (graph1.getLogWeight(c, candIdx)!=graph2.getLogWeight(c2, candIdx2)){
//                            throw new RuntimeException("edge scores differ");
//                        }
//
//                    }
//
//                }
//
//            }
//            if (!found){
//                throw new RuntimeException("candidate not found");
//            }
//        }
//
//
//    }
//
//    private Scored<FragmentsCandidate>[][] transformToLogScores(Scored<FragmentsCandidate>[][] scoredCandidates){
//        Scored<FragmentsCandidate>[][] logC = new Scored[scoredCandidates.length][];
//        for (int i = 0; i < scoredCandidates.length; i++) {
//            Scored<FragmentsCandidate>[] scoreds = scoredCandidates[i];
//            Scored<FragmentsCandidate>[] s2 = new Scored[scoreds.length];
//            for (int j = 0; j < scoreds.length; j++) {
//                s2[j] = new Scored<>(scoreds[j].getCandidate(), Math.log(scoreds[j].getScore()));
//            }
//            logC[i] = s2;
//        }
//        return logC;
//    }
//
//
//    private Scored<FragmentsCandidate>[][] combineResults(Scored<FragmentsCandidate>[][] results1, String[] resultIds1, Scored<FragmentsCandidate>[][] results2, String[] resultIds2) {
//        TObjectIntMap<String> idMap = new TObjectIntHashMap<>();
//        for (int i = 0; i < resultIds1.length; i++) {
//            idMap.put(resultIds1[i], i);
//        }
//
//        Scored<FragmentsCandidate>[][] combinedResults = new Scored[results2.length][];
//        for (int i = 0; i < resultIds2.length; i++) {
//            String id = resultIds2[i];
//            if (idMap.containsKey(id)){
//                combinedResults[i] = results1[idMap.get(id)];
//            } else {
//                combinedResults[i] = results2[i];
//            }
//        }
//        return combinedResults;
//    }
//
//    /**
//     * results must be sorted!
//     * @param results
//     * @param resultIdxs
//     * @return
//     */
//    private FragmentsCandidate[][] combineNewAndOld(Scored<FragmentsCandidate>[][] results, TIntArrayList resultIdxs) {
//        if (results.length == 0){
//            return possibleFormulas;
//        }
//
//        TIntIntMap idxMap = new TIntIntHashMap(results.length, 0.75f, -1, -1);
//        for (int i = 0; i < resultIdxs.size(); i++) {
//            idxMap.put(resultIdxs.get(i), i);
//        }
//
//        FragmentsCandidate[][] newPossibleFormulas = (FragmentsCandidate[][])Array.newInstance(cClass, possibleFormulas.length, 1);
//        for (int i = 0; i < possibleFormulas.length; i++) {
//            if (idxMap.containsKey(i)){
//                //use already computed. Take all with probability with combined probability >= 99%
//                Scored<FragmentsCandidate>[] scoreds = results[idxMap.get(i)];
//                List<FragmentsCandidate> candidates = new ArrayList<>();
//                double combinedProbs = 0d;
//                for (Scored<FragmentsCandidate> scored : scoreds) {
//                    candidates.add(scored.getCandidate());
//                    combinedProbs += scored.getScore();
//                    if (combinedProbs>=0.99) break;
//                }
//                newPossibleFormulas[i] = candidates.toArray((FragmentsCandidate[])Array.newInstance(cClass,0));
//            } else {
//                newPossibleFormulas[i] = possibleFormulas[i];
//            }
//        }
//
//        return newPossibleFormulas;
//    }
//
//    /**
//     * results must be sorted!
//     */
//    private FragmentsCandidate[][] combineNewAndOldAndSetFixedProbabilities(Scored<FragmentsCandidate>[][] results, TIntArrayList resultIdxs) {
//        if (results.length == 0){
//            return possibleFormulas;
//        }
//
//        TIntIntMap idxMap = new TIntIntHashMap(results.length, 0.75f, -1, -1);
//        for (int i = 0; i < resultIdxs.size(); i++) {
//            idxMap.put(resultIdxs.get(i), i);
//        }
//
//        FragmentsCandidate[][] newPossibleFormulas = (FragmentsCandidate[][])Array.newInstance(cClass, possibleFormulas.length, 1);
//
//
//
//        for (int i = 0; i < possibleFormulas.length; i++) {
//            try {
//                if (idxMap.containsKey(i)) {
//                    Scored<FragmentsCandidate>[] scoreds = results[idxMap.get(i)];
//                    List<FragmentsCandidate> candidates = new ArrayList<>();
//                    for (Scored<FragmentsCandidate> scored : scoreds) {
//                        FragmentsCandidate candidate = scored.getCandidate();
//                        candidate.clearNodeScores();
//                        candidate.addNodeProbabilityScore(scored.getScore());
//                        candidates.add(candidate);
//                    }
//                    newPossibleFormulas[i] = candidates.toArray((FragmentsCandidate[]) Array.newInstance(cClass, 0));
//                } else {
//                    newPossibleFormulas[i] = possibleFormulas[i];
//                }
//
//            } catch (Exception e) {
//                System.out.println("Error: "+e.getMessage());
//                System.out.println(idxMap.containsKey(i));
//                Scored<FragmentsCandidate>[] scoreds = results[idxMap.get(i)];
//                System.out.println(Arrays.toString(scoreds));
//                for (int j = 0; j < scoreds.length; j++) {
//                    Scored<FragmentsCandidate> scored = scoreds[j];
//                    System.out.println(j);
//                    System.out.println(scored);
//                    System.out.println(scored.getCandidate());
//                    System.out.println("isScored "+(scored instanceof  Scored));
//                    System.out.println("isFragmentCandidate "+(scored.getCandidate() instanceof  FragmentsCandidate));
//                }
//            }
//
//        }
//
//        return newPossibleFormulas;
//    }
//
//
//    public Scored<FragmentsCandidate>[][] getChosenFormulas(){
//        return combinedResult;
//    }
//
//    public Graph<FragmentsCandidate> getGraph() {
//        return graph;
//    }
//
//    public String[] getIds() {
//        return usedIds;
//    }
}
