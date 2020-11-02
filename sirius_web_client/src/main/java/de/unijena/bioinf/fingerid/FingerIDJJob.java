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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.fingerid.annotations.FormulaResultThreshold;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.annotations.AnnotationJJob;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import de.unijena.bioinf.utils.NetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

// FingerID Scheduler job does not manage dependencies between different  tools.
// this is done by the respective subtooljobs in the frontend
public class FingerIDJJob<S extends FormulaScore> extends BasicMasterJJob<List<FingerIdResult>> {
    // structure Elucidator
    private final CSIPredictor predictor;

    // input data
    private Ms2Experiment experiment;
    private List<IdentificationResult<S>> idResult = null;


    protected Map<IdentificationResult<S>, IdentificationResult<S>> addedIdentificationResults = new HashMap<>();

    // temprorary parameter
    private boolean computeConfidence = true;

    public FingerIDJJob(@NotNull CSIPredictor predictor, boolean confidence) {
        this(predictor, null, confidence);
    }

    public FingerIDJJob(@NotNull CSIPredictor predictor, @Nullable Ms2Experiment experiment, boolean confidence) {
        this(predictor, experiment, null, confidence);
    }

    public FingerIDJJob(@NotNull CSIPredictor predictor, @Nullable Ms2Experiment experiment, @Nullable List<IdentificationResult<S>> formulaIDResults, boolean confidence) {
        super(JobType.SCHEDULER);
        this.predictor = predictor;
        this.experiment = experiment;
        this.idResult = formulaIDResults;
        this.computeConfidence = confidence;
    }

    public void setInput(Ms2Experiment experiment, List<IdentificationResult<S>> formulaIDResults) {
        notSubmittedOrThrow();
        this.experiment = experiment;
        this.idResult = formulaIDResults;
    }


    public void setIdentificationResult(List<IdentificationResult<S>> results) {
        notSubmittedOrThrow();
        this.idResult = results;
    }

    public void setExperiment(Ms2Experiment experiment) {
        notSubmittedOrThrow();
        this.experiment = experiment;
    }

    public Map<IdentificationResult<S>, IdentificationResult<S>> getAddedIdentificationResults() {
        return addedIdentificationResults;
    }

    public Ms2Experiment getExperiment() {
        return experiment;
    }

    @Override
    protected List<FingerIdResult> compute() throws Exception {
        logDebug("Instance '" + experiment.getName() + "': Starting CSI:FingerID Computation.");
        if ((experiment.getPrecursorIonType().getCharge() > 0) != (predictor.predictorType.isPositive()))
            throw new IllegalArgumentException("Charges of predictor and instance are not equal");

        if (this.idResult.isEmpty()) return null;

        //sort input with descending score
        final List<IdentificationResult<S>> idResult = new ArrayList<>(this.idResult);
        idResult.sort(Comparator.reverseOrder());


        // EXPAND LIST for different Adducts
        // expand adduct trees before filtering scores.
        // This is important because zodiac can create diffent scores for adducts that correspond to the same tree
        logDebug("Expanding Identification Results for different Adducts.");
        {
            final PossibleAdducts adducts;
            if (experiment.getPrecursorIonType().isIonizationUnknown()) {
                if (!experiment.hasAnnotation(DetectedAdducts.class))
                    new Ms1Preprocessor().preprocess(experiment);
                adducts = experiment.getPossibleAdductsOrFallback();
            } else {
                adducts = new PossibleAdducts(experiment.getPrecursorIonType());
            }

            final Map<IdentificationResult<S>, IdentificationResult<S>> ionTypes = new HashMap<>();
            final Set<MolecularFormula> neutralFormulas = new HashSet<>();
            for (IdentificationResult<S> ir : idResult)
                neutralFormulas.add(ir.getMolecularFormula());
            for (IdentificationResult<S> ir : idResult) {
                if (ir.getPrecursorIonType().hasNeitherAdductNorInsource()) {
                    for (PrecursorIonType ionType : adducts) {
                        if (!ionType.equals(ir.getTree().getAnnotationOrThrow(PrecursorIonType.class)) && new IonTreeUtils().isResolvable(ir.getTree(), ionType)) {
                            try {
                                IdentificationResult<S> newIr = IdentificationResult.withPrecursorIonType(ir, ionType);
                                if (newIr.getTree().numberOfVertices() >= 3 && (neutralFormulas.add(newIr.getMolecularFormula())))
                                    ionTypes.put(newIr, ir);
                            } catch (IllegalArgumentException e) {
                                logError("Error with instance " + getExperiment().getName() + " and formula " + ir.getMolecularFormula() + " and ion type " + ionType);
                                throw e;
                            }
                        }
                    }
                }
            }

            idResult.addAll(ionTypes.keySet());

            // WORKAROUND: we have to remove the original results if they do not match the ion type
            if (!experiment.getPrecursorIonType().isIonizationUnknown()) {
                if (experiment.getPrecursorIonType().isIntrinsicalCharged()) {
                    // for this special case we do not want to duplicate all the trees
                    // but we also have to ensure not to delete all trees just because they look
                    // identical to the original one
                    //idResult.replaceAll(x->IdentificationResult.withPrecursorIonType(x, experiment.getPrecursorIonType()));
                } else {
                    idResult.removeIf(f -> !f.getPrecursorIonType().equals(experiment.getPrecursorIonType()));
                    ionTypes.keySet().removeIf(f -> !f.getPrecursorIonType().equals(experiment.getPrecursorIonType())); //todo needed?
                }
            }

            idResult.sort(Collections.reverseOrder()); //descending
            addedIdentificationResults = ionTypes;
        }


        final ArrayList<IdentificationResult<S>> filteredResults = new ArrayList<>();
        {
            // WORKAROUND
            boolean isLogarithmic = false;
            for (IdentificationResult<S> ir : idResult) {
                FormulaScore scoreObject = ir.getScoreObject();
                if (scoreObject.getScoreType() == FormulaScore.ScoreType.Logarithmic) {
                    isLogarithmic = true;
                    break;
                }
            }

            final boolean isAllNaN = idResult.stream().allMatch(x -> Double.isNaN(x.getScore()));
            //filterIdentifications list if wanted
            final FormulaResultThreshold thresholder = experiment.getAnnotationOrThrow(FormulaResultThreshold.class);
            if (thresholder.useThreshold() && idResult.size() > 0 && !isAllNaN) {
                logDebug("Filter Identification Results (soft threshold) for CSI:FingerID usage");

                // first filterIdentifications identificationResult list by top scoring formulas
                final IdentificationResult<S> top = idResult.get(0);
                assert !Double.isNaN(top.getScore());
                filteredResults.add(top);
                final double threshold = isLogarithmic ? thresholder.calculateThreshold(top.getScore()) : 0.01;
                for (int k = 1, n = idResult.size(); k < n; ++k) {
                    IdentificationResult<S> e = idResult.get(k);
                    if (Double.isNaN(e.getScore()) || e.getScore() < threshold) break;
                    if (e.getTree() == null || e.getTree().numberOfVertices() <= 1) {
                        logDebug("Cannot estimate structure for " + e.getMolecularFormula() + ". Fragmentation Tree is empty.");
                        continue;
                    }
                    filteredResults.add(e);
                }
            } else {
                filteredResults.addAll(idResult);
            }
        }

        {
            final Iterator<IdentificationResult<S>> iter = filteredResults.iterator();
            while (iter.hasNext()) {
                final IdentificationResult<S> ir = iter.next();
                if (ir.getTree().numberOfVertices() < 3) {
                    logWarn("Ignore fragmentation tree for " + ir.getMolecularFormula() + " because it contains less than 3 vertices.");
                    iter.remove();
                }
            }
        }

        if (filteredResults.isEmpty()) {
            logWarn("No suitable fragmentation tree left.");
            return Collections.emptyList();
        }

        final StructureSearchDB searchDB = experiment.getAnnotationOrThrow(StructureSearchDB.class);

        logDebug("Preparing CSI:FingerID search jobs.");
        ////////////////////////////////////////
        //submit jobs for prediction and blast
        ///////////////////////////////////////
        final PredictorTypeAnnotation predictors = experiment.getAnnotationOrThrow(PredictorTypeAnnotation.class);
        final Map<AnnotationJJob<?, FingerIdResult>, FingerIdResult> annotationJJobs = new LinkedHashMap<>(filteredResults.size());

        // formula job: retrieve fingerprint candidates for specific MF;
        // no SubmitSubJob needed because ist is not a CPU or IO job
        final List<FormulaJob> formulaJobs = filteredResults.stream().map(fingeridInput ->
                new FormulaJob(
                        fingeridInput.getMolecularFormula(),
                        predictor.database,
                        searchDB.searchDBs,
                        fingeridInput.getPrecursorIonType(), true) //todo maybe only if confidence is "enabled"
        ).collect(Collectors.toList());

        jobManager.submitJobsInBatches(formulaJobs);

        int i = 0;
        for (IdentificationResult<S> fingeridInput : filteredResults) {
            final FingerIdResult fres = new FingerIdResult(fingeridInput.getTree());
            // prediction job: predict fingerprint
            final FingerprintPredictionJJob predictionJob = NetUtils.tryAndWait(
                    () -> predictor.csiWebAPI.submitFingerprintJob(
                            new FingerprintJobInput(experiment, fingeridInput, fingeridInput.getTree(), UserDefineablePredictorType.toPredictorTypes(experiment.getPrecursorIonType(), predictors.value))
                    ), this::checkForInterruption
            );

            annotationJJobs.put(predictionJob, fres);

            // fingerblast job: score candidate fingerprints against predicted fingerprint
            final BayesnetScoring bayesnetScoring = NetUtils.tryAndWait(() ->
                    predictor.csiWebAPI.getBayesnetScoring(predictor.predictorType,fingeridInput.getMolecularFormula()),
                    this::checkForInterruption);



            final FingerblastJJob blastJob;
            if(bayesnetScoring != null) {
                blastJob = new FingerblastJJob(predictor,bayesnetScoring);
            }else{
                // bayesnetScoring is null --> make a prepare job which computes the bayessian network (covTree) for the
                // given molecular formula
                blastJob = new FingerblastJJob(predictor);
                final CovtreeWebJJob covTreeJob = NetUtils.tryAndWait(() ->
                        predictor.csiWebAPI.submitCovtreeJob(fingeridInput.getMolecularFormula(),predictor.predictorType),
                        this::checkForInterruption);
                blastJob.addRequiredJob(covTreeJob);
            }

            blastJob.addRequiredJob(formulaJobs.get(i++));
            blastJob.addRequiredJob(predictionJob);
            annotationJJobs.put(submitSubJob(blastJob), fres);

            //confidence job: calculate confidence of scored candidate list
            if (predictor.getConfidenceScorer() != null && computeConfidence) {
                final ConfidenceJJob confidenceJJob = new ConfidenceJJob(predictor, experiment, fingeridInput);
                confidenceJJob.addRequiredJob(blastJob);
                annotationJJobs.put(submitSubJob(confidenceJJob), fres);
            }
        }

        logDebug("Searching with CSI:FingerID");
        /////////////////////////////////////////////////
        //collect results and annotate to fingerid Result
        /////////////////////////////////////////////////
        // Sub Jobs should also be usable without annotation stuff -> So we annotate outside the subjobs compute methods

        // TODO kaidu: das ist total doof so. Nicht jeder Schritt darf abstürzen und der Rest geht einfach weiter.
        // aber es darf auch nicht sein dass, wenn z.B. die Datenbankverbindung off ist, wir keine Fingerprints mehr
        // berechnen können. Sauber wäre es, klar zu definieren welche Jobs abstürzen dürfen und welche nicht.
        // Oder die Jobs bauen ordentliches Fehlerhandling!

        // this loop is just to ensure that we wait until all jobs are finished.
        // the logic if a job can fail or not is done via job dependencies (so above)
        for (var job : annotationJJobs.keySet()) {
            try {
                job.takeAndAnnotateResult(annotationJJobs.get(job));
            } catch (Throwable e) {
                LoggerFactory.getLogger(FingerIDJJob.class).error("Error during fingerid job in step " + job.getClass().getSimpleName() + ". Skip this step but proceed with the other steps.", e);
            }
        }

        logDebug("CSI:FingerID Search DONE!");
        //in linked maps values() collection is not a set -> so we have to make that distinct
        return annotationJJobs.values().stream().distinct().collect(Collectors.toList());
    }

    @Override
    public String identifier() {
        return super.identifier() + " | Instance: " + experiment.toString() + "@" + experiment.getIonMass() + "m/z";
    }
}
