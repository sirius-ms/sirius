
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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

import java.util.ArrayList;
import java.util.BitSet;


public class ParaCount {

    private final ArrayList<ParaNodes> parallelogram;
    private final BitSet flagged;

    public ParaCount(FTree tree) {
        Fragment root = tree.getRoot();
        this.parallelogram = new ArrayList<ParaNodes>();
        this.flagged = new BitSet(tree.numberOfVertices());
        OuterTraversal(root, root);
    }

    public ArrayList<ParaNodes> getParallelogram() {
        return this.parallelogram;
    }

    private void OuterTraversal(Fragment u, Fragment root) { // root eingabe
        flagged.set(u.getVertexId(), true);
        InnerTraversal(u, root, root, root, false);
        for (int i = 0; i < u.getOutDegree(); i++) {
            OuterTraversal(u.getChildren(i), root);
        }
        flagged.set(u.getVertexId(), false);
    }

    private void InnerTraversal(Fragment u, Fragment v, Fragment x, Fragment y, Boolean updateY) {
        if (u.getFormula().equals(v.getFormula())) {
            return;
        }
        if (flagged.get(v.getVertexId())) {
            x = v;
        } else {
            if (updateY) {
                y = v;
            }
            MolecularFormula uANDy = u.getFormula().add(y.getFormula());
            MolecularFormula vANDx = v.getFormula().add(x.getFormula());
            if (uANDy.equals(vANDx)) {    // loss ( labels )
                ParaNodes nodes = new ParaNodes(x, y, u, v);
                parallelogram.add(nodes);
            }
            if (!(vANDx.isSubtractable(uANDy)) || !(uANDy.isSubtractable(x.getFormula()))) {
                return;
            }
        }
        for (int i = 0; i < v.getOutDegree(); i++) {
            InnerTraversal(u, v.getChildren(i), x, y, flagged.get(v.getVertexId()));
        }
    }
}
