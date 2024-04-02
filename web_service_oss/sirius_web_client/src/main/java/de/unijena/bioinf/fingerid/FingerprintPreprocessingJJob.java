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
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.fingerid.annotations.FormulaResultThreshold;
import de.unijena.bioinf.jjobs.BasicJJob;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Preprocessing JJob to filter and prepare IdentificationResults for Fingerprint prediction.
 * This includes:
 * 1. filtering of insufficient FragTrees
 * 2. Soft thresholding of Formula Candidates
 * 3. Expansion of Adducts
 * <p>
 * SINGLE THREADED: because multi threading might not have a big impact here
 */
public class FingerprintPreprocessingJJob extends BasicJJob<List<SScored<FTree, FormulaScore>>> {
    // input data
    private Ms2Experiment experiment;
    private List<SScored<FTree, FormulaScore>> idResult;
    private Set<MolecularFormula> enforcedFormulas;

    public FingerprintPreprocessingJJob() {
        this(null);
    }

    public FingerprintPreprocessingJJob(@Nullable Ms2Experiment experiment) {
        this(experiment, null, null);
    }

    public FingerprintPreprocessingJJob(@Nullable Ms2Experiment experiment, @Nullable List<SScored<FTree, FormulaScore>> formulaIDResults, @Nullable Set<MolecularFormula> enforcedFormulas) {
        super(JobType.CPU);
        this.experiment = experiment;
        this.idResult = formulaIDResults;
        this.enforcedFormulas = Objects.isNull(enforcedFormulas) ? Collections.EMPTY_SET : enforcedFormulas;
    }

    public void setInput(Ms2Experiment experiment, List<SScored<FTree, FormulaScore>> formulaIDResults, Set<MolecularFormula> enforcedFormulas) {
        notSubmittedOrThrow();
        this.experiment = experiment;
        this.idResult = formulaIDResults;
        this.enforcedFormulas = enforcedFormulas;
    }


    public void setIdentificationResult(List<SScored<FTree, FormulaScore>> results) {
        notSubmittedOrThrow();
        this.idResult = results;
    }

    public void setExperiment(Ms2Experiment experiment) {
        notSubmittedOrThrow();
        this.experiment = experiment;
    }

    public Ms2Experiment getExperiment() {
        return experiment;
    }

    @Override
    protected List<SScored<FTree, FormulaScore>> compute() throws Exception {
        logDebug("Instance '" + experiment.getName() + "': Starting CSI:FingerID Preprocessing.");

        if (this.idResult.isEmpty()) return List.of();

        //sort input with descending score
        final List<SScored<FTree, FormulaScore>> idResult = new ArrayList<>(this.idResult);
        idResult.sort(Comparator.reverseOrder());


        checkForInterruption();
        ArrayList<SScored<FTree, FormulaScore>> filteredResults = new ArrayList<>();
        {
            // WORKAROUND
            boolean isLogarithmic = false;
            for (SScored<FTree, FormulaScore> ir : idResult) {
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
                final SScored<FTree, FormulaScore> top = idResult.get(0);
                assert !Double.isNaN(top.getScore());
                filteredResults.add(top);
                final double threshold = isLogarithmic ? thresholder.calculateThreshold(top.getScore()) : 0.01;
                for (int k = 1, n = idResult.size(); k < n; ++k) {
                    SScored<FTree, FormulaScore> e = idResult.get(k);
                    if (Double.isNaN(e.getScore()) || e.getScore() < threshold) break;
                    if (e.getCandidate() == null || e.getCandidate().numberOfVertices() <= 1) {
                        logDebug("Cannot estimate structure for " + e.getCandidate().getRoot().getFormula() + ". Fragmentation Tree is empty.");
                        continue;
                    }
                    filteredResults.add(e);
                }
                if (!enforcedFormulas.isEmpty()) {
                    //add results that must always be considered
                    Set< SScored<FTree, FormulaScore>> resultsSet = new HashSet<>(filteredResults);
                    idResult.stream().filter(r -> enforcedFormulas.contains(r.getCandidate().getRoot().getFormula())).forEach(resultsSet::add);
                    filteredResults = new ArrayList<>(resultsSet);
                }
            } else {
                filteredResults.addAll(idResult);
            }
        }
        checkForInterruption();
        {
            final Iterator< SScored<FTree, FormulaScore>> iter = filteredResults.iterator();
            while (iter.hasNext()) {
                final  SScored<FTree, FormulaScore> ir = iter.next();
                if (ir.getCandidate().numberOfVertices() < 3) {
                    logWarn("Ignore fragmentation tree for " + ir.getCandidate().getRoot().getFormula() + " because it contains less than 3 vertices.");
                    iter.remove();
                }
            }
        }
        checkForInterruption();
        if (filteredResults.isEmpty()) {
            logWarn("No suitable fragmentation tree left.");
            return List.of();
        }

        return filteredResults;
    }

    @Override
    public String identifier() {
        return super.identifier() + " | " + experiment.getName() + "@" + experiment.getIonMass() + "m/z";
    }
}
