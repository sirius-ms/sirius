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
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Describes the searchable fields of one project: what its index holds, said in the terms an API client reads.
 * <p>
 * Assembles the whole answer - the fields of the model, the keys that a dynamic field has actually taken, the
 * project's tags, and the vocabularies of fields whose values are project state. Everything it needs from the
 * index it takes as facts (see {@link IndexFacts}); the index itself neither knows nor does any of this.
 */
public class SearchableFieldService {

    private final @NotNull IndexFacts facts;
    private final @NotNull SearchableFieldDescriber describer;

    /**
     * Vocabularies of fields whose values are project state - the tags defined in the project, the adducts
     * detected in it. Owned by the project, which is the only thing that can answer them, and asked on every
     * description so that a tag definition extended or an import run later is reflected right away.
     */
    private final @Nullable FieldVocabulary projectVocabulary;

    /**
     * What this project's tag definitions say about their tags - the values they allow and what they mean.
     * Asked on every description, like the vocabularies, so an edited definition shows up without reopening.
     */
    private final @Nullable TagFieldDocs tagDocs;

    public SearchableFieldService(@NotNull IndexFacts facts, @Nullable FieldVocabulary projectVocabulary) {
        this(facts, projectVocabulary, null);
    }

    public SearchableFieldService(@NotNull IndexFacts facts, @Nullable FieldVocabulary projectVocabulary,
                                  @Nullable TagFieldDocs tagDocs) {
        this(facts, projectVocabulary, tagDocs, new SearchableFieldDescriber(ApiDocFieldDescriptions.PROVIDER));
    }

    public SearchableFieldService(@NotNull IndexFacts facts, @Nullable FieldVocabulary projectVocabulary,
                                  @Nullable TagFieldDocs tagDocs,
                                  @NotNull SearchableFieldDescriber describer) {
        this.facts = facts;
        this.projectVocabulary = projectVocabulary;
        this.tagDocs = tagDocs;
        this.describer = describer;
    }

    /**
     * Every field of the given model that can be searched in this project. Empty for a model that has no index.
     */
    public List<SearchableField> describe(@NotNull Class<?> modelClass) {
        List<SearchableField> fields = new ArrayList<>(SearchableFields.expandDynamicKeyFields(
                describer.describe(facts.schemaOf(modelClass)), facts.materializedFieldNames(modelClass)));

        // A field of the model may have a vocabulary that only the project knows - the adducts it detected.
        // That is the more specific answer, so it replaces whatever the model declared for itself.
        fields.forEach(field -> {
            List<String> possibleValues = possibleValuesOf(field.getName());
            if (possibleValues != null)
                field.setPossibleValues(possibleValues);
        });

        // A tag is project state through and through - it exists because this project defines it - so its
        // field is built here, complete: what it can hold and what its definition says it means.
        // Tags are searchable on objects that carry them, and only where there is an index at all - a model
        // without one describes nothing, not even the project's tags.
        if (!fields.isEmpty() && Taggable.class.isAssignableFrom(modelClass)) {
            Map<String, ValueType> tagValueTypes = facts.tagValueTypes();
            tagValueTypes.keySet().stream().sorted().forEach(tagName -> {
                // one read of the definition answers both halves of what the field says about itself
                TagFieldDocs.TagFieldDoc doc = tagDocs == null ? null : tagDocs.describe(tagName);
                fields.add(SearchableFields.toTagSearchableField(Taggable.makeTagFieldName(tagName), tagName,
                        tagValueTypes.get(tagName),
                        doc == null ? null : doc.possibleValues(),
                        doc == null ? null : doc.description()));
            });
        }
        return fields;
    }

    @Nullable
    private List<String> possibleValuesOf(@NotNull String fieldName) {
        return projectVocabulary == null ? null : projectVocabulary.getPossibleValues(fieldName);
    }


}
