package de.unijena.bioinf.FragmentationTreeConstruction.model.graph;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.functional.Function;
import de.unijena.bioinf.functional.iterator.Iterators;

import java.util.*;

public class FTGraph implements Iterable<FTVertex>, FragmentationGraph {

    protected ArrayList<FTVertex> vertices;
    protected FTVertex pseudoRoot;
    protected ProcessedInput processedInput;
    protected int numberOfEdges;
    protected HashMap<Class, VertexAnnotation> fragmentAnnotations;

    public FTGraph(ProcessedInput processedInput) {
        this.processedInput = processedInput;
        this.fragmentAnnotations = new HashMap<Class, VertexAnnotation>();
        this.vertices = new ArrayList<FTVertex>(processedInput.getMergedPeaks().size()+1);
        pseudoRoot = new FTVertex(null,null,0);
        vertices.add(pseudoRoot);
    }

    public ProcessedInput getProcessedInput() {
        return processedInput;
    }

    @Override
    public FTVertex getRoot() {
        return pseudoRoot;
    }

    public int numberOfColors() {
        return processedInput.getMergedPeaks().size();
    }

    public int numberOfVertices() {
        return vertices.size();
    }

    public int numberOfEdges() {
        return numberOfEdges;
    }

    public FTVertex getVertex(int index) {
        return vertices.get(index);
    }

    /**
     * Creates a new graph containing only vertices reachable from vertex
     * TODO: check and improve performance
     * @param vertex
     * @return
     */
    public FTGraph getSubgraphBelow(FTVertex vertex) {
        final GraphTraversal.InOrderIterator iter = new GraphTraversal.InOrderIterator(vertex);
        final MolecularFormula f = vertex.getMolecularFormula();
        final FTVertex[] newVertices = new FTVertex[this.vertices.size()];
        final FTGraph newGraph = new FTGraph(processedInput);
        while (iter.hasNext()) {
            final FTVertex v = iter.next();
            newVertices[v.index] = newGraph.addVertex(v);
        }
        for (int i=0; i < newVertices.length; ++i) {
            final FTVertex v = newVertices[i];
            if (v == null) continue;
            for (FTEdge uv : vertices.get(i).getIncomingEdges()) {
                if (newVertices[uv.from.index] != null) {
                    newGraph.connect(newVertices[uv.from.index], newVertices[v.index]);
                }
            }
        }
        return newGraph;
    }

    /**
     * iterates the vertices in an arbitrary order specified by their index. This is the fastest way to iterate vertices
     */
    public Iterator<FTVertex> iterateByIndizes() {
        return Collections.unmodifiableList(vertices).iterator();
    }

    /**
     * iterates the vertices in an order u1 < u2 < ... < un such that for all ui < uj, ui is NEVER an ancestor of uj
     */
    public Iterator<FTVertex> iterateInPostOrder() {
        return new GraphTraversal.PostOrderIterator(this);
    }

    /**
     * iterates the vertices in an order u1 < u2 < ... < un such that for all ui < uj, ui is NEVER an ancestor of uj
     * and such that for all ui the getColor of ui is in the given restrictedColors set
     * @param restrictedColors bit set for which restrictedColors(i) is set if the getColor i is allowed
     * @return
     */
    public Iterator<FTVertex> iterateInPostOrder(BitSet restrictedColors) {
        return new GraphTraversal.PostOrderColorRestrictedIterator(this, restrictedColors);
    }

    /**
     * iterate over all edges in the graph in an arbitrary order
     */
    public Iterator<FTEdge> iterateEdges() {
        return Iterators.join(Iterators.map(vertices.iterator(), new Function<FTVertex, Iterable<FTEdge>>() {
            @Override
            public Iterable<FTEdge> apply(FTVertex arg) {
                return arg.getIncomingEdges();
            }
        }));
    }


    /**
     * add a new vertex into the graph
     * @param peakNumber the index of the corresponding peak in the merged peak list
     * @param formula molecular formula of the vertex
     */
    public FTVertex addVertex(int peakNumber, MolecularFormula formula) {
        final FTVertex vertex = new FTVertex(processedInput.getMergedPeaks().get(peakNumber), formula, vertices.size());
        vertices.add(vertex);
        return vertex;
    }

    protected FTVertex addVertex(FTVertex copy) {
        final FTVertex vertex = new FTVertex(copy, vertices.size());
        vertices.add(vertex);
        return vertex;
    }

    public void removeVertex(FTVertex vertex) {
        if (vertex==pseudoRoot) throw new IllegalArgumentException("Cannot remove pseudoroot from graph");
        if (vertices.isEmpty()) vertices.clear();
        else {
            final FTVertex last = vertices.get(vertices.size()-1);
            vertices.set(vertex.index, last);
            vertices.set(last.index, null);
            vertices.remove(vertices.size()-1);
            last.index = vertex.index;
            disconnectVertex(vertex);
        }
    }

    public FTEdge connectToRoot(FTVertex vertex) {
        return connect(pseudoRoot, vertex);
    }

    public FTEdge connect(FTVertex a, FTVertex b) {
        final FTEdge edge = new FTEdge(a, b);
        addEdgeIn(a.outgoingEdges, edge);
        addEdgeIn(b.incomingEdges, edge);
        ++numberOfEdges;
        return edge;
    }

    public void disconnect(FTEdge edge) {
        removeEdgeFromArray(edge, edge.from.outgoingEdges);
        removeEdgeFromArray(edge, edge.to.incomingEdges);
        --numberOfEdges;
    }

    public boolean disconnect(FTVertex a, FTVertex b) {
        final FTEdge e = getEdge(a, b);
        if (e != null) disconnect(e);
        return e != null;
    }

    public FTEdge getEdge(FTVertex from, FTVertex to) {
        if (from.outgoingEdges.length < to.incomingEdges.length) {
            for (FTEdge e : from.outgoingEdges) if (e.to==to) return e;
        } else {
            for (FTEdge e : to.incomingEdges) if (e.from==from) return e;
        }
        return null;
    }

    protected void disconnectVertex(FTVertex vertex) {
        for (FTEdge in : vertex.incomingEdges) {
            if (in != null) {
                removeEdgeFromArray(in, in.from.outgoingEdges);
                --numberOfEdges;
            }
        }
        for (FTEdge in : vertex.outgoingEdges) {
            if (in != null) {
                removeEdgeFromArray(in, in.to.incomingEdges);
                --numberOfEdges;
            }
        }
    }

    private static void removeEdgeFromArray(FTEdge edge, FTEdge[] edges) {
        for (int k=0; k < edges.length; ++k) if (edges[k]==edge) {
            final int n = edges.length-1;
            if (k < n) edges[k]=edges[n];
            edges[n] = null;
            return;
        }
        throw new NoSuchElementException("There is no edge " + edge + " in the edge list");
    }

    private void addEdgeIn(FTEdge[] edges, FTEdge edge) {
        int k=edges.length-1;
        while (k >= 0 && edges[k]==null) --k;
        if (k + 1 >= edges.length) {
            edges = Arrays.copyOf(edges, edges.length+1);
        }
        edges[k+1] = edge;
    }


    @Override
    public Iterator<FTVertex> iterator() {
        return iterateByIndizes();
    }

    @SuppressWarnings("unchecked cast")
    public <T> VertexAnnotation<T> getVertexAnnotationOrThrow(Class<T> klass) {
        final VertexAnnotation<T> ano = fragmentAnnotations.get(klass);
        if (ano == null) throw new NullPointerException("No peak annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }
    public <T> VertexAnnotation<T> addVertexAnnotation(Class<T> klass) {
        if (fragmentAnnotations.containsKey(klass)) throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        final VertexAnnotation<T> ano = new VertexAnnotation<T>(fragmentAnnotations.size(), klass);
        fragmentAnnotations.put(klass, ano);
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T> VertexAnnotation<T> getOrCreateVertexAnnotation(Class<T> klass) {
        if (fragmentAnnotations.containsKey(klass)) return fragmentAnnotations.get(klass);
        final VertexAnnotation<T> ano = new VertexAnnotation<T>(fragmentAnnotations.size(), klass);
        fragmentAnnotations.put(klass, ano);
        return ano;
    }

}
