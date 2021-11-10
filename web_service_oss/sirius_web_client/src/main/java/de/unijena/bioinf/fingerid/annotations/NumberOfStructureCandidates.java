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

package de.unijena.bioinf.fingerid.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty
public class NumberOfStructureCandidates implements Ms2ExperimentAnnotation {
    public static final NumberOfStructureCandidates ZERO = new NumberOfStructureCandidates(0);
    public static final NumberOfStructureCandidates MIN_VALUE = new NumberOfStructureCandidates(Integer.MIN_VALUE);
    public static final NumberOfStructureCandidates MAX_VALUE = new NumberOfStructureCandidates(Integer.MAX_VALUE);
    public static final NumberOfStructureCandidates ONE = new NumberOfStructureCandidates(1);

    public final int value;

    private NumberOfStructureCandidates() {
        this(0);
    }
    public NumberOfStructureCandidates(int value) {
        this.value = value;
    }
}
