package de.unijena.bioinf.ConfidenceScore;

import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ConfidenceScore.confidenceScore.*;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.*;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
* Created by Marcus Ludwig on 09.03.16.
*/
public class EvalConfidenceScore {

    final int MAX_CANDIDATES = -1; //all candidates
    private static final String SEP = "\t";

    final static boolean DEBUG = true;

    private final FingerprintStatistics statistics;

    protected final HashMap<String, List<Query>> queriesPerFormula;

    final MarvinsScoring marvinsScoring;
    final MaximumLikelihoodScoring maximumLikelihoodScoring;
    final ProbabilityEstimateScoring probabilityEstimateScoring;

    private static final Comparator<ScoredCandidate> SCORED_CANDIDATE_COMPARATOR = new ScoredCandidate.MaxBestComparator();

    private int[] indices;

    private ChemicalDatabase db;


    /////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////     WithOUT CANDIDATES         ///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////
    public static void train(Path queryFile, Path statisticsFile, Path indexMappingFile, Path outputFile, boolean useLinearSVM, ChemicalDatabase db) throws IOException, InterruptedException {
        train(queryFile, null, statisticsFile, indexMappingFile, outputFile, useLinearSVM, db);
    }


    public static void predict(Path queryFile, Path modelFile, Path outputFile, ChemicalDatabase db) throws IOException {
        predict(queryFile, null, modelFile, outputFile, db);
    }



  /////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////     With CANDIDATES         ///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////
    public static void train(Path queryFile, Path candidatesFolder, Path statisticsFile, Path indexMappingFile, Path outputFile, boolean useLinearSVM, ChemicalDatabase db) throws IOException, InterruptedException {
        Query[] queries  = parseFromFile(queryFile.toFile());
        Object[] o = readStatistics(statisticsFile,indexMappingFile);
        FingerprintStatistics statistics = (FingerprintStatistics)o[0];
        int[] absFPIndices = (int[])o[1];
        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(queries, statistics, absFPIndices, db);

        System.out.println("compute hitlist");

        List<Instance> instances = evalConfidenceScore.computeHitList((candidatesFolder==null? null : candidatesFolder.toFile()));

        List<Query> queriesX = new ArrayList<>();
        List<Candidate[]> candidatesX = new ArrayList<>();
        for (Instance instance : instances) {
            queriesX.add(instance.query);
            candidatesX.add(instance.candidates.toArray(new Candidate[0]));
        }

        System.out.println("train");

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//        ExecutorService executorService = Executors.newFixedThreadPool(1);
//        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.DefaultMultipleSVMs(useLinearSVM);
        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.AdvancedMultipleSVMs(useLinearSVM);

        trainConfidenceScore.train(executorService, queriesX.toArray(new Query[0]), candidatesX.toArray(new Candidate[0][]), statistics);

        QueryPredictor queryPredictor = trainConfidenceScore.getPredictors();
        queryPredictor.absFPIndices = absFPIndices;
        queryPredictor.writeToFile(outputFile);

        executorService.shutdown();
    }


    public static void predict(Path queryFile, Path candidatesFolder, Path modelFile, Path outputFile, ChemicalDatabase db) throws IOException {
        Query[] queries;
        try {
            queries = parseFromFile(queryFile.toFile());
        } catch (RuntimeException e){
            queries = parseFromFileUnknowns(queryFile.toFile());
        }

        QueryPredictor queryPredictor = QueryPredictor.loadFromFile(modelFile);
        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(queries, queryPredictor.getStatistics(), queryPredictor.getAbsFPIndices(), db);

        System.out.println("compute hitlist");
        List<Instance> instances = evalConfidenceScore.computeHitList((candidatesFolder==null? null : candidatesFolder.toFile()));

        TDoubleArrayList platts = new TDoubleArrayList();
        TByteArrayList corrects = new TByteArrayList();
        List<String> ids = new ArrayList<>();

        System.out.println("predict");
        for (Instance instance : instances) {
            if (instance.candidates.size()==0) continue;
            boolean isCorrect = (instance.candidates.get(0).inchi.equals(instance.query.inchi));
            double platt = queryPredictor.estimateProbability(instance.query, instance.candidates.toArray(new Candidate[0]));
//            boolean predicted = queryPredictor.predict(instance.query, instance.candidates.toArray(new Candidate[0]));

            assert platt>=0;
            assert platt<=1;
            platts.add(platt);
            corrects.add((byte)(isCorrect ? 1 : 0));
            ids.add(instance.query.getId());
        }

        boolean[] correctsBool = new boolean[corrects.size()];
        for (int i = 0; i < correctsBool.length; i++) correctsBool[i] = (corrects.get(i)>0);

        Stats stats = new Stats(platts.toArray(), correctsBool);
        BufferedWriter writer = Files.newBufferedWriter(outputFile);
        writer.write("#AUC"+SEP+stats.getAUC()+"\n");
        writer.write("id"+SEP+"platt"+SEP+"isCorrect"+"\n");
        for (int i = 0; i < correctsBool.length; i++) {
//            String id = instances.get(i).query.getId();
            String id = ids.get(i);
            byte b = corrects.get(i);
            double platt = platts.get(i);
            writer.write(id+SEP+platt+SEP+b+"\n");
        }
        writer.close();
    }

    public static void crossvalidation(Path queryFile, Path candidatesFolder, Path statisticsFile, Path indexMappingFile, Path outputFile, boolean useLinearSVM, ChemicalDatabase db) throws IOException, InterruptedException {
        final int FOLD = 5;

        Query[] queries  = parseFromFile(queryFile.toFile());
        Object[] o = readStatistics(statisticsFile,indexMappingFile);
        FingerprintStatistics statistics = (FingerprintStatistics)o[0];
        int[] absFPIndices = (int[])o[1];
        EvalConfidenceScore evalConfidenceScore = new EvalConfidenceScore(queries, statistics, absFPIndices, db);

        System.out.println("compute hitlist");

        List<Instance> instances = evalConfidenceScore.computeHitList((candidatesFolder==null? null : candidatesFolder.toFile()));

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
        //todo for positive and negative instances
        System.out.println(instances.get(0).query.getId());
        Collections.shuffle(instances);
        System.out.println(instances.get(0).query.getId());

        List<Instance> posInstances = new ArrayList<>();
        List<Instance> negInstances = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.candidates.size()==0) continue;
            if (instance.query.inchi.equals(instance.candidates.get(0).inchi)){
                posInstances.add(instance);
            } else {
                negInstances.add(instance);
            }
        }

        System.out.println("pos: "+posInstances.size()+" neg: "+negInstances.size());

        List<Query> queries_Pos = new ArrayList<>();
        List<Candidate[]> candidates_Pos = new ArrayList<>();
        for (Instance instance : posInstances) {
            queries_Pos.add(instance.query);
            candidates_Pos.add(instance.candidates.toArray(new Candidate[0]));
        }
        List<Query> queries_Neg = new ArrayList<>();
        List<Candidate[]> candidates_Neg = new ArrayList<>();
        for (Instance instance : negInstances) {
            queries_Neg.add(instance.query);
            candidates_Neg.add(instance.candidates.toArray(new Candidate[0]));
        }

//        List<Query> queriesX = new ArrayList<>();
//        List<Candidate[]> candidatesX = new ArrayList<>();
//        for (Instance instance : instances) {
//            queriesX.add(instance.query);
//            candidatesX.add(instance.candidates.toArray(new Candidate[0]));
//        }


        System.out.println("train");

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//        ExecutorService executorService = Executors.newFixedThreadPool(1);
//        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.DefaultMultipleSVMs(useLinearSVM);
//        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.AdvancedMultipleSVMs(useLinearSVM);
//        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.All(useLinearSVM);
        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.JustScoreFeature(useLinearSVM);
//        ...//todo test csiFingerID score separation

        TDoubleArrayList platts = new TDoubleArrayList();
        TByteArrayList corrects = new TByteArrayList();
        List<String> ids = new ArrayList<>();

        final int sizePos = candidates_Pos.size();
        final int sizeNeg = candidates_Neg.size();


        HashSet<String> idSet = new HashSet<>();

        int end_Pos = 0;
        int end_Neg = 0;
        for (int i = 0; i < FOLD; i++) {
            int start_Pos = end_Pos;
            int start_Neg = end_Neg;
            end_Pos = (int)Math.ceil(1.0*(i+1)*sizePos/FOLD);
            end_Neg = (int)Math.ceil(1.0*(i+1)*sizeNeg/FOLD);

            List<Candidate[]> trainCand = new ArrayList<>(candidates_Pos.subList(0, start_Pos));
            trainCand.addAll(candidates_Pos.subList(end_Pos,sizePos));
            trainCand.addAll(candidates_Neg.subList(0, start_Neg));
            trainCand.addAll(candidates_Neg.subList(end_Neg,sizeNeg));


            System.out.println(start_Pos+" "+end_Pos+" | "+start_Neg+" "+end_Neg);

            List<Query> trainQueries = new ArrayList<>(queries_Pos.subList(0, start_Pos));
            trainQueries.addAll(queries_Pos.subList(end_Pos,sizePos));
            trainQueries.addAll(queries_Neg.subList(0, start_Neg));
            trainQueries.addAll(queries_Neg.subList(end_Neg,sizeNeg));


            List<Instance> testInstances = new ArrayList<>(posInstances.subList(start_Pos, end_Pos));
            testInstances.addAll(negInstances.subList(start_Neg, end_Neg));

            System.out.println("train: "+trainQueries.size()+" "+trainCand.size());
            System.out.println("test: "+testInstances.size());

            //train
            trainConfidenceScore.train(executorService, trainQueries.toArray(new Query[0]), trainCand.toArray(new Candidate[0][]), statistics);

            QueryPredictor queryPredictor = trainConfidenceScore.getPredictors();
            queryPredictor.absFPIndices = absFPIndices;


            //test


            System.out.println("predict");
            for (Instance instance : testInstances) {
                if (instance.candidates.size()==0) continue;

                if (idSet.contains(instance.query.getId())){
                    System.out.println("Already tested query");
                }
                idSet.add(instance.query.getId());
                boolean isCorrect = (instance.query.inchi.equals(instance.candidates.get(0).inchi));
//                boolean isCorrect = (instance.candidates.get(0).inchi.equals(instance.query.inchi));
//                double platt = queryPredictor.estimateProbability(instance.query, instance.candidates.toArray(new Candidate[0]));
                double platt = queryPredictor.score(instance.query, instance.candidates.toArray(new Candidate[0]));
//            boolean predicted = queryPredictor.predict(instance.query, instance.candidates.toArray(new Candidate[0]));

                assert platt>=0;
                assert platt<=1;
                platts.add(platt);
                corrects.add((byte)(isCorrect ? 1 : 0));
                ids.add(instance.query.getId());
            }

        }

        //write output
        boolean[] correctsBool = new boolean[corrects.size()];
        for (int i = 0; i < correctsBool.length; i++) correctsBool[i] = (corrects.get(i)>0);

        Stats stats = new Stats(platts.toArray(), correctsBool);
        BufferedWriter writer = Files.newBufferedWriter(outputFile);
        writer.write("#AUC"+SEP+stats.getAUC()+"\n");
        writer.write("id"+SEP+"platt"+SEP+"isCorrect"+"\n");
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
        BufferedWriter writer = Files.newBufferedWriter(file);
        writer.write(x+SEP+y+"\n");
        for (int i = 0; i < data.length; i++) {
            double[] doubles = data[i];
            writer.write(doubles[0]+SEP+doubles[1]+"\n");
        }
        writer.close();
    }

    /*
    statistics file with abs indices and a mapping to the unique ones
     */
    private static Object[] readStatistics(Path statisticsFile, Path indexMapping) throws IOException {
        List<String> lines = Files.readAllLines(statisticsFile);
        String[] header = lines.get(0).split("\\s");

        int tpIdx = -1;
        int fpIdx = -1;
        int tnIdx = -1;
        int fnIdx = -1;
        int absIdxIdx = -1;
        for (int i = 0; i < header.length; i++) {
            String s = header[i];
            if (s.toLowerCase().equals("tp")){
                tpIdx = i;
            } else if (s.toLowerCase().equals("fp")){
                fpIdx = i;
            } else if (s.toLowerCase().equals("tn")){
                tnIdx = i;
            } else if (s.toLowerCase().equals("fn")){
                fnIdx = i;
            }  else if (s.toLowerCase().equals("absindex")){
                absIdxIdx = i;
            }
        }
        if (tpIdx<0 && fpIdx<0 && tnIdx<0 && fnIdx<0 && absIdxIdx<0){
            absIdxIdx = 0;
            tpIdx = 1;
            fpIdx = 2;
            tnIdx = 3;
            fnIdx = 4;
        } else if (tpIdx<0 || fpIdx<0 || tnIdx<0 || fnIdx<0 || absIdxIdx<0){
            throw new RuntimeException("expected header in statistics file containing: \"tp\" \"tn\" \"fp\" \"fn\" \"absIndex\"");
        } else {
            lines.remove(0);
        }


        if (indexMapping==null){
            TIntArrayList tpList = new TIntArrayList();
            TIntArrayList tnList = new TIntArrayList();
            TIntArrayList fpList = new TIntArrayList();
            TIntArrayList fnList = new TIntArrayList();
            TIntArrayList abIndices = new TIntArrayList();
            for (String line : lines) {
                String[] columns = line.split("\\s");
                int[] row = new int[columns.length];
                for (int i = 0; i < row.length; i++) {
                    row[i] = Integer.parseInt(columns[i]);
                }
                tpList.add(row[tpIdx]); tnList.add(row[tnIdx]); fpList.add(row[fpIdx]); fnList.add(row[fnIdx]);abIndices.add(row[absIdxIdx]);
            }
            FingerprintStatistics statistics = new FingerprintStatistics(tpList.toArray(), fpList.toArray(), tnList.toArray(), fnList.toArray());
            return new Object[]{statistics,abIndices.toArray()};
        } else {
            lines = Files.readAllLines(indexMapping);
            header = lines.get(0).split("\\s");
            int absCol = -1;
            int relCol = -1;
            if (header[0].toLowerCase().equals("absindex")){
                absCol = 0;
                relCol = 1;
            } else if (header[1].toLowerCase().equals("absindex")){
                absCol = 1;
                relCol = 0;
            } else {
                throw new RuntimeException("absIndex col not found");
            }

            TIntIntHashMap indexMap = new TIntIntHashMap(lines.size(), 0.75f, -1, -1);
            Iterator<String> iterator = lines.listIterator(1);
            while (iterator.hasNext()) {
                String next = iterator.next();
                String[] row = next.split("\\s");
                indexMap.put(Integer.parseInt(row[absCol]), Integer.parseInt(row[relCol]));
            }

            int[] relIndices = indexMap.values();
            Arrays.sort(relIndices);

            int relIndexPos = 0;
            TIntArrayList tpList = new TIntArrayList();
            TIntArrayList tnList = new TIntArrayList();
            TIntArrayList fpList = new TIntArrayList();
            TIntArrayList fnList = new TIntArrayList();
            TIntArrayList abIndices = new TIntArrayList();
            for (String line : lines) {
                String[] columns = line.split("\\s");
                int[] row = new int[columns.length];
                for (int i = 0; i < row.length; i++) {
                    row[i] = Integer.parseInt(columns[i]);
                }
                int currentIndex = indexMap.get(row[4]);
                if (currentIndex<0) continue;
                abIndices.add(row[absIdxIdx]);
                while (relIndexPos<relIndices.length && relIndices[relIndexPos]<currentIndex){
                    tpList.add(0); tnList.add(0); fpList.add(0); fnList.add(0);
                    relIndexPos++;
                }
                tpList.add(row[tpIdx]); tnList.add(row[tnIdx]); fpList.add(row[fpIdx]); fnList.add(row[fnIdx]);
                relIndexPos++;

            }
            while (relIndexPos<relIndices.length){
                tpList.add(0); tnList.add(0); fpList.add(0); fnList.add(0);
                relIndexPos++;
            }

            if (tpList.size()!=abIndices.size()){
                throw new RuntimeException("statistics file / index mapping: size of absolute indices differs from features in statistic");
            }

            FingerprintStatistics statistics = new FingerprintStatistics(tpList.toArray(), fpList.toArray(), tnList.toArray(), fnList.toArray());
            statistics.setMinimalNumberOfOccurences(10);
            statistics.setFThreshold(0.49999999999);
            return new Object[]{statistics, abIndices.toArray()};
        }





    }

    public EvalConfidenceScore(Query[] queries, FingerprintStatistics statistics, int[] absFPIndices, ChemicalDatabase db) {
        marvinsScoring = new MarvinsScoring();
        maximumLikelihoodScoring = new MaximumLikelihoodScoring();
        probabilityEstimateScoring = new ProbabilityEstimateScoring();
        this.statistics = statistics;
        this.indices = absFPIndices;

        final HashMap<String, List<Query>> map = new HashMap<>();
        for (Query c : queries) {
            final String formula = c.getFormula();
            if (!map.containsKey(formula)) map.put(formula, new ArrayList<Query>());
            map.get(formula).add(c);
        }
        this.queriesPerFormula = map;

        this.db = db;
    }

    public List<Instance> computeHitList(File target) throws IOException {
        List<Instance> instances = new ArrayList<>();
        List<String> queriesPerFormulaList = new ArrayList<>(queriesPerFormula.keySet());

        final List<Query> allQueries = new LinkedList<>();
        final List<List<ScoredCandidate>> allScoredCandidates = new LinkedList<>();
        for (String name : queriesPerFormulaList) {
            if (name == null) continue;
            final List<Query> queries = queriesPerFormula.get(name);

            if (target!=null){
                final File queryFile = new File(target, name + ".csv");
                if (!queryFile.exists()) {
                    System.err.println(queryFile + " not found. Skip all queries with this molecular formula");
                    continue;
                }
                System.out.println(queryFile);

                if (!queryFile.exists()) continue;
                Iterable<Candidate> iter = new CandidateParser(queryFile);

                List<List<ScoredCandidate>> hits = getTopHits(queries, iter, MAX_CANDIDATES);
                allScoredCandidates.addAll(hits);
                allQueries.addAll(queries);
            } else {
                final boolean enforceBio = true;
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Iterable<Candidate> iter = searchByFingerBlast(db, enforceBio, indices, MolecularFormula.parse(queries.get(0).getFormula()), executorService);
                executorService.shutdown();

                List<List<ScoredCandidate>> hits = getTopHits(queries, iter, MAX_CANDIDATES);
                allScoredCandidates.addAll(hits);
                allQueries.addAll(queries);
            }
        }

        Iterator<Query> queryIterator = allQueries.iterator();
        Iterator<List<ScoredCandidate>> scoredCandListIterator = allScoredCandidates.listIterator();

        while (queryIterator.hasNext()) {
            Query query = queryIterator.next();
            List<ScoredCandidate> scoredCandidates = scoredCandListIterator.next();
            Collections.sort(scoredCandidates, new ScoredCandidate.MaxBestComparator());
            final Instance instance = new Instance(query, scoredCandidates);
            instances.add(instance);

        }

        return instances;
    }

    private Iterable<Candidate> searchByFingerBlast(final ChemicalDatabase db, final boolean enforceBio, final int[] indizes, final MolecularFormula formula,ExecutorService backgroundThread) {
        final ConcurrentLinkedQueue<FingerprintCandidate> candidates = new ConcurrentLinkedQueue<>();
        final Future future = backgroundThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    db.lookupStructuresAndFingerprintsByFormula(formula, enforceBio, indizes, candidates);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        List<Candidate> candidateList = new ArrayList<>();
        FingerprintCandidate c;
        try {
            future.get(); // wait now until all candidates are loaded
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        while ((c=candidates.poll())!=null) {
            candidateList.add(new Candidate(c.getInchi().in2D, c.getFingerprint()));
        }
        return candidateList;
    }


    private List<List<ScoredCandidate>> getTopHits(List<Query> queries, Iterable<Candidate> candidates, int maxCandidates){

        final Scorer[] scorerPerQuery = new Scorer[queries.size()];
        final Scorer[] mlScorerPerQuery = new Scorer[queries.size()];
        final Scorer[] probScorerPerQuery = new Scorer[queries.size()];
        final List<List<ScoredCandidate>> scoredCandidatesPerQuery = new ArrayList<>(queries.size());
        for (int k=0; k < queries.size(); ++k) {
            scorerPerQuery[k] = marvinsScoring.getScorer(statistics);
            scorerPerQuery[k].preprocessQuery(queries.get(k), statistics);

            scoredCandidatesPerQuery.add(new ArrayList<ScoredCandidate>());

            mlScorerPerQuery[k] = maximumLikelihoodScoring.getScorer(statistics);
            mlScorerPerQuery[k].preprocessQuery(queries.get(k), statistics);

            probScorerPerQuery[k] = probabilityEstimateScoring.getScorer(statistics);
            probScorerPerQuery[k].preprocessQuery(queries.get(k), statistics);
        }

        for (Candidate c : candidates) {
            for (int k=0; k < queries.size(); ++k) {
                final Query query = queries.get(k);
                final List<ScoredCandidate> scoredCandidates = scoredCandidatesPerQuery.get(k);
                final double candidateScore = scorerPerQuery[k].score(query, c, statistics);
                final double mlCandidateScore = mlScorerPerQuery[k].score(query, c, statistics);
                final double probCandidateScore = probScorerPerQuery[k].score(query, c, statistics);

                scoredCandidates.add(new ScoredCandidate(c, candidateScore, mlCandidateScore, probCandidateScore));
            }
        }

        List<List<ScoredCandidate>> scoredCandidatesPerQuery2 = new ArrayList<>(queries.size());
        for (List<ScoredCandidate> scoredCandidates : scoredCandidatesPerQuery) {
            scoredCandidates.sort(SCORED_CANDIDATE_COMPARATOR);
            if (scoredCandidates.size()<=maxCandidates || maxCandidates<0) scoredCandidatesPerQuery2.add(scoredCandidates);
            else scoredCandidatesPerQuery2.add(scoredCandidates.subList(0, maxCandidates));
        }
        return scoredCandidatesPerQuery2;
    }


    public static Query[] parseFromFile(File referenceFile){
        return Iterators.toArray(new QueryParser(referenceFile).iterator(), Query.class);
    }

    public static Query[] parseFromFileUnknowns(File referenceFile){
        return Iterators.toArray(new UnknownQueryParser(referenceFile).iterator(), Query.class);
    }

    protected class Instance {
        protected final Query query;
        protected final List<ScoredCandidate> candidates;
        protected Instance(Query query, List<ScoredCandidate> candidates){
            this.query = query;
            this.candidates = candidates;
        }
    }

}
