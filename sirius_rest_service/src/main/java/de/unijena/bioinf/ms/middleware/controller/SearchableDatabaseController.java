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

import de.unijena.bioinf.ms.middleware.configuration.ApiError;
import de.unijena.bioinf.ms.middleware.configuration.NoApiError;
import de.unijena.bioinf.ms.middleware.model.MultipartInputResource;
import de.unijena.bioinf.ms.middleware.model.databases.BioTransformerParameters;
import de.unijena.bioinf.ms.middleware.model.databases.DatabaseStructure;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabaseParameters;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * List all searchable databases, both the ones included in SIRIUS and the custom ones added by the user.
     * <p>
     * A searchable database provides structures and reference spectra (optional), and can be selected as a search
     * parameter for structure database search and spectral library search. Note that every imported spectral
     * library also acts as a structure database.
     *
     * @param includeStats       if true, the number of structures, formulas and reference spectra is included
     *                           per database. Computing these counts touches the database files, so requesting
     *                           them is noticeably slower than a plain listing.
     * @param includeWithErrors  if true, databases that could not be loaded are listed as well, carrying the
     *                           reason in their errorMessage field. Use this to show a broken database to the
     *                           user instead of silently hiding it.
     * @return all databases known to this SIRIUS instance.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> getDatabases(
            @RequestParam(defaultValue = "false") boolean includeStats,
            @RequestParam(defaultValue = "false") boolean includeWithErrors) {
        return chemDbService.findAll(includeStats, includeWithErrors);
    }

    /**
     * List only the custom databases, that is the structure databases and spectral libraries the user has
     * created or added. These are the databases that can be modified and imported into.
     *
     * @param includeStats      if true, the number of structures, formulas and reference spectra is included
     *                          per database. Slower, since the database files have to be read.
     * @param includeWithErrors if true, databases that could not be loaded are listed as well, carrying the
     *                          reason in their errorMessage field.
     * @return all custom databases known to this SIRIUS instance.
     */
    @GetMapping(value = "/custom", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> getCustomDatabases(
            @RequestParam(defaultValue = "false") boolean includeStats,
            @RequestParam(defaultValue = "false") boolean includeWithErrors) {
        return getDatabases(includeStats, includeWithErrors).stream().filter(SearchableDatabase::isCustomDb).toList();
    }

    /**
     * List only the databases that ship with SIRIUS, such as PubChem and the bio databases. These are
     * read-only: they cannot be imported into, modified or removed.
     *
     * @param includeStats if true, the number of structures, formulas and reference spectra is included per
     *                     database. Slower, since the database files have to be read.
     * @return all databases included in SIRIUS.
     */
    @GetMapping(value = "/included", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> getIncludedDatabases(@RequestParam(defaultValue = "false") boolean includeStats) {
        return getDatabases(includeStats, false).stream().filter(not(SearchableDatabase::isCustomDb)).toList();
    }


    /**
     * Get a single searchable database by its id.
     *
     * @param databaseId   id of the database to retrieve, as reported by the listing endpoints.
     * @param includeStats if true (the default here), the number of structures, formulas and reference spectra
     *                     is included.
     * @return the requested database.
     */
    @ApiError(status = 404, value = "No database with the given id exists.")
    @GetMapping(value = "/{databaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchableDatabase getDatabase(@PathVariable String databaseId, @RequestParam(defaultValue = "true") boolean includeStats) {
        return chemDbService.findById(databaseId, includeStats);
    }

    /**
     * Create a new, empty custom database.
     * <p>
     * The new database is created on disk and registered with this SIRIUS instance, so it can immediately be
     * used as a search parameter and imported into via the import endpoint. It contains no structures and no
     * reference spectra until something is imported.
     *
     * @param databaseId  id of the new database. Must be URL-safe, that is letters, digits, '-' and '_' only,
     *                    and must not be in use by another database.
     * @param dbToCreate  optional settings for the new database. If omitted, the database is created in the
     *                    default custom database directory with default settings. Supply a location to place
     *                    the database file elsewhere, a displayName for the user interface, and
     *                    matchRtOfReferenceSpectra for in-house libraries whose retention times are comparable
     *                    to the measured samples.
     * @return the created database.
     */
    @ApiError(status = 400, value = "The database id is not a valid database name. It must consist of letters, digits, '-' and '_' only.")
    @ApiError(status = 409, value = "A database with this id already exists, or a file already exists at the target location.")
    @NoApiError(404) // the path variable is the id of the database to be created, so there is nothing to not find
    @PostMapping(value = "/{databaseId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchableDatabase createDatabase(@PathVariable @Pattern(regexp = "^[a-zA-Z0-9-_]+$") String databaseId, @Valid @RequestBody(required = false) SearchableDatabaseParameters dbToCreate) {
        return chemDbService.create(databaseId, dbToCreate);
    }

    /**
     * Change the settings of an existing custom database.
     * <p>
     * NOT IMPLEMENTED YET: changing the display name and the retention time matching flag of an existing database
     * is not supported so far, and every request currently fails. The request and response shape is settled
     * though, so a client can be written against this endpoint today: it will start succeeding in a future
     * version without any change on the client side.
     * <p>
     * Until then, create a new database with the desired settings and import into it.
     *
     * @param databaseId id of the database to update.
     * @param dbUpdate   the settings to apply.
     * @return the updated database.
     */
    @ApiError(status = 500, value = "Currently always, since updating custom databases is not implemented yet. This will become a normal server-side error once the endpoint is implemented.")
    @NoApiError(404) // fails before anything is looked up
    @PutMapping(value = "/{databaseId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchableDatabase updateDatabase(@PathVariable String databaseId, @RequestBody(required = false) SearchableDatabaseParameters dbUpdate) {
        return chemDbService.update(databaseId, dbUpdate);
    }

    /**
     * Register existing custom database files with this SIRIUS instance, so that they become searchable.
     * <p>
     * Use this to make databases that already exist on disk available again, for example after reinstalling
     * SIRIUS or when sharing a database file with a colleague. The files are opened in place, not copied.
     *
     * @param pathToDatabases local file paths of the database files (.siriusdb) to register. Each must exist,
     *                        must not already be registered, and its name must not collide with an existing
     *                        database.
     * @return the databases that were successfully registered. Files that exist but could not be opened are
     *         skipped and are absent from the result.
     */
    @Deprecated(forRemoval = true)
    @Operation(
            summary = "[DEPRECATED] This endpoint is based on local file paths and will likely be replaced in future versions of this API."
    )
    @ApiError(status = 400, value = "A path does not exist or is not a file, is already registered, or its database name is already in use. No database is registered in that case.")
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<SearchableDatabase> addDatabases(@RequestBody List<String> pathToDatabases) {
        return chemDbService.add(pathToDatabases);
    }

    /**
     * Remove a custom database from this SIRIUS instance, and optionally delete it from disk.
     * <p>
     * This is idempotent: removing a database that is not registered succeeds and does nothing, so a client
     * does not have to check first.
     *
     * @param databaseId id of the database to remove.
     * @param delete     if true, the database file is deleted from disk and the data is lost. If false (the
     *                   default), only the registration is removed and the file is kept, so the database can
     *                   be registered again later.
     */
    @NoApiError(404) // removal is idempotent: removing an unknown database succeeds and does nothing
    @DeleteMapping(value = "/{databaseId}")
    public void removeDatabase(@PathVariable String databaseId, @RequestParam(defaultValue = "false") boolean delete) {
        chemDbService.remove(databaseId, delete);
    }

    /**
     * Import structures and reference spectra into an existing custom database.
     * <p>
     * Structures can be imported from tab- or comma-separated files with a SMILES column and optional id and
     * name columns, and from directories or .zip archives of SDF files. Reference spectra and their structures can be imported from
     * .ms, .mgf, .msp, .mat, .txt (MassBank), .mb and .json (GNPS, MoNA); they must be centroided and annotated
     * with a structure. Any imported spectral library also acts as a structure database.
     * <p>
     * SIRIUS computes a molecular fingerprint for every imported structure. Fingerprints of structures already
     * known to SIRIUS are downloaded, all others are computed locally, which can take considerable time for
     * large imports. The request blocks until the import has finished.
     * <p>
     * For best results, standardize SMILES with the PubChem standardization before importing, since the
     * machine learning methods in SIRIUS are trained on PubChem standardized structures.
     *
     * @param databaseId               id of the custom database to import into. Must exist.
     * @param inputFiles               structure and/or spectra files to import.
     * @param bioTransformerParameters if given, BioTransformer is applied to all input structures and the
     *                                 resulting transformation products are imported alongside them. If null,
     *                                 no biotransformation is performed.
     * @param bufferSize               number of compounds to keep in memory before writing them to the
     *                                 database. Raise it to speed up large imports on machines with enough RAM.
     * @return the affected database, including its updated statistics.
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
    @ApiError(status = 404, value = "No database with the given id exists.")
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
     * [DEPRECATED] This endpoint will likely be removed or changed in future versions of this API.
     * [INTERNAL] This endpoint is for internal use and not intended to become part of the stable API specification at any time. This endpoint can change (or be removed) at any time, even in minor updates.
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

    /**
     * [EXPERIMENTAL] Page through the structures contained in a custom database.
     * <p>
     * Returns the stored structures with their name, SMILES, InChI, InChI key, molecular formula and mass.
     * Only custom databases are supported; the databases included in SIRIUS cannot be enumerated this way.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint
     * can change at any time, even in minor updates.
     *
     * @param databaseId id of the custom database to read from.
     * @param pageable   paging and sorting. Request it unpaged to retrieve the whole database at once, which
     *                   for a large database means a correspondingly large response.
     * @return a page of the structures in the database.
     */
    @ApiError(status = 404, value = "No custom database with the given id exists. Databases included in SIRIUS cannot be enumerated.")
    @GetMapping(value = "{databaseId}/structures", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<DatabaseStructure> getStructures(@PathVariable String databaseId, @ParameterObject Pageable pageable) {
        return chemDbService.findAllStructures(databaseId, pageable);
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
