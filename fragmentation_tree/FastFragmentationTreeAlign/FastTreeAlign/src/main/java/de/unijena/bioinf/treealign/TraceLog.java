
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

package de.unijena.bioinf.treealign;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Iterator;

public class TraceLog<T> extends AbstractBacktrace<T> {

    private final PrintStream out;

    public TraceLog(PrintStream out) {
        this.out = out;
    }

    public TraceLog() {
        this.out = System.out;
    }

    @Override
    public void deleteLeft(float score, T node) {
        out.println("DELETE LEFT " + node + " WITH SCORE "+ score);
    }

    @Override
    public void deleteRight(float score, T node) {
        out.println("DELETE RIGHT " + node + " WITH SCORE "+ score);
    }

    @Override
    public void match(float score, T left, T right) {
        out.println("MATCH " + left + " WITH: " + right + " WITH SCORE "+ score);
    }

    @Override
    public void matchVertices(float score, T left, T right) {
        out.println("MATCH FRAGMENTS OF " + left + " WITH: " + right + " WITH SCORE "+ score);
    }
    
    @Override
    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber) {
        final ArrayDeque<T> lefts = new ArrayDeque<T>(leftNumber);
        final ArrayDeque<T> rights = new ArrayDeque<T>(rightNumber);
        while (left.hasNext()) lefts.offerFirst(left.next());
        while (right.hasNext()) rights.offerFirst(right.next());
        out.print("JOIN (" + lefts.removeFirst() + (lefts.isEmpty() ? ")" :  " WITH "));
        while (!lefts.isEmpty()) {
            out.print(lefts.removeFirst() + (lefts.isEmpty() ? ")" :  " WITH "));
        }
        out.print(" MATCHING: (" + rights.removeFirst() + (rights.isEmpty() ? "" :  " WITH "));
        while (!rights.isEmpty()) {
            out.print(rights.removeFirst() + (rights.isEmpty() ? "" :  " WITH "));
        }
        out.println(" WITH SCORE " + score);
    }
}
