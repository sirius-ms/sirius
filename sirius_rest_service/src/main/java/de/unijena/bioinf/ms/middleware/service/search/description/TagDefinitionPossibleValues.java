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

import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueFormatter;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The vocabulary of a project tag ({@code tags.<tagName>}), taken from the tag definition that restricts it.
 * <p>
 * The definition is looked up when the fields are described, not cached: possible values may be added to a tag
 * at any time (see the tag controller), and a cached copy would report a stale vocabulary until the project is
 * reopened. Describing the searchable fields is not a hot path, so one lookup per tag is cheap enough to buy
 * always-current values.
 */
@RequiredArgsConstructor
public class TagDefinitionPossibleValues implements FieldVocabulary {

    private final @NotNull Function<String, Optional<TagDefinition>> tagDefinitionByName;

    @Override
    public @Nullable List<String> getPossibleValues(@NotNull String fieldName) {
        String tagName = Taggable.tagNameOf(fieldName);
        if (tagName == null)
            return null; // not a tag field
        return tagDefinitionByName.apply(tagName)
                .map(TagDefinitionPossibleValues::valuesInQueryForm)
                .filter(values -> !values.isEmpty()) // an unrestricted tag accepts free text
                .orElse(null);
    }

    /**
     * The values as a query has to contain them, which for dates and times is their formatted form rather than
     * the number they are stored and indexed as - the same conversion the tag definition API reports them with.
     */
    private static List<String> valuesInQueryForm(@NotNull TagDefinition definition) {
        ValueDefinition<?> valueDefinition = definition.getValueDefinition();
        ValueFormatter<?, ?> formatter = valueDefinition.getValueType().getFormatter();
        return valueDefinition.getPossibleValues().stream()
                .map(formatter::toFormattedGeneric)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }
}
