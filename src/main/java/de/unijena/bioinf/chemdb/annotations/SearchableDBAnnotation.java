package de.unijena.bioinf.chemdb.annotations;

import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class SearchableDBAnnotation implements Ms2ExperimentAnnotation {
    public final static String NO_DB = "none";
    public final SearchableDatabase value;

    protected SearchableDBAnnotation(SearchableDatabase value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name();
    }

    public String name() {
        if (value == null)
            return NO_DB;
        return value.name();
    }

    public boolean searchInPubchem() {
        if (value == null)
            return false;
        return value.searchInPubchem();
    }

    public boolean searchInBio() {
        if (value == null)
            return false;
        return value.searchInBio();
    }

    public boolean isCustomDb() {
        if (value == null)
            return false;
        return value.isCustomDb();
    }

    public File getDatabasePath() {
        if (value == null)
            return null;
        return value.getDatabasePath();
    }

    public long getDBFlag() {
        if (value == null)
            return 0L;
        return DatasourceService.getDBFlagFromName(name());
    }

    public static SearchableDatabase makeDB(@NotNull String name) {
        Path dbDir = Path.of(name);
        if (Files.isDirectory(dbDir))
            return SearchableDatabases.getDatabaseByPath(dbDir);
        return SearchableDatabases.getDatabaseByName(name);
    }
}
