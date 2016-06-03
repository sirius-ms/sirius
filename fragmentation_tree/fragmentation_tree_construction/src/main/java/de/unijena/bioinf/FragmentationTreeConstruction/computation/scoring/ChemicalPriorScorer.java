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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

@Called("Chemical Prior")
public class ChemicalPriorScorer implements DecompositionScorer<Object> {

    public static final double LEARNED_NORMALIZATION_CONSTANT = 0.17546357436139415d;
    public static final double LEARNED_NORMALIZATION_CONSTANT_FOR_ROOT = 0.43916395724493595d;
    private MolecularFormulaScorer prior;
    private double normalizationConstant, minimalMass;

    public ChemicalPriorScorer() {
        this(ChemicalCompoundScorer.createDefaultCompoundScorer(), LEARNED_NORMALIZATION_CONSTANT, 100d);
    }

    public ChemicalPriorScorer(MolecularFormulaScorer prior, double normalizationConstant) {
        this(prior, normalizationConstant, 100d);
    }

    public ChemicalPriorScorer(MolecularFormulaScorer prior, double normalizationConstant, double minimalMass) {
        assert minimalMass >= 0 && normalizationConstant < 10; // just to be shure that nobody mix both parameters ^^°
        this.prior = prior;
        this.normalizationConstant = normalizationConstant;
        this.minimalMass = minimalMass;
    }

    public MolecularFormulaScorer getPrior() {
        return prior;
    }

    public void setPrior(MolecularFormulaScorer prior) {
        this.prior = prior;
    }

    public double getNormalizationConstant() {
        return normalizationConstant;
    }

    public void setNormalizationConstant(double normalizationConstant) {
        this.normalizationConstant = normalizationConstant;
    }

    public double getMinimalMass() {
        return minimalMass;
    }

    public void setMinimalMass(double minimalMass) {
        this.minimalMass = minimalMass;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    public double score(MolecularFormula formula) {
        return formula.getMass() >= minimalMass ? Math.max(-10d, prior.score(formula)) - normalizationConstant : 0d;
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
        return formula.getMass() >= minimalMass ? Math.max(-10d, prior.score(formula)) - normalizationConstant : 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.prior = (MolecularFormulaScorer)helper.unwrap(document, document.getFromDictionary(dictionary, "prior"));
        this.normalizationConstant = document.getDoubleFromDictionary(dictionary, "normalization");
        this.minimalMass = document.getDoubleFromDictionary(dictionary, "minimalMass");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "prior", helper.wrap(document, prior));
        document.addToDictionary(dictionary, "normalization", normalizationConstant);
        document.addToDictionary(dictionary, "minimalMass", minimalMass);
    }
}
