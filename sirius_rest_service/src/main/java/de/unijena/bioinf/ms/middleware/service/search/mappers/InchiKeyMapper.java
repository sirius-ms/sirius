package de.unijena.bioinf.ms.middleware.service.search.mappers;

import de.unijena.bioinf.ms.middleware.service.search.PubChemNameResolver;
import de.unijena.bioinf.ms.middleware.service.search.SiriusStandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.getIndexedFieldsFromSimpleValue;

/**
 * Mapper for InChIKeys that supports structure name resolution via PubChem during search.
 */
public class InchiKeyMapper implements FieldMapper<String> {

    @Override
    public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable String pojo) {
        List<IndexableField> indexableFields = new ArrayList<>();
        if (pojo == null)
            return indexableFields;

        // 1. Index under the provided field name (e.g., topAnnotations.name)
        indexableFields.addAll(getIndexedFieldsFromSimpleValue(rootFieldName, pojo, false, false, true, false));

        // 2. Also index under "inchiKey" (sibling) for compatibility if rootFieldName ends with .name
        if (rootFieldName.endsWith(".name")) {
            String sibling = rootFieldName.substring(0, rootFieldName.lastIndexOf('.') + 1) + "inchiKey";
            indexableFields.addAll(getIndexedFieldsFromSimpleValue(sibling, pojo, false, false, true, false));
        }

        // 3. Also index under top-level "inchiKey" for easy searching via "inchiKey: x"
        indexableFields.addAll(getIndexedFieldsFromSimpleValue("inchiKey", pojo, false, false, true, false));

        return indexableFields;
    }

    @Override
    public @Nullable String toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
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
            @NotNull Map<String, SortField.Type> sortTypes
    ) {
        NameSearchAnalyzer analyzer = new NameSearchAnalyzer();
        analyzerMap.put(rootFieldName, analyzer);
        analyzerMap.put("inchiKey", analyzer);
        
        if (rootFieldName.endsWith(".name")) {
            String sibling = rootFieldName.substring(0, rootFieldName.lastIndexOf('.') + 1) + "inchiKey";
            analyzerMap.put(sibling, analyzer);
        }

        // Add to default search fields
        defaultSearchFields.add(rootFieldName);
    }

    /**
     * Analyzer that resolves chemical names to InChIKeys via PubChem during query parsing.
     */
    public static class NameSearchAnalyzer extends SiriusStandardAnalyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            TokenStreamComponents components = super.createComponents(fieldName);
            return new TokenStreamComponents(components.getSource(), new PubChemResolutionFilter(components.getTokenStream()));
        }

        @Override
        protected TokenStream normalize(String fieldName, TokenStream in) {
            return new PubChemResolutionFilter(super.normalize(fieldName, in));
        }

        private static class PubChemResolutionFilter extends org.apache.lucene.analysis.TokenFilter {
            private final org.apache.lucene.analysis.tokenattributes.CharTermAttribute charTermAttr = addAttribute(org.apache.lucene.analysis.tokenattributes.CharTermAttribute.class);

            protected PubChemResolutionFilter(TokenStream input) {
                super(input);
            }

            @Override
            public boolean incrementToken() throws java.io.IOException {
                if (input.incrementToken()) {
                    String term = charTermAttr.toString();
                    // We only resolve if the term is NOT already a 14-char InChIKey connectivity part
                    if (!PubChemNameResolver.isInchiKey2D(term)) {
                        String resolved = PubChemNameResolver.resolveInchiKeyFromPubChem(term);
                        if (resolved != null) {
                            charTermAttr.setEmpty().append(resolved.toLowerCase());
                        }
                    }
                    return true;
                }
                return false;
            }
        }
    }
}
