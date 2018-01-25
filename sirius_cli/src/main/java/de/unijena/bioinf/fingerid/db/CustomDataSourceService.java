/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.fingerid.db;

import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.DatasourceService.Sources;

import java.util.*;

/**
 * References to other databases can be stored as 32 or 64 bit sets. This class decodes these
 * bitsets into names.
 */
public class CustomDataSourceService {
    private static final Set<DataSourceChangeListener> listeners = new LinkedHashSet<>();
    private static final int lastEnumBit;
    private static final BitSet bits;
    public static final Map<String, Source> SOURCE_MAP;

    static {
        SOURCE_MAP = new LinkedHashMap<>(Sources.values().length * +5);
        long b = 0L;
        for (DatasourceService.Sources s : Sources.values()) {
            SOURCE_MAP.put(s.name, new EnumSource(s));
            b |= s.flag;
        }
        bits = BitSet.valueOf(new long[]{b});
        lastEnumBit = bits.cardinality();
    }

    public interface Source {
        long flag();

        String name();

        String sqlQuery();

        long searchFlag();

        String URI();

        boolean isCustomSource();

        String getLink(String id);
    }

    static class EnumSource implements Source {
        public final Sources source;

        public EnumSource(Sources source) {
            this.source = source;
        }

        @Override
        public long flag() {
            return source.flag;
        }

        @Override
        public String name() {
            return source.name;
        }

        @Override
        public String sqlQuery() {
            return source.sqlQuery;
        }

        @Override
        public long searchFlag() {
            return source.searchFlag;
        }

        @Override
        public String URI() {
            return source.URI;
        }

        @Override
        public boolean isCustomSource() {
            return false;
        }

        @Override
        public String getLink(String id) {
            return source.getLink(id);
        }
    }

    static class CustomSource implements Source {
        public final long flag;
        public final long searchFlag;
        public final String name;

        public CustomSource(long flag, long searchFlag, String name) {
            this.flag = flag;
            this.searchFlag = searchFlag;
            this.name = name;
        }

        public CustomSource(long flag, String name) {
            this(flag, flag, name);
        }

        @Override
        public long flag() {
            return flag;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String sqlQuery() {
            return null;
        }

        @Override
        public long searchFlag() {
            return searchFlag;
        }

        @Override
        public String URI() {
            return null;
        }

        @Override
        public boolean isCustomSource() {
            return true;
        }

        @Override
        public String getLink(String id) {
            return null;
        }
    }

    public static boolean removeCustomSource(String name) {
        Source s = getSourceFromName(name);
        if (s == null) return true;
        if (s.isCustomSource()) {
            s = SOURCE_MAP.remove(name);
            bits.andNot(BitSet.valueOf(new long[]{s.flag()}));
            notifyListeners(Collections.singleton(s.name()));
            return true;
        }
        return false;
    }

    public static int size() {
        return SOURCE_MAP.size();
    }

    public static Iterable<Source> sources() {
        return SOURCE_MAP.values();
    }

    public static Source addCustomSourceIfAbsent(String name) {
        Source s = getSourceFromName(name);
        if (s == null) {
            int bitIndex = bits.nextClearBit(lastEnumBit);
            bits.set(bitIndex);
            long flag = 1 << bitIndex;
            Source r = new CustomSource(flag, name);
            SOURCE_MAP.put(name, r);
            notifyListeners(Collections.singleton(r.name()));
            return r;
        }
        return null;
    }


    public static Set<String> getDataSourcesFromBitFlags(long flags) {
        final HashSet<String> set = new HashSet<>();
        return getDataSourcesFromBitFlags(set, flags);
    }

    public static Set<String> getDataSourcesFromBitFlags(Set<String> set, long flags) {
        for (Source s : SOURCE_MAP.values()) {
            if ((flags & s.flag()) == s.flag()) {
                set.add(s.name());
            }
        }
        return set;
    }

    public static Source getSourceFromName(String name) {
        return SOURCE_MAP.get(name);
    }


    public static void notifyListeners(Collection<String> changed) {
        for (DataSourceChangeListener listener : listeners) {
            listener.fireDataSourceChanged(changed);
        }
    }

    public static boolean removeListener(DataSourceChangeListener listener) {
        return listeners.remove(listener);
    }

    public static boolean addListener(DataSourceChangeListener listener) {
        return listeners.add(listener);
    }


    public interface DataSourceChangeListener extends EventListener {
        void fireDataSourceChanged(Collection<String> changed);
    }
}
