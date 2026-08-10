package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Describes the searchable fields this mapper contributes below the given root field name.
     * <p>
     * The default implementation derives the description from {@link #applyAnalyzersAndPointConfigs}, so it is
     * always consistent with the actual query parser configuration. Override to add descriptions or to expose
     * fields that need no parser configuration.
     */
    default List<SearchableField> describeSearchableFields(@NotNull String rootFieldName) {
        Map<String, PointsConfig> pointsConfigMap = new HashMap<>();
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        List<CharSequence> defaultSearchFields = new ArrayList<>();
        Map<String, SortField.Type> sortTypes = new HashMap<>();
        applyAnalyzersAndPointConfigs(rootFieldName, pointsConfigMap, analyzerMap, defaultSearchFields, sortTypes);
        return LuceneMappingUtils.toSearchableFields(pointsConfigMap, analyzerMap, defaultSearchFields, sortTypes);
    }
}
