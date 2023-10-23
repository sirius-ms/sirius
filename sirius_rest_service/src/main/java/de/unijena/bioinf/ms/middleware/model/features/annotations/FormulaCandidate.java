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


package de.unijena.bioinf.ms.middleware.model.features.annotations;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Molecular formula candidate that holds a unique identifier (molecular formula + adduct).
 * It can be extended with optional scoring metrics and the raw results
 * such as fragmentation trees and simulated isotope pattern.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormulaCandidate {
    public enum OptFields {statistics, fragmentationTree, simulatedIsotopePattern, predictedFingerprint, compoundClasses, canopusPredictions};

    /**
     * Unique identifier of this formula candidate
     */
    protected String formulaId;
    /**
     * molecular formula of this formula candidate
     */
    protected String molecularFormula;
    /**
     * Adduct of this formula candidate
     */
    protected String adduct;


    //Additional Fields
    /**
     * Sirius Score (isotope + tree score) of the formula candidate.
     * If NULL result is not available
     */
    protected Double siriusScore;
    protected Double isotopeScore;
    protected Double treeScore;
    /**
     * Zodiac Score of the formula candidate.
     * If NULL result is not available
     */
    protected Double zodiacScore;

    protected Integer numOfexplainedPeaks;
    protected Integer numOfexplainablePeaks;

    protected Double totalExplainedIntensity;
    protected Deviation medianMassDeviation;

    /**
     * The fragmentation tree that belongs to this molecular formula candidate (produces the treeScore).
     */
    protected FragmentationTree fragmentationTree;
    /**
     * The simulated isotope pattern that is compared against the measured isotope pattern to produce the isotopeScore.
     */
    protected AnnotatedSpectrum simulatedIsotopePattern;

    /**
     * Probabilistic molecular fingerprint predicted by CSI:FingerID
     */
    protected double[] predictedFingerprint;

    /**
     * Most likely compound classes for different levels of each ontology for this FormulaCandidate (predictedFingerprint)
     */
    protected CompoundClasses compoundClasses;

    /**
     * All classes predicted by canopus for this FormulaCandidate (predictedFingerprint)
     */
    protected CanopusPrediction canopusPrediction;




    //todo add LipidClass prediction

    //todo move to Service layer
    public static FormulaCandidate of(@NotNull FormulaResultId formulaId) {
        return FormulaCandidate.builder()
                .formulaId(formulaId.fileName())
                .molecularFormula(formulaId.getMolecularFormula().toString())
                .adduct(formulaId.getIonType().toString())
                .build();
    }

    public static FormulaCandidate of(@NotNull FormulaResultId formulaId, @Nullable FormulaScoring scorings) {
        final FormulaCandidate frs = of(formulaId);

        if (scorings != null) {
            scorings.getAnnotation(SiriusScore.class).
                    ifPresent(sscore -> frs.setSiriusScore(sscore.score()));
            scorings.getAnnotation(IsotopeScore.class).
                    ifPresent(iscore -> frs.setIsotopeScore(iscore.score()));
            scorings.getAnnotation(TreeScore.class).
                    ifPresent(tscore -> frs.setTreeScore(tscore.score()));
            scorings.getAnnotation(ZodiacScore.class).
                    ifPresent(zscore -> frs.setZodiacScore(zscore.score()));
        }

        return frs;
    }

    public static FormulaCandidate of(@NotNull FormulaResult formulaResult) {
        @NotNull FormulaScoring scorings = formulaResult.getAnnotationOrThrow(FormulaScoring.class);

        final FormulaCandidate frs = of(formulaResult.getId(), scorings);

        formulaResult.getAnnotation(FTree.class).
                ifPresent(fTree -> {
                    final FTreeMetricsHelper metrHelp = new FTreeMetricsHelper(fTree);
                    frs.setNumOfexplainedPeaks(metrHelp.getNumOfExplainedPeaks());
                    frs.setNumOfexplainablePeaks(metrHelp.getNumberOfExplainablePeaks());
                    frs.setTotalExplainedIntensity(metrHelp.getExplainedIntensityRatio());
                    frs.setMedianMassDeviation(metrHelp.getMedianMassDeviation());
                });

        return frs;
    }
}
