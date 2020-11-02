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

package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

public final class RetentionTime implements Ms2ExperimentAnnotation, Comparable<RetentionTime> {

    private final double start, middle, end;
    protected static final RetentionTime MISSING_RT_VALUE = new RetentionTime(Double.NaN);

    public RetentionTime(double start, double end) {
        this(start, end, start + (end - start) / 2d);
    }

    public RetentionTime(double start, double end, double maximum) {
        if (!Double.isNaN(start)) {
            if (start >= end)
                throw new IllegalArgumentException("No proper interval given: [" + start + ", " + end + "]" );
            if (maximum < start || maximum > end) {
                throw new IllegalArgumentException("Given retention time middle is not in range: " + maximum + " is not in [" + start + ", " + end + "]" );
            }
        }
        this.start = start;
        this.end = end;
        this.middle = maximum;
    }

    public RetentionTime(double timeInSeconds) {
        this(Double.NaN, Double.NaN, timeInSeconds);
    }

    public RetentionTime merge(RetentionTime other) {
        if (isInterval() && other.isInterval())
            return new RetentionTime(Math.min(start, other.start), Math.max(end, other.end));
        else
            return new RetentionTime(Math.min(start, other.start), Math.max(end, other.end), (middle+other.middle)/2);
    }

    public boolean isInterval() {
        return !Double.isNaN(start);
    }

    public double getRetentionTimeInSeconds() {
        return middle;
    }

    public double getStartTime() {
        if (!isInterval()) throw new RuntimeException("No retention time range given");
        return start;
    }

    public double getEndTime() {
        if (!isInterval()) throw new RuntimeException("No retention time range given");
        return end;
    }

    public double getMiddleTime() {
        return middle;
    }

    @Override
    public String toString() {
        return middle + " in [" + start + ", " + end + "]";
    }

    public String asStringValue() {
        return asStringValue(this);
    }

    public static String asStringValue(RetentionTime rt) {
        return rt.middle + ":" + rt.start + "," + rt.end;
    }

    public static RetentionTime fromStringValue(String rt) {
        String[] one = rt.split(":");
        String[] two = one[1].split(",");
        return new RetentionTime(Double.parseDouble(two[0].strip()), Double.parseDouble(two[1].strip()), Double.parseDouble(one[0].strip()));
    }

    @Override
    public int compareTo(@NotNull RetentionTime o) {
        return Double.compare(middle, o.middle);
    }

    public static <T extends RetentionTime> T NA(@NotNull Class<T> retentionTime) {
        return  (T) MISSING_RT_VALUE;
    }

}
