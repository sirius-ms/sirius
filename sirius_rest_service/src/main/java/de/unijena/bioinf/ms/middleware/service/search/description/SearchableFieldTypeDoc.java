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

import de.unijena.bioinf.projectspace.SearchableFieldDoc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What API type to report for a field the index can only see as the keyword it stores.
 * <p>
 * The companion of {@link SearchableFieldDoc}, and separate from it because the two say things at different
 * levels. What values a field can hold is a statement about the model, made where the model is defined - which
 * may be a module that knows nothing about this service, which is why that annotation lives next to
 * {@code IndexField}. The API type is a statement in this service's own vocabulary
 * ({@code SearchableField.FieldType}), so it is declared here, on the fields a {@code FieldMapper} contributes -
 * the only fields with no java type to be more precise about.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SearchableFieldTypeDoc {

    /**
     * The types to report for the fields this field's mapper contributes - see {@link FieldTypes}. Names a class
     * rather than a type, so one statement covers every field the mapper adds and which of them it applies to is
     * decided by the field name.
     * <p>
     * A field mapped straight from a java field needs none; its java type is more precise than anything that
     * could be declared here.
     */
    Class<? extends FieldTypes> value() default FieldTypes.None.class;
}
