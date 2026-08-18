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

import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.search.SearchableField;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.TagDefinitionPossibleValues;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFields;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A project tag definition may restrict a tag to a list of possible values. Those values are exactly what a
 * query has to contain, so they are reported for the tag's search field ({@code tags.<tagName>}).
 */
public class TagPossibleValuesTest {

    private static TagDefinition tagDefinition(String tagName, ValueType valueType, List<?> possibleValues) {
        return TagDefinition.builder()
                .tagName(tagName)
                .tagType("TEST")
                .valueDefinition(new ValueDefinition<>(valueType, possibleValues, null, null))
                .build();
    }

    private static TagDefinitionPossibleValues providerFor(TagDefinition... definitions) {
        Map<String, TagDefinition> byName = new HashMap<>();
        for (TagDefinition definition : definitions)
            byName.put(definition.getTagName(), definition);
        return new TagDefinitionPossibleValues(tagName -> Optional.ofNullable(byName.get(tagName)));
    }

    // ---- the field name a tag is searched under ------------------------------------------------------------

    @Test
    public void testTagNameIsRecoveredFromTheFieldName() {
        assertEquals("sampleType", Taggable.tagNameOf(Taggable.makeTagFieldName("sampleType")));
        // tag names are free text and may contain dots - everything behind the prefix is the name
        assertEquals("my.tag", Taggable.tagNameOf("tags.my.tag"));
    }

    @Test
    public void testNonTagFieldNamesHaveNoTagName() {
        assertNull(Taggable.tagNameOf("ionMass"));
        assertNull(Taggable.tagNameOf("tags"));
        assertNull(Taggable.tagNameOf("tags."));
        assertNull(Taggable.tagNameOf("topAnnotations.tags.foo"));
    }

    // ---- reading the vocabulary off a tag definition -------------------------------------------------------

    @Test
    public void testPossibleValuesOfATextTagAreReported() {
        TagDefinitionPossibleValues provider = providerFor(
                tagDefinition("sampleType", ValueType.TEXT, List.of("Sample", "Blank", "Standard")));

        assertEquals(List.of("Sample", "Blank", "Standard"), provider.getPossibleValues("tags.sampleType"));
    }

    /**
     * Values are reported the way they are written in a query, which for dates and times is their formatted
     * form - not the millisecond long they are stored and indexed as.
     */
    @Test
    public void testValuesAreReportedInTheirQueryForm() {
        TagDefinitionPossibleValues provider = providerFor(
                tagDefinition("measured", ValueType.DATE, List.of(0L, 86_400_000L)),
                tagDefinition("replicate", ValueType.INTEGER, List.of(1, 2, 3)));

        assertEquals(List.of("1970-01-01", "1970-01-02"), provider.getPossibleValues("tags.measured"));
        assertEquals(List.of("1", "2", "3"), provider.getPossibleValues("tags.replicate"));
    }

    @Test
    public void testTagWithoutRestrictedValuesHasNone() {
        TagDefinitionPossibleValues provider = providerFor(tagDefinition("comment", ValueType.TEXT, List.of()));

        assertNull(provider.getPossibleValues("tags.comment"));
    }

    @Test
    public void testUnknownTagAndNonTagFieldsHaveNoValues() {
        TagDefinitionPossibleValues provider = providerFor(
                tagDefinition("sampleType", ValueType.TEXT, List.of("Sample")));

        assertNull(provider.getPossibleValues("tags.neverDefined"));
        assertNull(provider.getPossibleValues("ionMass"));
    }

    // ---- how the tag search field reports them -------------------------------------------------------------

    @Test
    public void testTagFieldCarriesTheDeclaredValues() {
        SearchableField field = SearchableFields.toTagSearchableField(
                "tags.sampleType", "sampleType", ValueType.TEXT, List.of("Sample", "Blank"));

        assertEquals(List.of("Sample", "Blank"), field.getPossibleValues());
    }

    @Test
    public void testUnrestrictedTagFieldAcceptsFreeText() {
        SearchableField field = SearchableFields.toTagSearchableField(
                "tags.comment", "comment", ValueType.TEXT, null);

        assertNull(field.getPossibleValues());
    }

    /**
     * Boolean tags cannot declare values (the tag definition rejects that), but they have some: a boolean tag is
     * queried as true/false, and so is a value-less tag, which is indexed as a presence flag.
     */
    @Test
    public void testBooleanAndPresenceTagsOfferTrueAndFalse() {
        assertEquals(List.of("true", "false"), SearchableFields
                .toTagSearchableField("tags.isBlank", "isBlank", ValueType.BOOLEAN, null).getPossibleValues());
        assertEquals(List.of("true", "false"), SearchableFields
                .toTagSearchableField("tags.pfas", "pfas", ValueType.NONE, null).getPossibleValues());
    }

    // ---- end to end through the search context -------------------------------------------------------------

    /**
     * Possible values can be added to a tag definition at any time, so they are read when the fields are
     * described rather than cached when the tag is registered - a cached copy would silently go stale.
     */
    @Test
    public void testSearchContextReportsTheCurrentValuesOfATag() throws IOException {
        Map<String, TagDefinition> definitions = new HashMap<>();
        definitions.put("sampleType", tagDefinition("sampleType", ValueType.TEXT, List.of("Sample")));
        Function<String, Optional<TagDefinition>> lookup = name -> Optional.ofNullable(definitions.get(name));

        try (PerPojoSearchContext context = new PerPojoSearchContext(null,
                new HashMap<>(Map.of("sampleType", ValueType.TEXT)), null,
                new TagDefinitionPossibleValues(lookup))) {

            assertEquals(List.of("Sample"), tagField(context).getPossibleValues());

            // a value added to the definition afterwards must show up without touching the search context
            definitions.put("sampleType", tagDefinition("sampleType", ValueType.TEXT, List.of("Sample", "Blank")));

            assertEquals(List.of("Sample", "Blank"), tagField(context).getPossibleValues());
        }
    }

    /**
     * Without a provider the tag fields are described as before - free text.
     */
    @Test
    public void testSearchContextWithoutProviderReportsNoValues() throws IOException {
        try (PerPojoSearchContext context = new PerPojoSearchContext(null,
                new HashMap<>(Map.of("sampleType", ValueType.TEXT)))) {
            assertNull(tagField(context).getPossibleValues());
        }
    }

    private static SearchableField tagField(PerPojoSearchContext context) {
        return context.getSearchableFields(Run.class).stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()))
                .get("tags.sampleType");
    }
}
