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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DBVersion {
    private final String customDbSchemeVersion;
    private final String chemDbDate;

    public DBVersion(@Nullable String customDbSchemeVersion, @Nullable String chemDbDate) {
        this.customDbSchemeVersion = customDbSchemeVersion;
        this.chemDbDate = chemDbDate;
    }

    public String getCustomDbSchemeVersion() {
        return customDbSchemeVersion;
    }

    public String getChemDbDate() {
        return chemDbDate;
    }

    public boolean isChemDbValid(String chemDbDate) {
        return this.chemDbDate != null && this.chemDbDate.equals(chemDbDate);
    }

    public boolean isChemDbValid(@NotNull DBVersion other) {
        return isChemDbValid(other.getChemDbDate());
    }

    public boolean isCustomDbSchemeValid(String customDbSchemeVersion) {
        return this.customDbSchemeVersion != null && this.customDbSchemeVersion.equals(customDbSchemeVersion);

    }

    public boolean isCustomDbSchemeValid(@NotNull DBVersion other) {
        return isCustomDbSchemeValid(other.getCustomDbSchemeVersion());
    }

    @NotNull
    public static DBVersion newLocalVersion(Path dbCache) {
        final Path f = dbCache.resolve("version");
        String dbDate = null;
        if (Files.exists(f)) {
            try {
                final List<String> content = Files.readAllLines(f, StandardCharsets.UTF_8);
                if (!content.isEmpty())
                    dbDate = content.get(0);
            } catch (IOException e) {
                LoggerFactory.getLogger(DBVersion.class).error("Error when reading chemDB version file.", e);
            }
        }

        if (dbDate == null || dbDate.isBlank())
            LoggerFactory.getLogger(DBVersion.class).warn("DBVersion of file '" + f.toAbsolutePath() + "' is empty!");

        return new DBVersion(PropertyManager.getProperty("de.unijena.bioinf.fingerid.customdb.version"), dbDate);
    }
}
