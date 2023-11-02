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

package de.unijena.bioinf.ms.annotations;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Any kind of annotation for input, intermediates and output.
 *
 * Note that annotations have to be:
 * - immutable
 * - low memory footprint
 * - serializable (e.g. by a toString and fromString method)
 */
public interface DataAnnotation {
    static <T extends DataAnnotation> String getIdentifier(Class<T> annotationType) {
        return annotationType.getName();
    }

    @JsonIgnore
    default String getIdentifier() {
        return getIdentifier(getClass());
    }
}
