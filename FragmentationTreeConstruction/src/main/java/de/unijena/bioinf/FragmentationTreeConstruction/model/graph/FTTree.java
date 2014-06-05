package de.unijena.bioinf.FragmentationTreeConstruction.model.graph;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.HashMap;

/*
TODO: Maybe not such a good idea to let FTTree be a subclass of FTGraph... -> Create abstract superclass for both
 */
public class FTTree  extends FTGraph {

    protected HashMap<Class, Object> annotations;
    protected double weight;

    public FTTree(ProcessedInput processedInput) {
        super(processedInput);
        this.annotations = new HashMap<Class, Object>();
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public FTEdge connect(FTVertex a, FTVertex b) {
        if (b.numberOfIncomingEdges()>0) throw new IllegalArgumentException("Tree vertices can have maximal one incoming edge");
        return super.connect(a,b);
    }

    public FTVertex addVertex(FTGraph graph, int vertexId) {
        if (graph == this) throw new IllegalArgumentException("Cannot add two identical vertices into the same graph");
        return super.addVertex(graph.getVertex(vertexId));
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

    @SuppressWarnings("unchecked cast")
    public <T> T getOrCreateAnnotation(Class<T> klass) {
        if (annotations.containsKey(klass)) return (T)annotations.get(klass);
        try {
            final T obj = klass.newInstance();
            annotations.put(klass, obj);
            return obj;
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    public <T> void addAnnotation(Class<T> klass, T annotation) {
        if (annotations.containsKey(klass)) throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        annotations.put(klass, annotation);
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T ano = (T)annotations.get(klass);
        if (ano == null) throw new NullPointerException("No annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }



}
