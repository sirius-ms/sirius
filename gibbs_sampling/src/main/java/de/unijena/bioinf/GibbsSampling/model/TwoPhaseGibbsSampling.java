package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.lang.reflect.Array;
import java.util.*;

/**
 * compute Gibbs Sampling first with good quality spectra. Then, insert other ones and compute again.
 * @param <C>
 */
public class TwoPhaseGibbsSampling<C extends Candidate<?>> {
    private String[] ids;
    private C[][] possibleFormulas;
    private NodeScorer<C>[] nodeScorers;
    private EdgeScorer<C>[] edgeScorers;
    private EdgeFilter edgeFilter;
    private int workersCount;
    private int repetitions;
    private Class<C> cClass;

    private Scored<C>[][] results1;
    private Scored<C>[][] results2;
    private Scored<C>[][] combinedResult;
    private String[] usedIds;

    private Graph<C> graph;
    private GibbsParallel<C> gibbsParallel;
    private String[] firstRoundIds;

    public TwoPhaseGibbsSampling(String[] ids, C[][] possibleFormulas, NodeScorer[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, int workersCount, int repetitions) {
        this.ids = ids;
        this.possibleFormulas = possibleFormulas;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.workersCount = workersCount;
        this.repetitions = repetitions;
        init();
    }

    private void init(){
        TIntArrayList firstRoundCompoundsIdx = new TIntArrayList();
        for (int i = 0; i < possibleFormulas.length; i++) {
            C[] poss = possibleFormulas[i];
            if (poss.length>0 && CompoundQuality.isNotBadQuality(poss[0].getExperiment())){
                firstRoundCompoundsIdx.add(i);
            }
            if (cClass==null && poss.length>0) cClass = (Class<C>)poss[0].getClass();
        }


        C[][] firstRoundPossibleFormulas;
        String[] firstRoundIds;
        if (firstRoundCompoundsIdx.size()==possibleFormulas.length){
            firstRoundPossibleFormulas = possibleFormulas;
            firstRoundIds = ids;
        } else {
            firstRoundPossibleFormulas = (C[][])Array.newInstance(cClass, firstRoundCompoundsIdx.size(), 1);
            firstRoundIds = new String[firstRoundCompoundsIdx.size()];
            for (int i = 0; i < firstRoundCompoundsIdx.size(); i++) {
                firstRoundPossibleFormulas[i] = possibleFormulas[firstRoundCompoundsIdx.get(i)];
                firstRoundIds[i] = ids[firstRoundCompoundsIdx.get(i)];
            }
        }



        System.out.println("running first round with "+firstRoundIds.length+" compounds.");
        gibbsParallel = new GibbsParallel<>(firstRoundIds, firstRoundPossibleFormulas, nodeScorers, edgeScorers, edgeFilter, workersCount, repetitions);
        graph = gibbsParallel.getGraph();
    }

    public void run(int maxSteps, final int burnIn){
        gibbsParallel.iteration(maxSteps, burnIn);

        results1 = gibbsParallel.getChosenFormulasBySampling();

        firstRoundIds = gibbsParallel.getGraph().getIds();


        if (firstRoundIds.length==possibleFormulas.length){
            combinedResult = gibbsParallel.getChosenFormulasBySampling();
        } else {
            C[][] combined = combineNewAndOld(results1, firstRoundIds);

            System.out.println("running second round with "+combined.length+" compounds.");
            gibbsParallel = new GibbsParallel<>(ids, combined, nodeScorers, edgeScorers, edgeFilter, workersCount, repetitions);

            gibbsParallel.iteration(maxSteps, burnIn);

            results2 = gibbsParallel.getChosenFormulasBySampling();

            usedIds = gibbsParallel.getGraph().ids;

            combinedResult = combineResults(results1, firstRoundIds, results2, usedIds);

            graph = gibbsParallel.getGraph();
        }

    }

    private Scored<C>[][] combineResults(Scored<C>[][] results1, String[] resultIds1, Scored<C>[][] results2, String[] resultIds2) {
        TObjectIntMap<String> idMap = new TObjectIntHashMap<>();
        for (int i = 0; i < resultIds1.length; i++) {
            idMap.put(resultIds1[i], i);
        }

        Scored<C>[][] combinedResults = new Scored[results2.length][];
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

    private C[][] combineNewAndOld(Scored<C>[][] results, String[] resultIds) {
        if (results.length == 0){
            return possibleFormulas;
        }

        TObjectIntMap<String> idMap = new TObjectIntHashMap<>();
        for (int i = 0; i < resultIds.length; i++) {
            idMap.put(resultIds[i], i);
        }

        C[][] newPossibleFormulas = (C[][])Array.newInstance(cClass, possibleFormulas.length, 1);
        for (int i = 0; i < possibleFormulas.length; i++) {
            if (idMap.containsKey(ids[i])){
                //use already computed. Take all with probability > 1%
                Scored<C>[] scoreds = results[idMap.get(ids[i])];
                List<C> candidates = new ArrayList<>();
                for (Scored<C> scored : scoreds) {
                    if (scored.getScore()>0.01) candidates.add(scored.getCandidate());
                }
                newPossibleFormulas[i] = candidates.toArray((C[])Array.newInstance(cClass,0));
            } else {
                newPossibleFormulas[i] = possibleFormulas[i];
            }
        }

        return newPossibleFormulas;
    }

    public Scored<C>[][] getChosenFormulas(){
        return combinedResult;
    }

    public Graph<C> getGraph() {
        return graph;
    }

    public String[] getIds() {
        return usedIds;
    }
}
