
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ftalign.parallelograms;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotReader;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

public class ParallelogramIterator implements Iterator<FTree> {

    private int i, k, n;
    private ArrayList<ParaNodes> parallelograms;
    private FTree originalTree;
    private FTree modifiedtree;


    public ParallelogramIterator(FTree originalTree) {
        this.originalTree = originalTree;
        this.i = 0;
        this.parallelograms = new ParaCount(originalTree).getParallelogram();
        this.n = parallelograms.size();
        this.k = 1 << n; // if we have n parallelograms we have 2^n possible trees! 2^n is equal to 1<<n
        genNext();
    }


    private static boolean isRealAncestor(Fragment u, Fragment v) {
        while (!v.isRoot()) {
            v = v.getParent();
            if (v == u) return true;
        }
        return false;
    }

    /*

    private static boolean search(TreeCursor<Fragment> cursor, MolecularFormula formula) {
        while (true) {
            final MolecularFormula current = cursor.getNode().getFormula();
            boolean equal = true;
            for (int k = 0; k < current.length(); ++k) {
                if (current.numberAt(k) < formula.numberAt(k)) {
                    while (!cursor.hasNextSibling())
                        if (cursor.isRoot()) return false;
                        else cursor.gotoParent();
                    cursor.gotoNextSibling();
                } else if (current.numberAt(k) > formula.numberAt(k)) {
                    equal = false;
                }
            }
            if (equal) return true;
            else if (cursor.isLeaf()) {
                while (!cursor.hasNextSibling())
                    if (cursor.isRoot()) return false;
                    else cursor.gotoParent();
                cursor.gotoNextSibling();
            } else cursor.gotoFirstChild();
        }
    }

    */

    public static void print(FTree tree) {
        try {
            new FTDotWriter().writeGraph(new OutputStreamWriter(System.out), tree, Collections.<Fragment, List<String>>emptyMap(), Collections.<Fragment, Map<Class<?>, Double>>emptyMap(),
                    Collections.<Loss, Map<Class<?>, Double>>emptyMap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FTree parseFile(File f) {
        try {
            return new GenericParser<FTree>(new FTDotReader()).parseFile(f);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        testAligning();
    }

    private static void testAligning() {
        FTree tree = parseFile(new File("/home/kaidu/Documents/temp/foo/challenge14.dot"));
        // ...

    }

    private static void testCounting() {
        for (File f : new File("/home/kaidu/Documents/temp/foo").listFiles()) {
            FTree tree = parseFile(f);
            System.out.println(f);
            //System.out.println(getPara(tree).size());
        }
    }

    private void genNext() {
        while (i++ < n) {
            modifiedtree = generateModifiedTree(i - 1);
            if (modifiedtree != null) return;
        }
        modifiedtree = null;
    }

    @Override
    public boolean hasNext() {
        return modifiedtree != null;
    }

    @Override
    public FTree next() {
        final FTree t = modifiedtree;
        genNext();
        return t;
    }

    private FTree generateModifiedTree(int bitset) {
        final FTree changedTree = new FTree(originalTree);
        for (int j = 0; j < n; ++j) {
            if ((bitset & (1 << j)) != 0) { // if j-th bit is set
                ParaNodes temp = parallelograms.get(j);
                Fragment x = changedTree.getFragmentAt(temp.getX().getVertexId());
                Fragment y = changedTree.getFragmentAt(temp.getY().getVertexId());
                Fragment u = changedTree.getFragmentAt(temp.getU().getVertexId());
                Fragment v = changedTree.getFragmentAt(temp.getV().getVertexId());

                TreeCursor<Fragment> cursor;

                // there have to be an edge xy
                if (changedTree.isConnected(x, y)) return null;

                // u is an ancestor of x.
                if (!isRealAncestor(u, x)) return null;

                // v is an ancestor of y.
                if (!isRealAncestor(v, y)) return null;

                // u is no ancestor of y!
                if (isRealAncestor(u, y)) return null;

                // delete edge (pv,v) and add edge (u,v)
                changedTree.swapLoss(v, u);
            }
        }
        return changedTree;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
