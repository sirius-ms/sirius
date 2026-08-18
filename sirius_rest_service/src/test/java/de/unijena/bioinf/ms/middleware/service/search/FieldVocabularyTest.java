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
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldDescriber;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexFieldWithMapper;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.ms.middleware.service.search.description.FieldVocabulary;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldDoc;
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
 * Tests for the {@link FieldVocabulary} mechanism that lets a field declare its closed vocabulary, so that
 * clients can offer the values for completion instead of leaving the user to guess them.
 * <p>
 * The vocabulary is never written into the annotation itself: {@link SearchableFieldDoc#possibleValues()} names
 * a vocabulary class, and a {@link FieldMapper} may implement one for the fields it contributes. Both are asked
 * per field name, because one mapper typically contributes several fields with different vocabularies.
 * <p>
 * Note which annotation this is: declaring a vocabulary says nothing about how the field is indexed, so it does
 * not belong on {@code @IndexField}.
 */
public class FieldVocabularyTest {

    public enum TestQuality {GOOD, DECENT, BAD}

    /**
     * A vocabulary that is the same for every field it is asked for - the common case for a field whose values
     * come from a fixed domain list (e.g. the chromatography of a run).
     */
    public static class ChromatographyValues implements FieldVocabulary {
        @Override
        public List<String> getPossibleValues(@NotNull String fieldName) {
            return List.of("Liquid Chromatography", "Gas Chromatography");
        }
    }

    /**
     * Echoes the field name it was asked for, so a test can assert which name the machinery passes in.
     */
    public static class FieldNameEchoingValues implements FieldVocabulary {
        @Override
        public List<String> getPossibleValues(@NotNull String fieldName) {
            return List.of(fieldName);
        }
    }

    /**
     * A provider that has no opinion about the field - the values derived from the java type must still apply.
     */
    public static class NoOpinionValues implements FieldVocabulary {
        @Override
        public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
            return null;
        }
    }

    /**
     * Vocabularies are expected to be stateless and are instantiated once, see
     * {@link #testProviderIsInstantiatedOncePerDescriber}.
     */
    public static class CountingValues implements FieldVocabulary {
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
     * Has no no-arg constructor, so it cannot be instantiated by the describer.
     */
    public static class UninstantiableValues implements FieldVocabulary {
        @SuppressWarnings("unused")
        public UninstantiableValues(String needsAnArgument) {
        }

        @Override
        public List<String> getPossibleValues(@NotNull String fieldName) {
            return List.of();
        }
    }

    public static class TestNested {
        @IndexField
        @SearchableFieldDoc(possibleValues = FieldNameEchoingValues.class)
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
    public static class VocabularyAwareMapper extends TestClassMapper implements FieldVocabulary {
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

        @IndexField
        @SearchableFieldDoc(possibleValues = ChromatographyValues.class)
        public String chromatography;

        @IndexField(name = "analyzers")
        @SearchableFieldDoc(possibleValues = FieldNameEchoingValues.class)
        public List<String> massAnalyzers;

        @IndexField
        @SearchableFieldDoc(possibleValues = FieldNameEchoingValues.class)
        public Map<String, String> labels;

        @IndexField
        public TestQuality quality;

        @IndexField
        @SearchableFieldDoc(possibleValues = ChromatographyValues.class)
        public TestQuality overriddenQuality;

        @IndexField
        @SearchableFieldDoc(possibleValues = NoOpinionValues.class)
        public TestQuality undecidedQuality;

        @IndexField
        public boolean hasMs1;

        @IndexField
        @SearchableFieldDoc(possibleValues = CountingValues.class)
        public String counted;

        @IndexField
        @SearchableFieldDoc(possibleValues = CountingValues.class)
        public String countedTwice;

        @IndexField
        public TestNested nested;

        @IndexFieldWithMapper(mapper = VocabularyAwareMapper.class)
        @SearchableFieldDoc(possibleValues = VocabularyAwareMapper.class)
        public String compoundClasses;

        // the same mapper, registered only as a mapper: its vocabulary must not apply behind our back
        @IndexFieldWithMapper(mapper = VocabularyAwareMapper.class)
        public String undeclaredClasses;

        @IndexFieldWithMapper(mapper = TestClassMapper.class)
        public String plainClasses;
    }

    public static class BrokenProviderPojo {
        @IndexField(documentId = true)
        public String id;

        @IndexField
        @SearchableFieldDoc(possibleValues = UninstantiableValues.class)
        public String broken;
    }

    private static Map<String, SearchableField> describeAsMap(Class<?> pojoClass) {
        return DescribedFields.asMap(pojoClass);
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
     * Providers are stateless, so a describer constructs each of them once and reuses it - for every field that
     * declares it and however often the fields are described. The cache is owned by the describer, so a new one
     * legitimately builds its own instance; that is the trade for not having a process-wide cache to manage.
     */
    @Test
    public void testProviderIsInstantiatedOncePerDescriber() {
        IndexSchema schema = DescribedFields.schemaOf(TestPojo.class);
        SearchableFieldDescriber describer = new SearchableFieldDescriber();
        int before = CountingValues.INSTANTIATIONS.get();

        // two fields declare the provider, and both descriptions walk them
        describer.describe(schema);
        describer.describe(schema);

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
                "message must name the vocabulary: " + e.getMessage());
        assertTrue(e.getMessage().contains("broken"),
                "message must name the field: " + e.getMessage());
    }

    /**
     * Knowing a vocabulary is not part of being a mapper: a mapper that has nothing to say about its values
     * simply is not a vocabulary, and its fields are described without any.
     */
    @Test
    public void testMapperThatIsNoVocabularyContributesFieldsWithoutValues() {
        assertFalse(new TestClassMapper() instanceof FieldVocabulary);

        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);
        assertNull(fields.get("plainClasses.level").getPossibleValues());
        assertNull(fields.get("plainClasses.name").getPossibleValues());
    }

    /**
     * A mapper is asked per field, because it typically contributes several fields with different vocabularies.
     */
    @Test
    public void testMapperSuppliesValuesPerField() {
        VocabularyAwareMapper mapper = new VocabularyAwareMapper();

        assertEquals(List.of("PATHWAY", "SUPERCLASS", "CLASS"), mapper.getPossibleValues("classes.level"));
        assertNull(mapper.getPossibleValues("classes.name"));
    }

    /**
     * A class may be both a mapper and a vocabulary, but it has to be registered as both: mapping a field and
     * explaining its values are two statements, and the second one is not implied by the first. Registering it
     * per declaration also means the same mapper can be paired with different vocabularies elsewhere.
     */
    @Test
    public void testAMapperVocabularyAppliesWhereItIsDeclared() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        assertEquals(List.of("PATHWAY", "SUPERCLASS", "CLASS"),
                fields.get("compoundClasses.level").getPossibleValues());
        assertNull(fields.get("compoundClasses.name").getPossibleValues(),
                "the vocabulary answers per field, and has nothing to say about this one");
    }

    /**
     * The same mapper on a field that does not declare it says nothing - being a vocabulary is not something a
     * mapper can smuggle in through the annotation that registers it for indexing.
     */
    @Test
    public void testAMapperVocabularyDoesNotApplyWhereItIsNotDeclared() {
        Map<String, SearchableField> fields = describeAsMap(TestPojo.class);

        assertNull(fields.get("undeclaredClasses.level").getPossibleValues());
        assertNull(fields.get("undeclaredClasses.name").getPossibleValues());
    }
}
