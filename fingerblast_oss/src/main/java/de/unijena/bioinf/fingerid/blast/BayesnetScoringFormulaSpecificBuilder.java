package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static de.unijena.bioinf.fingerid.blast.BayesnetScoring.*;

public class BayesnetScoringFormulaSpecificBuilder extends BayesnetScoringBuilder {

    //todo change to non-static to override methods?
    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, Path dotFilePath) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringFormulaSpecificBuilder(performances, predictedSpecific, correctSpecific, predictedAll, correctAll, covTreeEdges, false, 100).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, Path dotFilePath, double generalDataWeight) throws IOException {
        int[][] covTreeEdges = parseTreeFromDotFile(dotFilePath);
        return new BayesnetScoringFormulaSpecificBuilder(performances, predictedSpecific, correctSpecific, predictedAll, correctAll, covTreeEdges, false, generalDataWeight).buildScoring();
    }

    public static BayesnetScoring createScoringMethod(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, int[][] covTreeEdges, boolean allowOnlyNegativeScores, double generalDataWeight) {
        return new BayesnetScoringFormulaSpecificBuilder(performances, predictedSpecific, correctSpecific, predictedAll, correctAll, covTreeEdges, false, generalDataWeight).buildScoring();
    }


    protected ProbabilityFingerprint[] predictedSpecific;
    protected Fingerprint[] correctSpecific;
    protected double generalDataWeight;
//        private static TSynchronizedIntObjectMap<double[][][]> indexToContingencyTableCommonPart; //cannot save computation. tree structure is different!!!!

    public BayesnetScoringFormulaSpecificBuilder(PredictionPerformance[] performances, ProbabilityFingerprint[] predictedSpecific, Fingerprint[] correctSpecific, ProbabilityFingerprint[] predictedAll, Fingerprint[] correctAll, int[][] covTreeEdges, boolean allowOnlyNegativeScores, double generalDataWeight) {
        super(performances, predictedAll, correctAll, covTreeEdges, allowOnlyNegativeScores);
        this.predictedSpecific = predictedSpecific;
        this.correctSpecific = correctSpecific;
        this.generalDataWeight = generalDataWeight;
//            if (indexToContingencyTableCommonPart==null) init();
    }


    @Override
    protected void makeStatistics(AbstractCorrelationTreeNode[] nodeList, double alpha) {
        //todo currently only supports one parent case
        Map<AbstractCorrelationTreeNode, double[][][]> nodeToPlattContingencyTables = new HashMap<>();

        //changed final double pseudo = 0.5d;
        final double pseudo = 1d;
        for (AbstractCorrelationTreeNode node : nodeList) {
            final boolean isRoot = node.numberOfParents()==0;
            if (isRoot) continue;
//                if (indexToContingencyTableCommonPart.containsKey(node.getFingerprintIndex())) continue; //we computed the "all" part already
            //just one-parent case
            double[][][] plattContingencyTables = new double[4][2][2];
            nodeToPlattContingencyTables.put(node, plattContingencyTables);

            for (int i = 0; i < plattContingencyTables.length; i++) {
                double[][] a = plattContingencyTables[i];
                for (int j = 0; j < a.length; j++) {
                    double[] b = a[j];
                    for (int k = 0; k < b.length; k++) {
                        b[k] = pseudo;

                    }
                }
            }
        }


        for (int i = 0; i < predicted.length; i++) {
            double[] probFp = predicted[i].toProbabilityArray();
            boolean[] reality = correct[i].toBooleanArray();

            for (AbstractCorrelationTreeNode node : nodeList) {
                final boolean isRoot = node.numberOfParents()==0;
                if (isRoot) continue;
//                    if (indexToContingencyTableCommonPart.containsKey(node.getFingerprintIndex())) continue; //we computed the "all" part already

                AbstractCorrelationTreeNode[] parents = node.getParents();

                double[] parentsPredictions = getParentPredictions(parents, probFp, reality, performances, alpha);
                boolean[] parentsTruth = getParentTruth(parents, reality);
                final boolean truth = reality[node.getFingerprintIndex()];
                final double prediction = laplaceSmoothing(probFp[node.getFingerprintIndex()], alpha);

                double[][][] plattContingencyTables = nodeToPlattContingencyTables.get(node);

                int idx = node.getArrayIdxForGivenAssignment(truth, parentsTruth);

                final double parentPrediction = parentsPredictions[0];
                final double oneMinusPrediction = (1d-prediction);
                final double oneMinusParentPrediction = (1d-parentPrediction);
                //todo put these finals somewhere else
                plattContingencyTables[idx][ChildT][RootT] += prediction*parentPrediction;
                plattContingencyTables[idx][ChildT][RootF] += prediction*oneMinusParentPrediction;
                plattContingencyTables[idx][ChildF][RootT] += oneMinusPrediction*parentPrediction;
                plattContingencyTables[idx][ChildF][RootF] += oneMinusPrediction*oneMinusParentPrediction;

            }

        }


//            //changed test
//            TDoubleArrayList sums = new TDoubleArrayList();
//            boolean[] correctBool = null;
//            if (correctSpecific.length==1){
//                correctBool = correctSpecific[0].toBooleanArray();
//            }

        //all predictions are worth xxx
        double allPredictionsWeight = generalDataWeight + pseudo*4;
        for (AbstractCorrelationTreeNode node : nodeList) {
            final boolean isRoot = node.numberOfParents()==0;
            if (isRoot) continue;
//                if (indexToContingencyTableCommonPart.containsKey(node.getFingerprintIndex())) continue; //we computed the "all" part already
            //just one-parent case
            double[][][] plattContingencyTables = nodeToPlattContingencyTables.get(node);

//                //changed test: distribution if just 1 specific strucutre
//                if (correctSpecific.length==1){
//                    AbstractCorrelationTreeNode[] parents = node.getParents();
//                    boolean[] parentsTruth = getParentTruth(parents, correctBool);
//                    final boolean truth = correctBool[node.getFingerprintIndex()];
//                    int idx = node.getArrayIdxForGivenAssignment(truth, parentsTruth);
//                    double[][] plattContingencyTable = plattContingencyTables[idx];
//                    double sum = plattContingencyTable[ChildT][RootT] + plattContingencyTable[ChildT][RootF] + plattContingencyTable[ChildF][RootT] + plattContingencyTable[ChildF][RootF];
//                    sums.add(Math.round(100*sum)/100d); //                    ../less than thres testing!!!
//                }


            for (int i = 0; i < plattContingencyTables.length; i++) {
                double[][] plattContingencyTable = plattContingencyTables[i];
                double sum = plattContingencyTable[ChildT][RootT] + plattContingencyTable[ChildT][RootF] + plattContingencyTable[ChildF][RootT] + plattContingencyTable[ChildF][RootF];

                //changed test
//                    sums.add(Math.round(100*sum)/100d); //                    ../less than thres testing!!!
//                   ///// changed don't skip!
                if (sum<allPredictionsWeight) continue; //we don't want to upscale common instances


                double norm = sum/(allPredictionsWeight);
                plattContingencyTable[ChildT][RootT] /= norm;
                plattContingencyTable[ChildT][RootF] /= norm;
                plattContingencyTable[ChildF][RootT] /= norm;
                plattContingencyTable[ChildF][RootF] /= norm;
            }
        }

//            if ( correctSpecific.length==1) System.out.println("sums (1 correct): "+ Arrays.toString(sums.toArray()));

//
//                for (AbstractCorrelationTreeNode node : nodeList) {
//                    final boolean isRoot = node.numberOfParents()==0;
//                    if (isRoot) continue;
//                    if (indexToContingencyTableCommonPart.containsKey(node.getFingerprintIndex())){
//                        double[][][] plattContingencyTables = cloneArray(indexToContingencyTableCommonPart.get(node.getFingerprintIndex()));
//                        nodeToPlattContingencyTables.put(node, plattContingencyTables);
//                    } else {
//                        synchronized (indexToContingencyTableCommonPart) {
//                            if (indexToContingencyTableCommonPart.containsKey(node.getFingerprintIndex())) continue;
//                            double[][][] plattContingencyTables = cloneArray(nodeToPlattContingencyTables.get(node));
//                            indexToContingencyTableCommonPart.put(node.getFingerprintIndex(), plattContingencyTables);
//                        }
//
//                    }
//                }


        for (int i = 0; i < predictedSpecific.length; i++) {
            double[] probFp = predictedSpecific[i].toProbabilityArray();
            boolean[] reality = correctSpecific[i].toBooleanArray();

            for (AbstractCorrelationTreeNode node : nodeList) {
                final boolean isRoot = node.numberOfParents()==0;
                if (isRoot) continue;

                AbstractCorrelationTreeNode[] parents = node.getParents();

                double[] parentsPredictions = getParentPredictions(parents, probFp, reality, performances, alpha);
                boolean[] parentsTruth = getParentTruth(parents, reality);
                final boolean truth = reality[node.getFingerprintIndex()];
                final double prediction = laplaceSmoothing(probFp[node.getFingerprintIndex()], alpha);

                double[][][] plattContingencyTables = nodeToPlattContingencyTables.get(node);

                int idx = node.getArrayIdxForGivenAssignment(truth, parentsTruth);

                final double parentPrediction = parentsPredictions[0];
                final double oneMinusPrediction = (1d-prediction);
                final double oneMinusParentPrediction = (1d-parentPrediction);
                plattContingencyTables[idx][ChildT][RootT] += prediction*parentPrediction;
                plattContingencyTables[idx][ChildT][RootF] += prediction*oneMinusParentPrediction;
                plattContingencyTables[idx][ChildF][RootT] += oneMinusPrediction*parentPrediction;
                plattContingencyTables[idx][ChildF][RootF] += oneMinusPrediction*oneMinusParentPrediction;

            }

        }



        //normalize

        for (int i = 0; i < predictedSpecific.length; i++) {
            for (AbstractCorrelationTreeNode node : nodeList) {
                final boolean isRoot = node.numberOfParents()==0;
                if (isRoot) continue;
                double[][][] plattContingencyTables = nodeToPlattContingencyTables.get(node);

                double[] covariances = new double[plattContingencyTables.length];
                for (int j = 0; j < plattContingencyTables.length; j++) {
                    double[][] plattContingencyTable = plattContingencyTables[j];

                    double sum = plattContingencyTable[ChildT][RootT] + plattContingencyTable[ChildT][RootF] + plattContingencyTable[ChildF][RootT] + plattContingencyTable[ChildF][RootF];
                    plattContingencyTable[ChildT][RootT] /= sum;
                    plattContingencyTable[ChildT][RootF] /= sum;
                    plattContingencyTable[ChildF][RootT] /= sum;
                    plattContingencyTable[ChildF][RootF] /= sum;



                    if (node instanceof BayesnetScoringCorrelation.CorrelationTreeNodeCorrelation) {
//                        System.out.println("compute new correlation MF specific");
                        //compute correlation, not covariance
                         final double q11 = plattContingencyTable[ChildT][RootT];
                        final double q01 = plattContingencyTable[ChildF][RootT];
                        final double q10 = plattContingencyTable[ChildT][RootF];
                        final double q00 = plattContingencyTable[ChildF][RootF];

                        covariances[j] = ((q11 * q00) + (q10 * q01)) / Math.sqrt((q11 + q10) * (q11 + q01) * (q00 + q10) * (q00 + q01));
                    } else {
                        //todo compute inside treenode?
                        //todo this has not n-1 correction;
                        covariances[j] = plattContingencyTable[ChildT][RootT] - ((plattContingencyTable[ChildT][RootT]+plattContingencyTable[ChildT][RootF])*(plattContingencyTable[ChildT][RootT]+plattContingencyTable[ChildF][RootT]));
                    }
                }
                setCovariance(node, covariances);

            }

        }
    }

    private static double[][][] cloneArray(double[][][] a){
        double[][][] b = new double[a.length][][];
        for (int i = 0; i < a.length; i++) {
            double[][] doubles = a[i];
            b[i] = new double[a[i].length][];
            for (int j = 0; j < doubles.length; j++) {
                double[] aDouble = doubles[j];
                b[i][j] = aDouble.clone();
            }
        }
        return b;
    }
}
