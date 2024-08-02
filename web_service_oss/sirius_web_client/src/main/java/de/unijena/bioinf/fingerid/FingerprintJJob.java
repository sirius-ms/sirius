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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.webapi.WebJJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Executing {@link WebJJob}s for a given set of
 * {@link IdentificationResult}s/{@link FingerIdResult}s from a given {@link Ms2Experiment}.
 * <p>
 * This Job is just a Scheduler do wrap all predictions jobs of a single experiment into one single job.
 */
public class FingerprintJJob extends BasicJJob<List<FingerIdResult>> {
    // structure Elucidator
    private final CSIPredictor predictor;
    private final WebAPI<?> webAPI;

    // input data
    private Ms2Experiment experiment;
    private List<FingerIdResult> idResult;
    private Map<WebJJob<FingerprintJobInput, ?, FingerprintResult, ?>, FingerIdResult> predictionJobs = null;

    public FingerprintJJob(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable Ms2Experiment experiment, @Nullable List<FingerIdResult> preprocessed) {
        super(JobType.WEBSERVICE);
        this.predictor = predictor;
        this.experiment = experiment;
        this.idResult = preprocessed;
        this.webAPI = webAPI;
    }

    public void setInput(Ms2Experiment experiment, List<FingerIdResult> formulaIDResults) {
        notSubmittedOrThrow();
        this.experiment = experiment;
        this.idResult = formulaIDResults;
    }


    public void setFingerIdResultInput(List<FingerIdResult> results) {
        notSubmittedOrThrow();
        this.idResult = results;
    }

    public void setExperimentInput(Ms2Experiment experiment) {
        notSubmittedOrThrow();
        this.experiment = experiment;
    }

    @Override
    protected List<FingerIdResult> compute() throws Exception {
        logDebug("Instance '" + experiment.getName() + "': Starting CSI:FingerID fingerprint prediction.");
        if ((experiment.getPrecursorIonType().getCharge() > 0) != (predictor.predictorType.isPositive())){
            throw new IllegalArgumentException("Charges of predictor and instance are not equal. Instance IonType = " + experiment.getPrecursorIonType() + " | Selected Predictor = " + predictor.predictorType);
        }

        if (this.idResult == null || this.idResult.isEmpty()) {
            logWarn("No suitable input trees found.");
            return List.of();
        }

        checkForInterruption();

        logDebug("Submitting CSI:FingerID fingerprint prediction jobs.");
        predictionJobs = new LinkedHashMap<>(idResult.size());

        // spec has to count compounds
        final int specHash = Spectrums.mergeSpectra(experiment.getMs2Spectra()).hashCode();

        checkForInterruption();
        {
            final SpectralPreprocessor spectralPreprocessor = new SpectralPreprocessor();
            for (FingerIdResult fingeridInput : idResult) {
                // prediction job: predict fingerprint
                predictionJobs.put(webAPI.submitFingerprintJob(new FingerprintJobInput(
                        spectralPreprocessor.extractInputFeatures(fingeridInput.getSourceTree(), experiment),
                        EnumSet.of(UserDefineablePredictorType.CSI_FINGERID.toPredictorType(experiment.getPrecursorIonType()))
                ), specHash), new FingerIdResult(fingeridInput.getSourceTree()));
            }
        }
        checkForInterruption();

        logDebug("CSI:FingerID fingerprint predictions DONE!");
        predictionJobs.forEach((k, v) -> v.takeAndAnnotate(k));

        //in linked maps values() collection is not a set -> so we have to make that distinct
        return predictionJobs.values().stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        super.cancel(mayInterruptIfRunning);
        if (predictionJobs != null)
            predictionJobs.keySet().forEach(j -> j.cancel(mayInterruptIfRunning));
        predictionJobs = null;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
    }


    @Override
    public String identifier() {
        return super.identifier() + " | " + experiment.getName() + "@" + experiment.getIonMass() + "m/z";
    }

    public static FingerprintJJob of(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable Ms2Experiment experiment, @NotNull Stream<FTree> trees) {
        return new FingerprintJJob(predictor, webAPI, experiment, trees.map(FingerIdResult::new).collect(Collectors.toList()));
    }
    public static FingerprintJJob of(@NotNull CSIPredictor predictor, @NotNull WebAPI<?> webAPI, @Nullable Ms2Experiment experiment, @NotNull List<FTree> trees) {
        return of(predictor, webAPI, experiment, trees.stream());
    }
}
