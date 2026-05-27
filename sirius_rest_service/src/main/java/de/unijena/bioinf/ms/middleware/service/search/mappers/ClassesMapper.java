package de.unijena.bioinf.ms.middleware.service.search.mappers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.SIRIUS_TEXT_ANALYZER;
import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.getIndexedFieldsFromSimpleValue;

/**
 * Mapper for predicted compound classes (ClassyFire lineage).
 */
public class ClassesMapper implements FieldMapper<List<String>> {

    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable List<String> pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();
        if (pojo == null)
            return indexableFields;

        for (String cl : pojo) {
            // 1. Index under the provided field name (e.g., topAnnotations.class)
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName, cl, false, false, true, false));

            // 2. Also index under "classes" (sibling) for compatibility if rootFieldName ends with .class
            if (rootFieldName.endsWith(".class")) {
                String sibling = rootFieldName.substring(0, rootFieldName.lastIndexOf('.') + 1) + "classes";
                indexableFields.addAll(getIndexedFieldsFromSimpleValue(sibling, cl, false, false, true, false));
            }

            // 3. Also index under top-level "class" for easy searching via "class: x"
            indexableFields.addAll(getIndexedFieldsFromSimpleValue("class", cl, false, false, true, false));
        }

        return indexableFields;
    }

    @Override
    public @Nullable List<String> toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        List<String> results = new ArrayList<>();
        for (IndexableField field : document)
            if (rootFieldName.equals(field.name()))
                results.add(field.stringValue());
        return results.isEmpty() ? null : results;
    }

    @Override
    public void applyAnalyzersAndPointConfigs(
            @NotNull String rootFieldName,
            @NotNull Map<String, PointsConfig> pointsConfigMap,
            @NotNull Map<String, Analyzer> analyzerMap,
            @NotNull List<CharSequence> defaultSearchFields,
            @NotNull Map<String, SortField.Type> sortTypes
    ) {
        analyzerMap.put(rootFieldName, SIRIUS_TEXT_ANALYZER);
        analyzerMap.put("class", SIRIUS_TEXT_ANALYZER);
        
        if (rootFieldName.endsWith(".class")) {
            String sibling = rootFieldName.substring(0, rootFieldName.lastIndexOf('.') + 1) + "classes";
            analyzerMap.put(sibling, SIRIUS_TEXT_ANALYZER);
        }
        
        defaultSearchFields.add(rootFieldName);
    }
}
