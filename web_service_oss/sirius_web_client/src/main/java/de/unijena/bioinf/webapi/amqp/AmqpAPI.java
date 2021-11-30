/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.webapi.amqp;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.LoginException;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.CanopusWebResultConverter;
import de.unijena.bioinf.fingerid.CovtreeWebResultConverter;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.FingerprintWebResultConverter;
import de.unijena.bioinf.fingerid.blast.BayesianScoringUtils;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.blast.BayesnetScoringBuilder;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.amqp.client.AmqpClient;
import de.unijena.bioinf.ms.amqp.client.AmqpClients;
import de.unijena.bioinf.ms.amqp.client.jobs.AmqpWebJJob;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingData;
import de.unijena.bioinf.ms.rest.model.info.LicenseInfo;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.stores.model.CanopusClientDataStore;
import de.unijena.bioinf.ms.stores.model.FingerIdClientDataStore;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.webapi.AbstractWebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class AmqpAPI<WebChemDB extends AbstractChemicalDatabase> extends AbstractWebAPI<WebChemDB> {
    private final AmqpClient amqpClient;

    private FingerIdClientDataStore fingeridModels;
    private CanopusClientDataStore canopusModels;

    private WebChemDB webChemDB;

    public AmqpAPI(@Nullable AuthService authService, AmqpClient amqpClient, FingerIdClientDataStore fingeridModels, CanopusClientDataStore canopusModels, WebChemDB webChemDB) {
        super(authService);
        this.amqpClient = amqpClient;
        this.amqpClient.startConsuming(30000);
        this.webChemDB = webChemDB;
        this.fingeridModels = fingeridModels;
        this.canopusModels = canopusModels;
    }

    @Override
    public String getSignUpURL() {
        return null;//todo implement
    }

    @Override
    public boolean deleteAccount() {
        return false; //todo implement
    }

    @Override
    public void acceptTermsAndRefreshToken() throws LoginException {
//todo implement
    }

    @Override
    public void changeHost(URI host) {
//todo implement
    }

    @Override
    public @Nullable List<Term> getTerms() {
        //todo implement
        return null;
    }

    @Override
    public LicenseInfo getLicenseInfo() throws IOException {
        //todo implement
        return null;
    }

    @Override
    public @Nullable VersionsInfo getVersionInfo() {
        //todo request via data service as static file like the models?
        return new VersionsInfo(FingerIDProperties.sirius_guiVersion(), getChemDbDate(), false);
    }

    @Override
    @Nullable
    public String getChemDbDate() {
        try {
            return webChemDB.getChemDbDate();
        } catch (ChemicalDatabaseException e) {
            LoggerFactory.getLogger(getClass()).error("Could not retrieve Chemical database version from web-based chemical database", e);
            return null;
        }
    }

    @Override
    public int checkConnection() {
        return amqpClient.isConnected() ? 0 : 1;
    }

    @Override
    public WorkerList getWorkerInfo() throws IOException {
        //todo create rabbitMQ query to request workers available  ->  queue subscribers
        return new WorkerList();
    }

    @Override
    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME) throws IOException {
        //todo implement
        throw new UnsupportedOperationException("Error reporting is not yet implemented!");
    }


    @Override
    public void deleteClientAndJobs() throws IOException {
        //todo send message to delete que and stuff
    }

    @Override
    public int getCountedJobs(boolean byMonth) throws IOException {
        //todo implement
        return 0;
    }

    @Override
    public int getCountedJobs(@NotNull Date monthAndYear, boolean byMonth) throws IOException {
        //todo implement
        return 0;
    }

    @Override
    public void consumeStructureDB(long filter, @Nullable BlobStorage cacheDir, IOFunctions.IOConsumer<WebChemDB> doWithClient) throws IOException {
        doWithClient.accept(webChemDB);
    }

    @Override
    public <T> T applyStructureDB(long filter, @Nullable BlobStorage cacheDir, IOFunctions.IOFunction<WebChemDB, T> doWithClient) throws IOException {
        return doWithClient.apply(webChemDB);
    }

    @Override
    public AmqpWebJJob<CanopusJobInput, ?, CanopusResult> submitCanopusJob(CanopusJobInput input, @Nullable Integer countingHash) throws IOException {
        System.out.println("TODO: handle counting hash when using AMQP protocol!!!!!!!!!!!!!!!!!!");
        final MaskedFingerprintVersion version = getClassifierMaskedFingerprintVersion(input.predictor.toCharge());
        AmqpWebJJob<CanopusJobInput, CanopusJobOutput, CanopusResult> job = amqpClient.publish(AmqpClients.jobRoutePrefix("canopus", input.predictor.isPositive()),
                input, (id) -> new AmqpWebJJob<>(id, input, new CanopusWebResultConverter(version, MaskedFingerprintVersion.allowAll(NPCFingerprintVersion.get()))));
        job.setCountingHash(countingHash);
        return job;
    }

    @Override
    protected CanopusCfData getCanopusCfDataUncached(@NotNull PredictorType predictorType) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(canopusModels.getCanopusCfClientData(predictorType)
                .orElseThrow(() -> new IOException("Error when fetching Canopus ClassyFire model Data"))))) {
            return CanopusCfData.read(r);
        }
    }

    @Override
    protected CanopusNpcData getCanopusNpcDataUncached(@NotNull PredictorType predictorType) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(canopusModels.getCanopusNpcClientData(predictorType)
                .orElseThrow(() -> new IOException("Error when fetching Canopus ClassyFire model Data"))))) {
            return CanopusNpcData.read(r);
        }
    }

    //region CSI:FingerID
    @Override
    public AmqpWebJJob<FingerprintJobInput, ?, FingerprintResult> submitFingerprintJob(FingerprintJobInput input) throws IOException {
        //check predictor compatibility
        final int c = input.experiment.getPrecursorIonType().getCharge();
        for (PredictorType type : input.predictors)
            if (!type.isValid(c))
                throw new IllegalArgumentException("Predictor " + type.name() + " is not compatible with charge " + c + ".");
        final MaskedFingerprintVersion version = getCDKMaskedFingerprintVersion(input.experiment.getPrecursorIonType().getCharge());
        return amqpClient.publish(AmqpClients.jobRoutePrefix("fingerprint", input.experiment.getPrecursorIonType().isPositive()),
                input, (id) -> new AmqpWebJJob<>(id, input, new FingerprintWebResultConverter(version)));
    }


    @Override
    protected FingerIdData getFingerIdDataUncached(@NotNull PredictorType predictorType) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                fingeridModels.getFingerIdClientData(predictorType)
                        .orElseThrow(() -> new IOException("Error when fetching CSI:FingerID model Data"))))) {
            return FingerIdData.read(r);
        }
    }

    @Override
    public WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> submitCovtreeJob(@NotNull MolecularFormula formula, @NotNull PredictorType predictorType) throws IOException {
        final CovtreeJobInput input = new CovtreeJobInput(formula.toString(), predictorType);
        final MaskedFingerprintVersion fpVersion = getFingerIdData(predictorType).getFingerprintVersion();
        final PredictionPerformance[] performances = getFingerIdData(predictorType).getPerformances();
        return amqpClient.publish(AmqpClients.jobRoutePrefix("bayestree", input.predictor.isPositive()),
                input, (id) -> new AmqpWebJJob<>(id, input, new CovtreeWebResultConverter(fpVersion, performances)));
    }

    /**
     * @param predictorType pos or neg
     * @param formula Molecular formula for which the tree is requested (Default tree will be used if formula is null)
     * @return {@link BayesnetScoring} for the given {@link PredictorType} and {@link MolecularFormula}
     * @throws IOException if something went wrong with the web query
     */
    @Override
    public BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType, @Nullable MolecularFormula formula) throws IOException {
        final MaskedFingerprintVersion fpVersion = getFingerIdData(predictorType).getFingerprintVersion();
        final PredictionPerformance[] performances = getFingerIdData(predictorType).getPerformances();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fingeridModels.getBayesnetScoringTree(predictorType, formula)
                .orElseThrow(() -> new IOException("Error when fetching Bayesnet Scoring models"))))){
            return BayesnetScoringBuilder.readScoring(br, fpVersion, BayesianScoringUtils.calculatePseudoCount(performances), BayesianScoringUtils.allowOnlyNegativeScores);
        }
    }

    //uncached -> access via predictor
    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull PredictorType predictorType) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fingeridModels.getConfidenceSVMs(predictorType)
                .orElseThrow(() -> new IOException("Error fetching confidence SVMs"))))){
            return TrainedSVM.readSVMs(br);
        }
    }

    //uncached -> access via predictor
    public TrainingData getTrainingStructures(PredictorType predictorType) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fingeridModels.getFingerIdTrainingStructures(predictorType)
                .orElseThrow(() -> new IOException("Error fetching confidence SVMs"))))){
            return TrainingData.readTrainingData(br);
        }
    }
    //endRegion

    //region FingerprintVersions
    /**
     * @return The Fingerprint version used by the rest Database --  not really needed but for sanity checks
     * @throws IOException if connection error happens
     */
    public CdkFingerprintVersion getCDKChemDBFingerprintVersion() throws IOException {
        return new CdkFingerprintVersion(CdkFingerprintVersion.withECFP().getUsedFingerprints()); //todo DUMMY add to amqp api
    }
    //endregion


}
