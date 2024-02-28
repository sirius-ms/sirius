/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.compute.DatabaseImportSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabaseParameters;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.databases.ChemDbService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.EnumSet;
import java.util.List;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;
import static java.util.function.Predicate.not;

@RestController
@RequestMapping(value = "/api/databases")
@Tag(name = "Searchable Databases", description = "Manage structure and spectral databases that can be used by various computational methods.")
@Slf4j
public class SearchableDatabaseController {

    private final ChemDbService chemDbService;
    private final ComputeService computeService;

    public SearchableDatabaseController(ChemDbService chemDbService, ComputeService computeService) {
        this.chemDbService = chemDbService;
        this.computeService = computeService;
    }

    @GetMapping("")
    public List<SearchableDatabase> getDatabases(@RequestParam(defaultValue = "false") boolean includeStats) {
        return chemDbService.findAll(includeStats);
    }

    @GetMapping("/custom")
    public List<SearchableDatabase> getCustomDatabases(@RequestParam(defaultValue = "false") boolean includeStats) {
        return getDatabases(includeStats).stream().filter(SearchableDatabase::isCustomDb).toList();
    }

    @GetMapping("/included")
    public List<SearchableDatabase> getIncludedDatabases(@RequestParam(defaultValue = "false") boolean includeStats) {
        return getDatabases(includeStats).stream().filter(not(SearchableDatabase::isCustomDb)).toList();
    }


    @GetMapping("/{databaseId}")
    public SearchableDatabase getDatabase(@PathVariable String databaseId, @RequestParam(defaultValue = "true") boolean includeStats) {
        return chemDbService.findById(databaseId, includeStats);
    }
    @PostMapping("/{databaseId}")
    public SearchableDatabase createDatabase(@PathVariable String databaseId, @RequestBody(required = false) SearchableDatabaseParameters dbToCreate) {
        return chemDbService.create(databaseId, dbToCreate);
    }

    @PutMapping("/{databaseId}")
    public SearchableDatabase updateDatabase(@PathVariable String databaseId, @RequestBody(required = false) SearchableDatabaseParameters dbUpdate) {
        return chemDbService.update(databaseId, dbUpdate);
    }

    @PostMapping("")
    public List<SearchableDatabase> addDatabases(@RequestBody List<String> pathToProjects) {
        return chemDbService.add(pathToProjects);
    }

    @DeleteMapping("/{databaseId}")
    public void removeDatabase(@PathVariable String databaseId, @RequestParam(defaultValue = "false") boolean delete){
        chemDbService.remove(databaseId, delete);
    }
}
