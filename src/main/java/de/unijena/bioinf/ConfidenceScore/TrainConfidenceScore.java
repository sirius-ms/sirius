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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private void trainOnePredictor(ExecutorService executorService, CompoundWithAbstractFP<ProbabilityFingerprint>[] queries, CompoundWithAbstractFP<Fingerprint>[][] rankedCandidates , FeatureCreator featureCreator, int step) throws InterruptedException {
        List<double[]> featureList = new ArrayList();
        TIntArrayList usedInstances = new TIntArrayList();
        for (int i = 0; i < queries.length; i++) {
            CompoundWithAbstractFP<ProbabilityFingerprint> query = queries[i];
            CompoundWithAbstractFP<Fingerprint>[] candidates = rankedCandidates[i];
            if (featureCreator.isCompatible(query, candidates)){
                usedInstances.add(i);
                double[] features = featureCreator.computeFeatures(query, candidates);
                for (int j = 0; j < features.length; j++) {
                    double feature = features[j];
                    if (Double.isNaN(feature)){
                        String name = featureCreator.getFeatureNames()[j];
                        throw new IllegalArgumentException("NaN created by feature "+name);
                    }
                }
                featureList.add(features);
            }

        }
        double[][] featureMatrix = featureList.toArray(new double[0][]);

        Scaler scaler = new Scaler.StandardScaler(featureMatrix);
        double[] sds = ((Scaler.StandardScaler) scaler).getSD();
        for (int i = 0; i < sds.length; i++) {
            double sd = sds[i];
            if (sd==0){
                String name = featureCreator.getFeatureNames()[i];
                System.out.println("Zero standard deviation for feature "+name);
            }

        }
//        Scaler scaler = new Scaler.NoScaler(featureMatrix);

        double[][] scaledMatrix = scaler.scale(featureMatrix);
        for (double[] doubles : scaledMatrix) {
            for (int j = 0; j < doubles.length; j++) {
                double aDouble = doubles[j];
                if (Double.isNaN(aDouble)){
                    String name = featureCreator.getFeatureNames()[j];
                    throw new IllegalArgumentException("NaN after scaling for feature "+name);
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
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature()
                    });
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
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
                    featureCreator = new CombinedFeatureCreator( new FeatureCreator[]{
                            new NumOfCandidatesCounter(),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(),
                            new MedianMeanScoresFeature(),
                            new NormalizedToMedianScores(),
                            new TanimotoSimilarity(1,i),
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

    private void setFeatureCreators(FeatureCreator[] featureCreatorList){
        this.featureCreators = featureCreatorList;
    }


    private void setPriority(int[] priority) {
        this.priority = priority;
    }

}
