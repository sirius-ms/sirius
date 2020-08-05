
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
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

public class Fragment implements Comparable<Fragment> {

    protected final static Loss[] EMPTY_EDGES = new Loss[0];
    private final static Object[] EMPTY_ANNO = new Object[0];

    protected MolecularFormula formula;
    protected Ionization ionization;
    protected short color;

    protected short peakId;


    protected Loss[] outgoingEdges;
    protected Object[] annotations;
    protected int vertexId;
    protected short outDegree;
    protected Loss[] incomingEdges;
    protected short inDegree;

    public Fragment(int vertexId) {
        this(vertexId, null, null);
    }

    public Fragment(int vertexId, MolecularFormula formula, Ionization ionization) {
        this.formula = formula;
        this.ionization = ionization;
        this.color = 0;
        this.outDegree = 0;
        this.outgoingEdges = EMPTY_EDGES;
        this.annotations = EMPTY_ANNO;
        this.vertexId = vertexId;
        this.inDegree = 0;
        this.incomingEdges = EMPTY_EDGES;
    }

    protected Fragment(Fragment other) {
        this.formula = other.formula;
        this.ionization = other.ionization;
        this.color = other.color;
        this.peakId = other.peakId;
        this.outDegree = other.outDegree;
        this.outgoingEdges = other.outgoingEdges.clone();
        this.annotations = other.annotations.clone();
        this.vertexId = other.vertexId;
        this.inDegree = other.inDegree;
        this.incomingEdges = other.incomingEdges.clone();
    }

    /**
     * Reduce memory usage of this fragment by compacting the internal array structure.
     * Call this after deleting lot of edges
     */
    public void compact() {
        if (outgoingEdges.length > outDegree+2) {
            outgoingEdges = Arrays.copyOf(outgoingEdges, outDegree);
        }
        if (incomingEdges.length > inDegree+2) {
            incomingEdges = Arrays.copyOf(incomingEdges, inDegree);
        }
    }

    public boolean isDeleted() {
        return vertexId < 0;
    }

    public int getVertexId() {
        return vertexId;
    }

    void setVertexId(int vertexId) {
        this.vertexId = vertexId;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = (short)color;
    }

    public short getPeakId() {
        return peakId;
    }

    public void setPeakId(int peakId) {
        this.peakId = (short)peakId;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public Ionization getIonization() {
        return ionization;
    }

    public void setFormula(MolecularFormula formula, Ionization ionization) {
        this.formula = formula;
        this.ionization = ionization;
    }

    public Loss getOutgoingEdge(int k) {
        if (k >= outDegree)
            throw new IndexOutOfBoundsException("Index " + k + ". Number of outgoing edges: " + outDegree);
        return outgoingEdges[k];
    }

    public Loss getIncomingEdge(int k) {
        if (k >= inDegree)
            throw new IndexOutOfBoundsException("Index " + k + ". Number of incoming edges: " + inDegree);
        return incomingEdges[k];
    }

    /**
     * Is only recommended for tree structures. Returns the first incoming edge of the vertex
     *
     * @return
     */
    public Loss getIncomingEdge() {
        return getIncomingEdge(0);
    }

    public Fragment getParent() {
        return getParent(0);
    }

    public Fragment getChildren(int k) {
        return getOutgoingEdge(k).target;
    }

    public Fragment getParent(int k) {
        return getIncomingEdge(k).source;
    }

    public List<Fragment> getChildren() {
        return new AbstractList<Fragment>() {
            @Override
            public Fragment get(int index) {
                return getChildren(index);
            }

            @Override
            public int size() {
                return outDegree;
            }
        };
    }

    public List<Loss> getOutgoingEdges() {
        return new AbstractList<Loss>() {
            @Override
            public Loss get(int index) {
                return outgoingEdges[index];
            }

            @Override
            public int size() {
                return outDegree;
            }
        };
    }

    public List<Loss> getIncomingEdges() {
        return new AbstractList<Loss>() {
            @Override
            public Loss get(int index) {
                return incomingEdges[index];
            }

            @Override
            public int size() {
                return inDegree;
            }
        };
    }

    public List<Fragment> getParents() {
        return new AbstractList<Fragment>() {
            @Override
            public Fragment get(int index) {
                return getParent(index);
            }

            @Override
            public int size() {
                return inDegree;
            }
        };
    }

    public int getInDegree() {
        return inDegree;
    }

    public int getOutDegree() {
        return outDegree;
    }

    final Object getAnnotation(int id) {
        if (id >= annotations.length) return null;
        return annotations[id];
    }

    final void setAnnotation(int id, int capa, Object o) {
        if (id >= annotations.length) annotations = Arrays.copyOf(annotations, Math.max(capa, id + 1));
        annotations[id] = o;
    }

    public boolean isRoot() {
        return inDegree == 0;
    }

    public boolean isLeaf() {
        return outDegree == 0;
    }

    public String toString() {
        return formula + " <- (" + Arrays.toString(incomingEdges) + ")";
    }

    @Override
    public int compareTo(Fragment o) {
        return getFormula().compareTo(o.getFormula());
    }

    public static <T> TCustomHashMap<Fragment, T> newFragmentWithIonMap() {
        return new TCustomHashMap<>(new HashFormulaWithIon());
    }

    protected static class HashFormulaWithIon implements HashingStrategy<Fragment> {

        @Override
        public int computeHashCode(Fragment object) {
            return object.getFormula().hashCode() ^ 17*object.getIonization().hashCode();
        }

        @Override
        public boolean equals(Fragment o1, Fragment o2) {
            return o1.getFormula().equals(o2.getFormula()) && o1.getIonization().equals(o2.getIonization());
        }
    }
}
