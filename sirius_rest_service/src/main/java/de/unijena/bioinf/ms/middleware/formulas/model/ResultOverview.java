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

package de.unijena.bioinf.ms.middleware.formulas.model;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Results that are available for a {@link FormulaResultContainer} represented as boolean or numeric score (if available).
 * NULL scores indicate that the corresponding result is not available.
 */
@Getter
@Setter
@NoArgsConstructor
public class ResultOverview {
    /**
     * Sirius Score (isotope + tree score) of the formula candidate.
     * If NULL result is not available
     */
    private Double siriusScore;
    /**
     * Zodiac Score of the formula candidate.
     * If NULL result is not available
     */
    private Double zodiacScore;
    /**
     * CSI:FingerID Score of the top ranking structure candidate of the structure database search
     * performed for this formula candidate.
     * If NULL structure database result is not available or the structure candidate list is empty.
     */
    private Double topCSIScore;
    /**
     * Confidence Score of the
     * IF NULL structure database result not available a structure candidate hit of another formula candidate is
     * the top ranking structure candidate.
     */
    private Double confidenceScore;

    /**
     * True if Canopus compound class prediction results are available.
     */
    private Boolean canopusResult = null;

    public ResultOverview(FormulaScoring scoring) {
        this.siriusScore = scoring.getAnnotation(SiriusScore.class).map(FormulaScore::score).orElse(null);
        this.zodiacScore = scoring.getAnnotation(ZodiacScore.class).map(FormulaScore::score).orElse(null);
        this.topCSIScore = scoring.getAnnotation(TopCSIScore.class).map(FormulaScore::score).orElse(null);
        this.confidenceScore = scoring.getAnnotation(ConfidenceScore.class).map(FormulaScore::score).orElse(null);
    }
}
