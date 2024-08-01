
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
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

/**
 * Special elements like F, I, S, P, Cl, Br and so on are rarely and losses containing this elements seems to be
 * often special common losses. This scorer does not penalize strange elements in general, but only strange
 * elements in uncommon losses. So remark that you add this score to the CommonLossScorer to compensate this
 * penalty.
 */
public class StrangeElementScorer implements LossScorer, MolecularFormulaScorer {

    public static final double LEARNED_PENALTY = -1.9176802031231173d;
    public static final double LEARNED_NORMALIZATION = -0.13929596343581177d;

    private double penalty;
    private double normalization;

    public StrangeElementScorer() {
        this(LEARNED_PENALTY, LEARNED_NORMALIZATION);
    }

    public StrangeElementScorer(double penalty, double normalization) {
        this.penalty = penalty;
        this.normalization = normalization;
    }

    public StrangeElementScorer(double penalty) {
        this.penalty = penalty;
        this.normalization = 0d;
    }

    public double getPenalty() {
        return penalty;
    }

    public double getNormalization() {
        return normalization;
    }

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        final PeriodicTable pt = PeriodicTable.getInstance();
        return pt.getAllByName("C", "H", "N", "O", "Na", "K", "Cl", "Br");
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final PrecursorIonType ion = input.getExperimentInformation().getPrecursorIonType();
        final int c;
        if (ion.isIonizationUnknown()) {
            c = ion.getCharge();
        } else {
            c=0;
        }
        return score(loss.getFormula(), (Element[]) precomputed, c);
    }

    private double score(MolecularFormula loss, Element[] precomputed, int allowAdductsOf) {
        int numberOfNormalElements = loss.numberOf(precomputed[0]) + loss.numberOf(precomputed[1]) + loss.numberOf(precomputed[2]) + loss.numberOf(precomputed[3]);
        if (allowAdductsOf != 0) {
            if (allowAdductsOf > 0) {
                if (loss.numberOf(precomputed[4]) > 0) ++numberOfNormalElements;
                else if (loss.numberOf(precomputed[5]) > 0) ++ numberOfNormalElements;
            } else {
                if (loss.numberOf(precomputed[6]) > 0) ++numberOfNormalElements;
                else if (loss.numberOf(precomputed[7]) > 0) ++ numberOfNormalElements;
            }
        }

        if (numberOfNormalElements < loss.atomCount()) {
            return penalty-normalization;
        } else return -normalization;
    }

    @Override
    public double score(MolecularFormula formula) {
        final PeriodicTable pt = PeriodicTable.getInstance();
        final Element[] precomputed = pt.getAllByName("C", "H", "N", "O", "Na", "K", "Cl", "Br");
        return score(formula, precomputed, 0);
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        penalty = document.getDoubleFromDictionary(dictionary, "penalty");
        normalization = document.getDoubleFromDictionary(dictionary, "normalization");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "penalty", penalty);
        document.addToDictionary(dictionary, "normalization", normalization);
    }
}
