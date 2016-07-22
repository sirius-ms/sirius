package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ConfidenceScore.confidenceScore.*;
import de.unijena.bioinf.ConfidenceScore.svm.LibLinearImpl;
import de.unijena.bioinf.ConfidenceScore.svm.LibSVMImpl;
import de.unijena.bioinf.ConfidenceScore.svm.LinearSVMPredictor;
import de.unijena.bioinf.ConfidenceScore.svm.SVMInterface;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by Marcus Ludwig on 08.03.16.
 */
public class TrainConfidenceScore {

    private FeatureCreator[] featureCreators;
    private Scaler scalers[];
    private LinearSVMPredictor[] predictors;
    private int[] priority;
    private final SVMInterface svmInterface;
    private PredictionPerformance[] statistics;

    private final boolean DEBUG = true;

    public TrainConfidenceScore(boolean useLinearSVM){
        if (useLinearSVM) svmInterface = new LibLinearImpl();
        else svmInterface = new LibSVMImpl();
    }

    /**
     * rankedCandidates[i] are the candidates corresponding to query "queries[i]". the candidates for each query must be sorted by score, best to worst !
     * @param executorService
     * @param queries
     * @param rankedCandidates sorted best to worst!
     */
    public void train(ExecutorService executorService, CompoundWithAbstractFP<ProbabilityFingerprint>[] queries, CompoundWithAbstractFP<Fingerprint>[][] rankedCandidates, PredictionPerformance[] statistics) throws InterruptedException {
        if (queries.length!=rankedCandidates.length){
            throw new IllegalArgumentException("query and candidates sizes differ");
        }
        prepare(statistics);

        this.statistics = statistics;
        this.scalers = new Scaler[featureCreators.length];
        this.predictors = new LinearSVMPredictor[featureCreators.length];

        for (int i = 0; i < featureCreators.length; i++) {
            FeatureCreator featureCreator = featureCreators[i];
            trainOnePredictor(executorService, queries, rankedCandidates, featureCreator, i);
        }

    }

    private class FeatureWithIdx {
        private final int idx;
        private final double[] features;
        private FeatureWithIdx(int idx, double[] features){
            this.idx = idx;
            this.features = features;
        }
    }

    private void trainOnePredictor(ExecutorService executorService, CompoundWithAbstractFP<ProbabilityFingerprint>[] queries, CompoundWithAbstractFP<Fingerprint>[][] rankedCandidates , final FeatureCreator featureCreator, int step) throws InterruptedException {
        List<double[]> featureList = new ArrayList();
        TIntArrayList usedInstances = new TIntArrayList();

        List<Future<FeatureWithIdx>> futures = new ArrayList<>();
        for (int i = 0; i < queries.length; i++) {
            final int idx = i;
            final CompoundWithAbstractFP<ProbabilityFingerprint> query = queries[i];
            final CompoundWithAbstractFP<Fingerprint>[] candidates = rankedCandidates[i];
            if (featureCreator.isCompatible(query, candidates)){
                usedInstances.add(i);
                futures.add(executorService.submit(new Callable<FeatureWithIdx>() {
                    @Override
                    public FeatureWithIdx call() throws Exception {
                        double[] features = featureCreator.computeFeatures(query, candidates);
                        for (int j = 0; j < features.length; j++) {
                            double feature = features[j];
                            if (Double.isNaN(feature)){
                                String name = featureCreator.getFeatureNames()[j];
                                throw new IllegalArgumentException("NaN created by feature "+name+" in "+featureCreator.getClass().getSimpleName());
                            }
                        }
                        return new FeatureWithIdx(idx, features);
                    }
                }));

            }

        }

        List<FeatureWithIdx> results = new ArrayList<>();
        for (Future<FeatureWithIdx> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        Collections.sort(results, new Comparator<FeatureWithIdx>() {
            @Override
            public int compare(FeatureWithIdx o1, FeatureWithIdx o2) {
                return Integer.compare(o1.idx, o2.idx);
            }
        });

        for (FeatureWithIdx result : results) {
            featureList.add(result.features);
        }


        if (DEBUG){
            System.out.println("computed features");
        }

//        for (int i = 0; i < queries.length; i++) {
//            CompoundWithAbstractFP<ProbabilityFingerprint> query = queries[i];
//            CompoundWithAbstractFP<Fingerprint>[] candidates = rankedCandidates[i];
//            if (featureCreator.isCompatible(query, candidates)){
//                usedInstances.add(i);
//                double[] features = featureCreator.computeFeatures(query, candidates);
//                for (int j = 0; j < features.length; j++) {
//                    double feature = features[j];
//                    if (Double.isNaN(feature)){
//                        String name = featureCreator.getFeatureNames()[j];
//                        throw new IllegalArgumentException("NaN created by feature "+name+" in "+featureCreator.getClass().getSimpleName());
//                    }
//                }
//                featureList.add(features);
//            }
//        }

        double[][] featureMatrix = featureList.toArray(new double[0][]);

        Scaler scaler = new Scaler.StandardScaler(featureMatrix);
        double[] sds = ((Scaler.StandardScaler) scaler).getSD();
        for (int i = 0; i < sds.length; i++) {
            double sd = sds[i];
            if (sd==0){
                String name = featureCreator.getFeatureNames()[i];
                System.out.println("Zero standard deviation for feature "+name+" in "+featureCreator.getClass().getSimpleName());
            }

        }
//        Scaler scaler = new Scaler.NoScaler(featureMatrix);

        double[][] scaledMatrix = scaler.scale(featureMatrix);
        for (double[] doubles : scaledMatrix) {
            for (int j = 0; j < doubles.length; j++) {
                double aDouble = doubles[j];
                if (Double.isNaN(aDouble)){
                    String name = featureCreator.getFeatureNames()[j];
                    throw new IllegalArgumentException("NaN after scaling for feature "+name+" in "+featureCreator.getClass().getSimpleName());
                }
            }
        }

        //create compound
        List<TrainLinearSVM.Compound> compounds = new ArrayList<>();
        for (int i = 0; i < scaledMatrix.length; i++) {
            double[] features = scaledMatrix[i];
            int pos = usedInstances.get(i);
            CompoundWithAbstractFP<ProbabilityFingerprint> query = queries[pos];
            CompoundWithAbstractFP<Fingerprint>[] candidates = rankedCandidates[pos];
            compounds.add(createCompound(query, candidates, features));
        }


//        TrainLinearSVM trainLinearSVM = new TrainLinearSVM(executorService, compounds, svmInterface);
        TrainLinearSVM trainLinearSVM = new TrainLinearSVM(executorService, compounds, svmInterface, 10, new int[]{-5,5});

        LinearSVMPredictor predictor = trainLinearSVM.trainWithCrossvalidation();

//        LinearSVMPredictor predictor = new LinearSVMPredictor(new double[]{1}, 0);

        if (DEBUG){
            System.out.println("trained predictor");
        }

        this.predictors[step] = predictor;
        this.scalers[step] = scaler;
    }


    public QueryPredictor getPredictors(){
        return new QueryPredictor(getFeatureCreators(),
                getScalers(),
                getLinearPredictor(), priority, statistics);
    }

    public FeatureCreator[] getFeatureCreators() {
        return featureCreators;
    }

    public Scaler[] getScalers() {
        return scalers;
    }

    public LinearSVMPredictor[] getLinearPredictor() {
        return predictors;
    }

    private TrainLinearSVM.Compound createCompound(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates, double[] features){
        byte classification;
        String queryInchi2D = query.getInchi().in2D;
        String candidateInchi2D = rankedCandidates[0].getInchi().in2D;
        if (queryInchi2D.equals(candidateInchi2D)) classification = 1;
        else classification = -1;
        TrainLinearSVM.Compound compound = new TrainLinearSVM.Compound(queryInchi2D, classification, features);
        return compound;
    }

    private void prepare(PredictionPerformance[] statistics){
        for (FeatureCreator featureCreator : featureCreators) {
            featureCreator.prepare(statistics);
        }
    }

    public static TrainConfidenceScore JustScoreFeature(){
        return JustScoreFeature(true);
    }

    public static TrainConfidenceScore JustScoreFeature(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        FeatureCreator featureCreator = new MarvinScoreFeature();

        trainConfidenceScore.setFeatureCreators(new FeatureCreator[]{featureCreator});
        trainConfidenceScore.setPriority(new int[]{1});

        return trainConfidenceScore;
    }

    public static TrainConfidenceScore DefaultFeatures(){
        return DefaultFeatures(true);
    }

    public static TrainConfidenceScore DefaultFeatures(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        FeatureCreator featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                new ScoreFeatures(),
                new ScoreDifferenceFeatures(1,4),
                new LogarithmScorer(new ScoreFeatures()),
                new LogarithmScorer(new ScoreDifferenceFeatures(1,4)),//needs At least 5 Candidates per Compound!
                new PlattFeatures()
        });

        trainConfidenceScore.setFeatureCreators(new FeatureCreator[]{featureCreator});
        trainConfidenceScore.setPriority(new int[]{1});

        return trainConfidenceScore;
    }

    public static TrainConfidenceScore DefaultMultipleSVMs(){
        return DefaultMultipleSVMs(true);
    }

    public static TrainConfidenceScore DefaultMultipleSVMs(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures()
                    });
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures()
                    });
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures()
                    });
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures()
                    });
                    break;
            }
            featureCreators[i] = featureCreator;

        }
        trainConfidenceScore.setFeatureCreators(featureCreators);
        int[] priority = new int[length];
        for (int i = 0; i < priority.length; i++) priority[i] = i+1;
        trainConfidenceScore.setPriority(priority);

        return trainConfidenceScore;
    }

    public static TrainConfidenceScore AdvancedMultipleSVMs(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
            }
            featureCreators[i] = featureCreator;

        }
        trainConfidenceScore.setFeatureCreators(featureCreators);
        int[] priority = new int[length];
        for (int i = 0; i < priority.length; i++) priority[i] = i+1;
        trainConfidenceScore.setPriority(priority);

        return trainConfidenceScore;
    }


    public static TrainConfidenceScore MedianMultipleSVMs(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new MedianMeanScoresFeature()
                    });
                    break;
            }
            featureCreators[i] = featureCreator;

        }
        trainConfidenceScore.setFeatureCreators(featureCreators);
        int[] priority = new int[length];
        for (int i = 0; i < priority.length; i++) priority[i] = i+1;
        trainConfidenceScore.setPriority(priority);

        return trainConfidenceScore;
    }

    public static TrainConfidenceScore All(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1)
                    });
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1,2)
                    });
                    break;
                default:
                    int[] positions = new int[i];
                    for (int j = 0; j < i; j++) positions[j] = j+1;
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new MedianMeanScoresFeature(),
                            new DiffToMedianMeanScores(),
                            new TanimotoSimilarity(positions),
                            new TanimotoSimilarityAvg(positions),
                            new TanimotoSimilarityAvgToPerc(10,20,50),
                            new NormalizedToMedianMeanScores(1,i),
                            new DifferentiatingMolecularPropertiesCounter(0.8, -1)
                    });
                    break;
            }
            featureCreators[i] = featureCreator;

        }
        trainConfidenceScore.setFeatureCreators(featureCreators);
        int[] priority = new int[length];
        for (int i = 0; i < priority.length; i++) priority[i] = i+1;
        trainConfidenceScore.setPriority(priority);

        return trainConfidenceScore;
    }


    public static TrainConfidenceScore AllLong(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int[] sizes = new int[]{1,2,3,4,5,10,20,50};
        FeatureCreator[] featureCreators = new FeatureCreator[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            final FeatureCreator featureCreator;
            switch (size){
                case 1:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1)
                    });
                    break;
                case 3:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1,2)
                    });
                    break;
                case 4:
                case 5:
                    int[] positions = new int[size-1];
                    for (int j = 0; j < size-1; j++) positions[j] = j+1;
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,size-1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,size-1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new MedianMeanScoresFeature(),
                            new DiffToMedianMeanScores(),
                            new TanimotoSimilarity(1,size-1),
                            new TanimotoSimilarityAvg(positions),
                            new TanimotoSimilarityAvgToPerc(10,20,50),
//                            new NormalizedToMedianMeanScores(1,size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.8, -1)
                    });
                    break;
                case 10:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,4,9),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,4,9)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new MedianMeanScoresFeature(),
                            new DiffToMedianMeanScores(),
                            new TanimotoSimilarity(1,4,9),
                            new TanimotoSimilarityAvg(1,2,3,4),
                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9),
//                            new TanimotoSimilarityAvgToFixedLength(4,9),
//                            new NormalizedToMedianMeanScores(1,4,9), //changed stranged
                            new DifferentiatingMolecularPropertiesCounter(0.8, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 9),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 9)
                    });
                    break;
                case 20:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,4,9),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,4,9)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new MedianMeanScoresFeature(),
                            new DiffToMedianMeanScores(),
                            new TanimotoSimilarity(1,4),
                            new TanimotoSimilarityAvg(1,2,3,4),
                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9),
                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19),
//                            new TanimotoSimilarityAvgToPos(4,9,19),
//                            new NormalizedToMedianMeanScores(1,4,9), //changed stranged
                            new DifferentiatingMolecularPropertiesCounter(0.8, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.9, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 9),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 9)
                    });
                    break;
                case 50:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,4,9),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,4,9)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new MedianMeanScoresFeature(),
                            new DiffToMedianMeanScores(),
                            new TanimotoSimilarity(1,4),
                            new TanimotoSimilarityAvg(1,2,3,4),
                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9),
                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19),
                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49),
//                            new TanimotoSimilarityAvgToPos(4,9,19,49),
//                            new NormalizedToMedianMeanScores(1,4,9), //changed stranged
                            new DifferentiatingMolecularPropertiesCounter(0.8, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.9, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 9),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 9)
                    });
                    break;
                default:
                    throw new RuntimeException("unexpected size");
            }
            featureCreators[i] = featureCreator;

        }
        trainConfidenceScore.setFeatureCreators(featureCreators);
        int[] priority = new int[sizes.length];
        for (int i = 0; i < priority.length; i++) priority[i] = i+1;
        trainConfidenceScore.setPriority(priority);

        return trainConfidenceScore;
    }


    private void setFeatureCreators(FeatureCreator[] featureCreatorList){
        this.featureCreators = featureCreatorList;
    }


    private void setPriority(int[] priority) {
        this.priority = priority;
    }

}

