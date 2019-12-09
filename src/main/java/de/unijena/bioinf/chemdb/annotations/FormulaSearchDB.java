package de.unijena.bioinf.chemdb.annotations;

import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.Nullable;

@DefaultProperty
public class FormulaSearchDB extends SearchableDBAnnotation {
    private static FormulaSearchDB CONSIDER_ALL_FORMULAS = null; //No DB for Search

    public static FormulaSearchDB noDBSearchAnnotation() {
        if (CONSIDER_ALL_FORMULAS == null)
            CONSIDER_ALL_FORMULAS = new FormulaSearchDB(null);
        return CONSIDER_ALL_FORMULAS;
    }


    public FormulaSearchDB(SearchableDatabase value) {
        super(value);
    }

    public boolean hasSearchableDB() {
        return value != null;
    }

    @DefaultInstanceProvider
    public static FormulaSearchDB fromString(@DefaultProperty @Nullable String value) {
        if (value == null || value.isEmpty() || value.toLowerCase().equals(NO_DB))
            return noDBSearchAnnotation();
        return new FormulaSearchDB(makeDB(value));
    }
}
