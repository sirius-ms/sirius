/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb.annotations;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SearchableDBAnnotation implements Ms2ExperimentAnnotation {
    public final static String NO_DB = "none";
    public final List<SearchableDatabase> searchDBs;
    private final long filter;
    private final boolean containsRestDb;
    private final boolean containsCustomDb;

    protected SearchableDBAnnotation(@Nullable Collection<SearchableDatabase> searchDBs) {
        this.searchDBs = searchDBs == null ? Collections.emptyList() : List.copyOf(searchDBs);
        filter = this.searchDBs.stream().mapToLong(SearchableDatabase::getFilterFlag).reduce((a, b) -> a | b).orElse(0);
        containsCustomDb = this.searchDBs.stream().anyMatch(SearchableDatabase::isCustomDb);
        containsRestDb = this.searchDBs.stream().anyMatch(SearchableDatabase::isRestDb);
    }

    @Override
    public String toString() {
        return searchDBs.stream().map(SearchableDatabase::name).collect(Collectors.joining(","));
    }

    public boolean containsRestDB() {
        return containsRestDb;
    }

    public boolean containsCustomDB() {
        return containsCustomDb;
    }

    public boolean containsDBs() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return searchDBs == null || searchDBs.isEmpty();
    }

    public long getDBFlag() {
        return filter;
    }

    public static List<SearchableDatabase> makeDB(@NotNull String names) {
        if (names.equalsIgnoreCase(DataSource.ALL.realName) || names.equalsIgnoreCase(DataSource.ALL.name()))
            return SearchableDatabases.getAllSelectableDbs();
        if (names.equalsIgnoreCase(DataSource.ALL_BUT_INSILICO.realName) || names.equalsIgnoreCase(DataSource.ALL_BUT_INSILICO.name()))
            return SearchableDatabases.getNonInSilicoSelectableDbs();

        return Arrays.stream(names.trim().split("\\s*,\\s*"))
                .map(SearchableDatabases::getDatabase).flatMap(Optional::stream).distinct().collect(Collectors.toList());
    }
}
