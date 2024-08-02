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
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionFix;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.procedure.TDoubleProcedure;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public class GraphBuilder<C extends Candidate<?>> extends BasicMasterJJob<Graph<C>> implements JobProgressEventListener{
    protected static final boolean THIN_OUT_GRAPH = false;//never improved somthing. is slow.

    Graph<C> graph;
    EdgeScorer<C>[] edgeScorers;
    EdgeFilter edgeFilter;
    Class<C> cClass;

    private int numberOfFinishedComputations = 0;
    private double step;
    private int size;

    public GraphBuilder(Graph<C> graph, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, Class<C> cClass) {
        super(JobType.CPU);
        this.graph = graph;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.cClass = cClass;
    }

    public GraphBuilder(String[] ids, Scored<C>[][] possibleFormulas,  EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, Class<C> cClass) {
        super(JobType.CPU);
        logDebug("initialize graph builder");
        this.graph = Graph.getGraph(ids, possibleFormulas);
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.cClass = cClass;
    }

    public static <C extends Candidate<?>> GraphBuilder<C> createGraphBuilder(String[] ids, C[][] possibleFormulas, NodeScorer<C>[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, Class<C> cClass){
        return createGraphBuilder(ids, possibleFormulas, nodeScorers, edgeScorers, edgeFilter, null, cClass);
    }

    public static <C extends Candidate<?>> GraphBuilder<C> createGraphBuilder(String[] ids, C[][] possibleFormulas, NodeScorer<C>[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, TIntHashSet fixedCompounds, Class<C> cClass){
        final long t1 = System.currentTimeMillis();
        Graph<C> graph = createGraph(ids, possibleFormulas, nodeScorers, edgeScorers, edgeFilter, fixedCompounds);
        final long t2 = System.currentTimeMillis();
        LoggerFactory.getLogger(GraphBuilder.class).debug("Building (part of) the graph took " + ((t2-t1)/1000d) + " seconds.");
        return new GraphBuilder<C>(graph, edgeScorers, edgeFilter, cClass);
    }

    public static <C extends Candidate<?>> Graph<C> createGraph(String[] ids, C[][] possibleFormulas, NodeScorer<C>[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, TIntHashSet fixedCompounds){
        for (NodeScorer<C> nodeScorer : nodeScorers) {
            for (int i = 0; i < possibleFormulas.length; i++) {
                if (isFixed(fixedCompounds, i)) continue;
                C[] candidates = possibleFormulas[i];
                nodeScorer.score(candidates);
            }
        }

        List<String> newIds = new ArrayList();
        List<Scored<C>[]> newFormulas = new ArrayList();

        for(int i = 0; i < possibleFormulas.length; ++i) {
            C[] candidates = possibleFormulas[i];
            String id = ids[i];
            ArrayList<Scored<Candidate>> scoredCandidates = new ArrayList();

            for (C candidate : candidates) {
                scoredCandidates.add(new Scored(candidate, candidate.getNodeLogProb()));
            }

            if(scoredCandidates.size() > 0) {
                newIds.add(id);
                newFormulas.add(scoredCandidates.toArray(new Scored[0]));
            }
        }

        String[] filteredIds = newIds.toArray(new String[0]);
        Scored<C>[][] scoredPossibleFormulas = newFormulas.toArray(new Scored[0][]);
        return Graph.getGraph(filteredIds, scoredPossibleFormulas);
    }

    private static boolean isFixed(TIntHashSet fixedCompounds, int i) {
        if (fixedCompounds==null) return false;
        if (fixedCompounds.contains(i)) return true;
        return false;
    }

    @Override
    protected Graph<C> compute() throws Exception {
        numberOfFinishedComputations = 0;
        if (graph.possibleFormulas.length==0){
            graph.connections = new int[0][0];
        } else {
            this.calculateWeight();
            this.setConnections();

            if (THIN_OUT_GRAPH){
                long time = System.currentTimeMillis();
                graph.thinOutGraph();
                if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("thinning out graph in "+(System.currentTimeMillis()-time)+" ms");
            }
        }
        return graph;
    }


    protected void calculateWeight() throws ExecutionException {
        C[][] allCandidates = (C[][]) Array.newInstance(cClass, graph.getPossibleFormulas().length, 1);

        for(int minValue = 0; minValue < allCandidates.length; ++minValue) {
            Scored<C>[] scored = graph.getPossibleFormulas(minValue);
            allCandidates[minValue] = (C[])Array.newInstance(cClass, scored.length);

            for(int final_this = 0; final_this < scored.length; ++final_this) {
                allCandidates[minValue][final_this] = scored[final_this].getCandidate();
            }
        }

        logInfo("Prepare Scorers");

        double minV = 0.0D;
        //todo this is a big hack!!!!
        for (EdgeScorer<C> edgeScorer : edgeScorers) {
            if (edgeScorer instanceof ScoreProbabilityDistributionFix){
                if (edgeFilter instanceof EdgeThresholdFilter){
                    ((ScoreProbabilityDistributionFix)edgeScorer).setThresholdAndPrepare(allCandidates);
                } else {
                    submitSubJob(((ScoreProbabilityDistributionFix)edgeScorer).getPrepareJob(allCandidates)).awaitResult();
                }

            } else if (edgeScorer instanceof ScoreProbabilityDistributionEstimator){
                if (edgeFilter instanceof EdgeThresholdFilter){
                    ((ScoreProbabilityDistributionEstimator)edgeScorer).setThresholdAndPrepare(allCandidates);
                } else {
                    submitSubJob(((ScoreProbabilityDistributionEstimator)edgeScorer).getPrepareJob(allCandidates)).awaitResult();
                }
            } else {
                edgeScorer.prepare(allCandidates);
            }

            minV += edgeScorer.getThreshold();
        }


        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("minV "+minV);

        this.edgeFilter.setThreshold(minV);
        size = graph.getSize();
        step = Math.max(size/20, 1);
        updateProgress(0, size,0, "Computing edges");


        //now using workers to compute edges, for the reason that not so many subjobs have to be created.
        //But does not seem to improve performance compared to previous version.
        logDebug("computing edges");
        long start = System.currentTimeMillis();

        List<Integer> allIndices = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            allIndices.add(i);
        }
        ConcurrentLinkedQueue<Integer> candidatesQueue = new ConcurrentLinkedQueue<>(allIndices);
        logInfo("Number of candidates to compute: " + allIndices.size());
        List<BasicJJob> jobs = new ArrayList<>();
        final int cpuThreads = super.jobManager.getCPUThreads();
        for (int i = 0; i < cpuThreads; i++) {
            EdgeCalculationWorker job = new EdgeCalculationWorker(candidatesQueue, graph);
            job.totalEdges = allIndices.size()/cpuThreads;
            jobs.add(job);
            submitSubJob(job);
        }
        logInfo("running "+jobs.size()+" workers to compute edges");

        for (BasicJJob job : jobs) {
            job.awaitResult();
        }
        logInfo("finished computing edges after "+(System.currentTimeMillis()-start));

    }


    protected void setConnections() throws ExecutionException {
        long time = System.currentTimeMillis();
        graph.connections = this.edgeFilter.postprocessCompleteGraph(graph, this);
        HighQualityRandom random = new HighQualityRandom();

        if (GibbsMFCorrectionNetwork.DEBUG){
            logInfo("setting connections in: "+(System.currentTimeMillis()-time)+" ms");
            TDoubleArrayList someScores = new TDoubleArrayList();

            for(int i = 0; i < 1000; ++i) {
                int a = random.nextInt(graph.numberOfCompounds());
                if(graph.connections[a].length != 0) {
                    int b = random.nextInt(graph.connections[a].length);
                    someScores.add(graph.getLogWeight(a, graph.connections[a][b]));
                }
            }

//        System.out.println("some scores: " + Arrays.toString(someScores.toArray()));
        }


        assert graph.isSymmetricSparse();

        if(false && !graph.arePeaksConnected()) {
            //todo
//            System.out.println("warning: graph is not well connected. consider using less stringent EdgeFilters");
        }

        long sum = 0;

        for(int i = 0; i < graph.connections.length; ++i) {
            int[] connection = graph.connections[i];
            sum += connection.length;
        }

        logInfo("Number of connections " + sum / 2);

        if (GibbsMFCorrectionNetwork.DEBUG) {
            final TDoubleArrayList samples = new TDoubleArrayList();
            for (TDoubleArrayList weight : graph.weights) {
                weight.forEach(new TDoubleProcedure() {
                    @Override
                    public boolean execute(double v) {
                        if (v<0) throw new RuntimeException("graph weights are negative");
                        if (random.nextDouble()<0.001) samples.add(v);
                        return true;
                    }
                });
            }
            samples.sort();
            System.out.println("all good");
            System.out.println("mean: "+samples.sum()/samples.size());
            System.out.println("median: "+samples.get(samples.size()/2));
            System.out.println("min: "+samples.min());
            System.out.println("max: "+samples.max());

            final TDoubleList s2;
            if (samples.size()>1000){
                samples.shuffle(random);
                s2 = samples.subList(0, 1000);
            } else {
                s2 = samples;
            }
            System.out.println(Arrays.toString(s2.toArray()));
        }

    }


    @Override
    public void progressChanged(JobProgressEvent progressEvent) {
        if (progressEvent.getProgress() !=100) return;
        ++numberOfFinishedComputations;
        if(numberOfFinishedComputations % step == 0 || numberOfFinishedComputations==size) {
            logInfo(Math.round(100*((double)numberOfFinishedComputations)/size)+"%");
        }
    }

    private class EdgeCalculationWorker extends BasicJJob {
        private ConcurrentLinkedQueue<Integer> remainingCandidates;
        private Graph<C> graph;

        protected int computedEdges, totalEdges;

        private EdgeCalculationWorker(ConcurrentLinkedQueue<Integer> remainingCandidates, Graph<C> graph) {
            this.remainingCandidates = remainingCandidates;
            this.graph = graph;
        }
        @Override
        protected Object compute() throws Exception {
            final int edgesPerPercentagePoint = (int)Math.max(1, Math.floor(totalEdges/100d));
            while (!remainingCandidates.isEmpty()){
                Integer idx = remainingCandidates.poll();
                if (idx==null) continue;
                final C candidate = graph.getPossibleFormulas1D(idx).getCandidate();

                TDoubleArrayList scores = new TDoubleArrayList(graph.getSize());

                for(int j = 0; j < graph.getSize(); ++j) {
                    if(graph.getPeakIdx(idx) == graph.getPeakIdx(j)) {
                        scores.add(0.0D);
                    } else {
                        C candidate2 = graph.getPossibleFormulas1D(j).getCandidate();
                        double score = 0.0D;

                        for(int k = 0; k < edgeScorers.length; ++k) {
                            EdgeScorer edgeScorer = edgeScorers[k];
                            score += edgeScorer.score(candidate, candidate2);
                        }

                        scores.add(score);
                    }
                }

                edgeFilter.filterEdgesAndSetThreshold(graph, idx, scores.toArray());

                ++computedEdges;
                if (computedEdges%edgesPerPercentagePoint==0 && totalEdges>0) {
                    logInfo(String.format("%d / %d (%d %%)", computedEdges, totalEdges, (computedEdges*100)/totalEdges));
                }
                //progess is always fired if job done
//                checkForInterruption();

            }
            return null;
        }
    }
}
