package de.unijena.bioinf.storage.db;

import javax.validation.constraints.NotNull;

public abstract class NoSQLPOJO {

    public static final Index[] index = {};

    public enum IndexType {
        UNIQUE, NON_UNIQUE, FULL_TEXT
    }

    public static class Index {

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

}
