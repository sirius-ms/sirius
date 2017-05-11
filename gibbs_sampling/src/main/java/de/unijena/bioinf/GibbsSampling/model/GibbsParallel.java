package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GibbsParallel {
    private String[] ids;
    private MFCandidate[][] possibleFormulas;
    private NodeScorer[] nodeScorers;
    private EdgeScorer[] edgeScorers;
    private EdgeFilter edgeFilter;
    private int workersCount;
    private final List<GibbsMFCorrectionNetwork> gibbsNetworks;
    private Scored<MFCandidate>[][] maxPosterior;
    private Scored<MFCandidate>[][] addedUpPosterior;
    private Scored<MFCandidate>[][] sampling;
    private Graph graph;

    public GibbsParallel(String[] ids, MFCandidate[][] possibleFormulas, NodeScorer[] nodeScorers, EdgeScorer[] edgeScorers, EdgeFilter edgeFilter, int workersCount) {
        this.ids = ids;
        this.possibleFormulas = possibleFormulas;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.workersCount = workersCount;
        this.gibbsNetworks = new ArrayList();
        this.graph = this.init();
    }

    private Graph init() {
        Graph graph = GibbsMFCorrectionNetwork.buildGraph(this.ids, this.possibleFormulas, this.nodeScorers, this.edgeScorers, this.edgeFilter, this.workersCount);
        int i = 0;

        while(i++ < this.workersCount) {
            this.gibbsNetworks.add(new GibbsMFCorrectionNetwork(graph, 1));
        }

        return graph;
    }

    private void combineResults() {
        TObjectDoubleHashMap[] maxPosteriorCombined = this.createMap(this.possibleFormulas.length);
        TObjectDoubleHashMap[] addedUpPosteriorCombined = this.createMap(this.possibleFormulas.length);
        TObjectDoubleHashMap[] samplingCombined = this.createMap(this.possibleFormulas.length);
        Iterator var4 = this.gibbsNetworks.iterator();

        while(var4.hasNext()) {
            GibbsMFCorrectionNetwork gibbsNetwork = (GibbsMFCorrectionNetwork)var4.next();
            Scored[][] maxPosterior = gibbsNetwork.getChosenFormulasByMaxPosterior();
            this.add(maxPosteriorCombined, maxPosterior, true, 0.0D / 0.0);
            Scored[][] addedUpPosterior = gibbsNetwork.getChosenFormulasByAddedUpPosterior();
            this.add(addedUpPosteriorCombined, addedUpPosterior, false, 1.0D / (double)this.gibbsNetworks.size());
            Scored[][] sampling = gibbsNetwork.getChosenFormulasBySampling();
            this.add(samplingCombined, sampling, false, 1.0D / (double)this.gibbsNetworks.size());
        }

        this.maxPosterior = this.toArray(maxPosteriorCombined);
        this.addedUpPosterior = this.toArray(addedUpPosteriorCombined);
        this.sampling = this.toArray(samplingCombined);
    }

    private void add(TObjectDoubleHashMap<MFCandidate>[] combined, Scored<MFCandidate>[][] instance, boolean takeMax, double normalization) {
        for(int i = 0; i < instance.length; ++i) {
            Scored[] scoredCandidates = instance[i];
            TObjectDoubleHashMap scoresMap = combined[i];
            Scored[] var9 = scoredCandidates;
            int var10 = scoredCandidates.length;

            for(int var11 = 0; var11 < var10; ++var11) {
                Scored scoredCandidate = var9[var11];
                double score = scoredCandidate.getScore();
                MFCandidate candidate = (MFCandidate)scoredCandidate.getCandidate();
                if(takeMax) {
                    double currentScore = scoresMap.get(candidate);
                    if(score > currentScore) {
                        scoresMap.put(candidate, score);
                    }
                } else {
                    scoresMap.adjustOrPutValue(candidate, normalization * score, normalization * score);
                }
            }
        }

    }

    private Scored<MFCandidate>[][] toArray(TObjectDoubleHashMap<MFCandidate>[] maps) {
        Scored<MFCandidate>[][] array = new Scored[maps.length][];

        for(int i = 0; i < maps.length; ++i) {
            TObjectDoubleHashMap map = maps[i];
            Scored<MFCandidate>[] scoredCandidates = new Scored[map.size()];
            int j = 0;

            Scored scored;
            for(Iterator var7 = map.keySet().iterator(); var7.hasNext(); scoredCandidates[j++] = scored) {
                MFCandidate candidate = (MFCandidate)var7.next();
                scored = new Scored(candidate, map.get(candidate));
            }

            Arrays.sort(scoredCandidates, Scored.<MFCandidate>desc());
            array[i] = scoredCandidates;
        }

        return array;
    }

    private TObjectDoubleHashMap<MFCandidate>[] createMap(int length) {
        TObjectDoubleHashMap[] map = new TObjectDoubleHashMap[length];

        for(int i = 0; i < map.length; ++i) {
            map[i] = new TObjectDoubleHashMap();
        }

        return map;
    }

    public void iteration(int maxSteps, final int burnIn) {
        final int maxStepProportioned = maxSteps / this.workersCount;
        ExecutorService executorService = Executors.newFixedThreadPool(this.workersCount);
        ArrayList<Future> futures = new ArrayList();
        
        for (final GibbsMFCorrectionNetwork gibbsNetwork : gibbsNetworks) {
            futures.add(executorService.submit(new Runnable() {
                public void run() {
                    gibbsNetwork.iteration(maxStepProportioned, burnIn);
                }
            }));
        }


        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException var9) {
                var9.printStackTrace();
                throw new RuntimeException(var9);
            } catch (ExecutionException var10) {
                var10.printStackTrace();
                throw new RuntimeException(var10);
            }
        }
        

        executorService.shutdown();
        this.combineResults();

        for (GibbsMFCorrectionNetwork gibbsNetwork : gibbsNetworks) {
            gibbsNetwork.shutdown();
        }
    }

    public Scored<MFCandidate>[][] getChosenFormulasByMaxPosterior() {
        return this.maxPosterior;
    }

    public Scored<MFCandidate>[][] getChosenFormulasByAddedUpPosterior() {
        return this.addedUpPosterior;
    }

    public Scored<MFCandidate>[][] getChosenFormulasBySampling() {
        return this.sampling;
    }

    public Graph getGraph() {
        return this.graph;
    }
}
