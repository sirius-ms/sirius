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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the immutable tree operations of the search-bar query builder (cursor path into the open
 * group, append, remove, open/close group). Semantics ported from LuceneChemicalSearchBar.tsx.
 */
public class QueryTreeOpsTest {

    private static QueryClause clause(String field, String value) {
        return QueryClause.text(field, value, false);
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
}
