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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Supplies the values a searchable field can take, exactly as they are indexed.
 * <p>
 * Fields whose values come from a fixed domain (a compound class ontology, the adducts a project detected, a tag
 * restricted by its definition) are searchable but not guessable: a client can only offer them for completion if
 * it is told what they are. A vocabulary says what they are without the values themselves becoming string
 * literals in an annotation - they stay where they are defined.
 * <p>
 * This is documentation, not indexing: what a vocabulary reports changes what a client is offered and never what
 * a query matches. Implementations must be stateless and have a public no-arg constructor when they are named by
 * {@link SearchableFieldDoc}; a vocabulary that needs project state is constructed by whoever owns that state and
 * handed to the description instead.
 */
public interface FieldVocabulary {

    /**
     * @param fieldName the full path of the field as used in queries (e.g.
     *                  {@code topAnnotations.compoundClassAnnotation.npcPathway}). One vocabulary can serve
     *                  several fields, which is why the values are requested per field.
     * @return the values the field can take, exactly as they are indexed, or null if this vocabulary has none
     * for the given field (values derived from the java type, e.g. enum constants, then still apply)
     */
    @Nullable
    List<String> getPossibleValues(@NotNull String fieldName);

    /**
     * Combines vocabularies that each know different fields: the first one with an answer wins.
     */
    static FieldVocabulary firstOf(@NotNull FieldVocabulary... vocabularies) {
        return fieldName -> {
            for (FieldVocabulary vocabulary : vocabularies) {
                List<String> values = vocabulary.getPossibleValues(fieldName);
                if (values != null)
                    return values;
            }
            return null;
        };
    }

    /**
     * The default: the field has no closed vocabulary and accepts free text.
     */
    class None implements FieldVocabulary {
        @Override
        public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
            return null;
        }
    }
}
