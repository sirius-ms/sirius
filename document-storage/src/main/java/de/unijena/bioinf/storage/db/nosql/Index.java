package de.unijena.bioinf.storage.db.nosql;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@EqualsAndHashCode
public class Index {

    private final String[] fields;

    private final IndexType type;

    public Index(@NotNull IndexType type, @NotNull String... fields) {
        if (fields.length == 0) {
            throw new IllegalArgumentException("No fields.");
        }
        this.fields = fields;
        this.type = type;
    }

    public static Index unique(@NotNull String... fields) {
        return new Index(IndexType.UNIQUE, fields);
    }

    public static Index nonUnique(@NotNull String... fields) {
        return new Index(IndexType.NON_UNIQUE, fields);
    }

    public static Index fullText(@NotNull String... fields) {
        return new Index(IndexType.FULL_TEXT, fields);
    }

}
