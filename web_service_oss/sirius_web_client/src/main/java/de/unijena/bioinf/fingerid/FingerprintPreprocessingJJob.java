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
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.fingerid.annotations.FormulaResultThreshold;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Preprocessing JJob to filter and prepare IdentificationResults for Fingerprint prediction.
 * This includes:
 * 1. filtering of insufficient FragTrees
 * 2. Soft thresholding of Formula Candidates
 * 3. Expansion of Adducts
 *
 * @param <S> Scoring type of Identification result
 */
//todo currently single threaded because multi threading might not have a big impact here
public class FingerprintPreprocessingJJob<S extends FormulaScore> extends BasicJJob<List<IdentificationResult<S>>> {
    // input data
    private Ms2Experiment experiment;
    private List<IdentificationResult<S>> idResult;
    protected Map<IdentificationResult<S>, IdentificationResult<S>> addedIdentificationResults = Map.of();

    public FingerprintPreprocessingJJob() {
        this(null);
    }

    public FingerprintPreprocessingJJob(@Nullable Ms2Experiment experiment) {
        this(experiment, null);
    }

    public FingerprintPreprocessingJJob(@Nullable Ms2Experiment experiment, @Nullable List<IdentificationResult<S>> formulaIDResults) {
        super(JobType.CPU);
        this.experiment = experiment;
        this.idResult = formulaIDResults;
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
    protected List<IdentificationResult<S>> compute() throws Exception {
        logDebug("Instance '" + experiment.getName() + "': Starting CSI:FingerID Preprocessing.");

        if (this.idResult.isEmpty()) return List.of();

        //sort input with descending score
        final List<IdentificationResult<S>> idResult = new ArrayList<>(this.idResult);
        idResult.sort(Comparator.reverseOrder());


        // EXPAND LIST for different Adducts
        // expand adduct trees before filtering scores.
        // This is important because zodiac can create different scores for adducts that correspond to the same tree

        // skip if there is a single candidate with lipid annotation (El gordo)
        if (idResult.size() != 1 || !idResult.get(0).getTree().hasAnnotation(LipidSpecies.class)) {
            logDebug("Expanding Identification Results for different Adducts.");
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
            final Set<MolecularFormula> ws = experiment.getAnnotation(Whiteset.class).map(Whiteset::getNeutralFormulas).orElse(null);
            for (IdentificationResult<S> ir : idResult) {
                if (ir.getPrecursorIonType().hasNeitherAdductNorInsource()) {
                    for (PrecursorIonType ionType : adducts) {
                        if (!ionType.equals(ir.getTree().getAnnotationOrThrow(PrecursorIonType.class)) && new IonTreeUtils().isResolvable(ir.getTree(), ionType)) {
                            try {
                                IdentificationResult<S> newIr = IdentificationResult.withPrecursorIonType(ir, ionType, false);
                                if (ws == null || ws.contains(newIr.getMolecularFormula())) {
                                    newIr.getTree().setAnnotation(IonTreeUtils.ExpandedAdduct.class, IonTreeUtils.ExpandedAdduct.EXPANDED);
                                    if (newIr.getTree().numberOfVertices() >= 3 && (neutralFormulas.add(newIr.getMolecularFormula())))
                                        ionTypes.put(newIr, ir);
                                }
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
        } else {
            logDebug("Skip Expanding Identification Results for different Adducts due to existing El Gordo Result.");
        }

        checkForInterruption();
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
        checkForInterruption();
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
