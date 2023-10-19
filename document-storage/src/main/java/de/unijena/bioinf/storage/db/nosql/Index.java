package de.unijena.bioinf.storage.db.nosql;

import javax.validation.constraints.NotNull;

public class Index {

    private final String field;

    private final IndexType type;

    public Index(@NotNull String field, @NotNull IndexType type) {
        this.field = field;
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public IndexType getType() {
        return type;
    }

}
