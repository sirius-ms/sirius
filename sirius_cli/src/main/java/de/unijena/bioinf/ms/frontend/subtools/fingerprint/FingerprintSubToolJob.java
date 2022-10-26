/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.fingerprint;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerid.predictor_types.PredictorTypeAnnotation;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subtooljob for CSI:FingerID fingerprint prediction
 */
public class FingerprintSubToolJob extends InstanceJob {

    public static final List<Class<? extends DataAnnotation>> formulaResultComponentsToClear = List.of();

    public FingerprintSubToolJob(JobSubmitter submitter) {
        super(submitter);
        asWEBSERVICE();
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.loadCompoundContainer().hasResults() && inst.loadFormulaResults(FingerprintResult.class).stream().map(SScored::getCandidate).anyMatch(c -> c.hasAnnotation(FingerprintResult.class));
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<? extends SScored<FormulaResult, ? extends FormulaScore>> formulaResults =
                inst.loadFormulaResults(FormulaScoring.class, FTree.class, FingerprintResult.class);

        checkForInterruption();

        if (formulaResults == null || formulaResults.isEmpty()) {
            logInfo("Skipping instance \"" + inst.getExperiment().getName() + "\" because there are no trees computed.");
            return;
        }

        checkFingerprintCompatibilityOrThrow();

        checkForInterruption();

        // add CSIClientData to PS if it is not already there
        if (inst.getProjectSpaceManager().getProjectSpaceProperty(FingerIdDataProperty.class).isEmpty()) {
            final FingerIdData pos = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE), this::checkForInterruption);
            final FingerIdData neg = NetUtils.tryAndWait(() -> ApplicationCore.WEB_API.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE), this::checkForInterruption);
            inst.getProjectSpaceManager().setProjectSpaceProperty(FingerIdDataProperty.class, new FingerIdDataProperty(pos, neg));
        }

        updateProgress(10);
        checkForInterruption();

        final EnumSet<PredictorType> predictors = inst.getExperiment().getAnnotationOrThrow(PredictorTypeAnnotation.class).toPredictors(inst.getExperiment().getPrecursorIonType().getCharge());
        final @NotNull CSIPredictor csi = NetUtils.tryAndWait(() -> (CSIPredictor) ApplicationCore.WEB_API.getStructurePredictor(predictors.iterator().next()), this::checkForInterruption);

        checkForInterruption();

        updateProgress(20);
        // expand IDResult list with adducts
        final FingerprintPreprocessingJJob<?> fpPreproJob = new FingerprintPreprocessingJJob<>(inst.getExperiment(),
                formulaResults.stream().map(res ->
                        new IdentificationResult<>(res.getCandidate().getAnnotationOrThrow(FTree.class),
                                res.getScoreObject())).collect(Collectors.toList()));


        // do computation and await results
        List<? extends IdentificationResult<?>> filteredResults = submitSubJob(fpPreproJob).awaitResult();

        updateProgress(30);
        checkForInterruption();

        // prediction jobs: predict fingerprints via webservice
        final FingerprintJJob fpPredictJob = submitSubJob(FingerprintJJob.of(csi, ApplicationCore.WEB_API, inst.getExperiment(), filteredResults));

        updateProgress(35);
        List<FingerIdResult> result = fpPredictJob.awaitResult();

        updateProgress(70);
        checkForInterruption();

        // ############### Make results persistent ####################
        final Map<FTree, FormulaResult> formulaResultsMap = formulaResults.stream().collect(Collectors.toMap(r -> r.getCandidate().getAnnotationOrThrow(FTree.class), SScored::getCandidate));
        Map<? extends IdentificationResult<?>, ? extends IdentificationResult<?>> addedResults = fpPreproJob.getAddedIdentificationResults();

        updateProgress(80);
        // add new id results to project-space.
        addedResults.forEach((k, v) ->
                inst.newFormulaResultWithUniqueId(k.getTree())
                        .ifPresent(fr -> {
//                            fr.getAnnotationOrThrow(FormulaScoring.class).setAnnotationsFrom(
//                                    formulaResultsMap.get(v.getTree()).getAnnotationOrThrow(FormulaScoring.class));
                            //do not override but only set missing scores (may have different tree/SIRIUS score)
                            FormulaScoring formulaScoring = fr.getAnnotationOrThrow(FormulaScoring.class);
                            final Iterator<Map.Entry<Class<FormulaScore>, FormulaScore>> iter = formulaResultsMap.get(v.getTree()).getAnnotationOrThrow(FormulaScoring.class).annotationIterator();
                            while (iter.hasNext()) {
                                final Map.Entry<Class<FormulaScore>, FormulaScore> e = iter.next();
                                if (!formulaScoring.hasAnnotation(e.getKey())){
                                    formulaScoring.setAnnotation(e.getKey(), e.getValue());
                                }
                            }
                            inst.updateFormulaResult(fr, FormulaScoring.class);

                            formulaResultsMap.put(fr.getAnnotationOrThrow(FTree.class), fr);
                        }));


        assert formulaResultsMap.size() >= result.size();

        updateProgress(90);
        checkForInterruption();

        //annotate FingerIdResults to FormulaResult
        for (FingerIdResult res : result) {
            final FormulaResult formRes = formulaResultsMap.get(res.getSourceTree());
            assert res.getSourceTree() == formRes.getAnnotationOrThrow(FTree.class);

            // annotate results
            formRes.setAnnotation(FingerprintResult.class, res.getAnnotationOrThrow(FingerprintResult.class));
            inst.updateFormulaResult(formRes, FingerprintResult.class);
        }
        inst.updateCompoundID();
        updateProgress(97);
    }

    @Override
    protected Class<? extends DataAnnotation>[] formulaResultComponentsToClear() {
        return formulaResultComponentsToClear.toArray(Class[]::new);
    }


    @Override
    public String getToolName() {
        return PicoUtils.getCommand(FingerprintOptions.class).name();
    }
}
