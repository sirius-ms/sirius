/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.databases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchableDatabase extends SearchableDatabaseParameters {
    /**
     * A unique identifier or name of the database.
     * Should only contain file path and url save characters
     * For user databases this is usually the file name.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected String databaseId;

    /**
     * Indicates whether the database is a user database that is modifiable or if it is a
     * database that is included in SIRIUS which cannot be modified.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected Boolean userDb;

    /**
     * Date on which the data was imported / database was created.
     */
    @Schema(nullable = true)
    protected String importDate;

    /**
     * Number of unique compounds available in this database.
     */
    @Schema(nullable = true)
    protected Long numberOfStructures;

    /**
     * Number of different molecular formulas available in this database.
     */
    @Schema(nullable = true)
    protected Long numberOfFormulas;

    /**
     * Number of reference spectra available in this database
     */
    @Schema(nullable = true)
    protected Long numberOfReferenceSpectra;
}
