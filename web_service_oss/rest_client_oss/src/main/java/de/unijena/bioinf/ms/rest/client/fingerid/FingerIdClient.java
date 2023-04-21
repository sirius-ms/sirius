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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.BayesianScoringUtils;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.BayesnetScoringBuilder;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.rest.client.AbstractCsiClient;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingData;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class FingerIdClient extends AbstractCsiClient {
    @SafeVarargs
    public FingerIdClient(@Nullable URI serverUrl, @NotNull IOFunctions.IOConsumer<HttpUriRequest>... requestDecorators) {
        super(serverUrl, requestDecorators);
    }

    public JobUpdate<FingerprintJobOutput> postJobs(final FingerprintJobInput input, HttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/fingerid/" + CID + "/fp-jobs").build());
                    post.setEntity(new InputStreamEntity(new ByteArrayInputStream(
                            new ObjectMapper().writeValueAsBytes(input)), ContentType.APPLICATION_JSON));
                    post.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

                    return post;
                }, new TypeReference<>() {}
        );
    }

    /**
     * make statistics of fingerprints and write the used indizes of fingerprints into the
     * given TIntArrayList (as this property is not contained in FingerprintStatistics)
     *
     * @return prediction model
     * @throws IOException if http response parsing fails
     */
    public FingerIdData getFingerIdData(PredictorType predictorType, HttpClient client) throws IOException {
        return execute(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/fingerid/data")
                        .setParameter("predictor", predictorType.toBitsAsString())
                        .build()),
                FingerIdData::read
        );
    }


    public JobUpdate<CovtreeJobOutput> postCovtreeJobs(final CovtreeJobInput input, HttpClient client) throws IOException {
        return executeFromJson(client,
                () -> {
                    final HttpPost post = new HttpPost(buildVersionSpecificWebapiURI("/fingerid/" + CID + "/covtree-jobs").build());
                    post.setEntity(new InputStreamEntity(new ByteArrayInputStream(
                            new ObjectMapper().writeValueAsBytes(input)), ContentType.APPLICATION_JSON));
                    post.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
                    return post;
                }, new TypeReference<>() {}
        );
    }


    public BayesnetScoring getCovarianceScoring(@NotNull PredictorType predictorType, @NotNull FingerprintVersion fpVersion, @Nullable MolecularFormula formula, @NotNull PredictionPerformance[] performances, @NotNull HttpClient client) throws IOException {
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


    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull final PredictorType predictorType, HttpClient client) throws IOException {
        return execute(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/fingerid/confidence")
                        .setParameter("predictor", predictorType.toBitsAsString())
                        .build()),
                TrainedSVM::readSVMs
        );
    }

    public TrainingData getTrainingStructures(PredictorType predictorType, HttpClient client) throws IOException {
        return execute(client,
                () -> new HttpGet(buildVersionSpecificWebapiURI("/fingerid/trainingstructures").setParameter("predictor", predictorType.toBitsAsString()).build()),
                TrainingData::readTrainingData
        );
    }
    //endregion

}
