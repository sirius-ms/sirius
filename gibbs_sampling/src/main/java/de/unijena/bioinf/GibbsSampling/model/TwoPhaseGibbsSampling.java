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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
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
import java.util.concurrent.ExecutionException;

/**
 * compute Gibbs Sampling first with good quality spectra. Then, insert other ones and compute again.
 * @param <C>
 */
public class TwoPhaseGibbsSampling<C extends Candidate<?>> extends BasicMasterJJob<ZodiacResult<C>> {
    private String[] ids;
    private C[][] possibleFormulas;
    private NodeScorer<C>[] nodeScorers;
    private EdgeScorer<C>[] edgeScorers;
    private EdgeFilter edgeFilter;
    private int repetitions;
    private Class<C> cClass;

    private CompoundResult<C>[] results1;
    private CompoundResult<C>[] results2;
    private CompoundResult<C>[] combinedResult;
    private String[] usedIds;

    private Graph<C> graph;
    private GibbsParallel<C> gibbsParallel;
    private String[] firstRoundIds;
    private TIntArrayList firstRoundCompoundsIdx;


    public TwoPhaseGibbsSampling(String[] ids, C[][] possibleFormulas, NodeScorer[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, int repetitions, Class<C> cClass){
        super(JobType.CPU);
        this.ids = ids;
        this.possibleFormulas = possibleFormulas;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.repetitions = repetitions;
        this.cClass = cClass;
    }

    private void init() throws ExecutionException {
        firstRoundCompoundsIdx = selectCompoundsForFirstRoundGibbsSampling();
        for (int i = 0; i < possibleFormulas.length; i++) {
            C[] poss = possibleFormulas[i];
////            if (poss.length>0 && CompoundQuality.isNotBadQuality(poss[0].getExperiment())){
//            //todo compound quality handling has changed a lot
//            if (poss.length>0 && poss[0].getExperiment().getAnnotation(CompoundQuality.class, CompoundQuality::new).isNotBadQuality()){
//                firstRoundCompoundsIdx.add(i);
//            }
            if (cClass==null && poss.length>0){
                cClass = (Class<C>)poss[0].getClass();
                break;
            }
        }


        C[][] firstRoundPossibleFormulas;
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
        logInfo("Start first round with " + firstRoundCompoundsIdx.size() + " of " + possibleFormulas.length + " compounds.");




        logInfo("ZODIAC: Graph building");
        long start = System.currentTimeMillis();
        GraphBuilder<C> graphBuilder = GraphBuilder.createGraphBuilder(firstRoundIds, firstRoundPossibleFormulas, nodeScorers, edgeScorers, edgeFilter, cClass);
        graph = submitSubJob(graphBuilder).awaitResult();
        logInfo("finished building graph after: "+(System.currentTimeMillis()-start)+" ms");
    }

    /*
    select compounds for first round of sampling based on quality.
     */
    private TIntArrayList selectCompoundsForFirstRoundGibbsSampling(){
        TIntArrayList firstRoundCompoundsIdx = new TIntArrayList();
        int totalNumber = possibleFormulas.length;
        long numberOfGoodQualityCompounds = Arrays.stream(possibleFormulas).filter(c->c.length>0 && c[0].getExperiment().getAnnotation(CompoundQuality.class, CompoundQuality::new).isNotBadQuality()).count();

        double goodRatio = 1d*numberOfGoodQualityCompounds/totalNumber;
        boolean onlyUseGoodAndUnknownQuality;
        if (numberOfGoodQualityCompounds<300 || goodRatio<0.33) {
            //if we have few good quality compounds, also use some others with only bad MS1 but good MS2
            onlyUseGoodAndUnknownQuality = false;
        } else {
            onlyUseGoodAndUnknownQuality = true;
        }

        for (int i = 0; i < possibleFormulas.length; i++) {
            C[] poss = possibleFormulas[i];
            if (onlyUseGoodAndUnknownQuality) {
                //check if MS1 and MS2-quality is ok
                if (poss.length>0 && poss[0].getExperiment().getAnnotation(CompoundQuality.class, CompoundQuality::new).isNotBadQuality()){
                    firstRoundCompoundsIdx.add(i);
                }
            } else {
                //just check if MS2-quality is ok
                if (poss.length>0){
                    CompoundQuality quality = poss[0].getExperiment().getAnnotation(CompoundQuality.class, CompoundQuality::new);
                    if (quality.isNot(CompoundQuality.CompoundQualityFlag.FewPeaks) &&
                            quality.isNot(CompoundQuality.CompoundQualityFlag.Chimeric) &&
                            quality.isNot(CompoundQuality.CompoundQualityFlag.PoorlyExplained)) {
                        firstRoundCompoundsIdx.add(i);
                    }
                }
            }

            if (cClass==null && poss.length>0) cClass = (Class<C>)poss[0].getClass();
        }
        return firstRoundCompoundsIdx;
    }

    private int maxSteps = -1;
    private int burnIn = -1;

    public void setIterationSteps(int maxSteps, int burnIn) {
        this.maxSteps = maxSteps;
        this.burnIn = burnIn;
    }

    @Override
    protected ZodiacResult<C> compute() throws Exception {
        if (maxSteps<0 || burnIn<0) throw new IllegalArgumentException("number of iterations steps not set.");
        checkForInterruption();
        init();
        checkForInterruption();
        logInfo("Running ZODIAC with "+firstRoundIds.length+" of "+ids.length+" compounds.");
        Graph.validateAndThrowError(graph, this::logWarn);
        gibbsParallel = new GibbsParallel<>(graph, repetitions);
        gibbsParallel.setIterationSteps(maxSteps, burnIn);
        long start = System.currentTimeMillis();
        submitSubJob(gibbsParallel);
        checkForInterruption();
        results1 = gibbsParallel.awaitResult();
        logDebug("finished running " + repetitions + " repetitions in parallel: "+(System.currentTimeMillis()-start)+" ms");

        checkForInterruption();

        firstRoundIds = gibbsParallel.getGraph().getIds();

//        addConnectivityInfo(results1, graph, false);

        if (firstRoundIds.length==possibleFormulas.length){
            combinedResult = results1;
            usedIds = firstRoundIds;
        } else {
//            //todo that's no good idea. Candidates should rather keep their probabilities
//            C[][] combined = combineNewAndOld(results1, firstRoundIds);
//
//            System.out.println("running second round with "+combined.length+" compounds.");
//            gibbsParallel = new GibbsParallel<>(ids, combined, nodeScorers, edgeScorers, edgeFilter, workersCount, repetitions);

            //changed same as in 3phase
            logInfo("Running second round: Score "+(ids.length-results1.length)+" low quality compounds. "+ids.length+" compounds overall.");
            //todo rather sample everything and just use results of low quality compounds? may there arise problems? in principle should not as we still sample all compounds (even 'fixed')
            C[][] candidatesNewRound = combineNewAndOldAndSetFixedProbabilities(results1, firstRoundCompoundsIdx);
            //todo this stupid thing creates a complete new graph.

            TIntHashSet fixedIds = new TIntHashSet(firstRoundCompoundsIdx);
            GraphBuilder<C> graphBuilder = GraphBuilder.createGraphBuilder(ids, candidatesNewRound, nodeScorers, edgeScorers, edgeFilter, fixedIds, cClass);
            graph = submitSubJob(graphBuilder).awaitResult();
            checkForInterruption();
            Graph.validateAndThrowError(graph, this::logWarn);

            gibbsParallel = new GibbsParallel<>(graph, repetitions, fixedIds);
            gibbsParallel.setIterationSteps(maxSteps, burnIn);
            submitSubJob(gibbsParallel);

            results2 = gibbsParallel.awaitResult();
            checkForInterruption();

//            addConnectivityInfo(results2, graph, true);

            usedIds = gibbsParallel.getGraph().ids;

            //still necessary e.g. because of connectivity infos from first round
            combinedResult = combineResults(results1, firstRoundIds, results2, usedIds);

        }
        return new ZodiacResult(ids, graph, combinedResult);
    }

    private void addConnectivityInfo(CompoundResult<C>[] results, Graph<C> graph, boolean onlyAddNew){
        for (int i = 0; i < results.length; i++) {
            CompoundResult<C> result = results[i];
            if (onlyAddNew && result.hasAnnotation(Connectivity.class)) continue;
            result.addAnnotation(Connectivity.class, new Connectivity(graph.getMaxNumberOfConnectedCompounds(i)));
        }
    }

    private CompoundResult<C>[] combineResults(CompoundResult<C>[] results1, String[] resultIds1, CompoundResult<C>[] results2, String[] resultIds2) {
        TObjectIntMap<String> idMap = new TObjectIntHashMap<>();
        for (int i = 0; i < resultIds1.length; i++) {
            idMap.put(resultIds1[i], i);
        }

        CompoundResult<C>[] combinedResults = new CompoundResult[results2.length];
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
     * @param resultIds
     * @return
     */
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
                //use already computed. Take best candidates until they add up to over 99%
                Scored<C>[] scoreds = results[idMap.get(ids[i])];
                List<C> candidates = new ArrayList<>();
                double combinedProbs = 0d;
                for (Scored<C> scored : scoreds) {
                    candidates.add(scored.getCandidate());
                    combinedProbs += scored.getScore();
                    if (combinedProbs>=0.99) break;
                }
                newPossibleFormulas[i] = candidates.toArray((C[])Array.newInstance(cClass,0));
            } else {
                newPossibleFormulas[i] = possibleFormulas[i];
            }
        }

        return newPossibleFormulas;
    }

    /**
     * results must be sorted!
     */
    private C[][] combineNewAndOldAndSetFixedProbabilities(CompoundResult<C>[] results, TIntArrayList resultIdxs) {
        if (results.length == 0){
            return possibleFormulas;
        }

        TIntIntMap idxMap = new TIntIntHashMap(results.length, 0.75f, -1, -1);
        for (int i = 0; i < resultIdxs.size(); i++) {
            idxMap.put(resultIdxs.get(i), i);
        }

        C[][] newPossibleFormulas = (C[][])Array.newInstance(cClass, possibleFormulas.length, 1);



        for (int i = 0; i < possibleFormulas.length; i++) {
            try {
                if (idxMap.containsKey(i)) {
                    Scored<C>[] scoreds = results[idxMap.get(i)].getCandidates();
                    List<C> candidates = new ArrayList<>();
                    for (Scored<C> scored : scoreds) {
                        C candidate = scored.getCandidate();
                        candidate.clearNodeScores();
                        candidate.addNodeProbabilityScore(scored.getScore());
                        candidates.add(candidate);
                    }
                    newPossibleFormulas[i] = candidates.toArray((C[]) Array.newInstance(cClass, 0));
                } else {
                    newPossibleFormulas[i] = possibleFormulas[i];
                }

            } catch (Exception e) {
                System.out.println("Error: "+e.getMessage());
                System.out.println(idxMap.containsKey(i));
                Scored<C>[] scoreds = results[idxMap.get(i)].getCandidates();
                System.out.println(Arrays.toString(scoreds));
                for (int j = 0; j < scoreds.length; j++) {
                    Scored<C> scored = scoreds[j];
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

    public Scored<C>[][] getChosenFormulas(){
        Scored<C>[][] scoredCandidates = new Scored[combinedResult.length][];
        for (int i = 0; i < combinedResult.length; i++) {
            scoredCandidates[i] = combinedResult[i].getCandidates();
        }
        return scoredCandidates;
    }

    public Graph<C> getGraph() {
        return graph;
    }

    public String[] getIds() {
        return usedIds;
    }

}
