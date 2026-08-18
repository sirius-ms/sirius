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
import de.unijena.bioinf.ms.middleware.service.search.description.FieldVocabulary;
import de.unijena.bioinf.ms.middleware.service.search.description.IndexFacts;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldDescriber;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchContext;
import de.unijena.bioinf.ms.middleware.service.search.mappers.FieldMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.SortField;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Describing a model, the way production does it: the index reports what it holds, the describer explains it.
 * <p>
 * Configuring an index and describing it are two steps on purpose - the second one is not part of the search
 * engine and does not have to happen at all. Tests that only care about the answer go through here.
 */
final class DescribedFields {

    private DescribedFields() {
    }

    static IndexSchema schemaOf(Class<?> pojoClass, FieldMapper<?>... mappers) {
        FieldMapper<?>[] withTags = mappers.length > 0 ? mappers
                : new FieldMapper<?>[]{new TagMapper(tagName -> ValueType.TEXT)};
        return new GenericPojoMapper<>(pojoClass, withTags).detectAnalyzersAndPointConfigs(
                new HashMap<String, PointsConfig>(), new HashMap<String, Analyzer>(),
                new ArrayList<CharSequence>(), new HashMap<String, SortField.Type>(), new HashMap<>());
    }

    static List<SearchableField> of(Class<?> pojoClass) {
        return of(pojoClass, null);
    }

    static List<SearchableField> of(Class<?> pojoClass, @Nullable Function<Field, String> descriptions) {
        return new SearchableFieldDescriber(descriptions).describe(schemaOf(pojoClass));
    }

    static Map<String, SearchableField> asMap(Class<?> pojoClass) {
        return asMap(pojoClass, null);
    }

    static Map<String, SearchableField> asMap(Class<?> pojoClass, @Nullable Function<Field, String> descriptions) {
        return of(pojoClass, descriptions).stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()));
    }

    /**
     * The whole answer for one project, the way the project assembles it: index facts plus the vocabularies
     * only the project can supply.
     */
    static SearchableFieldService serviceFor(SearchContext context, @Nullable FieldVocabulary projectVocabulary) {
        return new SearchableFieldService(IndexFacts.of(context), projectVocabulary);
    }

    static List<SearchableField> of(SearchContext context, @Nullable FieldVocabulary projectVocabulary, Class<?> modelClass) {
        return serviceFor(context, projectVocabulary).describe(modelClass);
    }
}
