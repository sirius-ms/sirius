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
 * Symmetry contract between {@link LuceneQueryCompiler} (AST -> string) and {@link QueryStringParser}
 * (string -> AST): the two must be mutual inverses. This is the guarantee that makes it safe to
 * hydrate a committed query back into chips and re-compile it without drift.
 * <p>
 * The invariant is expressed on the normalisation {@code norm = compile . parse}:
 * <ul>
 *   <li><b>identity</b>: for every canonical string the compiler can emit, {@code norm(s) == s};</li>
 *   <li><b>idempotence</b>: for inputs where Lucene operator precedence adds explicit parentheses
 *       (mixed {@code AND}/{@code OR} on one level), the first pass normalises and every further
 *       pass is stable - {@code norm(norm(s)) == norm(s)}.</li>
 * </ul>
 */
public class QueryCodecSymmetryTest {

    private static SearchableField field(String name, SearchableFieldType type) {
        return new SearchableField().name(name).fieldType(type);
    }

    private static final List<SearchableField> FIELDS = List.of(
            field("ionMass", SearchableFieldType.DOUBLE),
            field("name", SearchableFieldType.TEXT),
            field("quality", SearchableFieldType.ENUM),
            field("hasMsMs", SearchableFieldType.BOOLEAN),
            field("tags.city", SearchableFieldType.TEXT));

    /** compile . parse - the string -> AST -> string normalisation. */
    private static String norm(String s) {
        return LuceneQueryCompiler.compile(
                QueryStringParser.parse(s, FIELDS).orElseThrow(() -> new AssertionError("did not parse: " + s)), "");
    }

    private static void assertIdentity(String s) {
        assertEquals(s, norm(s), "compile(parse(s)) must reproduce s");
    }

    private static void assertIdempotent(String s) {
        assertEquals(norm(s), norm(norm(s)), "normalisation must reach a stable fixpoint");
    }

    /** compile an AST, then require the emitted string to be an exact round-trip fixpoint. */
    private static void assertIdentity(QueryContainer ast) {
        assertIdentity(LuceneQueryCompiler.compile(ast, ""));
    }

    @Test
    public void testStringIdentityAcrossEveryCanonicalShape() {
        assertIdentity("hasMsMs:true");
        assertIdentity("ionMass:195.08");
        assertIdentity("ionMass:[300 TO 400]");
        assertIdentity("ionMass:{300 TO 400}");
        assertIdentity("ionMass:[300 TO *]");
        assertIdentity("ionMass:{300 TO *}");
        assertIdentity("ionMass:[* TO 400]");
        assertIdentity("ionMass:{* TO 400}");
        assertIdentity("quality:GOOD OR quality:DECENT");
        assertIdentity("hasMsMs:true AND quality:GOOD");
        assertIdentity("(quality:GOOD OR quality:DECENT) AND hasMsMs:true");
        assertIdentity("NOT hasMsMs:true");
        assertIdentity("caffeine");
        assertIdentity("name:\"caffeic acid\"");
        assertIdentity("(hasMsMs:true) AND (caffeine)");
        // additional shapes the compiler can emit
        assertIdentity("name:caff*");                       // trailing wildcard
        assertIdentity("name:ca?feine");                    // single-char wildcard
        assertIdentity("tags.city:Jena");                   // dotted (dynamic tag) field
        assertIdentity("NOT (quality:GOOD OR quality:DECENT)"); // negated group
        assertIdentity("(name:a AND name:b) OR name:c");    // explicit precedence group
    }

    @Test
    public void testMixedPrecedenceNormalisesToExplicitGroupsButStaysStable() {
        // AND binds tighter than OR; the parser makes that explicit on the way back
        assertEquals("(name:a AND name:b) OR name:c", norm("name:a AND name:b OR name:c"));
        assertIdempotent("name:a AND name:b OR name:c");
        assertIdempotent("name:a OR name:b AND name:c");
    }

    @Test
    public void testAstSeededRoundTripForNegatedAndNestedGroups() {
        QueryClause a = QueryClause.text("name", "a", false);
        QueryClause b = QueryClause.text("name", "b", false);
        QueryClause c = QueryClause.text("name", "c", false);

        QueryGroup negated = new QueryGroup(QueryNode.nextId("group"), true, List.of(a, b), List.of(LogicOp.OR));
        assertIdentity(new QueryContainer(List.of(negated), List.of()));

        QueryGroup inner = new QueryGroup(QueryNode.nextId("group"), false, List.of(a, b), List.of(LogicOp.AND));
        QueryGroup outer = new QueryGroup(QueryNode.nextId("group"), false, List.of(inner, c), List.of(LogicOp.OR));
        assertIdentity(new QueryContainer(List.of(outer), List.of()));
    }

    @Test
    public void testAstSeededRoundTripForEveryNumberOp() {
        for (NumberOp op : NumberOp.values()) {
            QueryClause clause = op.isRange()
                    ? QueryClause.numeric("ionMass", op, "100", "200", false)
                    : QueryClause.numeric("ionMass", op, "150", null, false);
            assertIdentity(new QueryContainer(List.of(clause), List.of()));
        }
    }
}
