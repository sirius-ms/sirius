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
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingData;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingStructures;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class FingerIdClient extends AbstractCsiClient {
    @SafeVarargs
    public FingerIdClient(@Nullable URI serverUrl,
                          @Nullable String contextPath,
                          @NotNull IOFunctions.IOConsumer<Request.Builder>... requestDecorators
    ) {
        super(serverUrl, contextPath, requestDecorators);
    }

    /**
     * make statistics of fingerprints and write the used indizes of fingerprints into the
     * given TIntArrayList (as this property is not contained in FingerprintStatistics)
     *
     * @return prediction model
     * @throws IOException if http response parsing fails
     */
    public FingerIdData getFingerIdData(@NotNull PredictorType predictorType,
                                        @NotNull OkHttpClient client
    ) throws IOException {
        return execute(client,
                () -> new Request.Builder().get().url(buildVersionSpecificWebapiURI("/fingerid/data")
                        .addQueryParameter("predictor", predictorType.toBitsAsString())
                        .build()),
                FingerIdData::read
        );
    }

    public BayesnetScoring getCovarianceScoring(@NotNull PredictorType predictorType,
                                                @NotNull FingerprintVersion fpVersion,
                                                @Nullable MolecularFormula formula,
                                                @NotNull PredictionPerformance[] performances,
                                                @NotNull OkHttpClient client
    ) throws IOException {
        return execute(client,
                () -> {
                    HttpUrl.Builder u = buildVersionSpecificWebapiURI("/fingerid/covariancetree")
                            .addQueryParameter("predictor", predictorType.toBitsAsString());
                    if (formula != null)
                        u.addQueryParameter("formula", formula.toString());
                    return new Request.Builder().get().url(u.build());
                }, br -> BayesnetScoringBuilder.readScoring(br, fpVersion, BayesianScoringUtils.calculatePseudoCount(performances), BayesianScoringUtils.allowOnlyNegativeScores)
        );
    }


    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull final PredictorType predictorType,
                                                        @NotNull OkHttpClient client
    ) throws IOException {
        return execute(client,
                () -> new Request.Builder().get().url(buildVersionSpecificWebapiURI("/fingerid/confidence")
                        .addQueryParameter("predictor", predictorType.toBitsAsString())
                        .build()),
                TrainedSVM::readSVMs
        );
    }

    @Deprecated
    public TrainingData getTrainingStructures(@NotNull PredictorType predictorType, @NotNull OkHttpClient client) throws IOException {
        return execute(client,
                () -> new Request.Builder().get().url(buildVersionSpecificWebapiURI("/fingerid/trainingstructures")
                        .addQueryParameter("predictor", predictorType.toBitsAsString()).build()),
                TrainingData::readTrainingData
        );
    }

    public TrainingStructures getTrainingStructuresAll(@NotNull PredictorType predictorType, @NotNull OkHttpClient client) throws IOException {
        return execute(client,
                () -> new Request.Builder().get().url(buildVersionSpecificWebapiURI("/fingerid/training-structures-all")
                        .addQueryParameter("predictor", predictorType.toBitsAsString()).build()),
                r -> new ObjectMapper().readValue(r, TrainingStructures.class)
        );
    }
    //endregion

}
