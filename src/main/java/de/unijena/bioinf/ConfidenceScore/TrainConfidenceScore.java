package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ConfidenceScore.confidenceScore.*;
import de.unijena.bioinf.ConfidenceScore.svm.LibLinearImpl;
import de.unijena.bioinf.ConfidenceScore.svm.LibSVMImpl;
import de.unijena.bioinf.ConfidenceScore.svm.SVMInterface;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.DatabaseException;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Marcus Ludwig on 08.03.16.
 */
public class TrainConfidenceScore {

    private FeatureCreator[] featureCreators;
    private Scaler scalers[];
    private Predictor[] predictors;
    private int[] priority;
    private final SVMInterface svmInterface;
    private PredictionPerformance[] statistics;

    private final boolean DEBUG = true;
    private final boolean DEBUG_OUT = false;

    private ChemicalDatabase db;

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
        train(executorService, queries, rankedCandidates, statistics, true);
    }


    public void train(ExecutorService executorService, CompoundWithAbstractFP<ProbabilityFingerprint>[] queries, CompoundWithAbstractFP<Fingerprint>[][] rankedCandidates, PredictionPerformance[] statistics, boolean doCrossval) throws InterruptedException {
        if (queries.length!=rankedCandidates.length){
            throw new IllegalArgumentException("query and candidates sizes differ");
        }
        prepare(statistics);

        this.statistics = statistics;
        this.scalers = new Scaler[featureCreators.length];
        this.predictors = new Predictor[featureCreators.length];

        for (int i = 0; i < featureCreators.length; i++) {
            FeatureCreator featureCreator = featureCreators[i];
            trainOnePredictor(executorService, queries, rankedCandidates, featureCreator, i, doCrossval);
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
        trainOnePredictor(executorService, queries, rankedCandidates , featureCreator, step, true);
    }

    private void trainOnePredictor(ExecutorService executorService, CompoundWithAbstractFP<ProbabilityFingerprint>[] queries, CompoundWithAbstractFP<Fingerprint>[][] rankedCandidates , final FeatureCreator featureCreator, int step, boolean doCrossval) throws InterruptedException {
        List<double[]> featureList = new ArrayList();
        TIntArrayList usedInstances = new TIntArrayList();

        ExecutorService executorService2 = Executors.newSingleThreadExecutor();

//        Path out = Paths.get("./scores_"+step);
//        try {
//            BufferedWriter w = new BufferedWriter(new FileWriter(out.toFile()));


        System.out.println("with cutting");
        Random r = new Random();
        int positiveInstances=0, negativeInstances = 0;
        int maxCandidateNumber = featureCreator.getRequiredCandidateSize();
        List<Future<FeatureWithIdx>> futures = new ArrayList<>();
        for (int i = 0; i < queries.length; i++) {
            final int idx = i;
            final CompoundWithAbstractFP<ProbabilityFingerprint> query = queries[i];

//            {
//                for (int j = 0; j < rankedCandidates[i].length; j++){
//                    double score = ((ScoredCandidate)rankedCandidates[i][j]).score;
//                    if (j==rankedCandidates[i].length-1) w.write(score+"\n");
//                    else w.write(score+"\t");
//                }
//            }
            if (featureCreator.isCompatible(query, rankedCandidates[i])){
//                final CompoundWithAbstractFP<Fingerprint>[] candidates = reduceCandidates(rankedCandidates[i], query, maxCandidateNumber, true, false); //changed!!!

                final CompoundWithAbstractFP<Fingerprint>[] candidates;
                if (step==predictors.length-1){
                    candidates = rankedCandidates[i];
                } else {
                    if (rankedCandidates[i].length<=maxCandidateNumber) //just enough candidates to use classifier
                        candidates = rankedCandidates[i];
                    else { //decide whether instance shall be positive or negative, can just become positive if there is a high chance that this can be drawn randomly
                        final int candNum = rankedCandidates[i].length;
                        final int pos = findCorrectIndex(rankedCandidates[i], query);
                        boolean containsCorrectInFront = pos>=0 && probNoOfAChoosen(pos, candNum-pos, maxCandidateNumber)>0.01; //correct contained an their is a change > 10% that no better candidate is drawn
                        boolean nextPositive = containsCorrectInFront&&r.nextDouble()>(1d*positiveInstances/(positiveInstances+negativeInstances));
                        CompoundWithAbstractFP<Fingerprint>[] candidates2;
                        do {
                            candidates2 = reduceCandidates(rankedCandidates[i], query, maxCandidateNumber, nextPositive, false); //changed!!!
//                            System.out.println("next "+nextPositive+" "+(candidates2[0].getInchi().key2D().equals(query.getInchi().key2D())));
                        } while ((candidates2[0].getInchi().key2D().equals(query.getInchi().key2D()))!=nextPositive);
//                        System.out.println("yeah");
                        candidates = candidates2;
                    }

                }

                if (query.getInchi().key2D().equals(candidates[0].getInchi().key2D())) positiveInstances++;
                else negativeInstances++;

                usedInstances.add(i);
                ////////////////////////////////
//                if (DEBUG){
//                    int size = candidates.length;
//                    double[] scores = new double[size];
//                    for (int j = 0; j < candidates.length; j++)
//                        scores[j] = ((ScoredCandidate)candidates[j]).score;
//                    System.out.println("candidates: "+size+" | "+Arrays.toString(scores));
//                }

                //////////////////////////////
                futures.add(executorService2.submit(new Callable<FeatureWithIdx>() {
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
        System.out.println("positiveInstances: "+positiveInstances+" | negativeInstances: "+negativeInstances);

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

//            w.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        //changed
        executorService2.shutdown();

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

        //        Scaler scaler = new Scaler.NoScaler(featureMatrix);
        Scaler scaler = new Scaler.StandardScaler(featureMatrix);
//        Scaler scaler = new Scaler.MinMaxScaler(featureMatrix);
        double[] sds = ((Scaler.StandardScaler) scaler).getSD();
        for (int i = 0; i < sds.length; i++) {
            double sd = sds[i];
            if (sd==0){
                String name = featureCreator.getFeatureNames()[i];
                System.out.println("Zero standard deviation for feature "+name+" in "+featureCreator.getClass().getSimpleName());
            }

        }

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



        if (DEBUG_OUT){
            Path outpath = Paths.get("./featureMatrix_"+step);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(outpath.toFile()));
                for (TrainLinearSVM.Compound compound : compounds) {
                    double[] features = compound.getFeatures();
                    for (int i = 0; i < features.length; i++) {
                        final double feature = features[i];
                        if (i==features.length-1) writer.write(feature+"\n");
                        else writer.write(feature+"\t");
                    }
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


//        TrainLinearSVM trainLinearSVM = new TrainLinearSVM(executorService, compounds, svmInterface);

        Predictor predictor;
        if (doCrossval){
            if (svmInterface instanceof LibSVMImpl){
                TrainLinearSVM trainLinearSVM = new TrainLinearSVM(executorService, compounds, svmInterface, 10, new int[]{-5,5}, SVMInterface.svm_parameter.RBF);
                predictor = trainLinearSVM.trainWithCrossvalidationOptimizeGammaAndDegree(new double[]{1d/64, 1d/32, 1d/16, 1d/8, 1d/4, 1d/2, 1, 2, 4, 6, 8, 16}, new int[]{1});
            } else {
                TrainLinearSVM trainLinearSVM = new TrainLinearSVM(executorService, compounds, svmInterface, 10, new int[]{-5,5}, SVMInterface.svm_parameter.LINEAR);
//                predictor = trainLinearSVM.trainWithCrossvalidation(); //changed
                System.out.println("crossvalidation.");
                predictor = trainLinearSVM.trainWithCrossvalidation();
            }

        } else {
            if (DEBUG){
                System.out.println("anti-crossvalidation");
            }
            if (svmInterface instanceof LibSVMImpl){
                TrainLinearSVM trainLinearSVM = new TrainLinearSVM(executorService, compounds, svmInterface, 10, new int[]{-5,5}, SVMInterface.svm_parameter.RBF);
                predictor = trainLinearSVM.trainAntiCrossvalidation(new double[]{1d/64, 1d/32, 1d/16, 1d/8, 1d/4, 1d/2, 1, 2, 4, 6, 8, 16}, new int[]{1});
            } else {
                TrainLinearSVM trainLinearSVM = new TrainLinearSVM(executorService, compounds, svmInterface, 10, new int[]{-5,5}, SVMInterface.svm_parameter.LINEAR);
                predictor = trainLinearSVM.trainAntiCrossvalidation();
            }

        }


//        Predictor predictor = new LinearSVMPredictor(new double[]{1}, 0);

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

    public Predictor[] getLinearPredictor() {
        return predictors;
    }

    private TrainLinearSVM.Compound createCompound(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates, double[] features){
        byte classification;
        String queryInchiKey2D = query.getInchi().key2D();
//        String candidateInchiKey2D = rankedCandidates[0].getInchi().key2D();
//        if (queryInchiKey2D.equals(candidateInchiKey2D)) classification = 1;
//        else classification = -1;
        if (equal(query, rankedCandidates[0])) classification = 1;
        else  classification = -1;
        TrainLinearSVM.Compound compound = new TrainLinearSVM.Compound(queryInchiKey2D, classification, features);
        return compound;
    }


    private boolean equal(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint> candidate){
        String tanimoto = System.getProperty("confidence.tanimoto.equal");
        if (tanimoto==null){
            return query.getInchi().key2D().equals(candidate.getInchi().key2D());
        } else {
            double minTanimoto = Double.parseDouble(tanimoto);
            FingerprintCandidate fpc = null;
            try {
                if (db==null) db = new ChemicalDatabase();
                List<FingerprintCandidate> list = db.lookupFingerprintsByInchis(Collections.singletonList(query.getInchi().key2D()));
                if (list.size()==0){
                    System.err.println("no fp for query"+query.getInchi().key2D());
                    return  query.getInchi().key2D().equals(candidate.getInchi().key2D());
                }
                FingerprintCandidate fpc1 = list.get(0);

                list = db.lookupFingerprintsByInchis(Collections.singletonList(candidate.getInchi().key2D()));
                if (list.size()==0){
                    System.err.println("no fp for candidate "+candidate.getInchi().key2D());
                    return  query.getInchi().key2D().equals(candidate.getInchi().key2D());
                }
                FingerprintCandidate fpc2 = list.get(0);
                double t = fpc1.getFingerprint().tanimoto(fpc2.getFingerprint());
                return t>=minTanimoto;
            } catch (DatabaseException e) {
                System.err.println("no fp for query"+query.getInchi().key2D());
                return  query.getInchi().key2D().equals(candidate.getInchi().key2D());
//                throw new RuntimeException(e);
            }

        }
    }

    private CompoundWithAbstractFP<Fingerprint>[] reduceCandidates(CompoundWithAbstractFP<Fingerprint>[] candidates, CompoundWithAbstractFP query, int size, boolean keepCorrect, boolean removeCorrect){
        CompoundWithAbstractFP<Fingerprint>[] newList = new CompoundWithAbstractFP[size];
        final Random r = new Random();
        TIntList indizes = new TIntArrayList();
        if (keepCorrect || removeCorrect){
            int idx = findCorrectIndex(candidates, query);
            for (int i = 0; i < candidates.length; i++){
                if (i!=idx)indizes.add(i);
            }
            shuffle(indizes, r);
            if (idx<0 || removeCorrect)
                indizes = indizes.subList(0, size);
            else {
                indizes = indizes.subList(0, size-1);
                indizes.add(idx);
            }


        } else {
            for (int i = 0; i < candidates.length; i++) indizes.add(i);
            shuffle(indizes, r);
            indizes = indizes.subList(0, size);
        }

        indizes.sort();
//        System.out.println(Arrays.toString(indizes.toArray()));
        for (int i = 0; i < newList.length; i++) {
            newList[i] = candidates[indizes.get(i)];
        }
        return newList;
    }

    private void shuffle(TIntList indizes, Random r){
        final int size = indizes.size();
        int tmp;
        for (int i = 0; i < size; i++) {
            int p = r.nextInt(size);
            tmp = indizes.replace(i, indizes.get(p));
            indizes.set(p,tmp);
        }
    }

    private double probNoOfAChoosen(int aCount, int bCount, int drawings){
        if (bCount<drawings) return 0d;
        int sum = aCount+bCount;
        double p = 1d;
        for (int i = 0; i < drawings; i++) {
            p *= ((double)bCount--)/(sum--);
        }
        return p;
    }


    private int findCorrectIndex(CompoundWithAbstractFP<Fingerprint>[] candidates, CompoundWithAbstractFP query){
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].getInchi().key2D().equals(query.getInchi().key2D())) return i;
        }
        return -1;
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

        FeatureCreator featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                new ScoreDifferenceFeatures(1,4),
                new LogarithmScorer(new ScoreFeatures()),
                new LogarithmScorer(new ScoreDifferenceFeatures(1,4)),//needs At least 5 Candidates per Compound!
                new PlattFeatures());

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
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures());
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
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
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

    public static TrainConfidenceScore CSIOnly(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new CSIScoreFeature(),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new CSIScoreFeature(),
                            new CSIScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new CSIScoreFeature(),
                            new CSIScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new CSIScoreFeature(),
                            new CSIScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
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

    public static TrainConfidenceScore NoLogScoresRobust(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new RobustPlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new RobustPlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new RobustPlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new RobustPlattFeatures(),
                            new MolecularFormulaFeature());
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


    public static TrainConfidenceScore NoLogScores(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
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

    public static TrainConfidenceScore NoLogScoresNoMFRobust(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new RobustPlattFeatures());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new RobustPlattFeatures());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new RobustPlattFeatures());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new RobustPlattFeatures());
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

    public static TrainConfidenceScore NoLogScoresNoMF(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new PlattFeatures());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures());
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

    public static TrainConfidenceScore NoLog(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
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

    public static TrainConfidenceScore PlattLogs(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new PlattFeatures(),
                            new LogarithmScorer(new PlattFeatures()),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new PlattFeatures(),
                            new LogarithmScorer(new PlattFeatures()),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new PlattFeatures(),
                            new LogarithmScorer(new PlattFeatures()),
                            new MolecularFormulaFeature());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new PlattFeatures(),
                            new LogarithmScorer(new PlattFeatures()),
                            new MolecularFormulaFeature());
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

    public static TrainConfidenceScore AdvancedMultipleSVMs2(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 3:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 4:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()));
                    break;
                default:
                    return null;
            }
            featureCreators[i] = featureCreator;

        }
        trainConfidenceScore.setFeatureCreators(featureCreators);
        int[] priority = new int[length];
        for (int i = 0; i < priority.length; i++) priority[i] = i+1;
        trainConfidenceScore.setPriority(priority);

        return trainConfidenceScore;
    }


    public static TrainConfidenceScore AdvancedMultipleSVMsLog(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new LogarithmScorer(new PlattFeatures()),
                            new LogarithmScorer(new MolecularFormulaFeature()));
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new LogarithmScorer(new PlattFeatures()),
                            new LogarithmScorer(new MolecularFormulaFeature()));
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new LogarithmScorer(new PlattFeatures()),
                            new LogarithmScorer(new MolecularFormulaFeature()));
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new LogarithmScorer(new PlattFeatures()),
                            new LogarithmScorer(new MolecularFormulaFeature()));
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


    public static TrainConfidenceScore AdvancedMultipleSVMs50(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int[] sizes = new int[]{1,2,3,4,5,10,20,50};
        FeatureCreator[] featureCreators = new FeatureCreator[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            final FeatureCreator featureCreator;
            switch (size){
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 3:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 4:
                case 5:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i-1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i-1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 10:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,4,9),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,4,9)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 20:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,4,9,19),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,4,9,19)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 50:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,4,9,19,49),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,4,9,19,49)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
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

    public static TrainConfidenceScore AdvancedMultipleSVMs10(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int[] sizes = new int[]{1,2,3,4,5,10};
        FeatureCreator[] featureCreators = new FeatureCreator[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            final FeatureCreator featureCreator;
            switch (size){
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 3:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 4:
                case 5:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i-1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i-1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 10:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,4,9),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,4,9)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
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

    public static TrainConfidenceScore MedianMultipleSVMs(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int length = 5;
        FeatureCreator[] featureCreators = new FeatureCreator[length];
        for (int i = 0; i < length; i++) {
            final FeatureCreator featureCreator;
            switch (i){
                case 0:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                default:
                    featureCreator = new CombinedFeatureCreator(new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,i),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,i)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new MedianMeanScoresFeature());
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
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 1:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1));
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1,2));
                    break;
                default:
                    int[] positions = new int[i];
                    for (int j = 0; j < i; j++) positions[j] = j+1;
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
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
//                            new NormalizedToMedianMeanScores(1,i),
                            new DifferentiatingMolecularPropertiesCounter(0.8, -1));
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



    public static TrainConfidenceScore All10(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int[] sizes = new int[]{1,2,3,4,5,10};
        FeatureCreator[] featureCreators = new FeatureCreator[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            final FeatureCreator featureCreator;
            switch (size){
                case 1:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1));
                    break;
                case 3:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1,2));
                    break;
                case 4:
                case 5:
                    int[] positions = new int[size-1];
                    for (int j = 0; j < size-1; j++) positions[j] = j+1;
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
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
                            new DifferentiatingMolecularPropertiesCounter(0.8, -1));
                    break;
                case 10:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
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
                            new DifferentiatingMolecularPropertiesCounter(0.9, 9));
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

    public static TrainConfidenceScore AllLong(boolean useLinearSVM){
        TrainConfidenceScore trainConfidenceScore = new TrainConfidenceScore(useLinearSVM);

        int[] sizes = new int[]{1,2,3,4,5,10,20,50};
        FeatureCreator[] featureCreators = new FeatureCreator[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            final FeatureCreator featureCreator;
            switch (size){
                case 1:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new LogarithmScorer(new ScoreFeatures()),
                            new PlattFeatures(),
                            new MolecularFormulaFeature());
                    break;
                case 2:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1));
                    break;
                case 3:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
                            new LogarithmScorer(new NumOfCandidatesCounter()),
                            new ScoreFeatures(),
                            new ScoreDifferenceFeatures(1,2),
                            new LogarithmScorer(new ScoreFeatures()),
                            new LogarithmScorer(new ScoreDifferenceFeatures(1,2)),//needs At least 5 Candidates per Compound!
                            new PlattFeatures(),
                            new MolecularFormulaFeature(),
                            new TanimotoSimilarity(1,2));
                    break;
                case 4:
                case 5:
                    int[] positions = new int[size-1];
                    for (int j = 0; j < size-1; j++) positions[j] = j+1;
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
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
                            new DifferentiatingMolecularPropertiesCounter(0.8, -1));
                    break;
                case 10:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
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
                            new DifferentiatingMolecularPropertiesCounter(0.9, 9));
                    break;
                case 20:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
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
//                            new TanimotoSimilarityAvg(1,2,3,4),
//                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9),
                            new TanimotoSimilarityAvgToPos(4,9,19),
//                            new NormalizedToMedianMeanScores(1,4,9), //changed stranged
                            new DifferentiatingMolecularPropertiesCounter(0.8, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.9, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 9),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 9));
                    break;
                case 50:
                    featureCreator = new CombinedFeatureCreator(new NumOfCandidatesCounter(),
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
//                            new TanimotoSimilarityAvg(1,2,3,4),
//                            new TanimotoSimilarityAvg(1,2,3,4,5,6,7,8,9),
                            new TanimotoSimilarityAvgToPos(4,9,19,49),
//                            new NormalizedToMedianMeanScores(1,4,9), //changed stranged
                            new DifferentiatingMolecularPropertiesCounter(0.8, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.9, size-1),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.8, 9),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 4),
                            new DifferentiatingMolecularPropertiesCounter(0.9, 9));
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

