package de.unijena.bioinf.chemdb.annotations;

import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@DefaultProperty
public class FormulaSearchDB extends SearchableDBAnnotation {
    public final static FormulaSearchDB CONSIDER_ALL_FORMULAS = new FormulaSearchDB(null); //No DB for Search

    public static FormulaSearchDB noSearchDBAnnotation() {
        return CONSIDER_ALL_FORMULAS;
    }

    public FormulaSearchDB(Collection<SearchableDatabase> source) {
        super(source);
    }

    @DefaultInstanceProvider
    public static FormulaSearchDB fromString(@DefaultProperty @Nullable String value) {
        if (value == null || value.isEmpty() || value.toLowerCase().equals(NO_DB))
            return noSearchDBAnnotation();
        return new FormulaSearchDB(makeDB(value));
    }
}
