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

package de.unijena.bioinf.ms.middleware.model.databases;

import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseSettings;

public class SearchableDatabases {
    public static SearchableDatabase of(CustomDataSources.Source source) {
        SearchableDatabase.SearchableDatabaseBuilder<?, ?> b = SearchableDatabase.builder()
                .databaseId(source.name())
                .customDb(source.isCustomSource())
                .displayName(source.displayName())
                .searchable(CustomDataSources.isSearchable(source));

        if (source.isCustomSource())
            b.location(((CustomDataSources.CustomSource) source).location());
        return b.build();
    }

    public static SearchableDatabase of(CustomDatabase customDb) {
        SearchableDatabase.SearchableDatabaseBuilder<?, ?> b = SearchableDatabase.builder()
                .databaseId(customDb.name())
                .customDb(true)
                .displayName(customDb.displayName())
                .location(customDb.storageLocation())
                .matchRtOfReferenceSpectra(customDb.getSettings().isMatchRtOfReferenceSpectra())
                .dbVersion(customDb.getDatabaseVersion())
                .updateNeeded(customDb.needsUpgrade())
                .searchable(CustomDataSources.isSearchable(customDb.name()));

        CustomDatabaseSettings.Statistics stats = customDb.getStatistics();
        if (stats != null)
            b.numberOfFormulas(stats.getFormulas())
                    .numberOfStructures(stats.getCompounds())
                    .numberOfReferenceSpectra(stats.getSpectra());
        return b.build();
    }
}
