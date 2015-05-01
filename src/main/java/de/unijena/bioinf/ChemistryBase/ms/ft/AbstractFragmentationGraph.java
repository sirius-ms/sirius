package de.unijena.bioinf.ChemistryBase.ms.ft;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.*;

abstract class AbstractFragmentationGraph implements Iterable<Fragment> {

    protected final HashMap<Class<Object>, Object> annotations;
    protected final HashSet<Class<Object>> aliases;
    protected final ArrayList<Fragment> fragments;
    protected final HashMap<Class<Object>, FragmentAnnotation<Object>> fragmentAnnotations;
    protected final HashMap<Class<Object>, LossAnnotation<Object>> lossAnnotations;
    protected int edgeNum;

    public AbstractFragmentationGraph() {
        this.annotations = new HashMap<Class<Object>, Object>();
        this.fragments = new ArrayList<Fragment>();
        this.fragmentAnnotations = new HashMap<Class<Object>, FragmentAnnotation<Object>>();
        this.lossAnnotations = new HashMap<Class<Object>, LossAnnotation<Object>>();
        this.aliases = new HashSet<Class<Object>>();
        edgeNum = 0;
    }

    protected AbstractFragmentationGraph(AbstractFragmentationGraph graph) {
        this.annotations = new HashMap<Class<Object>, Object>(graph.annotations);
        this.aliases = new HashSet<Class<Object>>();
        this.fragmentAnnotations = new HashMap<Class<Object>, FragmentAnnotation<Object>>(graph.fragmentAnnotations);
        this.lossAnnotations = new HashMap<Class<Object>, LossAnnotation<Object>>(graph.lossAnnotations);
        this.edgeNum = graph.edgeNum;
        this.fragments = new ArrayList<Fragment>(graph.fragments.size());
        for (Fragment f : graph.fragments) fragments.add(new Fragment(f));
        for (Fragment f : fragments) {
            for (int k = 0; k < f.incomingEdges.length; ++k) {
                final Loss l = f.incomingEdges[k];
                if (l != null) {
                    final Loss newl = new Loss(l, fragments.get(l.source.vertexId), fragments.get(l.target.vertexId));
                    f.incomingEdges[k] = newl;
                }
            }
        }
        for (Fragment f : fragments) {
            for (int k = 0; k < f.outgoingEdges.length; ++k) {
                final Loss l = f.outgoingEdges[k];
                if (l != null) {
                    final Loss newl = fragments.get(l.target.vertexId).getIncomingEdge(l.targetEdgeOffset);
                    f.outgoingEdges[k] = newl;
                }
            }
        }
    }

    /**
     * maps all vertices from graph1 to graph2. Returns a map (fragment a -> fragment b) where a is a fragment of
     * graph1 and b is a corresponding fragment from graph 2. Two fragments belong to each other if they have the same
     * molecular formula.
     */
    public static BiMap<Fragment, Fragment> createFragmentMapping(AbstractFragmentationGraph graph1, AbstractFragmentationGraph graph2) {

        if (graph1.numberOfVertices() > graph2.numberOfVertices())
            return createFragmentMapping(graph2, graph1).inverse();
        final HashMap<MolecularFormula, Fragment> formulas = new HashMap<MolecularFormula, Fragment>(graph1.numberOfVertices());
        final BiMap<Fragment, Fragment> bimap = HashBiMap.create(Math.min(graph1.numberOfVertices(), graph2.numberOfVertices()));

        for (Fragment f : graph1.getFragments()) {
            formulas.put(f.getFormula(), f);
        }

        for (Fragment f : graph2.getFragmentsWithoutRoot()) {
            if (formulas.containsKey(f.getFormula())) {
                bimap.put(formulas.get(f.getFormula()), f);
            }
        }
        return bimap;
    }

    private static void deleteOutEdgeInternal(Fragment vertex, Loss l) {
        if (l.sourceEdgeOffset + 1 < vertex.outDegree) {
            vertex.outgoingEdges[l.sourceEdgeOffset] = vertex.outgoingEdges[vertex.outDegree - 1];
            vertex.outgoingEdges[--vertex.outDegree] = null;
            vertex.outgoingEdges[l.sourceEdgeOffset].sourceEdgeOffset = l.sourceEdgeOffset;
        } else {
            vertex.outgoingEdges[l.sourceEdgeOffset] = null;
            --vertex.outDegree;
        }
    }

    private static void deleteInEdgeInternal(Fragment vertex, Loss l) {
        if (l.targetEdgeOffset + 1 < vertex.inDegree) {
            vertex.incomingEdges[l.targetEdgeOffset] = vertex.incomingEdges[vertex.inDegree - 1];
            vertex.incomingEdges[--vertex.inDegree] = null;
            vertex.incomingEdges[l.targetEdgeOffset].targetEdgeOffset = l.targetEdgeOffset;
        } else {
            vertex.incomingEdges[l.targetEdgeOffset] = null;
            --vertex.inDegree;
        }
    }

    public HashMap<MolecularFormula, Fragment> fragmentsByFormula() {
        final HashMap<MolecularFormula, Fragment> map = new HashMap<MolecularFormula, Fragment>(fragments.size());
        for (Fragment f : getFragments()) {
            if (f.getFormula().getMass() > 0) map.put(f.getFormula(), f);
        }
        return map;
    }

    public Iterable<Fragment> inPostOrder(final Fragment startingRoot) {
        return new Iterable<Fragment>() {
            @Override
            public Iterator<Fragment> iterator() {
                return postOrderIterator(startingRoot);
            }
        };
    }

    public Iterable<Fragment> inPreOrder(final Fragment startingRoot) {
        return new Iterable<Fragment>() {
            @Override
            public Iterator<Fragment> iterator() {
                return preOrderIterator(startingRoot);
            }
        };
    }

    public final Iterable<Fragment> inPostOrder() {
        return inPostOrder(getRoot());
    }

    public final Iterable<Fragment> inPreOrder() {
        return inPreOrder(getRoot());
    }

    public Iterator<Fragment> postOrderIterator() {
        return postOrderIterator(getRoot());
    }

    public Iterator<Fragment> preOrderIterator() {
        return preOrderIterator(getRoot());
    }

    public abstract Iterator<Fragment> postOrderIterator(Fragment startingRoot);

    public abstract Iterator<Fragment> preOrderIterator(Fragment startingRoot);

    public Loss getLoss(Fragment f, MolecularFormula g) {
        for (int i = 0; i < f.outDegree; ++i) {
            if (f.getChildren(i).getFormula().equals(g)) {
                return f.getOutgoingEdge(i);
            }
        }
        return null;
    }

    public Loss getLoss(MolecularFormula a, MolecularFormula b) {
        for (Fragment f : fragments) {
            if (f.getFormula().equals(a)) {
                return getLoss(f, b);
            }
        }
        return null;
    }

    public abstract Fragment getRoot();

    public Fragment getFragmentAt(int k) {
        return fragments.get(k);
    }

    public List<FragmentAnnotation<Object>> getFragmentAnnotations() {
        return new ArrayList<FragmentAnnotation<Object>>(fragmentAnnotations.values());
    }

    public Map<Class<Object>, Object> getAnnotations() {
        return Collections.unmodifiableMap(annotations);
    }

    public List<LossAnnotation<Object>> getLossAnnotations() {
        return new ArrayList<LossAnnotation<Object>>(lossAnnotations.values());
    }


    @SuppressWarnings("unchecked cast")
    public <T> FragmentAnnotation<T> getFragmentAnnotationOrThrow(Class<T> klass) {
        final FragmentAnnotation<T> ano = (FragmentAnnotation<T>) fragmentAnnotations.get(klass);
        if (ano == null)
            throw new NullPointerException("No peak annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T> FragmentAnnotation<T> getFragmentAnnotationOrNull(Class<T> klass) {
        return (FragmentAnnotation<T>) fragmentAnnotations.get(klass);
    }

    @SuppressWarnings("unchecked cast")
    public <T> LossAnnotation<T> getLossAnnotationOrThrow(Class<T> klass) {
        final LossAnnotation<T> ano = (LossAnnotation<T>) lossAnnotations.get(klass);
        if (ano == null)
            throw new NullPointerException("No peak annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T> LossAnnotation<T> getLossAnnotationOrNull(Class<T> klass) {
        return (LossAnnotation<T>) lossAnnotations.get(klass);
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T ano = (T) annotations.get(klass);
        if (ano == null) throw new NullPointerException("No annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrNull(Class<T> klass) {
        return (T) annotations.get(klass);
    }

    public boolean removeAnnotation(Class<?> klass) {
        return annotations.remove(klass) != null;
    }

    public <T> FragmentAnnotation<T> addFragmentAnnotation(Class<T> klass) {
        if (fragmentAnnotations.containsKey(klass))
            throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        final int n = fragmentAnnotations.size();
        final FragmentAnnotation<T> ano = new FragmentAnnotation<T>(n, n + 1, klass);
        for (FragmentAnnotation<Object> a : fragmentAnnotations.values()) a.capa = n + 1;
        fragmentAnnotations.put((Class<Object>) klass, (FragmentAnnotation<Object>) ano);
        return ano;
    }

    public <T> LossAnnotation<T> addLossAnnotation(Class<T> klass) {
        if (lossAnnotations.containsKey(klass))
            throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        final int n = lossAnnotations.size();
        final LossAnnotation<T> ano = new LossAnnotation<T>(n, n + 1, klass);
        for (LossAnnotation<Object> a : lossAnnotations.values()) a.capa = n + 1;
        lossAnnotations.put((Class<Object>) klass, (LossAnnotation<Object>) ano);
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <S, T extends S> FragmentAnnotation<S> addAliasForFragmentAnnotation(Class<T> previous, Class<S> newOne) {
        final FragmentAnnotation<T> ano = getFragmentAnnotationOrThrow(previous);
        final FragmentAnnotation<S> newAno = new FragmentAnnotation<S>(ano, newOne);
        fragmentAnnotations.put((Class<Object>) newOne, (FragmentAnnotation<Object>) newAno);
        return newAno;
    }

    @SuppressWarnings("unchecked cast")
    public <S, T extends S> LossAnnotation<S> addAliasForLossAnnotation(Class<T> previous, Class<S> newOne) {
        final LossAnnotation<T> ano = getLossAnnotationOrThrow(previous);
        final LossAnnotation<S> newAno = new LossAnnotation<S>(ano, newOne);
        lossAnnotations.put((Class<Object>) newOne, (LossAnnotation<Object>) newAno);
        return newAno;
    }

    public <T> void addAnnotation(Class<T> klass, T annotation) {
        if (annotations.containsKey(klass))
            throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        annotations.put((Class<Object>) klass, (Object) annotation);
    }

    public <S, T extends S> void addAliasForAnnotation(Class<T> previous, Class<S> newOne) {
        if (!annotations.containsKey(previous)) throw new RuntimeException("Cannot find annotation of class " + previous.getName());
        annotations.put((Class<Object>) newOne, annotations.get(previous));
        aliases.add((Class<Object>) newOne);
    }

    public boolean isAliasAnnotation(Class<?> klass) {
        return aliases.contains(klass);
    }

    public <T> boolean setAnnotation(Class<T> klass, T annotation) {
        if (aliases.contains(klass)) throw new IllegalArgumentException("Cannot change value for alias annotation");
        return annotations.put((Class<Object>) klass, annotation) == annotation;
    }

    @SuppressWarnings("unchecked cast")
    public <T> FragmentAnnotation<T> getOrCreateFragmentAnnotation(Class<T> klass) {
        if (fragmentAnnotations.containsKey(klass)) return (FragmentAnnotation<T>) fragmentAnnotations.get(klass);
        return addFragmentAnnotation(klass);
    }

    @SuppressWarnings("unchecked cast")
    public <T> LossAnnotation<T> getOrCreateLossAnnotation(Class<T> klass) {
        if (lossAnnotations.containsKey(klass)) return (LossAnnotation<T>) lossAnnotations.get(klass);
        return addLossAnnotation(klass);
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getOrCreateAnnotation(Class<T> klass) {
        if (annotations.containsKey(klass)) return (T) annotations.get(klass);
        try {
            final T obj = klass.newInstance();
            annotations.put((Class<Object>) klass, (Object) obj);
            return obj;
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected Fragment addFragment(MolecularFormula formula) {
        final Fragment f = new Fragment(fragments.size(), formula);
        fragments.add(f);
        return f;
    }

    protected Loss addLoss(Fragment u, Fragment v) {
        return addLoss(u, v, u.formula.subtract(v.formula));
    }

    protected Loss addLoss(Fragment u, Fragment v, MolecularFormula f) {
        final Loss l = getLoss(u, v);
        if (l != null) return l;
        final Loss loss = new Loss(u, v, f, 0d);
        if (u.outgoingEdges.length <= u.outDegree) {
            u.outgoingEdges = Arrays.copyOf(u.outgoingEdges, u.outDegree + 1);
        }
        u.outgoingEdges[u.outDegree] = loss;
        loss.sourceEdgeOffset = u.outDegree++;
        if (v.incomingEdges.length <= v.inDegree) {
            v.incomingEdges = Arrays.copyOf(v.incomingEdges, v.inDegree + 1);
        }
        v.incomingEdges[v.inDegree] = loss;
        loss.targetEdgeOffset = v.inDegree++;
        ++edgeNum;
        return loss;
    }

    protected void deleteLoss(Loss l) {
        deleteInEdgeInternal(l.target, l);
        deleteOutEdgeInternal(l.source, l);
        l.sourceEdgeOffset = l.targetEdgeOffset = -1;
        --edgeNum;
    }

    protected void deleteFragmentsKeepTopologicalOrder(Iterable<Fragment> todelete) {
        for (Fragment fragment : todelete) {
            if (fragments.get(fragment.vertexId) != fragment)
                throw new NoSuchElementException("The given fragment is not part of this graph");
            final int in = fragment.getInDegree(), out = fragment.getOutDegree();
            edgeNum -= in;
            edgeNum -= out;
            for (int i=0; i < in; ++i) {
                final Loss l = fragment.getIncomingEdge(i);
                deleteOutEdgeInternal(l.source, l);
            }
            for (int i=0; i < out; ++i) {
                final Loss l = fragment.getOutgoingEdge(i);
                deleteInEdgeInternal(l.target, l);
            }
            fragments.set(fragment.vertexId, null);
            fragment.vertexId = -1;
        }
        int i=0;
        for (int k=0; k < fragments.size(); ++k) {
            if (fragments.get(k)==null) {

            } else {
                if (k > i) {
                    fragments.set(i, fragments.get(k));
                    fragments.set(k, null);
                    fragments.get(i).vertexId = i;
                }
                ++i;
            }
        }
        for (int k=fragments.size()-1; k >= 0; --k)
            if (fragments.get(k)==null) fragments.remove(k);
    }

    protected void deleteFragment(Fragment fragment) {
        if (fragments.get(fragment.vertexId) != fragment)
            throw new NoSuchElementException("The given fragment is not part of this graph");
        // delete fragment by swapping it with last element and then delete last element
        final Fragment SWAP = fragments.get(fragments.size() - 1);
        fragments.set(fragment.vertexId, SWAP);
        fragments.remove(fragments.size() - 1);
        SWAP.setVertexId(fragment.vertexId);
        // now delete all edges of the deleted fragment
        final int in = fragment.getInDegree(), out = fragment.getOutDegree();
        edgeNum -= in;
        edgeNum -= out;
        for (int i=0; i < in; ++i) {
            final Loss l = fragment.getIncomingEdge(i);
            deleteOutEdgeInternal(l.source, l);
        }
        for (int i=0; i < out; ++i) {
            final Loss l = fragment.getOutgoingEdge(i);
            deleteInEdgeInternal(l.target, l);
        }
        fragment.vertexId = -1;
    }

    public List<Fragment> getFragments() {
        return Collections.unmodifiableList(fragments);
    }

    public List<Fragment> getFragmentsWithoutRoot() {
        return new AbstractList<Fragment>() {
            @Override
            public Fragment get(int index) {
                return fragments.get(index + 1);
            }

            @Override
            public int size() {
                return fragments.size() - 1;
            }
        };
    }

    public abstract Iterator<Loss> lossIterator();

    public abstract List<Loss> losses();

    public int numberOfVertices() {
        return fragments.size();
    }

    public abstract int numberOfEdges();

    public boolean isConnected(Fragment u, Fragment v) {
        return getLoss(u, v) != null;
    }

    public abstract Loss getLoss(Fragment u, Fragment v);


}
