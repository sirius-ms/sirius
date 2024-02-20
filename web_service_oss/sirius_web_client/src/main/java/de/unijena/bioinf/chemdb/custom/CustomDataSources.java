

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

package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * References to other databases can be stored as 32 or 64 bit sets. This class decodes these
 * bitsets into names.
 */
public class CustomDataSources {
    private static final Set<DataSourceChangeListener> listeners = new LinkedHashSet<>();
    private static final int lastEnumBit;
    private static final BitSet bits;
    private static final Map<String, Source> SOURCE_MAP;

    public final static Set<String> NON_SEARCHABLE_LIST = Set.of(DataSource.TRAIN.realName, DataSource.LIPID.realName(), DataSource.ALL.realName, DataSource.ALL_BUT_INSILICO.realName,
            DataSource.PUBCHEMANNOTATIONBIO.realName, DataSource.PUBCHEMANNOTATIONDRUG.realName, DataSource.PUBCHEMANNOTATIONFOOD.realName, DataSource.PUBCHEMANNOTATIONSAFETYANDTOXIC.realName,
            DataSource.SUPERNATURAL.realName
    );

    public static final String WEB_CACHE_DIR = "web-cache"; //cache directory for all remote (web) dbs
    public static final String CUSTOM_DB_DIR = "custom";
    public static final String PROP_KEY = "de.unijena.bioinf.chemdb.custom.source";

    @NotNull
    public static Path getCustomDatabaseDirectory() {
        return getDatabaseDirectory().resolve(CUSTOM_DB_DIR);
    }

    @NotNull
    public static Path getWebDatabaseCacheDirectory() {
        return getDatabaseDirectory().resolve(WEB_CACHE_DIR);
    }

    public static FileBlobStorage getWebDatabaseCacheStorage() {
        try {
            Files.createDirectories(getWebDatabaseCacheDirectory());
            return new FileBlobStorage(getWebDatabaseCacheDirectory());
        } catch (IOException e) {
            throw new RuntimeException("Could not create cache directories!", e);
        }
    }

    public static Path getDatabaseDirectory() {
        final String val = PropertyManager.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        Path p;
        if (val == null || val.isBlank()){
            p =  Path.of(System.getProperty("java.io.tmpdir")).resolve("csi_cache_dir");
            LoggerFactory.getLogger(CustomDataSources.class).warn("No structure db cache found. Using fallback: " + p);
        }else {
            p = Paths.get(val);
        }
        return p;
    }


    static {
        SOURCE_MAP = new LinkedHashMap<>(DataSource.values().length * +5);
        long b = 0L;
        for (DataSource s : DataSource.values()) {
            SOURCE_MAP.put(s.realName, new EnumSource(s));
            b |= s.flag;
        }
        bits = BitSet.valueOf(new long[]{b});
        lastEnumBit = bits.cardinality();
    }

    static boolean removeCustomSource(String name) {
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

    static Source addCustomSourceIfAbsent(String name, String bucketLocation) {
        Source s = getSourceFromName(name);
        if (s == null) {
            int bitIndex = bits.nextClearBit(lastEnumBit);
            bits.set(bitIndex);
            long flag = 1L << bitIndex;
            Source r = new CustomSource(flag, name, bucketLocation);
            SOURCE_MAP.put(name, r);
            notifyListeners(Collections.singleton(r.name()));
            return r;
        }
        return null;
    }

    public static int size() {
        return SOURCE_MAP.size();
    }

    public static Stream<Source> sourcesStream() {
        return SOURCE_MAP.values().stream();
    }

    public static Iterable<Source> sources() {
        return SOURCE_MAP.values();
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

    public static boolean containsDB(String name){
        return SOURCE_MAP.containsKey(name);
    }

    public static long removeCustomSourceFromFlag(long flagToChange){
        return flagToChange & getNonCustomSourceFlags();
    }

    public static long getNonCustomSourceFlags() {
        return SOURCE_MAP.values().stream().filter(s -> !s.isCustomSource()).mapToLong(Source::flag).reduce((a, b) -> a | b).orElse(0);
    }

    public static long getCustomSourceFlags() {
        return SOURCE_MAP.values().stream().filter(Source::isCustomSource).mapToLong(Source::flag).reduce((a, b) -> a | b).orElse(0);
    }

    public static List<Source> getSources() {
        return sourcesStream().collect(Collectors.toList());
    }


    public static List<EnumSource> getNonCustomSources() {
        return sourcesStream().filter(s -> !s.isCustomSource()).map(s -> (EnumSource) s).collect(Collectors.toList());
    }

    public static List<CustomSource> getCustomSources() {
        return sourcesStream().filter(Source::isCustomSource).map(s -> (CustomSource) s).collect(Collectors.toList());
    }


    @Nullable
    public static Source getSourceFromName(String name) {
        return SOURCE_MAP.get(name);
    }

    @NotNull
    public static Optional<Source> getSourceFromNameOpt(String name) {
        return Optional.ofNullable(getSourceFromName(name));
    }

    public static List<Source> getSourcesFromNames(String... names) {
        return getSourcesFromNames(Arrays.asList(names));
    }

    public static List<Source> getSourcesFromNames(Collection<String> names) {
        return getSourcesFromNamesStrm(names).collect(Collectors.toList());
    }

    protected static Stream<Source> getSourcesFromNamesStrm(Collection<String> names) {
        return names.stream().map(CustomDataSources::getSourceFromName).filter(Objects::nonNull);
    }


    public static long getFlagFromName(String name) {
        return getSourceFromName(name).flag();
    }

    public static long getDBFlagsFromNames(Collection<String> names) {
        return getSourcesFromNamesStrm(names).mapToLong(Source::flag).reduce((a, b) -> a | b).orElse(0);
    }

    public static List<Source> getAllSelectableDbs() {
        return sourcesStream().filter(db -> !NON_SEARCHABLE_LIST.contains(db.name()))
                .collect(Collectors.toList());
    }

    public static List<Source> getNonInSilicoSelectableDbs() {
        return Arrays.stream(DataSource.valuesNoALLNoMINES())
                .map(DataSource::realName)
                .filter(s -> !NON_SEARCHABLE_LIST.contains(s))
                .map(SOURCE_MAP::get)
                .toList();
    }

    // listener stuff
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






    // classes
    public interface Source {
        long flag();

        String id();

        String name();

        long searchFlag();

        String URI();

        boolean isCustomSource();
        default boolean noCustomSource(){
            return !isCustomSource();
        };

        String getLink(String id);

    }

    public static class EnumSource implements Source {
        public final DataSource source;

        EnumSource(DataSource source) {
            this.source = source;
        }

        public DataSource source() {
            return source;
        }

        @Override
        public long flag() {
            return source.flag;
        }

        @Override
        public String id() {
            return source.name();
        }

        @Override
        public String name() {
            return source.realName;
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

        @Override
        public String toString() {
            return name();
        }
    }

    public static class CustomSource implements Source {
        public final long flag;
        public final long searchFlag;
        public final String name;
        public final String location;

        CustomSource(long flag, long searchFlag, String name, String bucketPath) {
            this.flag = flag;
            this.searchFlag = searchFlag;
            this.name = name;
            this.location = bucketPath;
        }

        public CustomSource(long flag, String name, String bucketPath) {
            this(flag, flag, name, bucketPath);
        }

        @Override
        public long flag() {
            return flag;
        }

        @Override
        public String id() {
            return location;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long searchFlag() {
            return searchFlag;
        }

        //this is for web links
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

        @Override
        public String toString() {
            return name();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomSource that)) return false;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
