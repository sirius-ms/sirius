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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.confidence_score.CSICovarianceConfidenceScorer;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.blast.BayesnetScoringWithDynamicComputation;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.webapi.WebAPI;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * This is the API class for CSI:FingerID and Fingerblast.
 * We init a separate predictor object for positive and negative ionization
 */
public class CSIPredictor extends AbstractStructurePredictor {
    protected MaskedFingerprintVersion fpVersion;
    protected PredictionPerformance[] performances;
    protected volatile boolean initialized;

    public CSIPredictor(PredictorType predictorType) {
        super(predictorType);
        if (!UserDefineablePredictorType.CSI_FINGERID.contains(predictorType))
            throw new IllegalArgumentException("Illegal Predicortype for this object. CSI:FingerID positive and negative only.");
    }

    @Override
    public FingerblastScoring<?> getPreparedFingerblastScorer(ParameterStore parameters) {
        BayesnetScoringWithDynamicComputation scorer = (BayesnetScoringWithDynamicComputation) fingerblastScoring.getScoring();
        scorer.prepare(parameters);
        return scorer;
    }

    public MaskedFingerprintVersion getFingerprintVersion() {
        return fpVersion;
    }

    public PredictionPerformance[] getPerformances() {
        return performances;
    }


    private WebWithCustomDatabase getAndRefreshDB(@NotNull final WebAPI<?> csiWebAPI) throws IOException {
        WebWithCustomDatabase db = csiWebAPI.getChemDB();
        db.checkCache();
        return db;
    }


    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized void initialize(@NotNull final WebAPI<?> csiWebAPI) throws IOException {
        if (initialized)
            throw new IllegalStateException("Predictor is already initialized"); //maybe just skip
        //use batch to not request a new client from pool for each rest call
        csiWebAPI.executeBatch((api, client) -> {
            final FingerIdData fingeridData = api.fingerprintClient().getFingerIdData(predictorType, client);
            final CSICovarianceConfidenceScorer<?> confidenceScorerTmp = makeConfidenceScorer(api, client, fingeridData.getPerformances());
            final TrainingStructuresSet trainingStructuresTMP = TrainingStructuresPerPredictor.getInstance()
                    .getTrainingStructuresSet(predictorType, api, client);

            //web requests done assign values
            performances = fingeridData.getPerformances();
            fpVersion = fingeridData.getFingerprintVersion();
            fingerblastScoring = new ScoringMethodFactory.BayesnetScoringWithDynamicComputationScoringMethod();
            confidenceScorer = confidenceScorerTmp;
            trainingStructures = trainingStructuresTMP;
        });

        database = getAndRefreshDB(csiWebAPI);
        initialized = true;
        // do all web stuff first. if one fails we do not have an invalid object state and can just retry everything
    }

    private CSICovarianceConfidenceScorer<?> makeConfidenceScorer(@NotNull final WebAPI.Clients api, @NotNull OkHttpClient client, @NotNull final PredictionPerformance[] performances) throws IOException {
        try {
            final Map<String, TrainedSVM> confidenceSVMs = api.fingerprintClient().getTrainedConfidence(predictorType, client);
            if (confidenceSVMs == null || confidenceSVMs.isEmpty())
                throw new IllegalStateException("WebAPI returned empty confidence SVMs");

            ScoringMethodFactory.BayesnetScoringWithDynamicComputationScoringMethod cvs = new ScoringMethodFactory.BayesnetScoringWithDynamicComputationScoringMethod();
            if (cvs == null)
                throw new IllegalStateException(("WebAPI returned no default bayesian network."));

            final ScoringMethodFactory.CSIFingerIdScoringMethod csiScoring = new ScoringMethodFactory.CSIFingerIdScoringMethod(performances);

            return new CSICovarianceConfidenceScorer<>(confidenceSVMs, cvs);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error when loading confidence SVMs or the bayesian network. Confidence SCore will not be available!");
            LoggerFactory.getLogger(getClass()).debug("Error when loading confidence SVMs or the bayesian network.", e);
            return null;
        }
    }
}
