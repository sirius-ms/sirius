package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.GibbsSampling.model.ReactionStepSizeScorer.ConstantReactionStepSizeScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.ReactionScorer;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GibbsMFCorrectionNetwork {
    private static final double DEFAULT_CORRELATION_STEPSIZE = 100.0D;
    protected Graph graph;
    private static final boolean iniAssignMostLikely = false;
    private int burnInRounds;
    private int currentRound;
    double[] priorProb;
    private int[] activeEdgeCounter;
    int[] activeIdx;
    boolean[] active;
    int[] overallAssignmentFreq;
    double[] assignmentFreqByPosterior;
    double[] maxPosteriorProbs;
    double[] posteriorProbs;
    double[] posteriorProbSums;
    private Random random;
    private final ExecutorService executorService;
    private final int numOfThreads;
    private final double pseudo;
    private final double logPseudo;

    public GibbsMFCorrectionNetwork(String[] ids, MFCandidate[][] possibleFormulas, NodeScorer[] nodeScorers, EdgeScorer[] edgeScorers, EdgeFilter edgeFilter, int threads) {
        this.pseudo = 0.01D;
        this.logPseudo = Math.log(0.01D);

        for (MFCandidate[] pF : possibleFormulas) {
            if (pF==null || pF.length==0) throw new RuntimeException("some peaks don\'t have any explanation");
        }

        this.graph = buildGraph(ids, possibleFormulas, nodeScorers, edgeScorers, edgeFilter, threads);
        this.random = new Random();
        this.numOfThreads = threads;
        this.executorService = Executors.newFixedThreadPool(threads);
        this.setActive();

    }

    public GibbsMFCorrectionNetwork(String[] ids, MFCandidate[][] possibleFormulas, Reaction[] reactions) {
        this(ids, possibleFormulas, new NodeScorer[]{new StandardNodeScorer()}, new EdgeScorer[]{new ReactionScorer(reactions, new ConstantReactionStepSizeScorer())}, new EdgeThresholdFilter(1.0D), 1);
    }

    public GibbsMFCorrectionNetwork(Graph graph, int threads) {
        this.pseudo = 0.01D;
        this.logPseudo = Math.log(0.01D);
        this.graph = graph;
        this.random = new Random();
        this.numOfThreads = threads;
        this.executorService = Executors.newFixedThreadPool(threads);
        this.setActive();
    }

    public void shutdown() {
        this.executorService.shutdown();
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

    public static Graph buildGraph(String[] ids, MFCandidate[][] possibleFormulas, NodeScorer[] nodeScorers, EdgeScorer[] edgeScorers, EdgeFilter edgeFilter, int numOfThreads) {
        NodeScorer[] newIds = nodeScorers;
        int newFormulas = nodeScorers.length;

        int filteredIds;
        for(filteredIds = 0; filteredIds < newFormulas; ++filteredIds) {
            NodeScorer scoredPossibleFormulas = newIds[filteredIds];
            scoredPossibleFormulas.score(possibleFormulas);
        }

        ArrayList var16 = new ArrayList();
        ArrayList var17 = new ArrayList();

        for(filteredIds = 0; filteredIds < possibleFormulas.length; ++filteredIds) {
            MFCandidate[] var19 = possibleFormulas[filteredIds];
            String graph = ids[filteredIds];
            ArrayList scoredCandidates = new ArrayList();
            MFCandidate[] var12 = var19;
            int var13 = var19.length;

            for(int var14 = 0; var14 < var13; ++var14) {
                MFCandidate candidate = var12[var14];
                scoredCandidates.add(new Scored(candidate, candidate.getNodeLogProb()));
            }

            if(scoredCandidates.size() > 0) {
                var16.add(graph);
                var17.add(scoredCandidates.toArray(new Scored[0]));
            }
        }

        String[] var18 = (String[])var16.toArray(new String[0]);
        Scored[][] var20 = (Scored[][])var17.toArray(new Scored[0][]);
        Graph var21 = new Graph(var18, var20);
        var21.init(edgeScorers, edgeFilter, numOfThreads);
        return var21;
    }

    private void setActive() {
        System.out.println("setActive");
        this.priorProb = new double[this.graph.getSize()];
        this.activeEdgeCounter = new int[this.graph.getSize()];
        this.activeIdx = new int[this.graph.numberOfCompounds()];
        this.active = new boolean[this.graph.getSize()];
        int z = 0;

        int i;
        int j;
        for(i = 0; i < this.graph.numberOfCompounds(); ++i) {
            Scored[] conn = this.graph.getPossibleFormulas(i);
            j = -2147483648;
            double[] scores = new double[conn.length];
            double sum = 0.0D;

            for(int j1 = 0; j1 < conn.length; ++j1) {
                double score = Math.exp(conn[j1].getScore());
                scores[j1] = score;
                sum += score;
            }

            j = this.getRandomIdx(0, scores.length - 1, sum, scores);
            this.activeIdx[i] = j;
            this.active[j + z] = true;
            z += conn.length;
        }

        for(i = 0; i < this.priorProb.length; ++i) {
            int[] var11 = this.graph.getConnections(i);

            for(j = 0; j < var11.length; ++j) {
                if(this.active[var11[j]]) {
                    this.addActiveEdge(var11[j], i);
                    ++this.activeEdgeCounter[i];
                }
            }

            this.priorProb[i] += (double)(this.graph.numberOfCompounds() - this.activeEdgeCounter[i] - 1) * Math.log(0.01D);
        }

        this.posteriorProbs = new double[this.graph.getSize()];
        this.posteriorProbSums = new double[this.graph.numberOfCompounds()];

        for(i = 0; i < this.graph.numberOfCompounds(); ++i) {
            this.updatePeak(i);
        }

        this.overallAssignmentFreq = new int[this.graph.getSize()];
        this.assignmentFreqByPosterior = new double[this.graph.getSize()];
        this.maxPosteriorProbs = new double[this.graph.getSize()];
    }

    private double getPosteriorScore(double prior, double score) {
        return prior + score;
    }

    public void iteration(int maxSteps) {
        this.iteration(maxSteps, maxSteps / 5);
    }

    public void iteration(int maxSteps, int burnIn) {
        this.burnInRounds = burnIn;
        int iterationStepLength = this.graph.numberOfCompounds();
        long startTime = System.nanoTime();

        for(int i = 0; i < burnIn + maxSteps; ++i) {
            this.currentRound = i;
            boolean changed = false;
            int[] randomOrdering = getRandomOrdering(iterationStepLength);

            for(int runtime = 0; runtime < randomOrdering.length; ++runtime) {
                if(this.iterationStep(randomOrdering[runtime])) {
                    changed = true;
                }
            }

            if(i % 5000 == 0) {
                long var11 = System.nanoTime() - startTime;
                System.out.println("runtime in ms: " + var11 / 1000000L);
            }
        }

        System.out.println("rounds: " + maxSteps);
    }

    public String[] getIds() {
        return this.graph.getIds();
    }

    public Scored<MFCandidate>[][] getAllPossibleMolecularFormulas() {
        return this.graph.getPossibleFormulas();
    }

    public Scored<MFCandidate>[][] getAllEdges() {
        ArrayList edgeList = new ArrayList();

        for(int i = 0; i < this.graph.getSize(); ++i) {
            int[] currentConnections = this.graph.getConnections(i);
            Scored mf1 = this.graph.getPossibleFormulas1D(i);

            for(int j = 0; j < currentConnections.length; ++j) {
                int c = currentConnections[j];
                if(c <= i) {
                    Scored mf2 = this.graph.getPossibleFormulas1D(c);
                    edgeList.add(new Scored[]{mf1, mf2});
                }
            }
        }

        return (Scored[][])edgeList.toArray(new Scored[0][]);
    }

    public int[][] getAllEdgesIndices() {
        return this.graph.getAllEdgesIndices();
    }

    private Scored<MFCandidate>[][] getFormulasSortedByScoring(double[] scoring) {
        Scored<MFCandidate>[][] candidatesByCompound = new Scored[this.graph.numberOfCompounds()][];

        for(int i = 0; i < this.graph.numberOfCompounds(); ++i) {
            int[] b = this.graph.getPeakBoundaries(i);
            int min = b[0];
            int max = b[1];
            Scored<MFCandidate>[] candidates = new Scored[max - min + 1];
            double sum = 0.0D;

            int j;
            double freq;
            for(j = min; j <= max; ++j) {
                freq = scoring[j];
                sum += freq;
            }

            for(j = min; j <= max; ++j) {
                freq = scoring[j];
                candidates[j - min] = new Scored<>(this.graph.getPossibleFormulas1D(j).getCandidate(), freq / sum);
            }

            Arrays.sort(candidates, Scored.<MFCandidate>desc());
            candidatesByCompound[i] = candidates;
        }

        return candidatesByCompound;
    }

    private Scored<MFCandidate>[][] getFormulasSortedByScoring(int[] scoring) {
        Scored<MFCandidate>[][] candidatesByCompound = new Scored[this.graph.numberOfCompounds()][];

        for(int i = 0; i < this.graph.numberOfCompounds(); ++i) {
            int[] b = this.graph.getPeakBoundaries(i);
            int min = b[0];
            int max = b[1];
            Scored<MFCandidate>[] candidates = new Scored[max - min + 1];
            int sum = 0;

            int j;
            int freq;
            for(j = min; j <= max; ++j) {
                freq = scoring[j];
                sum += freq;
            }

            for(j = min; j <= max; ++j) {
                freq = scoring[j];
                candidates[j - min] = new Scored<>(this.graph.getPossibleFormulas1D(j).getCandidate(), 1.0D * (double)freq / (double)sum);
            }

            Arrays.sort(candidates, Scored.<MFCandidate>desc());
            candidatesByCompound[i] = candidates;
        }

        return candidatesByCompound;
    }

    public EdgeScorer[] getEdgeScorers() {
        return this.graph.getUsedEdgeScorers();
    }

    public Graph getGraph() {
        return this.graph;
    }

    public Scored<MFCandidate>[][] getChosenFormulasByMaxPosterior() {
        return this.getFormulasSortedByScoring(this.maxPosteriorProbs);
    }

    public Scored<MFCandidate>[][] getChosenFormulasBySampling() {
        return this.getFormulasSortedByScoring(this.overallAssignmentFreq);
    }

    public Scored<MFCandidate>[][] getChosenFormulasByAddedUpPosterior() {
        return this.getFormulasSortedByScoring(this.assignmentFreqByPosterior);
    }

    public Scored<MFCandidate>[][] getChosenFormulas() {
        return this.getFormulasSortedByScoring(this.overallAssignmentFreq);
    }

    private boolean iterationStep(int peakIdx) {
        int[] b = this.graph.getPeakBoundaries(peakIdx);
        int min = b[0];
        int max = b[1];
        double probSum = this.posteriorProbSums[peakIdx];
        int absIdx = this.getRandomIdx(min, max, probSum, this.posteriorProbs);
        int relCurrentActive;
        if(this.currentRound > this.burnInRounds) {
            double absCurrentActive;
            if((double)(this.currentRound - this.burnInRounds) % 100.0D == 0.0D) {
                ++this.overallAssignmentFreq[absIdx];

                for(relCurrentActive = min; relCurrentActive <= max; ++relCurrentActive) {
                    absCurrentActive = this.posteriorProbs[relCurrentActive];
                    this.assignmentFreqByPosterior[relCurrentActive] += absCurrentActive;
                }
            }

            for(relCurrentActive = min; relCurrentActive <= max; ++relCurrentActive) {
                absCurrentActive = this.posteriorProbs[relCurrentActive];
                if(this.maxPosteriorProbs[relCurrentActive] < absCurrentActive) {
                    this.maxPosteriorProbs[relCurrentActive] = absCurrentActive;
                }
            }
        }

        relCurrentActive = this.activeIdx[peakIdx];
        int var18 = relCurrentActive + min;
        int relIndex = absIdx - min;
        if(relCurrentActive == relIndex) {
            return false;
        } else {
            BitSet toUpdate = new BitSet();
            int[] c = this.graph.getConnections(var18);
            int[] i = c;
            int var14 = c.length;

            int var15;
            int conjugate;
            int corrspondingPeakIdx;
            for(var15 = 0; var15 < var14; ++var15) {
                conjugate = i[var15];
                this.removeActiveEdge(var18, conjugate);
                corrspondingPeakIdx = this.graph.getPeakIdx(conjugate);
                toUpdate.set(corrspondingPeakIdx);
            }

            c = this.graph.getConnections(absIdx);
            i = c;
            var14 = c.length;

            for(var15 = 0; var15 < var14; ++var15) {
                conjugate = i[var15];
                this.addActiveEdge(absIdx, conjugate);
                corrspondingPeakIdx = this.graph.getPeakIdx(conjugate);
                toUpdate.set(corrspondingPeakIdx);
            }

            for(int var19 = toUpdate.nextSetBit(0); var19 >= 0; var19 = toUpdate.nextSetBit(var19 + 1)) {
                this.updatePeak(var19);
                if(var19 == 2147483647) {
                    break;
                }
            }

            this.activeIdx[peakIdx] = relIndex;
            this.active[var18] = false;
            this.active[absIdx] = true;
            return true;
        }
    }

    private void removeActiveEdge(int outgoing, int incoming) {
        this.priorProb[incoming] -= this.graph.getLogWeight(outgoing, incoming);
        this.priorProb[incoming] += this.logPseudo;
    }

    private void addActiveEdge(int outgoing, int incoming) {
        this.priorProb[incoming] += this.graph.getLogWeight(outgoing, incoming);
        this.priorProb[incoming] -= this.logPseudo;
    }

    private int getRandomIdx(int minIdx, int maxIdx, double probSum, double[] probs) {
        double r = this.random.nextDouble() * probSum;
        int absIdx = minIdx - 1;
        double sum = 0.0D;

        try {
            do {
                ++absIdx;
                sum += probs[absIdx];
            } while(sum < r);
        } catch (Exception var12) {
            var12.printStackTrace();
            System.err.println("sum " + sum);
            System.err.println("min " + maxIdx + " max " + maxIdx + " absIdx " + absIdx + " " + Arrays.toString(Arrays.copyOfRange(probs, minIdx, maxIdx + 1)));
            System.err.println("probsum " + probSum + " sum " + sum + " r " + r);
        }

        if(absIdx > maxIdx) {
            throw new RuntimeException("sampling by probabilities produced error");
        } else {
            return absIdx;
        }
    }

    private void updatePeak(int peakIdx) {
        int[] b = this.graph.getPeakBoundaries(peakIdx);
        int min = b[0];
        int max = b[1];
        double maxLog = -1.0D / 0.0;

        for(int sum = min; sum <= max; ++sum) {
            this.posteriorProbs[sum] = this.getPosteriorScore(this.priorProb[sum], this.graph.getCandidateScore(sum));
            if(this.posteriorProbs[sum] > maxLog) {
                maxLog = this.posteriorProbs[sum];
            }
        }

        double var10 = 0.0D;

        for(int i = min; i <= max; ++i) {
            this.posteriorProbs[i] = Math.exp(this.posteriorProbs[i] - maxLog);
            var10 += this.posteriorProbs[i];
        }

        assert var10 > 0.0D;

        this.posteriorProbSums[peakIdx] = var10;
    }

    public static int[] getRandomOrdering(int max) {
        return getRandomOrdering(0, max);
    }

    public static int[] getRandomOrdering(int min, int max) {
        TIntArrayList numbers = new TIntArrayList(max - min);
        TIntArrayList ordering = new TIntArrayList(max - min);
        Random random = new Random();

        int pos;
        for(pos = min; pos < max; ++pos) {
            numbers.add(pos);
        }

        while(numbers.size() > 0) {
            pos = random.nextInt(numbers.size());
            ordering.add(numbers.removeAt(pos));
        }

        return ordering.toArray();
    }
}
