package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public abstract class Fragment {

    protected final List<Loss> outgoingEdges;
    int index;
    private final ProcessedPeak peak;
    private int color;
    protected final ScoredMolecularFormula decomposition;

    Fragment(int index, ScoredMolecularFormula decomposition, ProcessedPeak peak) {
        this.decomposition = decomposition;
        this.outgoingEdges = new ArrayList<Loss>();
        this.index = index;
        this.peak = peak;
        this.color = peak.getIndex();
    }

    void setColor(int color) {
        this.color = color;
    }

    public Loss getEdgeTo(Fragment other) {
        for (Loss l : outgoingEdges) if (l.getTail().equals(other)) return l;
        return null;
    }

    public Loss getEdgeTo(int id) {
        for (Loss l : outgoingEdges) if (l.getTail().getIndex() == id) return l;
        return null;
    }

    public boolean isLeaf() {
        return outgoingEdges.isEmpty();
    }

    public int numberOfChildren() {
        return outgoingEdges.size();
    }

    public int getIndex() {
        return index;
    }

    void addOutgoingEdge(Loss outgoing) {
        outgoingEdges.add(outgoing);
    }

    public int getColor() {
        return color;
    }

    public ProcessedPeak getPeak() {
        return peak;
    }

    public abstract List<Loss> getIncomingEdges();

    public List<Loss> getOutgoingEdges() {
        return Collections.unmodifiableList(outgoingEdges);
    }

    public ScoredMolecularFormula getDecomposition() {
        return decomposition;
    }

    public abstract List<Fragment> getParents();

    public List<Fragment> getChildren() {
        final Fragment[] children = new Fragment[outgoingEdges.size()];
        for (int i=0; i < outgoingEdges.size(); ++i) children[i] = outgoingEdges.get(i).getTail();
        return Arrays.asList(children); // TODO: is this function is used for temp lists, e.g. v.getChildren().size() ?
    }

    public String toString() {
        return "<" + getColor() + ":" + decomposition.toString() + ">";
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Fragment)) return false;
        final Fragment f = (Fragment)obj;
        return (f.getIndex() == index && f.getColor() == getColor());
    }
}
