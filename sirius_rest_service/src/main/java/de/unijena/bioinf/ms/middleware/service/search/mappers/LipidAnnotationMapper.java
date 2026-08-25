package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.middleware.model.annotations.LipidAnnotation;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
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

public class LipidAnnotationMapper implements FieldMapper<LipidAnnotation> {

    /**
     * The fields this mapper writes, below the root it is given. Public because what their values are is
     * explained elsewhere (see the {@code description} package) and both sides have to agree on the name.
     */
    public static final String LIPID = ".lipid";
    public static final String LIPID_SPECIES = ".lipidSpecies";
    public static final String LIPID_MAPS_ID = ".lipidMapsId";
    public static final String LIPID_CLASS_NAME = ".lipidClassName";

    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable LipidAnnotation pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();

        if (pojo == null || pojo.getLipidSpecies() == null)
            return indexableFields;

        // always true, we match boolean false with -NOT field:true
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName + LIPID, true, false, false, false, false));
        // only store species because this is enough to restore all other information
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName + LIPID_SPECIES, pojo.getLipidSpecies(), true, false, false, false));

        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName + LIPID_MAPS_ID, pojo.getLipidMapsId(), false, false, false, false));
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName + LIPID_CLASS_NAME, pojo.getLipidClassName(), false, false, true, false));

        return indexableFields;
    }

    @Override
    public @Nullable LipidAnnotation toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        // restore lipid annotation from stored lipid species.
        String fieldName = rootFieldName + LIPID_SPECIES;
        for (IndexableField field : document)
            if (fieldName.equals(field.name()))
                return AnnotationUtils.asLipidAnnotation(LipidSpecies.fromString(field.stringValue()));

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
        analyzerMap.put(rootFieldName + LIPID, new KeywordAnalyzer());
        analyzerMap.put(rootFieldName + LIPID_SPECIES, new KeywordAnalyzer());
        analyzerMap.put(rootFieldName + LIPID_MAPS_ID, new KeywordAnalyzer());
        analyzerMap.put(rootFieldName + LIPID_CLASS_NAME, SIRIUS_TEXT_ANALYZER); //todo do we want specif lipid class analyzer?
        defaultSearchFields.add(rootFieldName + LIPID_CLASS_NAME);
    }
}
