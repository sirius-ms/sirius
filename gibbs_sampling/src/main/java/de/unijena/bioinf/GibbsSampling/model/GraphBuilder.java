package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionFix;
import de.unijena.bioinf.jjobs.*;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.procedure.TDoubleProcedure;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

public class GraphBuilder<C extends Candidate<?>> extends BasicMasterJJob<Graph<C>> implements JobProgressEventListener{
    protected static final boolean THIN_OUT_GRAPH = false;//never helped. is slow.

    Graph<C> graph;
    EdgeScorer<C>[] edgeScorers;
    EdgeFilter edgeFilter;

    private int numberOfFinishedComputations = 0;
    private int step;
    private int size;

    public GraphBuilder(Graph<C> graph, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter) {
        super(JobType.CPU);
        this.graph = graph;
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
    }

    public GraphBuilder(String[] ids, Scored<C>[][] possibleFormulas,  EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter) {
        super(JobType.CPU);
        LOG().debug("initialize graph builder");
        this.graph = Graph.getGraph(ids, possibleFormulas);
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
    }

    public static <C extends Candidate<?>> GraphBuilder<C> createGraphBuilder(String[] ids, C[][] possibleFormulas, NodeScorer<C>[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter){
        return createGraphBuilder(ids, possibleFormulas, nodeScorers, edgeScorers, edgeFilter, null);
    }

    public static <C extends Candidate<?>> GraphBuilder<C> createGraphBuilder(String[] ids, C[][] possibleFormulas, NodeScorer<C>[] nodeScorers, EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, TIntHashSet fixedCompounds){
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

        return new GraphBuilder<C>(filteredIds, scoredPossibleFormulas, edgeScorers, edgeFilter);
    }

    private static boolean isFixed(TIntHashSet fixedCompounds, int i) {
        if (fixedCompounds==null) return false;
        if (fixedCompounds.contains(i)) return true;
        return false;
    }

    @Override
    protected Graph<C> compute() throws Exception {
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


    private void calculateWeight() throws ExecutionException {
        Class<C> cClass = getCandidateClass();


        C[][] allCandidates = (C[][]) Array.newInstance(cClass, graph.getPossibleFormulas().length, 1);

        for(int minValue = 0; minValue < allCandidates.length; ++minValue) {
            Scored<C>[] scored = graph.getPossibleFormulas(minValue);
            allCandidates[minValue] = (C[])Array.newInstance(cClass, scored.length);

            for(int final_this = 0; final_this < scored.length; ++final_this) {
                allCandidates[minValue][final_this] = scored[final_this].getCandidate();
            }
        }



        double minV = 0.0D;
        //todo this is a big hack!!!!
        for (EdgeScorer<C> edgeScorer : edgeScorers) {
            if (edgeScorer instanceof ScoreProbabilityDistributionFix){
                if (edgeFilter instanceof EdgeThresholdFilter){
                    ((ScoreProbabilityDistributionFix)edgeScorer).setThresholdAndPrepare(allCandidates);
                } else {
                    ((ScoreProbabilityDistributionFix)edgeScorer).prepare(allCandidates);
                }

            } else if (edgeScorer instanceof ScoreProbabilityDistributionEstimator){
                if (edgeFilter instanceof EdgeThresholdFilter){
                    ((ScoreProbabilityDistributionEstimator)edgeScorer).setThresholdAndPrepare(allCandidates);
                } else {
                    ((ScoreProbabilityDistributionEstimator)edgeScorer).prepare(allCandidates);
                }
            } else {
                edgeScorer.prepare(allCandidates);
            }

            minV += edgeScorer.getThreshold();
        }


        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("minV "+minV);

        this.edgeFilter.setThreshold(minV);
        final Graph final_graph = graph;
        size = graph.getSize();
        step = Math.max(size/20, 1);
        updateProgress(0, size,0, "start computing edges");
        for(int i = 0; i < size; ++i) {
            final int final_i = i;
            final C candidate = graph.getPossibleFormulas1D(i).getCandidate();
            BasicJJob job = new BasicJJob() {
                @Override
                protected Object compute() throws Exception {
                    TDoubleArrayList scores = new TDoubleArrayList(graph.getSize());

                    for(int j = 0; j < graph.getSize(); ++j) {
                        if(graph.getPeakIdx(final_i) == graph.getPeakIdx(j)) {
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

                    edgeFilter.filterEdgesAndSetThreshold(final_graph, final_i, scores.toArray());

                    //progess
                    updateProgress(100);

                    return null;
                }
            };


            job.addPropertyChangeListener(this);
            submitSubJob(job);
        }

        awaitAllSubJobs();

    }


    private void setConnections() {
        long time = System.currentTimeMillis();
        graph.connections = this.edgeFilter.postprocessCompleteGraph(graph);
        HighQualityRandom random = new HighQualityRandom();

        if (GibbsMFCorrectionNetwork.DEBUG){
            LOG().info("setting connections in: "+(System.currentTimeMillis()-time)+" ms");
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

        LOG().info("number of connections " + sum / 2);

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

    private Class<C> getCandidateClass(){
        for (Scored<C>[] s : graph.getPossibleFormulas()) {
            for (Scored<C> scored : s) {
                return (Class<C>)scored.getCandidate().getClass();
            }
        }
        throw new NoSuchElementException("no experiments with any molecular formula candidate given");
    }

    @Override
    public void progressChanged(JobProgressEvent progressEvent) {
        if (progressEvent.getNewValue()!=100) return;
        ++numberOfFinishedComputations;
        if((numberOfFinishedComputations-1) % step == 0 || (numberOfFinishedComputations)==(size)) {
//            LOG.info((100*(progress)/size)+"%");
//            //todo write some job progress
            updateProgress(0, size, numberOfFinishedComputations);

        }
    }

}
