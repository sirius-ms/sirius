package de.unijena.bioinf.fingerid.blast;/*
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

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * this is not a functioning scoring but for testing purposes only
 */
public class BayesnetScoringWithTwoParents extends BayesnetScoring {

    private static final Logger Log = LoggerFactory.getLogger(BayesnetScoringWithTwoParents.class);

    protected BayesnetScoringWithTwoParents(TIntObjectHashMap<AbstractCorrelationTreeNode> nodes, AbstractCorrelationTreeNode[] nodeList, AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores) {
        super(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores);
    }



    public static class TwoParentsCorrelationTreeNode extends AbstractCorrelationTreeNode {
        protected AbstractCorrelationTreeNode[] parents;
        protected List<AbstractCorrelationTreeNode> children;
        protected int fingerprintIndex;

        protected double[][] covariances;
        TDoubleArrayList[] plattByRef;

        private int numberOfCombinations;

        public TwoParentsCorrelationTreeNode(int fingerprintIndex, AbstractCorrelationTreeNode... parents) {
            assert parents.length==2;
            this.fingerprintIndex = fingerprintIndex;
            this.parents = parents;
            //number of combinations of child,parent_i,... being 0/1 times number of necessary correlations (pairwise and more)
            this.numberOfCombinations = (int) Math.pow(2,parents.length+1);
//            this.covariances = new double[numberOfCombinations][numberOfCombinations-1-(parents.length+1)];
            this.covariances = new double[numberOfCombinations][numberOfCombinations]; //just use it with some 'holes' in between
            this.children = new ArrayList<>();
            initPlattByRef();
        }

        protected void initPlattByRef() {
            plattByRef = new TDoubleArrayList[(parents.length + 1) * numberOfCombinations];
            for (int j = 0; j < plattByRef.length; j++) {
                plattByRef[j] = new TDoubleArrayList();
            }

            //add 'pseudo counts'
            for (int j = 0; j < numberOfCombinations; j++) {

                //000
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(0d);
                //
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(1d);
//
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(0d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(0d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(0d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(0d);
                plattByRef[3 * j + 2].add(1d);
//
                plattByRef[3 * j].add(0d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(1d);
//
                plattByRef[3 * j].add(1d);
                plattByRef[3 * j + 1].add(1d);
                plattByRef[3 * j + 2].add(1d);


            }

        }

        @Override
        int getIdxThisPlatt(boolean thisTrue, boolean... parentsTrue) {
            int idx = 3*getArrayIdxForGivenAssignment(thisTrue, parentsTrue);
            return idx;
        }

        @Override
        int getIdxRootPlatt(boolean thisTrue, int parentIdx, boolean... parentsTrue) {
            final int idx = getIdxThisPlatt(thisTrue, parentsTrue);
            return idx+parentIdx+1;
        }

        @Override
        protected void addPlatt(int bin, double platt) {
            plattByRef[bin].add(platt);
        }


        @Override
        public int getArrayIdxForGivenAssignment(boolean thisTrue, boolean... parentsTrue){
            assert parentsTrue.length==2;
            return ((thisTrue ? 1 : 0))+(parentsTrue[0] ? 2 : 0)+(parentsTrue[1] ? 4 : 0);
        }

        /*
        get idx of the covariance you like to have: e.g. c_ij -> (thisTrue, parent0True, parent1False) or c_ijk -> (thisTrue, parent0True, parent1True)
         */
        public int getCovIdx(boolean thisContained, boolean... parentsContained){
            final int idx = getArrayIdxForGivenAssignment(thisContained, parentsContained); //this actually introduces some 'holes' in the array (e.g their is no covariance of one variable or the empty set)
            return idx;
        }



        @Override
        void computeCovariance() {
            //indices of the specific cov in array
            int covIndex_ij = getCovIdx(true, true, false);
            int covIndex_ik = getCovIdx(true, false, true);
            int covIndex_jk = getCovIdx(false, true, true);
            int covIndex_ijk = getCovIdx(true, true, true);


            for (int l = 0; l < 2; l++) {
                boolean thisTrue = (l==1);
                for (int m = 0; m < 2; m++) {
                    boolean parent0True = (m==1);
                    for (int n = 0; n < 2; n++) {
                        boolean parent1True = (n==1);

                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_ij] =
                                Statistics.covariance(
                                        plattByRef[getIdxThisPlatt(thisTrue, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 0, parent0True, parent1True)].toArray()
                                );

                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_ik] =
                                Statistics.covariance(
                                        plattByRef[getIdxThisPlatt(thisTrue, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 1, parent0True, parent1True)].toArray()
                                );
                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_jk] =
                                Statistics.covariance(
                                        plattByRef[getIdxRootPlatt(thisTrue, 0, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 1, parent0True, parent1True)].toArray()
                                );
                        covariances[getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)][covIndex_ijk] =
                                covariance(
                                        plattByRef[getIdxThisPlatt(thisTrue, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 0, parent0True, parent1True)].toArray(),
                                        plattByRef[getIdxRootPlatt(thisTrue, 1, parent0True, parent1True)].toArray()
                                );
                    }
                }
            }

            initPlattByRef();//remove oldPlatt
        }

        private double covariance(double[] a, double[] b, double[] c){
            if (a.length!=b.length || b.length!=c.length) throw new RuntimeException("array sizes differ");
            double meanA = Statistics.expectation(a);
            double meanB = Statistics.expectation(b);
            double meanC = Statistics.expectation(c);

            double sum = 0;
            for (int i = 0; i < a.length; i++) {
                sum += (a[i]-meanA)*(b[1]-meanB)*(c[i]-meanC);
            }
            return sum/a.length; //todo n or n-1?
        }


        @Override
        void setCovariance(double[] covariances) {
            //convert to 2D
            int secondDimSize = numberOfCombinations;//numberOfCombinations-1-(parents.length-1);
            if (covariances.length!=(numberOfCombinations*secondDimSize))
                throw new RuntimeException(String.format("incorrect covariance array length: %d vs %d%n",covariances.length, numberOfCombinations*secondDimSize));
            int i=0, j=0;
            for (double covariance : covariances) {
                this.covariances[i][j] = covariance;
                if (j==secondDimSize-1){
                    ++i;
                    j=0;
                } else {
                    ++j;
                }
            }
            initPlattByRef();
        }

        //todo make some stuff protected again
        @Override
        public double getCovariance(int whichCovariance, boolean real, boolean... realParents) {
            return covariances[getCovIdx(real, realParents)][whichCovariance];
        }

        @Override
        public double[] getCovarianceArray() {
            //convert
//            double[] covariances1D = new double[numberOfCombinations*numberOfCombinations-1-(parents.length-1)];
            double[] covariances1D = new double[numberOfCombinations*numberOfCombinations];
            int i = 0;
            for (int j = 0; j < covariances.length; j++) {
                double[] cov = covariances[j];
                for (int k = 0; k < cov.length; k++) {
                    covariances1D[i++] = cov[k];
                }
            }
            return covariances1D;
        }

        @Override
        public AbstractCorrelationTreeNode[] getParents() {
            return parents;
        }

        @Override
        public int numberOfParents() {
            return parents.length;
        }

        @Override
        void replaceParent(AbstractCorrelationTreeNode oldParent, AbstractCorrelationTreeNode newParent) {
            for (int i = 0; i < parents.length; i++) {
                if (oldParent.equals(parents[i])){
                    parents[i] = newParent;
                    return;
                }
            }
            throw new RuntimeException("old parent not found");
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

    }

    public class Scorer extends BayesnetScoring.Scorer {

        @Override
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
                TwoParentsCorrelationTreeNode v = (TwoParentsCorrelationTreeNode)x;
                final AbstractCorrelationTreeNode[] parents = v.getParents();
                final int i = v.getFingerprintIndex();
                final int j = parents[0].getFingerprintIndex();
                final int k = parents[1].getFingerprintIndex();

                //indices of the specific cov in array
                int covIndex_ij = v.getCovIdx(true, true, false);
                int covIndex_ik = v.getCovIdx(true, false, true);
                int covIndex_jk = v.getCovIdx(false, true, true);
                int covIndex_ijk = v.getCovIdx(true, true, true);


                double[] necessaryContingencyEntries = new double[8];
                for (int l = 0; l < 2; l++) {
                    boolean thisTrue = (l==1);
                    for (int m = 0; m < 2; m++) {
                        boolean parent0True = (m==1);
                        for (int n = 0; n < 2; n++) {
                            boolean parent1True = (n==1);

                            final double p_i = getProbability(i, thisTrue);
                            final double p_j = getProbability(j, parent0True);
                            final double p_k = getProbability(k, parent1True);


                            double cov_ij = v.getCovariance(covIndex_ij, thisTrue, parent0True, parent1True);
                            double cov_ik = v.getCovariance(covIndex_ik, thisTrue, parent0True, parent1True);
                            double cov_jk = v.getCovariance(covIndex_jk, thisTrue, parent0True, parent1True);
                            double cov_ijk = v.getCovariance(covIndex_ijk, thisTrue, parent0True, parent1True);

                            double[][][] q = computeContingencyTable(p_i, p_j, p_k, cov_ij, cov_ik, cov_jk, cov_ijk, alpha);

                            necessaryContingencyEntries[v.getArrayIdxForGivenAssignment(thisTrue, parent0True, parent1True)] = q[l][m][n];
                            ++numberOfComputedContingencyTables;
                        }
                    }
                }

                abcdMatrixByNodeIdxAndCandidateProperties[i] = necessaryContingencyEntries;

            }

        }

        @Override
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
                if (Double.isNaN(score) || Double.isInfinite(score)){
                    System.err.println("NaN score for the following fingerprints:");
                    System.err.println(Arrays.toString(smoothedPlatt));
                    System.err.println(Arrays.toString(databaseEntry));
                    System.err.println("for tree node u (" + u.getFingerprintIndex() + ") -> v (" + v.getFingerprintIndex() + ")");
                    System.err.println("with covariance:");
                    System.err.println(Arrays.toString(v.covariances));
                    System.err.printf(Locale.US, "p_i = %f\n", p_i);
                    System.err.printf(Locale.US, "alpha = %f\n", alpha);
                    throw new RuntimeException("bad score: "+score);
                }


            } else {
                TwoParentsCorrelationTreeNode v = (TwoParentsCorrelationTreeNode)x;
                final AbstractCorrelationTreeNode[] parents = v.getParents();
                final int i = v.getFingerprintIndex();
                final int j = parents[0].getFingerprintIndex();
                final int k = parents[1].getFingerprintIndex();

                final boolean real = databaseEntry[i];
                final boolean realParent0 = databaseEntry[j];
                final boolean realParent1 = databaseEntry[k];

                final double p_i = getProbability(i, real);
                final double p_j = getProbability(j, realParent0);
                final double p_k = getProbability(k, realParent1);



                double correspondingEntry = Math.log(getABCDMatrixEntry(v, real, realParent0, realParent1));


                // already normalized
                score = correspondingEntry;


                if (allowOnlyNegativeScores && score>0.01){
                    Log.debug("overestimated2: %f for parent1: %d parent2: %d and child: %d with predictions %f and %f and %f and parent_cov %f%n", Math.exp(score), (realParent1?1:0), (realParent1?1:0), (real?1:0), p_j, p_k, p_i, v.getCovariance(v.getCovIdx(false, true, true), real, realParent0, realParent1));
                    score = 0;
                }

                assert !Double.isNaN(score);
                assert !Double.isInfinite(score);
                if (Double.isNaN(score) || Double.isInfinite(score)){
                    System.err.println("NaN score for the following fingerprints:");
                    System.err.println(Arrays.toString(smoothedPlatt));
                    System.err.println(Arrays.toString(databaseEntry));
                    System.err.println("for tree node u (" + parents[0].getFingerprintIndex() + ", " + parents[1].getFingerprintIndex() + ") -> v (" + v.getFingerprintIndex() + ")");
                    System.err.println("with covariances: ");
                    for (double[] row : v.covariances) {
                        System.err.println("\t" + Arrays.toString(row));
                    }
//                System.err.printf(Locale.US, "and a = %f, b = %f, c = %f, d = %f\n", a, b, c, d);
                    System.err.printf(Locale.US, "p_i = %f\n", p_i);
                    System.err.printf(Locale.US, "alpha = %f\n", alpha);
                    throw new RuntimeException("bad score: "+score);
                }

            }

            ++numberOfScoredNodes;
            return score;
        }

        protected double[][][] computeContingencyTable(double p_i, double p_j, double p_k, double cov_ij, double cov_ik, double cov_jk, double cov_ijk, double pseudoCount) {
            final double pipj = p_i*p_j;
            final double pipk = p_i*p_k;
            final double pjpk = p_j*p_k;
            final double pipjpk = pipj*p_k;


            double q111_soft = Math.max(cov_ij+pipj+cov_ik+pipk+cov_jk+pjpk-p_i-p_j-p_k,
                    Math.min(1+cov_ij+pipj+cov_ik+pipk+cov_jk+pjpk-p_i-p_j-p_k,
                            cov_ijk+p_k*cov_ij+p_j*cov_ik+p_i*cov_jk+pipjpk
                    )
            );

            double q111 = Math.max(0, Math.min(p_i, Math.min(p_j, Math.min(p_k,q111_soft))));

            if (q111<p_i+p_j-1-p_i*p_j*(1-p_k)){
                final double newV = p_i+p_j-1-p_i*p_j*(1-p_k);
//            System.out.printf("thresholding %f to %f%n", q111, newV);
                q111=newV;

            }
            if (q111<p_i+p_k-1-p_i*p_k*(1-p_j)){
                final double newV = p_i+p_k-1-p_i*p_k*(1-p_j);
//            System.out.printf("thresholding2 %f to %f%n", q111, newV);
                q111=newV;
            }
            if (q111<p_j+p_k-1-p_j*p_k*(1-p_i)){
                final double newV = p_j+p_k-1-p_j*p_k*(1-p_i);
//            System.out.printf("thresholding3 %f to %f%n", q111, newV);
                q111=newV;
            }
            if (q111>(p_i+p_j+p_k)/3){
                final double newV = (p_i+p_j+p_k)/3;
                System.out.printf("thresholding4 %f to %f%n", q111, newV);
                q111=newV;
            }


            if (q111<p_i+p_j-p_i*p_j-1){
                final double newV = p_i+p_j-p_i*p_j-1;
                System.out.printf("thresholding1.2 %f to %f%n",q111, newV);
                q111 = newV;
            }
            if (q111<p_i+p_k-p_i*p_k-1){
                final double newV = p_i+p_k-p_i*p_k-1;
                System.out.printf("thresholding2.2 %f to %f%n",q111, newV);
                q111 = newV;
            }
            if (q111<p_j+p_k-p_j*p_k-1){
                final double newV = p_j+p_k-p_j*p_k-1;
                System.out.printf("thresholding3.2 %f to %f%n",q111, newV);
                q111 = newV;
            }



            double x_soft = Math.max(0.000, cov_ij + pipj + cov_ik + pipk - 2*q111); //changed >=0
            double y_soft = Math.max(0.000, cov_ij + pipj + cov_jk + pjpk - 2*q111);
            double z_soft = Math.max(0.000, cov_ik + pipk + cov_jk + pjpk - 2*q111);

            //now iterate and find good multiplier to fullfill constraints
            double[] xyz = new double[]{x_soft,y_soft,z_soft};
            boolean[] leaveOut_xyz = new boolean[]{false, false, false};
            if (!satisfyConstraints2(xyz, leaveOut_xyz, p_i, p_j, p_k, q111)){
                new RuntimeException("we got a problem. constraints not satisfiable");
            }

            double[][][] q = new double[2][2][2];
            q[1][1][1] = q111;


            //changed to test if 0
            q[1][1][0] = Math.min(p_i-q111, Math.min(p_j-q111,(x_soft==0?0:(cov_ij+pipj-q111) / x_soft * xyz[0] / 2) + (y_soft==0?0:(cov_ij+pipj-q111) / y_soft * xyz[1] / 2)));
            q[1][0][1] = Math.min(p_i-q111, Math.min(p_k-q111,(x_soft==0?0:(cov_ik+pipk-q111) / x_soft * xyz[0] / 2) + (z_soft==0?0:(cov_ik+pipk-q111) / z_soft * xyz[2] / 2)));
            q[0][1][1] = Math.min(p_j-q111, Math.min(p_k-q111,(y_soft==0?0:(cov_jk+pjpk-q111) / y_soft * xyz[1] / 2) + (z_soft==0?0:(cov_jk+pjpk-q111) / z_soft * xyz[2] / 2)));

            if (q[1][1][1]+q[1][1][0]+q[1][0][1]>1.5){

                System.out.printf("too big1: %f %f %f with xyz %f vs %f | %f vs %f | %f vs %f%n", q[1][1][1], q[1][1][0], q[1][0][1], x_soft, xyz[0], y_soft, xyz[1], z_soft, xyz[2]);
            }
            if (q[1][1][1]+q[1][1][0]+q[0][1][1]>1.5){
                System.out.printf("too big2: %f %f %f with xyz %f vs %f | %f vs %f | %f vs %f%n", q[1][1][1], q[1][1][0], q[0][1][1], x_soft, xyz[0], y_soft, xyz[1], z_soft, xyz[2]);
            }
            if (q[1][1][1]+q[1][0][1]+q[0][1][1]>1.5){
                System.out.printf("too big3: %f %f %f with xyz %f vs %f | %f vs %f | %f vs %f%n", q[1][1][1], q[1][0][1], q[0][1][1], x_soft, xyz[0], y_soft, xyz[1], z_soft, xyz[2]);
            }



            if (anyNAN(q)) {
                System.out.println("stop");
                for (int i = 0; i < q.length; i++) {
                    final double[][] level2 = q[i];
                    for (int j = 0; j < level2.length; j++) {
                        final double[] level3 = level2[j];
                        for (int k = 0; k < level3.length; k++) {
                            System.out.printf("q%d%d%d = %f%n",i,j,k,q[i][j][k]);
                        }

                    }
                }
                System.out.printf("xyz %f %f %f%n",xyz[0],xyz[1],xyz[2]);
                System.out.printf("xyz_soft %f %f %f%n",x_soft,y_soft,z_soft);
                System.out.printf("q110 estimate %f%n", cov_ij+pipj-q111);
                System.out.printf("q101 estimate %f%n", cov_ik+pipk-q111);
                System.out.printf("q011 estimate %f%n", cov_jk+pjpk-q111);

            }

            //todo is there a better way??
            if (q[1][1][0]<0){
                q[1][1][0] = 0;
            }
            if (q[1][0][1]<0){
                q[1][0][1] = 0;
            }
            if (q[0][1][1]<0){
                q[0][1][1] = 0;
            }
            //todo improve bound!!!!!!
            if (q[1][1][0]>1-q111){
                q[1][1][0] = 1-q111;
            }
            if (q[1][0][1]>1-q111){
                q[1][0][1] = 1-q111;
            }
            if (q[0][1][1]>1-q111){
                q[0][1][1] = 1-q111;
            }



            q[1][0][0] = p_i - (q[1][1][1] + q[1][1][0] + q[1][0][1]);
            q[0][1][0] = p_j - (q[1][1][1] + q[1][1][0] + q[0][1][1]);
            q[0][0][1] = p_k - (q[1][1][1] + q[1][0][1] + q[0][1][1]);


            if(anyNAN(q)){
                System.out.println("stop2.1");
//            System.out.println(fileString);
                System.out.printf("probs %f %f %f%n", p_i, p_j, p_k);

                for (int i = 0; i < q.length; i++) {
                    final double[][] level2 = q[i];
                    for (int j = 0; j < level2.length; j++) {
                        final double[] level3 = level2[j];
                        for (int k = 0; k < level3.length; k++) {
                            System.out.printf("q%d%d%d = %f%n",i,j,k,q[i][j][k]);
                        }

                    }
                }

            }

            if (q[1][0][0]<0){
//            System.out.println("lowerbound2 "+q[1][0][0]);
                q[1][0][0] = 0;
            }
            if (q[0][1][0]<0){
//            System.out.println("lowerbound2 "+q[0][1][0]);
                q[0][1][0] = 0;
            }
            if (q[0][0][1]<0){
//            System.out.println("lowerbound2 "+q[0][0][1]);
                q[0][0][1] = 0;
            }

            if(anyNAN(q)){
                System.out.println("stop2.2");
            }

            //todo to changed
//        if (q[1][0][0]>1-q111-q[1][1][0]-q[1][0][1]-q[0][1][1]){
            if (q[1][0][0]>1){
                q[1][0][0] = 1;
                System.out.println("upperbound2");
            }
            if (q[0][1][0]>1){
                q[0][1][0] = 1;
                System.out.println("upperbound2");
            }
            if (q[0][0][1]>1){
                q[0][0][1] = 1;
                System.out.println("upperbound2");
            }

            if(anyNAN(q)){
                System.out.println("stop3");
            }


            q[0][0][0] = 1 - (q[1][1][1]+q[1][1][0]+q[1][0][1]+q[1][0][0]+q[0][1][1]+q[0][1][0]+q[0][0][1]);

            if (q[0][0][0]<0) {
                q[0][0][0] = 0;
            }

            if(!allPositive(q)){
                throw new RuntimeException("estimation produced negative probability");
            }
            if(!allBelow1(q)){
                throw new RuntimeException("estimation produced probability greater 1");
            }
            if(anyNAN(q)){
                for (int i = 0; i < q.length; i++) {
                    final double[][] level2 = q[i];
                    for (int j = 0; j < level2.length; j++) {
                        final double[] level3 = level2[j];
                        for (int k = 0; k < level3.length; k++) {
                            System.out.printf("q%d%d%d = %f%n",i,j,k,q[i][j][k]);
                        }

                    }
                }

                throw new RuntimeException("estimation produced NaN");
            }

            addPseudoAndRenormalize(q, pseudoCount);

            //changed already normalize against parent
            return normalizeOverFirstDim(q);
        }


    }

    private static double[][][] normalizeOverFirstDim(double[][][] q){
        double[][][] new_q = new double[2][2][2];
        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 2; k++) {
                double sum = q[0][j][k]+q[1][j][k];
                new_q[0][j][k] = q[0][j][k]/sum;
                new_q[1][j][k] = q[1][j][k]/sum;
            }
        }
        return new_q;
    }

    private static void addPseudoAndRenormalize(double[][][] q, double pseudoCount){
        //todo this only works if already sum 1; rather normalize completely?
        int numOfEntries = q.length*q[0].length*q[0][0].length;
        double norm = 1d+numOfEntries*pseudoCount;
        for (int i = 0; i < q.length; i++) {
            final double[][] level2 = q[i];
            for (int j = 0; j < level2.length; j++) {
                final double[] level3 = level2[j];
                for (int k = 0; k < level3.length; k++) {
                    level3[k] = (level3[k]+pseudoCount)/norm;
                }
            }
        }
    }

    private static boolean allPositive(double[][][] array){
        for (int i = 0; i < array.length; i++) {
            final double[][] level2 = array[i];
            for (int j = 0; j < level2.length; j++) {
                final double[] level3 = level2[j];
                if  (!allPositive(level3)) return false;
            }
        }
        return true;
    }


    private static boolean anyNAN(double[][][] array){
        for (int i = 0; i < array.length; i++) {
            final double[][] level2 = array[i];
            for (int j = 0; j < level2.length; j++) {
                final double[] level3 = level2[j];
                if (anyNAN(level3)) return true;
            }
        }
        return false;
    }

    private static boolean allPositive(double[] array){
        for (int k = 0; k < array.length; k++) {
            if (array[k]<0) return false;
        }
        return true;
    }

    private static boolean allBelow1(double[][][] array){
        for (int i = 0; i < array.length; i++) {
            final double[][] level2 = array[i];
            for (int j = 0; j < level2.length; j++) {
                final double[] level3 = level2[j];
                if (!allBelow1(level3)) return false;
            }
        }
        return true;
    }

    private static boolean allBelow1(double[] array){
        for (int k = 0; k < array.length; k++) {
            if (array[k]>1d) return false;
        }
        return true;
    }

    private static boolean anyNAN(double[] array){
        for (int k = 0; k < array.length; k++) {
            if (Double.isNaN(array[k])) return true;
        }
        return false;
    }




    private static boolean allZero(double[] array){
        for (double d : array) {
            if (d!=0d) return false;
        }
        return true;
    }

    private static boolean satisfyConstraints2(double[] xyz, boolean[] leaveOut_xyz, double p_i, double p_j, double p_k, double q111){
        double[] initial_xyz = Arrays.copyOf(xyz, xyz.length);
        if (anyNAN(xyz)){
            System.out.println("input NaN");
            System.out.printf("%f %f %f%n",xyz[0],xyz[1],xyz[2]);
        }

        double[] new_value;
        double twice_pi_pj_pk_2q111;
        double xyz_sum = -1d;
        int round = 0;
        boolean allViolated = false;
        do {
            ++round;


            boolean x_violated_lb = false, y_violated_lb = false, z_violated_lb = false;
            boolean x_violated_ub = false, y_violated_ub = false, z_violated_ub = false;
            //hard constraints
            new_value = new double[1];
            if (!satisfiesHardConstraint(xyz[0], p_i, q111, new_value)) {
                if (new_value[0]<xyz[0]) x_violated_ub = true;
                else x_violated_lb = true;
                if (xyz[0]==new_value[0]) throw new RuntimeException("error, nothing changed");
                xyz[0] = new_value[0];
            }
            if (!satisfiesHardConstraint(xyz[1], p_j, q111, new_value)) {
                if (new_value[0]<xyz[1]) y_violated_ub = true;
                else y_violated_lb = true;
                if (xyz[1]==new_value[0]) throw new RuntimeException("error, nothing changed");
                xyz[1] = new_value[0];
            }
            if (!satisfiesHardConstraint(xyz[2], p_k, q111, new_value)) {
                if (new_value[0]<xyz[2]) z_violated_ub = true;
                else y_violated_lb = true;
                if (xyz[2]==new_value[0]) throw new RuntimeException("error, nothing changed");
                xyz[2] = new_value[0];
            }


            if(round==10) return false;

            double sum_lb = 0; double fix_lb = 0;
            double sum_ub = 0; double fix_ub = 0;

            if (x_violated_lb) fix_lb += xyz[0];
            else sum_lb += xyz[0];
            if (y_violated_lb) fix_lb += xyz[1];
            else sum_lb += xyz[1];
            if (z_violated_lb) fix_lb += xyz[2];
            else sum_lb += xyz[2];

            if (x_violated_ub) fix_ub += xyz[0];
            else sum_ub += xyz[0];
            if (y_violated_ub) fix_ub += xyz[1];
            else sum_ub += xyz[1];
            if (z_violated_ub) fix_ub += xyz[2];
            else sum_ub += xyz[2];


            twice_pi_pj_pk_2q111 = 2*(p_i+p_j+p_k+2*q111);
            if (sum_lb + fix_lb < twice_pi_pj_pk_2q111-2){
                if (sum_lb==0d){
//                    System.out.println("all violated");
//                    sum_lb = fix_lb;
//                    fix_lb = 0;
//                    allViolated = true;
                    if (allZero(xyz)) return false;
                    continue;
                }
                double multiplier = (twice_pi_pj_pk_2q111-2-fix_lb)/sum_lb;
                if (!x_violated_lb) xyz[0] *= multiplier;
                if (!y_violated_lb) xyz[1] *= multiplier;
                if (!z_violated_lb) xyz[2] *= multiplier;
            } else if (sum_ub + fix_ub > twice_pi_pj_pk_2q111) {
                if (sum_ub==0d){
//                    System.out.println("all violated2");
                    sum_ub = fix_ub;
                    fix_ub = 0;
//                    allViolated = true;
                    if (allZero(xyz)) return false;
                    continue;
                }
                double multiplier = (twice_pi_pj_pk_2q111 - fix_ub) / sum_ub;
                if (!x_violated_ub) xyz[0] *= multiplier;
                if (!y_violated_ub) xyz[1] *= multiplier;
                if (!z_violated_ub) xyz[2] *= multiplier;
            }

            xyz_sum = sum(xyz);
        } while (!(satisfiesHardConstraint(xyz[0],p_i, q111, new_value) &&
                satisfiesHardConstraint(xyz[1],p_j, q111, new_value) &&
                satisfiesHardConstraint(xyz[2],p_k, q111, new_value) &&
                xyz_sum >= (twice_pi_pj_pk_2q111-2) &&
                xyz_sum <= (twice_pi_pj_pk_2q111) ));

        if (allViolated){
            System.out.println("all violated still worked out");
        }

        if (anyNAN(xyz)){
            System.out.println("produced NaN");
            System.out.printf("%f %f %f%n",xyz[0],xyz[1],xyz[2]);
        }
        if (!allBelow1(xyz)){
            System.out.println("too big");
            System.out.printf("%f %f %f%n",xyz[0],xyz[1],xyz[2]);
        }
        if (!allPositive(xyz)){
            System.out.println("negative");
            System.out.printf("%f %f %f%n",xyz[0],xyz[1],xyz[2]);
        }
//        System.out.println("round "+round);
        return true;
    }

    private static boolean satisfyConstraints(double[] xyz, boolean[] leaveOut_xyz, double p_i, double p_j, double p_k, double q111, boolean firstRound){
        boolean x_violated = false, y_violated = false, z_violated = false;
        //hard constraints
        double[] new_value = new double[1];
        if (!leaveOut_xyz[0]) {
            if (!satisfiesHardConstraint(xyz[0], p_i, q111, new_value)) {
                xyz[0] = new_value[0];
                x_violated = true;
            }
        }
        if (!leaveOut_xyz[1]) {
            if (!satisfiesHardConstraint(xyz[1], p_j, q111, new_value)) {
                xyz[1] = new_value[0];
                y_violated = true;
            }
        }
        if (!leaveOut_xyz[2]) {
            if (!satisfiesHardConstraint(xyz[2], p_k, q111, new_value)) {
                xyz[2] = new_value[0];
                z_violated = true;
            }
        }

        if (!firstRound){
            leaveOut_xyz[0] = leaveOut_xyz[0] || x_violated;
            leaveOut_xyz[1] = leaveOut_xyz[1] || y_violated;
            leaveOut_xyz[2] = leaveOut_xyz[2] || z_violated;
        }

        double sum_xyz = sum(xyz);
        double twice_pi_pj_pk_2q111 = 2*(p_i+p_j+p_k+2*q111);
        double multiplier = 1.0;
        if (sum_xyz > twice_pi_pj_pk_2q111){
            if (!any(leaveOut_xyz)) return false; //didn't satisfy constraints
            multiplier = findMultiplier(xyz, leaveOut_xyz, twice_pi_pj_pk_2q111, sum_xyz);
        } else if (sum_xyz < twice_pi_pj_pk_2q111-2){
            if (!any(leaveOut_xyz)) return false; //didn't satisfy constraints
            multiplier = findMultiplier(xyz, leaveOut_xyz, twice_pi_pj_pk_2q111-2, sum_xyz);
        }
        if (multiplier!=1.0) {
            //todo test inf of zero?
            for (int i = 0; i < xyz.length; i++) {
                if (!leaveOut_xyz[i]) xyz[i] = multiplier*xyz[i];
            }
            return satisfyConstraints(xyz, leaveOut_xyz, p_i, p_j, p_k, q111, false);

        } else {
            //satisfied
            return true;
        }
    }

    private static double findMultiplier(double[] xyz, boolean[] leaveOut_xyz, double bound, double sum_xyz) {
        double fixedValue = 0;
        for (int i = 0; i < xyz.length; i++) {
            if (leaveOut_xyz[i]) fixedValue += xyz[i];
        }
        return  (bound-fixedValue)/(sum_xyz-fixedValue);
    }

    private static  double sum(double[] arr){
        double s = 0;
        for (double v : arr) {
            s += v;
        }
        return s;
    }

    private static boolean any(boolean[] arr){
        for (boolean b : arr) {
            if (b) return true;
        }
        return false;
    }

    private static boolean satisfiesHardConstraint(double w, double p, double q, double[] new_w) {
        if (w > p - q){
            new_w[0] = p - q;
            return false;
        } else if (w < 0){
            new_w[0] = 0;
            return false;
        }
        return true;
    }




}
