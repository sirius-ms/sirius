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
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SearchableDBAnnotation implements Ms2ExperimentAnnotation {
    public final static String NO_DB = "none";
    public final List<CustomDataSources.Source> searchDBs;
    private final long filter;

    protected SearchableDBAnnotation(@Nullable Collection<CustomDataSources.Source> searchDBs) {
        this.searchDBs = searchDBs == null ? Collections.emptyList() : List.copyOf(searchDBs);
        filter = this.searchDBs.stream().mapToLong(CustomDataSources.Source::flag).reduce((a, b) -> a | b).orElse(0);
    }

    @Override
    public String toString() {
        return searchDBs.stream().map(CustomDataSources.Source::name).collect(Collectors.joining(","));
    }

    public boolean isEmpty() {
        return searchDBs == null || searchDBs.isEmpty();
    }

    public long getDBFlag() {
        return filter;
    }

    public static List<CustomDataSources.Source> makeDB(@NotNull String names) {
        if (names.equalsIgnoreCase(DataSource.ALL.name()) || names.equalsIgnoreCase(DataSource.ALL.realName()))
            return CustomDataSources.getAllSelectableDbs();

        return Arrays.stream(names.trim().split("\\s*,\\s*"))
                .map(CustomDataSources::getSourceFromName).filter(Objects::nonNull).distinct().toList();
    }
}
