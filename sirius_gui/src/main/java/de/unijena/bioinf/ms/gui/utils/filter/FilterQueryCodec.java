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

package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.query.LogicOp;
import de.unijena.bioinf.ms.gui.utils.query.LuceneQueryCompiler;
import de.unijena.bioinf.ms.gui.utils.query.QueryClause;
import de.unijena.bioinf.ms.gui.utils.query.QueryContainer;
import de.unijena.bioinf.ms.gui.utils.query.QueryGroup;
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;
import de.unijena.bioinf.ms.gui.utils.search.QueryStringParser;
import io.sirius.ms.sdk.model.SearchableField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * The codec between a {@link FeatureFilter} and a lucene query string, both ways.
 * <p>
 * {@link #encode} is the query the filter stands for - its facets ({@link FacetCodecs}), the user's own query
 * joined by the block joint, the inversion wrapper - in the executable form ({@code compileExecutable}, so a
 * negation-only facet block carries its match-all anchor). {@link #decode} takes such a string apart again:
 * the string is parsed into the query tree ({@link QueryStringParser}), and every facet shape the
 * {@link FacetCodecs} recognise is absorbed into filter state, while everything else stays the user's query
 * ({@link Absorbed}). Pasting a query, saving a preset and copying the executed query are therefore one pure
 * function each way.
 * <p>
 * Decoding is conservative: facets are absorbed only from a flat AND at the top level (or from the facet
 * block of a {@code (facets) join (query)} shape); an OR between top-level terms, a construct the tree does
 * not model, or a clause that merely resembles a facet leaves things as the user's query.
 */
public final class FilterQueryCodec {

    private FilterQueryCodec() {
    }

    /**
     * What a query took apart into: the facet edits it carried, whether it was inverted, the joint between the
     * facet block and the user's part, and the user's part as query text.
     *
     * @param facets     the absorbed facets as one edit (identity when none)
     * @param facetCount how many facets were absorbed
     * @param inverted   whether the query carried the inversion wrapper
     * @param join       the joint between the facet block and the user's query
     * @param userQuery  the residual: whatever was not a facet, as query text (the whole query when nothing was)
     */
    public record Absorbed(@NotNull UnaryOperator<FeatureFilter> facets, int facetCount, boolean inverted,
                           @NotNull LogicOp join, @NotNull String userQuery) {

        /** The query as a filter of its own: the absorbed facets over the empty filter. */
        public FeatureFilter toFilter(@NotNull FilterContext ctx) {
            return applyTo(FeatureFilter.empty(ctx));
        }

        /** {@code base} with the absorbed facets, inversion, joint and user query applied. */
        public FeatureFilter applyTo(@NotNull FeatureFilter base) {
            return facets.apply(base).withInverted(inverted).withQueryJoin(join).withUserQuery(userQuery);
        }

        public boolean anyFacet() {
            return facetCount > 0;
        }

        static Absorbed nothing(boolean inverted, @NotNull String userQuery) {
            return new Absorbed(UnaryOperator.identity(), 0, inverted, LogicOp.AND, userQuery);
        }
    }

    // --- filter -> string ---

    /**
     * The query {@code filter} stands for, in the executable form: {@code (facets) join (user query)}, wrapped
     * as {@code *:* AND NOT (...)} when inverted. Empty when nothing is filtered.
     */
    public static String encode(@NotNull FeatureFilter filter, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode confidenceMode) {
        List<QueryNode> nodes = PanelQueryNodeFactory.nodesFor(filter, ctx, confidenceMode);
        List<LogicOp> ands = new ArrayList<>(Math.max(0, nodes.size() - 1));
        for (int i = 1; i < nodes.size(); i++)
            ands.add(LogicOp.AND);
        String core = LuceneQueryCompiler.compileExecutable(new QueryContainer(nodes, ands), filter.userQuery(), filter.queryJoin());
        return filter.inverted() && !core.isBlank() ? "*:* AND NOT (" + core + ")" : core;
    }

    // --- string -> filter ---

    /**
     * Takes a query string apart (see the class doc). {@code fields} tells a numeric equality from a text
     * match while parsing; the facets themselves need none. A string the tree cannot model (or that is not
     * valid lucene) is kept whole as the user's query.
     */
    public static Absorbed decode(@NotNull String query, @NotNull FilterContext ctx, @NotNull List<SearchableField> fields) {
        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return Absorbed.nothing(false, "");
        Optional<QueryContainer> parsed = QueryStringParser.parse(trimmed, fields);
        return parsed.map(tree -> absorb(tree, ctx)).orElseGet(() -> Absorbed.nothing(false, trimmed));
    }

    /** {@link #decode} on an already parsed tree. */
    public static Absorbed absorb(@NotNull QueryContainer tree, @NotNull FilterContext ctx) {
        QueryContainer body = unwrap(tree, ctx);

        // the inversion wrapper: *:* AND NOT (core) - a negated GROUP; a negated bare clause after the anchor is
        // the negation-only facet block instead (*:* AND NOT lipid:true), which the flat case handles below
        boolean inverted = false;
        if (body.items().size() == 2 && body.logics().get(0) == LogicOp.AND
                && isMatchAll(body.items().get(0)) && body.items().get(1) instanceof QueryGroup core && core.negated()) {
            inverted = true;
            body = unwrap(asContainer(core.withNegated(false)), ctx);
        }

        // the two-block shape: (facets) join (user query) - the facet block must decode completely
        if (body.items().size() == 2
                && body.items().get(0) instanceof QueryGroup first && !first.negated()
                && body.items().get(1) instanceof QueryGroup second && !second.negated()) {
            LogicOp join = body.logics().get(0);
            Optional<List<UnaryOperator<FeatureFilter>>> facets = absorbAll(asContainer(first), ctx);
            Optional<List<UnaryOperator<FeatureFilter>>> more = absorbAll(asContainer(second), ctx);
            if (facets.isPresent() && more.isPresent() && join == LogicOp.AND) {
                // both blocks are facets: one flat filter, no user part
                List<UnaryOperator<FeatureFilter>> all = new ArrayList<>(facets.get());
                all.addAll(more.get());
                return new Absorbed(compose(all), all.size(), inverted, LogicOp.AND, "");
            }
            if (facets.isPresent())
                return new Absorbed(compose(facets.get()), facets.get().size(), inverted, join,
                        LuceneQueryCompiler.compile(asContainer(second), ""));
        }

        // a flat AND list: each item is a facet or stays a user clause. An OR anywhere at this level means
        // absorbing anything would change what the query matches, so nothing is absorbed.
        if (!body.logics().stream().allMatch(l -> l == LogicOp.AND))
            return Absorbed.nothing(inverted, LuceneQueryCompiler.compile(body, ""));
        List<UnaryOperator<FeatureFilter>> facets = new ArrayList<>();
        List<QueryNode> residual = new ArrayList<>();
        for (QueryNode item : body.items()) {
            if (isMatchAll(item) && body.items().size() > 1)
                continue; // the anchor compileExecutable adds in front of a negation-only block
            Optional<UnaryOperator<FeatureFilter>> facet = FacetCodecs.decodeAny(item, ctx);
            if (facet.isPresent())
                facets.add(facet.get());
            else
                residual.add(item);
        }
        List<LogicOp> ands = new ArrayList<>(Math.max(0, residual.size() - 1));
        for (int i = 1; i < residual.size(); i++)
            ands.add(LogicOp.AND);
        return new Absorbed(compose(facets), facets.size(), inverted, LogicOp.AND,
                LuceneQueryCompiler.compile(new QueryContainer(residual, ands), ""));
    }

    /** Every item of an all-AND container decoded, or empty if one of them is not a facet. */
    private static Optional<List<UnaryOperator<FeatureFilter>>> absorbAll(QueryContainer container, FilterContext ctx) {
        if (container.isEmpty() || !container.logics().stream().allMatch(l -> l == LogicOp.AND))
            return Optional.empty();
        List<UnaryOperator<FeatureFilter>> facets = new ArrayList<>();
        for (QueryNode item : container.items()) {
            if (isMatchAll(item) && container.items().size() > 1)
                continue;
            Optional<UnaryOperator<FeatureFilter>> facet = FacetCodecs.decodeAny(item, ctx);
            if (facet.isEmpty())
                return Optional.empty();
            facets.add(facet.get());
        }
        return facets.isEmpty() ? Optional.empty() : Optional.of(facets);
    }

    private static UnaryOperator<FeatureFilter> compose(List<UnaryOperator<FeatureFilter>> edits) {
        return filter -> {
            for (UnaryOperator<FeatureFilter> edit : edits)
                filter = edit.apply(filter);
            return filter;
        };
    }

    /**
     * A container holding just one non-negated group is that group's content - unless the group is a facet
     * in its own right (the RT triple, an adduct or quality alternative), which must stay whole to be recognised.
     */
    private static QueryContainer unwrap(QueryContainer container, FilterContext ctx) {
        while (container.items().size() == 1 && container.items().get(0) instanceof QueryGroup group && !group.negated()
                && FacetCodecs.decodeAny(group, ctx).isEmpty())
            container = asContainer(group);
        return container;
    }

    private static QueryContainer asContainer(QueryNode node) {
        if (node instanceof QueryGroup group)
            return new QueryContainer(group.items(), group.logics());
        return new QueryContainer(List.of(node), List.of());
    }

    private static boolean isMatchAll(QueryNode node) {
        return node instanceof QueryClause c && !c.negated() && c.op() == null && c.field().equals("*") && c.value1().equals("*");
    }
}
