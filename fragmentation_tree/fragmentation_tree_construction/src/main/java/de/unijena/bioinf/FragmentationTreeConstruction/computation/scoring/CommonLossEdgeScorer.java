
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

import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;
import gnu.trove.decorator.TObjectDoubleMapDecorator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TObjectDoubleProcedure;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CommonLossEdgeScorer implements LossScorer {

    public final static String[] ales_list = new String[]{
            "H2", "H2O", "CH4", "C2H4", "C2H2",
            "C4H8", "C5H8", "C6H6", "CH2O",
            "CO", "CH2O2", "CO2", "C2H4O2",
            "C2H2O", "C3H6O2", "C3H4O4",
            "C3H2O3", "C5H8O4", "C6H10O5",
            "C6H8O6", "NH3", "CH5N",
            "CH3N", "C3H9N", "CHNO", "CH4N2O",
            "H3PO3", "H3PO4", "HPO3", "C2H5O4P",
            "H2S", "S", "SO2", "SO3", "H2SO4"
    };

    /**
     * list from literature collected by Ma et al in
     MS2Analyzer: A Software for Small Molecule Substructure Annotations from Accurate Tandem Mass Spectra
     Anal Chem, 2014
     */
    public final static String[] literature_list = new String[]{
            "H2", "CH3", "NH2", "OH", "NH3", "H2O", "HCN", "CO", "NO", "CH2O", "CH3O", "CH5N", "CH4O", "H2S", "HCl", "C2H2O", "C3H6", "CHNO", "C3H7", "CO2", "CHO2", "CH3NO", "C2H7N", "NO2", "CH2O2", "CH4S", "H2NO2", "H3PO4", "CH3Cl", "C2HNO", "C2O2", "C2HO2", "C2H3NO", "CNO2", "C2H2O2", "C3H9N", "C2H4O2", "CH3NO2", "CH4OS", "CF3", "C3H5NO", "C2O3", "CNO3", "C3H6S", "C6H5", "CH3N2Cl", "C6H6", "SO3", "HPO3", "HSO3", "H2SO3","CN2O3", "C3H6O3", "H3PO4", "C3O4", "C5H10O2", "CN2O4", "C3H7NOS", "C7H8O", "C6H5S", "C4O4", "CN2O5", "C4H8O4", "C3H7NO2S", "HI", "C5H7NO3", "C6H10O3", "C5H8O4", "C2H8O4NP", "C6H10O4", "C5H10N2O3", "C3H7O5P", "C9H16O2", "C6H11NO4", "C6H10O5", "C5H9NO3S", "C3H9O6P", "C6H8O6", "C5H10N2O3S", "C6H13NO5", "C3H8NO6P", "C3H12NO6P", "C6H10O7", "C8H13NO5", "C8H15NO6", "C9H12O8", "C8H14N2O5S", "C6H13O9P", "C6H13O9S", "C9H14O9", "C10H15N3O6", "C10H17N3O6", "C6H16NO9P", "C10H17N3O6S", "C12H23NO10"
    };

    private final static String[] implausibleLosses = new String[]{"C2O", "C4O", "C3H2", "C5H2", "C7H2", "N", "C"};
    private final TObjectDoubleHashMap<MolecularFormula> commonLosses;
    private TObjectDoubleHashMap<MolecularFormula> recombinatedList;
    private double normalization;
    private Recombinator recombinator;

    public CommonLossEdgeScorer() {
        this(Collections.<MolecularFormula, Double>emptyMap(), null);
    }


    public CommonLossEdgeScorer(Map<MolecularFormula, Double> commonLosses, Recombinator recombinator, double normalization) {
        this.commonLosses = convertMap(commonLosses);
        this.recombinatedList = null;
        this.normalization = normalization;
        this.recombinator = recombinator;
    }


    public CommonLossEdgeScorer(Map<MolecularFormula, Double> commonLosses, Recombinator recombinator) {
        this(commonLosses, recombinator, 0d);
    }

    private static TObjectDoubleHashMap<MolecularFormula> convertMap(Map<MolecularFormula, Double> map) {
        final TObjectDoubleHashMap newMap = new TObjectDoubleHashMap<MolecularFormula>(map.size());
        for (Map.Entry<MolecularFormula, Double> entry : map.entrySet()) newMap.put(entry.getKey(), entry.getValue());
        return newMap;
    }

    /**
     * If you have no clue about the correct score of your common losses, you can assume that they are all equally distributed.
     * In this case, you have to ignore the loss size scoring, e.g. if H2 and C6H6 have the same frequency, they are not
     * allowed to get different scores. We do this by adding the negative loss size score as common loss score. Therefore,
     * when adding the loss size score later, both scores are sumed up to 0.
     * Nevertheless: Maybe it is not wrong to say: C6H6 is "nicer" than H2, as it contains more information. Therefore,
     * you can add only e.g. 70% of the negative loss size score to the common loss score.
     *
     * @param lossSizeScorer
     * @param compensation   multiplicator with loss size score
     * @return
     */
    public static CommonLossEdgeScorer getLossSizeCompensationForExpertList(LossSizeScorer lossSizeScorer, double compensation) {
        final CommonLossEdgeScorer scorer = new CommonLossEdgeScorer();
        for (String f : ales_list) {
            final MolecularFormula m = MolecularFormula.parseOrThrow(f);
            scorer.addCommonLoss(m, -(lossSizeScorer.score(m) + lossSizeScorer.getNormalization()) * compensation);
        }
        return scorer;
    }

    /**
     * @param penalty
     * @return
     */
    public CommonLossEdgeScorer addImplausibleLosses(double penalty) {
        for (String f : implausibleLosses) {
            addCommonLoss(MolecularFormula.parseOrThrow(f), penalty);
        }
        return this;
    }

    public Map<MolecularFormula, Double> getCommonLosses() {
        return Collections.unmodifiableMap(new TObjectDoubleMapDecorator<MolecularFormula>(commonLosses));
    }


    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        getRecombinatedList();
        return null;
    }

    public boolean isCommonLoss(MolecularFormula f) {
        return commonLosses.get(f) > 0;
    }

    public boolean isRecombinatedLoss(MolecularFormula f) {
        return !isCommonLoss(f) && getRecombinatedList().get(f) > 0;
    }

    public void addCommonLoss(MolecularFormula loss, double score) {
        commonLosses.put(loss, score);
        recombinatedList = null;
    }

    public void clearLosses() {
        commonLosses.clear();
        recombinatedList = null;
    }

    public Recombinator getRecombinator() {
        return recombinator;
    }

    public void setRecombinator(Recombinator recombinator) {
        this.recombinator = recombinator;
        recombinatedList = null;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public void merge(Map<MolecularFormula, Double> map) {
        this.commonLosses.putAll(map);
        recombinatedList = null;
    }

    public void merge(TObjectDoubleHashMap<MolecularFormula> map) {
        this.commonLosses.putAll(map);
        recombinatedList = null;
    }

    public void merge(CommonLossEdgeScorer lossScorer) {
        merge(lossScorer.commonLosses);
    }

    public double score(MolecularFormula formula) {
        final double score = getRecombinatedList().get(formula);
        if (score != 0) return score - normalization;
        else return commonLosses.get(formula) - normalization;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getFormula());
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final Iterator<Map.Entry<String, G>> iter = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "losses"));
        clearLosses();
        while (iter.hasNext()) {
            final Map.Entry<String, G> entry = iter.next();
            try {
                commonLosses.put(MolecularFormula.parse(entry.getKey()), document.getDouble(entry.getValue()));
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(CommonFragmentsScore.class).warn("Cannot parse Formula. Skipping!", e);
            }
        }
        this.normalization = document.getDoubleFromDictionary(dictionary, "normalization");
        if (document.hasKeyInDictionary(dictionary, "recombinator"))
            this.recombinator = (Recombinator) helper.unwrap(document, document.getFromDictionary(dictionary, "recombinator"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final D common = document.newDictionary();
        for (Map.Entry<MolecularFormula, Double> entry : getCommonLosses().entrySet()) {
            document.addToDictionary(common, entry.getKey().toString(), entry.getValue());
        }
        document.addDictionaryToDictionary(dictionary, "losses", common);
        if (recombinator != null)
            document.addToDictionary(dictionary, "recombinator", helper.wrap(document, recombinator));
        document.addToDictionary(dictionary, "normalization", normalization);
    }

    TObjectDoubleHashMap<MolecularFormula> getRecombinatedList() {
        if (recombinatedList == null)
            recombinatedList = recombinator == null ? new TObjectDoubleHashMap<MolecularFormula>()
                    : recombinator.recombinate(commonLosses, normalization);
        return recombinatedList;
    }

    /**
     * A recombinator extends the list of common losses by combination of losses
     */
    public interface Recombinator extends ImmutableParameterized<Recombinator> {
        TObjectDoubleHashMap<MolecularFormula> recombinate(TObjectDoubleHashMap<MolecularFormula> source, double normalizationConstant);
    }

    public static class LegacyOldSiriusRecombinator implements Recombinator {


        @Override
        public TObjectDoubleHashMap<MolecularFormula> recombinate(TObjectDoubleHashMap<MolecularFormula> source, double normalizationConstant) {
            final ArrayList<MolecularFormula> losses = new ArrayList<MolecularFormula>(source.keySet().size());
            source.forEachEntry(new TObjectDoubleProcedure<MolecularFormula>() {
                @Override
                public boolean execute(MolecularFormula a, double b) {
                    if (b >= 0) losses.add(a);
                    return true;
                }
            });
            final TObjectDoubleHashMap<MolecularFormula> recs = new TObjectDoubleHashMap<MolecularFormula>(source.size() * source.size() * source.size());
            List<MolecularFormula> src = new ArrayList<MolecularFormula>(losses);
            final double gamma = 10;
            for (int i = 2; i <= 3; ++i) {
                final double score = Math.log10(gamma / i);
                final ArrayList<MolecularFormula> newSrc = new ArrayList<MolecularFormula>();
                for (MolecularFormula f : losses) {
                    for (MolecularFormula g : src) {
                        newSrc.add(f.add(g));
                    }
                }
                src = newSrc;
                for (MolecularFormula f : src) recs.put(f, score);
            }
            return recs;
        }

        @Override
        public <G, D, L> Recombinator readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            return new LegacyOldSiriusRecombinator();
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        }
    }

    /**
     * The MinimalScoreRecombinator recombinates by the following strategy:
     * 1. calculate the final score of both common loss (after adding loss size prior and normalizations)
     * 2. take the minimum of both scores
     * 3. add a penalty
     */
    public static class MinimalScoreRecombinator implements Recombinator {

        private final double penalty;
        private final LossSizeScorer lossSizeScorer;

        MinimalScoreRecombinator() {
            this(null, 0d);
        }

        public MinimalScoreRecombinator(LossSizeScorer sc, double penalty) {
            this.penalty = penalty;
            this.lossSizeScorer = sc;
        }

        @Override
        public TObjectDoubleHashMap<MolecularFormula> recombinate(TObjectDoubleHashMap<MolecularFormula> source, double normalizationConstant) {
            final TObjectDoubleHashMap<MolecularFormula> recombination = new TObjectDoubleHashMap<MolecularFormula>(source.size() * source.size());
            final List<MolecularFormula> sourceList = new ArrayList<MolecularFormula>(source.keySet());
            for (int i = 0; i < sourceList.size(); ++i) {
                final MolecularFormula a = sourceList.get(i);
                if (source.get(a) < 0) continue;
                final double aScore = lossSizeScorer.score(a) + source.get(a);
                for (int j = i; j < sourceList.size(); ++j) {
                    final MolecularFormula b = sourceList.get(j);
                    if (source.get(b) < 0) continue;
                    final double bScore = lossSizeScorer.score(b) + source.get(b);
                    final MolecularFormula combination = a.add(b);
                    final double combinationScore = lossSizeScorer.score(combination) + source.get(combination);
                    final double recombinationScore = Math.min(aScore, bScore) + penalty;
                    if (recombinationScore > combinationScore) {
                        final double finalScore = recombinationScore - lossSizeScorer.score(combination);
                        recombination.put(combination, finalScore);
                    }
                }
            }
            return recombination;
        }

        @Override
        public <G, D, L> Recombinator readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            return new MinimalScoreRecombinator((LossSizeScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "lossSize")), document.getDoubleFromDictionary(dictionary, "penalty"));
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            document.addToDictionary(dictionary, "penalty", penalty);
            document.addToDictionary(dictionary, "lossSize", helper.wrap(document, lossSizeScorer));
        }
    }

    /**
     * The LossSizeRecombinator recombinates by the following strategy:
     * 1. calculate the final score of both common loss (after adding loss size prior and normalizations)
     * 2. use the average of both scores as new common loss score
     * 3. subtract the loss size score from this score, such that the loss size of the resulting loss is not considered
     * (as we expect not one loss with this size but two consecutive losses instead)
     * 4. add a small penalty for not observing the intermediate fragment
     */
    public static class LossSizeRecombinator implements Recombinator {

        private final double penalty;
        private final LossSizeScorer lossSizeScorer;


        LossSizeRecombinator() {
            this(null, 0d);
        }

        public LossSizeRecombinator(LossSizeScorer scorer, double penalty) {
            this.penalty = penalty;
            this.lossSizeScorer = scorer;
        }

        @Override
        public TObjectDoubleHashMap<MolecularFormula> recombinate(TObjectDoubleHashMap<MolecularFormula> source, double normalizationConstant) {
            final TObjectDoubleHashMap<MolecularFormula> recombination = new TObjectDoubleHashMap<MolecularFormula>(source.size() * source.size());
            final List<MolecularFormula> sourceList = new ArrayList<MolecularFormula>(source.keySet());
            for (int i = 0; i < sourceList.size(); ++i) {
                final MolecularFormula a = sourceList.get(i);
                final double aScore = lossSizeScorer.score(a) + source.get(a) - normalizationConstant;
                for (int j = i; j < sourceList.size(); ++j) {
                    final MolecularFormula b = sourceList.get(j);
                    final double bScore = lossSizeScorer.score(b) + source.get(b) - normalizationConstant;
                    final MolecularFormula combination = a.add(b);
                    final double abScore = lossSizeScorer.score(combination);
                    final double combinatedScore = (aScore + bScore) - abScore + normalizationConstant + penalty;
                    if (combinatedScore > 0d) {
                        Double sc = source.get(combination);
                        if (sc == null || sc.doubleValue() < combinatedScore) {
                            recombination.put(combination, combinatedScore);
                        }
                    }
                }
            }
            return recombination;
        }

        @Override
        public <G, D, L> Recombinator readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            return new LossSizeRecombinator((LossSizeScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "lossSize")), document.getDoubleFromDictionary(dictionary, "penalty"));
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            document.addToDictionary(dictionary, "penalty", penalty);
            document.addToDictionary(dictionary, "lossSize", helper.wrap(document, lossSizeScorer));
        }
    }

}
