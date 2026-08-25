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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the immutable tree operations of the search-bar query builder (cursor path into the open
 * group, append, remove, open/close group). Semantics ported from LuceneChemicalSearchBar.tsx.
 */
public class QueryTreeOpsTest {

    private static QueryClause clause(String field, String value) {
        return QueryClause.text(field, value, false);
    }

    /** A flat container of text clauses joined by {@code logics}. */
    private static QueryContainer flat(List<LogicOp> logics, String... fields) {
        List<QueryNode> items = new java.util.ArrayList<>(fields.length);
        for (String field : fields)
            items.add(clause(field, "1"));
        return new QueryContainer(items, logics);
    }

    private static List<String> fieldsOf(List<QueryNode> items) {
        return items.stream().map(node -> node instanceof QueryClause clause
                ? clause.field() : "(" + String.join("|", fieldsOf(((QueryGroup) node).items())) + ")").toList();
    }

    // --- setLogic: flipping a connector keeps every level AND-only or OR-only ---

    @Test
    public void testFlippingTheOnlyConnectorJustSetsIt() {
        QueryContainer root = flat(List.of(LogicOp.AND), "a", "b");
        QueryTreeOps.PathResult result = QueryTreeOps.setLogic(
                root, new int[0], new int[0], 0, LogicOp.OR, false).orElseThrow();
        assertEquals(List.of(LogicOp.OR), result.root().logics(), "two items cannot be a mixed level");
        assertEquals(List.of("a", "b"), fieldsOf(result.root().items()), "and nothing needs grouping");
    }

    @Test
    public void testFlippingOneOfSeveralConnectorsGroupsThePair() {
        // a AND b AND c, flip the second connector -> a AND (b OR c): the level stays AND-only, and the
        // OR sits in a group where it means what it reads
        QueryContainer root = flat(List.of(LogicOp.AND, LogicOp.AND), "a", "b", "c");
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[0], 1, LogicOp.OR, false)
                .orElseThrow().root();
        assertEquals(List.of("a", "(b|c)"), fieldsOf(flipped.items()));
        assertEquals(List.of(LogicOp.AND), flipped.logics());
        QueryGroup group = (QueryGroup) flipped.items().get(1);
        assertEquals(List.of(LogicOp.OR), group.logics());
        assertFalse(group.negated());
    }

    @Test
    public void testFlippingTheFirstConnectorGroupsTheFirstPair() {
        QueryContainer root = flat(List.of(LogicOp.AND, LogicOp.AND), "a", "b", "c");
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[0], 0, LogicOp.OR, false)
                .orElseThrow().root();
        assertEquals(List.of("(a|b)", "c"), fieldsOf(flipped.items()));
        assertEquals(List.of(LogicOp.AND), flipped.logics());
    }

    @Test
    public void testFlippingIntoAnAdjacentGroupExtendsItInsteadOfNesting() {
        // a AND (b OR c), flip the connector before the group -> (a OR b OR c) needs no second group
        QueryGroup group = new QueryGroup("g1", false,
                List.of(clause("b", "1"), clause("c", "1")), List.of(LogicOp.OR));
        QueryContainer root = new QueryContainer(List.of(clause("a", "1"), group), List.of(LogicOp.AND));
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[0], 0, LogicOp.OR, false)
                .orElseThrow().root();
        // the group became the container's only item, so its parentheses are redundant and it dissolves
        assertEquals(List.of("a", "b", "c"), fieldsOf(flipped.items()));
        assertEquals(List.of(LogicOp.OR, LogicOp.OR), flipped.logics());
    }

    @Test
    public void testFlippingAfterAGroupExtendsItToo() {
        // (a OR b) AND c, flip the connector after the group -> (a OR b OR c)
        QueryGroup group = new QueryGroup("g1", false,
                List.of(clause("a", "1"), clause("b", "1")), List.of(LogicOp.OR));
        QueryContainer root = new QueryContainer(List.of(group, clause("c", "1")), List.of(LogicOp.AND));
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[0], 0, LogicOp.OR, false)
                .orElseThrow().root();
        assertEquals(List.of("a", "b", "c"), fieldsOf(flipped.items()));
        assertEquals(List.of(LogicOp.OR, LogicOp.OR), flipped.logics());
    }

    @Test
    public void testANegatedGroupIsNeverExtended() {
        // NOT (b OR c) means something else with a inside it - wrap instead
        QueryGroup negated = new QueryGroup("g1", true,
                List.of(clause("b", "1"), clause("c", "1")), List.of(LogicOp.OR));
        QueryContainer root = new QueryContainer(List.of(clause("a", "1"), negated, clause("d", "1")),
                List.of(LogicOp.AND, LogicOp.AND));
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[0], 0, LogicOp.OR, true)
                .orElseThrow().root();
        assertEquals(List.of("(a|(b|c))", "d"), fieldsOf(flipped.items()));
        assertTrue(((QueryGroup) ((QueryGroup) flipped.items().get(0)).items().get(1)).negated(),
                "the negation stays on the group it belonged to");
    }

    @Test
    public void testFlippingBackDissolvesTheGroupItCreated() {
        // the inverse of testFlippingOneOfSeveralConnectorsGroupsThePair, so a flip is undoable
        QueryContainer root = flat(List.of(LogicOp.AND, LogicOp.AND), "a", "b", "c");
        QueryContainer grouped = QueryTreeOps.setLogic(root, new int[0], new int[0], 1, LogicOp.OR, false)
                .orElseThrow().root();
        QueryContainer back = QueryTreeOps.setLogic(grouped, new int[0], new int[]{1}, 0, LogicOp.AND, false)
                .orElseThrow().root();
        assertEquals(List.of("a", "b", "c"), fieldsOf(back.items()), "a group that agrees with its parent goes");
        assertEquals(List.of(LogicOp.AND, LogicOp.AND), back.logics());
    }

    @Test
    public void testAGroupThatStillDiffersFromItsParentStays() {
        // (b OR c OR d) inside an AND parent: flipping one of its connectors to AND would need a nested
        // group, which is refused without nesting ...
        QueryGroup group = new QueryGroup("g1", false,
                List.of(clause("b", "1"), clause("c", "1"), clause("d", "1")), List.of(LogicOp.OR, LogicOp.OR));
        QueryContainer root = new QueryContainer(List.of(clause("a", "1"), group), List.of(LogicOp.AND));
        assertTrue(QueryTreeOps.setLogic(root, new int[0], new int[]{1}, 0, LogicOp.AND, false).isEmpty());
        // ... and nests when it is allowed
        QueryContainer nested = QueryTreeOps.setLogic(root, new int[0], new int[]{1}, 0, LogicOp.AND, true)
                .orElseThrow().root();
        assertEquals(List.of("a", "((b|c)|d)"), fieldsOf(nested.items()));
    }

    @Test
    public void testANegatedGroupIsNeverDissolved() {
        // NOT (b AND c) is not the same as NOT b AND NOT c
        QueryGroup negated = new QueryGroup("g1", true,
                List.of(clause("b", "1"), clause("c", "1")), List.of(LogicOp.OR));
        QueryContainer root = new QueryContainer(List.of(clause("a", "1"), negated), List.of(LogicOp.AND));
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[]{1}, 0, LogicOp.AND, false)
                .orElseThrow().root();
        assertEquals(List.of("a", "(b|c)"), fieldsOf(flipped.items()), "the group stays");
        assertEquals(List.of(LogicOp.AND), ((QueryGroup) flipped.items().get(1)).logics());
    }

    @Test
    public void testTheOpenGroupIsNotDissolvedUnderTheCursor() {
        QueryGroup group = new QueryGroup("g1", false,
                List.of(clause("b", "1"), clause("c", "1")), List.of(LogicOp.OR));
        QueryContainer root = new QueryContainer(List.of(clause("a", "1"), group), List.of(LogicOp.AND));
        QueryTreeOps.PathResult result = QueryTreeOps.setLogic(root, new int[]{1}, new int[]{1}, 0,
                LogicOp.AND, false).orElseThrow();
        assertEquals(List.of("a", "(b|c)"), fieldsOf(result.root().items()),
                "it is still being edited - dissolving it would move the cursor out from under the user");
        assertArrayEquals(new int[]{1}, result.path());
    }

    @Test
    public void testTheCursorSurvivesAFlipThatShiftsIndices() {
        // a AND b AND (open group): grouping a and b shifts the open group from index 2 to index 1
        QueryContainer root = flat(List.of(LogicOp.AND, LogicOp.AND), "a", "b", "c");
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(root, new int[0], false, LogicOp.AND);
        assertArrayEquals(new int[]{3}, opened.path());

        QueryTreeOps.PathResult flipped = QueryTreeOps.setLogic(opened.root(), opened.path(), new int[0], 0,
                LogicOp.OR, false).orElseThrow();
        assertArrayEquals(new int[]{2}, flipped.path(), "the cursor follows the group it points at");
        assertEquals(List.of("(a|b)", "c", "()"), fieldsOf(flipped.root().items()));
    }

    @Test
    public void testAnAlreadyMixedLevelIsLeftAsItIs() {
        // a mix can only come from outside the builder (a pasted or hand-edited query); normalising it
        // would rewrite what the user wrote, so the flip is just applied
        QueryContainer root = flat(List.of(LogicOp.AND, LogicOp.OR), "a", "b", "c");
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[0], 0, LogicOp.OR, false)
                .orElseThrow().root();
        assertEquals(List.of("a", "b", "c"), fieldsOf(flipped.items()));
        assertEquals(List.of(LogicOp.OR, LogicOp.OR), flipped.logics());
    }

    @Test
    public void testFlippingToWhatItAlreadyIsChangesNothing() {
        QueryContainer root = flat(List.of(LogicOp.AND, LogicOp.AND), "a", "b", "c");
        QueryContainer flipped = QueryTreeOps.setLogic(root, new int[0], new int[0], 1, LogicOp.AND, false)
                .orElseThrow().root();
        assertEquals(List.of("a", "b", "c"), fieldsOf(flipped.items()));
        assertEquals(List.of(LogicOp.AND, LogicOp.AND), flipped.logics());
    }

    @Test
    public void testAnOutOfRangeConnectorIsRefused() {
        QueryContainer root = flat(List.of(LogicOp.AND), "a", "b");
        assertTrue(QueryTreeOps.setLogic(root, new int[0], new int[0], 1, LogicOp.OR, false).isEmpty());
        assertTrue(QueryTreeOps.setLogic(root, new int[0], new int[0], -1, LogicOp.OR, false).isEmpty());
    }

    // --- appendJoined / openGroupJoined: the autocomplete takes the same route as a flip ---

    @Test
    public void testAppendingWithTheOtherConnectorGroupsItWithItsNeighbour() {
        // a AND b, then "OR c" from the suggestion list -> a AND (b OR c), exactly what flipping the
        // second connector would have produced: one rule, whichever way the user gets there
        QueryContainer root = flat(List.of(LogicOp.AND), "a", "b");
        QueryTreeOps.PathResult result = QueryTreeOps.appendJoined(root, new int[0], clause("c", "1"),
                LogicOp.OR, false);
        assertEquals(List.of("a", "(b|c)"), fieldsOf(result.root().items()));
        assertEquals(List.of(LogicOp.AND), result.root().logics());
    }

    @Test
    public void testAppendingWithTheLevelsOwnConnectorJustAppends() {
        QueryContainer root = flat(List.of(LogicOp.AND), "a", "b");
        QueryContainer appended = QueryTreeOps.appendJoined(root, new int[0], clause("c", "1"),
                LogicOp.AND, false).root();
        assertEquals(List.of("a", "b", "c"), fieldsOf(appended.items()));
        assertEquals(List.of(LogicOp.AND, LogicOp.AND), appended.logics());

        // ... and the very first item has no connector to disagree with
        QueryContainer first = QueryTreeOps.appendJoined(QueryContainer.empty(), new int[0], clause("a", "1"),
                LogicOp.OR, false).root();
        assertEquals(List.of("a"), fieldsOf(first.items()));
        assertTrue(first.logics().isEmpty());
    }

    @Test
    public void testOpeningAGroupWithTheOtherConnectorWrapsItAndKeepsTheCursorInside() {
        QueryContainer root = flat(List.of(LogicOp.AND), "a", "b");
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroupJoined(root, new int[0], false, LogicOp.OR, true);
        // a AND (b OR ( )) - the new group is where typing continues
        assertEquals(List.of("a", "(b|())"), fieldsOf(opened.root().items()));
        assertArrayEquals(new int[]{1, 1}, opened.path());
        QueryContainer typed = QueryTreeOps.append(opened.root(), opened.path(), clause("c", "1"), LogicOp.AND);
        assertEquals(List.of("a", "(b|(c))"), fieldsOf(typed.items()));
    }

    // --- append & cursor path ---

    @Test
    public void testAppendAtTopLevelJoinsWithLogicOnlyWhenSiblingExists() {
        QueryContainer root = QueryContainer.empty();
        root = QueryTreeOps.append(root, new int[0], clause("a", "1"), LogicOp.OR);
        assertEquals(0, root.logics().size(), "first item must not record a joining operator");

        root = QueryTreeOps.append(root, new int[0], clause("b", "2"), LogicOp.OR);
        assertEquals(List.of(LogicOp.OR), root.logics());
        assertEquals(2, root.items().size());
    }

    @Test
    public void testOpenGroupDescendsAndAppendLandsInside() {
        QueryContainer root = QueryContainer.empty();
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(root, new int[0], false, LogicOp.AND);
        assertArrayEquals(new int[]{0}, opened.path());

        QueryContainer withClause = QueryTreeOps.append(opened.root(), opened.path(), clause("a", "1"), LogicOp.AND);
        QueryGroup group = (QueryGroup) withClause.items().get(0);
        assertEquals(1, group.items().size());
        assertEquals("a", ((QueryClause) group.items().get(0)).field());
    }

    @Test
    public void testResolvePathTruncatesWhenNodeIsGone() {
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(QueryContainer.empty(), new int[0], false, LogicOp.AND);
        QueryContainer root = opened.root();
        // group deleted -> path [0] no longer addresses a group
        QueryContainer removed = QueryTreeOps.removeNodeById(root, root.items().get(0).id());
        assertArrayEquals(new int[0], QueryTreeOps.resolvePath(removed, opened.path()));
    }

    @Test
    public void testContainerAtFollowsNestedGroups() {
        QueryTreeOps.PathResult g1 = QueryTreeOps.openGroup(QueryContainer.empty(), new int[0], false, LogicOp.AND);
        QueryTreeOps.PathResult g2 = QueryTreeOps.openGroup(g1.root(), g1.path(), false, LogicOp.AND);
        assertArrayEquals(new int[]{0, 0}, g2.path());

        QueryContainer withClause = QueryTreeOps.append(g2.root(), g2.path(), clause("a", "1"), LogicOp.AND);
        QueryContainer inner = QueryTreeOps.containerAt(withClause, g2.path());
        assertEquals(1, inner.items().size());
    }

    // --- removal ---

    @Test
    public void testRemoveFirstNodeDropsFirstLogic() {
        QueryContainer root = new QueryContainer(
                List.of(clause("a", "1"), clause("b", "2"), clause("c", "3")),
                List.of(LogicOp.OR, LogicOp.AND));
        QueryContainer removed = QueryTreeOps.removeNodeById(root, root.items().get(0).id());
        assertEquals(2, removed.items().size());
        assertEquals(List.of(LogicOp.AND), removed.logics());
        assertEquals("b", ((QueryClause) removed.items().get(0)).field());
    }

    @Test
    public void testRemoveMiddleNodeDropsTheLogicJoiningItToItsPredecessor() {
        // a OR b AND c - removing b drops the OR that joined b to a, leaving "a AND c"
        QueryContainer root = new QueryContainer(
                List.of(clause("a", "1"), clause("b", "2"), clause("c", "3")),
                List.of(LogicOp.OR, LogicOp.AND));
        QueryContainer removed = QueryTreeOps.removeNodeById(root, root.items().get(1).id());
        assertEquals(List.of(LogicOp.AND), removed.logics());
        assertEquals("c", ((QueryClause) removed.items().get(1)).field());
    }

    @Test
    public void testRemoveInsideGroupUnpacksLoneChildWithNegationXor() {
        // NOT (a AND NOT b) -- removing a must unpack to a plain b:
        // the group's NOT and the child's NOT cancel out (XOR)
        QueryClause a = clause("a", "1");
        QueryClause notB = QueryClause.text("b", "2", true);
        QueryGroup group = new QueryGroup("g", true, List.of(a, notB), List.of(LogicOp.AND));
        QueryContainer root = new QueryContainer(List.of(group), List.of());

        QueryContainer removed = QueryTreeOps.removeNodeById(root, a.id());
        QueryClause unpacked = (QueryClause) removed.items().get(0);
        assertEquals("b", unpacked.field());
        assertFalse(unpacked.negated(), "NOT(NOT b) must unpack to plain b");
    }

    @Test
    public void testRemoveGroupRemovesItWhole() {
        QueryGroup group = new QueryGroup("g", false,
                List.of(clause("a", "1"), clause("b", "2")), List.of(LogicOp.OR));
        QueryContainer root = new QueryContainer(List.of(clause("c", "3"), group), List.of(LogicOp.AND));

        QueryContainer removed = QueryTreeOps.removeNodeById(root, "g");
        assertEquals(1, removed.items().size());
        assertEquals(0, removed.logics().size());
        assertEquals("c", ((QueryClause) removed.items().get(0)).field());
    }

    // --- closing groups ---

    @Test
    public void testCloseEmptyGroupDropsIt() {
        QueryContainer root = QueryTreeOps.append(QueryContainer.empty(), new int[0], clause("a", "1"), LogicOp.AND);
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(root, new int[0], false, LogicOp.OR);
        assertEquals(2, opened.root().items().size());

        QueryTreeOps.PathResult closed = QueryTreeOps.closeGroup(opened.root(), opened.path());
        assertArrayEquals(new int[0], closed.path());
        assertEquals(1, closed.root().items().size(), "empty group must be dropped on close");
        assertEquals(0, closed.root().logics().size(), "the operator that joined the group must be dropped too");
    }

    @Test
    public void testCloseSingleChildGroupUnpacksWithNegationXor() {
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(QueryContainer.empty(), new int[0], true, LogicOp.AND);
        QueryContainer withChild = QueryTreeOps.append(opened.root(), opened.path(),
                QueryClause.text("a", "1", true), LogicOp.AND);

        QueryTreeOps.PathResult closed = QueryTreeOps.closeGroup(withChild, opened.path());
        QueryClause unpacked = (QueryClause) closed.root().items().get(0);
        assertEquals("a", unpacked.field());
        assertFalse(unpacked.negated(), "NOT (NOT a) must close to a plain a");
    }

    @Test
    public void testCloseMultiChildGroupKeepsIt() {
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(QueryContainer.empty(), new int[0], false, LogicOp.AND);
        QueryContainer c = QueryTreeOps.append(opened.root(), opened.path(), clause("a", "1"), LogicOp.AND);
        c = QueryTreeOps.append(c, opened.path(), clause("b", "2"), LogicOp.OR);

        QueryTreeOps.PathResult closed = QueryTreeOps.closeGroup(c, opened.path());
        assertArrayEquals(new int[0], closed.path());
        assertInstanceOf(QueryGroup.class, closed.root().items().get(0));
        assertEquals(2, ((QueryGroup) closed.root().items().get(0)).items().size());
    }

    // --- removeNode: keeps the open group under the cursor across index shifts / auto-unpack ---

    /** Removing a chip that sits BEFORE the open group must not close or mis-target it (Bug: the old
     *  resolvePath only truncated the stale positional path and never accounted for the index shift). */
    @Test
    public void testRemoveNodeBeforeOpenGroupReResolvesCursorById() {
        QueryContainer root = QueryTreeOps.append(QueryContainer.empty(), new int[0], clause("a", "1"), LogicOp.AND);
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(root, new int[0], false, LogicOp.AND); // group at index 1
        root = QueryTreeOps.append(opened.root(), opened.path(), clause("b", "2"), LogicOp.AND);
        String openGroupId = root.items().get(1).id();
        assertArrayEquals(new int[]{1}, opened.path());

        // delete the leading clause "a" -> the group shifts to index 0; the cursor must follow it
        QueryTreeOps.PathResult removed = QueryTreeOps.removeNode(root, opened.path(), root.items().get(0).id());
        assertEquals(1, removed.root().items().size());
        assertInstanceOf(QueryGroup.class, removed.root().items().get(0));
        assertEquals(openGroupId, removed.root().items().get(0).id(), "same group, now at index 0");
        assertArrayEquals(new int[]{0}, removed.path(), "cursor re-resolved to the group by id");
    }

    /** Removing one of the two children of the OPEN group must leave it open (a group, one child) -
     *  the open group is protected from the single-child auto-unpack while it is being edited. */
    @Test
    public void testRemoveChildOfOpenGroupKeepsItOpen() {
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(QueryContainer.empty(), new int[0], false, LogicOp.AND);
        QueryContainer root = QueryTreeOps.append(opened.root(), opened.path(), clause("a", "1"), LogicOp.AND);
        root = QueryTreeOps.append(root, opened.path(), clause("b", "2"), LogicOp.OR);
        String childToRemove = ((QueryGroup) root.items().get(0)).items().get(0).id();

        QueryTreeOps.PathResult removed = QueryTreeOps.removeNode(root, opened.path(), childToRemove);
        assertInstanceOf(QueryGroup.class, removed.root().items().get(0), "open group is NOT unpacked to its lone child");
        assertEquals(1, ((QueryGroup) removed.root().items().get(0)).items().size());
        assertArrayEquals(new int[]{0}, removed.path(), "cursor stays in the open group");
    }

    /** A non-open group reduced to one child by a removal still auto-unpacks (unchanged behavior). */
    @Test
    public void testRemoveChildOfNonOpenGroupUnpacksIt() {
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(QueryContainer.empty(), new int[0], false, LogicOp.AND);
        QueryContainer root = QueryTreeOps.append(opened.root(), opened.path(), clause("a", "1"), LogicOp.AND);
        root = QueryTreeOps.append(root, opened.path(), clause("b", "2"), LogicOp.OR);
        // the cursor is NOT in this group (top level); removing a child should collapse it to its lone child
        String childToRemove = ((QueryGroup) root.items().get(0)).items().get(1).id();

        QueryTreeOps.PathResult removed = QueryTreeOps.removeNode(root, new int[0], childToRemove);
        assertInstanceOf(QueryClause.class, removed.root().items().get(0), "closed group unpacks to its lone child");
        assertEquals("a", ((QueryClause) removed.root().items().get(0)).field());
    }

    @Test
    public void testPathOfIdFindsNestedNode() {
        QueryTreeOps.PathResult opened = QueryTreeOps.openGroup(QueryContainer.empty(), new int[0], false, LogicOp.AND);
        QueryContainer root = QueryTreeOps.append(opened.root(), opened.path(), clause("a", "1"), LogicOp.AND);
        String nestedId = ((QueryGroup) root.items().get(0)).items().get(0).id();
        assertArrayEquals(new int[]{0, 0}, QueryTreeOps.pathOfId(root, nestedId).orElseThrow());
        assertTrue(QueryTreeOps.pathOfId(root, "does-not-exist").isEmpty());
    }
}
