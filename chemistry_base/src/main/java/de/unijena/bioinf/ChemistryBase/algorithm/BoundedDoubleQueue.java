
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

package de.unijena.bioinf.ChemistryBase.algorithm;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A fixed size queue of double values. Inserting a new value into a full queue will lead to a removal of the smallest element
 */
public final class BoundedDoubleQueue implements Iterable<Double> {

    public static void main(String[] args) {
        final BoundedDoubleQueue queue = new BoundedDoubleQueue(10);
        for (int i=20; i >= 0; --i) {
            queue.add(i);
        }
        System.out.println(Arrays.toString(queue.toArray()));
    }

    private final double[] values;
    private int length;

    public BoundedDoubleQueue(int size) {
        this.values = new double[size];
        Arrays.fill(values, Double.NEGATIVE_INFINITY);
        this.length = 0;
    }

    public int length() {
        return length;
    }

    public double min() {
        if (length < values.length) {
            return Arrays.stream(values).limit(length).min().orElse(Double.NEGATIVE_INFINITY);
        } else return values[0];
    }

    public double max() {
        if (length < values.length) {
            return Arrays.stream(values).limit(length).max().orElse(Double.POSITIVE_INFINITY);
        } else return values[values.length-1];
    }

    public boolean add(double value) {
        if (length < values.length) {
            values[length++] = value;
            if (length==values.length) Arrays.sort(values);
            return true;
        } else {
            if (value <= values[0]) return false;
            final int index = ((length <= 5) ? linearSearch(value) : binarySearch(value));
            System.arraycopy(values, 1, values, 0, index-1);
            values[index-1] = value;
            return true;
        }

    }

    private int binarySearch(double value) {
        final int index = Arrays.binarySearch(values, 0, values.length, value);
        if (index >= 0) return index;
        else return (-index-1);
    }

    private int linearSearch(double value) {
        for (int i=0; i < values.length; ++i) {
            if (values[i] > value) {
                return i;
            }
        }
        return values.length;
    }

    public double median() {
        if (length < values.length) {
            double[] copy = Arrays.copyOf(values, length);
            Arrays.sort(copy);
            return copy[copy.length/2];
        } else return values[values.length/2];
    }

    public double average() {
        return Arrays.stream(values).average().orElse(0d);
    }

    @NotNull
    @Override
    public Iterator<Double> iterator() {
        return Arrays.stream(values).limit(length).iterator();
    }

    public double[] toArray() {
        if (length>=values.length) return values.clone();
        else {
            double[] copy = Arrays.copyOf(values, length);
            Arrays.sort(copy);
            return copy;
        }
    }
}
