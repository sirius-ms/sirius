package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ConfidenceScore.confidenceScore.*;
import de.unijena.bioinf.ConfidenceScore.svm.LibLinearImpl;
import de.unijena.bioinf.ConfidenceScore.svm.LibSVMImpl;
import de.unijena.bioinf.ConfidenceScore.svm.LinearSVMPredictor;
import de.unijena.bioinf.ConfidenceScore.svm.SVMInterface;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
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
    private FingerprintStatistics statistics;

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
    public void train(ExecutorService executorService, Query[] queries, Candidate[][] rankedCandidates, FingerprintStatistics statistics) throws InterruptedException {
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

    private void trainOnePredictor(ExecutorService executorService, Query[] queries, Candidate[][] rankedCandidates , FeatureCreator featureCreator, int step) throws InterruptedException {
        List<double[]> featureList = new ArrayList();
        TIntArrayList usedInstances = new TIntArrayList();
        for (int i = 0; i < queries.length; i++) {
            Query query = queries[i];
            Candidate[] candidates = rankedCandidates[i];
            if (featureCreator.isCompatible(query, candidates)){
                usedInstances.add(i);
                double[] features = featureCreator.computeFeatures(query, candidates);
                for (double feature : features){
                    if (Double.isNaN(feature)){
                        System.out.println("damn");
                    }
                }
                featureList.add(features);
            }

        }
        double[][] featureMatrix = featureList.toArray(new double[0][]);

        Scaler scaler = new Scaler.StandardScaler(featureMatrix);
//        Scaler scaler = new Scaler.NoScaler(featureMatrix);

        double[][] scaledMatrix = scaler.scale(featureMatrix);
        for (double[] doubles : scaledMatrix) {
            for (double aDouble : doubles) {
                if (Double.isNaN(aDouble)){
                    System.out.println("damn");
                }
            }
        }

        //create compound
        List<TrainLinearSVM.Compound> compounds = new ArrayList<>();
        for (int i = 0; i < scaledMatrix.length; i++) {
            double[] features = scaledMatrix[i];
            int pos = usedInstances.get(i);
            Query query = queries[pos];
            Candidate[] candidates = rankedCandidates[pos];
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

    private TrainLinearSVM.Compound createCompound(Query query, Candidate[] rankedCandidates, double[] features){
        byte classification;
        String queryInchi2D = inchi2d(query.inchi);
        String candidateInchi2D = inchi2d(rankedCandidates[0].inchi);
        if (queryInchi2D.equals(candidateInchi2D)) classification = 1;
        else classification = -1;
        TrainLinearSVM.Compound compound = new TrainLinearSVM.Compound(queryInchi2D, classification, features);
        return compound;
    }

    private void prepare(FingerprintStatistics statistics){
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

    private static Pattern inchi2dPattern = Pattern.compile("/[btmrsfi]");
    private static String inchi2d(String inchi) {
        final Matcher m = inchi2dPattern.matcher(inchi);
        if (m.find()) {
            return inchi.substring(0, m.start());
        } else {
            return inchi;
        }
    }
}
