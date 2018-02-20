package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.MasterJJob;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GibbsParallel<C extends Candidate<?>> extends BasicMasterJJob<CompoundResult<C>[]> {
    private int repetitions;
    private final List<GibbsMFCorrectionNetwork> gibbsNetworks;
    private Scored<C>[][] sampling;
    private Graph graph;

    public GibbsParallel(Graph<C> graph, int repetitions, TIntHashSet fixedCompounds) throws ExecutionException {
        super(JobType.CPU);
        this.repetitions = repetitions;
        this.graph = graph;
        this.gibbsNetworks = init(repetitions, fixedCompounds);
    }

    public GibbsParallel(Graph<C> graph, int repetitions) throws ExecutionException {
        this(graph, repetitions, null);
    }

    private List<GibbsMFCorrectionNetwork> init(int size, TIntHashSet fixedCompounds){
        List<GibbsMFCorrectionNetwork> networkList = new ArrayList<>();
        int i = 0;
        while(i++ < size) {
            networkList.add(new GibbsMFCorrectionNetwork(graph, fixedCompounds));
        }
        return networkList;
    }


    private void combineResults() {
        TObjectDoubleHashMap[] samplingCombined = this.createMap(graph.numberOfCompounds());
        for (GibbsMFCorrectionNetwork gibbsNetwork : gibbsNetworks) {
            Scored[][] sampling = gibbsNetwork.getChosenFormulasBySampling();
            add(samplingCombined, sampling, false, 1.0D / (double)this.gibbsNetworks.size());
        }

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

    private int maxSteps = -1;
    private int burnIn = -1;

    public void setIterationSteps(int maxSteps, int burnIn) {
        this.maxSteps = maxSteps;
        this.burnIn = burnIn;
    }

    @Override
    protected CompoundResult<C>[] compute() throws Exception {
        if (maxSteps<0 || burnIn<0) throw new IllegalArgumentException("number of iterations steps not set.");
        final int maxStepProportioned = maxSteps / this.repetitions;
        
        for (final GibbsMFCorrectionNetwork gibbsNetwork : gibbsNetworks) {
            gibbsNetwork.setIterationSteps(maxStepProportioned, burnIn);
            submitSubJob(gibbsNetwork);
        }

        awaitAllSubJobs();

        combineResults();

        return createCompoundResults();

    }


    protected CompoundResult<C>[] createCompoundResults(){
        final String[] ids = graph.getIds();
        final CompoundResult<C>[] results = new CompoundResult[ids.length];
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            Scored<C>[] candidates = sampling[i];
            final CompoundResult<C> compoundResult = new CompoundResult(id, candidates);
            compoundResult.addAnnotation(Connectivity.class, new Connectivity(graph.getMaxNumberOfConnectedCompounds(i)));
            results[i] = compoundResult;
        }
        return results;
    }

    @Deprecated
    public Scored<C>[][] getChosenFormulasBySampling() {
        return this.sampling;
    }

    public Scored<C>[][] getChosenFormulas() {
        return this.sampling;
    }

    public Graph getGraph() {
        return this.graph;
    }
}
