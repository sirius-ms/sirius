package de.unijena.bioinf.ms.middleware.service.search.mappers;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FieldMapper<T> {

    default void toDocument(@NotNull String rootFieldName, @NotNull Document docToAdd, @Nullable T pojo){
        toIndexableFields(rootFieldName, pojo).forEach(docToAdd::add);
    }

    Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable T pojo);

    @Nullable
    default T toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document){
        throw new UnsupportedOperationException("Deserialization from index document not supported! Likely because this field is not stored!");
    };
}
