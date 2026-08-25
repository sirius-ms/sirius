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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
     * Appends a node joined by {@code logic}, keeping the level homogeneous: when the level already joins
     * by the other connector, the new node is grouped with its neighbour rather than left in a mixed
     * level - the same restructuring {@link #setLogic} does for a flip, so building a query with the
     * autocomplete and rearranging it by clicking end in the same shapes.
     */
    public static PathResult appendJoined(@NotNull QueryContainer root, int[] path, @NotNull QueryNode node,
                                          @NotNull LogicOp logic, boolean allowNesting) {
        QueryContainer container = containerAt(root, path);
        LogicOp prevailing = uniformLogic(container);
        int index = container.items().size();
        if (index == 0 || prevailing == null || prevailing == logic)
            return new PathResult(append(root, path, node, logic), path);
        // append with the level's own connector first, then let the flip decide how to fit the other one
        return normalized(append(root, path, node, prevailing), path, path, index - 1, logic, allowNesting);
    }

    /** {@link #openGroup} with the same treatment of the connector that joins the new group. */
    public static PathResult openGroupJoined(@NotNull QueryContainer root, int[] path, boolean negated,
                                             @NotNull LogicOp logic, boolean allowNesting) {
        QueryContainer container = containerAt(root, path);
        LogicOp prevailing = uniformLogic(container);
        int index = container.items().size();
        if (index == 0 || prevailing == null || prevailing == logic)
            return openGroup(root, path, negated, logic);
        PathResult opened = openGroup(root, path, negated, prevailing);
        return normalized(opened.root(), opened.path(), path, index - 1, logic, allowNesting);
    }

    /** The homogeneity-keeping flip with a cursor carried through it. */
    private static PathResult normalized(QueryContainer root, int[] cursor, int[] containerPath, int index,
                                         LogicOp logic, boolean allowNesting) {
        return setLogic(root, cursor, containerPath, index, logic, allowNesting)
                // a group inside a group is the lesser evil: a level mixing AND and OR does not mean what
                // it reads, while a nesting is merely one brace more than the mode would have offered
                .or(() -> setLogic(root, cursor, containerPath, index, logic, true))
                .orElse(new PathResult(root, cursor));
    }

    /**
     * Sets the connector at {@code index} of the container at {@code containerPath} - and restructures
     * the tree so that no level ends up mixing AND with OR.
     * <p>
     * That restructuring is the point. A level mixing the two is not read by lucene the way the chips
     * show it: {@code a AND b OR c} becomes {@code +a +b c}, i.e. a required pair with an optional
     * extra, not the alternative it looks like. So a flip that would mix instead groups what it joins:
     * {@code a AND b AND c} with its second connector flipped becomes {@code a AND (b OR c)}, where the
     * OR sits in its own boolean query and means what it reads. An adjacent group already using that
     * connector is extended rather than nested into, and a group whose connector ends up agreeing with
     * its parent is redundant and dissolves - which is what makes a flip undoable by flipping back.
     * <p>
     * A negated group is neither extended nor dissolved: {@code NOT (b OR c)} is not {@code NOT b OR
     * NOT c}, and pulling a sibling inside the NOT would change what it negates.
     * <p>
     * {@code openPath} is re-resolved by id like in {@link #removeNode}, so the cursor keeps pointing at
     * the same open group even when the restructuring shifts sibling indices. The open group itself is
     * never dissolved while it is being edited.
     *
     * @param allowNesting whether a group may be created inside another group ({@code QueryMode.ADVANCED});
     *                     without it a flip that could only be resolved by nesting is refused
     * @return the new tree with the re-resolved cursor, or empty when the connector does not exist or the
     * flip would need a nesting that is not allowed - the caller then offers no flip at all
     */
    public static Optional<PathResult> setLogic(@NotNull QueryContainer root, int[] openPath,
                                                int[] containerPath, int index, @NotNull LogicOp logic,
                                                boolean allowNesting) {
        QueryContainer container = containerAt(root, containerPath);
        if (index < 0 || index >= container.logics().size())
            return Optional.empty();
        // a group directly inside the addressed container is one level deeper than that container
        boolean mayGroup = containerPath.length == 0 || allowNesting;
        Optional<QueryContainer> updated = withLogicKeptHomogeneous(container, index, logic, mayGroup);
        if (updated.isEmpty())
            return Optional.empty();

        String openGroupId = groupIdAt(root, openPath);
        QueryContainer newRoot = updateContainer(root, containerPath, ignored -> updated.get());
        // the new connector can make parentheses redundant on either side of the change: a child group
        // that now joins by the same connector as this container, and this container's own group inside
        // its parent
        newRoot = updateContainer(newRoot, containerPath, c -> simplified(c, openGroupId));
        if (containerPath.length > 0)
            newRoot = updateContainer(newRoot, Arrays.copyOf(containerPath, containerPath.length - 1),
                    c -> simplified(c, openGroupId));
        int[] newPath = openGroupId == null
                ? new int[0] : pathOfId(newRoot, openGroupId).orElseGet(() -> new int[0]);
        return Optional.of(new PathResult(newRoot, newPath));
    }

    /**
     * Whether {@link #setLogic} would do something - what the UI asks before offering a flip.
     */
    public static boolean canSetLogic(@NotNull QueryContainer root, int[] containerPath, int index,
                                      @NotNull LogicOp logic, boolean allowNesting) {
        QueryContainer container = containerAt(root, containerPath);
        if (index < 0 || index >= container.logics().size())
            return false;
        return withLogicKeptHomogeneous(container, index, logic,
                containerPath.length == 0 || allowNesting).isPresent();
    }

    /**
     * The container with its connector at {@code index} set to {@code logic}, grouping what that
     * connector joins when the level would otherwise mix connectors. Empty when only a new group could
     * express it and {@code mayGroup} forbids one.
     */
    private static Optional<QueryContainer> withLogicKeptHomogeneous(QueryContainer container, int index,
                                                                     LogicOp logic, boolean mayGroup) {
        List<LogicOp> logics = container.logics();
        // one connector cannot disagree with itself, an already-uniform level stays uniform, and a level
        // that is ALREADY mixed (only possible for a query pasted or hand-edited elsewhere) is not ours
        // to rewrite - in all three cases the connector is simply set
        if (logics.size() == 1 || logics.stream().allMatch(op -> op == logic) || isMixed(logics))
            return Optional.of(withLogicAt(container, index, logic));

        List<QueryNode> items = new ArrayList<>(container.items());
        QueryNode left = items.get(index);
        QueryNode right = items.get(index + 1);

        // an adjacent group that already joins by this connector takes the sibling in - one group for
        // "b OR c OR d" rather than a nest of pairs
        if (right instanceof QueryGroup group && !group.negated() && joinsBy(group, logic))
            return Optional.of(merged(container, index, group, index + 1, left, true));
        if (left instanceof QueryGroup group && !group.negated() && joinsBy(group, logic))
            return Optional.of(merged(container, index, group, index, right, false));

        if (!mayGroup)
            return Optional.empty();
        QueryGroup wrapped = new QueryGroup(QueryNode.nextId("group"), false, List.of(left, right), List.of(logic));
        return Optional.of(replacePair(container, index, wrapped));
    }

    /** Whether every connector of {@code group} is {@code logic} (a group of one joins by nothing). */
    private static boolean joinsBy(QueryGroup group, LogicOp logic) {
        return uniformLogic(group.items().size(), group.logics()) == logic;
    }

    private static boolean isMixed(List<LogicOp> logics) {
        return logics.stream().distinct().count() > 1;
    }

    private static QueryContainer withLogicAt(QueryContainer container, int index, LogicOp logic) {
        List<LogicOp> logics = new ArrayList<>(container.logics());
        logics.set(index, logic);
        return new QueryContainer(container.items(), logics);
    }

    /**
     * The container with {@code sibling} moved into {@code group}, dropping the connector at
     * {@code index} that used to join the two.
     *
     * @param atFront whether the sibling goes before the group's items (it sat to the group's left)
     */
    private static QueryContainer merged(QueryContainer container, int index, QueryGroup group, int groupIndex,
                                         QueryNode sibling, boolean atFront) {
        List<QueryNode> inner = new ArrayList<>(group.items());
        List<LogicOp> innerLogics = new ArrayList<>(group.logics());
        LogicOp joint = group.logics().get(0);
        inner.add(atFront ? 0 : inner.size(), sibling);
        innerLogics.add(joint);

        List<QueryNode> items = new ArrayList<>(container.items());
        List<LogicOp> logics = new ArrayList<>(container.logics());
        items.set(groupIndex, group.withContent(inner, innerLogics));
        items.remove(atFront ? index : index + 1); // the sibling now lives inside the group
        logics.remove(index);
        return new QueryContainer(items, logics);
    }

    /** The container with the two items at {@code index} replaced by the group wrapping them. */
    private static QueryContainer replacePair(QueryContainer container, int index, QueryGroup wrapped) {
        List<QueryNode> items = new ArrayList<>(container.items());
        List<LogicOp> logics = new ArrayList<>(container.logics());
        items.set(index, wrapped);
        items.remove(index + 1);
        logics.remove(index);
        return new QueryContainer(items, logics);
    }

    /**
     * The container with the parentheses that no longer say anything removed: a child group that joins by
     * the very connector this container uses anyway, or a lone child group - {@code (a OR b) OR c} is
     * {@code a OR b OR c}, and a group that IS the whole query needs no braces.
     * <p>
     * Left alone: a negated group (its NOT would have nowhere to go), the group being edited (the cursor
     * sits in it), a group that is itself mixed inside, and every group in a container that is already
     * mixed - a mix can only come from outside the builder, and rewriting it would rewrite what someone
     * else wrote.
     */
    private static QueryContainer simplified(QueryContainer container, @Nullable String openGroupId) {
        if (container.items().size() == 1) {
            QueryGroup sole = dissolvable(container.items().get(0), null, openGroupId);
            return sole == null ? container : new QueryContainer(sole.items(), sole.logics());
        }
        LogicOp uniform = uniformLogic(container);
        if (uniform == null)
            return container;

        List<QueryNode> items = new ArrayList<>(container.items().size());
        List<LogicOp> logics = new ArrayList<>(container.logics().size());
        for (int i = 0; i < container.items().size(); i++) {
            if (i > 0)
                logics.add(container.logics().get(i - 1));
            QueryGroup redundant = dissolvable(container.items().get(i), uniform, openGroupId);
            if (redundant == null) {
                items.add(container.items().get(i));
                continue;
            }
            items.addAll(redundant.items());
            // the group's own connectors, all `uniform` by now, take over the span it occupied
            for (int k = 1; k < redundant.items().size(); k++)
                logics.add(uniform);
        }
        return new QueryContainer(items, logics);
    }

    /**
     * {@code node} as the group whose parentheses can go, or null when they cannot.
     *
     * @param joining the connector the surrounding container uses, or null to accept any uniform group
     *                (a lone child, whose braces say nothing whatever it joins by)
     */
    @Nullable
    private static QueryGroup dissolvable(QueryNode node, @Nullable LogicOp joining, @Nullable String openGroupId) {
        if (!(node instanceof QueryGroup group) || group.negated() || group.items().size() < 2
                || group.id().equals(openGroupId))
            return null;
        LogicOp inner = uniformLogic(group.items().size(), group.logics());
        if (inner == null || (joining != null && inner != joining))
            return null;
        return group;
    }

    /** The one connector a container joins everything by, or null when it mixes them (or has none). */
    @Nullable
    private static LogicOp uniformLogic(QueryContainer container) {
        return uniformLogic(container.items().size(), container.logics());
    }

    @Nullable
    private static LogicOp uniformLogic(int itemCount, List<LogicOp> logics) {
        if (logics.isEmpty() || logics.size() != itemCount - 1 || isMixed(logics))
            return null;
        return logics.get(0);
    }

    /**
     * Removes the node {@code nodeId} and re-resolves {@code openPath} so it still points at the SAME
     * open group afterwards - located by id, not by position, so a removal that shifts sibling indices
     * (e.g. deleting a chip that sits before the open group) cannot silently close or mis-target it.
     * The open group is also protected from the single-child auto-unpack while it is being edited, so
     * removing one of its two children leaves it open rather than dissolving it under the cursor.
     * Returns the new tree and the corrected cursor ({@code []} when the open group no longer exists).
     */
    public static PathResult removeNode(@NotNull QueryContainer root, int[] openPath, @NotNull String nodeId) {
        String openGroupId = groupIdAt(root, openPath);
        QueryContainer updated = removeNodeById(root, nodeId, openGroupId);
        int[] newPath = openGroupId == null ? new int[0] : pathOfId(updated, openGroupId).orElseGet(() -> new int[0]);
        return new PathResult(updated, newPath);
    }

    /**
     * Drops the node with {@code nodeId} from anywhere in the tree, along with the operator that
     * joined it. Groups are removed whole; a nested group reduced to a single child by the removal
     * auto-unpacks (negation XOR), mirroring what {@link #closeGroup} does.
     */
    public static QueryContainer removeNodeById(@NotNull QueryContainer root, @NotNull String nodeId) {
        return removeNodeById(root, nodeId, null);
    }

    /**
     * As {@link #removeNodeById(QueryContainer, String)}, but never auto-unpacks the group whose id is
     * {@code protectedGroupId} (the one currently being edited) - it stays a group even with one child.
     */
    public static QueryContainer removeNodeById(@NotNull QueryContainer root, @NotNull String nodeId,
                                                @Nullable String protectedGroupId) {
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
            QueryContainer inner = removeNodeById(new QueryContainer(group.items(), group.logics()), nodeId, protectedGroupId);
            if (inner.items().size() == 1 && !group.id().equals(protectedGroupId)) {
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

    /** The id of the group addressed by {@code path} ({@code null} for the top level or an invalid path). */
    @Nullable
    private static String groupIdAt(@NotNull QueryContainer root, int[] path) {
        List<QueryNode> level = root.items();
        QueryGroup group = null;
        for (int idx : path) {
            if (idx < 0 || idx >= level.size() || !(level.get(idx) instanceof QueryGroup g))
                return null;
            group = g;
            level = g.items();
        }
        return group == null ? null : group.id();
    }

    /** The item-index path to the node with {@code id} anywhere in the tree, or empty if it is gone. */
    public static Optional<int[]> pathOfId(@NotNull QueryContainer root, @NotNull String id) {
        return pathOfId(root.items(), id, new int[0]);
    }

    private static Optional<int[]> pathOfId(List<QueryNode> items, String id, int[] prefix) {
        for (int i = 0; i < items.size(); i++) {
            QueryNode node = items.get(i);
            int[] path = Arrays.copyOf(prefix, prefix.length + 1);
            path[prefix.length] = i;
            if (node.id().equals(id))
                return Optional.of(path);
            if (node instanceof QueryGroup group) {
                Optional<int[]> found = pathOfId(group.items(), id, path);
                if (found.isPresent())
                    return found;
            }
        }
        return Optional.empty();
    }
}
