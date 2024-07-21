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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
     * Indicates whether the database is a user managed custom database or if it is a
     * database that is included in SIRIUS which cannot be modified.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected boolean customDb;

    /**
     * True when this database can be used as a search parameter.
     * False if the database is just an additional filter that can be applied after search.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected boolean searchable;

    /**
     * Date on which the data was imported / database was created.
     */
    @Schema(nullable = true)
    protected String dbDate;

    /**
     * database schema version
     */
    @Schema(nullable = true)
    protected Integer dbVersion;
    /**
     * If true the database version is outdated and the database needs to be updated or re-imported before it can be used.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected boolean updateNeeded;

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
