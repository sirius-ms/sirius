package de.unijena.bioinf.ConfidenceScore;

import com.google.gson.JsonObject;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ConfidenceScore.confidenceScore.FeatureCreator;
import de.unijena.bioinf.babelms.json.JSONDocumentType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Marcus Ludwig on 11.03.16.
 */
public class QueryPredictor implements Parameterized{
    private FeatureCreator[] featureCreators;
    private Scaler[] scalers;
    private LinearPredictor[] predictors;
    private int[] priorities;
    private PredictionPerformance[] statistics;
    protected int[] absFPIndices;

    QueryPredictor(){

    }

    /**
     *
     * @param featureCreators
     * @param scalers
     * @param predictors
     * @param priorities huge priorities are chosen first
     * @param statistics
     */
    public QueryPredictor(FeatureCreator[] featureCreators, Scaler[] scalers, LinearPredictor[] predictors, int[] priorities, PredictionPerformance[] statistics) {
        this.featureCreators = featureCreators;
        this.scalers = scalers;
        this.predictors = predictors;
        this.priorities = priorities;
        this.statistics = statistics;
    }


    private int findCompatibleFeatureCreator(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates){
        int bestCompatiblePos = -1;
        int bestCompatiblePriority = -1;
        for (int i = 0; i < featureCreators.length; i++) {
            if (featureCreators[i].isCompatible(query, rankedCandidates)){
                if (bestCompatiblePriority< priorities[i]){
                    bestCompatiblePos = i;
                    bestCompatiblePriority = priorities[i];
                }
            }

        }
        return bestCompatiblePos;
    }

    public boolean isApplicable(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates){
        return findCompatibleFeatureCreator(query, rankedCandidates)>0;
    }

    private double[] computeScaledFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates, int predictorNumber) throws PredictionException {
        if (predictorNumber<0) throw new PredictionException("no compatible predictor for this input");
        final double[] features = featureCreators[predictorNumber].computeFeatures(query, rankedCandidates);
        final double[] scaled = scalers[predictorNumber].scale(features);
        return scaled;
    }

    public boolean predict(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) throws PredictionException {
        final int i = findCompatibleFeatureCreator(query, rankedCandidates);
        final double[] scaled = computeScaledFeatures(query, rankedCandidates, i);
        return predictors[i].predict(scaled);
    }

    public double score(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) throws PredictionException {
        final int i = findCompatibleFeatureCreator(query, rankedCandidates);
        final double[] scaled = computeScaledFeatures(query, rankedCandidates, i);
        return predictors[i].score(scaled);
    }

    /**
     *
     * @param query
     * @param rankedCandidates the candidates for the query ranked by their score
     * @return
     */
    public double estimateProbability(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) throws PredictionException {
        final int i = findCompatibleFeatureCreator(query, rankedCandidates);
        final double[] scaled = computeScaledFeatures(query, rankedCandidates, i);
        return predictors[i].estimateProbability(scaled);
    }


    public PredictionPerformance[] getStatistics() {
        return statistics;
    }

    public int[] getAbsFPIndices() {
        return absFPIndices;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        List<FeatureCreator> featureCreatorList = new ArrayList<>();
        fillList(featureCreatorList, helper, document, dictionary, "featureCreators");
        List<Scaler> scalerList = new ArrayList<>();
        fillList(scalerList, helper, document, dictionary, "scalers");
        List<Scaler> linearPredictorList = new ArrayList<>();
        fillList(linearPredictorList, helper, document, dictionary, "predictors");
        L priorityList = document.getListFromDictionary(dictionary, "priorities");
        int size = document.sizeOfList(priorityList);
        int[] priorities = new int[size];
        for (int i = 0; i < size; i++) priorities[i] = (int)document.getIntFromList(priorityList, i);

        L list = document.getListFromDictionary(dictionary, "tp");
        size = document.sizeOfList(list);
        double[] tp = new double[size];
        for (int i = 0; i < size; i++) tp[i] = document.getDoubleFromList(list, i);
        list = document.getListFromDictionary(dictionary, "fp");
        size = document.sizeOfList(list);
        double[] fp = new double[size];
        for (int i = 0; i < size; i++) fp[i] = document.getDoubleFromList(list, i);
        list = document.getListFromDictionary(dictionary, "tn");
        size = document.sizeOfList(list);
        double[] tn = new double[size];
        for (int i = 0; i < size; i++) tn[i] = document.getDoubleFromList(list, i);
        list = document.getListFromDictionary(dictionary, "fn");
        size = document.sizeOfList(list);
        double[] fn = new double[size];
        for (int i = 0; i < size; i++) fn[i] = document.getDoubleFromList(list, i);
//        double minF = (int)document.getDoubleFromDictionary(dictionary, "minF");
//        int minPresent = (int)document.getIntFromDictionary(dictionary, "minPresent");
        list = document.getListFromDictionary(dictionary, "absFPIndices");
        size = document.sizeOfList(list);
        absFPIndices = new int[size];
        for (int i = 0; i < size; i++) absFPIndices[i] = (int)document.getIntFromList(list, i);

        this.statistics = new PredictionPerformance[size];
        for (int i = 0; i < size; i++) this.statistics[i] = new PredictionPerformance(tp[i], fp[i], tn[i], fn[i]);


        this.featureCreators = featureCreatorList.toArray(new FeatureCreator[0]);
        this.scalers = scalerList.toArray(new Scaler[0]);
        this.predictors = linearPredictorList.toArray(new LinearPredictor[0]);
        this.priorities = priorities;

        for (FeatureCreator featureCreator : featureCreators)
            featureCreator.prepare(statistics);

    }

    private <T, G, D, L> void fillList(List<T> list, ParameterHelper helper, DataDocument<G, D, L> document, D dictionary, String keyName) {
        if (!document.hasKeyInDictionary(dictionary, keyName)) return;
        Iterator<G> ls = document.iteratorOfList(document.getListFromDictionary(dictionary, keyName));
        while (ls.hasNext()) {
            final G l = ls.next();
            list.add((T) helper.unwrap(document, l));
        }
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (FeatureCreator featureCreator : featureCreators) document.addToList(list, helper.wrap(document, featureCreator));
        document.addListToDictionary(dictionary, "featureCreators", list);
        list = document.newList();
        for (Scaler scaler : scalers) document.addToList(list, helper.wrap(document, scaler));
        document.addListToDictionary(dictionary, "scalers", list);
        list = document.newList();
        for (LinearPredictor linearPredictor : predictors) document.addToList(list, helper.wrap(document, linearPredictor));
        document.addListToDictionary(dictionary, "predictors", list);
        list = document.newList();
        for (int p : priorities) document.addToList(list, p);
        document.addListToDictionary(dictionary, "priorities", list);

        double[] tp = new double[statistics.length];
        double[] fp = new double[statistics.length];
        double[] tn = new double[statistics.length];
        double[] fn = new double[statistics.length];

        for (int i = 0; i < statistics.length; i++) {
            PredictionPerformance predictionPerformance = statistics[i];
            tp[i] = predictionPerformance.getTp();
            fp[i] = predictionPerformance.getFp();
            tn[i] = predictionPerformance.getTn();
            fn[i] = predictionPerformance.getFn();
        }

        list = document.newList();
        for (double p : tp) document.addToList(list, p);
        document.addListToDictionary(dictionary, "tp", list);
        list = document.newList();
        for (double p : fp) document.addToList(list, p);
        document.addListToDictionary(dictionary, "fp", list);
        list = document.newList();
        for (double p : tn) document.addToList(list, p);
        document.addListToDictionary(dictionary, "tn", list);
        list = document.newList();
        for (double p : fn) document.addToList(list, p);
        document.addListToDictionary(dictionary, "fn", list);
//        document.addToDictionary(dictionary, "minF", statistics.getFThreshold());
//        document.addToDictionary(dictionary, "minPresent", statistics.getMinimalNumberOfOccurences());
        list = document.newList();
        for (int i : absFPIndices) document.addToList(list, i);
        document.addListToDictionary(dictionary, "absFPIndices", list);
    }


    public void writeToFile(Path path) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset());
        final JSONDocumentType json = new JSONDocumentType();
        final JsonObject obj = json.newDictionary();
        writeToProfile(json, obj);
        try {
            JSONDocumentType.writeJson(json, obj, writer);
        } finally {
            writer.close();
        }
    }


    public <G, D, L> void writeToProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        exportParameters(helper, document, dict);

    }

    public static QueryPredictor loadFromFile(Path path) throws IOException {
        final JsonObject json = JSONDocumentType.getJSON("queryPredictor", path.toAbsolutePath().toString());
        final JSONDocumentType document = new JSONDocumentType();
        return loadFromProfile(document, json);
    }

    public static QueryPredictor loadFromStream(Reader reader) throws IOException {
        final JsonObject json = JSONDocumentType.read(reader);
        final JSONDocumentType document = new JSONDocumentType();
        return loadFromProfile(document, json);
    }

    public static  <G, D, L> QueryPredictor loadFromProfile(DataDocument<G, D, L> document, G value){
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        QueryPredictor queryPredictor = new QueryPredictor();
        queryPredictor.importParameters(helper, document, dict);
        return queryPredictor;
    }


}
