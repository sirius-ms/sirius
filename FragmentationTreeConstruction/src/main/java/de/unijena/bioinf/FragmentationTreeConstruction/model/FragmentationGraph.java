package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.functional.Function;
import de.unijena.bioinf.functional.iterator.Iterators;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;

/**
 * @author Kai Dührkop
 */
public class FragmentationGraph implements FragmentationPathway {

    private List<GraphFragment> vertices;
    private List<ProcessedPeak> peaks, filteredPeaks;
    private ProcessedInput processedInput;
    private double rootScore;

    public FragmentationGraph(ProcessedInput input) {
        this.processedInput = input;
        this.vertices = new ArrayList<GraphFragment>();
        this.peaks = input.getMergedPeaks();
        this.rootScore = 0d;
    }

    public ProcessedInput getProcessedInput() {
        return processedInput;
    }

    public void setProcessedInput(ProcessedInput processedInput) {
        this.processedInput = processedInput;
    }

    public List<ProcessedPeak> prepareForTreeComputation() {
        final ArrayList<ProcessedPeak> peakList = new ArrayList<ProcessedPeak>(this.peaks);
        // remove useless nodes and edges from tree
        // a node is useless if all it's incomming edges are <= 0 and it is a leaf
        // an edge is useless, if it's score is -infinity
        trim();
        final int[] colorMap = new int[peaks.size()];
        // sort peaks such that the peaks with the highest scores are in front of the list
        final double colorScores[] = new double[peakList.size()];
        Arrays.fill(colorScores, Double.NEGATIVE_INFINITY);
        colorScores[getRoot().getColor()] = Double.POSITIVE_INFINITY; // always keep the root ;)
        for (Fragment f : getFragmentsWithoutRoot()) {
            for (Loss l : f.getIncomingEdges()) {
                colorScores[f.getColor()] = Math.max(colorScores[f.getColor()], l.getWeight());
            }
        }
        Collections.sort(peakList, new Comparator<ProcessedPeak>() {
            @Override
            public int compare(ProcessedPeak o1, ProcessedPeak o2) {
                return Double.compare(colorScores[o2.getIndex()], colorScores[o1.getIndex()]);
            }
        });
        // set root to first position of the array
        {
            final int parentPeakId = processedInput.getParentPeak().getIndex();
            for (int i=0; i < peakList.size(); ++i) {
                if (peakList.get(i).getIndex() == parentPeakId) {
                    if (i == 0) break;
                    final ProcessedPeak p = peakList.get(0);
                    peakList.set(0, peakList.get(i));
                    peakList.set(i, p);
                    break;
                }
            }
        }
        // reset indizes
        int i;
        for (i=0; i < peakList.size(); ++i) {
            final double score = colorScores[peakList.get(i).getIndex()];
            if (score < 0 && Double.isInfinite(score)) break;
            colorMap[peakList.get(i).getIndex()] = i;
        }
        // TODO: Unschön: peak.getIndex() zeigt nicht mehr auf den Index. Eventuell über einen Indizes-Array lösen
        this.filteredPeaks = new ArrayList<ProcessedPeak>(peakList.subList(0, i));


        for (GraphFragment f : getFragments()) {
            f.setColor(colorMap[f.getPeak().getIndex()]);
            assert f.getColor() < peakList.size();
        }

        assert vertices.size() >= filteredPeaks.size();

        return peaks;
    }

    /**
     * Delete all but number peaks, such that the graph becomes smaller and sparser
     * Deletes peaks with lowest intensities
     * Have to be called AFTER prepareForTreeComputation
     * @param number number of peaks which should be retained
     */
    public void retainPeaks(int number) {
        if (vertices.size() <= number) return;
        if (filteredPeaks == null) prepareForTreeComputation();
        final HashSet<GraphFragment> set = new HashSet<GraphFragment>();
        final ArrayDeque<GraphFragment> stack = new ArrayDeque<GraphFragment>(3);
        for (GraphFragment f : getFragmentsWithoutRoot()) {
            if (f.getColor() > number && !set.contains(f)) {
                stack.add(f);
                set.add(f);
            }
        }
        while (!stack.isEmpty()) {
            final GraphFragment f = stack.pop();
            final Iterator<Loss> liter = f.incommingEdges.iterator();
            while (liter.hasNext()) {
                final Loss l = liter.next();
                liter.remove();
                ((GraphFragment)l.getHead()).removeOutgoingEdge(l);
            }
            final Iterator<Loss> oiter = f.outgoingEdges.iterator();
            while (oiter.hasNext()) {
                final Loss l = oiter.next();
                oiter.remove();
                final GraphFragment g = ((GraphFragment)l.getTail());
                g.removeIncommingEdge(l);
                if (g.incommingEdges.isEmpty() && !set.contains(g)) {
                    stack.add(g);
                    set.add(g);
                }
            }
        }
    }

    void deleteFragments(List<GraphFragment> fs) {
        final ArrayDeque<GraphFragment> stack = new ArrayDeque<GraphFragment>();
        for (GraphFragment f : fs) {
            final Iterator<Loss> liter = f.incommingEdges.iterator();
            while (liter.hasNext()) {
                final Loss l = liter.next();
                liter.remove();
                ((GraphFragment)l.getHead()).removeOutgoingEdge(l);
            }
            final Iterator<Loss> oiter = f.outgoingEdges.iterator();
            while (oiter.hasNext()) {
                final Loss l = oiter.next();
                oiter.remove();
                ((GraphFragment)l.getTail()).removeIncommingEdge(l);
            }
        }
    }

    private void trim() {
        /*final long t1 = System.nanoTime();
        final int numberOfVertices = vertices.size();

        */
        final int numberOfEdges = Iterators.count(lossIterator());
        trimLeaves();
        trimEdges();
        int k=0;
        for (GraphFragment f : vertices) {
            f.index = k++;
        }
        final int numberOfEdges2 = Iterators.count(lossIterator());
        //System.err.println(numberOfEdges2 + " / " + numberOfEdges + " edges trimmed");
        /*
        {
            // TODO: Hotfix
            final Iterator<Loss> l = lossIterator();
            while (l.hasNext()) {
                Loss x = l.next();
                if (Double.isInfinite(x.getWeight())) x.setWeight(-10000000d);

            }
        }
        */
        //trimEdges();
        //assert !trimLeaves();
        //trimEdges();
        //assert !trimEdges();
        /*
        final int numberOfVertices2 = vertices.size();
        final int numberOfEdges2 = Iterators.count(lossIterator());
        final long t2 = System.nanoTime();
        System.err.println(numberOfVertices2 + " / " + numberOfVertices + " vertices trimmed");
        System.err.println(numberOfEdges2 + " / " + numberOfEdges + " edges trimmed");
        System.err.println("trim time: " + (t2-t1)/1000000);
        */
    }

    private void trimEdges() {
        for (GraphFragment f : vertices) {
            final ListIterator<Loss> iter = f.outgoingEdges.listIterator();
            while (iter.hasNext()) {
                final Loss l = iter.next();
                if (Double.isInfinite(l.getWeight())) {
                    ((GraphFragment)l.getTail()).removeIncommingEdge(l);
                    iter.remove();
                }
            }
        }
        // remove vertices without path to root
        final HashSet<GraphFragment> keep = new HashSet<GraphFragment>(verticesInPostOrder());
        if (keep.size() < vertices.size()) {
            final HashSet<GraphFragment> toDelete = new HashSet<GraphFragment>(vertices.size()-keep.size());
            for (GraphFragment f : vertices) {
                if (!keep.contains(f)) {
                    toDelete.add(f);
                    for (Loss l : f.outgoingEdges) ((GraphFragment)l.getTail()).removeIncommingEdge(l);
                }
            }
            vertices.removeAll(toDelete);
            //assert vertices.size() == keep.size();
        }
    }

    private boolean validGraph() {
        final HashMap<Integer, GraphFragment> map = new HashMap<Integer, GraphFragment>();
        for (GraphFragment f : vertices) map.put(f.getIndex(), f);
        for (GraphFragment f : vertices) {
            for (Loss l : f.outgoingEdges) if (l.getTail() != null && (map.get(l.getTail().getIndex()) != l.getTail())) return false;
            for (Loss l : f.incommingEdges) {
                if (l.getTail() != null) {
                    if (map.get(l.getTail().getIndex()) != l.getTail()) return false;
                } else if (f.getIndex()!=0) return false;
            }
        }
        for (Loss l : Iterators.asIterable(lossIterator())) {
            assert vertices.get(l.getHead().getIndex()) == l.getHead();
            assert vertices.get(l.getTail().getIndex()) == l.getTail();
        }
        return true;
    }

    /*
    private boolean trimEdges() {
        boolean trimmed = false;
        final List<Loss> edges = Iterators.toList(lossIterator());
        Collections.sort(edges, new Comparator<Loss>() {
            @Override
            public int compare(Loss o1, Loss o2) {
                return Double.compare(o1.getWeight(), o2.getWeight());
            }
        });
        iterate:
        for (Loss l : edges) {
            final GraphFragment f = (GraphFragment)l.getTail();
            if (!Double.isInfinite(l.getWeight())) {
                for (Loss l2 : f.getIncomingEdges()) {
                    if (l.getWeight() >= l2.getWeight()) {
                        continue iterate;
                    }
                }
            }
            f.removeIncommingEdge(l);
            ((GraphFragment)l.getHead()).removeOutgoingEdge(l);
            trimmed = true;
        }
        return trimmed;
    }
    */

    private boolean trimLeaves() {
        int x = vertices.size();
        final TIntArrayList toDelete = new TIntArrayList(20);
        final ArrayList<GraphFragment> ordered = verticesInPostOrder();
        for (int i=0; i < ordered.size()-1; ++i) {
            final GraphFragment fragment = ordered.get(i);
            if (fragment.shouldBeTrimmed()) {
                toDelete.add(fragment.getIndex());
                final Iterator<Loss> out = fragment.incommingEdges.iterator();
                while (out.hasNext()) {
                    final Loss l = out.next();
                    ((GraphFragment)l.getHead()).removeOutgoingEdge(l);
                    out.remove();
                }
            }
        }
        final int[] ids = toDelete.toArray();
        Arrays.sort(ids);
        int n = vertices.size();
        for (int i = ids.length-1; i >= 0; --i) {
            final int deleteIndex = ids[i];
            vertices.set(deleteIndex, vertices.get(n-1));
            vertices.remove(--n);
        }
        int k=0;
        for (GraphFragment vertex : vertices) {
            vertex.index = k++;
        }
        return (toDelete.size() > 0);
    }

    public ArrayList<ArrayList<GraphFragment>> verticesPerColor() {
        return verticesPerColor(peaks.size());
    }

    public ArrayList<ArrayList<GraphFragment>> verticesPerColor(int maxColor) {
        final int n = Math.min(maxColor+1, peaks.size());
        final ArrayList<ArrayList<GraphFragment>> verticesPerColor = new ArrayList<ArrayList<GraphFragment>>(n);
        for (int i=0; i < n; ++i) {
            verticesPerColor.add(new ArrayList<GraphFragment>());
        }
        for (GraphFragment f : getFragments()) {
            if (f.getColor() < n) {
                verticesPerColor.get(f.getColor()).add(f);
            }
        }
        return verticesPerColor;
    }

    public ArrayList<GraphFragment> verticesInPostOrder() {
        return verticesInPostOrder(peaks.size());
    }

    public ArrayList<GraphFragment> verticesInPostOrder(int maxColor) {
        final ArrayList<GraphFragment> list = new ArrayList<GraphFragment>(vertices.size());
        final ArrayDeque<StackNode> stack = new ArrayDeque<StackNode>();
        final BitSet visited = new BitSet(vertices.size());
        stack.push(new StackNode(getRoot(), 0));
        visited.set(getRoot().index, true);
        outerLoop:
        while (!stack.isEmpty()) {
            final StackNode node = stack.pop();
            if (!node.fragment.isLeaf()) {
                final List<Fragment> children = node.fragment.getChildren();
                for (int i=node.siblingIndex; i < children.size(); ++i) {
                    final GraphFragment fragment = (GraphFragment)children.get(i);
                    if (fragment.getColor() <= maxColor && !visited.get(fragment.index)) {
                        visited.set(fragment.index, true);
                        stack.push(new StackNode(node.fragment, i + 1));
                        stack.push(new StackNode(fragment, 0));
                        continue outerLoop;
                    }
                }
            }
            list.add(node.fragment);
        }
        assert maxColor == numberOfColors() ? list.size() <= vertices.size() : true;
        return list;
    }

    private static final class StackNode {
        private final int siblingIndex;
        private final GraphFragment fragment;

        private StackNode(GraphFragment fragment, int siblingIndex) {
            this.siblingIndex = siblingIndex;
            this.fragment = fragment;
        }
    }

    public void removeEdge(Loss l) {
        ((GraphFragment)l.getHead()).removeOutgoingEdge(l);
        ((GraphFragment)l.getTail()).removeIncommingEdge(l);
    }

    public double getRootScore() {
        return rootScore;
    }

    public void setRootScore(double rootScore) {
        this.rootScore = rootScore;
    }

    public int numberOfVertices() {
        return vertices.size();
    }

    @Override
    public int numberOfEdges() {
        int c = 0;
        for (Fragment f : getFragments()) {
            c += f.numberOfChildren();
        }
        return c;
    }

    public int numberOfColors() {
        return filteredPeaks != null ? filteredPeaks.size() : peaks.size();
    }

    public Loss getLossOf(int u, int v) {
        final Fragment uf = vertices.get(u);
        for (Loss l : uf.getOutgoingEdges()) {
            if (l.getTail().getIndex() == v) return l;
        }
        return null;
    }

    public double maximalScore() {
        final double[] maxScore = new double[Collections.max(peaks, new Comparator<ProcessedPeak>() {
            @Override
            public int compare(ProcessedPeak o1, ProcessedPeak o2) {
                return o1.getIndex() - o2.getIndex();
            }
        }).getIndex()+1];
        for (Loss l : Iterators.asIterable(lossIterator())) {
            final int c = l.getTail().getColor();
            maxScore[c] = Math.max(maxScore[c], l.getWeight());
        }
        double sum = rootScore;
        for (double s : maxScore) sum += s;
        return sum;
    }

    public GraphFragment getRoot() {
        return vertices.get(0);
    }

    @Override
    public Ionization getIonization() {
        return getProcessedInput().getExperimentInformation().getIonization();
    }

    public List<ProcessedPeak> getPeaks() {
        return Collections.unmodifiableList(peaks);
    }

    public GraphFragment getFragment(int index) {
        return vertices.get(index);
    }

    public List<GraphFragment> getFragments() {
        return Collections.unmodifiableList(vertices);
    }

    public List<GraphFragment> getFragmentsWithoutRoot() {
        return getFragments().subList(1, vertices.size());
    }

    public Iterator<Loss> lossIterator() {
        return Iterators.join(Iterators.map(vertices.iterator(), new Function<GraphFragment, Iterable<Loss>>() {
            @Override
            public Iterable<Loss> apply(GraphFragment arg) {
                return arg.getIncomingEdges();
            }
        }));
    }

    public GraphFragment addVertex(ProcessedPeak p, ScoredMolecularFormula decomposition) {
        final GraphFragment fragment = new GraphFragment(vertices.size(), decomposition, p);
        this.vertices.add(fragment);
        return fragment;
    }

    public Loss addEdge(GraphFragment a, GraphFragment b) {
        final Loss loss = new Loss(a, b);
        a.addOutgoingEdge(loss);
        b.addIncommingEdge(loss);
        return loss;
    }

}
