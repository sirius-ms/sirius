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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.Id;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import static de.unijena.bioinf.ms.persistence.model.core.tags.ValueType.*;

@ToString
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag<Value> {
    @Id
    @Setter(AccessLevel.PACKAGE)
    protected long tagId;

    @NotNull
    @Setter(AccessLevel.PACKAGE)
    protected Class<?> taggedObjectClass;

    @Setter(AccessLevel.PACKAGE)
    protected long taggedObjectId;

    @NotNull
    @Setter(AccessLevel.PACKAGE)
    protected String tagName;

    @NotNull
    @JsonIgnore
    protected final ValueType valueType;

    @Setter(AccessLevel.PACKAGE)
    @Nullable
    protected Value value;

    public Tag(@NotNull ValueType valueType, Class<Value> valueClass) {
        this.valueType = valueType;
    }

    public void setValueAsObject(@Nullable Object value) {
        setValue((Value) value);
    }

    public static class Serializer extends JsonSerializer<Tag> {
        @Override
        public void serialize(Tag tag, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            ValueType valueType = tag.getValueType();
            gen.writeStartObject();

            gen.writeNumberField("tagId", tag.getTagId());
            gen.writeStringField("tagName", tag.getTagName());
            gen.writeStringField("taggedObjectClass", tag.getTaggedObjectClass() != null ? tag.getTaggedObjectClass().getName() : null);
            gen.writeNumberField("taggedObjectId", tag.getTaggedObjectId());

            switch (valueType) {
                case BOOLEAN -> {
                    if (tag.getValue() != null)
                        gen.writeBooleanField(valueType.getValueFieldName(), (Boolean) tag.getValue());
                    else
                        gen.writeNullField(valueType.getValueFieldName());
                }
                case INTEGER, TIME -> {
                    if (tag.getValue() != null)
                        gen.writeNumberField(valueType.getValueFieldName(), (Integer) tag.getValue());
                    else
                        gen.writeNullField(valueType.getValueFieldName());
                }
                case REAL -> {
                    if (tag.getValue() != null)
                        gen.writeNumberField(valueType.getValueFieldName(), (Double) tag.getValue());
                    else
                        gen.writeNullField(valueType.getValueFieldName());
                }
                case TEXT -> {
                    if (tag.getValue() != null)
                        gen.writeStringField(valueType.getValueFieldName(), (String) tag.getValue());
                    else
                        gen.writeNullField(valueType.getValueFieldName());
                }
                case DATE -> {
                    if (tag.getValue() != null)
                        gen.writeNumberField(valueType.getValueFieldName(), (Long) tag.getValue());
                    else
                        gen.writeNullField(valueType.getValueFieldName());
                }

            }
        }
    }

    public static class Deserializer extends JsonDeserializer<Tag> {

        @Override
        public Tag<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            // Read the entire JSON node for this ValueDefinition
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            Tag<?> tag = new Tag<>(NONE, NONE.getTagValueClass());
            for (ValueType type : ValueType.values()) {
                if (type.hasValue() && node.has(type.getValueFieldName())) {
                    tag = new Tag<>(type, type.getTagValueClass());
                    switch (type) {
                        case BOOLEAN -> tag.setValueAsObject(node.findValue(type.getValueFieldName()).asBoolean());
                        case INTEGER, TIME -> tag.setValueAsObject(node.findValue(type.getValueFieldName()).asInt());
                        case REAL -> tag.setValueAsObject(node.findValue(type.getValueFieldName()).asDouble());
                        case TEXT -> tag.setValueAsObject(node.findValue(type.getValueFieldName()).asText());
                        case DATE -> tag.setValueAsObject(node.findValue(type.getValueFieldName()).asLong());
                    }
                    break;
                }
            }

            if (node.has("tagId"))
                tag.setTagId(node.findValue("tagId").asLong(-1L));
            if (node.has("tagName"))
                tag.setTagName(node.findValue("tagName").asText(null));
            try {
                if (node.has("taggedObjectClass"))
                    tag.setTaggedObjectClass(tag.getClass().getClassLoader().loadClass(node.findValue("taggedObjectClass").asText(null)));
            } catch (ClassNotFoundException e) {
                throw new IOException("Cannot load class from class name stored in 'taggedObjectClass'!", e);
            }
            if (node.has("taggedObjectId"))
                tag.setTaggedObjectId(node.findValue("taggedObjectId").asLong(-1));

            return tag;
        }
    }


}
