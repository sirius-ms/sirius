

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

package de.unijena.bioinf.webapi;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.StructurePredictor;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.TrainingData;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

/**
 * Frontend WebAPI class, that represents the client to our backend rest api
 */

@ThreadSafe
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

    String getSignUpURL();

    boolean deleteAccount();


    //region ServerInfo

    //6 csi web api for this version is not reachable because it is outdated
    //5 csi web api for this version is not reachable
    //4 csi server not reachable
    //3 no connection to bioinf web site
    //2 no connection to uni jena
    //1 no connection to internet (google/microft/ubuntu????)
    //0 everything is fine
    int MAX_STATE = 6;

    @Nullable VersionsInfo getVersionInfo();

    int checkConnection();

    WorkerList getWorkerInfo() throws IOException;

    <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME) throws IOException;
    //endregion

    //region Jobs
    void deleteClientAndJobs() throws IOException;
    //endregion

    //region ChemDB
    default WebWithCustomDatabase getChemDB() {
        return SearchableDatabases.makeWebWithCustomDB(this);
    }

    void consumeStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOConsumer<D> doWithClient) throws IOException;

    <T> T applyStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOFunction<D, T> doWithClient) throws IOException;

    //endregion

    //region Canopus
    default WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(MolecularFormula formula, int charge, ProbabilityFingerprint fingerprint) throws IOException {
        return submitCanopusJob(formula, fingerprint, (charge > 0 ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE));
    }

    default WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(MolecularFormula formula, ProbabilityFingerprint fingerprint, PredictorType type) throws IOException {
        return submitCanopusJob(new CanopusJobInput(formula.toString(), fingerprint.toProbabilityArrayBinary(), type));
    }


    WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(CanopusJobInput input) throws IOException;

    CanopusData getCanopusdData(@NotNull PredictorType predictorType) throws IOException;
    //endregion

    //region CSI:FingerID
    default WebJJob<FingerprintJobInput, ?, FingerprintResult, ?> submitFingerprintJob(final Ms2Experiment experiment, final FTree ftree, @NotNull EnumSet<PredictorType> types) throws IOException {
        return submitFingerprintJob(new FingerprintJobInput(experiment, null, ftree, types));
    }

    WebJJob<FingerprintJobInput, ?, FingerprintResult, ?> submitFingerprintJob(FingerprintJobInput input) throws IOException;

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
    default BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType) throws IOException{
        return getBayesnetScoring(predictorType,null);
    }

    /**
     * @param predictorType pos or neg
     * @param formula       Molecular formula for which the tree is requested (Default tree will be used if formula is null)
     * @return {@link BayesnetScoring} for the given {@link PredictorType} and {@link MolecularFormula}
     * @throws IOException if something went wrong with the web query
     */
    //uncached -> access via predictor
    BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType, @Nullable MolecularFormula formula) throws IOException;


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
        return getCanopusdData(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(charge)).getFingerprintVersion();
    }

    /**
     * @return The Fingerprint version used by the rest Database --  not really needed but for sanity checks
     * @throws IOException if connection error happens
     */
    CdkFingerprintVersion getCDKChemDBFingerprintVersion() throws IOException;

    String getChemDbDate();

    //endregion
}
