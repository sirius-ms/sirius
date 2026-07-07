/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.core.tags;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Every element of a tag definition's {@code possibleValues} must match the declared value type. Previously
 * only the first element was type-checked, so a mixed-type list was accepted and later corrupted the typed
 * set / threw {@code ArrayStoreException} during serialization.
 */
public class ValueDefinitionTest {

    @Test
    public void mixedTypePossibleValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new ValueDefinition<>(ValueType.TEXT, List.of("a", Integer.valueOf(123)), (Object) null, (Object) null));
    }

    @Test
    public void mixedTypePossibleValuesAreRejectedWhenTheFirstElementIsValid() {
        // pins that validation looks past the first (valid) element
        assertThrows(IllegalArgumentException.class, () ->
                new ValueDefinition<>(ValueType.INTEGER, List.of(Integer.valueOf(1), "x"), (Object) null, (Object) null));
    }

    @Test
    public void homogeneousPossibleValuesAreAccepted() {
        ValueDefinition<?> def =
                new ValueDefinition<>(ValueType.TEXT, List.of("a", "b"), (Object) null, (Object) null);
        assertTrue(def.getPossibleValues().containsAll(List.of("a", "b")));
    }
}
