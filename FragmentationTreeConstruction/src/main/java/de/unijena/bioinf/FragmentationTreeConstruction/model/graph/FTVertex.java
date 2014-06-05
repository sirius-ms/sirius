package de.unijena.bioinf.FragmentationTreeConstruction.model.graph;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.FragmentationTreeConstruction.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FTVertex implements Fragment {

    private final static FTEdge[] NO_EDGES = new FTEdge[0];

    /**
     * all other attributes that may depend on some scorers or other utility plugins
     */
    protected Object[] annotations;

    /**
     * each vertex is labeled with a molecular formula
     */
    protected MolecularFormula formula;

    /**
     * each vertex has outgoing edges
     * as the graph is changed very rarely, they are implemented as array to reduce memory consumption
     */
    protected FTEdge[] outgoingEdges;

    /**
     * each vertex has incoming edges
     * as the graph is changed very rarely, they are implemented as array to reduce memory consumption
     */
    protected FTEdge[] incomingEdges;

    /**
     * each vertex maps to a certain processed peak. This peak denotes also the getColor of the vertex
     * multiple vertices may share the same peak
     */
    protected ProcessedPeak peak;

    /**
     * the index of the vertex in the graph. May change when removing vertices, so don't rely on this value. It's mainly
     * for fast random access to vertices
     */
    protected int index;

    FTVertex(ProcessedPeak peak, MolecularFormula formula, int index) {
        this.annotations = Utils.EMPTY_ARRAY;
        this.formula = formula;
        this.outgoingEdges = NO_EDGES;
        this.incomingEdges = NO_EDGES;
        this.peak = peak;
        this.index = index;
    }

    FTVertex(FTVertex previous, int index) {
        this(previous.peak, previous.formula, index);
        this.annotations = previous.annotations.clone();
    }

    public List<FTEdge> getOutgoingEdges() {
        return new EdgeList(outgoingEdges);
    }

    @Override
    public List<FTVertex> getChildren() {
        final ArrayList<FTVertex> children = new ArrayList<FTVertex>(outgoingEdges.length);
        for (int k=0; k < outgoingEdges.length && outgoingEdges[k]!=null; ++k)
            children.add(outgoingEdges[k].to);
        return children;
    }

    public List<FTEdge> getIncomingEdges() {
        return new EdgeList(incomingEdges);
    }

    public FTEdge getOutgoingEdge(int k) {
        return outgoingEdges[k];
    }

    public FTEdge getIncomingEdge(int k) {
        return incomingEdges[k];
    }

    public int numberOfOutgoingEdges() {
        int n=outgoingEdges.length;
        for (; n > 0; --n) if (outgoingEdges[n-1]!=null) break;
        return n;
    }

    public int numberOfIncomingEdges() {
        int n=incomingEdges.length;
        for (; n > 0; --n) if (incomingEdges[n-1]!=null) break;
        return n;
    }

    public boolean isLeaf() {
        return outgoingEdges.length==0 || outgoingEdges[0]==null;
    }

    public MolecularFormula getMolecularFormula() {
        return formula;
    }

    public boolean isPseudoRoot() {
        return formula==null;
    }

    public int getColor() {
        return peak.getIndex();
    }

    public int getIndex() {
        return index;
    }

    public ProcessedPeak getPeak() {
        return peak;
    }

    @Override
    public CollisionEnergy getCollisionEnergy() {
        return peak.getCollisionEnergy();
    }

    public Object getAnnotation(int id) {
        if (id >= annotations.length) return null;
        return annotations[id];
    }
    public void setAnnotation(int id, Object annotation) {
        if (id >= annotations.length) annotations = Arrays.copyOf(annotations, id+1);
        annotations[id] = annotation;
    }
}
