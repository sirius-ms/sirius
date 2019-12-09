package de.unijena.bioinf.chemdb.annotations;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.Nullable;

@DefaultProperty
public class StructureSearchDB extends SearchableDBAnnotation {
    public StructureSearchDB(SearchableDatabase value) {
        super(value);
    }

    @DefaultInstanceProvider
    public static StructureSearchDB fromString(@DefaultProperty @Nullable String value) {
        if (value == null || value.isEmpty() || value.toLowerCase().equals(NO_DB))
            value = DataSource.ALL.realName;
        return new StructureSearchDB(makeDB(value));
    }
}
