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

    /**
     * The fields this mapper writes, below the root it is given. Public because what the values of these fields
     * are is explained elsewhere (see the {@code description} package) and both sides have to agree on the name.
     */
    public static final String CLASSY_FIRE = ".cfClass";
    public static final String NPC_PATHWAY = ".npcPathway";
    public static final String NPC_SUPERCLASS = ".npcSuperclass";
    public static final String NPC_CLASS = ".npcClass";


    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @org.jspecify.annotations.Nullable CompoundClasses pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();
        if (pojo == null)
            return indexableFields;

        String classyfireFieldName = rootFieldName + CLASSY_FIRE;

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


        if (pojo.getNpcPathway() != null)
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                    rootFieldName + NPC_PATHWAY,
                    pojo.getNpcPathway().getName(),
                    false, false, true, false));

        if (pojo.getNpcSuperclass() != null)
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                    rootFieldName + NPC_SUPERCLASS,
                    pojo.getNpcSuperclass().getName(),
                    false, false, true, false));

        if (pojo.getNpcClass() != null)
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(
                    rootFieldName + NPC_CLASS,
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
        analyzerMap.put(rootFieldName + CLASSY_FIRE, SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + NPC_PATHWAY, SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + NPC_SUPERCLASS, SIRIUS_TEXT_ANALYZER);
        analyzerMap.put(rootFieldName + NPC_CLASS, SIRIUS_TEXT_ANALYZER);

        defaultSearchFields.add(rootFieldName + CLASSY_FIRE);
        defaultSearchFields.add(rootFieldName + NPC_PATHWAY);
        defaultSearchFields.add(rootFieldName + NPC_SUPERCLASS);
        defaultSearchFields.add(rootFieldName + NPC_CLASS);
    }
}
