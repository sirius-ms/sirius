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

import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabaseParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public interface ChemDbService {
    WebWithCustomDatabase db();

    SearchableDatabase importById(@NotNull String databaseId, List<InputResource<?>> inputResources, int bufferSize);

    SearchableDatabase findById(@NotNull String databaseId, boolean includeStats);

    Page<SearchableDatabase> findAll(Pageable pageable, boolean includeStats);

    List<SearchableDatabase> findAll(boolean includeStats);

    SearchableDatabase create(@NotNull String databaseId, @Nullable SearchableDatabaseParameters dbParameters);


    void remove(String id, boolean delete);

    SearchableDatabase update(@NotNull String databaseId, @NotNull SearchableDatabaseParameters dbUpdate);

    default SearchableDatabase add(@NotNull String location){
        return add(List.of(location)).stream().findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find database at location: " + location));
    }
    List<SearchableDatabase> add(List<String> pathToDatabases);
}
