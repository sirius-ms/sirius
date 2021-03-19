

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

package de.unijena.bioinf.fingerid.fingerprints;

import gnu.trove.list.array.TIntArrayList;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashSet;

import static org.openscience.cdk.CDKConstants.ISAROMATIC;
import static org.openscience.cdk.CDKConstants.ISINRING;

public class SmartsBuilder {

    private int[][] graph;
    private IAtomContainer molecule;
    private GraphUtil.EdgeToBondMap edgeToBondMap;
    private final BitSet selected;

    private boolean useAromaticity = true;
    private boolean ignoreBondTypeInRings = true;
    private int maxAtoms = 20;

    public SmartsBuilder(IAtomContainer molecule) {
        this.molecule = molecule;
        this.edgeToBondMap = GraphUtil.EdgeToBondMap.withSpaceFor(molecule);
        this.graph = GraphUtil.toAdjList(molecule, edgeToBondMap);
        this.selected = new BitSet(molecule.getAtomCount());
    }

    public HashSet<String> buildRadialSmarts() {

        for (int u = 0; u < graph.length; ++u) {
            int b=0;
            for (int v : graph[u]) {
                if (edgeToBondMap.get(u,v).getFlag(ISINRING)) ++b;
            }
            if (b >= 2) molecule.getAtom(u).setFlag(ISINRING, true);
        }

        final HashSet<String> set = new HashSet<>();
        final ArrayDeque<Integer> stack = new ArrayDeque();
        for (int k=0, n = molecule.getAtomCount(); k < n; ++k) {
            selected.clear();
            selected.set(k);
            // add all neighbours to stack
            for (int i : graph[k]) {
                if (!selected.get(i)) stack.offerLast(i);
            }
            // poll something from stack
            while (!stack.isEmpty()) {
                final Integer ext = stack.pollFirst();
                selected.set(ext);
                // add all neighbours to stack
                for (int i : graph[ext]) {
                    if (!selected.get(i)) stack.offerLast(i);
                }
                if (selected.cardinality() > maxAtoms) continue;
                // build smarts
                set.add(buildSmart());
            }
        }
        return set;
    }

    public HashSet<String> buildRadialSmarts2() {

        for (int u = 0; u < graph.length; ++u) {
            int b=0;
            for (int v : graph[u]) {
                if (edgeToBondMap.get(u,v).getFlag(ISINRING)) ++b;
            }
            if (b >= 2) molecule.getAtom(u).setFlag(ISINRING, true);
        }

        final HashSet<BitSet> keys = new HashSet<>();
        final int N = molecule.getAtomCount();
        final HashSet<String> set = new HashSet<>();
        final TIntArrayList stack = new TIntArrayList();
        final TIntArrayList stack2 = new TIntArrayList();
        final BitSet visited = new BitSet(molecule.getAtomCount());
        for (int k=0, n = molecule.getAtomCount(); k < n; ++k) {
            selected.clear();
            stack.clear();
            stack2.clear();
            selected.set(k);
            visited.clear();
            stack.add(k);
            visited.set(k);
            for (int radius=0; radius <= 3; ++radius) {
                if (radius >= 1) {
                    for (int i=0, len=stack.size(); i < len; ++i) {
                        final int u = stack.get(i);
                        assert visited.get(u);
                        assert !selected.get(u);
                        selected.set(u);
                        if (!keys.contains(selected)) {
                            keys.add((BitSet) selected.clone());
                            set.add(buildSmart());
                        }
                        selected.set(u,false);
                    }
                }
                for (int i=0, len=stack.size(); i < len; ++i) {
                    final int u = stack.get(i);
                    assert visited.get(u);
                    selected.set(u);
                    for (int v : graph[u]) {
                        if (!visited.get(v)) {
                            stack2.add(v);
                            visited.set(v);
                        }
                    }
                }
                stack.clear();
                stack.addAll(stack2);
                stack2.clear();
                if (!keys.contains(selected)) {
                    keys.add((BitSet) selected.clone());
                    set.add(buildSmart());
                }
            }
        }
        return set;
    }

    public boolean isUseAromaticity() {
        return useAromaticity;
    }

    public void setUseAromaticity(boolean useAromaticity) {
        this.useAromaticity = useAromaticity;
    }

    public void addToSelection(int atomId) {
        selected.set(atomId);
    }

    public String buildSmart() {
        final BitSet visited = new BitSet(molecule.getAtomCount());
        for (int k=0, n = molecule.getAtomCount(); k < n; ++k) {
            if (!selected.get(k)) visited.set(k, true);
        }
        int atom = selected.nextSetBit(0);
        final StringBuilder buffer = new StringBuilder();
        writeAtomWithNeighbourhood(visited, buffer, atom);
        return buffer.toString();
    }

    private void writeAtomWithNeighbourhood(BitSet visited, StringBuilder sb, int atom) {
        visited.set(atom);
        writeAtom(sb, atom);
        writeNeighbourhood(visited, sb, atom);
    }

    private void writeNeighbourhood(BitSet visited, StringBuilder sb, int atom) {
        final TIntArrayList buffer = new TIntArrayList(4);
        // count selected neighbours
        int n=0;
        for (int k=0; k < graph[atom].length; ++k) {
            if (!visited.get(graph[atom][k])) buffer.add(graph[atom][k]);
        }
        if (buffer.size()>1) {
            // branch
            for (int k=0; k < buffer.size()-1; ++k) {
                sb.append('(');
                writeBond(sb, atom, buffer.get(k));
                writeAtomWithNeighbourhood(visited, sb, buffer.get(k));
                sb.append(')');
            }
            writeBond(sb, atom, buffer.get(buffer.size()-1));
            writeAtomWithNeighbourhood(visited, sb, buffer.get(buffer.size()-1));
        } else if (buffer.size()>0) {
            // do not branch
            writeBond(sb, atom, buffer.get(0));
            writeAtomWithNeighbourhood(visited, sb, buffer.get(0));
        }
    }

    private void writeAtom(StringBuilder buffer, int atom) {
        final IAtom a = molecule.getAtom(atom);
        if (useAromaticity) {
            if (a.getFlag(ISAROMATIC)) {
                buffer.append('[').append(a.getSymbol().toLowerCase());
            } else {
                buffer.append('[').append('#').append(a.getAtomicNumber());
                if (a.getFlag(ISINRING)) {
                    buffer.append(';').append('R');
                } else {
                    buffer.append(";!R");
                }
            }
        } else {
            buffer.append('[').append('#').append(a.getAtomicNumber());
            if (a.getFlag(ISINRING)) {
                buffer.append(';').append('R');
            } else {
                buffer.append(";!R");
            }
        }
        // get number of explicit neighbours
        buffer.append(";D").append(String.valueOf(graph[atom].length));
        buffer.append(']');
    }

    private void writeBond(StringBuilder buffer, int a, int b) {
        final IBond bond = edgeToBondMap.get(a,b);
        if (bond.getFlag(ISAROMATIC)) {
            buffer.append(':');
            return;
        }
        if (ignoreBondTypeInRings && molecule.getAtom(a).getFlag(ISINRING) && molecule.getAtom(b).getFlag(ISINRING)) {
            buffer.append('@');
            return;
        }
        final IBond.Order order = edgeToBondMap.get(a,b).getOrder();
        switch (order) {
            case SINGLE: break;
            case DOUBLE: buffer.append('='); break;
            case TRIPLE: buffer.append('#'); break;
            default: buffer.append('~'); break;
        }
    }


}
