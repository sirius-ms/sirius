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
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import gnu.trove.decorator.TObjectDoubleMapDecorator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Called("Free Radical")
public class FreeRadicalEdgeScorer implements LossScorer, MolecularFormulaScorer {

    private final TObjectDoubleHashMap<MolecularFormula> freeRadicals;
    private double generalRadicalScore;
    private double normalization;

    public FreeRadicalEdgeScorer() {
        this.freeRadicals = new TObjectDoubleHashMap<MolecularFormula>();
        this.generalRadicalScore = 0d;
        this.normalization = 0d;
    }

    public FreeRadicalEdgeScorer(Map<MolecularFormula, Double> freeRadicals, double generalRadicalScore, double normalization) {
        this.freeRadicals = new TObjectDoubleHashMap<MolecularFormula>(freeRadicals.size() * 2);
        this.freeRadicals.putAll(freeRadicals);
        this.generalRadicalScore = generalRadicalScore;
        this.normalization = normalization;
    }

    public static FreeRadicalEdgeScorer getRadicalScorerWithDefaultSet() {
        return getRadicalScorerWithDefaultSet(Math.log(0.9), Math.log(0.1), -0.011626542158820332d);
    }

    public static FreeRadicalEdgeScorer getRadicalScorerWithDefaultSet(double knownRadicalScore, double generalRadicalScore, double normalization) {
        final MolecularFormula[] formulas = new MolecularFormula[]{
                MolecularFormula.parse("H"), MolecularFormula.parse("O"), MolecularFormula.parse("OH"),
                MolecularFormula.parse("CH3"), MolecularFormula.parse("CH3O"),
                MolecularFormula.parse("C3H7"), MolecularFormula.parse("C4H9"),
                MolecularFormula.parse("C6H5O"), MolecularFormula.parse("C6H5"), MolecularFormula.parse("C6H6N"), MolecularFormula.parse("I"),
                MolecularFormula.parse("NO"), MolecularFormula.parse("NO2"), MolecularFormula.parse("Br"), MolecularFormula.parse("Cl")
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
    public Object prepare(ProcessedInput input) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object x_) {
        return score(loss.getFormula()) - normalization;
    }

    @Override
    public double score(MolecularFormula formula) {
        final Double score = freeRadicals.get(formula);
        if (score != null) return score.doubleValue();
        if (formula.maybeCharged()) return generalRadicalScore;
        return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final Iterator<Map.Entry<String, G>> iter = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "commonRadicals"));
        while (iter.hasNext()) {
            final Map.Entry<String, G> v = iter.next();
            addRadical(MolecularFormula.parse(v.getKey()), (Double) v.getValue());
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
