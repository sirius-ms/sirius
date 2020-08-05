

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

package de.unijena.bioinf.chemdb;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * References to other databases can be stored as 32 or 64 bit sets. This class decodes these
 * bitsets into names.
 */
public class DataSources {
    //static fields
    protected static Pattern NUMPAT = Pattern.compile("%(?:[0-9 ,+\\-]*)d");
    private static final Map<String, DataSource> SOURCES_ALIAS_MAP = new ConcurrentHashMap<>();

    static {
        for (DataSource value : DataSource.values()) {
            SOURCES_ALIAS_MAP.put(value.realName.toLowerCase(), value);
            SOURCES_ALIAS_MAP.put(value.name().toLowerCase(), value);
        }
    }

    public static Map<String, DataSource> getSourcesAliasMap() {
        return Collections.unmodifiableMap(SOURCES_ALIAS_MAP);
    }
    public static Iterable<String> getAliasNames(){
        return Collections.unmodifiableSet(SOURCES_ALIAS_MAP.keySet());
    }

    @NotNull
    public static Optional<DataSource> getSourceFromName(@NotNull final String name) {
        return Optional.ofNullable(getSourceFromNameOrNull(name));
    }


    @Nullable
    public static DataSource getSourceFromNameOrNull(@NotNull final String name) {
        return SOURCES_ALIAS_MAP.get(name.toLowerCase());
    }

    public static String getRealSourceName(@NotNull final String name) {
        final DataSource source = getSourceFromNameOrNull(name);
        if (source == null) return null;
        return source.realName;
    }

    public static boolean containsSource(@NotNull final String name) {
        return getSourceFromNameOrNull(name) != null;
    }

    public static long getDBFlag(@NotNull String dbName) {
        return DataSources.getSourceFromName(dbName).map(DataSource::flag).orElse(0L);
    }

    public static long getDBFlag(@NotNull Set<DataSource> sources) {
        return sources.stream().mapToLong(DataSource::flag).reduce((a, b) -> a |= b).orElse(0L);
    }

    public static Multimap<String, String> getLinkedDataSources(CompoundCandidate candidate) {
        Set<String> names = getDataSourcesFromBitFlags(candidate.getBitset());
        Multimap<String, String> databases = ArrayListMultimap.create(names.size(), 1);
        if (candidate.getLinks() != null) {
            for (DBLink link : candidate.getLinks()) {
                databases.put(link.name, link.id);
            }
        }

        for (String aname : names)
            if (!databases.containsKey(aname))
                databases.put(aname, null);

        return databases;
    }

    public static Set<String> getDataSourcesFromBitFlags(long flags) {
        final HashSet<String> set = new HashSet<>();
        return getDataSourcesFromBitFlags(set, flags);
    }

    public static Set<String> getDataSourcesFromBitFlags(Set<String> set, long flags) {
        for (DataSource s : DataSource.valuesNoALL()) {
            if ((flags & s.flag) == s.flag) {
                set.add(s.realName);
            }
        }
        return set;
    }


    public static long bioOrAll(boolean searchInBio) {
        return searchInBio ? DataSource.BIO.flag() : DataSource.ALL_BUT_INSILICO.flag();
    }
}
