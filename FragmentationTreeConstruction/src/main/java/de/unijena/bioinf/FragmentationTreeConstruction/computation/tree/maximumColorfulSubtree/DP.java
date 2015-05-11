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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;

import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;

import java.util.*;

class DP {


    private final FGraph graph;
    private final DPTable[] tables;
    private final List<Fragment> vertices;
    private final boolean transitiveClosure;
    private final int maxNumberOfColors;
    private final double epsilon;
    private final MaximumColorfulSubtreeAlgorithm algo;
    private Ano ano;

    public DP(MaximumColorfulSubtreeAlgorithm algo, FGraph graph, int k, boolean transitiveClosure) {
        this.algo = algo;
        this.graph = graph;
        //this.vertices = new ArrayList<Fragment>();
        //while (fiter.hasNext()) vertices.add(fiter.next());


        // order vertices by intensity
        final FragmentAnnotation<ProcessedPeak> ano = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        vertices = new ArrayList<Fragment>(graph.getFragmentsWithoutRoot());
        Collections.sort(vertices, new Comparator<Fragment>() {
            @Override
            public int compare(Fragment fragment, Fragment fragment2) {
                return Double.compare(ano.get(fragment2).getRelativeIntensity(), ano.get(fragment).getRelativeIntensity());
            }
        });
        for (int i = 0; i < vertices.size(); ++i) {
            vertices.get(i).setColor(i + 1);
        }
        graph.getRoot().getOutgoingEdge(0).getTarget().setColor(0);


        this.tables = new DPTable[graph.numberOfVertices()];
        this.maxNumberOfColors = k;
        double epsilon = 1e-6;
        this.transitiveClosure = transitiveClosure;

        final BitSet colorBitSet = new BitSet(graph.maxColor() + 1);
        for (int i = 0; i <= k; ++i) colorBitSet.set(i);
        final Iterator<Fragment> fiter = graph.postOrderIterator(graph.getRoot().getChildren(0), colorBitSet);

        this.vertices.clear();
        while (fiter.hasNext()) vertices.add(fiter.next());

        for (Fragment vertex : vertices) {
            for (int i = 0; i < vertex.getInDegree(); ++i) {
                final Loss l = vertex.getIncomingEdge(i);
                final double abs = Math.abs(l.getWeight());
                if (abs > 0) epsilon = Math.min(Math.abs(l.getWeight() / 10d), epsilon);
            }
            tables[vertex.getVertexId()] = new DPTable(algo, vertex.getColor(), colorsetFor(vertex));
        }
        this.epsilon = epsilon;
    }

    /*
        Computation
     */

    protected static FTree newTree(FGraph graph, FTree tree) {
        tree.addAnnotation(ProcessedInput.class, graph.getAnnotationOrThrow(ProcessedInput.class));
        tree.addAnnotation(Ionization.class, graph.getAnnotationOrThrow(Ionization.class));
        for (Map.Entry<Class<Object>, Object> entry : graph.getAnnotations().entrySet()) {
            tree.setAnnotation(entry.getKey(), entry.getValue());
        }
        return tree;
    }

    public FTree runAlgorithm() {
        double score = compute();
        final FTree tree = newTree(graph, backtrack());
        final double additionalScore = attachRemainingColors(tree);
        final Fragment graphRoot = graph.getRoot().getChildren(0);
        final TreeScoring scoring = tree.getAnnotationOrThrow(TreeScoring.class);
        scoring.setOverallScore(scoring.getOverallScore() + additionalScore);

        assert computationIsCorrect(tree, graph);
        return tree;
    }

    protected Ano getAno(FTree tree) {
        if (ano != null && ano.tree == tree) return ano;
        else {
            final Ano newAno = new Ano(graph, tree);
            this.ano = newAno;
            return ano;
        }
    }

    protected double compute() {
        for (int i = 0; i < vertices.size(); ++i) {
            final Fragment u = vertices.get(i);
            final DPTable W = tables[u.getVertexId()];
            final int[] sets = W.keys;
            pullAllFromChildren(u);
            for (int j = 1; j < sets.length; ++j) {
                final int S = sets[j];
                W.update(S | W.vertexBit, distributeColors(u, S));
            }
        }
        return Math.max(0, tables[graph.getRoot().getChildren(0).getVertexId()].bestScore());
    }

    /**
     * backtrack from root
     *
     * @return
     */
    protected FTree backtrack() {
        return backtrack(graph.getRoot().getChildren(0));
    }

    /**
     * backtrack best tree for each vertex once as root
     *
     * @return
     */
    protected List<FTree> backTrackAll() {
        List<FTree> fragmentationTrees = new ArrayList<FTree>();
        for (Fragment vertex : vertices) {
            final FTree tree = backtrack(vertex);
            fragmentationTrees.add(tree);
        }
        return Collections.unmodifiableList(fragmentationTrees);
    }

    protected FTree backtrack(Fragment vertex) {
        double scoreSum = 0d;
        final int vertexId = vertex.getVertexId();
        final double optScore = tables[vertexId].bestScore();
        final FTree tree = new FTree(vertex.getFormula());
        final TreeScoring treeScoring = new TreeScoring();
        tree.addAnnotation(TreeScoring.class, treeScoring);

        treeScoring.setRootScore(vertex.getIncomingEdge().getWeight());
        treeScoring.setOverallScore(Math.max(0, optScore) + treeScoring.getRootScore());

        Ano ano = getAno(tree);
        ano.set(tree.getRoot(), vertex);

        final TraceItem root = new TraceItem(vertex, tree.getRoot(), tables[vertexId].bitset & ~tables[vertexId].vertexBit,
                optScore);
        final ArrayDeque<TraceItem> stack = new ArrayDeque<TraceItem>();
        stack.add(root);
        while (!stack.isEmpty()) {
            final TraceItem node = stack.pop();
            if (isLeq(node.accumulatedWeight, 0)) continue;
            final Fragment u = node.vertex;
            // which bitset decomposition is used?
            final DPTable table = tables[u.getVertexId()];
            final int[] subsetsOfS = algo.subsetsFor(node.bitset);
            final int n = subsetsOfS.length;
            final double accWeight = node.accumulatedWeight;
            final int maxToLookup = n / 2 + n % 2;
            final int m = n - 1;
            final int stackSize = stack.size();
            for (int i = 1; i < maxToLookup; ++i) {
                final int S = subsetsOfS[i];
                final int notS = subsetsOfS[m - i];
                final double scoreS = Math.max(table.get(S), 0);
                final double scoreNotS = Math.max(table.get(notS), 0);
                if (isEqual(scoreS + scoreNotS, accWeight)) {
                    if (!isLeq(scoreS, 0)) {
                        final TraceItem sNode = new TraceItem(u, node.treeNode, S, scoreS);
                        stack.push(sNode);
                    }
                    if (!isLeq(scoreNotS, 0)) {
                        final TraceItem notSNode = new TraceItem(u, node.treeNode, notS, scoreNotS);
                        stack.push(notSNode);
                    }
                    break;
                }
            }
            if (stackSize >= stack.size()) { // if no further decomposition is found, look into subtrees
                final double subtreeWeight = backtrackChild(tree, stack, node, ano);
                assert !Double.isInfinite(subtreeWeight);
                scoreSum += subtreeWeight;
            }
        }
        if (!isEqual(scoreSum, Math.max(tables[vertexId].bestScore(), 0))) {
            throw new RuntimeException("Critical Error: Backtracked score " + scoreSum +
                    " is not equal to computed score " + tables[vertexId].bestScore());
        }
        return tree;
    }

    protected double backtrackChild(final FTree tree, Deque<TraceItem> stack, TraceItem node, Ano ano) {
        for (int li = 0; li < node.vertex.getOutDegree(); ++li) {
            final Loss l = node.vertex.getOutgoingEdge(li);
            final Fragment v = l.getTarget();
            final int color = v.getColor();
            if (color > maxNumberOfColors) continue;
            final int bits = (1 << color);
            if ((bits & node.bitset) == bits && ((tables[v.getVertexId()].bitset & node.bitset) == node.bitset)) {
                final int childBitset = node.bitset & ~bits;
                final double subtreeWeight = tables[v.getVertexId()].get(childBitset);
                final double weight = l.getWeight();
                if (isEqual(subtreeWeight + weight, node.accumulatedWeight)) {
                    final Fragment childNode = tree.addFragment(node.treeNode, l.getTarget().getFormula());
                    ano.set(childNode.getIncomingEdge(), l);
                    if (!isLeq(subtreeWeight, 0)) {
                        final TraceItem childItem = new TraceItem(v, childNode, childBitset,
                                subtreeWeight);
                        stack.push(childItem);
                    }
                    return weight;
                }
            }
        }
        assert false;
        return Double.NEGATIVE_INFINITY;
    }

    protected double attachRemainingColors(FTree tree) {
        Ano ano = getAno(tree);
        final int NColors = graph.maxColor() + 1;
        final double scoreOfTree;
        {
            double s = 0d;
            final Iterator<Loss> liter = tree.lossIterator();
            while (liter.hasNext()) {
                s += liter.next().getWeight();
            }
            scoreOfTree = s;
        }

        final BiMap<Fragment, Fragment> tree2GraphMapping = FTree.createFragmentMapping(tree, graph);
        final Map<Fragment, Fragment> graph2treeMapping = tree2GraphMapping.inverse();

        final int remainingColors = NColors - (maxNumberOfColors + 1);
        if (remainingColors <= 0) return 0d;
        double additionalScore = 0d;
        int n = graph.numberOfVertices() - vertices.size();
        final ArrayList<Fragment>[] remainings = new ArrayList[remainingColors];
        for (int c = maxNumberOfColors + 1; c < NColors; ++c) {
            remainings[c - (maxNumberOfColors + 1)] = new ArrayList<Fragment>();
        }
        for (int i = 1; i < graph.numberOfVertices(); ++i) {
            final int c = graph.getFragmentAt(i).getColor() - (maxNumberOfColors + 1);
            if (c >= 0) {
                remainings[c].add(graph.getFragmentAt(i));
            }
        }
        final List<Fragment> nodes = Lists.newArrayList(
                new PostOrderTraversal<Fragment>(tree.getRoot(), FTree.treeAdapter()).iterator()
        );
        for (int colorIndex = 0; colorIndex < remainings.length; ++colorIndex) {
            double bestScore = 0d;
            Fragment bestCandidate = null;
            Fragment bestRemaining = null;
            Loss bestEdge = null;
            for (int i = 0; i < remainings[colorIndex].size(); ++i) {
                final Fragment v = remainings[colorIndex].get(i);
                assert tree2GraphMapping.get(v) == null;
                for (Fragment treeNode : nodes) {
                    final Fragment u = tree2GraphMapping.get(treeNode);
                    assert u.getFormula().equals(treeNode.getFormula());
                    final Loss uv = graph.getLoss(u, v);
                    if (uv == null) continue;
                    double weight = uv.getWeight();
                    if (Double.isInfinite(weight)) continue;
                    for (Loss uw : treeNode.getOutgoingEdges()) {
                        final Loss vw = graph.getLoss(v, tree2GraphMapping.get(uw.getTarget()));
                        if (vw == null) continue;
                        final double improvement = vw.getWeight() - uw.getWeight();
                        if (improvement > 0) {
                            weight += improvement;
                        }
                    }
                    if (weight > bestScore) {
                        bestScore = weight;
                        bestCandidate = treeNode;
                        bestRemaining = v;
                        bestEdge = uv;
                    }
                }
            }
            if (bestScore > 0) {
                final Fragment graphV = bestRemaining;
                additionalScore += bestScore;

                final double scoreBefore = scoreOfChildren(bestCandidate);

                final Fragment v = tree.addFragment(bestCandidate, bestEdge.getTarget().getFormula());
                v.getIncomingEdge().setWeight(bestEdge.getWeight());
                ano.set(v, bestEdge.getTarget());
                tree2GraphMapping.put(v, bestEdge.getTarget());
                final Iterator<Loss> iterator = bestCandidate.getOutgoingEdges().iterator();
                final Fragment u = bestCandidate;
                final ArrayList<Loss> improvementEdges = new ArrayList<Loss>();

                while (iterator.hasNext()) {
                    final Loss uw = iterator.next();
                    assert uw.getSource() == u;
                    final Fragment w = uw.getTarget();
                    if (w == v) continue;
                    final Loss improvementEdge = graph.getLoss(graphV, tree2GraphMapping.get(w));
                    if (improvementEdge == null) continue;
                    final double improvement = improvementEdge.getWeight() - uw.getWeight();
                    if (improvement > 0) {
                        improvementEdges.add(improvementEdge);
                    }
                }
                ;
                for (Loss vw : improvementEdges) {
                    assert tree.getLoss(u, vw.getTarget().getFormula()) != null;
                    Fragment improvementChild = null;
                    for (int i = 0; i < u.getOutDegree(); ++i) {
                        if (u.getChildren(i).getFormula().equals(vw.getTarget().getFormula())) {
                            improvementChild = u.getChildren(i);
                            break;
                        }
                    }
                    final Loss newLoss = tree.swapLoss(improvementChild, v);
                    newLoss.setWeight(vw.getWeight());
                    ano.set(newLoss, vw);
                }

                nodes.add(v);

                final double scoreAfter = scoreOfChildren(bestCandidate) + scoreOfChildren(v);
                assert Collections.disjoint(bestCandidate.getChildren(), v.getChildren());

                assert scoreAfter > scoreBefore;
                assert (Math.abs((scoreAfter - scoreBefore) - bestScore) < 1e-6);

            }
        }

        final double newScoreOfTree;
        {
            double s = 0d;
            final Iterator<Loss> liter = tree.lossIterator();
            while (liter.hasNext()) {
                s += liter.next().getWeight();
            }
            newScoreOfTree = s;
        }

        assert (Math.abs(newScoreOfTree - scoreOfTree - additionalScore) < 1e-6);


        return additionalScore;
    }

    private double scoreOfChildren(Fragment f) {
        double score = 0d;
        for (Loss l : f.getOutgoingEdges()) score += l.getWeight();
        return score;
    }

    protected void pullAllFromChildren(Fragment u) {
        final DPTable W_u = tables[u.getVertexId()];
        final int colorOfU = W_u.color;
        final int bitU = W_u.vertexBit;
        for (Loss l : u.getOutgoingEdges()) {
            final Fragment v = l.getTarget();
            if (v.getColor() > maxNumberOfColors) continue;
            final DPTable W = tables[v.getVertexId()];
            if (W.color == W_u.color) continue;
            assert (W.bitset & W_u.bitset) == W.bitset;
            assert (W.vertexBit & W_u.bitset) == W.vertexBit;
            double edgeWeight = l.getWeight();
            if (transitiveClosure) edgeWeight += scoreTransitiveClosure(colorOfU, v.getColor());
            W_u.update(W.vertexBit | bitU, edgeWeight); // use only child vertex
            final int[] subsetsInChild = W.keys;
            for (int j = 1; j < subsetsInChild.length; ++j) {
                final int S = subsetsInChild[j] | W.vertexBit;
                assert ((S & ~W.vertexBit) & W.bitset) == (S & ~W.vertexBit);
                if ((S & bitU) != 0) continue;
                final double subscore = W.get(subsetsInChild[j]);
                if (subscore > 0) {
                    final double score = subscore/*W.getDirect(j)*/ + edgeWeight;
                    if (score > 0) {
                        W_u.update(S | bitU, score);
                    }
                }
            }
        }
    }

    protected double distributeColors(Fragment u, int S) {
        final DPTable W_u = tables[u.getVertexId()];
        final int[] subsetsOfS = algo.subsetsFor(S);
        final int n = subsetsOfS.length;
        final int maxToLookup = n / 2 + n % 2;
        final int m = n - 1;
        double opt = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < maxToLookup; ++i) {
            assert (subsetsOfS[i] | subsetsOfS[m - i]) == S && (subsetsOfS[i] & subsetsOfS[m - i]) == 0;
            final double score = W_u.get(subsetsOfS[i]) + W_u.get(subsetsOfS[m - i]);
            opt = Math.max(score, opt);
        }
        return opt;
    }

    private double scoreTransitiveClosure(int colorOfU, int colorOfChild) {
        return 0; // TODO: implement
    }

    protected void preprocessing() {
        //final int[] usedColors = new int[]
    }

    protected int colorsetFor(Fragment u) {
        int color = (1 << u.getColor());
        for (Fragment v : u.getChildren()) {
            if (v.getColor() > maxNumberOfColors) continue;
            final DPTable table = tables[v.getVertexId()];
            color |= table.bitset;
            color |= table.vertexBit;
        }
        return color;
    }

    /*
        1. Lösche Knoten deren Farbe nur einmal im Graphen vorkommt. Kontrahiere deren Kanten
        2. Speichere die gelöschten knoten ab, so dass sie später wieder eingefügt werden können
     */

    private boolean isEqual(double a, double b) {
        return Math.abs(a - b) <= epsilon;
    }

    private boolean isLeq(double a, double b) {
        return a <= b || isEqual(a, b);
    }

    private boolean computationIsCorrect(FTree result, FGraph graph) {
        double score = graph.getLoss(graph.getRoot(), result.getRoot().getFormula()).getWeight();
        final Iterator<Fragment> nodes =
                new PostOrderTraversal<Fragment>(result.getRoot(), FTree.treeAdapter()).iterator();
        final BitSet colors = new BitSet(graph.maxColor() + 1);
        while (nodes.hasNext()) {
            final Fragment node = nodes.next();
            final int color = node.getColor();
            if (colors.get(color))
                return false;
            colors.set(color, true);
            if (!node.isRoot()) score += node.getIncomingEdge().getWeight();
        }
        if (!isEqual(score, result.getAnnotationOrThrow(TreeScoring.class).getOverallScore())) {
            System.err.println(")/");
        }
        return isEqual(score, result.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
    }

    private boolean isColorful(FTree result) {
        final Iterator<Fragment> nodes =
                new PostOrderTraversal<Fragment>(result.getRoot(), FTree.treeAdapter()).iterator();
        final BitSet colors = new BitSet(graph.maxColor() + 1);
        while (nodes.hasNext()) {
            final int color = nodes.next().getColor();
            if (colors.get(color)) return false;
            colors.set(color, true);
        }
        return true;
    }

    private static class Ano {
        private List<FragmentAnnotation<Object>> graphFragmentAnos;
        private List<FragmentAnnotation<Object>> treeFragmentAnos;
        private List<LossAnnotation<Object>> graphLossAnos;
        private List<LossAnnotation<Object>> treeLossAnos;
        private FTree tree;

        private Ano(FGraph graph, FTree tree) {
            this.tree = tree;
            graphFragmentAnos = graph.getFragmentAnnotations();
            graphLossAnos = graph.getLossAnnotations();
            treeFragmentAnos = new ArrayList<FragmentAnnotation<Object>>();
            treeLossAnos = new ArrayList<LossAnnotation<Object>>();
            for (FragmentAnnotation<Object> fa : graphFragmentAnos) {
                treeFragmentAnos.add(tree.addFragmentAnnotation(fa.getAnnotationType()));
            }
            for (LossAnnotation<Object> fa : graphLossAnos) {
                treeLossAnos.add(tree.addLossAnnotation(fa.getAnnotationType()));
            }
        }

        public void set(Fragment treeVertex, Fragment graphVertex) {
            for (int i = 0; i < graphFragmentAnos.size(); ++i) {
                treeFragmentAnos.get(i).set(treeVertex, graphFragmentAnos.get(i).get(graphVertex));
            }
            treeVertex.setColor(graphVertex.getColor());
        }

        public void set(Loss treeLoss, Loss graphLoss) {
            for (int i = 0; i < graphLossAnos.size(); ++i) {
                treeLossAnos.get(i).set(treeLoss, graphLossAnos.get(i).get(graphLoss));
            }
            set(treeLoss.getTarget(), graphLoss.getTarget());
            treeLoss.setWeight(graphLoss.getWeight());
        }

    }


}
