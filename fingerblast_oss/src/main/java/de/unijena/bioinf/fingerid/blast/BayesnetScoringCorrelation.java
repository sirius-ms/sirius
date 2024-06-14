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

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;

public class BayesnetScoringCorrelation extends BayesnetScoring {

    private static final Logger Log = LoggerFactory.getLogger(BayesnetScoringCorrelation.class);


    protected BayesnetScoringCorrelation(TIntObjectHashMap<AbstractCorrelationTreeNode> nodes, AbstractCorrelationTreeNode[] nodeList, AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores) {
        super(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores);
    }

    public BayesnetScoringCorrelation(TIntObjectHashMap<AbstractCorrelationTreeNode> nodes, AbstractCorrelationTreeNode[] nodeList, AbstractCorrelationTreeNode[] forests, double alpha, FingerprintVersion fpVersion, PredictionPerformance[] performances, boolean allowOnlyNegativeScores, @Nullable FingerprintStatistics statistics) {
        super(nodes, nodeList, forests, alpha, fpVersion, performances, allowOnlyNegativeScores, statistics);
    }

    protected static class CorrelationTreeNodeCorrelation extends CorrelationTreeNode{
        /**
         * constructor for root
         * @param fingerprintIndex
         */
        public CorrelationTreeNodeCorrelation(int fingerprintIndex) {
            this(fingerprintIndex,  null);
        }

        public CorrelationTreeNodeCorrelation(int fingerprintIndex, AbstractCorrelationTreeNode parent){
            super(fingerprintIndex, parent);
        }


        @Override
        void computeCovariance(){
//            System.out.println("now with correlation");//todo this is not used for formula specific scoring!!
            //use correlation instead
            covariances[getArrayIdxForGivenAssignment(true, true)] = Statistics.pearson(plattByRef[getIdxThisPlatt(true, true)].toArray(), plattByRef[getIdxRootPlatt(true, true)].toArray());
            covariances[getArrayIdxForGivenAssignment(false, true)] = Statistics.pearson(plattByRef[getIdxThisPlatt(false, true)].toArray(), plattByRef[getIdxRootPlatt(false, true)].toArray());
            covariances[getArrayIdxForGivenAssignment(true, false)] = Statistics.pearson(plattByRef[getIdxThisPlatt(true, false)].toArray(), plattByRef[getIdxRootPlatt(true, false)].toArray());
            covariances[getArrayIdxForGivenAssignment(false, false)] = Statistics.pearson(plattByRef[getIdxThisPlatt(false, false)].toArray(), plattByRef[getIdxRootPlatt(false, false)].toArray());
            initPlattByRef();//remove oldPlatt
        }
    }


    @Override
    public CorrelationScorer getScoring() {
        return new CorrelationScorer();
    }

    @Override
    public CorrelationScorer getScoring(PredictionPerformance[] performances) {
        return new CorrelationScorer();
    }

    public class CorrelationScorer  extends Scorer {

        public CorrelationScorer(){
        }


        @Override
        void prepare(AbstractCorrelationTreeNode x){
            //todo necessary?
            if (x.numberOfParents()==0) return;
            preparedProperties.add(x.getFingerprintIndex());
            if (x instanceof CorrelationTreeNodeCorrelation){
                CorrelationTreeNodeCorrelation v = (CorrelationTreeNodeCorrelation)x;
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
//                        System.out.println("bayes covariance "+j+": "+covariance);
                        double[] abcd = computeABCD(covariance, p_i, p_j);
                        necessaryABCDs[v.getArrayIdxForGivenAssignment(childTrue, parentTrue)] = abcd[(parentTrue ? 0 : 1)+((childTrue ? 0 : 2))];
                        ++numberOfComputedSimpleContingencyTables;
                    }

                }

                abcdMatrixByNodeIdxAndCandidateProperties[j] = necessaryABCDs;

//                for (AbstractCorrelationTreeNode child : v.children) {
//                    prepare(child);
//                }
            } else {
                throw new RuntimeException("unknown scoring structure. not supported");
            }

        }




        @Override
        protected double conditional(boolean[] databaseEntry, AbstractCorrelationTreeNode x) {
            if (x.numberOfParents()==0){
                final int i = x.getFingerprintIndex();
                final boolean real = databaseEntry[i];
                numberOfScoredNodes++;
                if (real){
//                    return Math.log(smoothedPlatt[i]);
                    return Math.log(getProbability(i, true));
                } else {
//                    return Math.log(1d-smoothedPlatt[i]);
                    return Math.log(1d-getProbability(i,false));
                }
            }

            double score;
            if (x instanceof CorrelationTreeNodeCorrelation){
                CorrelationTreeNodeCorrelation v = (CorrelationTreeNodeCorrelation)x;
                final AbstractCorrelationTreeNode u = v.parent;
                final int i = u.getFingerprintIndex();
                final int j = v.getFingerprintIndex();
                final boolean real = databaseEntry[j];
                final boolean realParent = databaseEntry[i];
                final double p_i = getProbability(i, realParent);

                double correspondingEntry = getABCDMatrixEntry(v, real, realParent);


                //changed already normalized
                score = Math.log(correspondingEntry);


                //changed
                if (allowOnlyNegativeScores && score>0){
                    System.out.printf("overestimated: %f for parent: %d and child: %d with predictions %f and %f and cov %f%n", Math.exp(score), (realParent?1:0), (real?1:0), p_i, getProbability(j, real), v.getCovariance(0, real, realParent));
                    score = 0;
                } else if (score>0) {
                    System.out.printf("strange: overestimated: %f for parent: %d and child: %d with predictions %f and %f and cov %f%n", Math.exp(score), (realParent?1:0), (real?1:0), p_i, getProbability(j, real), v.getCovariance(0, real, realParent));
                }

                if (Double.isNaN(score) || Double.isInfinite(score)){
                    System.err.println("NaN score for the following fingerprints:");
                    System.err.println(Arrays.toString(smoothedPlatt));
                    System.err.println(Arrays.toString(databaseEntry));
                    System.err.println("for tree node u (" + u.getFingerprintIndex() + ") -> v (" + v.getFingerprintIndex() + ")");
                    System.err.println("with covariance:");
                    System.err.println(Arrays.toString(v.covariances));
//                System.err.printf(Locale.US, "and a = %f, b = %f, c = %f, d = %f\n", a, b, c, d);
                    System.err.printf(Locale.US, "p_i = %f\n", p_i);
                    System.err.printf(Locale.US, "alpha = %f\n", alpha);
                    throw new RuntimeException("bad score: "+score);
                }

            } else {
                throw new RuntimeException("unknown scoring structure. not supported");

            }

            ++numberOfScoredNodes;
            return score;
        }

        @Override
        protected double[] computeABCD(double correlation, double p_i, double p_j) {
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
//            double a = covariance+pipj;
            //changed to use correlation
//            System.out.println("now scoring with correlation");
            double a = Math.sqrt(pipj * (1-p_i) * (1-p_j))*correlation;
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
//            return new double[]{a,b,c,d};
            //changed already normalize against parent
            double sum1 = a+c;
            double sum2 = b+d;
            return new double[]{a/sum1,b/sum2,c/sum1,d/sum2};
        }



    }
}
