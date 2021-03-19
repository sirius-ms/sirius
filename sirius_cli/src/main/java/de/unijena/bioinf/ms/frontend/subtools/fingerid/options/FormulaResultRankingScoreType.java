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

package de.unijena.bioinf.ms.frontend.subtools.fingerid.options;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

/**
 * Specifies the Score that is used to rank the list Molecular Formula Identifications
 * before CSI:FingerID predictions are calculated.
 */

public enum FormulaResultRankingScoreType {
    // just to define a selection of scores that are selectable by the user
    AUTO(null),
    TREE(TreeScore.class),
    ISOTOPE(IsotopeScore.class),
    SIRIUS(SiriusScore.class),
    ZODIAC(ZodiacScore.class);

    private final Class<? extends FormulaScore> clazz;

    FormulaResultRankingScoreType(Class<? extends FormulaScore> clazz) {
        this.clazz = clazz;
    }

    public String clazzName() {
        return clazz().getName();
    }

    public String simpleClazzName() {
        return Score.simplify(clazz);
    }

    public Class<? extends FormulaScore> clazz() {
        return clazz;
    }

    public boolean isDefined() {
        return !this.equals(AUTO);
    }
}
