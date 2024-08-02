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

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Scoring from @see <a href=https://academic.oup.com/bioinformatics/article/34/13/i333/5045719></a>
 * <p>
 * In the paper this is named Bayesian (fixed tree).
 */
public class BayesnetScoring implements FingerblastScoringMethod<BayesnetScoring.Scorer> {

    private static final Logger Log = LoggerFactory.getLogger(BayesnetScoring.class);

    @Getter
    protected final TIntObjectHashMap<AbstractCorrelationTreeNode> nodes;
    @Getter
    protected final AbstractCorrelationTreeNode[] nodeList;
    @Getter
    protected final AbstractCorrelationTreeNode[] forests;
    @Getter
    protected final double alpha;
    @Getter
    protected final FingerprintVersion fpVersion;

    @Getter
    protected final PredictionPerformance[] performances;

    @Getter
    @Nullable
    protected FingerprintStatistics statistics;

    @Getter
    protected boolean allowOnlyNegativeScores;

    protected BayesnetScoring(TIntObjectHashMap<AbstractCorrelationTreeNode> nodes, AbstractCorrelationTreeNode[] nodeList, AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores) {
        this(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores, null);
    }
    protected BayesnetScoring(TIntObjectHashMap<AbstractCorrelationTreeNode> nodes, AbstractCorrelationTreeNode[] nodeList, AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores, @Nullable FingerprintStatistics statistics) {
        this.nodes = nodes;
        this.nodeList = nodeList;
        this.forests = forests;
        this.alpha = alpha;
        this.fpVersion = fpVersion;
        this.performances = performances;
        this.allowOnlyNegativeScores = allowOnlyNegativeScores;
        this.statistics = statistics;
    }


    void setStatistics(@Nullable FingerprintStatistics statistics) {
        this.statistics = statistics;
    }

    protected static final String SEP = "\t";

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();

        for (AbstractCorrelationTreeNode node : nodeList) {
            if (node.numberOfParents()==0) continue;
            int child = fpVersion.getAbsoluteIndexOf(node.getFingerprintIndex());

            stringBuilder.append(node.numberOfParents());
            stringBuilder.append(SEP);
            for (AbstractCorrelationTreeNode p : node.getParents()) {
                stringBuilder.append(fpVersion.getAbsoluteIndexOf(p.getFingerprintIndex()));
                stringBuilder.append(SEP);
            }
            stringBuilder.append(child); stringBuilder.append(SEP);
            double[] covariances = node.getCovarianceArray();
            for (int i = 0; i < covariances.length; i++) {
                stringBuilder.append(covariances[i]);
                if (i<covariances.length-1) stringBuilder.append(SEP);
            }

            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public void  writeTreeWithCovToFile(Path outputFile) throws IOException {
        try(BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
            writer.write(this.toString());
        }
    }


    public int getNumberOfRoots(){
        return forests.length;
    }



    protected final static int RootT=0,RootF=1,ChildT=0,ChildF=1;

    /**
     * important: order of parent and child changed!!!!!!
     */
    public static abstract class AbstractCorrelationTreeNode{

        abstract protected void initPlattByRef();

        /*
        get bin index of this to put platt value for this property
         */
        abstract int getIdxThisPlatt(boolean thisTrue, boolean... parentsTrue);

        /*
        get bin index of a parent property to put platt value. This is the index in getParents() array
         */
        abstract int getIdxRootPlatt(boolean thisTrue, int parentIdx, boolean... parentsTrue);


        /*
        add the predicted platt of the property. Include the information of this and the parents true values.
         */
        void addPlattThis(double platt, boolean thisTrue, boolean... parentsTrue) {
            addPlatt(getIdxThisPlatt(thisTrue, parentsTrue), platt);
        }

        void addPlattOfParent(double platt, int parentIdx, boolean thisTrue, boolean... parentsTrue) {
            addPlatt(getIdxRootPlatt(thisTrue, parentIdx, parentsTrue), platt);
        }

        abstract protected void addPlatt(int bin, double platt);

        abstract void computeCovariance();

        abstract void setCovariance(double[] covariances);

        public abstract double getCovariance(int whichCovariance, boolean real, boolean... realParents);

        public abstract double[] getCovarianceArray();

        public abstract AbstractCorrelationTreeNode[] getParents();

        public abstract int numberOfParents();

        abstract void replaceParent(AbstractCorrelationTreeNode oldParent, AbstractCorrelationTreeNode newParent);

        public abstract List<AbstractCorrelationTreeNode> getChildren();

        abstract boolean removeChild(AbstractCorrelationTreeNode child);

        public abstract int getFingerprintIndex();

        abstract void setFingerprintIndex(int newIdx);

        abstract public int getArrayIdxForGivenAssignment(boolean thisTrue, boolean... parentsTrue);

    }


    public static class CorrelationTreeNode extends AbstractCorrelationTreeNode{
        protected AbstractCorrelationTreeNode parent;
        protected List<AbstractCorrelationTreeNode> children;
        protected int fingerprintIndex;

        protected double[] covariances;
        TDoubleArrayList[] plattByRef;

        /**
         * constructor for root
         * @param fingerprintIndex
         */
        public CorrelationTreeNode(int fingerprintIndex) {
            this(fingerprintIndex,  null);
        }

        public CorrelationTreeNode(int fingerprintIndex, AbstractCorrelationTreeNode parent){
            this.fingerprintIndex = fingerprintIndex;
            this.parent = parent;
            this.covariances = new double[4];
            this.children = new ArrayList<>();
            initPlattByRef();
        }

        protected void initPlattByRef(){
            plattByRef = new TDoubleArrayList[8];
            for (int j = 0; j < plattByRef.length; j++) {
                plattByRef[j] = new TDoubleArrayList();
            }

            //add 'pseudo counts'
            for (int j = 0; j < 4; j++) {
                //00
                plattByRef[2*j].add(0d);
                plattByRef[2*j+1].add(0d);
                //01
                plattByRef[2*j].add(0d);
                plattByRef[2*j+1].add(1d);
                //10
                plattByRef[2*j].add(1d);
                plattByRef[2*j+1].add(0d);
                //11
                plattByRef[2*j].add(1d);
                plattByRef[2*j+1].add(1d);
            }
        }

        int getIdxThisPlatt(boolean thisTrue, boolean... rootTrue){
            int idx = 2*getArrayIdxForGivenAssignment(thisTrue, rootTrue);
            return idx;
        }

        @Override
        int getIdxRootPlatt(boolean thisTrue, int parentIdx, boolean... parentsTrue) {
            assert parentIdx==0;
            final int idx = getIdxThisPlatt(thisTrue, parentsTrue);
            return idx+parentIdx+1;
        }

        int getIdxRootPlatt(boolean thisTrue, boolean... parentsTrue) {
            return getIdxRootPlatt(thisTrue, 0, parentsTrue);
        }

        @Override
        protected void addPlatt(int bin, double platt) {
            plattByRef[bin].add(platt);
        }

        void computeCovariance(){
            covariances[getArrayIdxForGivenAssignment(true, true)] = Statistics.covariance(plattByRef[getIdxThisPlatt(true, true)].toArray(), plattByRef[getIdxRootPlatt(true, true)].toArray());
            covariances[getArrayIdxForGivenAssignment(false, true)] = Statistics.covariance(plattByRef[getIdxThisPlatt(false, true)].toArray(), plattByRef[getIdxRootPlatt(false, true)].toArray());
            covariances[getArrayIdxForGivenAssignment(true, false)] = Statistics.covariance(plattByRef[getIdxThisPlatt(true, false)].toArray(), plattByRef[getIdxRootPlatt(true, false)].toArray());
            covariances[getArrayIdxForGivenAssignment(false, false)] = Statistics.covariance(plattByRef[getIdxThisPlatt(false, false)].toArray(), plattByRef[getIdxRootPlatt(false, false)].toArray());
            initPlattByRef();//remove oldPlatt
        }

        void setCovariance(double[] covariances){
            this.covariances = covariances;
            this.initPlattByRef();//remove oldPlatt
        }

        @Override
        public double getCovariance(int whichCovariance, boolean real, boolean... realParent){
            assert realParent.length==1;
            return this.covariances[getArrayIdxForGivenAssignment(real, realParent)];
        }

        @Override
        public double[] getCovarianceArray() {
            return covariances;
        }

        @Override
        public AbstractCorrelationTreeNode[] getParents() {
            return new AbstractCorrelationTreeNode[]{parent};
        }

        @Override
        public int numberOfParents() {
            return (parent==null?0:1);
        }

        @Override
        void replaceParent(AbstractCorrelationTreeNode oldParent, AbstractCorrelationTreeNode newParent) {
            if (!oldParent.equals(parent)) throw new RuntimeException("old parent not found");
            parent = newParent;
        }

        @Override
       public List<AbstractCorrelationTreeNode> getChildren() {
            return children;
        }

        @Override
        boolean removeChild(AbstractCorrelationTreeNode child) {
            return children.remove(child);
        }

        @Override
        public int getFingerprintIndex() {
            return fingerprintIndex;
        }

        @Override
        void setFingerprintIndex(int newIdx) {
            fingerprintIndex = newIdx;
        }

        @Override
        public int getArrayIdxForGivenAssignment(boolean thisTrue, boolean... parentsTrue) {
            assert parentsTrue.length==1;
            return ((thisTrue ? 1 : 0))+(parentsTrue[0] ? 2 : 0);
        }
    }



    public Scorer getScoring() {
        return new Scorer();
    }

    public Scorer getScoring(PredictionPerformance[] performances) {
        return new Scorer();
    }


    public class Scorer implements FingerblastScoring<ProbabilityFingerprint> {
        protected double[][] abcdMatrixByNodeIdxAndCandidateProperties;
        protected ProbabilityFingerprint preparedProbabilityFingerprint;
        protected double[] smoothedPlatt;
        /*
        returns the one interesting field of the computed contingency table
         */
        protected double getABCDMatrixEntry(AbstractCorrelationTreeNode v, boolean thisTrue, boolean... parentsTrue){
            return abcdMatrixByNodeIdxAndCandidateProperties[v.getFingerprintIndex()][v.getArrayIdxForGivenAssignment(thisTrue, parentsTrue)];
        }


        private double threshold, minSamples;

        public Scorer(){
            this.threshold = 0;
            minSamples = 0;
        }

        @Override
        public double getThreshold() {
            return threshold;
        }

        @Override
        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public double getMinSamples() {
            return minSamples;
        }

        @Override
        public void setMinSamples(double minSamples) {
            this.minSamples = minSamples;
        }


        protected double getProbability(int idx, boolean candidateTrue){
            return smoothedPlatt[idx];
        }


        int numberOfComputedContingencyTables;
        int numberOfComputedSimpleContingencyTables;
        TIntHashSet preparedProperties;

        @Override
        public ProbabilityFingerprint extractParameters(ParameterStore store) {
            return store.get(ProbabilityFingerprint.class).orElseThrow();
        }

        @Override
        public void prepare(ProbabilityFingerprint fpPara) {
            numberOfComputedContingencyTables = 0;
            numberOfComputedSimpleContingencyTables =0;
            preparedProperties = new TIntHashSet();
            preparedProbabilityFingerprint = fpPara;
            smoothedPlatt = getSmoothedPlatt(preparedProbabilityFingerprint);
            abcdMatrixByNodeIdxAndCandidateProperties = new double[nodeList.length][];


            for (AbstractCorrelationTreeNode node : nodeList) {
                prepare(node);
            }

        }

        protected double[] getSmoothedPlatt(ProbabilityFingerprint predicted){
            double[] fp = predicted.toProbabilityArray();
            for (int i = 0; i < fp.length; i++) {
                fp[i] = laplaceSmoothing(fp[i], alpha);
            }
            return fp;
        }


        void prepare(AbstractCorrelationTreeNode x){
            if (x.numberOfParents()==0) return;
            preparedProperties.add(x.getFingerprintIndex());
            if (x instanceof CorrelationTreeNode){
                CorrelationTreeNode v = (CorrelationTreeNode)x;
                final AbstractCorrelationTreeNode u = v.parent;
                final int i = u.getFingerprintIndex();
                final int j = v.getFingerprintIndex();


                double[] necessaryABCDs = new double[4];

                for (int k = 0; k < 2; k++) {
                    boolean parentTrue = (k==0);
                    for (int l = 0; l < 2; l++) {
                        boolean childTrue = (l==0);
                        final double p_i = getProbability(i, parentTrue);
                        final double p_j = getProbability(j, childTrue);


                        final double covariance = v.getCovariance(0, childTrue, parentTrue);
                        double[] abcd = computeABCD(covariance, p_i, p_j);
                        necessaryABCDs[v.getArrayIdxForGivenAssignment(childTrue, parentTrue)] = abcd[(parentTrue ? 0 : 1)+((childTrue ? 0 : 2))];
                        ++numberOfComputedSimpleContingencyTables;
                    }

                }

                abcdMatrixByNodeIdxAndCandidateProperties[j] = necessaryABCDs;


            } else {
                throw new RuntimeException("unknown class for AbstractCorrelationTreeNode");
            }
        }


        ProbabilityFingerprint lastFP = null;
        boolean output = false;
        protected int numberOfScoredNodes;
        @Override
        public double score(ProbabilityFingerprint fingerprint, Fingerprint databaseEntry) {
            if (!preparedProbabilityFingerprint.equals(fingerprint)){
                throw new RuntimeException("the prepared fingerprint differs from the currently used one.");
            }
            numberOfScoredNodes = 0;
            if (fingerprint!=lastFP) output = true;

            double logProbability = 0d;

            boolean[] bool = databaseEntry.toBooleanArray();

            for (AbstractCorrelationTreeNode node : nodeList) {
                logProbability += conditional(bool, node);
            }

            return logProbability;
        }


        protected double conditional(boolean[] databaseEntry, AbstractCorrelationTreeNode x) {
            if (x.numberOfParents()==0){
                final int i = x.getFingerprintIndex();
                final boolean real = databaseEntry[i];
                numberOfScoredNodes++;
                if (real){
                    return Math.log(getProbability(i, true));
                } else {
                    return Math.log(1d-getProbability(i,false));
                }
            }

            double score;
            if (x instanceof CorrelationTreeNode){
                CorrelationTreeNode v = (CorrelationTreeNode)x;
                final AbstractCorrelationTreeNode u = v.parent;
                final int i = u.getFingerprintIndex();
                final int j = v.getFingerprintIndex();
                final boolean real = databaseEntry[j];
                final boolean realParent = databaseEntry[i];
                final double p_i = getProbability(i, realParent);

                double correspondingEntry = getABCDMatrixEntry(v, real, realParent);


                //already normalized
                score = Math.log(correspondingEntry);


                //changed
                if (allowOnlyNegativeScores && score>0){
                    Log.debug("overestimated: %f for parent: %d and child: %d with predictions %f and %f and cov %f%n", Math.exp(score), (realParent?1:0), (real?1:0), p_i, getProbability(j, real), v.getCovariance(0, real, realParent));
                    score = 0;
                } else if (score>0) {
                    Log.debug("strange: overestimated: %f for parent: %d and child: %d with predictions %f and %f and cov %f%n", Math.exp(score), (realParent?1:0), (real?1:0), p_i, getProbability(j, real), v.getCovariance(0, real, realParent));
                }


                assert !Double.isNaN(score);
                assert !Double.isInfinite(score);

            } else {
                throw new RuntimeException("unknown class for AbstractCorrelationTreeNode");
            }

            ++numberOfScoredNodes;
            return score;
        }

        protected double[] computeABCD(double covariance, double p_i, double p_j) {
            //matrix
            /*
                    Di
                ------------
             |  a       b   |   pj
         Dj  |              |
             |  c       d   |   1-pj
                ------------
                pi      1-pi


            (1) a+b = pj
            (2) a+c = pi
            (3) a-pi*pj = learnedij
            (4) a+b+c+d = 1

            learnedij is the covariance between i,j

             */

            final double pipj = p_i*p_j;

            //a = learned_ij+pi*pj and \in [0, min{pi,pj}]
            double a = covariance+pipj;
            if (a<0){
                a = 0;
            }
            else if (a > Math.min(p_i, p_j)){
                a = Math.min(p_i, p_j);
            }
            if (a<(p_i+p_j-1)){
                a = p_i+p_j-1;
            }

            double b = p_j-a;
            double c = p_i-a;
            double d = 1d-a-b-c;
            if (d<0) d = 0;

            //normalize
            double pseudoCount = alpha;
            a+=pseudoCount; b+=pseudoCount; c+=pseudoCount; d+=pseudoCount;
            double norm = 1d+4*pseudoCount;
            a/=norm; b/=norm; c/=norm; d/=norm;
            //already normalize against parent
            double sum1 = a+c;
            double sum2 = b+d;
            return new double[]{a/sum1,b/sum2,c/sum1,d/sum2};
        }



    }


    protected static double laplaceSmoothing(double probability, double alpha) {
        return (probability + alpha) / (1d + 2d * alpha);
    }
}
