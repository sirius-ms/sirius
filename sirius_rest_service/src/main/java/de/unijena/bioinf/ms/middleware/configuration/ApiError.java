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

package de.unijena.bioinf.ms.middleware.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents an error this endpoint can return, with the wording a client developer should read.
 * <p>
 * Put it on the handler method, next to the mapping, so it travels with the endpoint when the method or the path
 * is renamed. {@link ErrorResponseDocumentation} picks it up and adds the response to the generated OpenAPI
 * document, which is also where the generic errors that need no per-endpoint wording come from.
 * <p>
 * Do NOT use swagger's own {@code @ApiResponse} for this: declaring it on a handler method makes springdoc drop
 * the responses it derived from the return type, and for generic return types those cannot be restated by hand.
 * <p>
 * Example:
 * <pre>
 * &#64;ApiError(status = 409, value = "A database with this id already exists.")
 * &#64;PostMapping("/{databaseId}")
 * public SearchableDatabase createDatabase(...)
 * </pre>
 *
 * @see NoApiError for the opposite case, an error the generic rules would document but that cannot occur here
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ApiErrors.class)
public @interface ApiError {

    /**
     * HTTP status of the error, e.g. 404.
     */
    int status();

    /**
     * What actually goes wrong, phrased for whoever calls the endpoint. Replaces the generic wording that the
     * status would otherwise get.
     */
    String value();
}
