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
import de.unijena.bioinf.ms.middleware.service.search.description.TagDefinitionDocs;
import de.unijena.bioinf.ms.middleware.service.search.description.TagFieldDocs;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldService;
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
        return tagDefinition(tagName, valueType, possibleValues, null);
    }

    private static TagDefinition tagDefinition(String tagName, ValueType valueType, List<?> possibleValues,
                                               String description) {
        return TagDefinition.builder()
                .tagName(tagName)
                .tagType("TEST")
                .description(description)
                .valueDefinition(new ValueDefinition<>(valueType, possibleValues, null, null))
                .build();
    }

    private static TagDefinitionDocs providerFor(TagDefinition... definitions) {
        Map<String, TagDefinition> byName = new HashMap<>();
        for (TagDefinition definition : definitions)
            byName.put(definition.getTagName(), definition);
        return new TagDefinitionDocs(tagName -> Optional.ofNullable(byName.get(tagName)));
    }

    private static List<String> valuesOf(TagDefinitionDocs docs, String tagName) {
        TagFieldDocs.TagFieldDoc doc = docs.describe(tagName);
        return doc == null ? null : doc.possibleValues();
    }

    private static String descriptionOf(TagDefinitionDocs docs, String tagName) {
        TagFieldDocs.TagFieldDoc doc = docs.describe(tagName);
        return doc == null ? null : doc.description();
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
        TagDefinitionDocs provider = providerFor(
                tagDefinition("sampleType", ValueType.TEXT, List.of("Sample", "Blank", "Standard")));

        assertEquals(List.of("Sample", "Blank", "Standard"), valuesOf(provider, "sampleType"));
    }

    /**
     * Values are reported the way they are written in a query, which for dates and times is their formatted
     * form - not the millisecond long they are stored and indexed as.
     */
    @Test
    public void testValuesAreReportedInTheirQueryForm() {
        TagDefinitionDocs provider = providerFor(
                tagDefinition("measured", ValueType.DATE, List.of(0L, 86_400_000L)),
                tagDefinition("replicate", ValueType.INTEGER, List.of(1, 2, 3)));

        assertEquals(List.of("1970-01-01", "1970-01-02"), valuesOf(provider, "measured"));
        assertEquals(List.of("1", "2", "3"), valuesOf(provider, "replicate"));
    }

    @Test
    public void testTagWithoutRestrictedValuesHasNone() {
        TagDefinitionDocs provider = providerFor(tagDefinition("comment", ValueType.TEXT, List.of()));

        assertNull(valuesOf(provider, "comment"));
    }

    @Test
    public void testUnknownTagAndNonTagFieldsHaveNoValues() {
        TagDefinitionDocs provider = providerFor(
                tagDefinition("sampleType", ValueType.TEXT, List.of("Sample")));

        assertNull(provider.describe("neverDefined"), "a tag this project does not define says nothing at all");
    }

    // ---- how the tag search field reports them -------------------------------------------------------------

    @Test
    public void testTagFieldCarriesTheDeclaredValues() {
        SearchableField field = SearchableFields.toTagSearchableField(
                "tags.sampleType", "sampleType", ValueType.TEXT, List.of("Sample", "Blank"), null);

        assertEquals(List.of("Sample", "Blank"), field.getPossibleValues());
    }

    @Test
    public void testUnrestrictedTagFieldAcceptsFreeText() {
        SearchableField field = SearchableFields.toTagSearchableField(
                "tags.comment", "comment", ValueType.TEXT, null, null);

        assertNull(field.getPossibleValues());
    }

    /**
     * Neither kind of flag tag can declare values (the tag definition rejects that), but both have some, and
     * they are not the same: a boolean tag is written as true or false, a value-less tag only as true.
     */
    @Test
    public void testABooleanTagOffersBothValues() {
        assertEquals(List.of("true", "false"), SearchableFields
                .toTagSearchableField("tags.isBlank", "isBlank", ValueType.BOOLEAN, null, null).getPossibleValues());
    }

    /**
     * A value-less tag is a presence flag: the tag mapper writes true and nothing else, so offering false would
     * offer a value that matches nothing, whatever the tag. Absence is matched by negating the clause, which
     * needs a second clause to negate against - a purely negative query matches nothing in lucene.
     */
    @Test
    public void testAValuelessTagOffersOnlyTrue() {
        assertEquals(List.of("true"), SearchableFields
                .toTagSearchableField("tags.pfas", "pfas", ValueType.NONE, null, null).getPossibleValues());
    }

    /**
     * The invariant a client leans on when it treats a single-valued boolean field as a presence flag: no tag
     * can be a boolean and restricted to one value at the same time, so a boolean field offering one value is
     * never a tag that might offer two after its definition is extended.
     * <p>
     * Pinned here because the rule is enforced far away - in the persistence model, when the definition is
     * built - while what depends on it is in the GUI (see {@code CompletionParser.isSingleValued}), with the
     * API in between. Relaxing it there would silently make naming such a tag complete it to a value.
     */
    @Test
    public void testAFlagTagCannotBeRestrictedToValues() {
        for (ValueType flagType : List.of(ValueType.BOOLEAN, ValueType.NONE))
            assertThrows(IllegalArgumentException.class,
                    () -> new ValueDefinition<>(flagType, List.of(true), null, null),
                    "a " + flagType + " tag must not be allowed to declare its values");

        // the same call for a tag that may be restricted, so the assertions above are about the flag types
        assertEquals(List.of("Sample"),
                List.copyOf(new ValueDefinition<>(ValueType.TEXT, List.of("Sample"), null, null).getPossibleValues()));
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

        try (PerPojoSearchContext context = new PerPojoSearchContext(null, new HashMap<>(Map.of("sampleType", ValueType.TEXT)))) {
            SearchableFieldService fields = DescribedFields.serviceFor(context, new TagDefinitionDocs(lookup));

            assertEquals(List.of("Sample"), tagField(fields).getPossibleValues());

            // a value added to the definition afterwards must show up without describing the index again
            definitions.put("sampleType", tagDefinition("sampleType", ValueType.TEXT, List.of("Sample", "Blank")));

            assertEquals(List.of("Sample", "Blank"), tagField(fields).getPossibleValues());
        }
    }

    /**
     * Without a vocabulary the tag fields are described as free text.
     */
    @Test
    public void testDescriptionWithoutVocabularyReportsNoValues() throws IOException {
        try (PerPojoSearchContext context = new PerPojoSearchContext(null, new HashMap<>(Map.of("sampleType", ValueType.TEXT)))) {
            assertNull(tagField(DescribedFields.serviceFor(context, (TagFieldDocs) null)).getPossibleValues());
        }
    }

    private static SearchableField tagField(SearchableFieldService fields) {
        return fields.describe(Run.class).stream()
                .collect(Collectors.toMap(SearchableField::getName, Function.identity()))
                .get("tags.sampleType");
    }

    // ---- what a tag field says about itself ----------------------------------------------------------------

    /**
     * A tag definition carries the sentence explaining what the tag means - the PFAS tag says what makes a
     * feature a potential PFAS - and that is what a client should show for the field, rather than a restating
     * of the field name.
     */
    @Test
    public void testTheDescriptionOfATagIsWhatItsDefinitionSays() {
        TagDefinitionDocs docs = providerFor(
                tagDefinition("pfas", ValueType.TEXT, List.of(), "Features that look like a PFAS."));

        assertEquals("Features that look like a PFAS.", descriptionOf(docs, "pfas"));
    }

    @Test
    public void testATagWithoutADescriptionHasNone() {
        TagDefinitionDocs docs = providerFor(tagDefinition("comment", ValueType.TEXT, List.of()));

        assertNull(descriptionOf(docs, "comment"));
        assertNull(docs.describe("neverDefined"), "a tag this project does not define says nothing at all");
    }

    /**
     * The definition's sentence replaces the generated one; what the field name alone cannot say - that a
     * value-less tag is matched by searching for true - is kept either way.
     */
    @Test
    public void testTheTagFieldUsesTheDefinitionDescriptionWhenThereIsOne() {
        assertEquals("Features that look like a PFAS.", SearchableFields
                .toTagSearchableField("tags.pfas", "pfas", ValueType.TEXT, null, "Features that look like a PFAS.")
                .getDescription());

        assertEquals("Project tag 'pfas'", SearchableFields
                .toTagSearchableField("tags.pfas", "pfas", ValueType.TEXT, null, null).getDescription());

        assertEquals("A flag.; presence flag, search for value 'true'", SearchableFields
                .toTagSearchableField("tags.flag", "flag", ValueType.NONE, null, "A flag.").getDescription());
    }
}
