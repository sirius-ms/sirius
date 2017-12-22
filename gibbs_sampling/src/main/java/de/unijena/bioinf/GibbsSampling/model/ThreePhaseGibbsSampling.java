package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * compute Gibbs Sampling first with good quality spectra. Then, insert other ones and compute again.
 */
public class ThreePhaseGibbsSampling {
    private String[] ids;
    private FragmentsCandidate[][] possibleFormulas;
    private NodeScorer<FragmentsCandidate>[] nodeScorers;
    private EdgeScorer<FragmentsCandidate>[] edgeScorers;
    private EdgeFilter edgeFilter;
    private int workersCount;
    private int repetitions;
    private Class<FragmentsCandidate> cClass;

    private Scored<FragmentsCandidate>[][] results1;
    private Scored<FragmentsCandidate>[][] results2;
    private Scored<FragmentsCandidate>[][] combinedResult;
    private String[] usedIds;

    private Graph<FragmentsCandidate> graph;
    private GibbsParallel<FragmentsCandidate> gibbsParallel;
    private String[] firstRoundIds;
    private TIntArrayList firstRoundCompoundsIdx;

    private int numberOfCandidatesFirstRound;

    public ThreePhaseGibbsSampling(String[] ids, FragmentsCandidate[][] possibleFormulas, int numberOfCandidatesFirstRound, NodeScorer[] nodeScorers, EdgeScorer<FragmentsCandidate>[] edgeScorers, EdgeFilter edgeFilter, int workersCount, int repetitions) {
        this.ids = ids;
        this.possibleFormulas = possibleFormulas;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.workersCount = workersCount;
        this.repetitions = repetitions;
        this.numberOfCandidatesFirstRound = numberOfCandidatesFirstRound;
        assertInput();
        init();
    }
    
    private void assertInput(){
        for (int i = 0; i < possibleFormulas.length; i++) {
            FragmentsCandidate[] candidates = possibleFormulas[i];
            for (int j = 0; j < candidates.length; j++) {
                if (DummyFragmentCandidate.isDummy(candidates[j])){
                    if (j<candidates.length-1){
                        throw new RuntimeException("dummy node must be at last position of candidate list");
                    }
                }
                
            }
        }
    }

    private void init(){
        firstRoundCompoundsIdx = new TIntArrayList();
        for (int i = 0; i < possibleFormulas.length; i++) {
            FragmentsCandidate[] poss = possibleFormulas[i];
            if (poss.length>0 && CompoundQuality.isNotBadQuality(poss[0].getExperiment())){
                firstRoundCompoundsIdx.add(i);
            }
            if (cClass==null && poss.length>0) cClass = (Class<FragmentsCandidate>)poss[0].getClass();
        }


        FragmentsCandidate[][] firstRoundPossibleFormulas;
        String[] firstRoundIds;

        firstRoundPossibleFormulas = (FragmentsCandidate[][])Array.newInstance(cClass, firstRoundCompoundsIdx.size(), 1);
        firstRoundIds = new String[firstRoundCompoundsIdx.size()];
        for (int i = 0; i < firstRoundCompoundsIdx.size(); i++) {
            FragmentsCandidate[] candidates = possibleFormulas[firstRoundCompoundsIdx.get(i)];
            DummyFragmentCandidate dummy = extractDummy(candidates);
            if (dummy==null) {
                if (candidates.length>numberOfCandidatesFirstRound){
                    candidates = Arrays.copyOfRange(candidates, 0, numberOfCandidatesFirstRound);    
                }
            } else {
                int maxCandidatesWithDummy = numberOfCandidatesFirstRound+1;
                DummyFragmentCandidate newDummy = updateDummy(dummy, candidates, maxCandidatesWithDummy);
                if (candidates.length>maxCandidatesWithDummy){
                    candidates = Arrays.copyOfRange(candidates, 0, maxCandidatesWithDummy);
                }
                //dummy missing, add it
                candidates[candidates.length-1] = newDummy;
            }

            firstRoundPossibleFormulas[i] = candidates;
            firstRoundIds[i] = ids[firstRoundCompoundsIdx.get(i)];
        }



        System.out.println("running first round with "+firstRoundIds.length+" compounds.");
        gibbsParallel = new GibbsParallel<>(firstRoundIds, firstRoundPossibleFormulas, nodeScorers, edgeScorers, edgeFilter, workersCount, repetitions);
        graph = gibbsParallel.getGraph();
    }

    private DummyFragmentCandidate extractDummy(FragmentsCandidate[] candidates){
        //todo always at last position?!?
//        for (C candidate : candidates) {
//            if (DummyFragmentCandidate.isDummy(candidate)) return (DummyFragmentCandidate)candidate;
//        }
        if (DummyFragmentCandidate.isDummy(candidates[candidates.length-1])) return (DummyFragmentCandidate) candidates[candidates.length-1];
        return null;
    }

    private DummyFragmentCandidate updateDummy(DummyFragmentCandidate dummy,FragmentsCandidate[] candidates, int maxCandidatesWithDummy){
        if (maxCandidatesWithDummy>=candidates.length) return dummy;
        int numberOfIgnored = dummy.getNumberOfIgnoredInstances();
        numberOfIgnored += (candidates.length-maxCandidatesWithDummy);

        double worstScore = candidates[maxCandidatesWithDummy-1].getScore();

        return DummyFragmentCandidate.newDummy(worstScore, numberOfIgnored, dummy.getExperiment());
        
    }

    public void run(int maxSteps, final int burnIn){

        //1. run only on best quality spectra with restricted number of candidates
        gibbsParallel.iteration(maxSteps, burnIn);

        results1 = gibbsParallel.getChosenFormulasBySampling();

        firstRoundIds = gibbsParallel.getGraph().getIds();


        //2. now score 'all' candidates for each compound (rest of network is fixed on best only)
        //todo how to hack this?

        //extract top hits;
        Scored<FragmentsCandidate>[][] intermediateResults = gibbsParallel.getChosenFormulasBySampling();
        Scored<FragmentsCandidate>[][] scoredFixedCandidates = new Scored[firstRoundIds.length][];
        for (int i = 0; i < intermediateResults.length; i++) {
            //todo change to use all candidates with their fixed computed probabilities. and not only best hit
            Scored<FragmentsCandidate> bestHit = intermediateResults[i][0];
            scoredFixedCandidates[i] = new Scored[]{new Scored(bestHit.getCandidate(), 0d)};
        }
        //now replace each step one compound with all candidates and score them (not super efficient, but should play no big role compared to rest)
        for (int i = 0; i < firstRoundCompoundsIdx.size(); i++) {
            int firstRoundIdx = firstRoundCompoundsIdx.get(i);
            FragmentsCandidate[] allCandidates = possibleFormulas[firstRoundIdx];
            Scored<FragmentsCandidate>[] currentScoredCandidates = graph.getPossibleFormulas(i);

            if (allCandidates.length==currentScoredCandidates.length) continue;

            Scored<FragmentsCandidate>[] allScoredCandidates = new Scored[allCandidates.length];

            //compute new node scores for all compound candidates
            for (FragmentsCandidate candidate : allCandidates) {
                candidate.clearNodeScores();
            }
            for (NodeScorer<FragmentsCandidate> nodeScorer : nodeScorers) {
                nodeScorer.score(allCandidates);
            }

            for (int j = 0; j < allCandidates.length; j++) {
                allScoredCandidates[j] = new Scored<>(allCandidates[j], allCandidates[j].getNodeLogProb());
            }



            Scored<FragmentsCandidate>[] best = scoredFixedCandidates[i];
            scoredFixedCandidates[i] = allScoredCandidates;

//          todo   ...don't  init all edges
            Graph<FragmentsCandidate> graph = new Graph<FragmentsCandidate>(firstRoundIds, scoredFixedCandidates);
            graph.init(edgeScorers, edgeFilter, workersCount);


            //todo is this working? (log odds vs log probs -> recalculate)
            Scored<FragmentsCandidate>[] scoredCandidates = GibbsMFCorrectionNetwork.computeFromSnapshot(graph, i);

            //update results with all candidates for compound i;
            results1[i] = scoredCandidates;

            scoredFixedCandidates[i] = best;

//            Scored<FragmentsCandidate>[] scoredCandidiates = GibbsMFCorrectionNetwork.computeFromSnapshot(graph, i);
//            GibbsMFCorrectionNetwork<FragmentsCandidate> gibbs = new GibbsMFCorrectionNetwork<FragmentsCandidate>(firstRoundIds, fixedCandidates, nodeScorers, edgeScorers, edgeFilter, workersCount);
//            gibbs.iteration(Math.min(1000, maxSteps));




        }



        //3. include bad quality spectra (could also do this in step 2) //todo use gibbs sampling??
        if (firstRoundIds.length==possibleFormulas.length){
            combinedResult = results1;
            return;
        }



        FragmentsCandidate[][] candidatesNewRound = combineNewAndOldAndSetFixedProbabilities(results1, firstRoundCompoundsIdx);
        //todo this stupid thing creates a complete new graph.
        gibbsParallel = new GibbsParallel<>(ids, candidatesNewRound, nodeScorers, edgeScorers, edgeFilter, new TIntHashSet(firstRoundCompoundsIdx), workersCount, repetitions);



        gibbsParallel.iteration(maxSteps, burnIn);

        results2 = gibbsParallel.getChosenFormulasBySampling();

        usedIds = gibbsParallel.getGraph().ids;

        combinedResult = combineResults(results1, firstRoundIds, results2, usedIds); //todo test. should not be necessary as all candidates are used anyways.

        graph = gibbsParallel.getGraph();


    }

    private Scored<FragmentsCandidate>[][] combineResults(Scored<FragmentsCandidate>[][] results1, String[] resultIds1, Scored<FragmentsCandidate>[][] results2, String[] resultIds2) {
        TObjectIntMap<String> idMap = new TObjectIntHashMap<>();
        for (int i = 0; i < resultIds1.length; i++) {
            idMap.put(resultIds1[i], i);
        }

        Scored<FragmentsCandidate>[][] combinedResults = new Scored[results2.length][];
        for (int i = 0; i < resultIds2.length; i++) {
            String id = resultIds2[i];
            if (idMap.containsKey(id)){
                combinedResults[i] = results1[idMap.get(id)];
            } else {
                combinedResults[i] = results2[i];
            }
        }
        return combinedResults;
    }

    /**
     * results must be sorted!
     * @param results
     * @param resultIdxs
     * @return
     */
    private FragmentsCandidate[][] combineNewAndOld(Scored<FragmentsCandidate>[][] results, TIntArrayList resultIdxs) {
        if (results.length == 0){
            return possibleFormulas;
        }

        TIntIntMap idxMap = new TIntIntHashMap(results.length, 0.75f, -1, -1);
        for (int i = 0; i < resultIdxs.size(); i++) {
            idxMap.put(resultIdxs.get(i), i);
        }

        FragmentsCandidate[][] newPossibleFormulas = (FragmentsCandidate[][])Array.newInstance(cClass, possibleFormulas.length, 1);
        for (int i = 0; i < possibleFormulas.length; i++) {
            if (idxMap.containsKey(i)){
                //use already computed. Take all with probability with combined probability >= 99%
                Scored<FragmentsCandidate>[] scoreds = results[idxMap.get(i)];
                List<FragmentsCandidate> candidates = new ArrayList<>();
                double combinedProbs = 0d;
                for (Scored<FragmentsCandidate> scored : scoreds) {
                    candidates.add(scored.getCandidate());
                    combinedProbs += scored.getScore();
                    if (combinedProbs>=0.99) break;
                }
                newPossibleFormulas[i] = candidates.toArray((FragmentsCandidate[])Array.newInstance(cClass,0));
            } else {
                newPossibleFormulas[i] = possibleFormulas[i];
            }
        }

        return newPossibleFormulas;
    }

    /**
     * results must be sorted!
     */
    private FragmentsCandidate[][] combineNewAndOldAndSetFixedProbabilities(Scored<FragmentsCandidate>[][] results, TIntArrayList resultIdxs) {
        if (results.length == 0){
            return possibleFormulas;
        }

        TIntIntMap idxMap = new TIntIntHashMap(results.length, 0.75f, -1, -1);
        for (int i = 0; i < resultIdxs.size(); i++) {
            idxMap.put(resultIdxs.get(i), i);
        }

        FragmentsCandidate[][] newPossibleFormulas = (FragmentsCandidate[][])Array.newInstance(cClass, possibleFormulas.length, 1);



        for (int i = 0; i < possibleFormulas.length; i++) {
            try {
                if (idxMap.containsKey(i)) {
                    Scored<FragmentsCandidate>[] scoreds = results[idxMap.get(i)];
                    List<FragmentsCandidate> candidates = new ArrayList<>();
                    for (Scored<FragmentsCandidate> scored : scoreds) {
                        FragmentsCandidate candidate = scored.getCandidate();
                        candidate.clearNodeScores();
                        candidate.addNodeProbabilityScore(scored.getScore());
                        candidates.add(candidate);
                    }
                    newPossibleFormulas[i] = candidates.toArray((FragmentsCandidate[]) Array.newInstance(cClass, 0));
                } else {
                    newPossibleFormulas[i] = possibleFormulas[i];
                }

            } catch (Exception e) {
                System.out.println("Error: "+e.getMessage());
                System.out.println(idxMap.containsKey(i));
                Scored<FragmentsCandidate>[] scoreds = results[idxMap.get(i)];
                System.out.println(Arrays.toString(scoreds));
                for (int j = 0; j < scoreds.length; j++) {
                    Scored<FragmentsCandidate> scored = scoreds[j];
                    System.out.println(j);
                    System.out.println(scored);
                    System.out.println(scored.getCandidate());
                    System.out.println("isScored "+(scored instanceof  Scored));
                    System.out.println("isFragmentCandidate "+(scored.getCandidate() instanceof  FragmentsCandidate));
                }
            }

        }

        return newPossibleFormulas;
    }


    public Scored<FragmentsCandidate>[][] getChosenFormulas(){
        return combinedResult;
    }

    public Graph<FragmentsCandidate> getGraph() {
        return graph;
    }

    public String[] getIds() {
        return usedIds;
    }
}
