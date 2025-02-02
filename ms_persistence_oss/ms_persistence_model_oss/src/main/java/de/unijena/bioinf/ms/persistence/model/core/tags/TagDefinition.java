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

package de.unijena.bioinf.ms.persistence.model.core.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@SuperBuilder
@Jacksonized
@Getter
@ToString
public class TagDefinition {
    @Id
    private long tagDefId;

    private String tagName;

    private String tagType;

    private String description;

    @JsonSerialize(using = ValueDefinition.Serializer.class)
    @JsonDeserialize(using = ValueDefinition.Deserializer.class)
    private ValueDefinition<?> valueDefinition;

    @JsonIgnore
    public ValueType getValueType() {
        return valueDefinition.getValueType();
    }

    @Builder.Default
    private boolean editable = true;

    @JsonIgnore
    public Tag newTagWithFormattedValue(@Nullable Object formattedValue, @NotNull Class<?> taggedObjectClass, long taggedObjectId) throws IllegalArgumentException {
        return setFormattedValueOfTag(newTag(taggedObjectClass, taggedObjectId), formattedValue);

    }

    @JsonIgnore
    public Tag newTagWithValue(@Nullable Object value, @NotNull Class<?> taggedObjectClass, long taggedObjectId) throws IllegalArgumentException {
         return setValueOfTag(newTag(taggedObjectClass, taggedObjectId), value);
    }

    @SneakyThrows
    private Tag newTag(@NotNull Class<?> taggedObjectClass, long taggedObjectId) {
        Tag tag = new Tag(valueDefinition.getValueType());
        tag.setTagName(getTagName());
        tag.setTaggedObjectId(taggedObjectId);
        tag.setTaggedObjectClass(taggedObjectClass);
        return tag;
    }

    @JsonIgnore
    public Tag setValueOfTag(Tag tag, Object value) throws IllegalArgumentException {
        if (!tag.getTagName().equals(tagName))
            new IllegalArgumentException("The given tag does not match the TagDefinition! Expected: " + tagName + ". Found: " + tag.getTagName() + ".");
        if (valueDefinition != null && !valueDefinition.isValueValid(value))
            throw new IllegalArgumentException("Value '" + value + "' is not valid for tag '" + tagName + "' which expects types of type: " + valueDefinition.getValueType() + ".");

        tag.setValue(value);
        return tag;
    }

    @JsonIgnore
    public Tag setFormattedValueOfTag(Tag tag, Object formattedValue) throws IllegalArgumentException {
        if (!tag.getTagName().equals(tagName))
            new IllegalArgumentException("The given tag does not match the TagDefinition! Expected: " + tagName + ". Found: " + tag.getTagName() + ".");

        Object value = getValueDefinition().getValueType().getFormatter().fromFormattedGeneric(formattedValue);

        if (valueDefinition != null && !valueDefinition.isValueValid(value))
            throw new IllegalArgumentException("Value '" + value + "' is not valid for tag '" + tagName + "' which expects types of type: " + valueDefinition.getValueType() + ".");

        tag.setValue(value);

        return tag;
    }
}
