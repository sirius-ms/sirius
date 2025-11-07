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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.MultipartInputResource;
import de.unijena.bioinf.ms.middleware.model.databases.BioTransformerParameters;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabaseParameters;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.databases.ChemDbService;
import de.unijena.bioinf.ms.rest.client.databases.DownloadableDatabase;
import de.unijena.bioinf.webapi.WebAPI;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@RestController
@RequestMapping(value = "/api/databases")
@Tag(name = "Searchable Databases", description = "Manage structure and spectral databases that can be used by various computational methods.")
@Slf4j
public class SearchableDatabaseController {

    private final ChemDbService chemDbService;
    private final WebAPI<?> webAPI;

    public SearchableDatabaseController(ChemDbService chemDbService, WebAPI<?> webAPI) {
        this.chemDbService = chemDbService;
        this.webAPI = webAPI;
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> getDatabases(
            @RequestParam(defaultValue = "false") boolean includeStats,
            @RequestParam(defaultValue = "false") boolean includeWithErrors) {
        return chemDbService.findAll(includeStats, includeWithErrors);
    }

    @GetMapping(value = "/custom", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> getCustomDatabases(
            @RequestParam(defaultValue = "false") boolean includeStats,
            @RequestParam(defaultValue = "false") boolean includeWithErrors) {
        return getDatabases(includeStats, includeWithErrors).stream().filter(SearchableDatabase::isCustomDb).toList();
    }

    @GetMapping(value = "/included", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> getIncludedDatabases(@RequestParam(defaultValue = "false") boolean includeStats) {
        return getDatabases(includeStats, false).stream().filter(not(SearchableDatabase::isCustomDb)).toList();
    }


    @GetMapping(value = "/{databaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchableDatabase getDatabase(@PathVariable String databaseId, @RequestParam(defaultValue = "true") boolean includeStats) {
        return chemDbService.findById(databaseId, includeStats);
    }

    @PostMapping(value = "/{databaseId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchableDatabase createDatabase(@PathVariable @Pattern(regexp = "^[a-zA-Z0-9-_]+$") String databaseId, @Valid @RequestBody(required = false) SearchableDatabaseParameters dbToCreate) {
        return chemDbService.create(databaseId, dbToCreate);
    }

    @PutMapping(value = "/{databaseId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchableDatabase updateDatabase(@PathVariable String databaseId, @RequestBody(required = false) SearchableDatabaseParameters dbUpdate) {
        return chemDbService.update(databaseId, dbUpdate);
    }

    @Deprecated(forRemoval = true)
    @Operation(
            summary = "DEPRECATED: this endpoint is based on local file paths and will likely be replaced in future versions of this API."
    )
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> addDatabases(@RequestBody List<String> pathToDatabases) {
        return chemDbService.add(pathToDatabases);
    }

    @DeleteMapping(value = "/{databaseId}")
    public void removeDatabase(@PathVariable String databaseId, @RequestParam(defaultValue = "false") boolean delete) {
        chemDbService.remove(databaseId, delete);
    }

    /**
     * Start import of structure and spectra files into the specified database.
     *
     * @param databaseId database to import into
     * @param inputFiles files to be imported
     * @param bioTransformerParameters configuration for biotransformer execution. If null, BioTransformer is not applied.
     * @return Meta-Information of the affected database after the import has been performed.
     */
    @Operation(
            summary = "Start import of structure and spectra files into the specified database.",
            description = "Start import of structure and spectra files into the specified database.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = {
                                    @Encoding(name = "bioTransformerParameters", contentType = MediaType.APPLICATION_JSON_VALUE),
                                    @Encoding(name = "inputFiles", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
                            }
                    )
            )
    )
    @PostMapping(value = "/{databaseId}/import/from-files", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SearchableDatabase importIntoDatabase(@PathVariable String databaseId,
                                                 @RequestPart MultipartFile[] inputFiles,
                                                 @RequestPart(required = false) BioTransformerParameters bioTransformerParameters,
                                                 @RequestParam(defaultValue = "1000") int bufferSize
    ) {
        return chemDbService.importById(
                databaseId,
                Arrays.stream(inputFiles).map(MultipartInputResource::new).collect(Collectors.toList()),
                bioTransformerParameters,
                bufferSize
        );
    }

    /**
     * Get list of curated custom databases downloadable from the SIRIUS web service for local use.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     * [DEPRECATED] This endpoint will likely be removed or changed in future versions of this API.
     *
     * @return list of databases available for downloading.
     */
    @Deprecated(forRemoval = true)
    @GetMapping(value = "/downloadable", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public List<DownloadableDatabase> getDownloadableDatabases() {
        try {
            return webAPI.listDownloadableDatabases();
        } catch (Exception e) {
            log.error("Error getting downloadable databases", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting downloadable databases: " + e.getMessage(), e);
        }
    }

    //todo TBD whether we want to implement this endpoint
//    /**
//     * Start import of structure and spectra files into the specified database.
//     *
//     * @param databaseId database to import into
//     * @param inputFiles files to be imported
//     * @param optFields  set of optional fields to be included. Use 'none' only to override defaults.
//     * @return Job of the import command to be executed.
//     */
//    @PostMapping(value = "/{databaseId}/import/from-files-job", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public Job importIntoDatabaseAsJob(@PathVariable String databaseId,
//                                       @RequestPart MultipartFile[] inputFiles,
//                                       @RequestParam(defaultValue = "1000") int bufferSize,
//                                       @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
//    ) {
////        DatabaseImportSubmission dbImport = new DatabaseImportSubmission(databaseId, inputFiles, );
////        SearchableDatabase db = chemDbService.findById(dbImport.getDatabaseId(), false);
////        return computeService.createAndSubmitCommandJob(dbImport.asCommandSubmission(db.getLocation()), removeNone(optFields));
//        throw new UnsupportedOperationException("Async DB import not yet implemented");
//    }
}
