
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

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;
import gnu.trove.decorator.TObjectDoubleMapDecorator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Called("Free Radical")
public class FreeRadicalEdgeScorer implements LossScorer, MolecularFormulaScorer {

    private final TObjectDoubleHashMap<MolecularFormula> freeRadicals;
    private double generalRadicalScore;
    private double normalization;

    public FreeRadicalEdgeScorer() {
        this.freeRadicals = new TObjectDoubleHashMap<MolecularFormula>(50, 0.75f, Double.NEGATIVE_INFINITY);
        this.generalRadicalScore = 0d;
        this.normalization = 0d;
    }

    public FreeRadicalEdgeScorer(Map<MolecularFormula, Double> freeRadicals, double generalRadicalScore, double normalization) {
        this.freeRadicals = new TObjectDoubleHashMap<MolecularFormula>(freeRadicals.size() * 2, 0.75f, Double.NEGATIVE_INFINITY);
        this.freeRadicals.putAll(freeRadicals);
        this.generalRadicalScore = generalRadicalScore;
        this.normalization = normalization;
    }

    public static FreeRadicalEdgeScorer getRadicalScorerWithDefaultSet() {
        return getRadicalScorerWithDefaultSet(Math.log(0.9), Math.log(0.1), -0.011626542158820332d);
    }

    public static FreeRadicalEdgeScorer getRadicalScorerWithDefaultSet(double knownRadicalScore, double generalRadicalScore, double normalization) {
        final MolecularFormula[] formulas = new MolecularFormula[]{
                MolecularFormula.parseOrThrow("H"), MolecularFormula.parseOrThrow("O"), MolecularFormula.parseOrThrow("OH"),
                MolecularFormula.parseOrThrow("CH3"), MolecularFormula.parseOrThrow("CH3O"),
                MolecularFormula.parseOrThrow("C3H7"), MolecularFormula.parseOrThrow("C4H9"),
                MolecularFormula.parseOrThrow("C6H5O"), MolecularFormula.parseOrThrow("C6H5"), MolecularFormula.parseOrThrow("C6H6N"), MolecularFormula.parseOrThrow("I"),
                MolecularFormula.parseOrThrow("NO"), MolecularFormula.parseOrThrow("NO2"), MolecularFormula.parseOrThrow("Br"), MolecularFormula.parseOrThrow("Cl")
        };
        final HashMap<MolecularFormula, Double> radicals = new HashMap<MolecularFormula, Double>(formulas.length * 2);
        for (MolecularFormula formula : formulas) {
            radicals.put(formula, knownRadicalScore);
        }
        return new FreeRadicalEdgeScorer(radicals, generalRadicalScore, normalization);
    }

    public double getGeneralRadicalScore() {
        return generalRadicalScore;
    }

    public void setGeneralRadicalScore(double generalRadicalScore) {
        this.generalRadicalScore = generalRadicalScore;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public void addRadical(MolecularFormula formula, double logScore) {
        freeRadicals.put(formula, logScore);
    }

    public Map<MolecularFormula, Double> getFreeRadicals() {
        return Collections.unmodifiableMap(new TObjectDoubleMapDecorator<MolecularFormula>(freeRadicals));
    }

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object x_) {
        return score(loss.getFormula()) - normalization;
    }

    @Override
    public double score(MolecularFormula formula) {
        final double score = freeRadicals.get(formula);
        if (!Double.isInfinite(score)) return score;
        if (formula.maybeCharged()) return generalRadicalScore;
        return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final D dict = document.getDictionaryFromDictionary(dictionary, "commonRadicals");
        for (String key : document.keySetOfDictionary(dict)) {
            final double value = document.getDoubleFromDictionary(dict, key);
            MolecularFormula.parseAndExecute(key, f -> addRadical(f, value));
        }
        setGeneralRadicalScore(document.getDoubleFromDictionary(dictionary, "radicalPenalty"));
        setNormalization(document.getDoubleFromDictionary(dictionary, "normalization"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final D radicals = document.newDictionary();
        for (MolecularFormula f : freeRadicals.keySet()) {
            document.addToDictionary(radicals, f.toString(), freeRadicals.get(f));
        }
        document.addDictionaryToDictionary(dictionary, "commonRadicals", radicals);
        document.addToDictionary(dictionary, "radicalPenalty", generalRadicalScore);
        document.addToDictionary(dictionary, "normalization", normalization);

    }
}
