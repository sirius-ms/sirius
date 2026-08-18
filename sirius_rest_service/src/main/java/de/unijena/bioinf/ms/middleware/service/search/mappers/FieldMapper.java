package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.projectspace.QueryRewriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Maps one pojo field onto the lucene fields it is indexed as, and back.
 * <p>
 * A mapper that knows a closed vocabulary for the fields it contributes may say so by also implementing
 * {@code FieldVocabulary} of the description package - that is documentation and no concern of the mapping, so
 * it is not part of this contract.
 */
public interface FieldMapper<T> {

    default void toDocument(@NotNull String rootFieldName, @NotNull Document docToAdd, @Nullable T pojo){
        toIndexableFields(rootFieldName, pojo).forEach(docToAdd::add);
    }

    Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable T pojo);

    @Nullable
    T toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document);

    void applyAnalyzersAndPointConfigs(
            @NotNull String rootFieldName,
            @NotNull final Map<String, PointsConfig> pointsConfigMap,
            @NotNull final Map<String, Analyzer> analyzerMap,
            @NotNull final List<CharSequence> defaultSearchFields,
            @NotNull final Map<String, SortField.Type> sortTypes
    );
}
