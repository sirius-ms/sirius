package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ConfidenceScore.confidenceScore.ScoredCandidate;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.DatabaseException;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
* Created by Marcus Ludwig on 09.03.16.
*/
public class EvalConfidenceScore {

    final int MAX_CANDIDATES = -1; //all candidates

    private static final String SEP = "\t";
    final static boolean DEBUG = true;

    private final PredictionPerformance[] statistics;
    protected final HashMap<String, List<CompoundWithAbstractFP<ProbabilityFingerprint>>> queriesPerFormula;
    final CSIFingerIdScoring marvinsScoring;

    private static final Comparator<ScoredCandidate> SCORED_CANDIDATE_COMPARATOR = new ScoredCandidate.MaxBestComparator();

    private MaskedFingerprintVersion maskedFingerprintVersion;
    private ChemicalDatabase db;

    public static void train(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, Path outputFile, boolean useLinearSVM, ChemicalDatabase db) throws IOException, InterruptedException, DatabaseException {
        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.JustScoreFeature(useLinearSVM);
        train(queries, statistics, maskedFingerprintVersion, outputFile, db, trainConfidenceScore);
    }

    public static void train(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, Path outputFile, ChemicalDatabase db, TrainConfidenceScore trainConfidenceScore) throws IOException, InterruptedException, DatabaseException {
        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(queries, statistics, maskedFingerprintVersion, db);

        System.out.println("compute hitlist");

        List<Instance> instances = evalConfidenceScore.computeHitList();

        List<CompoundWithAbstractFP<ProbabilityFingerprint>> queriesX = new ArrayList<>();
        List<CompoundWithAbstractFP<ProbabilityFingerprint>[]> candidatesX = new ArrayList<>();
        for (Instance instance : instances) {
            queriesX.add(instance.query);
            candidatesX.add(instance.candidates.toArray(new CompoundWithAbstractFP[0]));
        }

        System.out.println("train");

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        trainConfidenceScore.train(executorService, queriesX.toArray(new CompoundWithAbstractFP[0]), candidatesX.toArray(new CompoundWithAbstractFP[0][]), statistics);

        QueryPredictor queryPredictor = trainConfidenceScore.getPredictors();



        queryPredictor.absFPIndices = getAbsIndices(maskedFingerprintVersion);
        queryPredictor.writeToFile(outputFile);

        executorService.shutdown();
    }

    private static int[] getAbsIndices(MaskedFingerprintVersion maskedFingerprintVersion){
        int[] absIndices = new int[maskedFingerprintVersion.size()];
        for (int i = 0; i < absIndices.length; i++) {
            absIndices[i] = maskedFingerprintVersion.getAbsoluteIndexOf(i);
        }
        return absIndices;
    }


    /**
     * assumes default CDK Fingerprint version #CdkFingerprintVersion.getDefault()
     *
     * @param queries
     * @param modelFile
     * @param outputFile
     * @param db
     * @throws IOException
     */
    public static void predict(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, Path modelFile, Path outputFile, ChemicalDatabase db) throws IOException, DatabaseException {
        QueryPredictor queryPredictor = QueryPredictor.loadFromFile(modelFile);
        MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(CdkFingerprintVersion.getDefault()).disableAll();
        for (int i : queryPredictor.getAbsFPIndices()) {
            builder.enable(i);
        }
        predict(queries, builder.toMask(), queryPredictor, outputFile, db);
    }

    public static void predict(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, MaskedFingerprintVersion maskedFingerprintVersion, Path modelFile, Path outputFile, ChemicalDatabase db) throws IOException, DatabaseException {
        QueryPredictor queryPredictor = QueryPredictor.loadFromFile(modelFile);
        predict(queries, maskedFingerprintVersion, queryPredictor, outputFile, db);
    }

    public static void predict(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, MaskedFingerprintVersion maskedFingerprintVersion, QueryPredictor queryPredictor, Path outputFile, ChemicalDatabase db) throws IOException, DatabaseException {
        predict(queries, null, maskedFingerprintVersion, queryPredictor, outputFile, db);
    }

    public static void predict(List<CompoundWithAbstractFP<ProbabilityFingerprint>> predictedQueries, List<CompoundWithAbstractFP<ProbabilityFingerprint>> correctQueries, MaskedFingerprintVersion maskedFingerprintVersion, Path modelFile, Path outputFile, ChemicalDatabase db) throws IOException, DatabaseException {
        QueryPredictor queryPredictor = QueryPredictor.loadFromFile(modelFile);
        predict(predictedQueries, correctQueries, maskedFingerprintVersion, queryPredictor, outputFile, db);
    }

    public static void predict(List<CompoundWithAbstractFP<ProbabilityFingerprint>> predictedQueries, List<CompoundWithAbstractFP<ProbabilityFingerprint>> correctQueries, MaskedFingerprintVersion maskedFingerprintVersion, QueryPredictor queryPredictor, Path outputFile, ChemicalDatabase db) throws IOException, DatabaseException {
        if (correctQueries==null) correctQueries = predictedQueries;
        if (predictedQueries.size()!=correctQueries.size()) throw new RuntimeException("predictedQueries and correctQueries size differ");
        Map<CompoundWithAbstractFP, CompoundWithAbstractFP> predictedToCorrect = new HashMap<>();
        for (int i = 0; i < predictedQueries.size(); i++) {
             predictedToCorrect.put(predictedQueries.get(i), correctQueries.get(i));
        }

        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(predictedQueries, queryPredictor.getStatistics(), maskedFingerprintVersion, db);

        System.out.println("compute hitlist");
        List<Instance> instances = evalConfidenceScore.computeHitList();

        TDoubleArrayList platts = new TDoubleArrayList();
        TDoubleArrayList corrects = new TDoubleArrayList();
        List<String> ids = new ArrayList<>();

        System.out.println("predict");
        for (Instance instance : instances) {
            if (instance.candidates.size()==0) continue;
//            boolean isCorrect = (inchi2d(instance.candidates.get(0).getInchi().in2D).equals(inchi2d(instance.query.getInchi().in2D)));
            double isCorrect = precentageCorrect(instance.candidates, predictedToCorrect.get(instance.query));
            double platt = 0;
            try {
                platt = queryPredictor.estimateProbability(instance.query, instance.candidates.toArray(new ScoredCandidate[0]));
//                platt = queryPredictor.score(instance.query, instance.candidates.toArray(new CompoundWithAbstractFP[0])); //changed
            } catch (PredictionException e) {
                continue;
            }

            assert platt>=0;
            assert platt<=1;
            platts.add(platt);
            corrects.add(isCorrect);
            ids.add(instance.query.getInchi().in3D);
        }

        double auc = getAUC(corrects, platts);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
        writer.write("#AUC"+SEP+auc+"\n");
        writer.write("inchi"+SEP+"platt"+SEP+"isCorrect"+"\n");
        for (int i = 0; i < corrects.size(); i++) {
            String id = ids.get(i);
            double c = corrects.get(i);
            double platt = platts.get(i);
            writer.write(id+SEP+platt+SEP+c+"\n");
        }
        writer.close();
    }


    public static void overfit(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, Path outputFile, boolean useLinearSVM, ChemicalDatabase db) throws IOException, InterruptedException, DatabaseException {
        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.JustScoreFeature(useLinearSVM);
        overfit(queries, statistics, maskedFingerprintVersion, outputFile, db, trainConfidenceScore);
    }

    public static void overfit(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, Path outputFile, ChemicalDatabase db, TrainConfidenceScore trainConfidenceScore) throws IOException, InterruptedException, DatabaseException {
        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(queries, statistics, maskedFingerprintVersion, db);

        System.out.println("compute hitlist");

        List<Instance> instances = evalConfidenceScore.computeHitList();

        if (DEBUG){
            System.out.println("candiates per query");
            TIntIntHashMap map = new TIntIntHashMap();
            for (Instance instance : instances) {
                int count = instance.candidates.size();
                map.adjustOrPutValue(count, 1,1);
            }
            map.forEachEntry(new TIntIntProcedure() {
                @Override
                public boolean execute(int a, int b) {
                    System.out.println(a+": "+b);
                    return true;
                }
            });
        }


        List<Instance> instances2 = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.candidates.size()!=0) instances2.add(instance);
        }
        instances = instances2;

        List<CompoundWithAbstractFP<Fingerprint>[]> usedCandidates = new ArrayList<>();
        List<CompoundWithAbstractFP<ProbabilityFingerprint>> usedQueries = new ArrayList<>();
        for (Instance instance : instances) {
            usedCandidates.add(instance.candidates.toArray(new ScoredCandidate[0]));
            usedQueries.add(instance.query);
        }

        System.out.println("train");

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


        TDoubleArrayList platts = new TDoubleArrayList();
        TDoubleArrayList corrects = new TDoubleArrayList();
        List<String> ids = new ArrayList<>();

        TIntIntHashMap map = new TIntIntHashMap();


        System.out.println("train: "+usedQueries.size()+" "+usedCandidates.size());
        System.out.println("test: "+instances.size());

        //train
        trainConfidenceScore.train(executorService, usedQueries.toArray(new CompoundWithAbstractFP[0]), usedCandidates.toArray(new CompoundWithAbstractFP[0][]), statistics);

        QueryPredictor queryPredictor = trainConfidenceScore.getPredictors();
        queryPredictor.absFPIndices = getAbsIndices(maskedFingerprintVersion);


        //test


        System.out.println("predict");
        for (Instance instance : instances) {
            if (instance.candidates.size()==0) continue;




//                boolean isCorrect = (inchi2d(instance.query.getInchi().in2D).equals(inchi2d(instance.candidates.get(0).getInchi().in2D)));
            double isCorrect = precentageCorrect(instance.candidates, instance.query);
            double platt = 0;
            try {
                    platt = queryPredictor.estimateProbability(instance.query, instance.candidates.toArray(new CompoundWithAbstractFP[0]));
//                platt = queryPredictor.score(instance.query, instance.candidates.toArray(new CompoundWithAbstractFP[0])); //changed
            } catch (PredictionException e) {
                System.err.println(e.getMessage());
                continue;
            }


            if (DEBUG){
                if ((isCorrect>=0.5)!=(platt>=0.5)){
                    map.adjustOrPutValue(instance.candidates.size(), 1,1);
                }
            }

            assert platt>=0;
            assert platt<=1;
            platts.add(platt);
            corrects.add(isCorrect);
        }




        if (DEBUG){
            System.out.println("candidates sizes for wrong classification");
            map.forEachEntry(new TIntIntProcedure() {
                @Override
                public boolean execute(int a, int b) {
                    System.out.println(a+": "+b);
                    return true;
                }
            });
        }

        //write output
        double auc = getAUC(corrects, platts);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
        writer.write("#AUC"+SEP+auc+"\n");
        writer.write("inchi"+SEP+"platt"+SEP+"isCorrect"+"\n");
        for (int i = 0; i < corrects.size(); i++) {
            String id = ids.get(i);
            double c = corrects.get(i);
            double platt = platts.get(i);
            writer.write(id+SEP+platt+SEP+c+"\n");
        }
        writer.close();

        executorService.shutdown();
    }

    public static void crossvalidation(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, Path outputFile, boolean useLinearSVM, ChemicalDatabase db) throws IOException, InterruptedException, DatabaseException {
        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.JustScoreFeature(useLinearSVM);
        crossvalidation(queries, statistics, maskedFingerprintVersion, outputFile, db, trainConfidenceScore);
    }


    public static void crossvalidation(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, Path outputFile, ChemicalDatabase db, TrainConfidenceScore trainConfidenceScore) throws IOException, InterruptedException, DatabaseException {
        final int FOLD = 10;
        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(queries, statistics, maskedFingerprintVersion, db);

        System.out.println("compute hitlist");

        List<Instance> instances = evalConfidenceScore.computeHitList();

        if (DEBUG){
            System.out.println("candiates per query");
            TIntIntHashMap map = new TIntIntHashMap();
            for (Instance instance : instances) {
                int count = instance.candidates.size();
                map.adjustOrPutValue(count, 1,1);
            }
            map.forEachEntry(new TIntIntProcedure() {
                @Override
                public boolean execute(int a, int b) {
                    System.out.println(a+": "+b);
                    return true;
                }
            });
        }


        List<Instance> instances2 = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.candidates.size()!=0) instances2.add(instance);
        }
        instances = instances2;

        List<Instance>[] instanceFolds = new ArrayList[FOLD];

        pickupTrainAndEvalStructureDependent(instances, instanceFolds, false, FOLD);

        List<CompoundWithAbstractFP<ProbabilityFingerprint>>[] queryFolds = new ArrayList[FOLD];
        List<CompoundWithAbstractFP<Fingerprint>[]>[] candidateFolds = new ArrayList[FOLD];

        for (int i = 0; i < instanceFolds.length; i++) {
            List<Instance> instanceFold = instanceFolds[i];
            queryFolds[i] = new ArrayList<>();
            candidateFolds[i] = new ArrayList<>();
            for (Instance instance : instanceFold) {
                queryFolds[i].add(instance.query);
                candidateFolds[i].add(instance.candidates.toArray(new ScoredCandidate[0]));
            }
        }

        System.out.println("train");

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


        TDoubleArrayList platts = new TDoubleArrayList();
        TDoubleArrayList corrects = new TDoubleArrayList();
        List<String> ids = new ArrayList<>();

        HashSet<InChI> idSet = new HashSet<>();

        TIntIntHashMap map = new TIntIntHashMap();

        for (int i = 0; i < FOLD; i++) {
            List<CompoundWithAbstractFP<Fingerprint>[]> trainCand = new ArrayList<>();
            for (int j = 0; j < FOLD; j++) {
                if (j!=i) trainCand.addAll(candidateFolds[j]);

            }

            List<CompoundWithAbstractFP<ProbabilityFingerprint>> trainQueries = new ArrayList<>();
            for (int j = 0; j < FOLD; j++) {
                if (j!=i) trainQueries.addAll(queryFolds[j]);

            }


            List<Instance> testInstances = new ArrayList<>(instanceFolds[i]);

            System.out.println("train: "+trainQueries.size()+" "+trainCand.size());
            System.out.println("test: "+testInstances.size());

            //train
            trainConfidenceScore.train(executorService, trainQueries.toArray(new CompoundWithAbstractFP[0]), trainCand.toArray(new CompoundWithAbstractFP[0][]), statistics);

            QueryPredictor queryPredictor = trainConfidenceScore.getPredictors();
            queryPredictor.absFPIndices = getAbsIndices(maskedFingerprintVersion);


            //test

            HashSet<InChI> idSet2 = new HashSet<>();

            System.out.println("predict");
            for (Instance instance : testInstances) {
                if (instance.candidates.size()==0) continue;

                if (DEBUG){
                    if (idSet.contains(instance.query.getInchi())){
                        System.out.println("Already tested query");
                    }
                }


                idSet2.add(instance.query.getInchi());
//                boolean isCorrect = (inchi2d(instance.query.getInchi().in2D).equals(inchi2d(instance.candidates.get(0).getInchi().in2D)));
                double isCorrect = precentageCorrect(instance.candidates, instance.query);
                double platt = 0;
                try {
                    platt = queryPredictor.estimateProbability(instance.query, instance.candidates.toArray(new CompoundWithAbstractFP[0]));
//                    platt = queryPredictor.score(instance.query, instance.candidates.toArray(new CompoundWithAbstractFP[0])); //changed
                } catch (PredictionException e) {
                    System.err.println(e.getMessage());
                    continue;
                }


                if (DEBUG){
                    if ((isCorrect>=0.5)!=(platt>=0.5)){
                        map.adjustOrPutValue(instance.candidates.size(), 1,1);
                    }
                }

                assert platt>=0;
                assert platt<=1;
                platts.add(platt);
                corrects.add(isCorrect);
                ids.add(instance.query.getInchi().in3D);
            }

            idSet = idSet2;
        }


        if (DEBUG){
            System.out.println("candidates sizes for wrong classification");
            map.forEachEntry(new TIntIntProcedure() {
                @Override
                public boolean execute(int a, int b) {
                    System.out.println(a+": "+b);
                    return true;
                }
            });
        }

        //write output
        double auc = getAUC(corrects, platts);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
        writer.write("#AUC"+SEP+auc+"\n");
        writer.write("inchi"+SEP+"platt"+SEP+"isCorrect"+"\n");
        for (int i = 0; i < corrects.size(); i++) {
            String id = ids.get(i);
            double c = corrects.get(i);
            double platt = platts.get(i);
            writer.write(id+SEP+platt+SEP+c+"\n");
        }
        writer.close();

        executorService.shutdown();
    }



private static void pickupTrainAndEvalStructureDependent(List<Instance> compounds, List<Instance>[] bins, boolean removeIdentifierDuplicates, int FOLDS) {
        for (int k=0; k < FOLDS; ++k) {
            bins[k] = new ArrayList<>();
        }

        final int[] foldSizes = new int[FOLDS];
        final int length = compounds.size();
        final int factor = length/FOLDS;
        final int mod = length % FOLDS;

        for (int i = 0; i < foldSizes.length; i++) {
            foldSizes[i] = factor;
            if (i<mod) ++foldSizes[i];
        }

        int[] randomOrder = new int[length];
        int pos = 0;
        for (int i = 0; i < foldSizes.length; i++) {
            int foldSize = foldSizes[i];
            for (int j = 0; j < foldSize; j++) {
                randomOrder[pos++] = i;
            }
        }
        Statistics.shuffle(randomOrder);

        final List<Instance> sortedCompounds = new ArrayList<>(compounds);
        Collections.sort(sortedCompounds, new Comparator<Instance>() {
            @Override
            public int compare(Instance o1, Instance o2) {
                return o1.query.getInchi().in2D.compareTo(o2.query.getInchi().in2D);
            }
        });

        List<Instance>[] split = new ArrayList[FOLDS];
        for (int i = 0; i < split.length; i++) {
            split[i] = new ArrayList<>();
        }


        int shufflePos = 0;
        int[] unbalancedSize = new int[FOLDS];
//        Iterator<Instance> iterator = compounds.iterator();
        Iterator<Instance> iterator = sortedCompounds.iterator();
        Instance next = iterator.next();
        while (next != null) {
            Instance compound = next;
            int randBucket = randomOrder[shufflePos];
            while (unbalancedSize[randBucket]>0) {
                unbalancedSize[randBucket]--;
                shufflePos++;
                if (shufflePos>=randomOrder.length) break;
                randBucket = randomOrder[shufflePos];
            }
            shufflePos++;
//            final String identifier = inchi2d(compound.query.getInchi().in2D);
            final String identifier = compound.query.getInchi().key2D();
            split[randBucket].add(compound);
            if (!iterator.hasNext()) break;
//            while ((iterator.hasNext()) && inchi2d((next = iterator.next()).query.getInchi().in2D).equals(identifier)){
            while ((iterator.hasNext()) && (next = iterator.next()).query.getInchi().key2D().equals(identifier)){
                if (!removeIdentifierDuplicates){
                    unbalancedSize[randBucket]++;
                    split[randBucket].add(next);

                }
            }

        }

        for (int i = 0; i < split.length; i++) {
            List<Instance> compoundList = split[i];
            bins[i].addAll(compoundList);

        }

    }


    private static void writeXY(double[][] data, Path file, String x, String y) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(file, Charset.defaultCharset());
        writer.write(x+SEP+y+"\n");
        for (int i = 0; i < data.length; i++) {
            double[] doubles = data[i];
            writer.write(doubles[0]+SEP+doubles[1]+"\n");
        }
        writer.close();
    }



    public EvalConfidenceScore(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, ChemicalDatabase db) {
        marvinsScoring = new CSIFingerIdScoring(statistics);
        this.statistics = statistics;
        this.maskedFingerprintVersion = maskedFingerprintVersion;

        final HashMap<String, List<CompoundWithAbstractFP<ProbabilityFingerprint>>> map = new HashMap<>();
        for (CompoundWithAbstractFP<ProbabilityFingerprint> c : queries) {
            final String formula = c.getInchi().extractFormula().formatByHill();
            if (!map.containsKey(formula)) map.put(formula, new ArrayList<CompoundWithAbstractFP<ProbabilityFingerprint>>());
            map.get(formula).add(c);
        }
        this.queriesPerFormula = map;

        this.db = db;
    }

    /**
     * parrallel !!!
     * @return
     * @throws IOException
     * @throws DatabaseException
     */
    public List<Instance> computeHitList() throws IOException, DatabaseException {
        List<Instance> instances = new ArrayList<>();
        List<String> queriesPerFormulaList = new ArrayList<>(queriesPerFormula.keySet());


        final int NUM_OF_THREADS = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_THREADS);
        List<ChemicalDatabase> databases = new ArrayList<>();
        for (int i = 0; i < NUM_OF_THREADS; i++) {
            try {
                databases.add(db.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

        }

        if (databases.size()==0){
            System.err.println("cloning DBs didn't work");
            databases.add(db);
        } else {
            System.out.println(databases.size()+" chemicalDBs");
        }

        List<Future<List<Instance>>> futures = new ArrayList<>();
        final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(queriesPerFormulaList);
        for (final ChemicalDatabase database : databases) {
            futures.add(executorService.submit(new Callable<List<Instance>>() {
                @Override
                public List<Instance> call() throws Exception {
                    List<Instance> instanceList = new ArrayList<Instance>();
                    String name;
                    while (!queue.isEmpty()){
                        name = queue.poll();
                        if (name == null) continue;
                        final List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries = queriesPerFormula.get(name);

                        List<CompoundWithAbstractFP<Fingerprint>> iter = searchByFingerBlast(database, maskedFingerprintVersion, queries.get(0).getInchi().extractFormula());

                        List<List<ScoredCandidate>> hits = getTopHits(queries, iter, MAX_CANDIDATES);

                        Iterator<CompoundWithAbstractFP<ProbabilityFingerprint>> queryIterator = queries.iterator();
                        Iterator<List<ScoredCandidate>> scoredCandListIterator = hits.listIterator();

                        while (queryIterator.hasNext()) {
                            CompoundWithAbstractFP<ProbabilityFingerprint> query = queryIterator.next();
                            List<ScoredCandidate> scoredCandidates = scoredCandListIterator.next();
                            final Instance instance = new Instance(query, scoredCandidates);
                            instanceList.add(instance);
                        }
                    }
                    return instanceList;
                }
            }));
        }

        List<Instance> results = new ArrayList<>();
        for (Future<List<Instance>> future : futures) {
            try {
                results.addAll(future.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        executorService.shutdown();

        return results;

    }


    private static List<CompoundWithAbstractFP<Fingerprint>> searchByFingerBlast(final ChemicalDatabase db, MaskedFingerprintVersion maskedFingerprintVersion, final MolecularFormula formula) throws DatabaseException {
        final ConcurrentLinkedQueue<FingerprintCandidate> candidates = new ConcurrentLinkedQueue<>();
        db.lookupStructuresAndFingerprintsByFormula(formula, candidates);

        List<CompoundWithAbstractFP<Fingerprint>> candidateList = new ArrayList<>();
        FingerprintCandidate c;

        while ((c=candidates.poll())!=null) {
            CompoundWithAbstractFP<Fingerprint> candidate = new CompoundWithAbstractFP<>(c.getInchi(), maskedFingerprintVersion.mask(c.getFingerprint()));
            candidateList.add(candidate);
        }
        return candidateList;
    }


    private List<List<ScoredCandidate>> getTopHits(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, Iterable<CompoundWithAbstractFP<Fingerprint>> candidates, int maxCandidates){
        final FingerblastScoring[] scorerPerQuery = new FingerblastScoring[queries.size()];
        final List<List<ScoredCandidate>> scoredCandidatesPerQuery = new ArrayList<>(queries.size());

        for (int k=0; k < queries.size(); ++k) {
            scorerPerQuery[k] = new CSIFingerIdScoring(statistics);
            scorerPerQuery[k].prepare(queries.get(k).getFingerprint());
            scoredCandidatesPerQuery.add(new ArrayList<ScoredCandidate>());
        }

        for (CompoundWithAbstractFP<Fingerprint> c : candidates) {
            for (int k=0; k < queries.size(); ++k) {
                final CompoundWithAbstractFP<ProbabilityFingerprint> query = queries.get(k);
                final List<ScoredCandidate> scoredCandidates = scoredCandidatesPerQuery.get(k);
                final double candidateScore = scorerPerQuery[k].score(query.getFingerprint(), c.getFingerprint());
                scoredCandidates.add(new ScoredCandidate(c, candidateScore));
            }
        }

        List<List<ScoredCandidate>> scoredCandidatesPerQuery2 = new ArrayList<>(queries.size());
        for (List<ScoredCandidate> scoredCandidates : scoredCandidatesPerQuery) {
            Collections.sort(scoredCandidates, SCORED_CANDIDATE_COMPARATOR);
            if (scoredCandidates.size()<=maxCandidates || maxCandidates<0) scoredCandidatesPerQuery2.add(scoredCandidates);
            else scoredCandidatesPerQuery2.add(scoredCandidates.subList(0, maxCandidates));
        }
        return scoredCandidatesPerQuery2;
    }


    private static double precentageCorrect(List<ScoredCandidate> candidates, CompoundWithAbstractFP<ProbabilityFingerprint> query){
//        String struct = inchi2d(query.getInchi().in2D);
        String struct = query.getInchi().key2D();
        double bestScore = candidates.get(0).score;
        int same_score = 0;
        int correct = 0;
        for (ScoredCandidate candidate : candidates) {
            double score = candidate.score;
            if (Math.abs(bestScore-score)>1e-16){
                break;
            }
//            if (struct.equals(inchi2d(candidate.getInchi().in2D))){
            if (struct.equals(candidate.getInchi().key2D())){
                correct++;
            }
            same_score++;
        }

        return 1.0*correct/same_score;
    }

    private static double getAUC(TDoubleArrayList corrects, TDoubleArrayList platts){
        TDoubleArrayList platts2 = new TDoubleArrayList();
        List<Boolean> correctsBool = new ArrayList<>();
        for (int i = 0; i < corrects.size(); i++){
            double c = corrects.get(i);
            double platt = platts.get(i);
            int mult = 1;
            while (Math.abs(c*mult-Math.round(c*mult))>1e-10) mult++;

            int x = (int)Math.round(c*mult);
            int y = mult-x;
            for (int j = 0; j < x; j++) {
                correctsBool.add(true);
                platts2.add(platt);

            }
            for (int j = 0; j < y; j++) {
                correctsBool.add(false);
                platts2.add(platt);

            }
        }
        boolean[] boolArr = new boolean[correctsBool.size()];
        for (int i = 0; i < boolArr.length; i++) {
            boolArr[i] = correctsBool.get(i);
        }
        return new Stats(platts2.toArray(), boolArr).getAUC();
    }
    protected class Instance {
        protected final CompoundWithAbstractFP<ProbabilityFingerprint> query;
        protected final List<ScoredCandidate> candidates;
        protected Instance(CompoundWithAbstractFP<ProbabilityFingerprint> query, List<ScoredCandidate> candidates){
            this.query = query;
            this.candidates = candidates;
        }
    }

}
