package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistributionEstimator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Graph<C extends Candidate<?>> {
    final TIntIntHashMap[] indexMap;
    final TDoubleArrayList[] weights;
    double[] edgeThresholds;
    int[][] connections;
    int[] boundaries;
    private int[] formulaIdxToPeakIdx;
    protected final int size;
    final String[] ids;
    final Scored<C>[][] possibleFormulas;
    final Scored<C>[] possibleFormulas1D;
    private EdgeScorer<C>[] edgeScorers;
    private EdgeFilter edgeFilter;

    public Graph(String[] ids, Scored<C>[][] possibleFormulas) {
        this.ids = ids;
        this.possibleFormulas = possibleFormulas;
        this.possibleFormulas1D = this.setUp(possibleFormulas);
        this.size = this.possibleFormulas1D.length;
        this.indexMap = new TIntIntHashMap[this.size];
        this.weights = new TDoubleArrayList[this.size];
        this.edgeThresholds = new double[this.size];

        for(int i = 0; i < this.indexMap.length; ++i) {
            this.indexMap[i] = new TIntIntHashMap(this.size / 100, 0.75F, -1, -1);
            this.weights[i] = new TDoubleArrayList(this.size / 100);
        }

        this.assertInput();
    }

    private void assertInput() {
        Scored[][] var1 = this.possibleFormulas;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Scored[] candidates = var1[var3];
            Scored[] var5 = candidates;
            int var6 = candidates.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                Scored candidate = var5[var7];
                if(candidate.getScore() > 0.0D) {
                    throw new RuntimeException("scores are supposed to be logarithmic");
                }
            }
        }

    }

    public double getLogWeight(int i, int j) {
        int relJ = this.indexMap[i].get(j);
        return relJ < 0?0.0D:this.weights[i].get(relJ);
    }

    public int getNumberOfConnections(int i) {
        return this.weights[i].size();
    }

    public int[] getLogWeightConnections(int i) {
        return this.indexMap[i].keys();
    }

    public void setLogWeight(int i, int j, double weight) {
        int relJ = this.indexMap[i].get(j);
        if(relJ < 0) {
            this.indexMap[i].put(j, this.weights[i].size());
            this.weights[i].add(weight);
        } else {
            this.weights[i].set(relJ, weight);
        }

    }

    public void setEdgeThreshold(int idx, double thres) {
        this.edgeThresholds[idx] = thres;
    }

    public double getEdgeThreshold(int idx) {
        return this.edgeThresholds[idx];
    }

    public int[][] getConnections() {
        return this.connections;
    }

    public int[] getConnections(int index) {
        return this.connections[index];
    }

    public Scored<C>[][] getPossibleFormulas() {
        return this.possibleFormulas;
    }

    public Scored<C>[] getPossibleFormulas(int index) {
        return this.possibleFormulas[index];
    }

    public Scored<C>[] getPossibleFormulas1D() {
        return this.possibleFormulas1D;
    }

    public Scored<C> getPossibleFormulas1D(int index) {
        return this.possibleFormulas1D[index];
    }

    public int getSize() {
        return this.size;
    }

    public int numberOfCompounds() {
        return this.possibleFormulas.length;
    }

    public double getCandidateScore(int index) {
        return this.possibleFormulas1D[index].getScore();
    }

    public String[] getIds() {
        return this.ids;
    }

    public int[][] getAllEdgesIndices() {
        ArrayList edgeList = new ArrayList();

        for(int i = 0; i < this.getSize(); ++i) {
            int[] currentConnections = this.getConnections(i);
            int peakIdx1 = this.getPeakIdx(i);
            int formulaRelIdx1 = this.getRelativeFormulaIdx(i);

            for(int j = 0; j < currentConnections.length; ++j) {
                int c = currentConnections[j];
                if(c <= i) {
                    int peakIdx2 = this.getPeakIdx(c);
                    int formulaRelIdx2 = this.getRelativeFormulaIdx(c);
                    edgeList.add(new int[]{peakIdx1, formulaRelIdx1, peakIdx2, formulaRelIdx2});
                }
            }
        }

        return (int[][])edgeList.toArray(new int[0][]);
    }

    public double[] getAllEdgesWeights() {
        TDoubleArrayList weighList = new TDoubleArrayList();

        for(int i = 0; i < this.getSize(); ++i) {
            int[] currentConnections = this.getConnections(i);

            for(int j = 0; j < currentConnections.length; ++j) {
                int c = currentConnections[j];
                if(c <= i) {
                    weighList.add(this.getLogWeight(i, c));
                }
            }
        }

        return weighList.toArray();
    }

    public void init(EdgeScorer<C>[] edgeScorers, EdgeFilter edgeFilter, int threads) {
        this.edgeScorers = edgeScorers;
        this.edgeFilter = edgeFilter;
        this.calculateWeight(threads);
        this.setConnections();
    }

    public EdgeScorer[] getUsedEdgeScorers() {
        return this.edgeScorers;
    }

    private Scored<C>[] setUp(Scored<C>[][] possibleFormulas) {
        System.out.println("initialize");
        int length = 0;

        for(int possibleFormulas1D = 0; possibleFormulas1D < possibleFormulas.length; ++possibleFormulas1D) {
            length += possibleFormulas[possibleFormulas1D].length;
        }

        Scored[] var9 = new Scored[length];
        this.formulaIdxToPeakIdx = new int[length];
        int z = 0;

        int sum;
        for(sum = 0; sum < possibleFormulas.length; ++sum) {
            Scored[] i = possibleFormulas[sum];

            for(int j = 0; j < i.length; ++j) {
                Scored smf = i[j];
                var9[z] = smf;
                this.formulaIdxToPeakIdx[z] = sum;
                ++z;
            }
        }

        this.boundaries = new int[possibleFormulas.length];
        sum = -1;

        for(int var10 = 0; var10 < possibleFormulas.length; ++var10) {
            sum += possibleFormulas[var10].length;
            this.boundaries[var10] = sum;
        }

        return var9;
    }

    private void setConnections() {
        this.connections = this.edgeFilter.postprocessCompleteGraph(this);
        TDoubleArrayList someScores = new TDoubleArrayList();
        HighQualityRandom random = new HighQualityRandom();

        int sum;
        int b;
        for(sum = 0; sum < 1000; ++sum) {
            int a = random.nextInt(this.numberOfCompounds());
            if(this.connections[a].length != 0) {
                b = random.nextInt(this.connections[a].length);
                someScores.add(this.getLogWeight(a, this.connections[a][b]));
            }
        }

//        System.out.println("some scores1: " + Arrays.toString(someScores.toArray()));

        assert this.isSymmetricSparse(this.connections);

        if(!this.arePeaksConnected(this.connections)) {
            //todo
//            System.out.println("warning: graph is not well connected. consider using less stringent EdgeFilters");
        }

        sum = 0;
        int[][] var8 = this.connections;
        b = var8.length;

        for(int var6 = 0; var6 < b; ++var6) {
            int[] connection = var8[var6];
            sum += connection.length;
        }

        System.out.println("number of connections " + sum / 2);
    }

    private void makeWeightsSymmetricAndCreateConnectionsArray() {
        TIntArrayList[] connectionsList = new TIntArrayList[this.getSize()];

        int connections;
        for(connections = 0; connections < this.getSize(); ++connections) {
            connectionsList[connections] = new TIntArrayList(100);
        }

        int i;
        for(connections = 0; connections < this.getSize(); ++connections) {
            for(i = connections + 1; i < this.getSize(); ++i) {
                double w1 = this.getLogWeight(connections, i);
                double w2 = this.getLogWeight(i, connections);
                double max;
                if(w1 < w2) {
                    this.setLogWeight(connections, i, w2);
                    max = w2;
                } else if(w2 < w1) {
                    this.setLogWeight(i, connections, w1);
                    max = w1;
                } else {
                    max = w1;
                }

                if(max != 0.0D) {
                    connectionsList[connections].add(i);
                }
            }
        }

        int[][] var10 = new int[this.getSize()][];

        for(i = 0; i < var10.length; ++i) {
            var10[i] = connectionsList[i].toArray();
        }

        this.connections = var10;
    }

    private Class<C> getCandidateClass(){
        for (Scored<C>[] s : getPossibleFormulas()) {
            for (Scored<C> scored : s) {
                return (Class<C>)scored.getCandidate().getClass();
            }
        }
        throw new NoSuchElementException("no instances at all");
    }

    private void calculateWeight(int threads) {
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        ArrayList futures = new ArrayList();
//        C[][] allCandidates = (C[][])new Candidate[this.getPossibleFormulas().length][];

        Class<C> cClass = getCandidateClass();


        C[][] allCandidates = (C[][]) Array.newInstance(cClass, this.getPossibleFormulas().length, 1);

        for(int minValue = 0; minValue < allCandidates.length; ++minValue) {
            Scored<C>[] scored = this.getPossibleFormulas(minValue);
//            allCandidates[minValue] = (C[])new Candidate[scored.length];
            allCandidates[minValue] = (C[])Array.newInstance(cClass, scored.length);

            for(int final_this = 0; final_this < scored.length; ++final_this) {
                allCandidates[minValue][final_this] = scored[final_this].getCandidate();
            }
        }

        double minV = 1.0D;

//        for (EdgeScorer<C> edgeScorer : edgeScorers) {
//            edgeScorer.prepare(allCandidates);
//            minV *= ((ScoreProbabilityDistributionEstimator)edgeScorer).getProbabilityDistribution().getMinProbability();
//            System.out.println("minV " + minV);
//        }

        //todo this is a big hack!!!!
        for (EdgeScorer<C> edgeScorer : edgeScorers) {
            if (edgeScorer instanceof ScoreProbabilityDistributionEstimator){
                if (edgeFilter instanceof EdgeThresholdFilter){
                    ((ScoreProbabilityDistributionEstimator)edgeScorer).setThresholdAndPrepare(allCandidates);
                } else {
                    edgeScorer.prepare(allCandidates);
                }
                minV *= ((ScoreProbabilityDistributionEstimator)edgeScorer).getProbabilityDistribution().getMinProbability();
//                System.out.println("minV " + minV);
            } else {
                edgeScorer.prepare(allCandidates);
            }

        }


        
        minV = Math.log(minV);
        this.edgeFilter.setThreshold(minV);
//        System.out.println("min value " + minV);
        final Graph final_graph = this;
        long time = System.currentTimeMillis();

        System.out.println("start computing edges");
        int step = this.getSize()/20;

        for(int i = 0; i < this.getSize(); ++i) {
            if(i % step == 0 || i==(this.getSize()-1)) {
                System.out.println((100*(i+1)/this.getSize())+"%");
            }
            
            final int final_i = i;
            final C var16 = this.getPossibleFormulas1D(i).getCandidate();
            futures.add(executorService.submit(new Runnable() {
                public void run() {
                    TDoubleArrayList scores = new TDoubleArrayList(Graph.this.getSize());

                    for(int j = 0; j < Graph.this.getSize(); ++j) {
                        if(Graph.this.getPeakIdx(final_i) == Graph.this.getPeakIdx(j)) {
                            scores.add(0.0D);
                        } else {
                            C candidate2 = Graph.this.getPossibleFormulas1D(j).getCandidate();
                            double score = 0.0D;
                            EdgeScorer[] var6 = Graph.this.edgeScorers;
                            int var7 = var6.length;

                            for(int var8 = 0; var8 < var7; ++var8) {
                                EdgeScorer edgeScorer = var6[var8];
                                score += Math.log(edgeScorer.score(var16, candidate2));
                            }

                            scores.add(score);
                        }
                    }

//                    if(final_i == 0) {
//                        System.out.println("xscores " + scores.size() + ": " + Arrays.toString(scores.subList(0, 1000).toArray()));
//                    }

                    Graph.this.edgeFilter.filterEdgesAndSetThreshold(final_graph, final_i, scores.toArray());
                }
            }));
        }

        this.futuresGet(futures);
        executorService.shutdown();

        System.out.println("computing edges in ms: "+(System.currentTimeMillis()-time));

        for (EdgeScorer edgeScorer : edgeScorers) {
            edgeScorer.clean();
        }

    }

    private void futuresGet(Iterable<Future> futures) {
        Iterator var2 = futures.iterator();

        while(var2.hasNext()) {
            Future future = (Future)var2.next();

            try {
                future.get();
            } catch (InterruptedException var5) {
                throw new RuntimeException(var5);
            } catch (ExecutionException var6) {
                throw new RuntimeException(var6);
            }
        }

    }

    public int getPeakIdx(int formulaIdx) {
        return this.formulaIdxToPeakIdx[formulaIdx];
    }

    public int getRelativeFormulaIdx(int formulaIdx) {
        int peakIdx = this.getPeakIdx(formulaIdx);
        int[] b = this.getPeakBoundaries(peakIdx);
        int min = b[0];
        return formulaIdx - min;
    }

    public int getAbsoluteFormulaIdx(int peakIdx, int relFormulaIdx) {
        int[] b = this.getPeakBoundaries(peakIdx);
        int min = b[0];
        return min + relFormulaIdx;
    }

    public int[] getPeakBoundaries(int peakIdx) {
        return new int[]{peakIdx == 0?0:this.boundaries[peakIdx - 1] + 1, this.boundaries[peakIdx], peakIdx};
    }

    public int getPeakLeftBoundary(int peakIdx) {
        return peakIdx == 0?0:this.boundaries[peakIdx - 1] + 1;
    }

    public int getPeakRightBoundary(int peakIdx) {
        return this.boundaries[peakIdx];
    }

    private boolean isSymmetricSparse(int[][] connections) {
        for(int i = 0; i < connections.length; ++i) {
            int u = i;
            int[] con = connections[i];

            for(int j = 0; j < con.length; ++j) {
                int c = con[j];
                boolean match = false;
                for (int v : connections[c]) {
                    if (v==u){
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
        }

        return true;
    }

    private void connectionStatistics(int[][] connections) {
        TIntIntHashMap maxCompoundConnectionsStats = new TIntIntHashMap();

        final int numberOfPeaks = numberOfCompounds();
        for (int i = 0; i < numberOfPeaks; i++) {
            int left = getPeakLeftBoundary(i);
            int right = getPeakRightBoundary(i);

            int maxConnections = -1;
            for (int j = left; j <= right; j++) {
                TIntHashSet connectedCompounds = new TIntHashSet();
                int[] conns = getConnections(j);
                for (int c : conns) {
                    connectedCompounds.add(getPeakIdx(c));
                }
                maxConnections = Math.max(maxConnections, connectedCompounds.size());
            }

            maxCompoundConnectionsStats.adjustOrPutValue(maxConnections, 1, 1);
        }

        int[] connectionCounts = maxCompoundConnectionsStats.keys();
        Arrays.sort(connectionCounts);

        final int minConnections = 5;
        final double percentOfBadConnectedCompounds = 0.1; //10%
        final int maxAllowedBadlyConnected = (int)(percentOfBadConnectedCompounds*numberOfPeaks);
        int numberOfBadlyConnected = 0;
        for (int connectionCount : connectionCounts) {
            if (connectionCount>minConnections) break;
            numberOfBadlyConnected += maxCompoundConnectionsStats.get(connectionCount);
        }

        if (numberOfBadlyConnected>maxAllowedBadlyConnected){
            //todo do something
        }

    }

    private boolean arePeaksConnected(int[][] connections){
        int numberOfPeaks = numberOfCompounds();
        List<TIntHashSet> buckets = new ArrayList<>();
        for (int i = 0; i < connections.length; i++) {
            int[] conn = connections[i];
            final int peakIdx = getPeakIdx(i);
            TIntHashSet bucket1 = null;
            for (TIntHashSet bucket : buckets) {
                if (bucket.contains(peakIdx)){
                    bucket1 = bucket;
                    break;
                }
            }
            if (bucket1==null){
                bucket1 = new TIntHashSet();
                bucket1.add(peakIdx);
            }

            for (int j = 0; j < conn.length; j++) {
                final int c = conn[j];
                final int peakIdx2 = getPeakIdx(c);
                TIntHashSet bucket2 = null;
                for (TIntHashSet bucket : buckets) {
                    if (bucket.contains(peakIdx2)){
                        bucket2 = bucket;
                        break;
                    }
                }
                if (bucket2==null) {
                    bucket1.add(peakIdx2);
                } else {
                    if (bucket1!=bucket2){
                        bucket1.addAll(bucket2);
                        buckets.remove(bucket2);
                    }
                }
            }
            if (bucket1.size()==numberOfPeaks) break;
        }

        if (buckets.size()==1) return true;
        for (TIntHashSet bucket : buckets) {
            if (bucket.size()>0.95*numberOfPeaks) return true;
        }
        return false;
    }
}
