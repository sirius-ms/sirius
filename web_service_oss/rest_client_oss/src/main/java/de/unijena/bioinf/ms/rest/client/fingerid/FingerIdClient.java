/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.client.fingerid;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import net.sf.jniinchi.INCHI_KEY;
import net.sf.jniinchi.JniInchiException;
import net.sf.jniinchi.JniInchiOutputKey;
import net.sf.jniinchi.JniInchiWrapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FingerIdClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(FingerIdClient.class);

    public FingerIdClient(URI serverUrl) {
        super(serverUrl);
    }

    public JobUpdate<FingerprintJobOutput> postJobs(final FingerprintJobInput input, CloseableHttpClient client) throws IOException {
        //check predictor compatibility
        final int c = input.experiment.getPrecursorIonType().getCharge();
        for (PredictorType type : input.predictors)
            if (!type.isValid(c))
                throw new IllegalArgumentException("Predictor " + type.name() + " is not compatible with charge " + c + ".");

        return executeFromJson(client,
                () -> {
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
                    Map<String,String> values =  new HashMap<>();
                    values.put("ms",stringMs);
                    values.put("ft",jsonTree);
                    values.put("predictors", PredictorType.getBitsAsString(input.predictors));
                    post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(values)));

                    return post;
                }, new TypeReference<>() {
                }
        );
    }

    /**
     * make statistics of fingerprints and write the used indizes of fingerprints into the
     * given TIntArrayList (as this property is not contained in FingerprintStatistics)
     *
     * @return prediction model
     * @throws IOException if http response parsing fails
     */
    public FingerIdData getFingerIdData(PredictorType predictorType, CloseableHttpClient client) throws IOException {
        return execute(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/fingerid/data")
                        .setParameter("predictor", predictorType.toBitsAsString())
                        .build()),
                FingerIdData::read
        );
    }


    public JobUpdate<CovtreeJobOutput> postCovtreeJobs(final CovtreeJobInput input, CloseableHttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/fingerid/" + CID + "/covtree-jobs").build());
                    String v = new ObjectMapper().writeValueAsString(input);
                    post.setEntity(new StringEntity(v, StandardCharsets.UTF_8));
                    post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
                    return post;
                }, new TypeReference<>() {
                }
        );
    }


    public BayesnetScoring getCovarianceScoring(@NotNull PredictorType predictorType, @NotNull FingerprintVersion fpVersion, @Nullable MolecularFormula formula, @NotNull PredictionPerformance[] performances, @NotNull CloseableHttpClient client) throws IOException {
        return execute(client,
                () -> {
                    final URIBuilder u = buildVersionSpecificWebapiURI("/fingerid/covariancetree")
                            .setParameter("predictor", predictorType.toBitsAsString());
                    if (formula != null)
                        u.setParameter("formula", formula.toString());
                    return new HttpGet(u.build());
                }, br -> BayesnetScoringBuilder.readScoring(br, fpVersion, BayesianScoringUtils.calculatePseudoCount(performances), BayesianScoringUtils.allowOnlyNegativeScores)
        );
    }

    // todo change json unmarshalling to jackson
    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull final PredictorType predictorType, CloseableHttpClient client) throws IOException {
        return execute(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/fingerid/confidence")
                        .setParameter("predictor", predictorType.toBitsAsString())
                        .build()),
                br -> {
                    final Map<String, TrainedSVM> svmMap = new HashMap<>();
                    final JsonReader jsonReader = Json.createReader(br);
                    JsonObject svms = jsonReader.readObject();
                    svms.keySet().forEach(key -> {
                        svmMap.put(key, new TrainedSVM(svms.getJsonObject(key)));
                    });
                    return svmMap;
                }
        );
    }

    public InChI[] getTrainingStructures(PredictorType predictorType, CloseableHttpClient client) throws IOException {
        return execute(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/fingerid/trainingstructures").setParameter("predictor", predictorType.toBitsAsString()).build()),
                br -> {
                    ArrayList<InChI> inchis = new ArrayList<>();
                    String line;
                    while ((line = br.readLine()) != null) {
                        try {
                            String[] tabs = line.split("\t");
                            InChI inChI;
                            if (tabs.length == 1) {
                                //no InChiKeys contained. Compute them.
                                String inchi = tabs[0];
                                String key = inchi2inchiKey(inchi);
                                inChI = InChIs.newInChI(key, inchi);
                            } else {
                                inChI = InChIs.newInChI(tabs[0], tabs[1]);
                            }

                            inchis.add(inChI);
                        } catch (JniInchiException ex) {
                            LOG.warn("Could not parse training structure InChI, skipping this entry");
                        }
                    }
                    return inchis.toArray(new InChI[0]);
                }
        );
    }
    //endregion

    //region helper
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
    //endregion
}
