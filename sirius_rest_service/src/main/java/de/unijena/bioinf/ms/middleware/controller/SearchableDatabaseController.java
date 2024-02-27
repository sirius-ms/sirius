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

import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabaseParameters;
import de.unijena.bioinf.ms.middleware.service.databases.ChemDbService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/databases")
@Tag(name = "Searchable Databases", description = "Manage structure and spectral databases that can be used by various computational methods.")
@Slf4j
public class SearchableDatabaseController {

    private final ChemDbService chemDbService;

    public SearchableDatabaseController(ChemDbService chemDbService) {
        this.chemDbService = chemDbService;
    }

    @GetMapping("")
    public Page<SearchableDatabase> getDatabases(@ParameterObject Pageable pageable, @RequestParam(defaultValue = "false") boolean includeStats) {
        return chemDbService.findAll(pageable, includeStats);
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

    @PostMapping("/{databaseId}/add")
    public SearchableDatabase addDatabase(@PathVariable String databaseId, @RequestParam String pathToProject) {
        return chemDbService.add(databaseId, pathToProject);
    }

    @DeleteMapping("/{databaseId}")
    public void removeDatabase(@PathVariable String databaseId, @RequestParam(defaultValue = "false") boolean delete){
        chemDbService.remove(databaseId, delete);
    }
}
