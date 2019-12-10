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
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.FingerprintPredictionJJob;
import de.unijena.bioinf.fingerid.StructurePredictor;
import de.unijena.bioinf.fingerid.blast.CovarianceScoringMethod;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.jobdb.JobId;
import de.unijena.bioinf.ms.jobdb.JobTable;
import de.unijena.bioinf.ms.jobdb.JobUpdate;
import de.unijena.bioinf.ms.jobdb.fingerid.FingerprintJobData;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.chemdb.ChemDBClient;
import de.unijena.bioinf.ms.rest.fingerid.FingerIdClient;
import de.unijena.bioinf.ms.rest.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.info.InfoClient;
import de.unijena.bioinf.ms.rest.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.jobs.JobsClient;
import de.unijena.bioinf.utils.ProxyManager;
import gnu.trove.list.array.TIntArrayList;
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

    //caches predicors so that we do not have to download the statistics and fingerprint infos every
    //time we need them
    private final Map<PredictorType, StructurePredictor> predictors = new HashMap<>();
    private final WebJobWatcher jobWatcher = new WebJobWatcher(this);

    public final InfoClient serverInfoClient;
    public final JobsClient jobsClient;
    public final ChemDBClient chemDBClient;
    public final FingerIdClient fingerprintClient;


    public WebAPI(@NotNull InfoClient infoClient, JobsClient jobsClient, @NotNull ChemDBClient chemDBClient, @NotNull FingerIdClient fingerIdClient) {
        this.serverInfoClient = infoClient;
        this.jobsClient = jobsClient;
        this.chemDBClient = chemDBClient;
        this.fingerprintClient = fingerIdClient;
    }

    public WebAPI(@NotNull URI host) {
        this(new InfoClient(host), new JobsClient(host), new ChemDBClient(host), new FingerIdClient(host));
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
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
            return serverInfoClient.getVersionInfo(client);
        }
    }

    public int checkConnection() {
        try (final ProxyManager.LockedClosableHttpClient client = ProxyManager.client()) {
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
    }
    //endregion

    //region Jobs
    public List<JobUpdate<?>> updateJobStates(JobTable jobTable) throws IOException {
        return updateJobStates(EnumSet.of(jobTable)).get(jobTable);
    }

    public EnumMap<JobTable, List<JobUpdate<?>>> updateJobStates(Collection<JobTable> jobTablesToCheck) throws IOException {
        return jobsClient.getJobs(jobTablesToCheck, ProxyManager.client());
    }

    public void deleteJobs(Collection<JobId> jobsToDelete) throws IOException {
        jobsClient.deleteJobs(jobsToDelete, ProxyManager.client());
    }

    public void deleteClientAndJobs() throws IOException {
        jobsClient.deleteAllJobs(ProxyManager.client());
    }
    //endregion

    //region ChemDB
    public RESTDatabase getRESTDb(BioFilter bioFilter) {
        return getRESTDb(bioFilter, null);
    }

    public RESTDatabase getRESTDb(BioFilter bioFilter, @Nullable File cacheDir) {
        return new RESTDatabase(cacheDir, bioFilter, chemDBClient, ProxyManager.client());
    }

    public CdkFingerprintVersion getFingerprintVersion() throws IOException {
       return chemDBClient.getFingerprintVersion(ProxyManager.client());
    }
    //endregion

    //region Canopus
    //endregion

    //region CSI:FingerID
    public FingerprintPredictionJJob submitFingerprintJob(final Ms2Experiment experiment, final FTree ftree, @NotNull EnumSet<PredictorType> types) throws IOException {
        return submitFingerprintJob(new FingerprintJobInput(experiment, null, ftree, types));
    }

    public FingerprintPredictionJJob submitFingerprintJob(FingerprintJobInput input) throws IOException {
        final JobUpdate<FingerprintJobData> jobUpdate = fingerprintClient.postJobs(input, ProxyManager.client());
        final MaskedFingerprintVersion version = getFingerprintMaskedVersion(input.experiment.getPrecursorIonType().getCharge());
        return jobWatcher.watchJob(new FingerprintPredictionJJob(input, jobUpdate, version, System.currentTimeMillis(), input.experiment.getName()));
    }


    public @NotNull StructurePredictor getPredictorFromType(PredictorType type) throws IOException {
        StructurePredictor p = predictors.get(type);
        if (p == null) {
            if (UserDefineablePredictorType.CSI_FINGERID.contains(type)) {
                p = new CSIPredictor(type, this);
                ((CSIPredictor) p).initialize();
                predictors.put(type, p);
            } else {
                //todo add IOKR if it is ready
                throw new UnsupportedOperationException("Only CSI:FingerID predictors are available sot far.");
            }
        }
        return p;
    }

    public PredictionPerformance[] getStatistics(PredictorType predictorType, TIntArrayList fingerprintIndizes) throws IOException {
        return fingerprintClient.getStatistics(predictorType, fingerprintIndizes, ProxyManager.client());
    }

    public CovarianceScoringMethod getCovarianceScoring(PredictorType predictorType, FingerprintVersion fpVersion, PredictionPerformance[] performances) throws IOException {
        return fingerprintClient.getCovarianceScoring(predictorType, fpVersion, performances, ProxyManager.client());
    }

    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull PredictorType predictorType) throws IOException {
        return fingerprintClient.getTrainedConfidence(predictorType, ProxyManager.client());
    }

    public InChI[] getTrainingStructures(PredictorType predictorType) throws IOException {
        return fingerprintClient.getTrainingStructures(predictorType, ProxyManager.client());
    }

    /**
     * gives you the MaskedFingerprint used by CSI:FingerID for a given Charge
     */
    public MaskedFingerprintVersion getFingerprintMaskedVersion(final int charge) throws IOException {
        return ((CSIPredictor) getPredictorFromType(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge))).getFingerprintVersion();
    }
    //endRegion





}
