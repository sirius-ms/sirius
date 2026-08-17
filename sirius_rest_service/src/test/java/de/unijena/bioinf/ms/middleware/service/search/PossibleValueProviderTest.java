/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.search.mappers.FieldMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFieldWithMapper;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.projectspace.PossibleValueProvider;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link PossibleValueProvider} mechanism that lets a field declare its closed vocabulary, so that
 * clients can offer the values for completion instead of leaving the user to guess them.
 * <p>
 * The vocabulary is never written into the annotation itself: {@link IndexField#possibleValueProvider()} names a
 * provider class, and {@link FieldMapper} is a provider itself for the fields it contributes. Both are asked per
 * field name, because one mapper typically contributes several fields with different vocabularies.
 */
public class PossibleValueProviderTest {

    public enum TestQuality {GOOD, DECENT, BAD}

    /**
     * A vocabulary that is the same for every field it is asked for - the common case for a field whose values
     * come from a fixed domain list (e.g. the chromatography of a run).
     */
    public static class ChromatographyValues implements PossibleValueProvider {
        @Override
        public List<String> getPossibleValues(@NotNull String fieldName) {
            return List.of("Liquid Chromatography", "Gas Chromatography");
        }
    }

    /**
     * Echoes the field name it was asked for, so a test can assert which name the machinery passes in.
     */
    public static class FieldNameEchoingValues implements PossibleValueProvider {
        @Override
        public List<String> getPossibleValues(@NotNull String fieldName) {
            return List.of(fieldName);
        }
    }

    /**
     * A provider that has no opinion about the field - the values derived from the java type must still apply.
     */
    public static class NoOpinionValues implements PossibleValueProvider {
        @Override
        public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
            return null;
        }
    }

    /**
     * Providers are expected to be stateless and are instantiated once, see {@link #testProviderIsInstantiatedOncePerMapper}.
     */
    public static class CountingValues implements PossibleValueProvider {
        static final AtomicInteger INSTANTIATIONS = new AtomicInteger();

        public CountingValues() {
            INSTANTIATIONS.incrementAndGet();
        }

        @Override
        public List<String> getPossibleValues(@NotNull String fieldName) {
            return List.of("counted");
        }
    }

    /**
     * Has no no-arg constructor, so it cannot be instantiated by the mapper.
     */
    public static class UninstantiableValues implements PossibleValueProvider {
        @SuppressWarnings("unused")
        public UninstantiableValues(String needsAnArgument) {
        }

        @Override
        public List<String> getPossibleValues(@NotNull String fieldName) {
            return List.of();
        }
    }

    public static class TestNested {
        @IndexField(possibleValueProvider = FieldNameEchoingValues.class)
        public String origin;
    }

    /**
     * A mapper contributing two fields, without an opinion on their values.
     */
    public static class TestClassMapper implements FieldMapper<String> {
        @Override
        public Iterable<IndexableField> toIndexableFields(@NotNull String rootFieldName, @Nullable String pojo) {
            return List.of();
        }

        @Override
        public @Nullable String toPojo(@NotNull String rootFieldName, @NotNull Iterable<IndexableField> document) {
            return null;
        }

        @Override
        public void applyAnalyzersAndPointConfigs(@NotNull String rootFieldName,
                                                  @NotNull Map<String, PointsConfig> pointsConfigMap,
                                                  @NotNull Map<String, Analyzer> analyzerMap,
                                                  @NotNull List<CharSequence> defaultSearchFields,
                                                  @NotNull Map<String, SortField.Type> sortTypes) {
            analyzerMap.put(rootFieldName + ".level", new KeywordAnalyzer());
            analyzerMap.put(rootFieldName + ".name", new KeywordAnalyzer());
        }
    }

    /**
     * Same mapper, but it knows the vocabulary of one of its two fields.
     */
    public static class VocabularyAwareMapper extends TestClassMapper {
        @Override
        public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
            return fieldName.endsWith(".level") ? List.of("PATHWAY", "SUPERCLASS", "CLASS") : null;
        }
    }

    public static class TestPojo {
        @IndexField(documentId = true)
        public String id;

        @IndexField
        public String freeText;

        @IndexField(possibleValueProvider = ChromatographyValues.class)
        public String chromatography;

        @IndexField(name = "analyzers", possibleValueProvider = FieldNameEchoingValues.class)
        public List<String> massAnalyzers;

        @IndexField(possibleValueProvider = FieldNameEchoingValues.class)
        public Map<String, String> labels;

        @IndexField
        public TestQuality quality;

        @IndexField(possibleValueProvider = ChromatographyValues.class)
        public TestQuality overriddenQuality;

        @IndexField(possibleValueProvider = NoOpinionValues.class)
        public TestQuality undecidedQuality;

        @IndexField
        public boolean hasMs1;

        @IndexField(possibleValueProvider = CountingValues.class)
        public String counted;

        @IndexField(possibleValueProvider = CountingValues.class)
        public String countedTwice;

        @IndexField
        public TestNested nested;

        @IndexFieldWithMapper(mapper = VocabularyAwareMapper.class)
        public String compoundClasses;
    }

    public static class BrokenProviderPojo {
        @IndexField(documentId = true)
        public String id;

        @IndexField(possibleValueProvider = UninstantiableValues.class)
        public String broken;
    }

    private static Map<String, SearchableField> describeAsMap(Class<?> pojoClass) {
        return new GenericPojoMapper<>(pojoClass).describeSearchableFields().stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()));
    }

    /**
     * The default is "free text": no provider means no closed vocabulary, and the field accepts anything.
     */
    @Test
    public void testFieldWithoutProviderHasNoPossibleValues() {
        assertNull(describeAsMap(TestPojo.class).get("freeText").getPossibleValues());
    }

    @Test
    public void testDeclaredProviderSuppliesPossibleValues() {
        assertEquals(List.of("Liquid Chromatography", "Gas Chromatography"),
                describeAsMap(TestPojo.class).get("chromatography").getPossibleValues());
    }

    /**
     * The provider is asked for the lucene field name a query has to use - not the java field name. That is the
     * renamed name for {@code @IndexField(name=...)}, the dotted path for nested fields, and the {@code .*}
     * template for map-like fields.
     */
    @Test
    public void testProviderReceivesTheLuceneFieldName() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        assertEquals(List.of("analyzers"), fields.get("analyzers").getPossibleValues());
        assertEquals(List.of("nested.origin"), fields.get("nested.origin").getPossibleValues());
        assertEquals(List.of("labels.*"), fields.get("labels.*").getPossibleValues());
    }

    /**
     * Enums and booleans keep reporting their values without any provider - the mechanism adds a way to declare
     * a vocabulary, it does not replace the one derived from the java type.
     */
    @Test
    public void testDerivedValuesStillApplyWithoutProvider() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        assertEquals(List.of("GOOD", "DECENT", "BAD"), fields.get("quality").getPossibleValues());
        assertEquals(List.of("true", "false"), fields.get("hasMs1").getPossibleValues());
    }

    /**
     * An explicitly declared vocabulary is more specific than the one derived from the java type, so it wins.
     */
    @Test
    public void testDeclaredProviderWinsOverDerivedEnumConstants() {
        assertEquals(List.of("Liquid Chromatography", "Gas Chromatography"),
                describeAsMap(TestPojo.class).get("overriddenQuality").getPossibleValues());
    }

    /**
     * A provider may decline to answer for a field (returning null); the derived values then still apply.
     */
    @Test
    public void testProviderWithoutOpinionFallsBackToDerivedValues() {
        assertEquals(List.of("GOOD", "DECENT", "BAD"),
                describeAsMap(TestPojo.class).get("undecidedQuality").getPossibleValues());
    }

    /**
     * Providers are stateless, so a mapper constructs each of them once and reuses it - for every field that
     * declares it and however often the fields are described. The cache is owned by the mapper, so a new mapper
     * legitimately builds its own instance; that is the trade for not having a process-wide cache to manage.
     */
    @Test
    public void testProviderIsInstantiatedOncePerMapper() {
        GenericPojoMapper<TestPojo> mapper = new GenericPojoMapper<>(TestPojo.class);
        int before = CountingValues.INSTANTIATIONS.get();

        // two fields declare the provider, and both descriptions walk them
        mapper.describeSearchableFields();
        mapper.describeSearchableFields();

        assertEquals(before + 1, CountingValues.INSTANTIATIONS.get());
    }

    /**
     * A provider that cannot be instantiated is a programming error - it must name both the provider and the
     * field it was declared on, otherwise it is not findable in a model of hundreds of fields.
     */
    @Test
    public void testUninstantiableProviderFailsWithAClearMessage() {
        Exception e = assertThrows(Exception.class, () -> describeAsMap(BrokenProviderPojo.class));
        assertTrue(e.getMessage().contains(UninstantiableValues.class.getName()),
                "message must name the provider: " + e.getMessage());
        assertTrue(e.getMessage().contains("broken"),
                "message must name the field: " + e.getMessage());
    }

    /**
     * A mapper that does not know its vocabulary contributes fields without possible values - the default, so
     * that existing mappers keep working unchanged.
     */
    @Test
    public void testMapperWithoutOverrideReportsNoPossibleValues() {
        List<SearchableField> fields = new TestClassMapper().describeSearchableFields("classes");

        assertEquals(2, fields.size());
        fields.forEach(field -> assertNull(field.getPossibleValues(), field.getName() + " must have no values"));
    }

    /**
     * A mapper is asked per field, because it typically contributes several fields with different vocabularies.
     */
    @Test
    public void testMapperSuppliesValuesPerField() {
        Map<String, SearchableField> fields = new VocabularyAwareMapper().describeSearchableFields("classes").stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()));

        assertEquals(List.of("PATHWAY", "SUPERCLASS", "CLASS"), fields.get("classes.level").getPossibleValues());
        assertNull(fields.get("classes.name").getPossibleValues());
    }

    /**
     * The mapper vocabulary must survive the way a pojo attaches mappers, not just a direct mapper call.
     */
    @Test
    public void testMapperValuesArePartOfThePojoDescription() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        assertEquals(List.of("PATHWAY", "SUPERCLASS", "CLASS"),
                fields.get("compoundClasses.level").getPossibleValues());
        assertNull(fields.get("compoundClasses.name").getPossibleValues());
    }
}
