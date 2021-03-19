
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

package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.treealign.AbstractBacktrace;

import java.io.PrintStream;
import java.util.Iterator;

public class TraceLog2 extends AbstractBacktrace<Fragment> {

    private final PrintStream out;

    public TraceLog2(PrintStream out) {
        this.out = out;
    }

    public TraceLog2() {
        this.out = System.out;
    }

    @Override
    public void deleteLeft(float score, Fragment node) {
        out.println(nodeInfo(node) + ",-,-," + score + ",DELETE");
    }

    private String nodeInfo(Fragment node) {
        return node.getFormula() + "," + node.getIncomingEdge().getFormula();
    }

    @Override
    public void deleteRight(float score, Fragment node) {
        out.println("-,-," + nodeInfo(node) + "," + score + ",DELETE");
    }

    @Override
    public void match(float score, Fragment left, Fragment right) {
        out.println(nodeInfo(left) + "," + nodeInfo(right) + "," + score + ",MATCH");
    }

    @Override
    public void matchVertices(float score, Fragment left, Fragment right) {
        out.println(nodeInfoR(left) + "," + nodeInfoR(right) + "," + score + ",ROOT");
    }

    private String nodeInfoR(Fragment left) {
        return left.getFormula() + ",!";
    }

    @Override
    public void join(float score, Iterator<Fragment> left, Iterator<Fragment> right, int leftNumber, int rightNumber) {
        out.println(nodeInfo(left.next()) + "," + nodeInfo(right.next()) + "," + score + ",JOIN");
    }

    @Override
    public void innerJoinLeft(Fragment node) {
        out.println(nodeInfo(node) + ",*,*," + "0" + ",PREJOIN");
    }

    @Override
    public void innerJoinRight(Fragment node) {
        out.println("*,*," + nodeInfo(node) + ",0" + ",PREJOIN");
    }
}
