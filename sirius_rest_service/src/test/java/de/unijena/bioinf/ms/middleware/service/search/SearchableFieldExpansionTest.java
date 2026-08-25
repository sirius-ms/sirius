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
import de.unijena.bioinf.ms.middleware.model.search.SearchableField.FieldType;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFields;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link SearchableFields#expandDynamicKeyFields} - turning a {@code prefix.*} dynamic-key
 * template into one concrete {@link SearchableField} per key actually present in the index, so the
 * autocomplete offers real field names (e.g. {@code topAnnotations.matchedDatabases.PubChem})
 * instead of the un-queryable {@code .*} template.
 */
public class SearchableFieldExpansionTest {

    private static SearchableField field(String name, FieldType type) {
        return SearchableField.builder().name(name).fieldType(type).build();
    }

    private static Map<String, SearchableField> byName(List<SearchableField> fields) {
        return fields.stream().collect(Collectors.toMap(SearchableField::getName, Function.identity()));
    }

    @Test
    public void testNonDynamicFieldsPassThroughUnchanged() {
        SearchableField ionMass = field("ionMass", FieldType.DOUBLE);
        List<SearchableField> out = SearchableFields.expandDynamicKeyFields(List.of(ionMass), Set.of("ionMass", "name"));
        assertEquals(1, out.size());
        assertSame(ionMass, out.get(0), "a field without a .* terminal must pass through untouched");
    }

    @Test
    public void testDynamicTemplateExpandsToConcreteKeysSortedWithClonedAttributes() {
        SearchableField template = SearchableField.builder()
                .name("topAnnotations.matchedDatabases.*").fieldType(FieldType.INTEGER).sortable(true).build();

        List<SearchableField> out = SearchableFields.expandDynamicKeyFields(List.of(template),
                Set.of("topAnnotations.matchedDatabases.PubChem", "topAnnotations.matchedDatabases.CHEBI",
                        "ionMass", "tags.foo"));

        // one concrete field per matching key, alphabetically ordered, no .* left
        assertEquals(List.of("topAnnotations.matchedDatabases.CHEBI", "topAnnotations.matchedDatabases.PubChem"),
                out.stream().map(SearchableField::getName).toList());
        // template attributes cloned onto each concrete field
        out.forEach(f -> {
            assertEquals(FieldType.INTEGER, f.getFieldType());
            assertTrue(f.isSortable());
        });
    }

    @Test
    public void testEnumTemplatePreservesPossibleValues() {
        SearchableField template = SearchableField.builder()
                .name("qualities.*").fieldType(FieldType.ENUM)
                .possibleValues(List.of("GOOD", "BAD", "NOT_APPLICABLE")).build();

        Map<String, SearchableField> out = byName(SearchableFields.expandDynamicKeyFields(
                List.of(template), Set.of("qualities.peakShape")));

        SearchableField concrete = out.get("qualities.peakShape");
        assertNotNull(concrete, "the enum template must expand to the concrete quality-category key");
        assertEquals(FieldType.ENUM, concrete.getFieldType());
        assertEquals(List.of("GOOD", "BAD", "NOT_APPLICABLE"), concrete.getPossibleValues());
        assertNull(out.get("qualities.*"), "the .* template itself must not be reported");
    }

    @Test
    public void testSignificantSuffixLengthIsStructuralLeafPlusKeySegments() {
        SearchableField dbTemplate = field("topAnnotations.matchedDatabases.*", FieldType.INTEGER);
        SearchableField foldTemplate = field("stats.foldChange.*", FieldType.DOUBLE);

        Map<String, SearchableField> out = byName(SearchableFields.expandDynamicKeyFields(
                List.of(dbTemplate, foldTemplate),
                Set.of("topAnnotations.matchedDatabases.GNPS",                 // single-segment key -> 1 + 1
                        "stats.foldChange.SAMPLE.BLANK.APEX_INTENSITY.AVG")));  // four-segment key  -> 1 + 4

        assertEquals(2, out.get("topAnnotations.matchedDatabases.GNPS").getSignificantSuffixLength());
        assertEquals(5, out.get("stats.foldChange.SAMPLE.BLANK.APEX_INTENSITY.AVG").getSignificantSuffixLength());
    }

    @Test
    public void testPlainFieldsDefaultToSuffixLengthOne() {
        SearchableField ionMass = field("ionMass", FieldType.DOUBLE);
        assertEquals(1, SearchableFields.expandDynamicKeyFields(List.of(ionMass), Set.of("ionMass"))
                .get(0).getSignificantSuffixLength());
    }

    @Test
    public void testTemplateWithoutMatchingKeysIsDropped() {
        SearchableField template = field("topAnnotations.matchedDatabases.*", FieldType.INTEGER);
        // index holds no matchedDatabases.* keys yet -> nothing concrete to query -> drop the template
        List<SearchableField> out = SearchableFields.expandDynamicKeyFields(List.of(template), Set.of("ionMass"));
        assertTrue(out.isEmpty(), "a dynamic template with no materialized keys must be dropped, not left as .*");
    }

    @Test
    public void testPrefixBoundaryDoesNotMatchSiblingsOrBareStem() {
        SearchableField template = field("qualities.*", FieldType.ENUM);
        List<SearchableField> out = SearchableFields.expandDynamicKeyFields(List.of(template),
                Set.of("qualitiesExtra",     // sibling with a shared prefix but no dot boundary -> no match
                        "qualities",          // bare stem, no terminal key -> no match
                        "qualities.peakShape",// real key -> match
                        "qualities.a.b"));     // multi-segment key (e.g. fold-change style) -> match
        assertEquals(List.of("qualities.a.b", "qualities.peakShape"),
                out.stream().map(SearchableField::getName).toList());
    }
}
