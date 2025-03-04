package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.middleware.model.annotations.LipidAnnotation;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import org.apache.lucene.index.IndexableField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.getIndexedFieldsFromSimpleValue;

public class LipidAnnotationMapper implements FieldMapper<LipidAnnotation> {
    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable LipidAnnotation pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();

        if (pojo == null || pojo.getLipidSpecies() == null)
            return indexableFields;

        // always true, we match boolean false with -NOT field:true
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName + ".lipid", true, false, false, false, false));
        // only store species because this is enough to restore all other information
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName +".lipidSpecies", pojo.getLipidSpecies(), true, false, false, false));

        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName +".lipidMapsId", pojo.getLipidSpecies(), false, false, false, false));
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName +".lipidClassName", pojo.getLipidSpecies(), false, false, true, false));

        return indexableFields;
    }

    @Override
    public @Nullable LipidAnnotation toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        // restore lipid annotation from stored lipid species.
        String fieldName = rootFieldName + ".lipidSpecies";
        for (IndexableField field : document)
            if (fieldName.equals(field.name()))
                return AnnotationUtils.asLipidAnnotation(LipidSpecies.fromString(field.stringValue()));

        return null;
    }
}
