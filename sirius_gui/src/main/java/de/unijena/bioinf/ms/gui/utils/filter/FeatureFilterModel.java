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
import de.unijena.bioinf.ms.persistence.model.core.tags.Groups;
import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.QuantMeasure;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory.*;

/**
 * Holds the <b>applied</b> filter of the compound list - a {@link FeatureFilter} value - together with the
 * {@link FilterContext} it is interpreted against, publishes changes to it, and compiles it to the lucene
 * query the list is loaded with. The value is replaced whole ({@link #set}); nothing here is mutable state
 * of its own except the context, which follows the project.
 * <p>
 * The many fine-grained getters and setters are the pre-value API kept as delegating shims so that every
 * caller compiles unchanged; they will go once the editors work on a {@code FilterSession}. The shared
 * search-text {@link Document} is a <em>derived</em> view of the value's user query for the same reason: an
 * edit to the document lands in the value, and a new value is written into the document.
 */
public class FeatureFilterModel implements SiriusPCS {
    /** Fired with the previous and the new {@link FeatureFilter} whenever the applied value changes. */
    public static final String FILTER_CHANGED = "filterChanged";

    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);
    private final Analyzer analyzer;

    private FilterContext context;
    private FeatureFilter value;

    @Getter
    private final Document searchTextDoc;
    /** Set while {@link #set} writes the document, so the document listener does not write the value back. */
    private boolean syncingDocument = false;

    /**
     * How the user's own query text is turned into the form that is EXECUTED - the match-all anchor a lucene
     * query needs when it only excludes ({@code NOT lipid:true} matches nothing as written). The rule lives
     * where queries are compiled and needs the query as a tree, so it is handed in rather than reached for:
     * the search package supplies it (see {@code QueryStringParser#executableForm}), and until it does the
     * query runs exactly as written.
     */
    @Getter
    @Setter
    @NotNull
    private UnaryOperator<String> executableQueryForm = UnaryOperator.identity();

    // views over the value for the widgets that edit a quality selection / the blank fold change in place
    @Getter
    private final QualityFilter featureQualityFilter = new QualityFilter(null, "Feature Quality", this);
    @Getter
    private final QualityFilter peakShapeQualityFilter = new QualityFilter(PEAK_QUALITY, this);
    @Getter
    private final QualityFilter alignmentQuality = new QualityFilter(ALIGNMENT_QUALITY, this);
    @Getter
    private final QualityFilter isotopePatternQuality = new QualityFilter(ISOTOPE_QUALITY, this);
    @Getter
    private final QualityFilter fragmentationPatternQuality = new QualityFilter(MS2_QUALITY, this);
    @Getter
    private final QualityFilter adductAssignmentQuality = new QualityFilter(ADDUCT_QUALITY, this);
    @Getter
    private final List<QualityFilter> categorizedQualityFilters = List.of(peakShapeQualityFilter, alignmentQuality, isotopePatternQuality, fragmentationPatternQuality, adductAssignmentQuality);
    @Getter
    private final FoldChangeFilter sampleBlankFoldChange = new FoldChangeFilter(this);

    /** The default filter over an empty project context (no detected adducts yet). */
    public FeatureFilterModel() {
        this(FilterContext.EMPTY);
    }

    /** The default filter ({@link FeatureFilter#defaults}) over the given context. */
    @SneakyThrows
    public FeatureFilterModel(@NotNull FilterContext context) {
        this.context = context;
        this.value = FeatureFilter.defaults(context);
        //this querybuilder is just to create the query string.
        // Therefore, we need to ensure values are not lowercased as in the default builder.
        this.analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .build();
        this.textFieldParser = new QueryParser(FAKE_FIELD, analyzer);

        this.searchTextDoc = new PlainDocument();
        this.searchTextDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                documentEdited();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                documentEdited();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                documentEdited();
            }
        });
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
            syncDocumentToValue();
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
            syncDocumentToValue();
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

    // --- the shared search document: a derived view of the value's user query ---

    /** Writes the value's user query into the document when they differ; called under the monitor. */
    private void syncDocumentToValue() {
        String text = value.userQuery();
        if (text.equals(documentText()))
            return;
        syncingDocument = true;
        try {
            searchTextDoc.remove(0, searchTextDoc.getLength());
            searchTextDoc.insertString(0, text, null);
        } catch (BadLocationException e) {
            //ignored
        } finally {
            syncingDocument = false;
        }
    }

    /** An edit made to the document by a widget lands in the value (without re-filtering). */
    private void documentEdited() {
        if (syncingDocument)
            return;
        String text = documentText();
        update(f -> f.withUserQuery(text));
    }

    private String documentText() {
        try {
            return searchTextDoc.getText(0, searchTextDoc.getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    public boolean isSearchTextFilterActive() {
        return get().isUserQueryActive();
    }

    /** The user's query text, or null when there is none. */
    @Nullable
    public String getSearchText() {
        String text = get().userQuery();
        return text.isEmpty() ? null : text;
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

    public Set<PrecursorIonType> getPossibleAdducts() {
        return getContext().possibleAdducts();
    }

    public Set<PrecursorIonType> getSelectedAdducts() {
        return get().adducts();
    }

    public void setAdducts(@NotNull Set<PrecursorIonType> adducts) {
        update(f -> f.withAdducts(adducts));
    }

    public boolean isAdductFilterActive() {
        return get().isAdductFilterActive();
    }

    /** See {@link FeatureFilter#isSupportedAdduct}. */
    public static boolean isSupportedAdduct(@NotNull PrecursorIonType ion) {
        return FeatureFilter.isSupportedAdduct(ion);
    }

    public boolean isMultiAdductsAllowed() {
        return get().isMultiAdductsAllowed();
    }

    public void removeMultiAdducts() {
        update(f -> f.withSupportedAdductsOnly(getContext()));
    }

    public void addMultiAdducts() {
        update(f -> f.withMultiAdducts(getContext()));
    }

    // --- delegating shims: flags ---

    public boolean isInverted() {
        return get().inverted();
    }

    public void setInverted(boolean inverted) {
        update(f -> f.withInverted(inverted));
    }

    @NotNull
    public LogicOp getQueryJoin() {
        return get().queryJoin();
    }

    public void setQueryJoin(@NotNull LogicOp queryJoin) {
        update(f -> f.withQueryJoin(queryJoin));
    }

    public boolean isHasMs1() {
        return get().hasMs1();
    }

    public void setHasMs1(boolean hasMs1) {
        update(f -> f.withHasMs1(hasMs1));
    }

    public boolean isHasMsMs() {
        return get().hasMsMs();
    }

    public void setHasMsMs(boolean hasMsMs) {
        update(f -> f.withHasMsMs(hasMsMs));
    }

    @Nullable
    public Boolean getLipidClassDetected() {
        return get().lipidClassDetected();
    }

    public void setLipidClassDetected(@Nullable Boolean value) {
        update(f -> f.withLipidClassDetected(value));
    }

    public boolean isLipidFilterEnabled() {
        return get().isLipidFilterEnabled();
    }

    @Nullable
    public Boolean getPfasDetected() {
        return get().pfasDetected();
    }

    public void setPfasDetected(@Nullable Boolean value) {
        update(f -> f.withPfasDetected(value));
    }

    public boolean isPfasFilterEnabled() {
        return get().isPfasFilterEnabled();
    }

    @Nullable
    public DbFilter getDbFilter() {
        return get().dbFilter();
    }

    public void setDbFilter(@Nullable DbFilter dbFilter) {
        update(f -> f.withDbFilter(dbFilter));
    }

    public boolean isDbFilterEnabled() {
        return get().isDbFilterEnabled();
    }

    @NotNull
    public ElementFilter getElementFilter() {
        return get().elementFilter();
    }

    public void setElementFilter(@NotNull ElementFilter value) {
        update(f -> f.withElementFilter(value));
    }

    public boolean isElementFilterEnabled() {
        return get().isElementFilterEnabled();
    }

    // --- delegating shims: ranges (values and bounds) ---

    public double getMinMz() {
        return getContext().minMz();
    }

    public double getMaxMz() {
        return getContext().maxMz();
    }

    public double getMinRt() {
        return getContext().minRt();
    }

    public double getMaxRt() {
        return getContext().maxRt();
    }

    public double getMinConfidence() {
        return getContext().minConfidence();
    }

    public double getMaxConfidence() {
        return getContext().maxConfidence();
    }

    public double getCurrentMinMz() {
        return get().mz().min();
    }

    public double getCurrentMaxMz() {
        return get().mz().max();
    }

    public double getCurrentMinRt() {
        return get().rt().min();
    }

    public double getCurrentMaxRt() {
        return get().rt().max();
    }

    public double getCurrentMinConfidence() {
        return get().confidence().min();
    }

    public double getCurrentMaxConfidence() {
        return get().confidence().max();
    }

    public void setCurrentMinMz(double currentMinMz) {
        if (currentMinMz < getMinMz()) throw new IllegalArgumentException("current value out of range: " + currentMinMz);
        update(f -> f.withMz(currentMinMz, f.mz().max()));
    }

    public void setCurrentMaxMz(double currentMaxMz) {
        if (currentMaxMz > getMaxMz()) throw new IllegalArgumentException("current value out of range: " + currentMaxMz);
        update(f -> f.withMz(f.mz().min(), currentMaxMz));
    }

    public void setCurrentMinRt(double currentMinRt) {
        if (currentMinRt < getMinRt()) throw new IllegalArgumentException("current value out of range: " + currentMinRt);
        update(f -> f.withRt(currentMinRt, f.rt().max()));
    }

    public void setCurrentMaxRt(double currentMaxRt) {
        if (currentMaxRt > getMaxRt()) throw new IllegalArgumentException("current value out of range: " + currentMaxRt);
        update(f -> f.withRt(f.rt().min(), currentMaxRt));
    }

    public void setCurrentMinConfidence(double currentMinConfidence) {
        if (currentMinConfidence < getMinConfidence())
            throw new IllegalArgumentException("current value out of range: " + currentMinConfidence);
        update(f -> f.withConfidence(currentMinConfidence, f.confidence().max()));
    }

    public void setCurrentMaxConfidence(double currentMaxConfidence) {
        if (currentMaxConfidence > getMaxConfidence())
            throw new IllegalArgumentException("current value out of range: " + currentMaxConfidence);
        update(f -> f.withConfidence(f.confidence().min(), currentMaxConfidence));
    }

    // --- delegating shims: what is active ---

    /**
     * filter options are active. that means selected values differ from absolute min/max
     *
     * @return true if active and false if not.
     */
    public boolean isActive() {
        return get().isActive(getContext());
    }

    public boolean isMaxMzFilterActive() {
        return get().mz().isMaxActive(getMaxMz());
    }

    public boolean isMinMzFilterActive() {
        return get().mz().isMinActive(getMinMz());
    }

    public boolean isMzFilterActive() {
        return get().isMzFilterActive(getContext());
    }

    public boolean isMaxRtFilterActive() {
        return get().rt().isMaxActive(getMaxRt());
    }

    public boolean isMinRtFilterActive() {
        return get().rt().isMinActive(getMinRt());
    }

    public boolean isRtFilterActive() {
        return get().isRtFilterActive(getContext());
    }

    public boolean isMaxConfidenceFilterActive() {
        return get().confidence().isMaxActive(getMaxConfidence());
    }

    public boolean isMinConfidenceFilterActive() {
        return get().confidence().isMinActive(getMinConfidence());
    }

    // --- compilation ---

    private static final String FAKE_FIELD = "__FAKE_FIELD__";
    private final QueryParser textFieldParser;

    /**
     * The executed lucene query, or empty when no filter is active. Derived from the SAME facet nodes
     * the search bar renders ({@link PanelQueryNodeFactory}) compiled via {@link LuceneQueryCompiler},
     * combined (AND) with the free-text segment, and wrapped for inversion - so the executed query and
     * the chips share one definition (see the equivalence tests, incl. the live-backend one).
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
     * The active structured facets (as query nodes) joined with the free-text segment - by AND, or by OR
     * when {@link FeatureFilter#queryJoin()} says so - compiled to lucene as it is EXECUTED - facets that
     * are pure negations (no lipid class, no pfas tag) get the match-all anchor they need to match anything
     * at all, see {@link LuceneQueryCompiler#compileExecutable}.
     */
    private String compileCore(@NotNull FeatureFilter filter, @NotNull FilterContext ctx, @NotNull ConfidenceDisplayMode confidenceMode) {
        List<QueryNode> nodes = PanelQueryNodeFactory.nodesFor(filter, ctx, confidenceMode);
        List<LogicOp> ands = new ArrayList<>(Math.max(0, nodes.size() - 1));
        for (int i = 1; i < nodes.size(); i++)
            ands.add(LogicOp.AND);
        return LuceneQueryCompiler.compileExecutable(new QueryContainer(nodes, ands), freeTextQuery(filter.userQuery()), filter.queryJoin());
    }

    /**
     * The full-text segment: the user's search text in its executable form when it parses as a lucene query
     * (see {@link #getExecutableQueryForm()}), otherwise quoted as a phrase so malformed input still runs
     * (against the default search fields) instead of failing the whole query.
     */
    private String freeTextQuery(@NotNull String text) {
        if (text.isEmpty())
            return "";
        try {
            synchronized (textFieldParser) { // lucene's classic parser is not thread-safe
                textFieldParser.parse(text);
            }
            // it is a query - so run the form that a query with only negations needs to match anything
            return executableQueryForm.apply(text);
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
    // (PanelQueryNodeFactory). Must match the backend @IndexField names.
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
