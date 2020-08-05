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

package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IntergraphMapping;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.LossValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

import java.util.Set;

/**
 * The idea of plugins is to organize code by its function. Currently, many specific features like isotope pattern
 * consists of several functions that have to be called at different code locations. By bundling them into a
 * single class, it should be easier to understand the effect of each plugin. The disadvantage is, that the code is not
 * longer located where it is called, making the understanding of the whole workflow more complicated. Furthermore,
 * there is no system for cross-plugin dependencies. Let's hope that we never need them!
 */
public abstract class SiriusPlugin {

    /*
    methods to override
     */

    /**
     * - is called the first time the plugin is loaded
     * - can add new scorers to the SIRIUS workflow
     * - currently, scorers are not implemented as hooks, as we have to be able to inspect them
     * - however, automatic inspection is horrible. We might change this system in future
     */
    public abstract void initializePlugin(PluginInitializer initializer);

    /**
     * By default, only decompositions from the same ion mode are allowed within a graph. Overwrite this method
     * to change the default behaviour and allow fragments from different ion modes
     * @param input processed input
     * @param candidate ionization of the precursor ion
     * @param ionModes set of all allowed ion modes in the graph. Contains ion mode of the root by default
     */
    public  void addPossibleIonModesToGraph(ProcessedInput input, Ionization candidate, Set<Ionization> ionModes) {

    }

    /**
     * set to true if anything in your plugin clashs with the reduction heuristics
     * @return
     */
    public boolean isGraphReductionForbidden(FGraph graph){
        return false;
    }

    public static class PluginInitializer {

        protected final FragmentationPatternAnalysis analysis;

        PluginInitializer(FragmentationPatternAnalysis analysis) {
            this.analysis = analysis;
        }

        public void addGeneralGraphScorer(GeneralGraphScorer scorer) {
            analysis.getGeneralGraphScorers().add(scorer);
        }

        public <T> void addFragmentScorer(FragmentScorer<T> scorer) {
            analysis.getFragmentScorers().add(scorer);
        }

        public <T> void addLossScorer(LossScorer<T> scorer) {
            analysis.getLossScorers().add(scorer);
        }

        public <T> void addPeakScorer(PeakScorer scorer) {
            analysis.getFragmentPeakScorers().add(scorer);
        }

        public <T> void addFragmentScorer(DecompositionScorer<T> scorer) {
            analysis.getDecompositionScorers().add(scorer);
        }

        public <T> void addRootScorer(DecompositionScorer<T> scorer) {
            analysis.getRootScorers().add(scorer);
        }

        public FragmentationPatternAnalysis getAnalysis() {
            return analysis;
        }
    }

    /*
     * Hooks
     * These methods allow to hook into the fragmentation tree analysis and change the behaviour of the tree computation.
     * It will not be possible to think of all possible special behaviour patterns. I will cover only the things which
     * are already in use. Every new pattern can be integrated afterwards. Hooks should have an empty default implementation.
     */

    protected void afterPreprocessing(ProcessedInput input) {

    }
    protected void beforeDecomposing(ProcessedInput input) {

    }

    protected DecompositionList transformDecompositionList(ProcessedInput input, ProcessedPeak peak, DecompositionList list) {
        return list;
    }

    protected void beforePeakScoring(ProcessedInput input) {

    }

    protected void beforeGraphBuilding(ProcessedInput input) {

    }

    /**
     * if not null, add a new LossValidator which might delete edges in the graph.
     * Because number of edges can get very large, using a validator is more efficient than filtering them afterwards
     * @return
     */
    protected LossValidator filterLossesInGraph(ProcessedInput input, Decomposition root) {
        return null;
    }

    /**
     * is called after building the graph. Is still allowed to add new nodes and egdes to the graph
     */
    protected void afterGraphBuilding(ProcessedInput input, FGraph graph) {

    }

    protected void transferAnotationsFromInputToGraph(ProcessedInput input, FGraph graph) {

    }

    protected void beforeGraphScoring(ProcessedInput input, FGraph graph) {
    }

    protected void afterGraphScoring(ProcessedInput input, FGraph graph) {
    }

    protected void afterTreeComputation(ProcessedInput input, FGraph graph, FTree tree) {
    }

    protected void transferAnotationsFromGraphToTree(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2treeFragments) {
    }

    /**
     * this method is ONLY called for optimal trees that are reported to the user
     */
    protected void releaseTreeToUser(ProcessedInput input, FGraph graph, FTree tree) {
    }








    }
