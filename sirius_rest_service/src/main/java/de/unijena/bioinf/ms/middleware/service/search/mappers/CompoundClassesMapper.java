package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClass;
import de.unijena.bioinf.ms.middleware.model.annotations.CompoundClasses;
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
public class CompoundClassesMapper implements FieldMapper<CompoundClasses> {

    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @org.jspecify.annotations.Nullable CompoundClasses pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();
        if (pojo == null)
            return indexableFields;

        String classyfireFieldName = rootFieldName + ".cfClass";

        if (pojo.getClassyFireLineage() != null)
            pojo.getClassyFireLineage().stream()
                    .map(CompoundClass::getName)
                    .map(cn -> getIndexedFieldsFromSimpleValue(classyfireFieldName, cn, false, false, true, false))
                    .forEach(indexableFields::addAll);
        if (pojo.getClassyFireAlternatives() != null)
            pojo.getClassyFireAlternatives().stream()
                    .map(CompoundClass::getName)
                    .map(cn -> getIndexedFieldsFromSimpleValue(classyfireFieldName, cn, false, false, true, false))
                    .forEach(indexableFields::addAll);


        indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                rootFieldName + ".npcPathway",
                pojo.getNpcPathway().getName(),
                false, false, true, false));

        indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                rootFieldName + ".npcSuperclass",
                pojo.getNpcSuperclass().getName(),
                false, false, true, false));

        indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                rootFieldName + ".npcClass",
                pojo.getNpcClass().getName(),
                false, false, true, false));

        return indexableFields;
    }


    @Override
    public @Nullable CompoundClasses toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        return null;
    }

    @Override
    public void applyAnalyzersAndPointConfigs(
            @NotNull String rootFieldName,
            @NotNull Map<String, PointsConfig> pointsConfigMap,
            @NotNull Map<String, Analyzer> analyzerMap,
            @NotNull List<CharSequence> defaultSearchFields,
            @NotNull Map<String, SortField.Type> sortTypes
    ) {
        analyzerMap.put(rootFieldName + ".cfClass", SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + ".npcPathway", SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + ".npcSuperclass", SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + ".npcClass", SIRIUS_TEXT_ANALYZER);

        defaultSearchFields.add(rootFieldName + ".cfClass");
        defaultSearchFields.add(rootFieldName + ".npcPathway");
        defaultSearchFields.add(rootFieldName + ".npcSuperclass");
        defaultSearchFields.add(rootFieldName + ".npcClass");
    }
}
