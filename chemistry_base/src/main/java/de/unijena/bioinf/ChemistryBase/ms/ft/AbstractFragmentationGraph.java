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
package de.unijena.bioinf.ChemistryBase.ms.ft;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TCustomHashMap;

import java.util.*;

abstract class AbstractFragmentationGraph implements Iterable<Fragment>, Annotated<DataAnnotation> {

    protected final Annotated.Annotations<DataAnnotation> annotations;//HashMap<Class<Object>, Object> annotations;
    protected final HashSet<Class<Object>> aliases;
    protected final ArrayList<Fragment> fragments;
    protected final HashMap<Class<Object>, FragmentAnnotation<Object>> fragmentAnnotations;
    protected final HashMap<Class<Object>, LossAnnotation<Object>> lossAnnotations;
    protected int edgeNum;

    public Annotated.Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public AbstractFragmentationGraph() {
        this.annotations = new Annotations<>();
        this.fragments = new ArrayList<Fragment>();
        this.fragmentAnnotations = new HashMap<Class<Object>, FragmentAnnotation<Object>>();
        this.lossAnnotations = new HashMap<Class<Object>, LossAnnotation<Object>>();
        this.aliases = new HashSet<Class<Object>>();
        edgeNum = 0;
    }

    protected AbstractFragmentationGraph(AbstractFragmentationGraph graph) {
        this.annotations = graph.annotations.clone();
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
     * maps all vertices from graph1 to graph2. Returns a map (fragment a {@literal ->} fragment b) where a is a fragment of
     * graph1 and b is a corresponding fragment from graph 2. Two fragments belong to each other if they have the same
     * molecular formula.
     */
    public static BiMap<Fragment, Fragment> createFragmentMapping(AbstractFragmentationGraph graph1, AbstractFragmentationGraph graph2) {
        if (graph1.numberOfVertices() > graph2.numberOfVertices())
            return createFragmentMapping(graph2, graph1).inverse();
//        final HashMap<MolecularFormula, Fragment> formulas = new HashMap<MolecularFormula, Fragment>(graph1.numberOfVertices());
        final TCustomHashMap<Fragment, Fragment> fragmentsGraph1Map = Fragment.newFragmentWithIonMap();
        final BiMap<Fragment, Fragment> bimap = HashBiMap.create(Math.min(graph1.numberOfVertices(), graph2.numberOfVertices()));

        for (Fragment f : graph1.getFragments()) {
            fragmentsGraph1Map.put(f, f);
        }

        for (Fragment f : graph2.getFragmentsWithoutRoot()) {
            if (fragmentsGraph1Map.containsKey(f)) {
                bimap.put(fragmentsGraph1Map.get(f), f);
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

    protected final boolean isOwnFragment(Fragment f) {
        return fragments.get(f.vertexId)==f;
    }

    public List<FragmentAnnotation<Object>> getFragmentAnnotations() {
        return new ArrayList<FragmentAnnotation<Object>>(fragmentAnnotations.values());
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

    public <T extends DataAnnotation> T getAnnotationOrNull(Class<T> klass) {
        return hasAnnotation(klass) ? getAnnotation(klass) : null;
    }

    public boolean removeAliasForFragmentAnnotation(Class<?> klass) {
        FragmentAnnotation<Object> f = fragmentAnnotations.get(klass);
        if (f.isAlias()) {
            fragmentAnnotations.remove(klass);
            return true;
        } else return false;
    }

    public boolean removeAliasForLossAnnotation(Class<?> klass) {
        LossAnnotation<Object> f = lossAnnotations.get(klass);
        if (f.isAlias()) {
            lossAnnotations.remove(klass);
            return true;
        } else return false;
    }

    /*
    public void copyAnnotations(AbstractFragmentationGraph otherGraph) {
        for (FragmentAnnotation<Object> entry : fragmentAnnotations.values()) {
           if (!entry.isAlias()) {
               otherGraph.addFragmentAnnotation(entry.getAnnotationType());
           }
        }
        for (FragmentAnnotation<Object> entry : fragmentAnnotations.values()) {
            if (entry.isAlias()) {
                otherGraph.addAliasForFragmentAnnotation(entry.getAliasType(), entry.getAnnotationType());
            }
        }
        for (LossAnnotation<Object> entry : lossAnnotations.values()) {
            if (!entry.isAlias()) {
                otherGraph.addLossAnnotation(entry.getAnnotationType());
            }
        }
        for (LossAnnotation<Object> entry : lossAnnotations.values()) {
            if (entry.isAlias()) {
                otherGraph.addAliasForLossAnnotation(entry.getAliasType(), entry.getAnnotationType());
            }
        }
        for (Map.Entry<Class<Object>,Object> entry : annotations.entrySet()) {
            otherGraph.setAnnotation(entry.getKey(), entry.getValue());
        }
        otherGraph.aliases.addAll(aliases);
    }
    */

    public boolean removeFragmentAnnotation(Class<? extends Object> klass) {
        final FragmentAnnotation<Object> ano = fragmentAnnotations.get(klass);
        if (ano==null) return false;
        if (ano.isAlias()) {
            fragmentAnnotations.remove(klass);
            return true;
        }
        fragmentAnnotations.remove(klass);
        for (Fragment f : fragments) {
            ano.set(f, null);
        }
        return true;
    }
    public boolean removeLossAnnotation(Class<? extends Object> klass) {
        final LossAnnotation<Object> ano = lossAnnotations.get(klass);
        if (ano==null) return false;
        if (ano.isAlias()) {
            lossAnnotations.remove(klass);
            return true;
        }
        lossAnnotations.remove(klass);
        for (Loss f : losses()) {
            ano.set(f, null);
        }
        return true;
    }

    public <T> FragmentAnnotation<T> addFragmentAnnotation(Class<T> klass) {
        if (fragmentAnnotations.containsKey(klass))
            throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        final BitSet ids = new BitSet();
        for (FragmentAnnotation<Object> a : fragmentAnnotations.values())
            if (!a.isAlias())
                ids.set(a.id);
        final int id = ids.nextClearBit(0);
        final int length = ids.length();
        final FragmentAnnotation<T> ano = new FragmentAnnotation<T>(id, length, klass);
        for (FragmentAnnotation<Object> a : fragmentAnnotations.values()) a.capa = length;
        fragmentAnnotations.put((Class<Object>) klass, (FragmentAnnotation<Object>) ano);
        return ano;
    }
    public <T> LossAnnotation<T> addLossAnnotation(Class<T> klass) {
        if (lossAnnotations.containsKey(klass))
            throw new RuntimeException("Loss annotation '" + klass.getName() + "' is already present.");
        final BitSet ids = new BitSet();
        for (LossAnnotation<Object> a : lossAnnotations.values())
            if (!a.isAlias())
                ids.set(a.id);
        final int id = ids.nextClearBit(0);
        final int length = ids.length();
        final LossAnnotation<T> ano = new LossAnnotation<T>(id, length, klass);
        for (LossAnnotation<Object> a : lossAnnotations.values()) a.capa = length;
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

    @Deprecated
    public <T extends DataAnnotation> T getOrCreateAnnotation(Class<T> klass) {
        return getAnnotation(klass, ()-> {
            try {
                return klass.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

    }

    protected Fragment addFragment(MolecularFormula formula, Ionization ionization) {
        final Fragment f = new Fragment(fragments.size(), formula, ionization);
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

    protected void deleteFragmentsKeepTopologicalOrder(Iterable<Fragment> todelete, TIntArrayList idsFrom, TIntArrayList idsTo) {
        for (Fragment fragment : todelete) {
            if (fragments.get(fragment.vertexId) != fragment)
                throw new NoSuchElementException("The given fragment is not part of this graph");
            final int in = fragment.getInDegree(), out = fragment.getOutDegree();
            edgeNum -= in;
            edgeNum -= out;
            for (int i=0; i < in; ++i) {
                final Loss l = fragment.getIncomingEdge(i);
                deleteOutEdgeInternalKeepTopologicalOrder(l.source, l);
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
                    if (idsFrom!=null) {
                        idsFrom.add(k);
                        idsTo.add(i);
                    }
                    fragments.get(i).vertexId = i;
                }
                ++i;
            }
        }
        for (int k=fragments.size()-1; k >= 0; --k)
            if (fragments.get(k)==null) fragments.remove(k);
    }

    private void deleteOutEdgeInternalKeepTopologicalOrder(Fragment source, Loss l) {
        if (l.sourceEdgeOffset+1 == source.outDegree) {
            source.outgoingEdges[--source.outDegree] = null;
        } else {

            final int moveFrom = l.sourceEdgeOffset+1;
            for (int i=moveFrom; i < source.outDegree; ++i) {
                final int newIndex = i-1;
                source.outgoingEdges[newIndex] = source.outgoingEdges[i];
                source.outgoingEdges[newIndex].sourceEdgeOffset = newIndex;
            }
            source.outgoingEdges[--source.outDegree]=null;
        }
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

    public void sortFragments()
    {
        Collections.sort(fragments);
        int id = 0;
        for (Fragment f : fragments) {
            f.setVertexId(id++);
        }
    }


}
