/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.model.annotations;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

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
    @Schema(enumAsRef = true, name = "FormulaCandidateOptField", nullable = true)
    public enum OptField {none, statistics, fragmentationTree, annotatedSpectrum, isotopePattern, lipidAnnotation, predictedFingerprint, compoundClasses, canopusPredictions}

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

    protected Integer rank;
    /**
     * Sirius Score (isotope + tree score) of the formula candidate.
     * If NULL result is not available
     */
    @Schema(nullable = true)
    protected Double siriusScore;
    @Schema(nullable = true)
    protected Double isotopeScore;
    @Schema(nullable = true)
    protected Double treeScore;
    /**
     * Zodiac Score of the formula candidate.
     * If NULL result is not available
     */
    @Schema(nullable = true)
    protected Double zodiacScore;

    //Additional Fields
    @Schema(nullable = true)
    protected Integer numOfExplainedPeaks;
    @Schema(nullable = true)
    protected Integer numOfExplainablePeaks;

    @Schema(nullable = true)
    protected Double totalExplainedIntensity;
    @Schema(nullable = true)
    @JsonDeserialize(using = SimpleSerializers.DeviationDeserializer.class)
    protected Deviation medianMassDeviation;

    /**
     * The fragmentation tree that belongs to this molecular formula candidate (produces the treeScore).
     */
    @Schema(nullable = true)
    protected FragmentationTree fragmentationTree;

    /**
     * Fragmentation spectrum (e.g. Merged MS/MS) which is annotated with fragments and losses
     */
    @Schema(nullable = true)
    protected AnnotatedSpectrum annotatedSpectrum;

    /**
     * The measured and simulated isotope pattern that have been  compared against each other to produce the isotopeScore.
     */
    @Schema(nullable = true)
    protected IsotopePatternAnnotation isotopePatternAnnotation;

    /**
     * ElGordo lipid annotation of this candidate.
     * NULL if annotation was not requested. lipidAnnotation.lipidSpecies == NULL if candidate has not been classified as a lipid
     */
    @Schema(nullable = true)
    protected LipidAnnotation lipidAnnotation;

    /**
     * Probabilistic molecular fingerprint predicted by CSI:FingerID
     */
    @Schema(nullable = true)
    protected double[] predictedFingerprint;

    /**
     * Most likely compound classes for different levels of each ontology for this FormulaCandidate (predictedFingerprint)
     */
    @Schema(nullable = true)
    protected CompoundClasses compoundClasses;

    /**
     * All classes predicted by canopus for this FormulaCandidate (predictedFingerprint)
     */
    @Schema(nullable = true)
    protected CanopusPrediction canopusPrediction;
}
