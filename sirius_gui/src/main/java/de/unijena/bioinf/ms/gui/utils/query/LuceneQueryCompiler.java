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

package de.unijena.bioinf.ms.gui.utils.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles the search-bar query tree into a lucene query string. Semantics ported from the
 * javascript search bar (LuceneChemicalSearchBar.tsx): empty groups compile to nothing (an open
 * group must never emit invalid {@code ()}), children that compile to nothing are dropped together
 * with their joining operator, and the free-text segment passes through verbatim, ANDed with the
 * structured part.
 * <p>
 * Extension over the javascript bar: values are escaped so they cannot break the lucene syntax.
 * Bare values keep the wildcard/fuzzy characters ({@code * ? ~}) usable and escape structural
 * characters (colons in time values, brackets in adducts); values with whitespace or quotes are
 * quoted.
 * <p>
 * This is one half of a codec: {@code de.unijena.bioinf.ms.gui.utils.search.QueryStringParser} is
 * the inverse (string -> tree). The two are proven mutual inverses by {@code QueryCodecSymmetryTest}
 * (compiled output re-parses and re-compiles unchanged), which is what makes it safe to hydrate a
 * committed query string back into editable chips. A dedicated lucene-based serializer was evaluated
 * and rejected: Lucene's flexible {@code QueryNode#toQueryString} is a debug form that mangles ranges
 * ({@code [field:a field:b]}) and keyless terms ({@code :term}), and its only canonical path requires
 * analysis (tokenization) we deliberately avoid.
 */
public final class LuceneQueryCompiler {

    private LuceneQueryCompiler() {
    }

    /**
     * The full query of the user segment: the structured clauses plus the free text, both optional.
     * Empty string if there is nothing to search.
     */
    public static String compile(@NotNull QueryContainer root, @NotNull String freeText) {
        String compiled = renderContainer(root.items(), root.logics());
        String text = freeText.trim();
        if (!compiled.isEmpty() && !text.isEmpty())
            return "(" + compiled + ") AND (" + text + ")";
        return compiled.isEmpty() ? text : compiled;
    }

    /**
     * The query as it must be EXECUTED against the index: {@link #compile}, plus a match-all anchor
     * where lucene needs one.
     * <p>
     * A lucene boolean query built only from negations matches nothing - {@code NOT lipid:true} returns
     * zero features instead of "everything that is not a lipid". Such a query therefore has to be
     * anchored as {@code *:* AND NOT lipid:true}. The anchor must sit in the same boolean query as the
     * negations, not around them: {@code (NOT lipid:true) AND (name:caffeine)} matches nothing as well,
     * while {@code (*:* AND NOT lipid:true) AND (name:caffeine)} is correct. An OR of negations cannot
     * share a single anchor either, since each alternative is its own boolean query.
     * <p>
     * Only the executed query is anchored; {@link #compile} keeps the plain form used for the chips and
     * for the query-string codec.
     */
    public static String compileExecutable(@NotNull QueryContainer root, @NotNull String freeText) {
        return compile(anchorNegations(root), freeText);
    }

    /**
     * The container with a match-all anchor added if all its items are negated, otherwise unchanged
     * (a single positive item already anchors the query for all of them).
     */
    private static QueryContainer anchorNegations(@NotNull QueryContainer root) {
        if (root.isEmpty() || root.items().stream().anyMatch(item -> !item.negated()))
            return root;

        if (root.logics().stream().allMatch(op -> op == LogicOp.AND)) {
            // conjunction: one leading anchor covers every negation
            List<QueryNode> items = new ArrayList<>(root.items().size() + 1);
            items.add(QueryClause.matchAll());
            items.addAll(root.items());
            List<LogicOp> logics = new ArrayList<>(root.logics().size() + 1);
            logics.add(LogicOp.AND);
            logics.addAll(root.logics());
            return new QueryContainer(items, logics);
        }

        // disjunction: anchor each alternative on its own
        return new QueryContainer(root.items().stream()
                .<QueryNode>map(item -> new QueryGroup(QueryNode.nextId("group"), false,
                        List.of(QueryClause.matchAll(), item), List.of(LogicOp.AND)))
                .toList(), root.logics());
    }

    /**
     * One node as lucene, empty string when it compiles to nothing (empty groups).
     */
    public static String render(@NotNull QueryNode node) {
        String core;
        if (node instanceof QueryClause clause) {
            core = renderClause(clause);
        } else {
            QueryGroup group = (QueryGroup) node;
            String content = renderContainer(group.items(), group.logics());
            if (content.isEmpty())
                return ""; // never emit "()" or a bare "NOT "
            core = "(" + content + ")";
        }
        return node.negated() ? "NOT " + core : core;
    }

    private static String renderContainer(@NotNull List<QueryNode> items, @NotNull List<LogicOp> logics) {
        // skip children that compile to nothing and drop their joining operator with them,
        // so no stray leading/duplicate AND/OR is emitted
        List<String> parts = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            String rendered = render(items.get(i));
            if (rendered.isEmpty())
                continue;
            if (parts.isEmpty())
                parts.add(rendered);
            else
                parts.add((i > 0 && i - 1 < logics.size() ? logics.get(i - 1) : LogicOp.AND) + " " + rendered);
        }
        return String.join(" ", parts);
    }

    private static String renderClause(@NotNull QueryClause clause) {
        if (clause.op() == null) {
            String value = escapeValue(clause.value1().trim());
            // keyless clause -> bare term matched against the default search fields
            return clause.field().isEmpty() ? value : clause.field() + ":" + value;
        }

        String v1 = orWildcard(clause.value1());
        String v2 = orWildcard(clause.value2());
        return clause.field() + ":" + switch (clause.op()) {
            case EQ -> v1;
            case LT -> "{* TO " + v1 + "}";
            case LTE -> "[* TO " + v1 + "]";
            case GT -> "{" + v1 + " TO *}";
            case GTE -> "[" + v1 + " TO *]";
            case RANGE_INCLUSIVE -> "[" + v1 + " TO " + v2 + "]";
            case RANGE_EXCLUSIVE -> "{" + v1 + " TO " + v2 + "}";
        };
    }

    private static String orWildcard(@Nullable String value) {
        if (value == null || value.trim().isEmpty())
            return "*";
        return escapeBare(value.trim());
    }

    /**
     * A clause value as it appears in the query: quoted when it contains whitespace or quotes
     * (escaping inner quotes), otherwise bare with structural characters escaped while wildcard and
     * fuzzy characters ({@code * ? ~}) stay usable.
     */
    static String escapeValue(@NotNull String value) {
        if (value.chars().anyMatch(Character::isWhitespace) || value.indexOf('"') >= 0)
            return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        return escapeBare(value);
    }

    /**
     * Escapes the characters that would change the structure of an unquoted term: field separator,
     * grouping/range brackets, boost/fuzzy-parameter markers and the regex delimiter. Deliberately
     * NOT escaped: {@code * ? ~} (wildcards/fuzzy stay usable) and {@code + -} (rarely leading, and
     * part of adduct notation which is covered by the bracket escaping).
     */
    private static String escapeBare(@NotNull String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c == ':' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}'
                    || c == '^' || c == '/' || c == '\\')
                escaped.append('\\');
            escaped.append(c);
        }
        return escaped.toString();
    }
}
