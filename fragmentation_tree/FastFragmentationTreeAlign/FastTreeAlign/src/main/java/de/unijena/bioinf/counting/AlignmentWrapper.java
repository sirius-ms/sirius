
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

package de.unijena.bioinf.counting;

import de.unijena.bioinf.treealign.Backtrace;
import de.unijena.bioinf.treealign.TreeAlignmentAlgorithm;

public class AlignmentWrapper<T> implements TreeAlignmentAlgorithm<T> {

    private final Object algorithm;

    public AlignmentWrapper(DPPathCounting<T> algorithm) {
        this.algorithm = algorithm;
    }
    public AlignmentWrapper(WeightedPathCounting<T> algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public float compute() {
        if (algorithm instanceof DPPathCounting) {
            final long value = ((DPPathCounting)algorithm).compute();
            if (value > Float.MAX_VALUE) throw new RuntimeException("Return value exceeds float number space");
            return (float)value;
        } else if (algorithm instanceof WeightedPathCounting) {
            final double result = ((WeightedPathCounting)algorithm).compute();
            return (float)result;
        } else throw new IllegalStateException();
    }

    @Override
    public void backtrace(Backtrace<T> tracer) {
        throw new UnsupportedOperationException();
    }
}
