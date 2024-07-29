
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
import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import gnu.trove.decorator.TObjectDoubleMapDecorator;
import gnu.trove.function.TDoubleFunction;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TObjectDoubleProcedure;
import org.slf4j.LoggerFactory;

import java.util.*;

// TODO: Add normalization as field
@Called("Common Fragments")
public class CommonFragmentsScore implements DecompositionScorer<Object>, MolecularFormulaScorer {

    private final TObjectDoubleHashMap<MolecularFormula> commonFragments;
    private TObjectDoubleHashMap<MolecularFormula> recombinatedFragments;
    private Recombinator recombinator;
    private double normalization;

    public static final double COMMON_FRAGMENTS_NORMALIZATION = 0.3105875550595019d;

    public static final Object[] COMMON_FRAGMENTS = new Object[]{
            "C6H6", 3.5774489869229, "C5H9N", 3.49383484688177, "C7H6", 3.280814303199,
            "C9H10O2", 3.14213091566505, "C5H11N", 3.11702499453398, "C9H10O", 3.10851430486607,
            "C4H9N", 3.10851430486607, "C8H8O2", 3.10851430486607, "C9H9N", 3.03768825229746,
            "C5H6", 3.02847159719253, "C9H8O", 3.00977946418038, "C3H7N", 3.00030072022584,
            "C6H4", 2.98106935829795, "C11H12O", 2.98106935829795, "C6H8", 2.98106935829795,
            "C8H8O", 2.96146088690957, "C5H8", 2.9515105560564, "C11H12", 2.9210513485717,
            "C11H14", 2.90021726166885, "C10H10O2", 2.90021726166885, "C6H11N", 2.88963515233832,
            "C10H10O", 2.83497673980045, "C10H12O2", 2.83497673980045, "C8H7NO", 2.8122484887229,
            "C4H7N", 2.80068766632182, "C6H13NO", 2.74078952474075, "C11H12O2", 2.74078952474075,
            "C9H11N", 2.71578822253533, "C12H14", 2.71578822253533, "C12H10", 2.70304919675791,
            "C8H9N", 2.690145791922, "C8H13N", 2.67707371035464, "C5H7N", 2.66382848360462,
            "C4H6", 2.66382848360462, "C16H18", 2.66382848360462, "C8H8", 2.66382848360462
    };


    /*
    This is a list of fragments which are relative common (count >= 5), which have a bad chemical prior (hetero-to-carbon{@literal >}0.6),
    and which are contained in the KEGG database
     */
    private static final Object[] COMPENSATE_STRANGE_CHEMICAL_PRIOR = new Object[]{
            "C5H6N4",1.5231111245008198,
            "C5H5N5",3.754546488631306,
            "C3H4ClN5",10.68601829423076,
            "C4H4N2S",0.877725764113497,
            "C5H4N4",1.5231111245008198,
            "C4H5N3O",0.877725764113497
    };

    public static CommonFragmentsScore map(Object... values) {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (int i=0; i < values.length; i += 2) {
            final String formula = (String)values[i];
            final double score = ((Number)values[i+1]).doubleValue();
            try {
                map.put(MolecularFormula.parse(formula), score);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(CommonFragmentsScore.class).warn("Cannot parse Formula. Skipping!", e);
            }
        }
        return new CommonFragmentsScore(map);
    }

    public static CommonFragmentsScore getLearnedCommonFragmentScorerThatCompensateChemicalPrior() {
        return map(COMPENSATE_STRANGE_CHEMICAL_PRIOR);
    }

    public static CommonFragmentsScore getLearnedCommonFragmentScorer() {
        return getLearnedCommonFragmentScorer(1);
    }

    public Map<MolecularFormula, Double> getCommonFragments() {
        return Collections.unmodifiableMap(new TObjectDoubleMapDecorator<MolecularFormula>(commonFragments));
    }

    public void addCommonFragment(MolecularFormula formula, double score) {
        commonFragments.put(formula, score);
        makeDirty();
    }

    private void makeDirty() {
        recombinatedFragments = commonFragments;
    }

    private static Map<MolecularFormula, Double> mergeMaps(Map<MolecularFormula, Double> map1, Map<MolecularFormula, Double> map2, double multiplicator) {
        final HashMap<MolecularFormula, Double> merged = new HashMap<MolecularFormula, Double>(map1);
        for (Map.Entry<MolecularFormula, Double> mapEntry : map2.entrySet()) {
            for (Map.Entry<MolecularFormula, Double> entry : map1.entrySet()) {
                merged.put(mapEntry.getKey().add(entry.getKey()), (mapEntry.getValue()+entry.getValue())*multiplicator);
            }
        }
        return merged;
    }

    public static CommonFragmentsScore getLearnedCommonFragmentScorer(final double scale) {
        final CommonFragmentsScore scorer = map(COMMON_FRAGMENTS);
        if (scale == 1) return scorer;
        scorer.commonFragments.transformValues(new TDoubleFunction() {
            @Override
            public double execute(double value) {
                return value * scale;
            }
        });
        scorer.setNormalization(COMMON_FRAGMENTS_NORMALIZATION*scale);
        return scorer;
    }


    public CommonFragmentsScore(HashMap<MolecularFormula, Double> commonFragments) {
        this(commonFragments, COMMON_FRAGMENTS_NORMALIZATION);
    }

    public CommonFragmentsScore(Map<MolecularFormula, Double> commonFragments, double normalization) {
        this.commonFragments = convertMap(commonFragments);
        this.normalization = normalization;
        recombinatedFragments = this.commonFragments;
        recombinator = null;
    }

    private static TObjectDoubleHashMap<MolecularFormula> convertMap(Map<MolecularFormula, Double> map) {
        final TObjectDoubleHashMap newMap = new TObjectDoubleHashMap<MolecularFormula>(map.size());
        for (Map.Entry<MolecularFormula, Double> entry : map.entrySet()) newMap.put(entry.getKey(), entry.getValue());
        return newMap;
    }


    public Recombinator getRecombinator() {
        return recombinator;
    }

    public void setRecombinator(Recombinator recombinator) {
        if (recombinator == this.recombinator) return;
        this.recombinator = recombinator;
        this.recombinatedFragments = commonFragments;
        makeDirty();
    }

    public CommonFragmentsScore() {
        this(new HashMap<MolecularFormula, Double>(), 0d);
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public Object prepare(ProcessedInput input) {
        return MolecularFormula.parseOrThrow("H");
    }

    @Override
    public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
        return getRecombinatedFragments().get(formula);
    }

    @Override
    public double score(MolecularFormula formula) {
        final Double val = getRecombinatedFragments().get(formula);
        if (val == null) return -normalization;
        else return val.doubleValue()-normalization;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        commonFragments.clear();
        final Iterator<Map.Entry<String, G>> iter = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "fragments"));
        while (iter.hasNext()) {
            final Map.Entry<String,G> entry = iter.next();
            try {
                commonFragments.put(MolecularFormula.parse(entry.getKey()), document.getDouble(entry.getValue()));
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(getClass()).warn("Cannot parse Formula. Skipping!", e);
            }
        }
        recombinatedFragments = commonFragments;
        normalization = document.getDoubleFromDictionary(dictionary, "normalization");
        if (document.hasKeyInDictionary(dictionary, "recombinator")) {
            this.recombinator = (Recombinator) helper.unwrap(document, document.getFromDictionary(dictionary, "recombinator"));
        }
    }

    @Override
    public <G, D, L> void exportParameters(final ParameterHelper helper, final DataDocument<G, D, L> document, final D dictionary) {
        final D common = document.newDictionary();
        commonFragments.forEachEntry(new TObjectDoubleProcedure<MolecularFormula>() {
            @Override
            public boolean execute(MolecularFormula a, double b) {
                document.addToDictionary(common, a.toString(), b);
                return true;
            }
        });
        document.addDictionaryToDictionary(dictionary, "fragments", common);
        document.addToDictionary(dictionary, "normalization", normalization);
        if (recombinator != null) document.addToDictionary(dictionary, "recombinator", helper.wrap(document, recombinator));
    }

    protected TObjectDoubleHashMap<MolecularFormula> getRecombinatedFragments() {
        if (recombinatedFragments == commonFragments && recombinator != null) {
            recombinatedFragments = recombinator.recombinate(commonFragments, normalization);
        }
        return recombinatedFragments;
    }

    /**
     * A recombinator extends the list of common losses by combination of losses
     */
    public interface Recombinator extends ImmutableParameterized<Recombinator> {
        TObjectDoubleHashMap<MolecularFormula> recombinate(TObjectDoubleHashMap<MolecularFormula> source, double normalizationConstant);
    }

    public static class LossCombinator implements Recombinator {

        private List<MolecularFormula> losses;
        private double penalty;

        public LossCombinator() {
            this.losses = new ArrayList<MolecularFormula>();
            this.penalty = 0d;
        }

        public LossCombinator(double penalty, List<MolecularFormula> losses) {
            this.penalty = penalty;
            this.losses = new ArrayList<MolecularFormula>(losses);
        }

        public LossCombinator(double penalty, CommonLossEdgeScorer lossScorer, LossSizeScorer lossSizeScorer) {
            this.penalty = penalty;
            losses = new ArrayList<MolecularFormula>();
            final Map<MolecularFormula, Double> commonLosses = lossScorer.getCommonLosses();
            for (MolecularFormula f : commonLosses.keySet()) {
                final double adjustedScore = commonLosses.get(f) + lossSizeScorer.score(f);
                if (commonLosses.get(f) > 1) {
                    losses.add(f);
                }
            }
        }

        @Override
        public TObjectDoubleHashMap<MolecularFormula>  recombinate(TObjectDoubleHashMap<MolecularFormula>  source, double normalizationConstant) {
            final TObjectDoubleHashMap<MolecularFormula>  recombination = new TObjectDoubleHashMap<MolecularFormula> (source.size()*losses.size());
            for (MolecularFormula loss : losses) {
                for (MolecularFormula f : source.keySet()) {
                    final MolecularFormula recomb = loss.add(f);
                    final double  score = source.get(f)+penalty;
                    if (score < 0) continue;
                    if (!source.containsKey(recomb) || source.get(recomb)<score) recombination.put(recomb, score);
                }
            }
            return recombination;
        }

        @Override
        public <G, D, L> Recombinator readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            double penalty = document.getDoubleFromDictionary(dictionary, "penalty");
            List<MolecularFormula> losses = new ArrayList<MolecularFormula>();
            final Iterator<G> iter = document.iteratorOfList(document.getListFromDictionary(dictionary, "losses"));
            while (iter.hasNext()) {
                try {
                    losses.add(MolecularFormula.parse(document.getString(iter.next())));
                } catch (UnknownElementException e) {
                    LoggerFactory.getLogger(getClass()).warn("Cannot parse Formula. Skipping!", e);
                }
            }
            return new LossCombinator(penalty, losses);
        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
            document.addToDictionary(dictionary, "penalty", penalty);
            final L ls = document.newList();
            for (MolecularFormula l : losses) document.addToList(ls, l.formatByHill());
            document.addListToDictionary(dictionary, "losses", ls);
        }
    }

}
