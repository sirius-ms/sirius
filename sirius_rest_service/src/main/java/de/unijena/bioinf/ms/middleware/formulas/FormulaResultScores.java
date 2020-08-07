/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.formulas;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.sirius.scores.SiriusScore;

public class FormulaResultScores {
    private Double siriusScore;
    private Double zodiacScore;
    private Double topCSIScore;
    private Double confidenceScore;

    private Boolean canopusResult = null;

    public FormulaResultScores(FormulaScoring scoring) {
        this.siriusScore = scoring.getAnnotation(SiriusScore.class).map(FormulaScore::score).orElse(null);
        this.zodiacScore = scoring.getAnnotation(ZodiacScore.class).map(FormulaScore::score).orElse(null);
        this.topCSIScore = scoring.getAnnotation(TopCSIScore.class).map(FormulaScore::score).orElse(null);
        this.confidenceScore = scoring.getAnnotation(ConfidenceScore.class).map(FormulaScore::score).orElse(null);
    }

    public Double getSiriusScore() {
        return siriusScore;
    }

    public Double getZodiacScore() {
        return zodiacScore;
    }

    public Double getTopFingerblastScore() {
        return topCSIScore;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public Boolean getCanopusResult() {
        return canopusResult;
    }

    public void setCanopusResult(Boolean canopusResult) {
        this.canopusResult = canopusResult;
    }
}
