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

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.ChemicalAMQPDatabase;
import de.unijena.bioinf.confidence_score.svm.TrainedSVM;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.amqp.client.AmqpClient;
import de.unijena.bioinf.ms.amqp.client.AmqpClients;
import de.unijena.bioinf.ms.amqp.client.jobs.AmqpWebJJob;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.ms.rest.model.worker.WorkerList;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class AmqpAPI implements WebAPI<ChemicalAMQPDatabase> {
    private final AmqpClient amqpClient;

    public AmqpAPI(AmqpClient amqpClient) {
        this.amqpClient = amqpClient;
        this.amqpClient.startConsuming(30000);
    }

    @Override
    public @Nullable VersionsInfo getVersionInfo() {
        //todo request via data service as static file like the models?
        return null;
    }

    @Override
    public int checkConnection() {
        return 0;
    }

    @Override
    public WorkerList getWorkerInfo() throws IOException {
        //todo create rabbitMQ query to request workers available  ->  queue subscribers
        return null;
    }

    @Override
    public <T extends ErrorReport> String reportError(T report, String SOFTWARE_NAME) throws IOException {
        return null;
    }


    @Override
    public void deleteClientAndJobs() throws IOException {
        //todo send message to delete que and stuff
    }

    @Override
    public void consumeStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOConsumer<ChemicalAMQPDatabase> doWithClient) throws IOException {

    }

    @Override
    public <T> T applyStructureDB(long filter, @Nullable File cacheDir, IOFunctions.IOFunction<ChemicalAMQPDatabase, T> doWithClient) throws IOException {
        return null;
    }

    @Override
    public AmqpWebJJob<CanopusJobInput, ?, CanopusResult> submitCanopusJob(CanopusJobInput input) throws IOException {
        final MaskedFingerprintVersion version = getClassifierMaskedFingerprintVersion(input.predictor.toCharge());
        return amqpClient.publish(AmqpClients.jobRoutePrefix("canopus", input.predictor.isPositive()),
                input, (id) -> new AmqpWebJJob<>(id, input, new CanopusWebResultConverter(version, MaskedFingerprintVersion.allowAll(NPCFingerprintVersion.get()))));

    }

    @Override
    public CanopusData getCanopusdData(@NotNull PredictorType predictorType) throws IOException {
        return null; //todo
    }

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
    public @NotNull StructurePredictor getStructurePredictor(@NotNull PredictorType type) throws IOException {
        return null;
    }

    @Override
    public FingerIdData getFingerIdData(@NotNull PredictorType predictorType) throws IOException {
        return null;
    }

    @Override
    public WebJJob<CovtreeJobInput, ?, BayesnetScoring, ?> submitCovtreeJob(@NotNull MolecularFormula formula, @NotNull PredictorType predictorType) throws IOException {
        final CovtreeJobInput input = new CovtreeJobInput(formula.toString(), predictorType);
        final MaskedFingerprintVersion fpVersion = getFingerIdData(predictorType).getFingerprintVersion();
        final PredictionPerformance[] performances = getFingerIdData(predictorType).getPerformances();
        return amqpClient.publish(AmqpClients.jobRoutePrefix("bayestree", input.predictor.isPositive()),
                input, (id) -> new AmqpWebJJob<>(id, input, new CovtreeWebResultConverter(fpVersion, performances)));
    }

    @Override
    public BayesnetScoring getBayesnetScoring(@NotNull PredictorType predictorType, @Nullable MolecularFormula formula) throws IOException {
        return null;
    }

    @Override
    public Map<String, TrainedSVM> getTrainedConfidence(@NotNull PredictorType predictorType) throws IOException {
        return null;
    }

    @Override
    public InChI[] getTrainingStructures(PredictorType predictorType) throws IOException {
        return new InChI[0];
    }

    @Override
    public CdkFingerprintVersion getCDKChemDBFingerprintVersion() throws IOException {
        return null;
    }


}
