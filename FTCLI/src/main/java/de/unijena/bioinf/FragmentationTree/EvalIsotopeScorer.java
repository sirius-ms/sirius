/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.PatternGenerator;

/**
 * TODO: Push into separate branch "newScores2013"
 */
public class EvalIsotopeScorer implements MolecularFormulaScorer{

    private MolecularFormula correctFormula;
    private SimpleSpectrum correctPattern;

    public EvalIsotopeScorer() {
    }

    public EvalIsotopeScorer(MolecularFormula correctFormula) {
        setCorrectFormula(correctFormula);
    }

    @Override
    public double score(MolecularFormula formula) {
        final SimpleSpectrum spec = simulatePattern(formula);
        final int minN = Math.min(spec.size(), correctPattern.size());
        final int maxN = Math.max(spec.size(), correctPattern.size());
        double sum = 0d;
        for (int i=0; i < minN; ++i) {
            sum += Math.abs(correctPattern.getIntensityAt(i)-spec.getIntensityAt(i));
        }
        for (int i=minN; i < maxN; ++i) {
            if (i < spec.size()) sum += spec.getIntensityAt(i);
            if (i < correctPattern.size()) sum += correctPattern.getIntensityAt(i);
        }
        return 2d-sum;
    }

    public MolecularFormula getCorrectFormula() {
        return correctFormula;
    }

    public void setCorrectFormula(MolecularFormula correctFormula) {
        this.correctFormula = correctFormula;
        this.correctPattern = simulatePattern(correctFormula);
    }

    private static SimpleSpectrum simulatePattern(MolecularFormula correctFormula) {
        final PatternGenerator gen = new PatternGenerator(Normalization.Sum(1d));
        return new SimpleSpectrum(gen.generatePatternWithTreshold(correctFormula, 0.01d));
    }


}
