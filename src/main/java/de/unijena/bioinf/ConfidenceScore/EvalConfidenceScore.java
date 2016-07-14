package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ConfidenceScore.confidenceScore.*;
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
        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.AdvancedMultipleSVMs(useLinearSVM);

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
        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(queries, queryPredictor.getStatistics(), maskedFingerprintVersion, db);

        System.out.println("compute hitlist");
        List<Instance> instances = evalConfidenceScore.computeHitList();

        TDoubleArrayList platts = new TDoubleArrayList();
        TByteArrayList corrects = new TByteArrayList();
        List<String> ids = new ArrayList<>();

        System.out.println("predict");
        for (Instance instance : instances) {
            if (instance.candidates.size()==0) continue;
            boolean isCorrect = (instance.candidates.get(0).getInchi().in2D.equals(instance.query.getInchi().in2D));
            double platt = 0;
            try {
                platt = queryPredictor.estimateProbability(instance.query, instance.candidates.toArray(new ScoredCandidate[0]));
            } catch (PredictionException e) {
                continue;
            }

            assert platt>=0;
            assert platt<=1;
            platts.add(platt);
            corrects.add((byte)(isCorrect ? 1 : 0));
            ids.add(instance.query.getInchi().in3D);
        }

        boolean[] correctsBool = new boolean[corrects.size()];
        for (int i = 0; i < correctsBool.length; i++) correctsBool[i] = (corrects.get(i)>0);

        Stats stats = new Stats(platts.toArray(), correctsBool);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
        writer.write("#AUC"+SEP+stats.getAUC()+"\n");
        writer.write("inchi"+SEP+"platt"+SEP+"isCorrect"+"\n");
        for (int i = 0; i < correctsBool.length; i++) {
            String id = ids.get(i);
            byte b = corrects.get(i);
            double platt = platts.get(i);
            writer.write(id+SEP+platt+SEP+b+"\n");
        }
        writer.close();
    }

    public static void crossvalidation(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, Path outputFile, boolean useLinearSVM, ChemicalDatabase db) throws IOException, InterruptedException, DatabaseException {
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



        //shuffle
        System.out.println(instances.get(0).query.getInchi().in3D);
        Collections.shuffle(instances);
        System.out.println(instances.get(0).query.getInchi().in3D);

        List<Instance> posInstances = new ArrayList<>();
        List<Instance> negInstances = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.candidates.size()==0) continue;
            if (instance.query.getInchi().in2D.equals(instance.candidates.get(0).getInchi().in2D)){
                posInstances.add(instance);
            } else {
                negInstances.add(instance);
            }
        }

        System.out.println("pos: "+posInstances.size()+" neg: "+negInstances.size());

        List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries_Pos = new ArrayList<>();
        List<CompoundWithAbstractFP<Fingerprint>[]> candidates_Pos = new ArrayList<>();
        for (Instance instance : posInstances) {
            queries_Pos.add(instance.query);
            candidates_Pos.add(instance.candidates.toArray(new CompoundWithAbstractFP[0]));
        }
        List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries_Neg = new ArrayList<>();
        List<CompoundWithAbstractFP<Fingerprint>[]> candidates_Neg = new ArrayList<>();
        for (Instance instance : negInstances) {
            queries_Neg.add(instance.query);
            candidates_Neg.add(instance.candidates.toArray(new CompoundWithAbstractFP[0]));
        }

        System.out.println("train");

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.AdvancedMultipleSVMs(useLinearSVM);
//        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.All(useLinearSVM);
//        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.JustScoreFeature(useLinearSVM);

        TDoubleArrayList platts = new TDoubleArrayList();
        TByteArrayList corrects = new TByteArrayList();
        List<String> ids = new ArrayList<>();

        final int sizePos = candidates_Pos.size();
        final int sizeNeg = candidates_Neg.size();


        HashSet<InChI> idSet = new HashSet<>();

        int end_Pos = 0;
        int end_Neg = 0;
        for (int i = 0; i < FOLD; i++) {
            int start_Pos = end_Pos;
            int start_Neg = end_Neg;
            end_Pos = (int)Math.ceil(1.0*(i+1)*sizePos/FOLD);
            end_Neg = (int)Math.ceil(1.0*(i+1)*sizeNeg/FOLD);

            List<CompoundWithAbstractFP<Fingerprint>[]> trainCand = new ArrayList<>(candidates_Pos.subList(0, start_Pos));
            trainCand.addAll(candidates_Pos.subList(end_Pos,sizePos));
            trainCand.addAll(candidates_Neg.subList(0, start_Neg));
            trainCand.addAll(candidates_Neg.subList(end_Neg,sizeNeg));


            System.out.println(start_Pos+" "+end_Pos+" | "+start_Neg+" "+end_Neg);

            List<CompoundWithAbstractFP<ProbabilityFingerprint>> trainQueries = new ArrayList<>(queries_Pos.subList(0, start_Pos));
            trainQueries.addAll(queries_Pos.subList(end_Pos,sizePos));
            trainQueries.addAll(queries_Neg.subList(0, start_Neg));
            trainQueries.addAll(queries_Neg.subList(end_Neg,sizeNeg));


            List<Instance> testInstances = new ArrayList<>(posInstances.subList(start_Pos, end_Pos));
            testInstances.addAll(negInstances.subList(start_Neg, end_Neg));

            System.out.println("train: "+trainQueries.size()+" "+trainCand.size());
            System.out.println("test: "+testInstances.size());

            //train
            trainConfidenceScore.train(executorService, trainQueries.toArray(new CompoundWithAbstractFP[0]), trainCand.toArray(new CompoundWithAbstractFP[0][]), statistics);

            QueryPredictor queryPredictor = trainConfidenceScore.getPredictors();
            queryPredictor.absFPIndices = getAbsIndices(maskedFingerprintVersion);


            //test


            System.out.println("predict");
            for (Instance instance : testInstances) {
                if (instance.candidates.size()==0) continue;

                if (DEBUG){
                    if (idSet.contains(instance.query.getInchi())){
                        System.out.println("Already tested query");
                    }
                }

                idSet.add(instance.query.getInchi());
                boolean isCorrect = (instance.query.getInchi().in2D.equals(instance.candidates.get(0).getInchi().in2D)); //todo inchi equals !?!
                double platt = 0;
                try {
                    platt = queryPredictor.estimateProbability(instance.query, instance.candidates.toArray(new CompoundWithAbstractFP[0]));
                } catch (PredictionException e) {
                    System.err.println(e.getMessage());
                    continue;
                }

                assert platt>=0;
                assert platt<=1;
                platts.add(platt);
                corrects.add((byte)(isCorrect ? 1 : 0));
                ids.add(instance.query.getInchi().in3D);
            }

        }

        //write output
        boolean[] correctsBool = new boolean[corrects.size()];
        for (int i = 0; i < correctsBool.length; i++) correctsBool[i] = (corrects.get(i)>0);

        Stats stats = new Stats(platts.toArray(), correctsBool);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
        writer.write("#AUC"+SEP+stats.getAUC()+"\n");
        writer.write("inchi"+SEP+"platt"+SEP+"isCorrect"+"\n");
        for (int i = 0; i < correctsBool.length; i++) {
            String id = ids.get(i);
            byte b = corrects.get(i);
            double platt = platts.get(i);
            writer.write(id+SEP+platt+SEP+b+"\n");
        }
        writer.close();

        executorService.shutdown();
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

    public List<Instance> computeHitList() throws IOException, DatabaseException {
        List<Instance> instances = new ArrayList<>();
        List<String> queriesPerFormulaList = new ArrayList<>(queriesPerFormula.keySet());

        final List<CompoundWithAbstractFP<ProbabilityFingerprint>> allQueries = new LinkedList<>();
        final List<List<ScoredCandidate>> allScoredCandidates = new LinkedList<>();
        for (String name : queriesPerFormulaList) {
            if (name == null) continue;
            final List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries = queriesPerFormula.get(name);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            List<CompoundWithAbstractFP<Fingerprint>> iter = searchByFingerBlast(db, maskedFingerprintVersion, queries.get(0).getInchi().extractFormula(), executorService);
            executorService.shutdown();

            List<List<ScoredCandidate>> hits = getTopHits(queries, iter, MAX_CANDIDATES);
            allScoredCandidates.addAll(hits);
            allQueries.addAll(queries);
        }

        Iterator<CompoundWithAbstractFP<ProbabilityFingerprint>> queryIterator = allQueries.iterator();
        Iterator<List<ScoredCandidate>> scoredCandListIterator = allScoredCandidates.listIterator();

        while (queryIterator.hasNext()) {
            CompoundWithAbstractFP<ProbabilityFingerprint> query = queryIterator.next();
            List<ScoredCandidate> scoredCandidates = scoredCandListIterator.next();
//            Collections.sort(scoredCandidates, new ScoredCandidate.MaxBestComparator()); // already done
            final Instance instance = new Instance(query, scoredCandidates);
            instances.add(instance);

        }

        return instances;
    }


    private static List<CompoundWithAbstractFP<Fingerprint>> searchByFingerBlast(final ChemicalDatabase db, MaskedFingerprintVersion maskedFingerprintVersion, final MolecularFormula formula, ExecutorService backgroundThread) throws DatabaseException {
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


    protected class Instance {
        protected final CompoundWithAbstractFP<ProbabilityFingerprint> query;
        protected final List<ScoredCandidate> candidates;
        protected Instance(CompoundWithAbstractFP<ProbabilityFingerprint> query, List<ScoredCandidate> candidates){
            this.query = query;
            this.candidates = candidates;
        }
    }

}
