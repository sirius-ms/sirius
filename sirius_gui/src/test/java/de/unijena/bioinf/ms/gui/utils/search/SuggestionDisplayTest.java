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

import de.unijena.bioinf.ms.gui.utils.query.NumberOp;
import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests what one autocomplete row shows: the description is the dimmed text behind the (possibly
 * shortened) name - it tells the user more than the path does - and the fully-qualified name only
 * fills in when there is no description. The hover tooltip always carries the name that is actually
 * submitted plus the full, untruncated description.
 */
public class SuggestionDisplayTest {

    private static final String INCHI = "topAnnotations.structureAnnotation.inchiKey";
    private static final String DESCRIPTION = "The InChIKey of the top structure candidate, "
            + "a 27 character hash of the InChI - unique per structure and safe to compare.";

    private static SearchableField field(String name, String description, Integer suffixLength) {
        SearchableField field = new SearchableField().name(name).fieldType(SearchableFieldType.TEXT)
                .description(description);
        return suffixLength == null ? field : field.significantSuffixLength(suffixLength);
    }

    private static TokenInputModel.Suggestion.FieldSuggestion suggestion(SearchableField field) {
        return new TokenInputModel.Suggestion.FieldSuggestion(field);
    }

    // --- field rows ---

    @Test
    public void testDescriptionWinsOverTheFullyQualifiedName() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                suggestion(field(INCHI, DESCRIPTION, 1)), FieldDisplay.Mode.COMPACT);

        assertEquals("inchiKey", row.display());
        assertEquals(DESCRIPTION, row.dimmed(), "the description is what the user needs, not the path");
        assertNotNull(row.tooltip());
        assertTrue(row.tooltip().contains(INCHI), "the submitted name stays discoverable in the tooltip");
        assertTrue(row.tooltip().contains(DESCRIPTION), "the tooltip carries the description in full");
    }

    @Test
    public void testFullNameFillsInWhenThereIsNoDescription() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                suggestion(field(INCHI, null, 1)), FieldDisplay.Mode.COMPACT);

        assertEquals("inchiKey", row.display());
        assertEquals(INCHI, row.dimmed());
        assertTrue(row.tooltip().contains(INCHI));
    }

    @Test
    public void testBlankDescriptionCountsAsNone() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                suggestion(field(INCHI, "   ", 1)), FieldDisplay.Mode.COMPACT);
        assertEquals(INCHI, row.dimmed());
    }

    @Test
    public void testFullyQualifiedModeShowsTheNameOnceOnly() {
        // the display already IS the full name - repeating it as dimmed text says nothing
        SuggestionDisplay.Row rowWithout = SuggestionDisplay.of(
                suggestion(field(INCHI, null, 1)), FieldDisplay.Mode.EXTENSIVE);
        assertEquals(INCHI, rowWithout.display());
        assertNull(rowWithout.dimmed());

        SuggestionDisplay.Row rowWith = SuggestionDisplay.of(
                suggestion(field(INCHI, DESCRIPTION, 1)), FieldDisplay.Mode.EXTENSIVE);
        assertEquals(INCHI, rowWith.display());
        assertEquals(DESCRIPTION, rowWith.dimmed());
    }

    @Test
    public void testCompactUsesTheSignificantSuffixLength() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                suggestion(field("tags.pfas.value", null, 2)), FieldDisplay.Mode.COMPACT);
        assertEquals("pfas.value", row.display());
        assertEquals("tags.pfas.value", row.dimmed());
    }

    @Test
    public void testUnshortenableNameIsNotRepeatedAsDimmedText() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                suggestion(field("ionMass", null, 1)), FieldDisplay.Mode.COMPACT);
        assertEquals("ionMass", row.display());
        assertNull(row.dimmed());
    }

    // --- non-field rows ---

    @Test
    public void testTokenRowKeepsItsDescriptionAndGetsATooltip() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                new TokenInputModel.Suggestion.TokenSuggestion(TokenInputModel.SpecialToken.NOT),
                FieldDisplay.Mode.COMPACT);

        assertEquals("NOT", row.display());
        assertEquals("Negate the next filter or group", row.dimmed());
        assertTrue(row.tooltip().contains("Negate the next filter or group"));
    }

    @Test
    public void testOperatorRowKeepsItsDescription() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                new TokenInputModel.Suggestion.OperatorSuggestion(NumberOp.GTE), FieldDisplay.Mode.COMPACT);

        assertEquals(NumberOp.GTE.getSymbol(), row.display());
        assertEquals(NumberOp.GTE.getDescription(), row.dimmed());
    }

    @Test
    public void testRowWithoutAnyDescriptionHasNoDimmedTextAndNoTooltip() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                new TokenInputModel.Suggestion.ValueSuggestion("GOOD"), FieldDisplay.Mode.COMPACT);

        assertEquals("GOOD", row.display());
        assertNull(row.dimmed());
        assertNull(row.tooltip());
    }

    @Test
    public void testTooltipsAreFormattedAsHtml() {
        SuggestionDisplay.Row row = SuggestionDisplay.of(
                suggestion(field(INCHI, DESCRIPTION, 1)), FieldDisplay.Mode.COMPACT);
        assertTrue(row.tooltip().startsWith("<html>"), row.tooltip());
    }
}
