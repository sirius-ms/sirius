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

package de.unijena.bioinf.ms.middleware.service.databases;

import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabase;
import de.unijena.bioinf.ms.middleware.model.databases.SearchableDatabaseParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChemDbService {
    WebWithCustomDatabase db();

    SearchableDatabase findById(@NotNull String databaseId, boolean includeStats);

    Page<SearchableDatabase> findAll(Pageable pageable, boolean includeStats);

    List<SearchableDatabase> findAll(boolean includeStats);

    SearchableDatabase create(@NotNull String databaseId, @Nullable SearchableDatabaseParameters dbParameters);

    SearchableDatabase add(@NotNull String databaseId, @NotNull String location);

    void remove(String id, boolean delete);

    SearchableDatabase update(String databaseId, SearchableDatabaseParameters dbUpdate);
}
