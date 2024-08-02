
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;
import java.util.function.Supplier;

public abstract class AbstractFragmentationGraph implements Iterable<Fragment>, Annotated<DataAnnotation> {

    protected final Annotated.Annotations<DataAnnotation> annotations;
    protected final ArrayList<Fragment> fragments;
    protected final HashMap<Class<DataAnnotation>, FragmentAnnotation<DataAnnotation>> fragmentAnnotations;
    protected final HashMap<Class<DataAnnotation>, LossAnnotation<DataAnnotation>> lossAnnotations;
    protected int edgeNum;

    public Annotated.Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public AbstractFragmentationGraph() {
        this.annotations = new Annotations<>();
        this.fragments = new ArrayList<Fragment>();
        this.fragmentAnnotations = new HashMap<>();
        this.lossAnnotations = new HashMap<>();
        edgeNum = 0;
    }

    protected AbstractFragmentationGraph(AbstractFragmentationGraph graph) {
        this.annotations = graph.annotations.clone();
        this.fragmentAnnotations = new HashMap<>(graph.fragmentAnnotations);
        this.lossAnnotations = new HashMap<>(graph.lossAnnotations);
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

    public List<FragmentAnnotation<DataAnnotation>> getFragmentAnnotations() {
        return new ArrayList<FragmentAnnotation<DataAnnotation>>(fragmentAnnotations.values());
    }

    public List<LossAnnotation<DataAnnotation>> getLossAnnotations() {
        return new ArrayList<LossAnnotation<DataAnnotation>>(lossAnnotations.values());
    }


    @SuppressWarnings("unchecked cast")
    public <T extends DataAnnotation> FragmentAnnotation<T> getFragmentAnnotationOrThrow(Class<T> klass) {
        final FragmentAnnotation<T> ano = (FragmentAnnotation<T>) fragmentAnnotations.get(klass);
        if (ano == null)
            throw new NullPointerException("No peak annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T extends DataAnnotation> FragmentAnnotation<T> getFragmentAnnotationOrNull(Class<T> klass) {
        return (FragmentAnnotation<T>) fragmentAnnotations.get(klass);
    }

    @SuppressWarnings("unchecked cast")
    public <T extends DataAnnotation> LossAnnotation<T> getLossAnnotationOrThrow(Class<T> klass) {
        final LossAnnotation<T> ano = (LossAnnotation<T>) lossAnnotations.get(klass);
        if (ano == null)
            throw new NullPointerException("No peak annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T extends DataAnnotation> LossAnnotation<T> getLossAnnotationOrNull(Class<T> klass) {
        return (LossAnnotation<T>) lossAnnotations.get(klass);
    }


    public boolean removeFragmentAnnotation(Class<? extends DataAnnotation> klass) {
        final FragmentAnnotation<DataAnnotation> ano = fragmentAnnotations.get(klass);
        if (ano==null) return false;
        fragmentAnnotations.remove(klass);
        for (Fragment f : fragments) {
            ano.set(f, null);
        }
        return true;
    }
    public boolean removeLossAnnotation(Class<? extends DataAnnotation> klass) {
        final LossAnnotation<DataAnnotation> ano = lossAnnotations.get(klass);
        if (ano==null) return false;
        lossAnnotations.remove(klass);
        for (Loss f : losses()) {
            ano.set(f, null);
        }
        return true;
    }

    @Deprecated
    public <T extends DataAnnotation> FragmentAnnotation<T> addFragmentAnnotation(Class<T> klass) {
        return addFragmentAnnotation(klass, ()-> {
            try {
                return klass.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Deprecated
    public <T extends DataAnnotation> LossAnnotation<T> addLossAnnotation(Class<T> klass) {
        return addLossAnnotation(klass, ()-> {
            try {
                return klass.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T extends DataAnnotation> FragmentAnnotation<T> addFragmentAnnotation(Class<T> klass, Supplier<T> constructor) {
        if (fragmentAnnotations.containsKey(klass))
            throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        final BitSet ids = new BitSet();
        for (FragmentAnnotation<DataAnnotation> a : fragmentAnnotations.values())
                ids.set(a.id);
        final int id = ids.nextClearBit(0);
        final int length = ids.length();
        final FragmentAnnotation<T> ano = new FragmentAnnotation<T>(id, length, klass, constructor);
        for (FragmentAnnotation<DataAnnotation> a : fragmentAnnotations.values()) a.capa = length;
        fragmentAnnotations.put( (Class<DataAnnotation>)klass, (FragmentAnnotation<DataAnnotation>) ano);
        return ano;
    }
    public <T extends DataAnnotation> LossAnnotation<T> addLossAnnotation(Class<T> klass, Supplier<T> constructor) {
        if (lossAnnotations.containsKey(klass))
            throw new RuntimeException("Loss annotation '" + klass.getName() + "' is already present.");
        final BitSet ids = new BitSet();
        for (LossAnnotation<DataAnnotation> a : lossAnnotations.values())
                ids.set(a.id);
        final int id = ids.nextClearBit(0);
        final int length = ids.length();
        final LossAnnotation<T> ano = new LossAnnotation<T>(id, length, klass, constructor);
        for (LossAnnotation<DataAnnotation> a : lossAnnotations.values()) a.capa = length;
        lossAnnotations.put((Class<DataAnnotation>) klass, (LossAnnotation<DataAnnotation>) ano);
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T extends DataAnnotation> FragmentAnnotation<T> getOrCreateFragmentAnnotation(Class<T> klass) {
        if (fragmentAnnotations.containsKey(klass)) return (FragmentAnnotation<T>) fragmentAnnotations.get(klass);
        return addFragmentAnnotation(klass,()->null);
    }

    @SuppressWarnings("unchecked cast")
    public <T extends DataAnnotation> LossAnnotation<T> getOrCreateLossAnnotation(Class<T> klass) {
        if (lossAnnotations.containsKey(klass)) return (LossAnnotation<T>) lossAnnotations.get(klass);
        return addLossAnnotation(klass,()->null);
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
        return addLoss(u, v, u.formula.isEmpty() || v.formula.isEmpty() ? MolecularFormula.emptyFormula() : u.formula.subtract(v.formula));
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

    /**
     * returns the mass error of this fragment after recalibration.
     * Returns NULL_DEVIATION if the fragment does not corresponds to any peak.
     */
    public Deviation getRecalibratedMassError(Fragment fragment) {
        AnnotatedPeak p = getFragmentAnnotationOrNull(AnnotatedPeak.class) != null ? getFragmentAnnotationOrNull(AnnotatedPeak.class).get(fragment) : null;
        if (p != null && p.isMeasured()) {
            return getMassErrorTo(fragment, p.getRecalibratedMass());
        } else return Deviation.NULL_DEVIATION;
    }

    /**
     * returns the mass error of this fragment. Returns NULL_DEVIATION if the fragment does not corresponds to any peak.
     */
    public Deviation getMassError(Fragment fragment) {
        AnnotatedPeak p = getFragmentAnnotationOrNull(AnnotatedPeak.class) != null ? getFragmentAnnotationOrNull(AnnotatedPeak.class).get(fragment) : null;
        if (p != null && p.isMeasured()) {
            return getMassErrorTo(fragment, p.getMass());
        } else return Deviation.NULL_DEVIATION;
    }

    public Deviation getMassErrorTo(Fragment fragment, double referenceMass) {
        ImplicitAdduct adduct = getFragmentAnnotationOrNull(ImplicitAdduct.class) != null ? getFragmentAnnotationOrNull(ImplicitAdduct.class).get(fragment) : null;
        if (adduct == null)
            adduct = ImplicitAdduct.none();
        return Deviation.fromMeasurementAndReference(referenceMass, fragment.getIonization().addToMass(fragment.formula.add(adduct.getAdductFormula()).getMass()));
    }
}
