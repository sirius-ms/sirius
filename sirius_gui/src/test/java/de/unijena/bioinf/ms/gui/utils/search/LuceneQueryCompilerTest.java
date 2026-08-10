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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for compiling the search-bar query tree into a lucene query string. The behavior mirrors the
 * javascript search bar (LuceneChemicalSearchBar.tsx), extended by escaping rules for values that would
 * otherwise break the lucene syntax (whitespace, colons in time values, ...).
 */
public class LuceneQueryCompilerTest {

    private static QueryContainer container(QueryNode... nodes) {
        // joined with AND by default like in the UI
        List<LogicOp> logics = nodes.length > 1
                ? java.util.Collections.nCopies(nodes.length - 1, LogicOp.AND)
                : List.of();
        return new QueryContainer(List.of(nodes), logics);
    }

    // --- text clauses & value escaping ---

    @Test
    public void testSimpleTermStaysBare() {
        assertEquals("quality:GOOD", LuceneQueryCompiler.render(QueryClause.text("quality", "GOOD", false)));
    }

    @Test
    public void testWildcardsAndFuzzyAreKeptUsable() {
        assertEquals("name:pyro*", LuceneQueryCompiler.render(QueryClause.text("name", "pyro*", false)));
        assertEquals("name:p?ro", LuceneQueryCompiler.render(QueryClause.text("name", "p?ro", false)));
        assertEquals("name:pyrofos~", LuceneQueryCompiler.render(QueryClause.text("name", "pyrofos~", false)));
    }

    @Test
    public void testWhitespaceValueIsQuoted() {
        assertEquals("name:\"Bicuculline methiodide\"",
                LuceneQueryCompiler.render(QueryClause.text("name", "Bicuculline methiodide", false)));
    }

    @Test
    public void testQuoteInValueIsEscapedInsideQuotes() {
        assertEquals("name:\"the \\\"best\\\" hit\"",
                LuceneQueryCompiler.render(QueryClause.text("name", "the \"best\" hit", false)));
    }

    @Test
    public void testColonInBareValueIsEscaped() {
        // time tag values contain colons that must not be read as field separators
        assertEquals("tags.time:12\\:00\\:00",
                LuceneQueryCompiler.render(QueryClause.text("tags.time", "12:00:00", false)));
    }

    @Test
    public void testGroupingCharsInBareValueAreEscaped() {
        assertEquals("detectedAdducts:\\[M+H\\]+",
                LuceneQueryCompiler.render(QueryClause.text("detectedAdducts", "[M+H]+", false)));
    }

    // --- numeric clauses ---

    @Test
    public void testNumericOperatorShapes() {
        assertEquals("ionMass:300", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.EQ, "300", null, false)));
        assertEquals("ionMass:{* TO 300}", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.LT, "300", null, false)));
        assertEquals("ionMass:[* TO 300]", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.LTE, "300", null, false)));
        assertEquals("ionMass:{300 TO *}", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.GT, "300", null, false)));
        assertEquals("ionMass:[300 TO *]", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.GTE, "300", null, false)));
        assertEquals("ionMass:[300 TO 400]", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.RANGE_INCLUSIVE, "300", "400", false)));
        assertEquals("ionMass:{300 TO 400}", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.RANGE_EXCLUSIVE, "300", "400", false)));
    }

    @Test
    public void testMissingRangeBoundsBecomeWildcards() {
        assertEquals("ionMass:[300 TO *]", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.RANGE_INCLUSIVE, "300", "", false)));
        assertEquals("ionMass:[* TO 400]", LuceneQueryCompiler.render(QueryClause.numeric("ionMass", NumberOp.RANGE_INCLUSIVE, "", "400", false)));
    }

    @Test
    public void testTimeRangeBoundsAreEscaped() {
        assertEquals("tags.time:[12\\:00\\:00 TO 14\\:00\\:00]",
                LuceneQueryCompiler.render(QueryClause.numeric("tags.time", NumberOp.RANGE_INCLUSIVE, "12:00:00", "14:00:00", false)));
    }

    // --- negation & groups ---

    @Test
    public void testNegatedClause() {
        assertEquals("NOT quality:BAD", LuceneQueryCompiler.render(QueryClause.text("quality", "BAD", true)));
    }

    @Test
    public void testGroupRendersParenthesizedWithLogic() {
        QueryGroup group = new QueryGroup("g1", false,
                List.of(QueryClause.text("quality", "GOOD", false),
                        QueryClause.numeric("ionMass", NumberOp.GT, "300", null, false)),
                List.of(LogicOp.OR));
        assertEquals("(quality:GOOD OR ionMass:{300 TO *})", LuceneQueryCompiler.render(group));
    }

    @Test
    public void testNegatedGroup() {
        QueryGroup group = new QueryGroup("g1", true,
                List.of(QueryClause.text("quality", "GOOD", false)), List.of());
        assertEquals("NOT (quality:GOOD)", LuceneQueryCompiler.render(group));
    }

    @Test
    public void testEmptyGroupCompilesToNothing() {
        assertEquals("", LuceneQueryCompiler.render(new QueryGroup("g1", false, List.of(), List.of())));
        // and a negated empty group must not become a bare "NOT "
        assertEquals("", LuceneQueryCompiler.render(new QueryGroup("g1", true, List.of(), List.of())));
    }

    @Test
    public void testEmptyChildIsDroppedWithItsJoiningOperator() {
        // an (empty) open group between two clauses must not leave a stray operator behind
        QueryGroup emptyGroup = new QueryGroup("g1", false, List.of(), List.of());
        QueryContainer root = new QueryContainer(
                List.of(QueryClause.text("quality", "GOOD", false), emptyGroup,
                        QueryClause.numeric("ionMass", NumberOp.GT, "300", null, false)),
                List.of(LogicOp.OR, LogicOp.OR));
        assertEquals("quality:GOOD OR ionMass:{300 TO *}", LuceneQueryCompiler.compile(root, ""));
    }

    @Test
    public void testNestedGroups() {
        QueryGroup inner = new QueryGroup("g2", false,
                List.of(QueryClause.text("quality", "GOOD", false),
                        QueryClause.text("quality", "DECENT", false)),
                List.of(LogicOp.OR));
        QueryGroup outer = new QueryGroup("g1", true,
                List.of(QueryClause.numeric("ionMass", NumberOp.LT, "300", null, false), inner),
                List.of(LogicOp.AND));
        assertEquals("NOT (ionMass:{* TO 300} AND (quality:GOOD OR quality:DECENT))",
                LuceneQueryCompiler.render(outer));
    }

    // --- top level compile incl. free text ---

    @Test
    public void testCompileChipsOnly() {
        QueryContainer root = container(
                QueryClause.numeric("ionMass", NumberOp.RANGE_INCLUSIVE, "300", "400", false),
                QueryClause.text("quality", "GOOD", false));
        assertEquals("ionMass:[300 TO 400] AND quality:GOOD", LuceneQueryCompiler.compile(root, ""));
    }

    @Test
    public void testCompileFreeTextOnlyPassesThroughVerbatim() {
        assertEquals("pyro* AND hasMsMs:true", LuceneQueryCompiler.compile(QueryContainer.empty(), "pyro* AND hasMsMs:true"));
    }

    @Test
    public void testCompileChipsAndFreeTextAreBothParenthesized() {
        QueryContainer root = container(QueryClause.text("quality", "GOOD", false));
        assertEquals("(quality:GOOD) AND (caffeine)", LuceneQueryCompiler.compile(root, " caffeine "));
    }

    @Test
    public void testCompileEmptyIsEmptyString() {
        assertEquals("", LuceneQueryCompiler.compile(QueryContainer.empty(), "   "));
    }
}
