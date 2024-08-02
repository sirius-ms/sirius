
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
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class StrangeElementLossScorer implements LossScorer {

    private HashSet<MolecularFormula> lossList;
    private double score;

    public StrangeElementLossScorer() {
        this.lossList = new HashSet<MolecularFormula>();
        this.score = 0d;
    }

    public StrangeElementLossScorer(Iterable<MolecularFormula> knownLosses, double score) {
        this.lossList = new HashSet<MolecularFormula>(knownLosses instanceof Collection ?
                (int) (((Collection<MolecularFormula>) knownLosses).size() * 1.5) : 150);
        for (MolecularFormula f : knownLosses) this.lossList.add(f);
        this.score = score;
    }

    public StrangeElementLossScorer(CommonLossEdgeScorer scorer) {
        this(scorer, Math.log(1.5));
    }

    public StrangeElementLossScorer(CommonLossEdgeScorer scorer, double score) {
        final Map<MolecularFormula, Double> map = scorer.getCommonLosses();
        this.lossList = new HashSet<MolecularFormula>(150);
        for (MolecularFormula f : map.keySet()) {
            if (!f.isCHNO())
                lossList.add(f);
        }
        this.score = score;
    }

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        final ArrayList<MolecularFormula> specialElements = new ArrayList<MolecularFormula>();
        final PeriodicTable t = PeriodicTable.getInstance();
        final Element C = t.getByName("C");
        final Element H = t.getByName("H");
        final Element N = t.getByName("N");
        final Element O = t.getByName("O");
        final MolecularFormula hydrogen = MolecularFormula.parseOrThrow("H");
        for (Element e : input.getExperimentInformation()
                .getAnnotationOrDefault(FormulaConstraints.class).getChemicalAlphabet().getElements()) {
            if (e == C || e == H || e == N || e == O) continue;
            specialElements.add(MolecularFormula.singleElement(e));
        }
        final HashSet<MolecularFormula> knownLosses = new HashSet<MolecularFormula>(150);
        for (MolecularFormula f : lossList) {
            addKnownStrangeLoss(specialElements, hydrogen, knownLosses, f);
            for (MolecularFormula g : lossList) {
                final MolecularFormula combined = f.add(g);
                addKnownStrangeLoss(specialElements, hydrogen, knownLosses, combined);
            }
        }
        return knownLosses;
    }

    protected void addKnownStrangeLoss(ArrayList<MolecularFormula> specialElements, MolecularFormula hydrogen, HashSet<MolecularFormula> knownLosses, MolecularFormula f) {
        if (f.isCHNO()) {
            for (int k = 1; k <= 2; ++k) {
                if (f.numberOfHydrogens() >= k && f.numberOfCarbons() > 0) {
                    final MolecularFormula substitution = f.subtract(hydrogen);
                    for (MolecularFormula e : specialElements) {
                        knownLosses.add(substitution.add(e.multiply(k)));
                    }
                }
            }
        } else {
            knownLosses.add(f);
        }
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        if (((HashSet<MolecularFormula>) precomputed).contains(loss.getFormula())) return score;
        else return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L list = document.getListFromDictionary(dictionary, "losses");
        final int n = document.sizeOfList(list);
        this.lossList = new HashSet<MolecularFormula>((int) (n * 1.5));
        for (int i = 0; i < n; ++i)
            MolecularFormula.parseAndExecute(document.getStringFromList(list, i), this::addLoss);

        this.score = document.getDoubleFromDictionary(dictionary, "score");

    }

    public boolean addLoss(MolecularFormula loss) {
        return lossList.add(loss);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L losses = document.newList();
        for (MolecularFormula f : lossList) document.addToList(losses, f.toString());
        document.addListToDictionary(dictionary, "losses", losses);
        document.addToDictionary(dictionary, "score", score);
    }
}
