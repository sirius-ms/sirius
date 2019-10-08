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
package de.unijena.bioinf.ChemistryBase.algorithm;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A fixed size queue of double values. Inserting a new value into a full queue will lead to a removal of the smallest element
 */
public final class BoundedDoubleQueue implements Iterable<Double> {

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
        return values[0];
    }

    public double max() {
        return values[length-1];
    }

    public boolean add(double value) {
        if (value < values[0]) return false;
        final int index = (length <= 5) ? linearSearch(value) : binarySearch(value);
        if (length < values.length) {
            if (index < length) System.arraycopy(values, index, values, index+1, length-index);
            values[index] = value;
            ++length;
            return true;
        } else {
            if (index > 0) {
                if (index > 1) System.arraycopy(values, 1, values, 0, index-1);
                values[index-1] = value;
                return true;
            } else return false;
        }
    }

    private int binarySearch(double value) {
        final int index = Arrays.binarySearch(values, 0, length, value);
        if (index >= 0) return index;
        else return (-index-1);
    }

    private int linearSearch(double value) {
        for (int i=0; i < length; ++i) {
            if (values[i] > value) {
                return i;
            }
        }
        return length;
    }

    @NotNull
    @Override
    public Iterator<Double> iterator() {
        return Arrays.stream(values).iterator();
    }

    public double[] toArray() {
        return values.clone();
    }
}
