

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

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.RestWithCustomDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.StructurePredictor;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Frontend WebAPI class, that represents the client to our backend rest api
 */

@ThreadSafe
public interface WebAPI<D extends AbstractChemicalDatabase> {


    void shutdownJobWatcher();

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
    List<JobUpdate<?>> updateJobStates(JobTable jobTable) throws IOException;

    EnumMap<JobTable, List<JobUpdate<?>>> updateJobStates(Collection<JobTable> jobTablesToCheck) throws IOException;

    void deleteJobs(Collection<JobId> jobsToDelete) throws IOException;

    void deleteClientAndJobs() throws IOException;
    //endregion

    //region ChemDB
    default RestWithCustomDatabase getChemDB() {
        return SearchableDatabases.makeRestWithCustomDB(this);
    }

    void consumeStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOConsumer<D> doWithClient) throws IOException;

    <T> T applyStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOFunction<D, T> doWithClient) throws IOException;

    //endregion

    //region Canopus
    WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(MolecularFormula formula, int charge, ProbabilityFingerprint fingerprint) throws IOException;

    WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(MolecularFormula formula, ProbabilityFingerprint fingerprint, PredictorType type) throws IOException;

    WebJJob<CanopusJobInput, ?, CanopusResult, ?> submitCanopusJob(CanopusJobInput input) throws IOException;

    public CanopusData getCanopusdData(@NotNull PredictorType predictorType) throws IOException;
    //endregion

    //region CSI:FingerID
    WebJJob<FingerprintJobInput, ?, FingerprintResult, ?> submitFingerprintJob(final Ms2Experiment experiment, final FTree ftree, @NotNull EnumSet<PredictorType> types) throws IOException;

    WebJJob<FingerprintJobInput, ?, FingerprintResult, ?> submitFingerprintJob(FingerprintJobInput input) throws IOException;

    @NotNull StructurePredictor getStructurePredictor(int charge) throws IOException;

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
    BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType) throws IOException;

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
    InChI[] getTrainingStructures(PredictorType predictorType) throws IOException;
    //endRegion

    //region FingerprintVersions

    /**
     * @return The MaskedFingerprint used by CSI:FingerID for a given Charge
     * @throws IOException if connection error happens
     */
    MaskedFingerprintVersion getCDKMaskedFingerprintVersion(final int charge) throws IOException;

    /**
     * @return The MaskedFingerprint version used the Canopus predictor
     * @throws IOException if connection error happens
     */
    MaskedFingerprintVersion getClassifierMaskedFingerprintVersion(final int charge) throws IOException;

    /**
     * @return The Fingerprint version used by the rest Database --  not really needed but for sanity checks
     * @throws IOException if connection error happens
     */
    CdkFingerprintVersion getCDKChemDBFingerprintVersion() throws IOException;

    //endregion
}
