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
import de.unijena.bioinf.ms.gui.utils.query.*;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import io.sirius.ms.sdk.model.SearchableDatabase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Builds query-builder {@link QueryNode}s for the active structured filters of a
 * {@link FeatureFilterModel}, using the REAL lucene index field names (e.g. {@code ionMass:[300 TO 400]},
 * {@code detectedAdducts:...}) - so the filter-panel state can be rendered by the SAME engine as the
 * user's own query clauses, only in a different (model) style.
 * <p>
 * This is the AlignedFeature-specific precursor of the pojo-agnostic {@code FilterTerm} provider
 * planned for P1 (see GUI-SEARCHBAR-PLAN.md): for now it just emits nodes, no binding/editing yet.
 * <p>
 * The field names below MUST match {@code FeatureFilterModel.toLuceneQueryBuilder}. Until the two are
 * unified at the source (plan P5), a test cross-checks that every field name emitted here occurs in
 * the model's compiled query, so the parallel mapping cannot silently drift. Compound facets (RT,
 * adducts, quality, elements, DB) render as OR/AND groups; the model's {@code NOT_APPLICABLE}
 * quality fallbacks are an execution detail and are intentionally not shown as chips.
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

    /**
     * One active filter-panel facet: its stable {@code id}, the faithful {@link QueryNode} it compiles
     * to (verified equal to the model's executed query), and how to {@code reset} exactly that facet in
     * the model. This is the per-facet descriptor the {@code FilterTerm} provider and (later, plan P5)
     * the single-source query builder are built on.
     */
    public record Facet(@NotNull String id, @NotNull QueryNode queryNode,
                        @NotNull Consumer<FeatureFilterModel> reset,
                        @org.jetbrains.annotations.Nullable RangeEdit range) {
        /** A facet with no inline range editing (set/complex facets edit via the dialog). */
        public Facet(@NotNull String id, @NotNull QueryNode queryNode, @NotNull Consumer<FeatureFilterModel> reset) {
            this(id, queryNode, reset, null);
        }
    }

    /**
     * The active panel facets as query nodes, AND-joined at the top level. {@code confidenceMode}
     * selects the confidence field the model actually queries. Empty when no facet is active.
     */
    public static List<QueryNode> nodesFor(@NotNull FeatureFilterModel model, @NotNull ConfidenceDisplayMode confidenceMode) {
        return facets(model, confidenceMode).stream().map(Facet::queryNode).toList();
    }

    /** The active facets as descriptors (node + reset), in a stable order. */
    public static List<Facet> facets(@NotNull FeatureFilterModel model, @NotNull ConfidenceDisplayMode confidenceMode) {
        List<Facet> facets = new ArrayList<>();

        if (model.isMzFilterActive())
            facets.add(new Facet("mz", range(FIELD_MZ, model.getCurrentMinMz(), model.getCurrentMaxMz()),
                    m -> {
                        m.setCurrentMinMz(m.getMinMz());
                        m.setCurrentMaxMz(m.getMaxMz());
                    },
                    new RangeEdit(model.getCurrentMinMz(), model.getCurrentMaxMz(), model.getMinMz(), model.getMaxMz(),
                            (mn, mx) -> {
                                model.setCurrentMinMz(mn);
                                model.setCurrentMaxMz(mx);
                            })));

        if (model.isRtFilterActive())
            // the model matches the RT window against ANY of the three retention-time fields (OR)
            facets.add(new Facet("rt", group(LogicOp.OR, List.of(
                    range(FIELD_RT_START, model.getCurrentMinRt(), model.getCurrentMaxRt()),
                    range(FIELD_RT_APEX, model.getCurrentMinRt(), model.getCurrentMaxRt()),
                    range(FIELD_RT_END, model.getCurrentMinRt(), model.getCurrentMaxRt()))),
                    m -> {
                        m.setCurrentMinRt(m.getMinRt());
                        m.setCurrentMaxRt(m.getMaxRt());
                    },
                    new RangeEdit(model.getCurrentMinRt(), model.getCurrentMaxRt(), model.getMinRt(), model.getMaxRt(),
                            (mn, mx) -> {
                                model.setCurrentMinRt(mn);
                                model.setCurrentMaxRt(mx);
                            })));

        if (model.isMinConfidenceFilterActive() || model.isMaxConfidenceFilterActive()) {
            String field = confidenceMode == ConfidenceDisplayMode.APPROXIMATE ? FIELD_CONFIDENCE_APPROX : FIELD_CONFIDENCE_EXACT;
            facets.add(new Facet("confidence", range(field, model.getCurrentMinConfidence(), model.getCurrentMaxConfidence()),
                    m -> {
                        m.setCurrentMinConfidence(m.getMinConfidence());
                        m.setCurrentMaxConfidence(m.getMaxConfidence());
                    },
                    new RangeEdit(model.getCurrentMinConfidence(), model.getCurrentMaxConfidence(),
                            model.getMinConfidence(), model.getMaxConfidence(),
                            (mn, mx) -> {
                                model.setCurrentMinConfidence(mn);
                                model.setCurrentMaxConfidence(mx);
                            })));
        }

        if (model.isHasMs1())
            facets.add(new Facet("hasMs1", QueryClause.text(FIELD_HAS_MS1, "true", false), m -> m.setHasMs1(false)));
        if (model.isHasMsMs())
            facets.add(new Facet("hasMsMs", QueryClause.text(FIELD_HAS_MSMS, "true", false), m -> m.setHasMsMs(false)));

        if (model.isAdductFilterActive())
            facets.add(new Facet("adducts", group(LogicOp.OR, model.getSelectedAdducts().stream()
                    .map(Object::toString).sorted()
                    .<QueryNode>map(adduct -> QueryClause.text(FIELD_ADDUCTS, adduct, false))
                    .toList()), m -> m.setAdducts(java.util.Set.of())));

        if (model.getFeatureQualityFilter().isEnabled())
            facets.add(new Facet("quality", group(LogicOp.OR, qualityTerms(FIELD_QUALITY, model.getFeatureQualityFilter())),
                    m -> m.getFeatureQualityFilter().reset()));

        model.getCategorizedQualityFilters().stream().filter(QualityFilter::isEnabled).forEach(filter -> {
            List<QueryNode> terms = qualityTerms(PREFIX_CATEGORIZED_QUALITY + filter.getId(), filter);
            // the model additionally lets features without any quality data pass (see toLuceneQueryBuilder)
            terms.add(QueryClause.text(FIELD_QUALITY, DataQuality.NOT_APPLICABLE.toString(), false));
            facets.add(new Facet("quality." + filter.getId(), group(LogicOp.OR, terms), m -> filter.reset()));
        });

        if (model.isElementFilterEnabled()) {
            List<QueryNode> perElement = new ArrayList<>();
            model.getElementFilter().getConstraints().getChemicalAlphabet().forEach(element -> perElement.add(
                    QueryClause.numeric(PREFIX_ELEMENT + element.getSymbol(), NumberOp.RANGE_INCLUSIVE,
                            Integer.toString(model.getElementFilter().getConstraints().getLowerbound(element)),
                            Integer.toString(model.getElementFilter().getConstraints().getUpperbound(element)), false)));
            // the top formula must satisfy ALL element constraints (AND)
            facets.add(new Facet("elements", group(LogicOp.AND, perElement), m -> m.setElementFilter(ElementFilter.disabled())));
        }

        if (model.getSampleBlankFoldChange().isEnabled())
            facets.add(new Facet("blank", QueryClause.numeric(FIELD_BLANK, NumberOp.RANGE_INCLUSIVE,
                    number(model.getSampleBlankFoldChange().getCurrentMinFoldChange()), "", false), // [min TO *]
                    m -> m.getSampleBlankFoldChange().reset()));

        if (model.isLipidFilterEnabled()) {
            // "no lipid class" is a negated clause; "a lipid class" a plain one
            boolean noLipid = Boolean.FALSE.equals(model.getLipidClassDetected());
            facets.add(new Facet("lipid", QueryClause.text(FIELD_LIPID, "true", noLipid),
                    m -> m.setLipidClassDetected(null)));
        }

        if (model.isPfasFilterEnabled()) {
            // presence of the tag as an open range: the parser rejects a bare wildcard (no leading
            // wildcards), and "no pfas" is its negation - which only matches thanks to the match-all
            // anchor compileExecutable adds
            boolean noPfas = Boolean.FALSE.equals(model.getPfasDetected());
            facets.add(new Facet("pfas", QueryClause.numeric(FIELD_PFAS, NumberOp.RANGE_INCLUSIVE, "", "", noPfas),
                    m -> m.setPfasDetected(null)));
        }

        if (model.isDbFilterEnabled()) {
            int candidates = model.getDbFilter().getNumOfCandidates();
            facets.add(new Facet("db", group(LogicOp.OR, model.getDbFilter().getDbs().stream()
                    .map(SearchableDatabase::getDatabaseId)
                    .filter(java.util.Objects::nonNull).sorted()
                    .<QueryNode>map(db -> QueryClause.numeric(PREFIX_DB + db, NumberOp.RANGE_INCLUSIVE,
                            "1", Integer.toString(candidates), false))
                    .toList()), m -> m.setDbFilter(null)));
        }

        return facets;
    }

    /**
     * The quality clauses for one field, faithful to {@code FeatureFilterModel.makeQualityQuery}: one
     * {@code field:value} per selected data quality, plus a {@code field:NOT_APPLICABLE} fallback so
     * features without quality data still pass (added unconditionally, exactly as the model does).
     */
    private static List<QueryNode> qualityTerms(String field, QualityFilter filter) {
        List<QueryNode> terms = new ArrayList<>();
        filter.getDataQualities().stream().map(Object::toString).sorted()
                .forEach(quality -> terms.add(QueryClause.text(field, quality, false)));
        terms.add(QueryClause.text(field, DataQuality.NOT_APPLICABLE.toString(), false));
        return terms;
    }

    /**
     * A faithful inclusive range clause on the concrete current bounds - matching the model's
     * {@code DoublePoint.newRangeQuery(field, currentMin, currentMax)} (no wildcard substitution;
     * an untouched bound already equals the absolute bound, exactly as the model queries it).
     */
    private static QueryClause range(String field, double currentMin, double currentMax) {
        return QueryClause.numeric(field, NumberOp.RANGE_INCLUSIVE, number(currentMin), number(currentMax), false);
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
