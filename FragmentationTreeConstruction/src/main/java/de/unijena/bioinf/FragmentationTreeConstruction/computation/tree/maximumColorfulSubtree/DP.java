package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;

import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.functional.iterator.Iterators;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;

import java.util.*;

class DP {


    private final FragmentationGraph graph;
    private final DPTable[] tables;
    private final List<GraphFragment> vertices;
    private final boolean transitiveClosure;
    private final int maxNumberOfColors;
    private final double epsilon;
    private final MaximumColorfulSubtreeAlgorithm algo;

    public DP(MaximumColorfulSubtreeAlgorithm algo, FragmentationGraph graph, int k, boolean transitiveClosure) {
        this.algo = algo;
        this.graph = graph;
        this.vertices = graph.verticesInPostOrder(k);
        this.tables = new DPTable[graph.numberOfVertices()];
        this.maxNumberOfColors = k;
        double epsilon = 1e-6;
        this.transitiveClosure = transitiveClosure;
        for (GraphFragment vertex : vertices) {
            for (Loss l : vertex.getIncomingEdges()) {
                final double abs = Math.abs(l.getWeight());
                epsilon = Math.min(Math.abs(l.getWeight()/10d), epsilon);
            }
            tables[vertex.getIndex()] = new DPTable(algo, vertex.getColor(), colorsetFor(vertex));
        }
        this.epsilon = epsilon;
    }

    /*
        Computation
     */

    public FragmentationTree runAlgorithm() {
        double score = compute();
        final FragmentationTree tree = backtrack();
        final double additionalScore = attachRemainingColors(tree);
        tree.setScore(tree.getScore()+additionalScore);
        assert tree.isComputationCorrect(graph.getRootScore());
        return tree;
    }

    protected double compute() {
        for (int i=0; i < vertices.size(); ++i) {
            final Fragment u = vertices.get(i);
            final DPTable W = tables[u.getIndex()];
            final int[] sets = W.keys;
            pullAllFromChildren(u);
            for (int j=1; j < sets.length; ++j) {
                final int S = sets[j];
                W.update(S|W.vertexBit, distributeColors(u, S));
            }
        }
        return Math.max(0, tables[graph.getRoot().getIndex()].bestScore());
    }

    protected FragmentationTree backtrack() {
        double scoreSum = 0d;
        final GraphFragment rootVertex = graph.getRoot();
        final int rootId = rootVertex.getIndex();
        final double optScore = tables[rootId].bestScore();
        final FragmentationTree tree = new FragmentationTree(graph.getRootScore() + Math.max(optScore, 0), graph);
        final TraceItem root = new TraceItem(rootVertex, tree.getRoot(), tables[rootId].bitset & ~tables[rootId].vertexBit,
                optScore);
        final ArrayDeque<TraceItem> stack = new ArrayDeque<TraceItem>();
        stack.add(root);
        while (!stack.isEmpty()) {
            final TraceItem node = stack.pop();
            if (isLeq(node.accumulatedWeight, 0)) continue;
            final GraphFragment u = node.vertex;
            // which bitset decomposition is used?
            final DPTable table = tables[u.getIndex()];
            final int[] subsetsOfS = algo.subsetsFor(node.bitset);
            final int n = subsetsOfS.length;
            final double accWeight = node.accumulatedWeight;
            final int maxToLookup = n/2 + n%2;
            final int m = n-1;
            final int stackSize = stack.size();
            for (int i=1; i < maxToLookup; ++i) {
                final int S = subsetsOfS[i];
                final int notS = subsetsOfS[m-i];
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
                final double subtreeWeight = backtrackChild(tree, stack, node);
                assert !Double.isInfinite(subtreeWeight);
                scoreSum += subtreeWeight;
            }
        }
        if (!isEqual(scoreSum, Math.max(tables[rootId].bestScore(), 0) )) {
            throw new RuntimeException("Critical Error: Backtracked score " + scoreSum +
                    " is not equal to computed score " + tables[rootId].bestScore());
        }
        return tree;
    }

    protected double backtrackChild(final FragmentationTree tree, Deque<TraceItem> stack, TraceItem node) {
        for (Loss l : node.vertex.getOutgoingEdges()) {
            final GraphFragment v = (GraphFragment)l.getTail();
            final int color = v.getColor();
            if (color > maxNumberOfColors) continue;
            final int bits = (1<<color);
            if ((bits & node.bitset) == bits && ((tables[v.getIndex()].bitset & node.bitset) == node.bitset)) {
                final int childBitset = node.bitset & ~bits;
                final double subtreeWeight = tables[v.getIndex()].get(childBitset);
                final double weight = l.getWeight();
                if (isEqual(subtreeWeight + weight, node.accumulatedWeight)) {
                    final TreeFragment childNode = tree.addVertex(node.treeNode, l);
                    if (!isLeq(subtreeWeight, 0)) {
                        final TraceItem childItem = new TraceItem(v, childNode, childBitset,
                                subtreeWeight );
                        stack.push(childItem);
                    }
                    return weight;
                }
            }
        }
        assert false;
        return Double.NEGATIVE_INFINITY;
    }

    protected double attachRemainingColors(FragmentationTree tree) {

        final double scoreOfTree;
        {
            double s = 0d;
            for (Loss l : Iterators.asIterable(tree.lossIterator())) {
                s += l.getWeight();
            }
            scoreOfTree = s;
        }

        final int remainingColors = graph.numberOfColors() - (maxNumberOfColors+1);
        if (remainingColors <= 0) return 0d;
        double additionalScore = 0d;
        int n = graph.numberOfVertices() - vertices.size();
        final ArrayList<GraphFragment>[] remainings = new ArrayList[remainingColors];
        for (int c = maxNumberOfColors+1; c < graph.numberOfColors(); ++c ) {
            remainings[c - (maxNumberOfColors+1)] = new ArrayList<GraphFragment>();
        }
        for (int i=0; i < graph.numberOfVertices(); ++i) {
            final int c = graph.getFragment(i).getColor() - (maxNumberOfColors + 1);
            if (c >= 0) {
                remainings[c].add(graph.getFragment(i));
            }
        }
        final List<TreeFragment> nodes = Iterators.toList(
                new PostOrderTraversal<TreeFragment>(tree.getRoot(), FragmentationTree.getAdapter()).iterator()
        );
        for (int colorIndex=0; colorIndex < remainings.length; ++colorIndex) {
            double bestScore = 0d;
            TreeFragment bestCandidate = null;
            GraphFragment bestRemaining = null;
            Loss bestEdge = null;
            for (int i=0; i < remainings[colorIndex].size(); ++i) {
                final GraphFragment v = remainings[colorIndex].get(i);
                for (TreeFragment treeNode : nodes) {
                    final GraphFragment u = graph.getFragment(treeNode.getIndex());
                    assert u.getDecomposition().equals(treeNode.getDecomposition());
                    final Loss uv = u.getEdgeTo(v.getIndex());
                    if (uv == null) continue;
                    double weight = uv.getWeight();
                    if (Double.isInfinite(weight)) continue;
                    for (Loss uw : treeNode.getOutgoingEdges()) {
                        final Loss vw = v.getEdgeTo(uw.getTail().getIndex());
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
                final GraphFragment graphV = bestRemaining;
                additionalScore += bestScore;

                final double scoreBefore = scoreOfChildren(bestCandidate);

                final TreeFragment v = tree.addVertex(bestCandidate, bestEdge);
                final Iterator<Loss> iterator = bestCandidate.getOutgoingEdges().iterator();
                final TreeFragment u = bestCandidate;
                final ArrayList<Loss> improvementEdges = new ArrayList<Loss>();

                while (iterator.hasNext()) {
                    final Loss uw = iterator.next();
                    assert uw.getHead() == u;
                    final Fragment w = uw.getTail();
                    if (w == v) continue;
                    final Loss improvementEdge = graphV.getEdgeTo(w.getIndex());
                    if (improvementEdge == null) continue;
                    final double improvement = improvementEdge.getWeight() - uw.getWeight();
                    if (improvement > 0) {
                        improvementEdges.add(improvementEdge);
                    }
                };
                for (Loss vw : improvementEdges) {
                    assert u.getEdgeTo(vw.getTail().getIndex()) != null;
                    tree.reconnect(u.getEdgeTo(vw.getTail().getIndex()), v.getParentEdge(), vw);
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
            for (Loss l : Iterators.asIterable(tree.lossIterator())) {
                s += l.getWeight();
            }
            newScoreOfTree = s;
        }

        assert (Math.abs(newScoreOfTree - scoreOfTree - additionalScore) < 1e-6);


        return additionalScore;
    }

    private double scoreOfChildren(TreeFragment f) {
        double score = 0d;
        for (Loss l : f.getOutgoingEdges()) score += l.getWeight();
        return score;
    }


    protected void pullAllFromChildren(Fragment u) {
        final DPTable W_u = tables[u.getIndex()];
        final int colorOfU = W_u.color;
        final int bitU = W_u.vertexBit;
        for (Loss l : u.getOutgoingEdges()) {
            final Fragment v = l.getTail();
            if (v.getColor() > maxNumberOfColors) continue;
            final DPTable W = tables[v.getIndex()];
            if (W.color == W_u.color) continue;
            assert (W.bitset & W_u.bitset) == W.bitset;
            assert (W.vertexBit & W_u.bitset) == W.vertexBit;
            double edgeWeight = l.getWeight();
            if (transitiveClosure) edgeWeight += scoreTransitiveClosure(colorOfU, v.getColor());
            W_u.update(W.vertexBit | bitU, edgeWeight); // use only child vertex
            final int[] subsetsInChild = W.keys;
            for (int j=1; j < subsetsInChild.length; ++j) {
                final int S = subsetsInChild[j] | W.vertexBit;
                assert ((S & ~W.vertexBit) & W.bitset)==(S & ~W.vertexBit);
                if ((S & bitU) != 0) continue;
                final double score = W.getDirect(j) + edgeWeight;
                if (score > 0) {
                    W_u.update(S | bitU, score);
                }
            }
        }
    }

    protected double distributeColors(Fragment u, int S) {
        final DPTable W_u = tables[u.getIndex()];
        final int[] subsetsOfS = algo.subsetsFor(S);
        final int n = subsetsOfS.length;
        final int maxToLookup = n/2 + n%2;
        final int m = n-1;
        double opt = Double.NEGATIVE_INFINITY;
        for (int i=0; i < maxToLookup; ++i) {
            assert (subsetsOfS[i] | subsetsOfS[m-i]) == S  && (subsetsOfS[i] & subsetsOfS[m-i]) == 0;
            final double score = W_u.get(subsetsOfS[i]) + W_u.get(subsetsOfS[m-i]);
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

    /*
        1. Lösche Knoten deren Farbe nur einmal im Graphen vorkommt. Kontrahiere deren Kanten
        2. Speichere die gelöschten knoten ab, so dass sie später wieder eingefügt werden können
     */


    protected int colorsetFor(Fragment u) {
        int color = (1<<u.getColor());
        for (Fragment v : u.getChildren()) {
            if (v.getColor() > maxNumberOfColors) continue;
            final DPTable table = tables[v.getIndex()];
            color |= table.bitset;
            color |= table.vertexBit;
        }
        return color;
    }

    private boolean isEqual(double a, double b) {
        return Math.abs(a-b) <= epsilon;
    }

    private boolean isLeq(double a, double b) {
        return a<=b || isEqual(a, b);
    }

    private boolean computationIsCorrect(FragmentationTree result, FragmentationGraph graph) {
        double score = graph.getRootScore();
        final Iterator<TreeFragment> nodes =
                new PostOrderTraversal<TreeFragment>(result.getRoot(), FragmentationTree.getAdapter()).iterator();
        final BitSet colors = new BitSet(graph.numberOfColors());
        while (nodes.hasNext()) {
            final TreeFragment node = nodes.next();
            final int color = node.getColor();
            if (colors.get(color)) return false;
            colors.set(color, true);
            if (!node.isRoot()) score += node.getParentEdge().getWeight();
        }
        return isEqual(score, result.getScore());
    }

    private boolean isColorful(FragmentationTree result) {
        final Iterator<TreeFragment> nodes =
                new PostOrderTraversal<TreeFragment>(result.getRoot(), FragmentationTree.getAdapter()).iterator();
        final BitSet colors = new BitSet(graph.numberOfColors());
        while (nodes.hasNext()) {
            final int color = nodes.next().getColor();
            if (colors.get(color)) return false;
            colors.set(color, true);
        }
        return true;
    }


}
