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
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Reads what a tag's definition says about it: the values it is restricted to, and the sentence explaining what
 * it means.
 * <p>
 * Both come from the one record, and it is read once per tag and per description. Not cached beyond that: a
 * definition can gain values or be edited while the project is open (see the tag controller), and a cached copy
 * would report a stale vocabulary until the project was reopened. Describing the searchable fields is not a hot
 * path, so one read per tag buys always-current answers.
 */
@RequiredArgsConstructor
public class TagDefinitionDocs implements TagFieldDocs {

    private final @NotNull Function<String, Optional<TagDefinition>> tagDefinitionByName;

    @Override
    public @Nullable TagFieldDoc describe(@NotNull String tagName) {
        return tagDefinitionByName.apply(tagName)
                .map(definition -> new TagFieldDoc(valuesInQueryForm(definition), description(definition)))
                .orElse(null);
    }

    /**
     * The values as a query has to contain them, which for dates and times is their formatted form rather than
     * the number they are stored and indexed as - the same conversion the tag definition API reports them with.
     * Null when the tag is not restricted at all, which is not the same as restricted to nothing.
     */
    @Nullable
    private static List<String> valuesInQueryForm(@NotNull TagDefinition definition) {
        ValueDefinition<?> valueDefinition = definition.getValueDefinition();
        ValueFormatter<?, ?> formatter = valueDefinition.getValueType().getFormatter();
        List<String> values = valueDefinition.getPossibleValues().stream()
                .map(formatter::toFormattedGeneric)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
        return values.isEmpty() ? null : values;
    }

    @Nullable
    private static String description(@NotNull TagDefinition definition) {
        String description = definition.getDescription();
        return description == null || description.isBlank() ? null : description;
    }
}
