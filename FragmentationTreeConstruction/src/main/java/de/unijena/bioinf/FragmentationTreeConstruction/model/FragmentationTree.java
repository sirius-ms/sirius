package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.functional.Function;
import de.unijena.bioinf.functional.iterator.Iterators;
import de.unijena.bioinf.functional.list.ListOperations;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class FragmentationTree implements Comparable<FragmentationTree>, FragmentationPathway {

    private double score, rootScore;
    private final TreeFragment root;
    private final ProcessedInput input;

    public FragmentationTree(double score, FragmentationGraph graph) {
        this.score = score;
        this.input = graph.getProcessedInput();
        this.root = new TreeFragment(0, null, graph.getRoot().getDecomposition(), null, graph.getRoot().getPeak());
        this.rootScore = graph.getRootScore();
    }

    public FragmentationTree(double score, FragmentationGraph graph, GraphFragment root, double rootScore) {
        this.score = score;
        this.input = graph.getProcessedInput();
        this.root = new TreeFragment(root.getIndex(), null, root.getDecomposition(), null, root.getPeak());
        this.rootScore = rootScore;
    }

    public TreeFragment addVertex(TreeFragment parent, Loss edge) {
        final Fragment child = edge.getTail();
        final TreeFragment treenode = new TreeFragment(child.getIndex(), parent, child.getDecomposition(),
                edge, child.getPeak());
        parent.addOutgoingEdge(treenode.getParentEdge());
        return treenode;
    }

    // remove edge uv and create new edge from u -> v -> w
    // TODO: =/
    public void reconnect(Loss uw, Loss uv, Loss vw) {
        final Fragment u = uw.getHead();
        final Fragment v = uv.getHead();
        final Fragment w = uw.getTail();
        assert u instanceof TreeFragment;
        assert v instanceof TreeFragment;
        assert w instanceof TreeFragment;
        final boolean done = u.getOutgoingEdges().remove(uw);
        assert done;
        final Loss l = new Loss(v, w, vw.getLoss(), vw.getWeight() );
        v.addOutgoingEdge(l);
        ((TreeFragment) w).setParentEdge(l);
    }

    public ProcessedInput getInput() {
        return input;
    }

    public TreeFragment getRoot() {
        return root;
    }

    @Override
    public Ionization getIonization() {
        return getInput().getExperimentInformation().getIonization();
    }

    @Override
    public List<ProcessedPeak> getPeaks() {
        return ListOperations.singleton().map(getFragments(), new Function<TreeFragment, ProcessedPeak>() {
            @Override
            public ProcessedPeak apply(TreeFragment arg) {
                return arg.getPeak();
            }
        });
    }

    @Override
    public List<TreeFragment> getFragments() {
        return Iterators.toList( new PostOrderTraversal(getCursor()).iterator());
    }

    @Override
    public List<TreeFragment> getFragmentsWithoutRoot() {
        final List<TreeFragment> frags = getFragments();
        frags.remove(frags.size()-1);
        return frags;
    }

    public Iterator<Loss> lossIterator() {
        final Iterator<TreeFragment> iter = new PostOrderTraversal<TreeFragment>(getCursor()).iterator();
        if (!iter.hasNext()) return Iterators.empty();
        return new Iterator<Loss>() {
            private TreeFragment fragment = iter.next();
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Loss next() {
                final Loss l = fragment.getParentEdge();
                fragment = iter.next();
                return l;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int numberOfColors() {
        return numberOfVertices();
    }

    @Override
    public int numberOfVertices() {
        return getCursor().numberOfVertices();
    }

    public static TreeAdapter<TreeFragment> getAdapter() {
        return new TreeAdapter<TreeFragment>() {
            @Override
            public int getDegreeOf(TreeFragment vertex) {
                return vertex.outgoingEdges.size();
            }

            @Override
            public List<TreeFragment> getChildrenOf(TreeFragment vertex) {
                return (List<TreeFragment>)((List)vertex.getChildren());
            }
        };
    }

    public TreeCursor<TreeFragment> getCursor() {
        return TreeCursor.getCursor(getRoot(), getAdapter());
    }

    public boolean isComputationCorrect(double rootScore) {
        return isComputationCorrect(rootScore, 1e-6);
    }

    public boolean isComputationCorrect(double rootScore, double tolerance) {
        final double score = getScore();
        double s = rootScore;
        final BitSet usedColors = new BitSet();
        for (TreeFragment f : new PostOrderTraversal<TreeFragment>(getCursor())) {
            final double c = f.isRoot() ? 0d : f.getParentEdge().getWeight();
            if (c != 0) tolerance = Math.min(tolerance, Math.abs(c)/10d);
            s += c;
            if (usedColors.get(f.getColor())) {
                return false;
            } else {
                usedColors.set(f.getColor(), true);
            }
        }
        if (Math.abs(score - s) > tolerance) {
            System.out.println("Error!");
        }
        return Math.abs(score - s) <= tolerance || (Double.compare(score-s, score-s) == 0);
    }

    private double recScore(TreeFragment f) {
        double d = f.isRoot() ? f.getDecomposition().getScore() : f.getParentEdge().getWeight();
        for (Loss l : f.getOutgoingEdges()) {
            d += recScore((TreeFragment)l.getTail());
        }
        return d;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getRootScore() {
        return rootScore;
    }

    public void setRootScore(double rootScore) {
        this.rootScore = rootScore;
    }

    @Override
    public int compareTo(FragmentationTree o) {
        return Double.compare(score, o.getScore());
    }
}
