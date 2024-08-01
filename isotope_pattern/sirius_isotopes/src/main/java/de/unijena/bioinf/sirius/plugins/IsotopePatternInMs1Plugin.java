/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IntergraphMapping;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.ExtractedIsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * If an isotope pattern analysis assigns an ExtractedIsotopePattern annotation to the processed input,
 * this plugin will integrate the corresponding scores into the fragmentation pattern analysis
 */
public class IsotopePatternInMs1Plugin extends SiriusPlugin {


    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addRootScorer(new Ms1IsotopePatternScorer());
    }

    @Override
    protected void transferAnotationsFromInputToGraph(ProcessedInput input, FGraph graph) {
        final ExtractedIsotopePattern pattern = input.getAnnotationOrNull(ExtractedIsotopePattern.class);
        if (pattern!=null) {
            final IsotopePattern iso = pattern.getExplanations().get(graph.getRoot().getChildren(0).getFormula());
            if (iso!=null) {
                graph.setAnnotation(IsotopePattern.class, iso);
            }
        }
    }

    @Override
    protected void transferAnotationsFromGraphToTree(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2treeFragments) {
        graph.getAnnotation(IsotopePattern.class).ifPresent(pattern -> {
            tree.setAnnotation(IsotopePattern.class, pattern);
            tree.getOrCreateFragmentAnnotation(Ms1IsotopePattern.class).set(tree.getRoot(), new Ms1IsotopePattern(pattern.getPattern(), pattern.getScore()));
        });
    }

    @Override
    protected void beforeDecomposing(ProcessedInput input) {
        if (input.getExperimentInformation().getMolecularFormula() != null) return;
        IsotopeSettings isotopeS = input.getAnnotationOrNull(IsotopeSettings.class);
        if (isotopeS != null && isotopeS.isFiltering()) {
            final ExtractedIsotopePattern extractedIsotopePattern = input.getAnnotationOrNull(ExtractedIsotopePattern.class);
            if (extractedIsotopePattern != null && input.getAnnotation(IsotopeSettings.class).map(IsotopeSettings::isFiltering).orElse(false)) {
                Whiteset whiteset = input.getAnnotationOrThrow(Whiteset.class);
                if (whiteset.isFinalized()) return;
                // find all high-scoring isotope pattern
                final MolecularFormula[] formulas = filterFormulasByIsotopeScore(extractedIsotopePattern);
                final PossibleAdducts adducts = input.getAnnotationOrThrow(PossibleAdducts.class);
                whiteset = whiteset.filterByMeasuredFormulas(Arrays.stream(formulas).collect(Collectors.toSet()), adducts.getAdducts(), IsotopePatternInMs1Plugin.class);
                input.setAnnotation(Whiteset.class, whiteset);
            }
        }
    }

    private MolecularFormula[] filterFormulasByIsotopeScore(ExtractedIsotopePattern pattern) {
        int isoPeaks = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        double scoreThresholdForFiltering = Double.NEGATIVE_INFINITY;
        for (IsotopePattern pat : pattern.getExplanations().values()) {
            maxScore = Math.max(pat.getScore(), maxScore);
            final int numberOfIsoPeaks = pat.getPattern().size() - 1;
            if (pat.getScore() >= (2 + 2 * numberOfIsoPeaks)) {
                isoPeaks = Math.max(pat.getPattern().size()-1, isoPeaks);
                scoreThresholdForFiltering = isoPeaks * 1d;
                scoreThresholdForFiltering = Math.max(0, scoreThresholdForFiltering-1);
            }
        }
        final double SCORE_THRESHOLD = scoreThresholdForFiltering;
        if (Double.isInfinite(SCORE_THRESHOLD)) return pattern.getExplanations().keySet().toArray(MolecularFormula[]::new);
        return pattern.getExplanations().entrySet().stream().filter(e -> e.getValue().getScore() >= SCORE_THRESHOLD).map(Map.Entry::getKey).toArray(MolecularFormula[]::new);
    }

    @Called("MS-Isotopes")
    public static class Ms1IsotopePatternScorer implements DecompositionScorer<IsotopePatternInMs1Plugin.Prepared> {

        @Override
        public IsotopePatternInMs1Plugin.Prepared prepare(ProcessedInput input) {
            return new IsotopePatternInMs1Plugin.Prepared(input.getAnnotationOrDefault(IsotopeSettings.class), input.getAnnotation(ExtractedIsotopePattern.class, ()->null), input.getExperimentInformation().getPrecursorIonType());
        }

        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, IsotopePatternInMs1Plugin.Prepared precomputed) {
            // the tree is always neutralized!
//            if (!precomputed.ionType.isIonizationUnknown())
//                formula = precomputed.ionType.precursorIonToNeutralMolecule(formula);
            if (precomputed.pattern!=null && precomputed.pattern.getExplanations().get(formula)!=null) {
                return precomputed.weight.getMultiplier() * Math.max(0d, precomputed.pattern.getExplanations().get(formula).getScore());
            } else return 0d;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }


    protected static class Prepared {
        private final IsotopeSettings weight;
        private final ExtractedIsotopePattern pattern;
        private final PrecursorIonType ionType;

        public Prepared(IsotopeSettings weight, ExtractedIsotopePattern pattern, PrecursorIonType ionType) {
            this.weight = weight;
            this.pattern = pattern;
            this.ionType = ionType;
        }
    }

}
