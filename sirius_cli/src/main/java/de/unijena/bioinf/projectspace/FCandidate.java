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
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public abstract class FCandidate<FID> implements Annotated<DataAnnotation> {

    private final Annotations<DataAnnotation> annotations = new Annotations<>();
    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public abstract MolecularFormula getMolecularFormula();
    public abstract PrecursorIonType getAdduct();

    public abstract FID getId();

    public abstract Double getSiriusScore();
    public abstract Double getIsotopeScore();
    public abstract Double getTreeScore();
    public abstract Double getZodiacScore();

    public FormulaScore getRankingScore(){
        return getZodiacScore() == null ? new SiriusScore(getSiriusScore()) : new ZodiacScore(getZodiacScore());
    }

    public FingerIdResult asFingerIdResult(){
        FingerIdResult fr = new FingerIdResult(getAnnotationOrThrow(FTree.class));
        getAnnotation(FingerprintResult.class).ifPresent(fr::annotate);
        getAnnotation(CanopusResult.class).ifPresent(fr::annotate);
        return fr;
    }

    public SScored<FTree, FormulaScore> asScoredFtree(){
        return new SScored<>(getAnnotationOrThrow(FTree.class), getRankingScore());
    }
}
