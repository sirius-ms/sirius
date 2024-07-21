/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.msnovelist;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty
public class NumberOfMsNovelistCandidates  implements Ms2ExperimentAnnotation {
    public static final NumberOfMsNovelistCandidates ZERO = new NumberOfMsNovelistCandidates(0);
    public static final NumberOfMsNovelistCandidates MIN_VALUE = new NumberOfMsNovelistCandidates(Integer.MIN_VALUE);
    public static final NumberOfMsNovelistCandidates MAX_VALUE = new NumberOfMsNovelistCandidates(Integer.MAX_VALUE);
    public static final NumberOfMsNovelistCandidates ONE = new NumberOfMsNovelistCandidates(1);

    public final int value;

    private NumberOfMsNovelistCandidates() {
        this(0);
    }
    public NumberOfMsNovelistCandidates(int value) {
        this.value = value;
    }
}
