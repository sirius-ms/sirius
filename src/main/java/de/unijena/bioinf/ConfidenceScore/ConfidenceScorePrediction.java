package de.unijena.bioinf.ConfidenceScore;

import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ConfidenceScore.confidenceScore.ScoredCandidate;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.DatabaseException;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.Mask;
import de.unijena.bioinf.fingerid.TrainedCSIFingerId;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by Marcus Ludwig on 27.06.16.
 *
 * Train and predict confidence scores for CSI-FingerId structure identifications.
 */
public class ConfidenceScorePrediction {

    final CSIFingerIdScoring csiFingerIdScoring;

    private static final Comparator<ScoredCandidate> SCORED_CANDIDATE_COMPARATOR = new ScoredCandidate.MaxBestComparator();

    private final PredictionPerformance[] statistics;
    private final QueryPredictor queryPredictor;
    private final MaskedFingerprintVersion maskedFingerprintVersion;


    public static void main(String... args) throws IOException, DatabaseException, InterruptedException {
        if (!((args.length==4 || args.length==5) && args[0].equals("train")) && !(args.length==5 && args[0].equals("predict"))){
            System.out.println("commands: 'train' or 'predict'\n" +
                    "train <predictedFPFile> <fingerid.data> <confidenceScoreModelOutputPath> [<maskFile>]\n" +
                    "predict <confidenceScoreModelOutputPath> <fingerid.data> <predictedFPFile> <outputFile>\n" +
                    "\n" +
                    "predictedFPFile must contain header with 'id' 'inchi' 'predicted_fp'. \n" +
                    "Everything after the predicted_fp-column is considered to be part of the fingerprint\n" +
                    "mask file just necessary if fingerid.data has a smaller molecular property set\n");
            return;
        }

        ChemicalDatabase db = new ChemicalDatabase();
        db.setBioFilter(BioFilter.ONLY_BIO);


        if (args[0].equals("train")){
            Path predictedFPFile = Paths.get(args[1]); //id, molecular formula column and last columns predicted fp
            Path fingeridFile = Paths.get(args[2]);
            Path outputFile = Paths.get(args[3]);
            Mask mask;
            if (args.length>4){
                final BufferedReader br = Files.newBufferedReader(Paths.get(args[4]), Charset.defaultCharset());
                    mask = Mask.fromString(br.readLine().split("\\s+"));
                br.close();
            } else {
                mask = null;
            }


            final TrainedCSIFingerId fingerid = TrainedCSIFingerId.load(fingeridFile.toFile(),false);
            PredictionPerformance[] statistics = fingerid.getPredictionPerformances();
            MaskedFingerprintVersion maskedFingerprintVersion = fingerid.getMaskedFingerprintVersion();

            List<? extends CompoundWithAbstractFP<ProbabilityFingerprint>> queries = readQueries(predictedFPFile, maskedFingerprintVersion, mask);

            QueryPredictor queryPredictor = train((List<CompoundWithAbstractFP<ProbabilityFingerprint>>)queries, statistics, maskedFingerprintVersion, db);
            queryPredictor.writeToFile(outputFile);
        } else {
            //predict
            Path confidenceModel = Paths.get(args[1]);
            Path fingeridFile = Paths.get(args[2]);
            Path predictedFPFile = Paths.get(args[3]); //id, molecular formula column and last columns predicted fp
            Path outputFile = Paths.get(args[4]);
            BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());

            final TrainedCSIFingerId fingerid = TrainedCSIFingerId.load(fingeridFile.toFile(),false);
            MaskedFingerprintVersion maskedFingerprintVersion = fingerid.getMaskedFingerprintVersion();

            QueryPredictor queryPredictor = QueryPredictor.loadFromFile(confidenceModel);
            ConfidenceScorePrediction confidenceScorePrediction = new ConfidenceScorePrediction(queryPredictor);
            List<CompoundWithId> queries = readQueries(predictedFPFile, maskedFingerprintVersion, null);

            for (CompoundWithId query : queries) {
                double confidenceScore = 0;
                try {
                    confidenceScore = confidenceScorePrediction.computeConfidenceScore(query, db);
                } catch (PredictionException e) {
                    System.err.println("no applicable predictor for query "+query.id);
                    continue;
                }
                writer.write(query.id+"\t"+confidenceScore+"\n");
            }
            writer.close();
        }

    }

    protected static List<CompoundWithId> readQueries(Path predictedFPFile, MaskedFingerprintVersion fingerprintVersion, Mask crossvalMask) throws IOException {
        BufferedReader reader = Files.newBufferedReader(predictedFPFile, Charset.defaultCharset());

        String[] header = reader.readLine().split("\\s");
        int id_col = -1;
        int inchi_col = -1;
        int fp_col = -1;

        for (int i = 0; i < header.length; i++) {
            String s = header[i].toLowerCase();
            if (s.equals("id")) id_col = i;
            else if (s.equals("inchi"))
                inchi_col = i;
            else if (s.equals("predicted") || s.equals("predicted_fp") || s.equals("predicted_fingerprint"))
                fp_col = i;
        }

        if (id_col<0) id_col = inchi_col;

        if (id_col<0 || inchi_col<0 || fp_col<0){
            throw new IOException("could not find all necassary columns. Need 'id' 'inchi' and 'predicted_fp'");
        }

        if (fp_col!=header.length-1){
            throw new IOException("predicted fingerprint column is to be expected the last.");
        }

        List<CompoundWithId> queries = new ArrayList<>();

        String line;
        while ((line=reader.readLine())!=null){
            String[] row = line.split("\\s");
            String id = row[id_col];
            String inchi = row[inchi_col];
            double[] predicted_fp = parsePredictedFP(Arrays.copyOfRange(row, fp_col, row.length));

            final CompoundWithId c;
            if (crossvalMask==null){
                c = new CompoundWithId(id, new InChI(null, inchi), new ProbabilityFingerprint(fingerprintVersion, predicted_fp));
            } else {
                predicted_fp = crossvalMask.unapply(predicted_fp);
                ProbabilityFingerprint probabilityFingerprint = fingerprintVersion.mask(predicted_fp);
                c = new CompoundWithId(id, new InChI(null, inchi), probabilityFingerprint);
            }


            queries.add(c);
        }

        return queries;
    }

    private static double[] parsePredictedFP(String[] row){
        double[] fp = new double[row.length];
        for (int i = 0; i < row.length; i++) {
            String s = row[i];
            fp[i] = Double.parseDouble(s);
        }
        return fp;
    }


    /**
     *
     * @param confidenceModel Path to the confidence score model file
     * @throws IOException
     */
    public ConfidenceScorePrediction(Path confidenceModel) throws IOException {
        this(QueryPredictor.loadFromFile(confidenceModel));
    }

    /**
     *
     * @param queryPredictor the learned confidence score predictor
     */
    public ConfidenceScorePrediction(QueryPredictor queryPredictor) {
        this(queryPredictor, fingerprintVersionFromIndices(queryPredictor.absFPIndices));
    }


    /**
     *
     * @param queryPredictor the learned confidence score predictor
     * @param maskedFingerprintVersion the {@link MaskedFingerprintVersion} used for your data
     */
    public ConfidenceScorePrediction(QueryPredictor queryPredictor, MaskedFingerprintVersion maskedFingerprintVersion) {
        this.queryPredictor = queryPredictor;
        this.maskedFingerprintVersion = maskedFingerprintVersion;
        statistics = queryPredictor.getStatistics();
        csiFingerIdScoring = new CSIFingerIdScoring(statistics);
    }

    private static MaskedFingerprintVersion fingerprintVersionFromIndices(int[] absFPIndices){
        MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(CdkFingerprintVersion.getDefault()).disableAll();
        for (int i : absFPIndices) {
            builder.enable(i);
        }
        return builder.toMask();
    }


    //-----------------------------------------------------------------------
    //----------------------------------------------------------------------

    /**
     * train a confidence score model
     * @param queries a list of {@link CompoundWithAbstractFP} with the correct structure inchi and the predicted fingerprint
     * @param statistics the {@link PredictionPerformance}s of the different molecular properties (fingerprint positions)
     * @param maskedFingerprintVersion the {@link MaskedFingerprintVersion} of the predicted fingerprints
     * @param db {@link ChemicalDatabase} to search in for candidate structure
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static QueryPredictor train(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, ChemicalDatabase db) throws IOException, InterruptedException, DatabaseException {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        QueryPredictor queryPredictor =  train(queries, statistics, maskedFingerprintVersion, db, executorService);
        executorService.shutdown();
        return queryPredictor;
    }

    /**
     * train a confidence score model
     * @param queries a list of {@link CompoundWithAbstractFP} with the correct structure inchi and the predicted fingerprint
     * @param statistics the {@link PredictionPerformance}s of the different molecular properties (fingerprint positions)
     * @param maskedFingerprintVersion the {@link MaskedFingerprintVersion} of the predicted fingerprints
     * @param db {@link ChemicalDatabase} to search for candidate structure in
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static QueryPredictor train(List<CompoundWithAbstractFP<ProbabilityFingerprint>> queries, PredictionPerformance[] statistics, MaskedFingerprintVersion maskedFingerprintVersion, ChemicalDatabase db, ExecutorService executorService) throws IOException, InterruptedException, DatabaseException {
        System.out.println("compute hitlist");

        List<CompoundWithAbstractFP<ProbabilityFingerprint>[]> candidatesList = new ArrayList<>();
        for (CompoundWithAbstractFP<ProbabilityFingerprint> query : queries) {
            MolecularFormula mf = query.getInchi().extractFormula();
            CompoundWithAbstractFP<ProbabilityFingerprint>[] candidates = searchByFingerBlast(db, maskedFingerprintVersion, mf).toArray(new CompoundWithAbstractFP[0]);
            candidatesList.add(candidates);
        }

        System.out.println("train");

        TrainConfidenceScore trainConfidenceScore = TrainConfidenceScore.AdvancedMultipleSVMs(true);
        trainConfidenceScore.train(executorService, queries.toArray(new CompoundWithAbstractFP[0]), candidatesList.toArray(new CompoundWithAbstractFP[0][]), statistics);

        QueryPredictor queryPredictor = trainConfidenceScore.getPredictors();


        queryPredictor.absFPIndices = getAbsIndices(maskedFingerprintVersion);
        return queryPredictor;
    }

    private static int[] getAbsIndices(MaskedFingerprintVersion maskedFingerprintVersion){
        int[] absIndices = new int[maskedFingerprintVersion.size()];
        for (int i = 0; i < absIndices.length; i++) {
            absIndices[i] = maskedFingerprintVersion.getAbsoluteIndexOf(i);
        }
        return absIndices;
    }

    //-----------------------------------------------------------------------
    //----------------------------------------------------------------------

    /**
     * fingerprint CANDIDATES must be SORTED by score or can be rescored (and sorted) using CSIFingerIdScoring
     * @param query {@link CompoundWithAbstractFP} with the estimated structure inchi and the predicted fingerprint
     * @param candidates candidate structures with inchi and fingerprint
     * @param rescore if true, score and sort candidates by CSIFingerIdScoring
     * @return
     */
    public double computeConfidenceScore(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] candidates, boolean rescore) throws PredictionException {
        final double platt;
        if (rescore){
            ScoredCandidate[] scoredCandidates = getScoredHitlist(query, candidates);
            platt = queryPredictor.estimateProbability(query, scoredCandidates);
        } else {
            platt = queryPredictor.estimateProbability(query, candidates);
        }
        return platt;
    }

    /**
     *
     * @param query {@link CompoundWithAbstractFP} with the estimated structure inchi and the predicted fingerprint
     * @param db {@link ChemicalDatabase} to search candidates structures in
     * @return
     * @throws PredictionException
     */
    public double computeConfidenceScore(CompoundWithAbstractFP<ProbabilityFingerprint> query, ChemicalDatabase db) throws PredictionException, DatabaseException {
        MolecularFormula mf = query.getInchi().extractFormula();
        CompoundWithAbstractFP<Fingerprint>[] candidates = searchByFingerBlast(db, this.maskedFingerprintVersion, mf).toArray(new CompoundWithAbstractFP[0]);

        ScoredCandidate[] scoredCandidates = getScoredHitlist(query, candidates);
        double platt = queryPredictor.estimateProbability(query, scoredCandidates);

        return platt;
    }

    private ScoredCandidate[] getScoredHitlist(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] candidates){
        csiFingerIdScoring.prepare(query.getFingerprint());

        ScoredCandidate[] scoredCandidates = new ScoredCandidate[candidates.length];
        for (int i = 0; i < candidates.length; i++) {
            CompoundWithAbstractFP<Fingerprint> c = candidates[i];
            final double candidateScore = csiFingerIdScoring.score(query.getFingerprint(), c.getFingerprint());
            scoredCandidates[i] = new ScoredCandidate(c, candidateScore);
        }

        Arrays.sort(scoredCandidates, SCORED_CANDIDATE_COMPARATOR);
        return scoredCandidates;
    }


    protected static List<CompoundWithAbstractFP<Fingerprint>> searchByFingerBlast(final ChemicalDatabase db, MaskedFingerprintVersion maskedFingerprintVersion, final MolecularFormula formula) throws DatabaseException {
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


    private static class CompoundWithId extends CompoundWithAbstractFP<ProbabilityFingerprint>{
        public final String id;
        public CompoundWithId(String id, InChI inchi, ProbabilityFingerprint fingerprint) {
            super(inchi, fingerprint);
            this.id = id;
        }
    }

}
