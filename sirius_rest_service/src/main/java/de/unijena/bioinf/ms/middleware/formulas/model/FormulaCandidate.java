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


package de.unijena.bioinf.ms.middleware.formulas.model;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaScoring;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class FormulaCandidate {

    protected Double siriusScore;
    protected Double isotopeScore;
    protected Double treeScore;
    protected Double zodiacScore;

    protected String molecularFormula;
    protected String adduct;

    protected Integer numOfexplainedPeaks;
    protected Integer numOfexplainablePeaks;

    protected Double totalExplainedIntensity;
    protected Deviation medianMassDeviation;

    //todo add LipidClass prediction

    public static FormulaCandidate of(FormulaResult formulaResult){
        final FormulaCandidate frs = new FormulaCandidate();
        @NotNull FormulaScoring scorings = formulaResult.getAnnotationOrThrow(FormulaScoring.class);

        frs.setMolecularFormula(formulaResult.getId().getMolecularFormula().toString());
        frs.setAdduct(formulaResult.getId().getIonType().toString());

        scorings.getAnnotation(SiriusScore.class).
                ifPresent(sscore -> frs.setSiriusScore(sscore.score()));
        scorings.getAnnotation(IsotopeScore.class).
                ifPresent(iscore -> frs.setIsotopeScore(iscore.score()));
        scorings.getAnnotation(TreeScore.class).
                ifPresent(tscore -> frs.setTreeScore(tscore.score()));
        scorings.getAnnotation(ZodiacScore.class).
                ifPresent(zscore -> frs.setZodiacScore(zscore.score()));

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
