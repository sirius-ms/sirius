/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;
import de.unijena.bioinf.ms.gui.utils.query.*;

import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Tab-completion grammar of the search bar: {@code [and|or] [not] <field-prefix>}
 * starts a clause, {@code [and|or] [not] (} opens a group, {@code )} closes one. Ported from
 * parseCompletion/parseClauseTail of LuceneChemicalSearchBar.tsx, extended by dot-segment matching
 * for nested field names.
 */
public class CompletionParserTest {

    private static SearchableField field(String name, SearchableFieldType type) {
        return new SearchableField().name(name).fieldType(type);
    }

    private static final List<SearchableField> FIELDS = List.of(
            field("ionMass", SearchableFieldType.DOUBLE),
            field("name", SearchableFieldType.TEXT),
            field("quality", SearchableFieldType.ENUM).possibleValues(List.of("GOOD", "DECENT", "BAD")),
            field("hasMs1", SearchableFieldType.BOOLEAN),
            field("hasMsMs", SearchableFieldType.BOOLEAN),
            field("topAnnotations.structureAnnotation.inchiKey", SearchableFieldType.TEXT),
            field("tags.city", SearchableFieldType.TEXT));

    private static Optional<Completion> parse(String text, boolean hasSibling, boolean groupOpen) {
        return CompletionParser.parse(text, FIELDS, hasSibling, groupOpen);
    }

    // --- clause completions ---

    @Test
    public void testEmptyTextOffersNothing() {
        assertTrue(parse("", false, false).isEmpty());
        assertTrue(parse("   ", false, false).isEmpty());
    }

    @Test
    public void testFieldPrefixOffersClause() {
        Completion.ClauseStart clause = (Completion.ClauseStart) parse("ion", false, false).orElseThrow();
        assertEquals("ionMass", clause.field().getName());
        assertFalse(clause.negated());
        assertNull(clause.logic());
    }

    @Test
    public void testUnknownPrefixOffersNothing() {
        assertTrue(parse("xyz", false, false).isEmpty());
    }

    @Test
    public void testNotRequiresWhitespaceSoNotionStaysFreeText() {
        Completion.ClauseStart clause = (Completion.ClauseStart) parse("not ion", false, false).orElseThrow();
        assertTrue(clause.negated());
        // "notion" must NOT be read as "not" + "ion"
        assertTrue(parse("notion", false, false).isEmpty());
    }

    @Test
    public void testConnectorOnlyCountsWithASibling() {
        Completion.ClauseStart withSibling = (Completion.ClauseStart) parse("and qual", true, false).orElseThrow();
        assertEquals(LogicOp.AND, withSibling.logic());
        assertEquals("quality", withSibling.field().getName());

        Completion.ClauseStart withoutSibling = (Completion.ClauseStart) parse("and qual", false, false).orElseThrow();
        assertNull(withoutSibling.logic(), "a connector without a sibling to join means nothing");
    }

    @Test
    public void testConnectorRequiresWhitespaceSoOrmaIsNotOrPlusMa() {
        // "orma" must not be read as OR + field prefix "ma"
        assertTrue(parse("orma", true, false).isEmpty());
    }

    @Test
    public void testOrNotFieldCombination() {
        Completion.ClauseStart clause = (Completion.ClauseStart) parse("or not qua", true, false).orElseThrow();
        assertEquals(LogicOp.OR, clause.logic());
        assertTrue(clause.negated());
        assertEquals("quality", clause.field().getName());
    }

    @Test
    public void testMatchingIsCaseInsensitive() {
        assertEquals("ionMass", ((Completion.ClauseStart) parse("IONM", false, false).orElseThrow()).field().getName());
    }

    @Test
    public void testDotSegmentMatchingFindsNestedFields() {
        // typing the leaf name finds the nested field
        Completion.ClauseStart clause = (Completion.ClauseStart) parse("inchi", false, false).orElseThrow();
        assertEquals("topAnnotations.structureAnnotation.inchiKey", clause.field().getName());
        // and full-prefix matching still works for tags
        assertEquals("tags.city", ((Completion.ClauseStart) parse("tags.ci", false, false).orElseThrow()).field().getName());
    }

    @Test
    public void testAmbiguousPrefixResolvesDeterministically() {
        // hasMs1 vs hasMsMs -> alphabetical order decides
        assertEquals("hasMs1", ((Completion.ClauseStart) parse("has", false, false).orElseThrow()).field().getName());
    }

    // --- groups ---

    @Test
    public void testOpenParenOffersGroup() {
        Completion.OpenGroup group = (Completion.OpenGroup) parse("(", false, false).orElseThrow();
        assertFalse(group.groupNegated());
        assertNull(group.logic());
        assertNull(group.clause());
    }

    @Test
    public void testNotBeforeParenNegatesTheGroup() {
        Completion.OpenGroup group = (Completion.OpenGroup) parse("not (", false, false).orElseThrow();
        assertTrue(group.groupNegated());
    }

    @Test
    public void testFieldPrefixInsideParenStartsClauseInGroup() {
        Completion.OpenGroup group = (Completion.OpenGroup) parse("or (ion", true, false).orElseThrow();
        assertEquals(LogicOp.OR, group.logic());
        assertNotNull(group.clause());
        assertEquals("ionMass", group.clause().field().getName());
    }

    @Test
    public void testNotAfterParenNegatesTheClauseNotTheGroup() {
        Completion.OpenGroup group = (Completion.OpenGroup) parse("( not ion", false, false).orElseThrow();
        assertFalse(group.groupNegated());
        assertTrue(group.clause().negated());
    }

    @Test
    public void testParenWithUnknownTailStaysFreeText() {
        assertTrue(parse("(xyz", false, false).isEmpty());
    }

    @Test
    public void testCloseParenOnlyOffersWhenAGroupIsOpen() {
        assertInstanceOf(Completion.CloseGroup.class, parse(")", false, true).orElseThrow());
        assertTrue(parse(")", false, false).isEmpty());
    }

    // --- value suggestions for the draft editor ---

    @Test
    public void testValueSuggestions() {
        assertEquals(List.of("GOOD", "DECENT", "BAD"),
                CompletionParser.valueSuggestions(FIELDS.get(2)));
        assertEquals(List.of("true", "false"),
                CompletionParser.valueSuggestions(FIELDS.get(3)));
        assertTrue(CompletionParser.valueSuggestions(FIELDS.get(1)).isEmpty());
    }
}
