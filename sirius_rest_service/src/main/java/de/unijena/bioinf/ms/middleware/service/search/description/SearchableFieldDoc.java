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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What to tell API users about an indexed field, beyond what the index itself knows.
 * <p>
 * Separate from the annotations that configure the index on purpose: how a field is stored, analyzed and parsed
 * decides what a query matches, while everything here only decides how the field is explained. Removing this
 * annotation from a field changes no search result.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SearchableFieldDoc {

    /**
     * A vocabulary for this field, so that clients can offer its values for completion instead of leaving the
     * user to guess them. Names a class rather than the values themselves, so the vocabulary stays where it is
     * defined (see {@link FieldVocabulary}).
     * <p>
     * Fields whose values follow from their java type (enums, booleans) need none. A declared vocabulary is the
     * more specific statement and wins over those derived values. A vocabulary that depends on the project is
     * not declared here at all - it is handed to the description by whoever owns the project state.
     */
    Class<? extends FieldVocabulary> possibleValues() default FieldVocabulary.None.class;
}
