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

public class GibbsParallel<C extends Candidate<?>> {
    private String[] ids;
    private C[][] possibleFormulas;
    private NodeScorer<C>[] nodeScorers;
    private EdgeScorer<C>[] edgeScorers;
    private EdgeFilter edgeFilter;
    private int workersCount;
    private int repetitions;
    private final List<GibbsMFCorrectionNetwork> gibbsNetworks;
    private Scored<C>[][] maxPosterior;
    private Scored<C>[][] addedUpPosterior;
    private Scored<C>[][] sampling;
    private Graph graph;

    public GibbsParallel(String[] ids, C[][] possibleFormulas, NodeScorer[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, int workersCount, int repetitions) {
        this.ids = ids;
        this.possibleFormulas = possibleFormulas;
        this.nodeScorers = nodeScorers;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.workersCount = workersCount;
        this.repetitions = repetitions;
        this.gibbsNetworks = new ArrayList();
        this.graph = this.init();
    }

    private Graph<C> init() {
        Graph<C> graph = GibbsMFCorrectionNetwork.buildGraph(this.ids, this.possibleFormulas, this.nodeScorers, this.edgeScorers, this.edgeFilter, this.workersCount);

        int i = 0;
        while(i++ < this.repetitions) {
            this.gibbsNetworks.add(new GibbsMFCorrectionNetwork(graph));
        }

        return graph;
    }

    private void combineResults() {
        TObjectDoubleHashMap[] maxPosteriorCombined = this.createMap(this.possibleFormulas.length);
        TObjectDoubleHashMap[] addedUpPosteriorCombined = this.createMap(this.possibleFormulas.length);
        TObjectDoubleHashMap[] samplingCombined = this.createMap(this.possibleFormulas.length);
        Iterator var4 = this.gibbsNetworks.iterator();
        for (GibbsMFCorrectionNetwork gibbsNetwork : gibbsNetworks) {
            Scored[][] maxPosterior = gibbsNetwork.getChosenFormulasByMaxPosterior();
            this.add(maxPosteriorCombined, maxPosterior, true, Double.NaN);
            Scored[][] addedUpPosterior = gibbsNetwork.getChosenFormulasByAddedUpPosterior();
            this.add(addedUpPosteriorCombined, addedUpPosterior, false, 1.0D / (double)this.gibbsNetworks.size());
            Scored[][] sampling = gibbsNetwork.getChosenFormulasBySampling();
            this.add(samplingCombined, sampling, false, 1.0D / (double)this.gibbsNetworks.size());
        }

        this.maxPosterior = this.toArray(maxPosteriorCombined);
        this.addedUpPosterior = this.toArray(addedUpPosteriorCombined);
        this.sampling = this.toArray(samplingCombined);
    }

    private void add(TObjectDoubleHashMap<Candidate>[] combined, Scored<Candidate>[][] instance, boolean takeMax, double normalization) {
        for(int i = 0; i < instance.length; ++i) {
            Scored[] scoredCandidates = instance[i];
            TObjectDoubleHashMap scoresMap = combined[i];

            for(int j = 0; j < scoredCandidates.length; ++j) {
                Scored scoredCandidate = scoredCandidates[j];
                double score = scoredCandidate.getScore();
                Candidate candidate = (Candidate)scoredCandidate.getCandidate();
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

    private Scored<C>[][] toArray(TObjectDoubleHashMap<C>[] maps) {
        Scored<C>[][] array = new Scored[maps.length][];

        for(int i = 0; i < maps.length; ++i) {
            TObjectDoubleHashMap<C> map = maps[i];
            Scored<C>[] scoredCandidates = new Scored[map.size()];
            int j = 0;

            for (C candidate : map.keySet()) {
                scoredCandidates[j++] =  new Scored(candidate, map.get(candidate));
            }



            Arrays.sort(scoredCandidates, Scored.<C>desc());
            array[i] = scoredCandidates;
        }

        return array;
    }

    private TObjectDoubleHashMap<Candidate>[] createMap(int length) {
        TObjectDoubleHashMap[] map = new TObjectDoubleHashMap[length];

        for(int i = 0; i < map.length; ++i) {
            map[i] = new TObjectDoubleHashMap();
        }

        return map;
    }

    public void iteration(int maxSteps, final int burnIn) {
        final int maxStepProportioned = maxSteps / this.repetitions;
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

    }

    public Scored<C>[][] getChosenFormulasByMaxPosterior() {
        return this.maxPosterior;
    }

    public Scored<C>[][] getChosenFormulasByAddedUpPosterior() {
        return this.addedUpPosterior;
    }

    public Scored<C>[][] getChosenFormulasBySampling() {
        return this.sampling;
    }

    public Graph getGraph() {
        return this.graph;
    }
}
