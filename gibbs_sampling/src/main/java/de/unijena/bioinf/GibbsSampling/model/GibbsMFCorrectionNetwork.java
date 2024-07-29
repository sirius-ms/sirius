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
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;

public class GibbsMFCorrectionNetwork<C extends Candidate<?>> extends BasicMasterJJob<Scored<C>[][]> {
    public static final boolean DEBUG = false;
    public static final int DEFAULT_CORRELATION_STEPSIZE = 10;
    private static final boolean OUTPUT_SAMPLE_PROBABILITY = false;
    protected Graph<C> graph;
    public static final boolean iniAssignMostLikely = true;
    private int burnInRounds;
    private int currentRound;
    double[] priorProb;
    private int[] activeEdgeCounter;
    int[] activeIdx;
    boolean[] active;
    int[] overallAssignmentFreq;
    double[] posteriorProbs;
    double[] posteriorProbSums;
    private Random random;


    /*
    instead of using sum of log odds, use the maximum.
    this might resolve 'clique' issues (compounds are in fact not independent)
     */
    private static final boolean USE_MAX_PRIOR_PROBABILITY = false;

    /*

     */
    private static final boolean USE_SQRT_PRIOR_PROBABILITY = false;


    /*
    compounds with these indices are fixed. There probability has already been computed and is stored as node score.
    That means active candidates are drawn solely from this probability. And the input probabilityies are also output as resut.
     */
    private TIntHashSet fixedCompounds;


    public GibbsMFCorrectionNetwork(Graph graph) {
        this(graph, null);
    }

    public GibbsMFCorrectionNetwork(Graph graph, TIntHashSet fixedCompounds) {
        super(JobType.CPU);
        this.graph = graph;
        this.fixedCompounds = fixedCompounds==null?new TIntHashSet():fixedCompounds;
        this.random = new Random();
        this.setActive();
    }

    private static boolean isFixed(TIntHashSet fixedCompounds, int i) {
        if (fixedCompounds==null) return false;
        if (fixedCompounds.contains(i)) return true;
        return false;
    }

    private void setActive() {
        this.priorProb = new double[this.graph.getSize()];
        this.activeEdgeCounter = new int[this.graph.getSize()];
        this.activeIdx = new int[this.graph.numberOfCompounds()];
        this.active = new boolean[this.graph.getSize()];
        int z = 0;

        for(int i = 0; i < this.graph.numberOfCompounds(); ++i) {
            Scored[] possibleFormulasArray = this.graph.getPossibleFormulas(i);
            int idx = Integer.MIN_VALUE;
            //set best explanation active
            if (iniAssignMostLikely){
                double maxScore = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < possibleFormulasArray.length; j++) {
                    double score = possibleFormulasArray[j].getScore();
                    if (score>maxScore){
                        maxScore = score;
                        idx = j;
                    }
                }
            } else {
                double[] scores = new double[possibleFormulasArray.length];
                double sum = 0;
                double maxLog = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < possibleFormulasArray.length; j++) {
                    double score = possibleFormulasArray[j].getScore();
                    if (score>maxLog) maxLog = score;
                }

                for (int j = 0; j < possibleFormulasArray.length; j++) {
                    final double score = Math.exp(possibleFormulasArray[j].getScore()-maxLog);
                    scores[j] = score;
                    sum += score;
                }
                assert sum > 0.0D;

                idx = getRandomOrdering(0, scores.length)[0];
            }

            activeIdx[i] = idx;
            active[idx+z] = true;
            z+=possibleFormulasArray.length;
        }


        ///set priorProb and maxPriorProb
        for(int i = 0; i < this.priorProb.length; ++i) {
            //not for fixed compounds
            int peak = graph.getPeakIdx(i);
            if (isFixed(fixedCompounds, peak)) continue;

            int[] conn = this.graph.getConnections(i);
            for(int j = 0; j < conn.length; ++j) {
                if(this.active[conn[j]]) {
                    this.addActiveEdge(conn[j], i);
                    ++this.activeEdgeCounter[i];
                }
            }

        }

        if (DEBUG) System.out.println("number of compounds: "+graph.numberOfCompounds());
        this.posteriorProbs = new double[this.graph.getSize()];
        this.posteriorProbSums = new double[this.graph.numberOfCompounds()];
        //set posteriorProbs, also for fixed compounds
        for(int i = 0; i < this.graph.numberOfCompounds(); ++i) {
            this.updatePeak(i);
        }

        this.overallAssignmentFreq = new int[this.graph.getSize()];
    }

    private double getPosteriorScore(double prior, double score) {
        return prior + score;
    }

    private int maxSteps = -1;
    private int burnIn = -1;

    public void setIterationSteps(int maxSteps, int burnIn) {
        this.maxSteps = maxSteps;
        this.burnIn = burnIn;
    }

    @Override
    protected Scored<C>[][] compute() throws Exception {
        if (maxSteps<0 || burnIn<0) throw new IllegalArgumentException("number of iterations steps not set.");
        updateProgress(0, maxSteps+burnIn, 0);
        setActive();
        this.burnInRounds = burnIn;
        int iterationStepLength = this.graph.numberOfCompounds();
        double sampleProbability;

        int step = (burnIn + maxSteps)/10;

        for(int i = 0; i < burnIn + maxSteps; ++i) {
            this.currentRound = i;
            boolean changed = false;
            int[] randomOrdering = getRandomOrdering(iterationStepLength);

            if (OUTPUT_SAMPLE_PROBABILITY) {
                if (i%10!=0) continue;
//                ...
                double overallProb = 0d;
                int isCorrect = 0;
                for (int j = 0; j < active.length; j++) {
                    if (active[j]){
                        if (((StandardCandidate)this.graph.getPossibleFormulas1D(j).getCandidate()).isCorrect()){
                            ++isCorrect;
                        }
                        double score = this.graph.getCandidateScore(j);


                        for (int k = 0; k < active.length; k++) {
                            if (j==k || !active[k]) continue;
                            score += this.graph.getLogWeight(k, j)/2;
                        }

                        overallProb += score;
                    }

                }
                System.out.println("posterior probability: "+overallProb+" | correct "+isCorrect);
            }


            for(int runtime = 0; runtime < randomOrdering.length; ++runtime) {
                if(this.iterationStep(randomOrdering[runtime])) {
                    changed = true;
                }
            }


            checkForInterruption();
            if (DEBUG && !changed) System.out.println("nothing changed in step "+i);

            updateProgress(0, maxSteps+burnIn, i+1);
//            if((i % step == 0 && i>0) || i == (burnIn+maxSteps-1)) {
//                logInfo("step "+((double)(((i+1)*100/(maxSteps+burnIn))))+"%");
//
//            }
        }
        return getChosenFormulas();
    }

    public String[] getIds() {
        return this.graph.getIds();
    }

    public Scored<C>[][] getAllPossibleMolecularFormulas() {
        return this.graph.getPossibleFormulas();
    }

    public Scored<C>[][] getAllEdges() {
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

    private Scored<C>[][] getFormulasSortedByScoring(double[] scoring) {
        Scored<C>[][] candidatesByCompound = new Scored[this.graph.numberOfCompounds()][];

        for(int i = 0; i < this.graph.numberOfCompounds(); ++i) {
            int[] b = this.graph.getPeakBoundaries(i);
            int min = b[0];
            int max = b[1];
            Scored<C>[] candidates = new Scored[max - min + 1];
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

            Arrays.sort(candidates, Comparator.reverseOrder());
            candidatesByCompound[i] = candidates;
        }

        return candidatesByCompound;
    }

    private Scored<C>[][] getFormulasSortedByScoring(int[] scoring) {
        Scored<C>[][] candidatesByCompound = new Scored[this.graph.numberOfCompounds()][];

        for(int i = 0; i < this.graph.numberOfCompounds(); ++i) {
            if (isFixed(fixedCompounds, i)){
                Scored<C>[] candidatesLogScore = graph.getPossibleFormulas(i);
                Scored<C>[] candidatesScored = new Scored[candidatesLogScore.length];
                for (int j = 0; j < candidatesScored.length; j++) {
                    //todo normalize??
                    candidatesScored[j] = new Scored<>(candidatesLogScore[j].getCandidate(), Math.exp(candidatesLogScore[j].getScore()));
                }
                candidatesByCompound[i] = candidatesScored;
                continue;
            }


            int[] b = this.graph.getPeakBoundaries(i);
            int min = b[0];
            int max = b[1];
            Scored<C>[] candidates = new Scored[max - min + 1];
            int sum = 0;

            int j;
            int freq;
            for(j = min; j <= max; ++j) {
                freq = scoring[j];
                sum += freq;
            }

            for(j = min; j <= max; ++j) {
                freq = scoring[j];
                double score = 1.0D * (double)freq / (double)sum;
                if (Double.isNaN(score)) {
                    throw new IllegalStateException("ZODIAC Gibbs sampling produced NaN score for: "+this.graph.getIds()[i]);
                }
                candidates[j - min] = new Scored<>(this.graph.getPossibleFormulas1D(j).getCandidate(), score);
            }

            Arrays.sort(candidates, Comparator.reverseOrder());
            candidatesByCompound[i] = candidates;
        }

        return candidatesByCompound;
    }

    public Graph getGraph() {
        return this.graph;
    }


    public Scored<C>[][] getChosenFormulasBySampling() {
        return this.getFormulasSortedByScoring(this.overallAssignmentFreq);
    }

    public Scored<C>[][] getChosenFormulas() {
        return this.getFormulasSortedByScoring(this.overallAssignmentFreq);
    }

    private boolean iterationStep(int peakIdx) {
        int[] b = this.graph.getPeakBoundaries(peakIdx);
        int min = b[0];
        int max = b[1];
        double probSum = this.posteriorProbSums[peakIdx];
        int absIdx = this.getRandomIdx(min, max, probSum, this.posteriorProbs);
        if(this.currentRound > this.burnInRounds) {
            if((double)(this.currentRound - this.burnInRounds) % DEFAULT_CORRELATION_STEPSIZE == 0.0D) {
                ++this.overallAssignmentFreq[absIdx];
            }
        }

        int relCurrentActive = this.activeIdx[peakIdx];
        int absCurrentActive = relCurrentActive + min;
        int relIndex = absIdx - min;
        if(relCurrentActive == relIndex) {
            return false;
        } else {
            BitSet toUpdate = new BitSet();
            int[] c = this.graph.getConnections(absCurrentActive);
            for (int conjugate : c) {
                final int corrspondingPeakIdx = this.graph.getPeakIdx(conjugate);
                if (isFixed(fixedCompounds, corrspondingPeakIdx)) continue;
                this.removeActiveEdge(absCurrentActive, conjugate);
                toUpdate.set(corrspondingPeakIdx);
            }

            c = this.graph.getConnections(absIdx);
            for (int conjugate : c) {
                final int corrspondingPeakIdx = this.graph.getPeakIdx(conjugate);
                if (isFixed(fixedCompounds, corrspondingPeakIdx)) continue;
                this.addActiveEdge(absIdx, conjugate);
                toUpdate.set(corrspondingPeakIdx);
            }


            for (int i = toUpdate.nextSetBit(0); i >= 0; i = toUpdate.nextSetBit(i+1)) {
                if (!isFixed(fixedCompounds, i)) updatePeak(i);
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }

            this.activeIdx[peakIdx] = relIndex;
            this.active[absCurrentActive] = false;
            this.active[absIdx] = true;
            return true;
        }
    }


    private void removeActiveEdge(int outgoing, int incoming) {
        if (USE_MAX_PRIOR_PROBABILITY) {
            final double removedWeight = this.graph.getLogWeight(outgoing, incoming);
            final double currentWeight = this.priorProb[incoming];
            if (removedWeight==currentWeight){
                //find 2nd best score
                double max = 0; //no active edge = 0;
                int[] conn = this.graph.getConnections(incoming);
                for(int j = 0; j < conn.length; ++j) {
                    final int c = conn[j];
                    if(this.active[c] && c!=outgoing) {
                        final double weight = graph.getLogWeight(c, incoming);
                        if (weight>max) max = weight;
                    }
                }

                this.priorProb[incoming] = max;

            }
            //else weight is not interesting. do nothing.

        } else {
            if (USE_SQRT_PRIOR_PROBABILITY){
                this.priorProb[incoming] -= Math.sqrt(this.graph.getLogWeight(outgoing, incoming));
            } else {
                this.priorProb[incoming] -= (this.graph.getLogWeight(outgoing, incoming));
            }
        }
    }

    private void addActiveEdge(int outgoing, int incoming) {
        if (USE_MAX_PRIOR_PROBABILITY) {
            final double newWeight = this.graph.getLogWeight(outgoing, incoming);
            final double currentWeight = this.priorProb[incoming];
            if (newWeight>currentWeight){
                this.priorProb[incoming] = newWeight;
            }
        } else {
            if (USE_SQRT_PRIOR_PROBABILITY){
                this.priorProb[incoming] += Math.sqrt(this.graph.getLogWeight(outgoing, incoming));
            } else {
                this.priorProb[incoming] += (this.graph.getLogWeight(outgoing, incoming));
            }
        }
    }




    /**
     *
     * @param minIdx
     * @param maxIdx
     * @param probSum
     * @param probs
     * @return absolute index
     */
    private int getRandomIdx(int minIdx, int maxIdx, double probSum, double[] probs){
        double r = random.nextDouble()*probSum;
        int absIdx = minIdx-1;
        double sum = 0;

        if (DEBUG) {
            try {
                do {
                    absIdx++;
                    sum += probs[absIdx];
                } while (sum<r);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("sum "+sum);
                System.err.println("min "+maxIdx+" max "+maxIdx+" absIdx "+absIdx+" "+Arrays.toString(Arrays.copyOfRange(probs, minIdx, maxIdx+1)));
                System.err.println("probsum "+probSum+" sum "+sum+" r "+r);
            }

            if (currentRound%10==0) {
                FragmentsCandidate candidate = (FragmentsCandidate)graph.getPossibleFormulas1D(minIdx).getCandidate();
                if (candidate.getExperiment().getName().equals("719")){
                    System.out.println("sampled "+(absIdx-minIdx));
                }
            }
        } else {
            do {
                absIdx++;
                sum += probs[absIdx];
            } while (sum<r);
        }

        if (absIdx>maxIdx) throw new RuntimeException("sampling by probability produced error");

        return absIdx;
    }

    private void updatePeak(int peakIdx) {
        int[] b = this.graph.getPeakBoundaries(peakIdx);
        int min = b[0];
        int max = b[1];
        double maxLog = Double.NEGATIVE_INFINITY;

        FragmentsCandidate candidate = null;
        if (DEBUG) {
            candidate = (FragmentsCandidate) graph.getPossibleFormulas1D(min).getCandidate();
            if (currentRound % 10 == 0) {
                if (candidate.getExperiment().getName().equals("719")) {
                    System.out.println("probs 719");
                    System.out.println(Arrays.toString(Arrays.copyOfRange(priorProb, min, max + 1)));
                    double[] scores = new double[max - min + 1];
                    for (int i = min; i <= max; i++) {
                        scores[i - min] = graph.getPossibleFormulas1D(i).getScore();

                    }
                    System.out.println(Arrays.toString(scores));
                }
            }
        }


        for(int i = min; i <= max; ++i) {
            this.posteriorProbs[i] = this.getPosteriorScore(this.priorProb[i], this.graph.getCandidateScore(i));
            if(this.posteriorProbs[i] > maxLog) {
                maxLog = this.posteriorProbs[i];
            }
        }

        double sum = 0.0D;

        for(int i = min; i <= max; ++i) {
            this.posteriorProbs[i] = Math.exp(this.posteriorProbs[i] - maxLog);
            sum += this.posteriorProbs[i];
        }

        if (DEBUG) {
            if (currentRound%10==0) {
                if (candidate.getExperiment().getName().equals("719")) {
                    System.out.println(Arrays.toString(Arrays.copyOfRange(posteriorProbs, min, max + 1)));
                }
            }
        }

        assert sum > 0.0D;

        this.posteriorProbSums[peakIdx] = sum;
    }

    /**
     *
     * @param max 0..max , max exclusive
     * @return
     */
    public static int[] getRandomOrdering(int max){
        return getRandomOrdering(0, max);
    }

    /**
     * min ... max, max exclusive
     * @param min
     * @param max
     * @return
     */
    public static int[] getRandomOrdering(int min, int max) {
        TIntArrayList numbers = new TIntArrayList(max - min);
        TIntArrayList ordering = new TIntArrayList(max - min);
        Random random = new Random();

        for(int i = min; i < max; ++i) {
            numbers.add(i);
        }

        while(numbers.size() > 0) {
            final int pos = random.nextInt(numbers.size());
            ordering.add(numbers.removeAt(pos));
        }

        return ordering.toArray();
    }

    /**
     * don't do gibbs sampling, but fix all graph and compute probabilities for one candidate.
     * node scores should be normalized
     * @return
     */
    public static <C extends Candidate<?>> Scored<C>[] computeFromSnapshot(Graph<C> graph, int compoundIdx) {
        int left = graph.getPeakLeftBoundary(compoundIdx);
        int right = graph.getPeakRightBoundary(compoundIdx);
        Scored<C>[] scoredCandidates = new Scored[right-left+1];

        double[] scores = new double[right-left+1];
        for (int i = left; i <= right; i++) {
            int[] conns = graph.getConnections(i);
            double score = graph.getCandidateScore(i);
            for (int c : conns) {
                score += graph.getLogWeight(c, i)*Math.exp(graph.getCandidateScore(c));
            }
            scores[i-left] = score;
        }


        double maxLog = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < scores.length; i++) {
            double score = scores[i];
            if (maxLog<score) maxLog = score;
        }

        double sum = 0;
        for (int i = 0; i < scores.length; i++) {
            final double score = Math.exp(scores[i]-maxLog);
            scores[i] = score;
            sum += score;
        }
        assert sum > 0.0D;


        for (int i = left; i <= right; i++) {
            scoredCandidates[i-left] = new Scored(graph.getPossibleFormulas1D(i).getCandidate(), scores[i-left]/sum);
        }

        Arrays.sort(scoredCandidates, Comparator.reverseOrder());

        return scoredCandidates;
    }

}
