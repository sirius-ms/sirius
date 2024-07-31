

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.webapi;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.LoginException;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.StructurePredictor;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.ms.rest.client.canopus.CanopusClient;
import de.unijena.bioinf.ms.rest.client.chemdb.StructureSearchClient;
import de.unijena.bioinf.ms.rest.client.fingerid.FingerIdClient;
import de.unijena.bioinf.ms.rest.client.info.InfoClient;
import de.unijena.bioinf.ms.rest.client.jobs.JobsClient;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingData;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionConsumables;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobInput;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobOutput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.rest.ConnectionError;
import de.unijena.bioinf.storage.blob.BlobStorage;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static de.unijena.bioinf.chemdb.custom.CustomDataSources.getWebDatabaseCacheStorage;

/**
 * Frontend WebAPI class, that represents the client to our backend rest api
 * THREAD-SAFE
 */

public interface WebAPI<D extends AbstractChemicalDatabase> {

    default void shutdown() throws IOException {
        if (!getAuthService().needsLogin()) {
            LoggerFactory.getLogger(getClass()).info("Try to delete leftover jobs on web server...");
            deleteClientAndJobs();
            LoggerFactory.getLogger(getClass()).info("...Job deletion Done!");
        }
        LoggerFactory.getLogger(getClass()).info("Closing AuthService...");
        getAuthService().close();
        LoggerFactory.getLogger(getClass()).info("AuthService closed");
    }

    AuthService getAuthService();

    void acceptTermsAndRefreshToken() throws LoginException;

    void changeActiveSubscription(Subscription activeSubscription);

    Subscription getActiveSubscription();

    void changeHost(Supplier<URI> hostSupplier);

    //region ServerInfo

    /**
     * 10 Worker Warning
     * 94 LincenseInfo Error
     * 93 Worker Error
     * 92 Secured Endpoint error UNEXPECTED
     * 91 Secured Endpoint error
     * 82 Unsecured web api endpoint not reachable UNEXPECTED
     * 81 Unsecured web api endpoint not reachable
     * 71 Authentication Server error: Login Error
     * 72 Authentication Server error: Token Error
     * 63 Login/Token: Server error no tos and/or pp
     * 62 Login/Token: Privacy Policy not Accepted
     * 61 Login/Token: Terms and Condition not Accepted
     * 52 Login/Token: No active License
     * 51 Login/Token: No Licenses
     * 4 Login/Token: Not Logged in
     * 3 no connection to login server
     * 2 no connection to auth server
     * 1 no connection to internet (google/microsoft/ubuntu?)
     * 0 everything is fine
     *
     * @return version and connectivity information of the webserver
     */
    @Nullable VersionsInfo getVersionInfo(boolean updateInfo) throws IOException;

    @Nullable
    default VersionsInfo getVersionInfo() throws IOException {
        return getVersionInfo(false);
    }

    Map<ConnectionError.Klass, Set<ConnectionError>> checkConnection();
    //endregion

    //region Jobs
    void deleteClientAndJobs() throws IOException;

    SubscriptionConsumables getConsumables(boolean byMonth) throws IOException;

    SubscriptionConsumables getConsumables(@NotNull Date monthAndYear, boolean byMonth) throws IOException;
    //endregion

    //region ChemDB
    WebWithCustomDatabase getChemDB() throws IOException;

    void consumeStructureDB(long filter, @Nullable BlobStorage cache, IOFunctions.IOConsumer<D> doWithClient) throws IOException;

    default void consumeStructureDB(long filter, IOFunctions.IOConsumer<D> doWithClient) throws IOException {
        consumeStructureDB(filter, getWebDatabaseCacheStorage(), doWithClient);
    }

    <T> T applyStructureDB(long filter, @Nullable BlobStorage cache, IOFunctions.IOFunction<D, T> doWithClient) throws IOException;

    default <T> T applyStructureDB(long filter, IOFunctions.IOFunction<D, T> doWithClient) throws IOException {
        return applyStructureDB(filter, getWebDatabaseCacheStorage(), doWithClient);
    }

    //endregion

    //region MsNovelist
    default WebJJob<MsNovelistJobInput, ?, MsNovelistJobOutput, ?> submitMsNovelistJob(MolecularFormula formula, int charge, ProbabilityFingerprint fingerprint, @Nullable Integer countingHash) throws IOException {
        return submitMsNovelistJob(formula, fingerprint, (charge > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE), countingHash);
    }

    default WebJJob<MsNovelistJobInput, ?, MsNovelistJobOutput, ?> submitMsNovelistJob(MolecularFormula formula, ProbabilityFingerprint fingerprint, PredictorType type, @Nullable Integer countingHash) throws IOException {
        return submitMsNovelistJob(new MsNovelistJobInput(formula.toString(), fingerprint.toProbabilityArrayBinary(), type), countingHash);
    }


    WebJJob<MsNovelistJobInput, ?, MsNovelistJobOutput, ?> submitMsNovelistJob(MsNovelistJobInput input, @Nullable Integer countingHash) throws IOException;
    //endregion

    //region Canopus
    default WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(MolecularFormula formula, int charge, ProbabilityFingerprint fingerprint, @Nullable Integer countingHash) throws IOException {
        return submitCanopusJob(formula, fingerprint, (charge > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE), countingHash);
    }

    default WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(MolecularFormula formula, ProbabilityFingerprint fingerprint, PredictorType type, @Nullable Integer countingHash) throws IOException {
        return submitCanopusJob(new CanopusJobInput(formula.toString(), fingerprint.toProbabilityArrayBinary(), type), countingHash);
    }


    WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(CanopusJobInput input, @Nullable Integer countingHash) throws IOException;

    CanopusCfData getCanopusCfData(@NotNull PredictorType predictorType) throws IOException;

    CanopusNpcData getCanopusNpcData(@NotNull PredictorType predictorType) throws IOException;
    //endregion

    //region CSI:FingerID
    WebJJob<FingerprintJobInput, ?, FingerprintResult, ?> submitFingerprintJob(FingerprintJobInput input, @Nullable Integer countingHash) throws IOException;

    @NotNull
    default StructurePredictor getStructurePredictor(int charge) throws IOException {
        return getStructurePredictor(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge));
    }

    @NotNull StructurePredictor getStructurePredictor(@NotNull PredictorType type) throws IOException;


    FingerIdData getFingerIdData(@NotNull PredictorType predictorType) throws IOException;

    // use via predictor/scoring method
    WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> submitCovtreeJob(@NotNull MolecularFormula formula, @NotNull PredictorType predictorType) throws IOException;

    /**
     * @param predictorType pos or neg
     * @return Default (non formula specific) {@link BayesnetScoring} for the given {@link PredictorType}
     * @throws IOException if something went wrong with the web query
     */
    //uncached -> access via predictor
    default BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType) throws IOException {
        return getBayesnetScoring(predictorType,  getFingerIdData(predictorType),null);
    }

    /**
     * @param predictorType pos or neg
     * @param formula       Molecular formula for which the tree is requested (Default tree will be used if formula is null)
     * @return {@link BayesnetScoring} for the given {@link PredictorType} and {@link MolecularFormula}
     * @throws IOException if something went wrong with the web query
     */
    //uncached -> access via predictor
    BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType, FingerIdData csi, @Nullable MolecularFormula formula) throws IOException;


    //uncached -> access via predictor
    Map<String, TrainedSVM> getTrainedConfidence(@NotNull PredictorType predictorType) throws IOException;

    //uncached -> access via predictor
    TrainingData getTrainingStructures(PredictorType predictorType) throws IOException;
    //endRegion

    //region FingerprintVersions

    /**
     * @return The MaskedFingerprint used by CSI:FingerID for a given Charge
     * @throws IOException if connection error happens
     */
    default MaskedFingerprintVersion getCDKMaskedFingerprintVersion(final int charge) throws IOException {
        return getFingerIdData(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge)).getFingerprintVersion();
    }

    /**
     * @return The MaskedFingerprint version used the Canopus predictor
     * @throws IOException if connection error happens
     */
    default MaskedFingerprintVersion getClassifierMaskedFingerprintVersion(final int charge) throws IOException {
        return getCanopusCfData(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge)).getFingerprintVersion();
    }

    /**
     * @return The Fingerprint version used by the rest Database --  not really needed but for sanity checks
     * @throws IOException if connection error happens
     */
    CdkFingerprintVersion getCDKChemDBFingerprintVersion() throws IOException;

    @Nullable
    String getChemDbDate();

    //endregion

    void executeBatch(IOFunctions.BiIOConsumer<Clients, OkHttpClient> doWithApi) throws IOException;
    interface Clients {
        InfoClient serverInfoClient();
        JobsClient jobsClient();
        StructureSearchClient chemDBClient();
        FingerIdClient fingerprintClient();
        CanopusClient canopusClient();
    }
}
