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
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.GroupQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.ModifierQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QuotedFieldQueryNode;
import org.apache.lucene.queryparser.flexible.standard.nodes.AbstractRangeQueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The inverse of {@link LuceneQueryCompiler}: parses a lucene query string into the query-builder AST
 * ({@link QueryContainer}/{@link QueryNode}) so an existing query can be hydrated back into editable
 * chips (groups, keyless free text and fielded clauses alike).
 * <p>
 * It uses Lucene's flexible {@link StandardSyntaxParser} to obtain the raw syntax tree (NO analysis,
 * so values are not lowercased or tokenized - the same non-manipulating choice the query builder and
 * the server-side query creation make) and maps that tree onto our nodes. Constructs we do not model
 * (fuzzy {@code ~}, boost {@code ^}, regexp, ...) make the whole parse return {@link Optional#empty()}
 * so the caller can fall back to treating the string as plain free text.
 */
public final class QueryStringParser {

    private QueryStringParser() {
    }

    /** Signals a syntax node we do not map; caught in {@link #parse} to fail the whole parse. */
    private static final class UnsupportedNodeException extends RuntimeException {
    }

    /**
     * Parses {@code query} into our AST, or empty if it is syntactically invalid or uses constructs we
     * do not model. An empty/blank query maps to an empty container. {@code fields} disambiguates a
     * {@code field:value} term as a numeric equality vs. a text match.
     */
    public static Optional<QueryContainer> parse(@NotNull String query, @NotNull List<SearchableField> fields) {
        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return Optional.of(QueryContainer.empty());

        org.apache.lucene.queryparser.flexible.core.nodes.QueryNode tree;
        try {
            tree = new StandardSyntaxParser().parse(trimmed, "");
        } catch (QueryNodeParseException e) {
            return Optional.empty();
        }
        try {
            Set<String> numericFields = fields.stream()
                    .filter(f -> TokenInputModel.NUMERIC_TYPES.contains(f.getFieldType()))
                    .map(SearchableField::getName)
                    .collect(Collectors.toSet());
            return Optional.of(mapContainer(tree, numericFields));
        } catch (UnsupportedNodeException e) {
            return Optional.empty();
        }
    }

    /** Maps a node as a container level, flattening a boolean/and/or into items + per-edge logics. */
    private static QueryContainer mapContainer(org.apache.lucene.queryparser.flexible.core.nodes.QueryNode node,
                                               Set<String> numericFields) {
        LogicOp logic = booleanLogic(node);
        if (logic == null)
            return new QueryContainer(List.of(mapNode(node, numericFields)), List.of());
        List<QueryNode> items = new ArrayList<>();
        for (org.apache.lucene.queryparser.flexible.core.nodes.QueryNode child : node.getChildren())
            items.add(mapNode(child, numericFields));
        List<LogicOp> logics = new ArrayList<>();
        for (int i = 1; i < items.size(); i++)
            logics.add(logic); // a boolean node joins all its children with the same operator
        return new QueryContainer(items, logics);
    }

    /** Maps a node as a single AST node (clause, or group for nested booleans / explicit groups). */
    private static QueryNode mapNode(org.apache.lucene.queryparser.flexible.core.nodes.QueryNode node,
                                     Set<String> numericFields) {
        if (node instanceof ModifierQueryNode modifier) {
            QueryNode child = mapNode(modifier.getChild(), numericFields);
            return modifier.getModifier() == ModifierQueryNode.Modifier.MOD_NOT ? child.withNegated(true) : child;
        }
        if (node instanceof GroupQueryNode group)
            return toGroup(mapContainer(group.getChild(), numericFields));
        if (booleanLogic(node) != null) // a nested boolean/and/or becomes a parenthesised group
            return toGroup(mapContainer(node, numericFields));
        if (node instanceof AbstractRangeQueryNode<?> range)
            return mapRange(range);
        if (node instanceof QuotedFieldQueryNode quoted) // a "phrase" value
            return mapField(quoted, numericFields);
        if (node.getClass() == FieldQueryNode.class) // a plain field:term or keyless term
            return mapField((FieldQueryNode) node, numericFields);
        // other FieldQueryNode subclasses (fuzzy ~, ...) and everything else are not part of our model
        throw new UnsupportedNodeException();
    }

    private static QueryNode mapField(FieldQueryNode field, Set<String> numericFields) {
        String name = field.getFieldAsString();
        String text = field.getTextAsString();
        if (name == null || name.isEmpty())
            return QueryClause.freeText(text, false);
        if (numericFields.contains(name))
            return QueryClause.numeric(name, NumberOp.EQ, text, null, false);
        return QueryClause.text(name, text, false);
    }

    private static QueryNode mapRange(AbstractRangeQueryNode<?> range) {
        FieldQueryNode lower = (FieldQueryNode) range.getLowerBound();
        FieldQueryNode upper = (FieldQueryNode) range.getUpperBound();
        String field = lower.getFieldAsString();
        String lowerText = lower.getTextAsString();
        String upperText = upper.getTextAsString();
        boolean lowerOpen = isOpenBound(lowerText);
        boolean upperOpen = isOpenBound(upperText);

        if (lowerOpen && !upperOpen) // [* TO v] / {* TO v}  ->  <= / <
            return QueryClause.numeric(field, range.isUpperInclusive() ? NumberOp.LTE : NumberOp.LT, upperText, null, false);
        if (!lowerOpen && upperOpen) // [v TO *] / {v TO *}  ->  >= / >
            return QueryClause.numeric(field, range.isLowerInclusive() ? NumberOp.GTE : NumberOp.GT, lowerText, null, false);
        // both bounds present -> a two-valued range (our model is fully inclusive or fully exclusive)
        NumberOp op = range.isLowerInclusive() ? NumberOp.RANGE_INCLUSIVE : NumberOp.RANGE_EXCLUSIVE;
        return QueryClause.numeric(field, op, lowerOpen ? "" : lowerText, upperOpen ? "" : upperText, false);
    }

    private static QueryGroup toGroup(QueryContainer content) {
        return new QueryGroup(QueryNode.nextId("group"), false, content.items(), content.logics());
    }

    private static boolean isOpenBound(String bound) {
        return bound == null || bound.isEmpty() || bound.equals("*");
    }

    /** The uniform logic of a boolean node (AND / OR / implicit AND), or null if {@code node} is not one. */
    private static LogicOp booleanLogic(org.apache.lucene.queryparser.flexible.core.nodes.QueryNode node) {
        if (node instanceof AndQueryNode)
            return LogicOp.AND;
        if (node instanceof OrQueryNode)
            return LogicOp.OR;
        if (node instanceof BooleanQueryNode) // implicit grouping (default operator) -> AND in our model
            return LogicOp.AND;
        return null;
    }
}
