package de.unijena.bioinf.ms.rest.client.fingerid;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.CovarianceScoringMethod;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import gnu.trove.list.array.TIntArrayList;
import net.sf.jniinchi.INCHI_KEY;
import net.sf.jniinchi.JniInchiException;
import net.sf.jniinchi.JniInchiOutputKey;
import net.sf.jniinchi.JniInchiWrapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FingerIdClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(FingerIdClient.class);

    public FingerIdClient(@NotNull URI serverUrl) {
        super(serverUrl);
    }

    /*public boolean deleteJobs(@NotNull final List<JobId> idsToDelete, CloseableHttpClient client) throws URISyntaxException {
        if (idsToDelete.isEmpty())
            return true;

        final String ids = "[" + idsToDelete.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
        final HttpDelete delete = new HttpDelete(buildVersionSpecificWebapiURI("/fingerid/" + CID + "/jobs")
                .setParameter("jobIds", ids)
                .build());
        int reponsecode = Integer.MIN_VALUE;
        String responseReason = null;
        try (CloseableHttpResponse response = client.execute(delete)) {
            reponsecode = response.getStatusLine().getStatusCode();
            responseReason = response.getStatusLine().getReasonPhrase();
            if (reponsecode == 200)
                return true;
            LOG.error("Could not delete Jobs! Response Code: " + reponsecode + " Reason: " + responseReason);
        } catch (Exception t) {
            LOG.error("Error when doing job deletion request! Response error code: " + reponsecode + " - Reason: " + responseReason, t);
        }
        return false;
    }*/

    /*public List<? extends JobUpdate> getJobs(CloseableHttpClient client) throws URISyntaxException {
        final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/fingerid/" + CID + "/jobs").build());
        int reponsecode = Integer.MIN_VALUE;
        String responseReason = null;

        try (CloseableHttpResponse response = client.execute(get)) {
            reponsecode = response.getStatusLine().getStatusCode();
            responseReason = response.getStatusLine().getReasonPhrase();

            try (final BufferedReader reader = new BufferedReader(getIn(response.getEntity()))) {
                return new ObjectMapper().<List<FingerprintJobUpdate>>readValue(reader, new TypeReference<List<FingerprintJobUpdate>>() {
                });
            }
        } catch (Exception e) {
            LOG.error("Error when updating jobs Response error code: " + reponsecode + " - Reason: " + responseReason + " - Message: " + e.getMessage());
        }
        return null;
    }*/


    public JobUpdate<FingerprintJobOutput> postJobs(final FingerprintJobInput input, CloseableHttpClient client) throws IOException {
        //check predictor compatibility
        int c = input.experiment.getPrecursorIonType().getCharge();
        for (PredictorType type : input.predictors) {
            if (!type.isValid(c))
                throw new IllegalArgumentException("Predictor " + type.name() + " is not compatible with charge " + c + ".");
        }

        try {
            final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/fingerid/" + CID + "/jobs").build());
            final String stringMs, jsonTree;
            {
                final JenaMsWriter writer = new JenaMsWriter();
                final StringWriter sw = new StringWriter();
                try (final BufferedWriter bw = new BufferedWriter(sw)) {
                    writer.write(bw, input.experiment);
                }
                stringMs = sw.toString();
            }
            {
                final FTJsonWriter writer = new FTJsonWriter();
                final StringWriter sw = new StringWriter();
                writer.writeTree(sw, input.ftree);
                jsonTree = sw.toString();
            }

            final NameValuePair ms = new BasicNameValuePair("ms", stringMs);
            final NameValuePair tree = new BasicNameValuePair("ft", jsonTree);
            final NameValuePair predictor = new BasicNameValuePair("predictors", PredictorType.getBitsAsString(input.predictors));

            final UrlEncodedFormEntity params = new UrlEncodedFormEntity(Arrays.asList(ms, tree, predictor));
            post.setEntity(params);

            // SUBMIT JOB
            try (CloseableHttpResponse response = client.execute(post)) {
                isSuccessful(response);
                try (final BufferedReader reader = new BufferedReader(getIn(response.getEntity()))) {
                    return new ObjectMapper().readValue(reader, new TypeReference<JobUpdate<FingerprintJobOutput>>(){});
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    ////////////////////////////////////JOBS END

    /**
     * make statistics of fingerprints and write the used indizes of fingerprints into the
     * given TIntArrayList (as this property is not contained in FingerprintStatistics)
     *
     * @param fingerprintIndizes
     * @return
     * @throws IOException
     */
    public PredictionPerformance[] getStatistics(PredictorType predictorType, final TIntArrayList fingerprintIndizes, CloseableHttpClient client) throws IOException {
        fingerprintIndizes.clear();
        try {
            final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/fingerid/statistics.csv")
                    .setParameter("predictor", predictorType.toBitsAsString())
                    .build()
            );

            final ArrayList<PredictionPerformance> performances = new ArrayList<>();
            try (CloseableHttpResponse response = client.execute(get)) {
                isSuccessful(response);
                HttpEntity e = response.getEntity();
                final BufferedReader br = new BufferedReader(getIn(e));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] tabs = line.split("\t");
                    final int index = Integer.parseInt(tabs[0]);
                    PredictionPerformance p = new PredictionPerformance(
                            Double.parseDouble(tabs[1]),
                            Double.parseDouble(tabs[2]),
                            Double.parseDouble(tabs[3]),
                            Double.parseDouble(tabs[4])
                    );
                    performances.add(p);
                    fingerprintIndizes.add(index);
                }
            }
            return performances.toArray(new PredictionPerformance[0]);
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    public CovarianceScoringMethod getCovarianceScoring(PredictorType predictorType, FingerprintVersion fpVersion, PredictionPerformance[] performances, CloseableHttpClient client) throws IOException {
        try {
            final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/fingerid/covariancetree.csv")
                    .setParameter("predictor", predictorType.toBitsAsString())
                    .build()
            );
            CovarianceScoringMethod covarianceScoringMethod;
            try (CloseableHttpResponse response = client.execute(get)) {
                isSuccessful(response);
                covarianceScoringMethod = CovarianceScoringMethod.readScoring(getIn(response.getEntity()), fpVersion, performances);
            }
            return covarianceScoringMethod;
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull final PredictorType predictorType, CloseableHttpClient client) throws IOException {
        try {
            final HttpGet get = new HttpGet(buildVersionSpecificWebapiURI("/fingerid/confidence.json")
                    .setParameter("predictor", predictorType.toBitsAsString())
                    .build()
            );

            final Map<String, TrainedSVM> svmMap = new HashMap<>();
            try (CloseableHttpResponse response = client.execute(get)) {
                isSuccessful(response);
                HttpEntity e = response.getEntity();
                try (InputStreamReader streamReader = new InputStreamReader(e.getContent())) {
                    final JsonReader jsonReader = Json.createReader(streamReader);
                    JsonObject svms = jsonReader.readObject();
                    svms.keySet().forEach(key -> {
                        svmMap.put(key, new TrainedSVM(svms.getJsonObject(key)));
                    });
                }
            }
            return svmMap;
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    public InChI[] getTrainingStructures(PredictorType predictorType, CloseableHttpClient client) throws IOException {
        final HttpGet get;
        try {
            get = new HttpGet(buildVersionSpecificWebapiURI("/fingerid/trainingstructures.csv").setParameter("predictor", predictorType.toBitsAsString()).build());
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        ArrayList<InChI> inchis = new ArrayList<>();
        CloseableHttpResponse response = client.execute(get);
        HttpEntity e = response.getEntity();

        final BufferedReader br = new BufferedReader(getIn(e));
        String line;
        while ((line = br.readLine()) != null) {
            try {
                String[] tabs = line.split("\t");
                InChI inChI;
                if (tabs.length == 1) {
                    //no InChiKeys contained. Compute them.
                    String inchi = tabs[0];
                    String key = inchi2inchiKey(inchi);
                    inChI = new InChI(key, inchi);
                } else {
                    inChI = new InChI(tabs[0], tabs[1]);
                }

                inchis.add(inChI);
            } catch (JniInchiException ex) {
                LOG.warn("Could not parse training structure InChI, skipping this entry");
            }
        }
        response.close();
        return inchis.toArray(new InChI[0]);
    }

    private static String inchi2inchiKey(String inchi) throws JniInchiException {
        if (inchi == null) throw new NullPointerException("Given InChI is null");
        if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");
        JniInchiOutputKey key = JniInchiWrapper.getInchiKey(inchi);
        if (key.getReturnStatus() == INCHI_KEY.OK) {
            return key.getKey();
        } else {
            throw new JniInchiException("Error while creating InChIKey: " + key.getReturnStatus());
        }
    }
}
