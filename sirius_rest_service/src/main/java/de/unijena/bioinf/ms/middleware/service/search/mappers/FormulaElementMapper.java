package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import lombok.extern.slf4j.Slf4j;
import de.unijena.bioinf.projectspace.QueryRewriter;
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

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.getIndexedFieldsFromSimpleValue;
import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.getPointsConfigForType;

@Slf4j
public class FormulaElementMapper implements FieldMapper<String> {
    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable String pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();

        if (pojo == null)
            return indexableFields;

        // simple formula as keyword
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName, pojo, true, false, false, false));

        // elementFilter: index per-element counts for range filtering. A single malformed formula must not
        // abort indexing of the whole document, so skip just the element breakdown when it cannot be parsed.
        MolecularFormula formula = MolecularFormula.parseOrNull(pojo);
        if (formula != null)
            formula.forEach(e ->
                    indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName + "." + e.getSymbol(), formula.numberOf(e), false, false, false, false)));
        else
            log.debug("Could not parse molecular formula '{}' for field '{}'; skipping element breakdown.", pojo, rootFieldName);

        return indexableFields;
    }

    @Override
    public @Nullable String toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
        // restore lipid annotation from stored lipid species.
        for (IndexableField field : document)
            if (rootFieldName.equals(field.name()))
                return field.stringValue();
        return null;
    }

    @Override
    public void applyAnalyzersAndPointConfigs(
            @NotNull String rootFieldName,
            @NotNull Map<String, PointsConfig> pointsConfigMap,
            @NotNull Map<String, Analyzer> analyzerMap,
            @NotNull List<CharSequence> defaultSearchFields,
            @NotNull Map<String, SortField.Type> sortTypes,
            @NotNull Map<String, QueryRewriter> queryRewriters
    ) {
        analyzerMap.put(rootFieldName, new KeywordAnalyzer());
        pointsConfigMap.put(rootFieldName + ".*", getPointsConfigForType(Integer.class));
    }


}
