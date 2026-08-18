/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.middleware.service.search.description;

import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * States what kind of value a field holds, where the index cannot say.
 * <p>
 * A field mapped straight from a java field is typed from that java type, which is what tells an enum from a
 * boolean from plain text. A field a {@code FieldMapper} contributes has no java type - the mapper invents both
 * the name and the value - so all that is left is how lucene holds it, and lucene holds a keyword either way.
 * A mapper writing a flag as {@code true} is the only one who knows it is a flag.
 * <p>
 * Like a vocabulary, this is documentation: it changes what a client is told a field is, never what a query
 * matches. Implementations must be stateless and have a public no-arg constructor.
 *
 * @see SearchableFieldDoc#fieldTypes()
 */
public interface FieldTypes {

    /**
     * @param fieldName the full path of the field as used in queries. One statement covers every field a mapper
     *                  contributes, so a type that applies to one of them is decided by the name
     * @return the type to report, or null to keep the one derived from the index
     */
    @Nullable
    SearchableField.FieldType typeOf(@NotNull String fieldName);

    /** The default: nothing to add, the index decides. */
    class None implements FieldTypes {
        @Override
        public @Nullable SearchableField.FieldType typeOf(@NotNull String fieldName) {
            return null;
        }
    }
}
