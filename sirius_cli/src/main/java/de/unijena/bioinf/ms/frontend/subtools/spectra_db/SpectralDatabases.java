/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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

package de.unijena.bioinf.ms.frontend.subtools.spectra_db;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDBs;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SpectralDatabases {

    private final static String PROP_KEY = "de.unijena.bioinf.chemdb.spectraldb.source";

    private final static Map<Path, SpectralLibrary> openLibraries = new HashMap<>();

    public static SpectralLibrary createAndAddSpectralLibrary(@NotNull Path path) throws IOException {
        if (!openLibraries.containsKey(path)) {
            openLibraries.put(path, SpectralNoSQLDBs.getLocalSpectralLibrary(path));
        }
        writeDBProperties();
        return openLibraries.get(path);
    }

    public static Optional<SpectralLibrary> getSpectralLibrary(@NotNull Path path) throws IOException {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            if (!openLibraries.containsKey(path)) {
                openLibraries.put(path, SpectralNoSQLDBs.getLocalSpectralLibrary(path));
            }
            return Optional.of(openLibraries.get(path));
        } else {
            return Optional.empty();
        }
    }

    public static void addDBToProperties(@NotNull SpectralLibrary db) throws IOException {
        if (!openLibraries.containsKey(Path.of(db.location()))) {
            throw new IOException("Database " + db.location() + " is not opened.");
        }

    }

    public static void removeDB(@NotNull SpectralLibrary db) throws IOException {
        Path location = Path.of(db.location());
        if (!openLibraries.containsKey(location)) {
            throw new IOException("Database " + db.location() + " is not opened.");
        }
        if (db instanceof SpectralNoSQLDatabase<?>) {
            ((SpectralNoSQLDatabase<?>) db).close();
        }
        openLibraries.remove(location);
        writeDBProperties();
    }

    private static void writeDBProperties() {
        SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(
                PROP_KEY,
                openLibraries.values().stream().map(SpectralLibrary::location).collect(Collectors.joining(","))
        );
    }

    public static Collection<SpectralLibrary> listSpectralLibraries() {
        String locations = PropertyManager.getProperty(PROP_KEY);
        if (locations != null && !locations.isBlank()) {
            String[] split = locations.split("\\s*,\\s*");

            for (String location : split) {
                try {
                    getSpectralLibrary(Path.of(location));
                } catch (IOException e) {
                    LoggerFactory.getLogger(SpectralDBOptions.class).error(e.getMessage(), e);
                }
            }
        }

        return openLibraries.values();
    }

}
