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

package de.unijena.bioinf.ms.middleware.service.databases;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.custom.*;
import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabaseParameters;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabases;
import de.unijena.bioinf.webapi.WebAPI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions.writeDBProperties;

@Slf4j
public class ChemDbServiceImpl implements ChemDbService {
    private final WebAPI<?> webAPI;
    private final IFingerprinterCache iFPCache;
    private CdkFingerprintVersion version;


    public ChemDbServiceImpl(@NotNull WebAPI<?> webAPI, @Nullable IFingerprinterCache iFPCache) {
        this.webAPI = webAPI;
        this.iFPCache = iFPCache;
        log.info("Scanning for custom databases...");
        try {
            //request fingerprint version to init db and check compatibility
            final CdkFingerprintVersion version = version();
            //loads all current available dbs
            @NotNull List<CustomDatabase> dbs = CustomDatabases.getCustomDatabases(version);
            log.info("...found: " + dbs.stream().map(CustomDatabase::name).collect(Collectors.joining(", ")));
        } catch (Exception e) {
            log.error("Error when loading Custom databases", e);
        }
    }

    @SneakyThrows
    private synchronized CdkFingerprintVersion version() {
        if (version == null)
            version = webAPI.getCDKChemDBFingerprintVersion();
        return version;
    }

    @SneakyThrows
    @Override
    public synchronized WebWithCustomDatabase db() {
        return webAPI.getChemDB();
    }

    @Override
    public SearchableDatabase importById(@NotNull String databaseId, List<InputResource<?>> inputResources, int bufferSize) {
        CustomDatabase db = CustomDatabases.getCustomDatabaseByName(databaseId, version())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Database with id '" + databaseId + "' does not exist."));

        Map<Boolean, List<InputResource<?>>> split = inputResources.stream()
                .collect(Collectors.partitioningBy(p -> MsExperimentParser.isSupportedFileName(p.getFilename())));

        SiriusJobs.runInBackground(CustomDatabaseImporter.makeImportToDatabaseJob(
                split.get(true), split.get(false), null, (NoSQLCustomDatabase<?, ?>) db, webAPI, iFPCache, bufferSize))
                .takeResult();

        return SearchableDatabases.of(db);

    }

    @Override
    public SearchableDatabase findById(@NotNull String databaseId, boolean includeStats) {
        return CustomDataSources.getSourceFromNameOpt(databaseId).map(s -> s.isCustomSource() && includeStats
                        ? CustomDatabases.getCustomDatabaseByName(s.name(), version())
                        .map(SearchableDatabases::of)
                        .orElse(SearchableDatabases.of(s))
                        : SearchableDatabases.of(s))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Database with id '" + databaseId + "' does not exist."));
    }

    @Override
    public Page<SearchableDatabase> findAll(Pageable pageable, boolean includeStats) {
        return new PageImpl<>(CustomDataSources.sourcesStream().skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(s -> {
                    if (s.isCustomSource() && includeStats)
                        return CustomDatabases.getCustomDatabaseByName(s.name(), version())
                                .map(SearchableDatabases::of).orElse(SearchableDatabases.of(s));
                    return SearchableDatabases.of(s);
                }).toList(), pageable, CustomDataSources.size());
    }

    @Override
    public List<SearchableDatabase> findAll(boolean includeStats) {
        return CustomDataSources.sourcesStream().map(s -> {
            if (s.isCustomSource() && includeStats)
                return CustomDatabases.getCustomDatabaseByName(s.name(), version())
                        .map(SearchableDatabases::of).orElse(SearchableDatabases.of(s));
            return SearchableDatabases.of(s);
        }).toList();
    }

    @Override
    public SearchableDatabase create(@NotNull String databaseId, @Nullable SearchableDatabaseParameters dbParameters) {
        Path location = Optional.ofNullable(dbParameters)
                .map(SearchableDatabaseParameters::getLocation)
                .map(Path::of).orElse(CustomDataSources.getCustomDatabaseDirectory());
        try {
            if (Files.isDirectory(location))
                location = location.resolve(databaseId + CustomDatabases.CUSTOM_DB_SUFFIX);
            else if (!location.getFileName().toString().endsWith(CustomDatabases.CUSTOM_DB_SUFFIX))
                location = location.getParent().resolve(location.getFileName() + CustomDatabases.CUSTOM_DB_SUFFIX); //add correct file extension to db.

            CustomDatabaseSettings.CustomDatabaseSettingsBuilder configBuilder = CustomDatabaseSettings.builder()
                    .name(databaseId)
                    .usedFingerprints(List.of(version().getUsedFingerprints()))
                    .schemaVersion(CustomDatabase.CUSTOM_DATABASE_SCHEMA)
                    .statistics(new CustomDatabaseSettings.Statistics());

            if (dbParameters != null) {
                configBuilder.displayName(dbParameters.getDisplayName())
                        .matchRtOfReferenceSpectra(Optional.ofNullable(dbParameters.getMatchRtOfReferenceSpectra())
                                .orElse(false));
            }

            CustomDatabase newDb = CustomDatabases.create(location.toAbsolutePath().toString(), configBuilder.build(), version());
            CustomDBOptions.writeDBProperties();
            return SearchableDatabases.of(newDb);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when creating user database at: " + location, e);
        }
    }

    @Override
    public List<SearchableDatabase> add(List<String> pathToDatabases) {
        List<File> psFiles = pathToDatabases.stream().distinct().map(File::new).distinct().toList();
        {
            String existsError = psFiles.stream().filter(f -> !f.isFile() || !f.exists()).map(File::getAbsolutePath)
                    .collect(Collectors.joining("', '"));
            if (!existsError.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Following locations do not exist or are no valid files: '" + existsError +
                                "'. Locations to open must exist and be valid database files.");
        }

        {
            String locationError = CustomDataSources.customSourcesStream()
                    .map(CustomDataSources.CustomSource::location)
                    .filter(f -> psFiles.stream().map(File::getAbsolutePath).anyMatch(f2 -> f2.equals(f)))
                    .collect(Collectors.joining("', '"));
            if (!locationError.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The databases at '" + locationError + "' are already available in SIRIUS.");
        }

        {
            // in principle, it is still possible to insert to db files with the same dbId/name at the same time,
            // and they would override each other. This means only the first one gets imported.
            // However, we can treat this as expected behaviour
            String idError = psFiles.stream().map(File::getName)
                    .filter(n -> CustomDataSources.getSourceFromNameOpt(n).isPresent())
                    .collect(Collectors.joining("', '"));
            if (!idError.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Databases with the names '" + idError + "' already exist.");
        }

        List<SearchableDatabase> dbs = pathToDatabases.stream().map(location -> {
            try {
                CustomDatabase newDb = CustomDatabases.open(location, true, version());
                return SearchableDatabases.of(newDb);
            } catch (IOException e) {
                log.error("Error when opening user database from: " + location, e);
                return null;
            }
        }).filter(Objects::nonNull).toList();

        writeDBProperties();
        return dbs;
    }

    @Override
    public void remove(String databaseId, boolean delete) {
        try {
            CustomDatabases.getCustomDatabaseByName(databaseId, version()).ifPresent(db -> {
                CustomDatabases.remove(db, delete);
            });
        } catch (Exception e) {
            log.error("Error when removing custom database: {}", databaseId, e);
            CustomDatabases.remove(databaseId);
        }finally {
            writeDBProperties();
        }
    }

    @Override
    public SearchableDatabase update(@NotNull String databaseId, @NotNull SearchableDatabaseParameters dbUpdate) {
        //TODO nightsky: implement modification of Displayname and RT matching.
        throw new UnsupportedOperationException("Updating Custom databases is not yest supported");
    }
}
