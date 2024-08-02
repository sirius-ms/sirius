
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.SupportVectorMolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

public class ChemicalPriorEdgeScorer implements LossScorer {

    private MolecularFormulaScorer prior;
    private double normalization;
    private double minimalMass;

    public ChemicalPriorEdgeScorer() {
        this(ChemicalCompoundScorer.createDefaultCompoundScorer(true), 0d, 100d);
    }

    public ChemicalPriorEdgeScorer(MolecularFormulaScorer prior, double normalization, double minimalMass) {
        this.prior = prior;
        this.normalization = normalization;
        this.minimalMass = minimalMass;
    }

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getSource().getFormula(), loss.getTarget().getFormula());
    }

    public double score(MolecularFormula parentFormula, MolecularFormula childFormula) {
        if (childFormula.getMass() < minimalMass) return 0d;
        double child,parent;
        if (prior instanceof SupportVectorMolecularFormulaScorer) {
            // legacy
            child = Math.min(1, prior.score(childFormula));
            parent = Math.min(1, prior.score(parentFormula));
            return Math.min(0,child-parent) - normalization;
        } else {
            child = Math.max(Math.log(0.0001), prior.score(childFormula));
            parent = Math.max(Math.log(0.0001), prior.score(parentFormula));
        }
        return Math.min(0, child - parent) - normalization;
    }

    public MolecularFormulaScorer getPrior() {
        return prior;
    }

    public void setPrior(MolecularFormulaScorer prior) {
        this.prior = prior;
    }

    public double getMinimalMass() {
        return minimalMass;
    }

    public void setMinimalMass(double minimalMass) {
        this.minimalMass = minimalMass;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.prior = (MolecularFormulaScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "prior"));
        this.normalization = document.getDoubleFromDictionary(dictionary, "normalization");
        this.minimalMass = document.getDoubleFromDictionary(dictionary, "minimalMass");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "prior", helper.wrap(document, prior));
        document.addToDictionary(dictionary, "normalization", normalization);
        document.addToDictionary(dictionary, "minimalMass", minimalMass);
    }
}
