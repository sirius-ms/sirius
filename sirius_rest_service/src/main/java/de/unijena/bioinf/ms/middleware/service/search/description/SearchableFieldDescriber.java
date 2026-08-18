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

package de.unijena.bioinf.ms.middleware.service.search.description;

import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.search.mappers.FieldFacts;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Explains an index to the people querying it: turns the facts an index reports about itself into the
 * {@link SearchableField}s a client reads.
 * <p>
 * Nothing here changes what a query matches. The index is asked what it holds; the answer is enriched with what
 * only the model knows - the java type behind a keyword, the vocabulary a field is restricted to, the javadoc
 * that explains it - and handed out. That enrichment is why this is not part of the index: it is presentation,
 * and it drags in documentation sources that indexing has no business knowing about.
 */
public class SearchableFieldDescriber {

    /**
     * Vocabularies are stateless (see {@link FieldVocabulary}), so one instance per class serves every field
     * that declares it. The cache is owned by the describer and dies with it.
     */
    private final Map<Class<? extends FieldVocabulary>, FieldVocabulary> vocabularies = new ConcurrentHashMap<>();

    /** Same contract as the vocabularies: stateless, one instance per class, owned by this describer. */
    private final Map<Class<? extends FieldTypes>, FieldTypes> fieldTypes = new ConcurrentHashMap<>();

    /**
     * Human-readable descriptions of a java field, e.g. from its OpenAPI annotations or javadoc. Injected so
     * that the description of a field and the framework it is documented with stay separable.
     */
    private final @NotNull Function<Field, String> descriptions;

    public SearchableFieldDescriber() {
        this(null);
    }

    public SearchableFieldDescriber(@Nullable Function<Field, String> descriptions) {
        this.descriptions = descriptions != null ? descriptions : field -> null;
    }

    /**
     * Describes every field of the schema, in the order the index reports them.
     */
    public List<SearchableField> describe(@NotNull IndexSchema schema) {
        List<SearchableField> described = new ArrayList<>(schema.fields().size());
        for (FieldFacts facts : schema.fields()) {
            SearchableField field = describe(facts);
            if (field != null)
                described.add(field);
        }
        return described;
    }

    /**
     * @return the description of one field, or null for a field that cannot be described to a client - a java
     * type nothing can be searched by. The index rejects those when it is built, so this is a guard, not a case.
     */
    @Nullable
    private SearchableField describe(@NotNull FieldFacts facts) {
        SearchableField.FieldType fieldType = declaredTypeOf(facts);
        if (fieldType == null)
            fieldType = facts.javaType() != null
                    ? SearchableFields.fieldTypeOf(facts.javaType())
                    : SearchableFields.fieldTypeOf(facts.kind());
        if (fieldType == null)
            return null;

        return SearchableField.builder()
                .name(facts.name())
                .fieldType(fieldType)
                .fullTextSearch(facts.analyzed())
                .sortable(facts.sortable())
                .defaultSearchField(facts.defaultSearchField())
                .possibleValues(possibleValuesOf(facts))
                .description(facts.javaType() != null ? descriptions.apply(facts.declaredBy()) : null)
                .build();
    }

    /**
     * The values a field can take, if they are known.
     * <p>
     * Declared via {@link SearchableFieldDoc#possibleValues()} - on the field itself, or on the field that
     * carries the mapper which contributed it; either way it is said in one place and read in one way. That is
     * the more specific statement and therefore wins.
     * <p>
     * Otherwise they follow from the java type: enums report their constants and booleans report true/false,
     * both exactly as they are indexed, so that a client can offer them for completion instead of leaving the
     * user to guess. Booleans are keyword indexed from {@link Boolean#toString()}, hence the lower case
     * literals.
     */
    @Nullable
    private List<String> possibleValuesOf(@NotNull FieldFacts facts) {
        List<String> declared = declaredPossibleValuesOf(facts);
        if (declared != null)
            return declared;

        Class<?> javaType = facts.javaType();
        if (javaType == null)
            return null;
        if (javaType.isEnum())
            return Arrays.stream(javaType.getEnumConstants()).map(e -> ((Enum<?>) e).name()).toList();
        if (javaType.equals(Boolean.class) || javaType.equals(boolean.class))
            return List.of("true", "false");
        return null;
    }

    /**
     * @return the type declared for this field, or null if none is declared or the declaration says nothing
     * about it - then the type follows from the java type, or from how the index holds it
     */
    @Nullable
    private SearchableField.FieldType declaredTypeOf(@NotNull FieldFacts facts) {
        SearchableFieldDoc doc = facts.declaredBy().getAnnotation(SearchableFieldDoc.class);
        if (doc == null || doc.fieldTypes() == FieldTypes.None.class)
            return null;
        return fieldTypes.computeIfAbsent(doc.fieldTypes(), clz -> instantiate(clz, facts.name()))
                .typeOf(facts.name());
    }

    private static <T> T instantiate(@NotNull Class<T> declared, @NotNull String fieldName) {
        try {
            return declared.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate '" + declared.getName() + "' declared on indexed"
                    + " field '" + fieldName + "'. It needs a public no-arg constructor.", e);
        }
    }

    /**
     * @return the vocabulary declared on the java field, or null if none is declared or the provider has no
     * vocabulary for this field
     */
    @Nullable
    private List<String> declaredPossibleValuesOf(@NotNull FieldFacts facts) {
        SearchableFieldDoc doc = facts.declaredBy().getAnnotation(SearchableFieldDoc.class);
        if (doc == null)
            return null;

        Class<? extends FieldVocabulary> vocabularyClass = doc.possibleValues();
        if (vocabularyClass == FieldVocabulary.None.class)
            return null;
        return vocabularies.computeIfAbsent(vocabularyClass, clz -> instantiate(clz, facts.name()))
                .getPossibleValues(facts.name());
    }
}
