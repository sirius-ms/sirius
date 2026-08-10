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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Immutable operations on the search-bar query tree. Semantics ported from the javascript search
 * bar (LuceneChemicalSearchBar.tsx): the cursor into the currently open group is a path of item
 * indices ({@code []} = top level, {@code [2,0]} = first child of the group at index 2), groups are
 * deleted whole (flattening could not preserve meaning since AND binds tighter than OR and the
 * group's own NOT would have nowhere to go), and a group reduced to a single child unpacks with the
 * negations combining via XOR ({@code NOT (NOT x)} becomes a plain {@code x}).
 */
public final class QueryTreeOps {

    private QueryTreeOps() {
    }

    /**
     * A tree operation that may also move the cursor (open/close group).
     */
    public record PathResult(@NotNull QueryContainer root, int[] path) {
    }

    /**
     * Truncates a cursor path to the longest prefix that still addresses a group, so a deletion
     * can never leave the cursor pointing at something gone.
     */
    public static int[] resolvePath(@NotNull QueryContainer root, int[] path) {
        List<Integer> valid = new ArrayList<>(path.length);
        List<QueryNode> level = root.items();
        for (int idx : path) {
            if (idx < 0 || idx >= level.size() || !(level.get(idx) instanceof QueryGroup group))
                break;
            valid.add(idx);
            level = group.items();
        }
        return valid.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * The container at {@code path} - the level new clauses currently land in. Falls back to the
     * deepest valid level if the path runs into a non-group.
     */
    public static QueryContainer containerAt(@NotNull QueryContainer root, int[] path) {
        QueryContainer current = root;
        for (int idx : path) {
            if (idx < 0 || idx >= current.items().size() || !(current.items().get(idx) instanceof QueryGroup group))
                return current;
            current = new QueryContainer(group.items(), group.logics());
        }
        return current;
    }

    /**
     * Rebuilds the tree with {@code fn} applied to the container at {@code path}. Every nested
     * write goes through here. Unchanged tree if the path does not address a group.
     */
    public static QueryContainer updateContainer(@NotNull QueryContainer root, int[] path,
                                                 @NotNull UnaryOperator<QueryContainer> fn) {
        return updateContainer(root, path, 0, fn);
    }

    private static QueryContainer updateContainer(QueryContainer current, int[] path, int depth,
                                                  UnaryOperator<QueryContainer> fn) {
        if (depth == path.length)
            return fn.apply(current);

        int idx = path[depth];
        if (idx < 0 || idx >= current.items().size() || !(current.items().get(idx) instanceof QueryGroup group))
            return current;

        QueryContainer inner = updateContainer(new QueryContainer(group.items(), group.logics()), path, depth + 1, fn);
        List<QueryNode> items = new ArrayList<>(current.items());
        items.set(idx, group.withContent(inner.items(), inner.logics()));
        return new QueryContainer(items, current.logics());
    }

    /**
     * Appends a node to the container at {@code path}. The joining operator is only recorded when
     * there already is a sibling to join.
     */
    public static QueryContainer append(@NotNull QueryContainer root, int[] path,
                                        @NotNull QueryNode node, @NotNull LogicOp logic) {
        return updateContainer(root, path, container -> {
            List<QueryNode> items = new ArrayList<>(container.items());
            List<LogicOp> logics = new ArrayList<>(container.logics());
            if (!items.isEmpty())
                logics.add(logic);
            items.add(node);
            return new QueryContainer(items, logics);
        });
    }

    /**
     * Inserts an empty group at the cursor and descends into it. Created eagerly (before the first
     * clause) so the open group is visible immediately; empty groups compile to nothing meanwhile.
     */
    public static PathResult openGroup(@NotNull QueryContainer root, int[] path,
                                       boolean negated, @NotNull LogicOp logic) {
        int index = containerAt(root, path).items().size();
        QueryContainer updated = append(root, path, QueryGroup.empty(negated), logic);
        int[] newPath = java.util.Arrays.copyOf(path, path.length + 1);
        newPath[path.length] = index;
        return new PathResult(updated, newPath);
    }

    /**
     * Leaves the innermost group, unpacking it when it gained nothing worth parenthesizing: an
     * empty group is dropped (with the operator that joined it), a single-child group is replaced
     * by its child with the negations combining via XOR.
     */
    public static PathResult closeGroup(@NotNull QueryContainer root, int[] path) {
        if (path.length == 0)
            return new PathResult(root, path);
        int[] parentPath = java.util.Arrays.copyOf(path, path.length - 1);
        int index = path[path.length - 1];

        QueryContainer updated = updateContainer(root, parentPath, container -> {
            if (index < 0 || index >= container.items().size()
                    || !(container.items().get(index) instanceof QueryGroup group)
                    || group.items().size() > 1)
                return container;

            List<QueryNode> items = new ArrayList<>(container.items());
            List<LogicOp> logics = new ArrayList<>(container.logics());
            if (group.items().isEmpty()) {
                // nothing was typed into it - drop it, and the operator that joined it
                items.remove(index);
                if (!logics.isEmpty())
                    logics.remove(index > 0 ? index - 1 : 0);
            } else {
                // a lone child needs no parentheses; negations combine via XOR
                QueryNode child = group.items().get(0);
                items.set(index, child.withNegated(child.negated() != group.negated()));
            }
            return new QueryContainer(items, logics);
        });
        return new PathResult(updated, parentPath);
    }

    /**
     * Drops the node with {@code nodeId} from anywhere in the tree, along with the operator that
     * joined it. Groups are removed whole; a nested group reduced to a single child by the removal
     * auto-unpacks (negation XOR), mirroring what {@link #closeGroup} does.
     */
    public static QueryContainer removeNodeById(@NotNull QueryContainer root, @NotNull String nodeId) {
        int idx = -1;
        for (int i = 0; i < root.items().size(); i++) {
            if (root.items().get(i).id().equals(nodeId)) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            List<QueryNode> items = new ArrayList<>(root.items());
            List<LogicOp> logics = new ArrayList<>(root.logics());
            items.remove(idx);
            if (!logics.isEmpty())
                logics.remove(idx > 0 ? idx - 1 : 0);
            return new QueryContainer(items, logics);
        }

        // not at this level - recurse into the groups
        List<QueryNode> items = new ArrayList<>(root.items().size());
        for (QueryNode node : root.items()) {
            if (!(node instanceof QueryGroup group)) {
                items.add(node);
                continue;
            }
            QueryContainer inner = removeNodeById(new QueryContainer(group.items(), group.logics()), nodeId);
            if (inner.items().size() == 1) {
                // group down to one item auto-unpacks; the negations combine via XOR
                QueryNode child = inner.items().get(0);
                items.add(child.withNegated(child.negated() != group.negated()));
            } else {
                items.add(group.withContent(inner.items(), inner.logics()));
            }
        }
        // the operators at this level are untouched: the deletion happened deeper
        return new QueryContainer(items, root.logics());
    }
}
