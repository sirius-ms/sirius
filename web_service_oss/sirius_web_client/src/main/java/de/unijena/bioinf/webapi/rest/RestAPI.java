

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

package de.unijena.bioinf.webapi.rest;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.LoginException;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.CanopusWebResultConverter;
import de.unijena.bioinf.fingerid.CovtreeWebResultConverter;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.FingerprintWebResultConverter;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.rest.client.canopus.CanopusClient;
import de.unijena.bioinf.ms.rest.client.chemdb.ChemDBClient;
import de.unijena.bioinf.ms.rest.client.chemdb.StructureSearchClient;
import de.unijena.bioinf.ms.rest.client.fingerid.FingerIdClient;
import de.unijena.bioinf.ms.rest.client.info.InfoClient;
import de.unijena.bioinf.ms.rest.client.jobs.JobsClient;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingData;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.webapi.AbstractWebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Frontend WebAPI class, that represents the client to our backend rest api
 */

@ThreadSafe
public final class RestAPI extends AbstractWebAPI<RESTDatabase> {
    private static final Logger LOG = LoggerFactory.getLogger(RestAPI.class);

    private final WebJobWatcher jobWatcher = new WebJobWatcher(this);

    public final InfoClient serverInfoClient;
    public final JobsClient jobsClient;
    public final StructureSearchClient chemDBClient;
    public final FingerIdClient fingerprintClient;
    public final CanopusClient canopusClient;



    public RestAPI(@Nullable AuthService authService, @NotNull InfoClient infoClient, JobsClient jobsClient, @NotNull ChemDBClient chemDBClient, @NotNull FingerIdClient fingerIdClient, @NotNull CanopusClient canopusClient) {
        super(authService);
        this.serverInfoClient = infoClient;
        this.jobsClient = jobsClient;
        this.chemDBClient = chemDBClient;
        this.fingerprintClient = fingerIdClient;
        this.canopusClient = canopusClient;
    }

    public RestAPI(@NotNull AuthService authService, @NotNull URI host) {
        this(authService, new InfoClient(host), new JobsClient(host, authService), new ChemDBClient(host, authService), new FingerIdClient(host, authService), new CanopusClient(host, authService));
    }

    public RestAPI(@NotNull AuthService authService, @NotNull String host) {
        this(authService, URI.create(host));
    }

    public RestAPI(@NotNull AuthService authService) {
        this(authService, URI.create(FingerIDProperties.fingeridWebHost()));
    }


    public AuthService getAuthService() {
        return authService;
    }

    public String getSignUpURL() {
        try {
            return getAuthService().signUpURL(jobsClient.getBaseURI("/signUp", true).build().toURL().toString());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException("Illegal URL!", e);
        }
    }

    @Override
    public void changeHost(URI host){
        this.serverInfoClient.setServerUrl(host);
        this.jobsClient.setServerUrl(host);
        this.chemDBClient.setServerUrl(host);
        this.fingerprintClient.setServerUrl(host);
        this.canopusClient.setServerUrl(host);
    }


    @Override
    public boolean deleteAccount(){
        return ProxyManager.doWithClient(jobsClient::deleteAccount);
    }


    @Override
    public void shutdown() throws IOException {
            jobWatcher.shutdown();
        super.shutdown();
    }

    @Override
    public void acceptTermsAndRefreshToken() throws LoginException {
        if (ProxyManager.doWithClient(jobsClient::acceptTerms));
            authService.refreshIfNeeded(true);
    }


    //region ServerInfo
    @Nullable
    public VersionsInfo getVersionInfo() {
        return ProxyManager.doWithClient(serverInfoClient::getVersionInfo);
    }

    @Override
    public String getChemDbDate() { //todo this is ugly an should be moved to a separate endpoint in the chemDB client
        @Nullable VersionsInfo v = getVersionInfo();
        return v == null ? null : v.databaseDate;
    }

    @Override
    public int checkConnection() {
        return ProxyManager.doWithClient(client -> {
            try {
                VersionsInfo v = serverInfoClient.getVersionInfo(client);
                if (v == null) {
                    int error = ProxyManager.checkInternetConnection(client);
                    if (error > 0) return error;
                    else return 4;
                } else if (v.outdated()) {
                    return MAX_STATE;
                } else if (serverInfoClient.testConnection()) {
                    return jobsClient.testSecuredConnection(client);
                } else {
                    return 5;
                }
            } catch (Exception e) {
                LOG.error("Error during connection check", e);
                return MAX_STATE;
            }
        });
    }

    public WorkerList getWorkerInfo() throws IOException {
        return ProxyManager.applyClient(serverInfoClient::getWorkerInfo);
    }

    @Nullable
    public List<Term> getTerms() {
        try {
            return ProxyManager.applyClient(serverInfoClient::getTerms);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not load Terms from server!", e);
            return null;
        }
    }

    public LicenseInfo getLicenseInfo() throws IOException {
        return ProxyManager.applyClient(jobsClient::getLicenseInfo);
    }

    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME) throws IOException {
        return ProxyManager.applyClient(client -> serverInfoClient.reportError(report, SOFTWARE_NAME, client));
    }
    //endregion

    //region Jobs
    public int getCountedJobs(boolean byMonth) throws IOException {
        return getCountedJobs(new Date(System.currentTimeMillis()), byMonth);
    }

    public int getCountedJobs(@NotNull Date monthAndYear, boolean byMonth) throws IOException {
        return ProxyManager.applyClient(client -> jobsClient.getCountedJobs(monthAndYear, byMonth, client));
    }

    public List<JobUpdate<?>> updateJobStates(JobTable jobTable) throws IOException {
        return updateJobStates(EnumSet.of(jobTable)).get(jobTable);
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> updateJobStates(Collection<JobTable> jobTablesToCheck) throws IOException {
        return ProxyManager.applyClient(client -> jobsClient.getJobs(jobTablesToCheck, client));
    }

    public void deleteJobs(Collection<JobId> jobsToDelete, Map<JobId, Integer> countingHashes) throws IOException {
        ProxyManager.consumeClient(client -> jobsClient.deleteJobs(jobsToDelete, countingHashes, client));
    }

    public void deleteClientAndJobs() throws IOException {
        ProxyManager.consumeClient(jobsClient::deleteAllJobs);
    }
    //endregion

    //region ChemDB
    public void consumeStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOConsumer<RESTDatabase> doWithClient) throws IOException {
        try (RESTDatabase restDB = new RESTDatabase(cacheDir, filter, getChemDbDate(), chemDBClient, ProxyManager.client())) {
            doWithClient.accept(restDB);
        }
    }

    public <T> T applyStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOFunction<RESTDatabase, T> doWithClient) throws IOException {
        try (RESTDatabase restDB = new RESTDatabase(cacheDir, filter, getChemDbDate(), chemDBClient, ProxyManager.client())) {
            return doWithClient.apply(restDB);
        }
    }

    //endregion

    //region Canopus
    @Override
    public WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(CanopusJobInput input, @Nullable Integer countingHash) throws IOException {
        JobUpdate<CanopusJobOutput> jobUpdate = ProxyManager.applyClient(client -> canopusClient.postJobs(input, client));
        final MaskedFingerprintVersion version = getClassifierMaskedFingerprintVersion(input.predictor.toCharge());
        RestWebJJob<CanopusJobInput, CanopusJobOutput, CanopusResult> job = new RestWebJJob<>(jobUpdate.getID(), input, new CanopusWebResultConverter(version, MaskedFingerprintVersion.allowAll(NPCFingerprintVersion.get())));
        job.setCountingHash(countingHash);
        return jobWatcher.watchJob(job);
    }

    @Override
    protected CanopusCfData getCanopusCfDataUncached(@NotNull PredictorType predictorType) throws IOException {
        return ProxyManager.applyClient(client -> canopusClient.getCfData(predictorType, client));
    }

    @Override
    protected CanopusNpcData getCanopusNpcDataUncached(@NotNull PredictorType predictorType) throws IOException {
        return ProxyManager.applyClient(client -> canopusClient.getNpcData(predictorType, client));
    }
    //endregion

    //region CSI:FingerID
    public WebJJob<FingerprintJobInput, ?, FingerprintResult, ?> submitFingerprintJob(FingerprintJobInput input) throws IOException {
        final JobUpdate<FingerprintJobOutput> jobUpdate = ProxyManager.applyClient(client -> fingerprintClient.postJobs(input, client));
        final MaskedFingerprintVersion version = getCDKMaskedFingerprintVersion(input.experiment.getPrecursorIonType().getCharge());
        return jobWatcher.watchJob(new RestWebJJob<>(jobUpdate.getID(), input, new FingerprintWebResultConverter(version)));
    }

    @Override
    protected FingerIdData getFingerIdDataUncached(@NotNull PredictorType predictorType) throws IOException {
        return ProxyManager.applyClient(client -> fingerprintClient.getFingerIdData(predictorType, client));
    }

    // use via predictor/scoring method
    public WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> submitCovtreeJob(@NotNull MolecularFormula formula, @NotNull PredictorType predictorType) throws IOException {
        final CovtreeJobInput input = new CovtreeJobInput(formula.toString(), predictorType);
        final JobUpdate<CovtreeJobOutput> jobUpdate = ProxyManager.applyClient(client -> fingerprintClient.postCovtreeJobs(input,  client));
        final MaskedFingerprintVersion fpVersion = getFingerIdData(predictorType).getFingerprintVersion();
        final PredictionPerformance[] performances = getFingerIdData(predictorType).getPerformances();
        return jobWatcher.watchJob(new RestWebJJob<>(jobUpdate.getID(), input, new CovtreeWebResultConverter(fpVersion, performances)));
    }


    /**
     * @param predictorType pos or neg
     * @param formula Molecular formula for which the tree is requested (Default tree will be used if formula is null)
     * @return {@link BayesnetScoring} for the given {@link PredictorType} and {@link MolecularFormula}
     * @throws IOException if something went wrong with the web query
     */
    //uncached -> access via predictor
    public BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType, @Nullable MolecularFormula formula) throws IOException {
        final MaskedFingerprintVersion fpVersion = getFingerIdData(predictorType).getFingerprintVersion();
        final PredictionPerformance[] performances = getFingerIdData(predictorType).getPerformances();
        return ProxyManager.applyClient(client -> fingerprintClient.getCovarianceScoring(predictorType, fpVersion, formula, performances, client));
    }


    //uncached -> access via predictor
    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull PredictorType predictorType) throws IOException {
        return ProxyManager.applyClient(client -> fingerprintClient.getTrainedConfidence(predictorType, client));
    }

    //uncached -> access via predictor
    public TrainingData getTrainingStructures(PredictorType predictorType) throws IOException {
        return ProxyManager.applyClient(client -> fingerprintClient.getTrainingStructures(predictorType, client));
    }
    //endRegion

    //region FingerprintVersions
    /**
     * @return The Fingerprint version used by the rest Database --  not really needed but for sanity checks
     * @throws IOException if connection error happens
     */
    public CdkFingerprintVersion getCDKChemDBFingerprintVersion() throws IOException {
        return ProxyManager.applyClient(chemDBClient::getCDKFingerprintVersion);
    }
    //endregion
}
