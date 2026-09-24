package de.unijena.bioinf.ms.gui.utils.filter;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.query.LogicOp;
import de.unijena.bioinf.ms.gui.utils.query.LuceneQueryCompiler;
import de.unijena.bioinf.ms.gui.utils.query.QueryContainer;
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;
import de.unijena.bioinf.ms.gui.utils.search.QueryStringParser;
import de.unijena.bioinf.ms.persistence.model.core.tags.Groups;
import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.QuantMeasure;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Holds the <b>applied</b> filter of the compound list - a {@link FeatureFilter} value - together with the
 * {@link FilterContext} it is interpreted against, publishes changes to it, and compiles it to the lucene
 * query the list is loaded with. The value is replaced whole ({@link #set}); the only state of its own is the
 * context, which follows the project. Editing happens on a {@link FilterSession}'s working value and lands
 * here through {@link FilterSession#commit()}.
 */
public class FeatureFilterModel implements SiriusPCS {
    /** Fired with the previous and the new {@link FeatureFilter} whenever the applied value changes. */
    public static final String FILTER_CHANGED = "filterChanged";

    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);
    private final Analyzer analyzer;
    private final QueryParser textFieldParser;

    private FilterContext context;
    private FeatureFilter value;

    /** The default filter over an empty project context (no detected adducts yet). */
    public FeatureFilterModel() {
        this(FilterContext.EMPTY);
    }

    /** The default filter ({@link FeatureFilter#defaults}) over the given context. */
    @SneakyThrows
    public FeatureFilterModel(@NotNull FilterContext context) {
        this.context = context;
        this.value = FeatureFilter.defaults(context);
        // only to check whether the user's text is a lucene query at all; values must not be lowercased
        // as in the default builder
        this.analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .build();
        this.textFieldParser = new QueryParser(FAKE_FIELD, analyzer);
    }

    // --- the value ---

    /** The applied filter. */
    public synchronized FeatureFilter get() {
        return value;
    }

    /** The project context the applied filter is interpreted against. */
    public synchronized FilterContext getContext() {
        return context;
    }

    /**
     * Replaces the applied filter and fires {@link #FILTER_CHANGED}. Does NOT fire
     * {@link #fireUpdateCompleted()} - the caller decides when the list is re-filtered, so a batch of edits
     * fires once.
     */
    public void set(@NotNull FeatureFilter newValue) {
        FeatureFilter old;
        synchronized (this) {
            old = value;
            value = newValue;
        }
        pcs.firePropertyChange(FILTER_CHANGED, old, newValue);
    }

    /** {@link #set}s the result of applying {@code edit} to the current value. */
    public void update(@NotNull UnaryOperator<FeatureFilter> edit) {
        FeatureFilter old, updated;
        synchronized (this) {
            old = value;
            updated = edit.apply(old);
            value = updated;
        }
        pcs.firePropertyChange(FILTER_CHANGED, old, updated);
    }

    public void fireUpdateCompleted() {
        //as long as we do not treat changes differently, we only have to listen to this event after performing all updates
        pcs.firePropertyChange("filterUpdateCompleted", null, this);
    }

    @Override
    public MutableHiddenChangeSupport pcs() {
        return pcs;
    }

    public void addUpdateCompleteListener(PropertyChangeListener listener) {
        addPropertyChangeListener("filterUpdateCompleted", listener);
    }

    // --- resets ---

    /** Everything off, query cleared. Fires {@link #FILTER_CHANGED} only; the caller fires {@link #fireUpdateCompleted()}. */
    public void resetFilter() {
        set(FeatureFilter.empty(getContext()));
    }

    /**
     * Resets the filter to SIRIUS's recommended DEFAULT configuration instead of clearing it - the committed
     * counterpart of the filter dialog's "Restore default filters", see {@link FeatureFilter#defaults}. Fires
     * {@link #FILTER_CHANGED} only; the caller fires {@link #fireUpdateCompleted()} afterwards.
     */
    public void resetFilterToDefaults() {
        set(FeatureFilter.defaults(getContext()));
    }

    // --- adducts: the one place the context changes ---

    /**
     * The project's detected adducts changed: the context follows, and the selection is reconciled
     * ({@link FeatureFilter#withAdductsReconciled}). Re-filters when that changed an active adduct filter.
     */
    public void updateAdducts(Collection<PrecursorIonType> detectedAdductsInProject) {
        Set<PrecursorIonType> listAdducts = new HashSet<>(detectedAdductsInProject);
        listAdducts.addAll(unknownAdductsOf(detectedAdductsInProject));

        FeatureFilter old, updated;
        synchronized (this) {
            old = value;
            Set<PrecursorIonType> previouslyPossible = context.possibleAdducts();
            context = context.withPossibleAdducts(listAdducts);
            updated = old.withAdductsReconciled(previouslyPossible, context);
            value = updated;
        }
        pcs.firePropertyChange(FILTER_CHANGED, old, updated);
        if (updated.isAdductFilterActive() && !old.adducts().equals(updated.adducts())) { // if list of adducts in the actual filter changed, we have to refilter.
            fireUpdateCompleted();
        } else {
            pcs.firePropertyChange("possibleAdductsUpdated", null, this); // just notify gui components that the available adductes have changed.
        }
    }

    /**
     * The unknown ion types the filter has to offer beside the detected ones: a feature whose adduct could
     * not be determined is indexed as {@code [M+?]+} / {@code [M+?]-} (never with an empty adduct field),
     * so without them in the list the default selection of all detected adducts silently drops every such
     * feature - which is most of them in a project that was never preprocessed for adducts.
     * <p>
     * The polarity follows the project: the unknown type of every charge that occurs among the detected
     * adducts, and both when nothing has been detected at all (there is nothing to infer from, and the one
     * that does not occur simply matches nothing).
     */
    private static Set<PrecursorIonType> unknownAdductsOf(Collection<PrecursorIonType> detected) {
        Set<PrecursorIonType> unknown = detected.stream()
                .map(adduct -> PrecursorIonType.unknown(adduct.getCharge()))
                .collect(Collectors.toSet());
        return unknown.isEmpty()
                ? Set.of(PrecursorIonType.unknownPositive(), PrecursorIonType.unknownNegative())
                : unknown;
    }

    // --- compilation ---

    private static final String FAKE_FIELD = "__FAKE_FIELD__";

    /**
     * The executed lucene query, or empty when no filter is active. Derived from the SAME facet nodes
     * the search bar renders ({@link PanelQueryNodeFactory}) compiled via {@link LuceneQueryCompiler},
     * combined with the user's query by the block joint, and wrapped for inversion - so the executed query
     * and the chips share one definition (see the equivalence tests, incl. the live-backend one).
     */
    public Optional<String> toLuceneQuery(@NotNull ConfidenceDisplayMode confidenceMode) {
        FeatureFilter filter;
        FilterContext ctx;
        synchronized (this) {
            filter = value;
            ctx = context;
        }
        if (!filter.isActive(ctx))
            return Optional.empty();
        String core = compileCore(filter, ctx, confidenceMode);
        if (core.isBlank())
            return Optional.empty();
        // inversion: a pure-negative query matches nothing, so anchor with match-all and exclude
        return Optional.of(filter.inverted() ? "*:* AND NOT (" + core + ")" : core);
    }

    /**
     * The active structured facets (as query nodes) joined with the user's query - by AND, or by OR when
     * {@link FeatureFilter#queryJoin()} says so - compiled to lucene as it is EXECUTED: facets that are pure
     * negations (no lipid class, no pfas tag) get the match-all anchor they need to match anything at all,
     * see {@link LuceneQueryCompiler#compileExecutable}.
     */
    private String compileCore(@NotNull FeatureFilter filter, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode confidenceMode) {
        List<QueryNode> nodes = PanelQueryNodeFactory.nodesFor(filter, ctx, confidenceMode);
        List<LogicOp> ands = new ArrayList<>(Math.max(0, nodes.size() - 1));
        for (int i = 1; i < nodes.size(); i++)
            ands.add(LogicOp.AND);
        return LuceneQueryCompiler.compileExecutable(new QueryContainer(nodes, ands), userQueryToRun(filter.userQuery()), filter.queryJoin());
    }

    /**
     * The user's query as it has to be RUN: in its executable form when it is a lucene query (the match-all
     * anchor a query with only negations needs, see {@link QueryStringParser#executableForm}), otherwise
     * quoted as a phrase so malformed input still runs (against the default search fields) instead of failing
     * the whole query.
     */
    private String userQueryToRun(@NotNull String text) {
        if (text.isEmpty())
            return "";
        try {
            synchronized (textFieldParser) { // lucene's classic parser is not thread-safe
                textFieldParser.parse(text);
            }
            return QueryStringParser.executableForm(text);
        } catch (ParseException e) {
            return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    public static final String BLANK_REMOVAL_SEARCH_FIELD_NAME = "stats.foldChange" +
            "." + Groups.SAMPLE_RUNS.getGroupName() +
            "." + Groups.BLANK_RUNS.getGroupName() +
            "." + QuantMeasure.APEX_INTENSITY +
            "." + AggregationType.AVG;

    // Canonical lucene index field names of the structured filter facets - the single GUI-side source
    // of truth, used both to build the executed query (above) and to render the facets as query chips
    // (PanelQueryNodeFactory / FacetCodecs). Must match the backend @IndexField names.
    public static final String FIELD_MZ = "ionMass";
    public static final String FIELD_RT_START = "rtStartSeconds";
    public static final String FIELD_RT_APEX = "rtApexSeconds";
    public static final String FIELD_RT_END = "rtEndSeconds";
    public static final String FIELD_CONFIDENCE_APPROX = "topAnnotations.confidenceApproxMatch";
    public static final String FIELD_CONFIDENCE_EXACT = "topAnnotations.confidenceExactMatch";
    public static final String FIELD_HAS_MS1 = "hasMs1";
    public static final String FIELD_HAS_MSMS = "hasMsMs";
    public static final String FIELD_ADDUCTS = "detectedAdducts";
    public static final String FIELD_QUALITY = "quality";
    public static final String PREFIX_CATEGORIZED_QUALITY = "qualities.";
    public static final String PREFIX_ELEMENT = "topAnnotations.formulaAnnotation.molecularFormula.";
    public static final String FIELD_LIPID = "topAnnotations.formulaAnnotation.lipidAnnotation.lipid";
    public static final String PREFIX_DB = "topAnnotations.matchedDatabases.";
    /**
     * The dynamic tag field of the pfas tag SIRIUS assigns during preprocessing/annotation. Mirrored as a
     * literal like the other field names: the GUI knows the index only through the REST API.
     */
    public static final String FIELD_PFAS = "tags.pfas";

}
