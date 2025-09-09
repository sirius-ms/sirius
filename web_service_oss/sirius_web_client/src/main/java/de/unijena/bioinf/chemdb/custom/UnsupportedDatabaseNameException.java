package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnsupportedDatabaseNameException extends IllegalArgumentException {
    public UnsupportedDatabaseNameException(String dbName) {
        this(dbName, null);
    }
    public UnsupportedDatabaseNameException(@NotNull String dbName, @Nullable String validAlternativeName) {
        super(makeMessage(dbName, validAlternativeName));
    }

    private static String makeMessage(@NotNull String dbName, @Nullable String validAlternativeName){
        String message = "Unsupported database name '" + dbName + "'.";
        if (Utils.notNullOrBlank(validAlternativeName))
            message += "Valid alternative name would be '" + validAlternativeName + "'.";
        return message;
    }
}
