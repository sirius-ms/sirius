/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.CanopusWebJJob;
import de.unijena.bioinf.fingerid.FingerprintPredictionJJob;
import de.unijena.bioinf.fingerid.StructurePredictor;
import de.unijena.bioinf.fingerid.blast.CovarianceScoringMethod;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.client.canopus.CanopusClient;
import de.unijena.bioinf.ms.rest.client.chemdb.ChemDBClient;
import de.unijena.bioinf.ms.rest.client.fingerid.FingerIdClient;
import de.unijena.bioinf.ms.rest.client.info.InfoClient;
import de.unijena.bioinf.ms.rest.client.jobs.JobsClient;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.utils.ProxyManager;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Frontend WebAPI class, that represents the client to our backend rest api
 */

@ThreadSafe
public final class WebAPI {
    private static final Logger LOG = LoggerFactory.getLogger(WebAPI.class);
    public static long WEB_API_JOB_TIME_OUT = PropertyManager.getLong("de.unijena.bioinf.fingerid.web.job.timeout", 1000L * 60L * 60L); //default 1h

    private final WebJobWatcher jobWatcher = new WebJobWatcher(this);

    public final InfoClient serverInfoClient;
    public final JobsClient jobsClient;
    public final ChemDBClient chemDBClient;
    public final FingerIdClient fingerprintClient;
    public final CanopusClient canopusClient;


    public WebAPI(@NotNull InfoClient infoClient, JobsClient jobsClient, @NotNull ChemDBClient chemDBClient, @NotNull FingerIdClient fingerIdClient, @NotNull CanopusClient canopusClient) {
        this.serverInfoClient = infoClient;
        this.jobsClient = jobsClient;
        this.chemDBClient = chemDBClient;
        this.fingerprintClient = fingerIdClient;
        this.canopusClient = canopusClient;
    }

    public WebAPI(@NotNull URI host) {
        this(new InfoClient(host), new JobsClient(host), new ChemDBClient(host), new FingerIdClient(host), new CanopusClient(host));
    }

    public WebAPI(@NotNull String host) {
        this(URI.create(host));
    }

    public WebAPI() {
        this(URI.create(FingerIDProperties.fingeridWebHost()));
    }

    //region ServerInfo

    //6 csi web api for this version is not reachable because it is outdated
    //5 csi web api for this version is not reachable
    //4 csi server not reachable
    //3 no connection to bioinf web site
    //2 no connection to uni jena
    //1 no connection to internet (google/microft/ubuntu????)
    //0 everything is fine
    public static final int MAX_STATE = 6;

    @Nullable
    public VersionsInfo getVersionInfo() {
        return ProxyManager.doWithClient(serverInfoClient::getVersionInfo);
    }

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
                    return 0;
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

    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME) throws IOException {
        return ProxyManager.applyClient(client -> serverInfoClient.reportError(report, SOFTWARE_NAME, client));
    }
    //endregion

    //region Jobs
    public List<JobUpdate<?>> updateJobStates(JobTable jobTable) throws IOException {
        return updateJobStates(EnumSet.of(jobTable)).get(jobTable);
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> updateJobStates(Collection<JobTable> jobTablesToCheck) throws IOException {
        return ProxyManager.applyClient(client -> jobsClient.getJobs(jobTablesToCheck, client));
    }

    public void deleteJobs(Collection<JobId> jobsToDelete) throws IOException {
        ProxyManager.consumeClient(client -> jobsClient.deleteJobs(jobsToDelete, client));
    }

    public void deleteClientAndJobs() throws IOException {
        ProxyManager.consumeClient(jobsClient::deleteAllJobs);
    }
    //endregion

    //region ChemDB
    public RESTDatabase getRESTDb(BioFilter bioFilter) {
        return getRESTDb(bioFilter, null);
    }

    public RESTDatabase getRESTDb(BioFilter bioFilter, @Nullable File cacheDir) {
        return ProxyManager.doWithClient(client -> new RESTDatabase(cacheDir, bioFilter, chemDBClient, client));
    }
    //endregion

    //region Canopus
    public CanopusWebJJob submitCanopusJob(MolecularFormula formula, int charge, ProbabilityFingerprint fingerprint) throws IOException {
        return submitCanopusJob(formula, fingerprint, (charge > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE));
    }

    public CanopusWebJJob submitCanopusJob(MolecularFormula formula, ProbabilityFingerprint fingerprint, PredictorType type) throws IOException {
        return submitCanopusJob(new CanopusJobInput(formula.toString(), fingerprint.toProbabilityArrayBinary(), type));
    }

    public CanopusWebJJob submitCanopusJob(CanopusJobInput input) throws IOException {
        JobUpdate<CanopusJobOutput> jobUpdate = ProxyManager.applyClient(client -> canopusClient.postJobs(input, client));
        final MaskedFingerprintVersion version = getClassifierMaskedFingerprintVersion(input.predictor.toCharge());
        return jobWatcher.watchJob(new CanopusWebJJob(jobUpdate.getGlobalId(), jobUpdate.getStateEnum(), version, System.currentTimeMillis()));
    }

    private final EnumMap<PredictorType, CanopusData> canopusData = new EnumMap<>(PredictorType.class);

    public final CanopusData getCanopusdData(@NotNull PredictorType predictorType) throws IOException {
        synchronized (canopusData) {
            if (!canopusData.containsKey(predictorType))
                canopusData.put(predictorType, ProxyManager.applyClient(client -> canopusClient.getCanopusData(predictorType, client)));
        }
        return canopusData.get(predictorType);
    }
    //endregion

    //region CSI:FingerID
    public FingerprintPredictionJJob submitFingerprintJob(final Ms2Experiment experiment, final FTree ftree, @NotNull EnumSet<PredictorType> types) throws IOException {
        return submitFingerprintJob(new FingerprintJobInput(experiment, null, ftree, types));
    }

    public FingerprintPredictionJJob submitFingerprintJob(FingerprintJobInput input) throws IOException {
        final JobUpdate<FingerprintJobOutput> jobUpdate = ProxyManager.applyClient(client -> fingerprintClient.postJobs(input, client));
        final MaskedFingerprintVersion version = getCDKMaskedFingerprintVersion(input.experiment.getPrecursorIonType().getCharge());
        return jobWatcher.watchJob(new FingerprintPredictionJJob(input, jobUpdate, version, System.currentTimeMillis(), input.experiment.getName()));
    }

    //caches predicors so that we do not have to download the statistics and fingerprint infos every
    private final EnumMap<PredictorType, StructurePredictor> fingerIdPredictors = new EnumMap<>(PredictorType.class);

    public @NotNull StructurePredictor getStructurePredictor(int charge) throws IOException {
        return getStructurePredictor(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge));
    }

    public @NotNull StructurePredictor getStructurePredictor(@NotNull PredictorType type) throws IOException {
        synchronized (fingerIdPredictors) {
            if (!fingerIdPredictors.containsKey(type)) {
                final CSIPredictor p = new CSIPredictor(type, this);
                p.initialize();
                fingerIdPredictors.put(type, p);
            }
        }
        return fingerIdPredictors.get(type);
    }


    private final EnumMap<PredictorType, FingerIdData> fingerIdData = new EnumMap<>(PredictorType.class);

    public FingerIdData getFingerIdData(@NotNull PredictorType predictorType) throws IOException {
        synchronized (fingerIdData) {
            if (!fingerIdData.containsKey(predictorType))
                fingerIdData.put(predictorType, ProxyManager.applyClient(client -> fingerprintClient.getFingerIdData(predictorType, client)));
        }
        return fingerIdData.get(predictorType);
    }

    //uncached -> access via predictor
    public CovarianceScoringMethod getCovarianceScoring(@NotNull PredictorType predictorType) throws IOException {
        final MaskedFingerprintVersion fpVersion = getFingerIdData(predictorType).getFingerprintVersion();
        final PredictionPerformance[] performances = getFingerIdData(predictorType).getPerformances();
        return ProxyManager.applyClient(client -> fingerprintClient.getCovarianceScoring(predictorType, fpVersion, performances, client));
    }

    //uncached -> access via predictor
    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull PredictorType predictorType) throws IOException {
        return ProxyManager.applyClient(client -> fingerprintClient.getTrainedConfidence(predictorType, client));
    }

    //uncached -> access via predictor
    public InChI[] getTrainingStructures(PredictorType predictorType) throws IOException {
        return ProxyManager.applyClient(client -> fingerprintClient.getTrainingStructures(predictorType, client));
    }
    //endRegion

    //region FingerprintVersions

    /**
     * @return The MaskedFingerprint used by CSI:FingerID for a given Charge
     * @throws IOException if connection error happens
     */
    public MaskedFingerprintVersion getCDKMaskedFingerprintVersion(final int charge) throws IOException {
        return getFingerIdData(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge)).getFingerprintVersion();
    }

    /**
     * @return The MaskedFingerprint version used the Canopus predictor
     * @throws IOException if connection error happens
     */
    public MaskedFingerprintVersion getClassifierMaskedFingerprintVersion(final int charge) throws IOException {
        return getCanopusdData(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge)).getFingerprintVersion();
    }

    /**
     * @return The Fingerprint version used by the rest Database ->  not really needed but for sanity checks
     * @throws IOException if connection error happens
     */
    public CdkFingerprintVersion getCDKChemDBFingerprintVersion() throws IOException {
        return ProxyManager.applyClient(chemDBClient::getCDKFingerprintVersion);
    }
    //endregion
}
