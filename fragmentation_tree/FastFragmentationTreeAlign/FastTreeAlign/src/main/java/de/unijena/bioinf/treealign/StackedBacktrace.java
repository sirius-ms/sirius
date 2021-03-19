
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

import java.util.*;

/**
 * @author Kai Dührkop
 */
public class StackedBacktrace<T> implements Backtrace<T> {

    private final List<Backtrace<T>> stack;

    public StackedBacktrace() {
        this.stack = new ArrayList<Backtrace<T>>();
    }

    public StackedBacktrace(Backtrace<T>... tracers) {
        this();
        pushAll(tracers);
    }

    @SuppressWarnings("unchecked")
    public <S extends Backtrace<T>> S get(Class<S> klass) {
        for (Backtrace<T> trace : stack) {
            if (klass.isInstance(trace)) return (S) trace;
        }
        return null;
    }

    public void pushAll(Backtrace<T>... tracers) {
        stack.addAll(Arrays.asList(tracers));
    }

    public void push(Backtrace<T> trace) {
        stack.add(trace);
    }

    public void pop() {
        stack.remove(stack.size() - 1);
    }

    @Override
    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber) {
        if (stack.size() == 1) stack.get(0).join(score, left, right, leftNumber, rightNumber);
        else {
            List<T> leftList = new LinkedList<T>();
            while (left.hasNext()) {
                leftList.add(left.next());
            }
            List<T> rightList = new LinkedList<T>();
            while (right.hasNext()) {
                rightList.add(right.next());
            }

            for (Backtrace<T> tracer : stack) {
                tracer.join(score, leftList.iterator(), rightList.iterator(), leftNumber, rightNumber);
            }
        }
    }

    @Override
    public void deleteLeft(float score, T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.deleteLeft(score, node);
        }
    }

    @Override
    public void deleteRight(float score, T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.deleteRight(score, node);
        }
    }

    @Override
    public void match(float score, T left, T right) {
        for (Backtrace<T> tracer : stack) {
            tracer.match(score, left, right);
        }
    }

    @Override
    public void innerJoinLeft(T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.innerJoinLeft(node);
        }
    }

    @Override
    public void innerJoinRight(T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.innerJoinRight(node);
        }
    }

    @Override
    public void matchVertices(float score, T left, T right) {
        for (Backtrace<T> tracer : stack) {
            tracer.matchVertices(score, left, right);
        }
    }
}
