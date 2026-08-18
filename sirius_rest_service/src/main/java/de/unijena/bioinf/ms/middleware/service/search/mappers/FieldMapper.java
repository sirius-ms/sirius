package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.projectspace.PossibleValueProvider;
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
 * A mapper is also the {@link PossibleValueProvider} for the fields it contributes: it typically contributes
 * several of them (e.g. one per ontology level), which is why the vocabulary is requested per field name. The
 * default is "no vocabulary", so a mapper only implements it if it actually knows its values.
 */
public interface FieldMapper<T> extends PossibleValueProvider {

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
     * The values a field contributed by this mapper can take, or null (the default) if this mapper does not know
     * a closed vocabulary for it. Asked per field because one mapper usually contributes several fields.
     */
    @Override
    default @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
        return null;
    }
}
