package de.unijena.bioinf.chemdb.annotations;

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
        filter = this.searchDBs.stream().mapToLong(SearchableDatabase::getFilterFlag).reduce((a, b) -> a |= b).orElse(0);
        containsCustomDb = this.searchDBs.stream().anyMatch(SearchableDatabase::isCustomDb);
        containsRestDb = this.searchDBs.stream().anyMatch(SearchableDatabase::isRestDb);
    }

    @Override
    public String toString() {
        return searchDBs.stream().map(SearchableDatabase::name).collect(Collectors.joining(","));
    }

    public boolean containsRestDb() {
        return containsRestDb;
    }

    public boolean containsCustomDb() {
        return containsCustomDb;
    }

    public long getDBFlag() {
        return filter;
    }

    public static List<SearchableDatabase> makeDB(@NotNull String names) {
        return Arrays.stream(names.trim().split("\\s*,\\s*"))
                .map(SearchableDatabases::getDatabaseByName).flatMap(Optional::stream).distinct().collect(Collectors.toList());
    }
}
