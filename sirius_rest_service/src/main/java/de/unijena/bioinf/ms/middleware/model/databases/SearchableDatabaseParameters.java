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

package de.unijena.bioinf.ms.middleware.model.databases;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.Size;

@Getter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchableDatabaseParameters {
    /**
     * display name of the database
     * Should be short
     */
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 1, max = 15, message = "If given, the display name must be between 1 and 15 characters long.")
    protected String displayName;

    /**
     * Storage location of user database
     * Might be NULL for non-user databases or if default location is used.
     */
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED) //for schema definition
    protected String location;

    /**
     * Indicates whether this database shall be used to use retention time information for library matching.
     * Typically used for in-house spectral libraries that have been measured on
     */
    @Schema(nullable = true, defaultValue = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected Boolean matchRtOfReferenceSpectra;
}
