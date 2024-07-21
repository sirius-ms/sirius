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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class SiriusProjectSpaceFCandidate extends FCandidate<FormulaResultId> {
    @Getter
    @NotNull
    private FormulaResultId fid;

    @NotNull
    private FormulaScoring scoring;

    @Override
    public FormulaResultId getId() {
        return fid;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return getId().getMolecularFormula();
    }

    @Override
    public PrecursorIonType getAdduct() {
        return getId().getIonType();
    }

    @Override
    public Double getSiriusScore() {
        return scoring.getAnnotation(SiriusScore.class).map(FormulaScore::score).orElse(null);
    }

    @Override
    public Double getIsotopeScore() {
        return scoring.getAnnotation(IsotopeScore.class).map(FormulaScore::score).orElse(null);
    }

    @Override
    public Double getTreeScore() {
        return scoring.getAnnotation(TreeScore.class).map(FormulaScore::score).orElse(null);
    }

    @Override
    public Double getZodiacScore() {
        return scoring.getAnnotation(ZodiacScore.class).map(FormulaScore::score).orElse(null);
    }

    public static SiriusProjectSpaceFCandidate of(FormulaResult res, Class<? extends DataAnnotation>... components){
        SiriusProjectSpaceFCandidate fc = new SiriusProjectSpaceFCandidate(res.getId(), res.getAnnotationOrThrow(FormulaScoring.class));
        for (Class<? extends DataAnnotation> aClass : components)
            res.getAnnotation(aClass).ifPresent(fc::annotate);
        return fc;
    }
}
