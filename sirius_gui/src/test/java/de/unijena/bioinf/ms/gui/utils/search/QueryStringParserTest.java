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

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link QueryStringParser} is the inverse of {@link LuceneQueryCompiler}. The core guarantee is a
 * stable round trip: {@code compile(parse(s)) == s} for every shape the compiler produces.
 */
public class QueryStringParserTest {

    private static SearchableField field(String name, SearchableFieldType type) {
        return new SearchableField().name(name).fieldType(type);
    }

    private static final List<SearchableField> FIELDS = List.of(
            field("ionMass", SearchableFieldType.DOUBLE),
            field("name", SearchableFieldType.TEXT),
            field("quality", SearchableFieldType.ENUM),
            field("hasMsMs", SearchableFieldType.BOOLEAN));

    private static QueryContainer parse(String s) {
        return QueryStringParser.parse(s, FIELDS).orElseThrow(() -> new AssertionError("did not parse: " + s));
    }

    /** parse then compile must reproduce the input string exactly. */
    private static void assertRoundTrip(String s) {
        assertEquals(s, LuceneQueryCompiler.compile(parse(s), ""), "round trip");
    }

    @Test
    public void testRoundTripAcrossEveryCompiledShape() {
        assertRoundTrip("hasMsMs:true");                                    // fielded text
        assertRoundTrip("ionMass:195.08");                                  // numeric equality
        assertRoundTrip("ionMass:[300 TO 400]");                            // inclusive range
        assertRoundTrip("ionMass:{300 TO 400}");                            // exclusive range
        assertRoundTrip("ionMass:[300 TO *]");                              // >=
        assertRoundTrip("ionMass:{300 TO *}");                              // >
        assertRoundTrip("ionMass:[* TO 400]");                              // <=
        assertRoundTrip("ionMass:{* TO 400}");                              // <
        assertRoundTrip("quality:GOOD OR quality:DECENT");                  // OR
        assertRoundTrip("hasMsMs:true AND quality:GOOD");                   // AND
        assertRoundTrip("(quality:GOOD OR quality:DECENT) AND hasMsMs:true"); // group + AND
        assertRoundTrip("NOT hasMsMs:true");                                // negation
        assertRoundTrip("caffeine");                                        // keyless free text
        assertRoundTrip("name:\"caffeic acid\"");                           // quoted phrase value
        assertRoundTrip("(hasMsMs:true) AND (caffeine)");                   // clauses + free-text segment
    }

    @Test
    public void testFieldedClauseStructure() {
        QueryClause c = (QueryClause) parse("hasMsMs:true").items().get(0);
        assertEquals("hasMsMs", c.field());
        assertEquals("true", c.value1());
        assertNull(c.op());
        assertFalse(c.negated());
    }

    @Test
    public void testNumericFieldBecomesEquality() {
        QueryClause c = (QueryClause) parse("ionMass:195.08").items().get(0);
        assertEquals(NumberOp.EQ, c.op(), "a term on a numeric field is an equality, not a text match");
        assertEquals("195.08", c.value1());
    }

    @Test
    public void testOpenEndedRangesMapToComparisons() {
        assertEquals(NumberOp.GTE, ((QueryClause) parse("ionMass:[300 TO *]").items().get(0)).op());
        assertEquals(NumberOp.GT, ((QueryClause) parse("ionMass:{300 TO *}").items().get(0)).op());
        assertEquals(NumberOp.LTE, ((QueryClause) parse("ionMass:[* TO 400]").items().get(0)).op());
        assertEquals(NumberOp.LT, ((QueryClause) parse("ionMass:{* TO 400}").items().get(0)).op());
        QueryClause gte = (QueryClause) parse("ionMass:[300 TO *]").items().get(0);
        assertEquals("300", gte.value1(), "the bound is carried in value1");
    }

    @Test
    public void testGroupAndConnectors() {
        QueryContainer root = parse("(quality:GOOD OR quality:DECENT) AND hasMsMs:true");
        assertEquals(2, root.items().size());
        assertEquals(List.of(LogicOp.AND), root.logics());
        QueryGroup group = (QueryGroup) root.items().get(0);
        assertEquals(2, group.items().size());
        assertEquals(List.of(LogicOp.OR), group.logics());
        assertInstanceOf(QueryClause.class, root.items().get(1));
    }

    @Test
    public void testNegation() {
        assertTrue(parse("NOT hasMsMs:true").items().get(0).negated());
    }

    @Test
    public void testKeylessTermIsFreeText() {
        QueryClause c = (QueryClause) parse("caffeine").items().get(0);
        assertTrue(c.isFreeText());
        assertEquals("caffeine", c.value1());
    }

    @Test
    public void testBlankQueryIsEmptyContainer() {
        assertTrue(QueryStringParser.parse("   ", FIELDS).orElseThrow().isEmpty());
    }

    @Test
    public void testInvalidSyntaxReturnsEmpty() {
        assertTrue(QueryStringParser.parse("ionMass:[300 TO", FIELDS).isEmpty(), "unbalanced range");
        assertTrue(QueryStringParser.parse("((", FIELDS).isEmpty(), "unbalanced parens");
    }

    @Test
    public void testUnsupportedConstructReturnsEmpty() {
        // fuzzy / boost are valid lucene but not part of our chip model -> fall back to plain text
        assertTrue(QueryStringParser.parse("name:caffeine~", FIELDS).isEmpty(), "fuzzy");
    }
}
