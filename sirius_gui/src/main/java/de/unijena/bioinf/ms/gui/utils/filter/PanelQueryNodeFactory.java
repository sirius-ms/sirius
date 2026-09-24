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
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The active structured facets of a {@link FeatureFilter} as query-builder {@link QueryNode}s, using the REAL
 * lucene index field names (e.g. {@code ionMass:[300 TO 400]}, {@code detectedAdducts:...}) - so the
 * filter-panel state can be rendered by the SAME engine as the user's own query clauses, only in a different
 * (model) style, and the executed query is built from the very same nodes. A pure function of the value and
 * its {@link FilterContext}.
 * <p>
 * The shapes themselves live in {@link FacetCodecs}, one {@link FacetCodec} per facet, next to the recogniser
 * that reads them back; this class is the encode-side façade over them.
 */
public final class PanelQueryNodeFactory {

    private PanelQueryNodeFactory() {
    }

    /** Applies a new numeric range to the facet it belongs to. */
    @FunctionalInterface
    public interface RangeUpdate {
        FeatureFilter apply(@NotNull FeatureFilter filter, double min, double max);
    }

    /** The inline-editable range of a numeric facet: its current values, the bounds, and how to apply new ones. */
    public record RangeFacet(double currentMin, double currentMax, double lowerBound, double upperBound,
                             @NotNull RangeUpdate update) {
    }

    /**
     * One active filter-panel facet: its stable {@code id}, the faithful {@link QueryNode} it compiles
     * to (verified equal to the executed query), and how to {@code reset} exactly that facet - as an
     * operation on the value, so it can be applied to any filter (a working copy as much as the applied one).
     */
    public record Facet(@NotNull String id, @NotNull QueryNode queryNode,
                        @NotNull UnaryOperator<FeatureFilter> reset,
                        @Nullable RangeFacet range) {
        /** A facet with no inline range editing (set/complex facets edit via the dialog). */
        public Facet(@NotNull String id, @NotNull QueryNode queryNode, @NotNull UnaryOperator<FeatureFilter> reset) {
            this(id, queryNode, reset, null);
        }
    }

    /** The active panel facets of the model's applied filter as query nodes, AND-joined at the top level. */
    public static List<QueryNode> nodesFor(@NotNull FeatureFilterModel model, @NotNull ConfidenceDisplayMode confidenceMode) {
        return nodesFor(model.get(), model.getContext(), confidenceMode);
    }

    /**
     * The active panel facets as query nodes, AND-joined at the top level. {@code confidenceMode}
     * selects the confidence field the filter actually queries. Empty when no facet is active.
     */
    public static List<QueryNode> nodesFor(@NotNull FeatureFilter filter, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode confidenceMode) {
        return facets(filter, ctx, confidenceMode).stream().map(Facet::queryNode).toList();
    }

    /** The active facets of the model's applied filter as descriptors (node + reset), in a stable order. */
    public static List<Facet> facets(@NotNull FeatureFilterModel model, @NotNull ConfidenceDisplayMode confidenceMode) {
        return facets(model.get(), model.getContext(), confidenceMode);
    }

    /** The active facets as descriptors (node + reset), in codec order. */
    public static List<Facet> facets(@NotNull FeatureFilter filter, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode confidenceMode) {
        return FacetCodecs.all().stream()
                .flatMap(codec -> codec.encode(filter, ctx, confidenceMode).stream())
                .toList();
    }
}
