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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts an endpoint out of a generic error that {@link ErrorResponseDocumentation} would otherwise document.
 * <p>
 * The generic rules are deliberately coarse: every endpoint can fail with 500, anything with a path variable can
 * answer 404, anything taking input can answer 400. Where that is wrong, say so here. Documenting an error that
 * cannot occur misleads a client developer just as much as documenting none.
 * <p>
 * Example, a delete that is idempotent and therefore never reports a missing object:
 * <pre>
 * &#64;NoApiError(404)
 * &#64;DeleteMapping("/{databaseId}")
 * public void removeDatabase(...)
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NoApiError {
    /**
     * HTTP statuses that this endpoint never returns.
     */
    int[] value();
}
