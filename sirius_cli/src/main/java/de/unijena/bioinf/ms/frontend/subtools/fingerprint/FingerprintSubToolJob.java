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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.utils.PicoUtils;
import de.unijena.bioinf.projectspace.FCandidate;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.spectraldb.InjectSpectralLibraryMatchFormulas;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.SpectralSearchResults;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Subtooljob for CSI:FingerID fingerprint prediction
 */
public class FingerprintSubToolJob extends InstanceJob {

    public FingerprintSubToolJob(JobSubmitter submitter) {
        super(submitter);
        asWEBSERVICE();
    }

    @Override
    public boolean isAlreadyComputed(@NotNull Instance inst) {
        return inst.hasFingerprintResult();
    }

    @Override
    protected void computeAndAnnotateResult(final @NotNull Instance inst) throws Exception {
        List<FCandidate<?>> inputData = inst.getFTrees();

        List<SScored<FTree, FormulaScore>> formulaResults = inputData.stream()
                .filter(f -> f.hasAnnotation(FTree.class))
                .map(FCandidate::asScoredFtree).toList();

        Map<FTree, FCandidate<?>> treeToId = inputData.stream()
                .filter(f -> f.hasAnnotation(FTree.class))
                .collect(Collectors.toMap(f -> f.getAnnotationOrThrow(FTree.class), f -> f));


        Set<MolecularFormula> enforcedFormulas = extractEnforcedFormulasFromSpectralLibrarySearch(inst);

        checkForInterruption();

        if (formulaResults.isEmpty()) {
            logInfo("Skipping instance \"" + inst.getName() + "\" because there are no trees computed.");
            return;
        }

        checkFingerprintCompatibilityOrThrow();

        checkForInterruption();

        // add CSIClientData to PS if it is not already there
        NetUtils.tryAndWait(() -> inst.getProjectSpaceManager().writeFingerIdDataIfMissing(ApplicationCore.WEB_API), this::checkForInterruption);

        updateProgress(10);
        checkForInterruption();

        final @NotNull CSIPredictor csi = NetUtils.tryAndWait(() -> (CSIPredictor) ApplicationCore.WEB_API.
                getStructurePredictor(inst.getIonType().getCharge()), this::checkForInterruption);

        checkForInterruption();

        updateProgress(20);
        //apply soft thresholding to select list of formula to compute
        final FingerprintPreprocessingJJob fpPreproJob = new FingerprintPreprocessingJJob(inst.getExperiment(), formulaResults, enforcedFormulas);

        // do computation and await results
        @NotNull List<SScored<FTree, FormulaScore>> filteredResults = submitSubJob(fpPreproJob).awaitResult();
        if (filteredResults.isEmpty()) {
            logInfo("Skipping instance \"" + inst.getName() + "\" because there is not suitable fragmentation tree left after FingerprintPreprocessingJob.");
            return;
        }

        updateProgress(30);
        checkForInterruption();

        // prediction jobs: predict fingerprints via webservice
        final FingerprintJJob fpPredictJob = submitSubJob(FingerprintJJob.of(csi, ApplicationCore.WEB_API, inst.getExperiment(), filteredResults.stream().map(SScored::getCandidate)));

        updateProgress(35);
        List<FingerIdResult> result = fpPredictJob.awaitResult();

        updateProgress(70);
        checkForInterruption();

        // ############### Make results persistent ####################
        result.stream().filter(fidr -> fidr.hasAnnotation(FingerprintResult.class))
                .forEach(fidr -> treeToId.get(fidr.getSourceTree()).annotate(fidr.getAnnotationOrThrow(FingerprintResult.class)));

        updateProgress(90);
        checkForInterruption();

        //annotate FingerIdResults to FormulaResult
        inst.saveFingerprintResult(inputData);
        updateProgress(97);
    }

    @Override
    public boolean needsProperIonizationMode() {
        return true;
    }

    private Set<MolecularFormula> extractEnforcedFormulasFromSpectralLibrarySearch(Instance inst) {
        InjectSpectralLibraryMatchFormulas injectionSettings = inst.getExperiment().getAnnotationOrDefault(InjectSpectralLibraryMatchFormulas.class);
        if (!injectionSettings.isAlwaysPredict()) return Set.of();

        List<SpectralSearchResult.SearchResult> matches = inst.getSpectraMatches();
        if (matches != null && !matches.isEmpty())
            return SpectralSearchResults.deriveDistinctFormulaSetWithThreshold(matches, inst.getIonMass(), injectionSettings.getMinScoreToInject(), injectionSettings.getMinPeakMatchesToInject());

        return Set.of();
    }

    @Override
    public String getToolName() {
        return PicoUtils.getCommand(FingerprintOptions.class).name();
    }
}
