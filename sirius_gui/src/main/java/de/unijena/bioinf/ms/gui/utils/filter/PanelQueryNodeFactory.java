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
import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import de.unijena.bioinf.ms.gui.utils.query.*;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Builds query-builder {@link QueryNode}s for the active structured facets of a {@link FeatureFilter},
 * using the REAL lucene index field names (e.g. {@code ionMass:[300 TO 400]}, {@code detectedAdducts:...})
 * - so the filter-panel state can be rendered by the SAME engine as the user's own query clauses, only in
 * a different (model) style. A pure function of the value and its {@link FilterContext}.
 * <p>
 * The field names below MUST match what {@code FeatureFilterModel.toLuceneQuery} executes - which is these
 * very nodes, compiled (see the equivalence tests). Compound facets (RT, adducts, quality, elements, DB)
 * render as OR/AND groups; the quality {@code NOT_APPLICABLE} fallbacks are an execution detail and are
 * intentionally not shown as chips.
 */
public final class PanelQueryNodeFactory {

    private PanelQueryNodeFactory() {
    }

    // Field names come from FeatureFilterModel (the single GUI-side source of truth), so the chips and
    // the executed query cannot drift on field names.
    static final String FIELD_MZ = FeatureFilterModel.FIELD_MZ;
    static final String FIELD_RT_START = FeatureFilterModel.FIELD_RT_START;
    static final String FIELD_RT_APEX = FeatureFilterModel.FIELD_RT_APEX;
    static final String FIELD_RT_END = FeatureFilterModel.FIELD_RT_END;
    static final String FIELD_CONFIDENCE_APPROX = FeatureFilterModel.FIELD_CONFIDENCE_APPROX;
    static final String FIELD_CONFIDENCE_EXACT = FeatureFilterModel.FIELD_CONFIDENCE_EXACT;
    static final String FIELD_HAS_MS1 = FeatureFilterModel.FIELD_HAS_MS1;
    static final String FIELD_HAS_MSMS = FeatureFilterModel.FIELD_HAS_MSMS;
    static final String FIELD_ADDUCTS = FeatureFilterModel.FIELD_ADDUCTS;
    static final String FIELD_QUALITY = FeatureFilterModel.FIELD_QUALITY;
    static final String PREFIX_CATEGORIZED_QUALITY = FeatureFilterModel.PREFIX_CATEGORIZED_QUALITY;
    static final String PREFIX_ELEMENT = FeatureFilterModel.PREFIX_ELEMENT;
    static final String FIELD_LIPID = FeatureFilterModel.FIELD_LIPID;
    static final String FIELD_PFAS = FeatureFilterModel.FIELD_PFAS;
    static final String PREFIX_DB = FeatureFilterModel.PREFIX_DB;
    static final String FIELD_BLANK = FeatureFilterModel.BLANK_REMOVAL_SEARCH_FIELD_NAME;

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

    /** The active facets as descriptors (node + reset), in a stable order. */
    public static List<Facet> facets(@NotNull FeatureFilter f, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode confidenceMode) {
        List<Facet> facets = new ArrayList<>();

        if (f.isMzFilterActive(ctx))
            facets.add(new Facet("mz", range(FIELD_MZ, f.mz().min(), f.mz().max()),
                    x -> x.withMz(ctx.minMz(), ctx.maxMz()),
                    new RangeFacet(f.mz().min(), f.mz().max(), ctx.minMz(), ctx.maxMz(), FeatureFilter::withMz)));

        if (f.isRtFilterActive(ctx))
            // the filter matches the RT window against ANY of the three retention-time fields (OR)
            facets.add(new Facet("rt", group(LogicOp.OR, List.of(
                    range(FIELD_RT_START, f.rt().min(), f.rt().max()),
                    range(FIELD_RT_APEX, f.rt().min(), f.rt().max()),
                    range(FIELD_RT_END, f.rt().min(), f.rt().max()))),
                    x -> x.withRt(ctx.minRt(), ctx.maxRt()),
                    new RangeFacet(f.rt().min(), f.rt().max(), ctx.minRt(), ctx.maxRt(), FeatureFilter::withRt)));

        if (f.isConfidenceFilterActive(ctx)) {
            String field = confidenceMode == ConfidenceDisplayMode.APPROXIMATE ? FIELD_CONFIDENCE_APPROX : FIELD_CONFIDENCE_EXACT;
            facets.add(new Facet("confidence", range(field, f.confidence().min(), f.confidence().max()),
                    x -> x.withConfidence(ctx.minConfidence(), ctx.maxConfidence()),
                    new RangeFacet(f.confidence().min(), f.confidence().max(), ctx.minConfidence(), ctx.maxConfidence(),
                            FeatureFilter::withConfidence)));
        }

        if (f.hasMs1())
            facets.add(new Facet("hasMs1", QueryClause.text(FIELD_HAS_MS1, "true", false), x -> x.withHasMs1(false)));
        if (f.hasMsMs())
            facets.add(new Facet("hasMsMs", QueryClause.text(FIELD_HAS_MSMS, "true", false), x -> x.withHasMsMs(false)));

        if (f.isAdductFilterActive())
            facets.add(new Facet("adducts", group(LogicOp.OR, f.adducts().stream()
                    .map(Object::toString).sorted()
                    .<QueryNode>map(adduct -> QueryClause.text(FIELD_ADDUCTS, adduct, false))
                    .toList()), x -> x.withAdducts(java.util.Set.of())));

        if (f.isFeatureQualityFilterEnabled())
            facets.add(new Facet("quality", group(LogicOp.OR, qualityTerms(FIELD_QUALITY, f.featureQuality())),
                    x -> x.withFeatureQuality(FeatureFilter.ALL_QUALITIES)));

        for (DefaultQualityCategory category : DefaultQualityCategory.values()) {
            if (!f.isCategorizedQualityFilterEnabled(category))
                continue;
            List<QueryNode> terms = qualityTerms(PREFIX_CATEGORIZED_QUALITY + category.name(), f.categorizedQuality().get(category));
            // the filter additionally lets features without any quality data pass
            terms.add(QueryClause.text(FIELD_QUALITY, DataQuality.NOT_APPLICABLE.toString(), false));
            facets.add(new Facet("quality." + category.name(), group(LogicOp.OR, terms),
                    x -> x.withCategorizedQuality(category, FeatureFilter.ALL_QUALITIES)));
        }

        if (f.isElementFilterEnabled()) {
            List<QueryNode> perElement = new ArrayList<>();
            f.elementFilter().getConstraints().getChemicalAlphabet().forEach(element -> perElement.add(
                    QueryClause.numeric(PREFIX_ELEMENT + element.getSymbol(), NumberOp.RANGE_INCLUSIVE,
                            Integer.toString(f.elementFilter().getConstraints().getLowerbound(element)),
                            Integer.toString(f.elementFilter().getConstraints().getUpperbound(element)), false)));
            // the top formula must satisfy ALL element constraints (AND)
            facets.add(new Facet("elements", group(LogicOp.AND, perElement), x -> x.withElementFilter(ElementFilter.disabled())));
        }

        if (f.blankFoldChangeEnabled())
            facets.add(new Facet("blank", QueryClause.numeric(FIELD_BLANK, NumberOp.RANGE_INCLUSIVE,
                    number(f.minBlankFoldChange()), "", false), // [min TO *]
                    x -> x.withBlankFoldChangeEnabled(false).withMinBlankFoldChange(FeatureFilter.DEFAULT_MIN_BLANK_FOLD_CHANGE)));

        if (f.isLipidFilterEnabled()) {
            // "no lipid class" is a negated clause; "a lipid class" a plain one
            boolean noLipid = Boolean.FALSE.equals(f.lipidClassDetected());
            facets.add(new Facet("lipid", QueryClause.text(FIELD_LIPID, "true", noLipid),
                    x -> x.withLipidClassDetected(null)));
        }

        if (f.isPfasFilterEnabled()) {
            // presence of the tag as an open range: the parser rejects a bare wildcard (no leading
            // wildcards), and "no pfas" is its negation - which only matches thanks to the match-all
            // anchor compileExecutable adds
            boolean noPfas = Boolean.FALSE.equals(f.pfasDetected());
            facets.add(new Facet("pfas", QueryClause.numeric(FIELD_PFAS, NumberOp.RANGE_INCLUSIVE, "", "", noPfas),
                    x -> x.withPfasDetected(null)));
        }

        if (f.isDbFilterEnabled()) {
            int candidates = f.dbFilter().getNumOfCandidates();
            facets.add(new Facet("db", group(LogicOp.OR, f.dbFilter().getDbs().stream()
                    .map(SearchableDatabase::getDatabaseId)
                    .filter(java.util.Objects::nonNull).sorted()
                    .<QueryNode>map(db -> QueryClause.numeric(PREFIX_DB + db, NumberOp.RANGE_INCLUSIVE,
                            "1", Integer.toString(candidates), false))
                    .toList()), x -> x.withDbFilter(null)));
        }

        return facets;
    }

    /**
     * The quality clauses for one field: one {@code field:value} per selected data quality, plus a
     * {@code field:NOT_APPLICABLE} fallback so features without quality data still pass (added
     * unconditionally).
     */
    private static List<QueryNode> qualityTerms(String field, Collection<io.sirius.ms.sdk.model.DataQuality> selected) {
        List<QueryNode> terms = new ArrayList<>();
        selected.stream().map(Object::toString).sorted()
                .forEach(quality -> terms.add(QueryClause.text(field, quality, false)));
        terms.add(QueryClause.text(field, DataQuality.NOT_APPLICABLE.toString(), false));
        return terms;
    }

    /**
     * A range clause, left open at whichever end is unbounded: an infinite bound is written as no bound at all
     * ({@code [200 TO *]}), since "Infinity" is not a number lucene parses.
     */
    private static QueryClause range(String field, double currentMin, double currentMax) {
        return QueryClause.numeric(field, NumberOp.RANGE_INCLUSIVE,
                openIfInfinite(currentMin), openIfInfinite(currentMax), false);
    }

    /** The bound as query text, or the empty string for an unbounded one - which renders as {@code *}. */
    private static String openIfInfinite(double bound) {
        return Double.isInfinite(bound) ? "" : number(bound);
    }

    /** A parenthesized group joining the items with the given operator; a single item stays a clause. */
    private static QueryNode group(LogicOp op, @NotNull List<QueryNode> items) {
        if (items.size() == 1)
            return items.get(0);
        List<LogicOp> logics = new ArrayList<>(Math.max(0, items.size() - 1));
        for (int i = 1; i < items.size(); i++)
            logics.add(op);
        return new QueryGroup(QueryNode.nextId("group"), false, items, logics);
    }

    private static String number(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value))
            return String.format(Locale.US, "%.0f", value);
        return String.format(Locale.US, "%s", value);
    }
}
