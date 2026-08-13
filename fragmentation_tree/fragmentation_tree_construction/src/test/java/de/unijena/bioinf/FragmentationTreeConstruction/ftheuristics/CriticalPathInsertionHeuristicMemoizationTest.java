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

package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The critical path sweep memoizes the score of every vertex in {@code criticalPaths}. Its runtime
 * guarantee - every vertex is computed once per insertion step - holds only as long as a computed
 * score is distinguishable from the "not computed yet" marker. A single non-finite loss weight used
 * to break that: {@code Math.max(x, NaN)} is NaN, NaN was the marker, so the memo silently switched
 * itself off and the sweep degenerated into an enumeration of every path in the graph. On a real
 * fragmentation graph that is not slow, it does not terminate.
 * <p>
 * Non-finite loss weights are reachable in production: forbidden losses score
 * {@link Double#NEGATIVE_INFINITY} (e.g. AdductSwitchLossScorer, CollisionEnergyEdgeScorer), and
 * {@code Math.log(Erf.erfc(...))} underflows to -Infinity for large mass deviations. The graph
 * scoring only guards those with assertions, which are disabled in production.
 */
public class CriticalPathInsertionHeuristicMemoizationTest {

    private static final int LAYERS = 10, WIDTH = 5;

    /** Counts how often the sweep enters a vertex, so a broken memo shows up as a number, not a hang. */
    private static class CountingHeuristic extends CriticalPathInsertionHeuristic {
        long visits = 0;

        CountingHeuristic(FGraph graph) {
            super(graph);
        }

        @Override
        protected double recomputeCriticalScore(int vertexId) {
            ++visits;
            return super.recomputeCriticalScore(vertexId);
        }
    }

    /**
     * One vertex may be entered at most once per insertion step, plus once per selectable edge that
     * points at it. Anything within a small multiple of vertices*steps is memoized; the degenerate
     * enumeration exceeds it by three orders of magnitude.
     */
    private static long visitBudget(FGraph graph) {
        return 4L * graph.numberOfVertices() * LAYERS;
    }

    @Test
    public void memoizationHoldsWithFiniteWeights() {
        final FGraph graph = layeredGraph();
        final CountingHeuristic h = new CountingHeuristic(graph);
        final FTree tree = h.solve();

        assertNotNull(tree);
        assertEquals(LAYERS, tree.numberOfVertices(), "one fragment per color");
        assertTrue(h.visits <= visitBudget(graph),
                "sweep visited " + h.visits + " vertices, budget " + visitBudget(graph));
    }

    @Test
    public void memoizationSurvivesNaNLossWeight() {
        final FGraph graph = layeredGraph();
        deepestEdge(graph).setWeight(Double.NaN);

        final CountingHeuristic h = new CountingHeuristic(graph);
        h.solve();

        assertTrue(h.visits <= visitBudget(graph),
                "a single NaN loss weight must not disable memoization - sweep visited " + h.visits
                        + " vertices, budget " + visitBudget(graph));
    }

    /**
     * A forbidden loss scores -Infinity. It must be memoized like any other score and must never be
     * selected, since the sweep clamps a critical path at 0.
     */
    @Test
    public void forbiddenLossesAreMemoizedAndNeverSelected() {
        final FGraph graph = layeredGraph();
        deepestEdge(graph).setWeight(Double.NEGATIVE_INFINITY);

        final CountingHeuristic h = new CountingHeuristic(graph);
        final FTree tree = h.solve();

        assertNotNull(tree);
        assertTrue(Double.isFinite(tree.getTreeWeight()),
                "a -Infinity loss must not end up in the tree weight: " + tree.getTreeWeight());
        assertTrue(h.visits <= visitBudget(graph),
                "sweep visited " + h.visits + " vertices, budget " + visitBudget(graph));
    }

    /** A +Infinity and a -Infinity on the same path produce NaN without any NaN input. */
    @Test
    public void memoizationSurvivesMixedInfinities() {
        final FGraph graph = layeredGraph();
        final Fragment deepest = graph.getFragmentAt(graph.numberOfVertices() - 1);
        deepest.getIncomingEdge(0).setWeight(Double.POSITIVE_INFINITY);
        final Fragment above = deepest.getIncomingEdge(0).getSource();
        for (int i = 0; i < above.getInDegree(); ++i)
            above.getIncomingEdge(i).setWeight(Double.NEGATIVE_INFINITY);

        final CountingHeuristic h = new CountingHeuristic(graph);
        h.solve();

        assertTrue(h.visits <= visitBudget(graph),
                "+Infinity above -Infinity yields NaN and must not disable memoization - sweep visited "
                        + h.visits + " vertices, budget " + visitBudget(graph));
    }

    /** The last edge of the graph, i.e. the one deepest in the recursion. */
    private static Loss deepestEdge(FGraph graph) {
        return graph.getFragmentAt(graph.numberOfVertices() - 1).getIncomingEdge(0);
    }

    /**
     * A layered graph, every vertex of a layer connected to every vertex of the next one - the shape
     * of a fragmentation graph with one decomposition set per peak. Sized so that a broken memo needs
     * a few hundred thousand visits: well past any sane budget, but still bounded, so a regression
     * fails in milliseconds instead of hanging the build.
     */
    private static FGraph layeredGraph() {
        final FGraph graph = new FGraph();
        final Charge ion = new Charge(1);
        final Fragment root = graph.addRootVertex(formulaOfLayer(0), ion);
        root.setColor(0);
        graph.getRoot().getOutgoingEdge(0).setWeight(1d);

        Fragment[] previous = new Fragment[]{root};
        for (int layer = 1; layer < LAYERS; ++layer) {
            final Fragment[] current = new Fragment[WIDTH];
            for (int i = 0; i < WIDTH; ++i) {
                current[i] = graph.addFragment(formulaOfLayer(layer), ion);
                current[i].setColor(layer);
            }
            for (Fragment u : previous)
                for (Fragment v : current)
                    graph.addLoss(u, v).setWeight(1d);
            previous = current;
        }
        return graph;
    }

    private static MolecularFormula formulaOfLayer(int layer) {
        final int carbons = LAYERS + 2 - layer;
        return MolecularFormula.parseOrThrow("C" + carbons + "H" + (2 * carbons));
    }
}
