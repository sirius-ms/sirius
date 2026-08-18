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
import de.unijena.bioinf.ms.persistence.model.core.tags.Groups;
import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.QuantMeasure;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.stream.Collectors;
import de.unijena.bioinf.ms.gui.utils.query.LogicOp;
import de.unijena.bioinf.ms.gui.utils.query.LuceneQueryCompiler;
import de.unijena.bioinf.ms.gui.utils.query.QueryContainer;
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;

import static de.unijena.bioinf.ms.persistence.model.core.DefaultQualityCategory.*;

/**
 * This model stores the filter criteria for a compound list
 */
public class FeatureFilterModel implements SiriusPCS {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);
    private final Analyzer analyzer;
    /*
    currently selected values
     */
    @Getter
    private boolean inverted;

    @Getter
    private final Document searchTextDoc;

    @Getter
    private double currentMinMz;
    @Getter
    private double currentMaxMz;
    @Getter
    private double currentMinRt;
    @Getter
    private double currentMaxRt;

    @Getter
    private double currentMinConfidence;
    @Getter
    private double currentMaxConfidence;


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

    // MSData filter
    @Getter
    private boolean hasMs1 = false;
    @Getter
    private boolean hasMsMs = true;

    private Set<PrecursorIonType> possibleAdducts = new HashSet<>();
    @Setter
    private Set<PrecursorIonType> adducts = new HashSet<>();

    /** null = no lipid filter, TRUE = a lipid class must be detected, FALSE = none may be. */
    @Getter
    private @Nullable Boolean lipidClassDetected = null;

    /** null = no pfas filter, TRUE = the feature must carry a pfas tag, FALSE = it must not. */
    @Getter
    private @Nullable Boolean pfasDetected = null;

    @NotNull
    private ElementFilter elementFilter = ElementFilter.disabled();

    @Nullable
    private DbFilter dbFilter;

    @Getter
    private final FoldChangeFilter sampleBlankFoldChange;
    /*
    min/max possible values
     */
    @Getter
    private final double minMz;
    @Getter
    private final double maxMz;
    @Getter
    private final double minRt;
    @Getter
    private final double maxRt;

    @Getter
    private final double minConfidence;
    @Getter
    private final double maxConfidence;


    public FeatureFilterModel() {
        this(0, 5000d, 0, 10000d, 0, 1d);
    }


    /**
     * the filter model is initialized with the min / max possible values
     * MAX VALUES SHOULD BE USED FOR DISPLAY ONLY. AND IF SELECTED VALUES EQUAL THE MAXIMUM, INFINITY SHOULD BE ASSUMED, see is[...]Active() methods.
     */
    @SneakyThrows
    private FeatureFilterModel(double minMz, double maxMz, double minRt, double maxRt, double minConfidence, double maxConfidence) {
        this.inverted = false;
        //this querybuilder is just to create the query string.
        // Therefore, we need to ensure values are not lowercased as in the default builder.
        this.analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .build();
        this.textFieldParser = new QueryParser(FAKE_FIELD, analyzer);

        this.searchTextDoc = new PlainDocument();

        this.sampleBlankFoldChange = new FoldChangeFilter(2.0);
        this.featureQualityFilter.getDataQualities().remove(DataQuality.BAD);
        this.featureQualityFilter.getDataQualities().remove(DataQuality.LOWEST);
        this.currentMinMz = minMz;
        this.currentMaxMz = maxMz;
        this.currentMinRt = minRt;
        this.currentMaxRt = maxRt;
        this.currentMinConfidence = minConfidence;
        this.currentMaxConfidence = maxConfidence;


        this.minMz = minMz;
        this.maxMz = maxMz;
        this.minRt = minRt;
        this.maxRt = maxRt;
        this.minConfidence = minConfidence;
        this.maxConfidence = maxConfidence;
    }

    public void fireUpdateCompleted() {
        //as long as we do not treat changes differently, we only have to listen to this event after performing all updates
        pcs.firePropertyChange("filterUpdateCompleted", null, this);
    }


    public boolean isLipidFilterEnabled() {
        return lipidClassDetected != null;
    }

    public void setLipidClassDetected(@Nullable Boolean value) {
        Boolean oldValue = lipidClassDetected;
        lipidClassDetected = value;
        pcs.firePropertyChange("setLipidClassDetected", oldValue, value);
    }

    public boolean isPfasFilterEnabled() {
        return pfasDetected != null;
    }

    public void setPfasDetected(@Nullable Boolean value) {
        Boolean oldValue = pfasDetected;
        pfasDetected = value;
        pcs.firePropertyChange("setPfasDetected", oldValue, value);
    }

    public void setDbFilter(@Nullable DbFilter dbFilter) {
        this.dbFilter = dbFilter;
    }

    @Nullable
    public DbFilter getDbFilter() {
        return dbFilter;
    }

    public boolean isDbFilterEnabled() {
        return dbFilter != null && !dbFilter.dbs.isEmpty();
    }

    public boolean isElementFilterEnabled() {
        return elementFilter.isActive();
    }

    @NotNull
    public ElementFilter getElementFilter() {
        return elementFilter;
    }

    public void setElementFilter(@NotNull ElementFilter value) {
        ElementFilter oldValue = elementFilter;
        elementFilter = value;
        pcs.firePropertyChange("setElementFilter", oldValue, value);
    }

    public void setHasMs1(boolean hasMs1) {
        boolean old = this.hasMs1;
        this.hasMs1 = hasMs1;
        pcs.firePropertyChange("setHasMs1", old, hasMs1);
    }

    public void setHasMsMs(boolean hasMsMs) {
        boolean old = this.hasMsMs;
        this.hasMsMs = hasMsMs;
        pcs.firePropertyChange("setHasMsMs", old, hasMsMs);
    }

    public void setInverted(boolean inverted) {
        boolean old = this.inverted;
        this.inverted = inverted;
        pcs.firePropertyChange("setInverted", old, inverted);
    }

    public void setCurrentMinMz(double currentMinMz) {
        if (currentMinMz < minMz) throw new IllegalArgumentException("current value out of range: " + currentMinMz);
        double oldValue = this.currentMinMz;
        this.currentMinMz = currentMinMz;
        pcs.firePropertyChange("setMinMz", oldValue, currentMinMz);
    }

    public void setCurrentMaxMz(double currentMaxMz) {
        if (currentMaxMz > maxMz) throw new IllegalArgumentException("current value out of range: " + currentMaxMz);
        double oldValue = this.currentMaxMz;
        this.currentMaxMz = currentMaxMz;
        pcs.firePropertyChange("setMaxMz", oldValue, currentMaxMz);
    }

    public void setCurrentMinRt(double currentMinRt) {
        if (currentMinRt < minRt) throw new IllegalArgumentException("current value out of range: " + currentMinRt);
        double oldValue = this.currentMinRt;
        this.currentMinRt = currentMinRt;
        pcs.firePropertyChange("setMinRt", oldValue, currentMinRt);

    }

    public void setCurrentMaxRt(double currentMaxRt) {
        if (currentMaxRt > maxRt) throw new IllegalArgumentException("current value out of range: " + currentMaxRt);
        double oldValue = this.currentMaxRt;
        this.currentMaxRt = currentMaxRt;
        pcs.firePropertyChange("setMaxRt", oldValue, currentMaxRt);

    }

    public void setCurrentMaxConfidence(double currentMaxConfidence) {
        if (currentMaxConfidence > maxConfidence)
            throw new IllegalArgumentException("current value out of range: " + currentMaxConfidence);
        double oldValue = this.currentMaxConfidence;
        this.currentMaxConfidence = currentMaxConfidence;
        pcs.firePropertyChange("setMaxConfidence", oldValue, currentMaxConfidence);

    }

    public void setCurrentMinConfidence(double currentMinConfidence) {
        if (currentMinConfidence < minConfidence)
            throw new IllegalArgumentException("current value out of range: " + currentMinConfidence);
        double oldValue = this.currentMinConfidence;
        this.currentMinConfidence = currentMinConfidence;
        pcs.firePropertyChange("setMinConfidence", oldValue, currentMinConfidence);

    }

    /**
     * filter options are active. that means selected values differ from absolute min/max
     *
     * @return true if active and false if not.
     */
    public boolean isActive() {
        if (hasMs1 || hasMsMs)
            return true;
        if (searchTextDoc.getLength() > 0)
            return true;
        if (currentMinMz != minMz || currentMaxMz != maxMz ||
                currentMinRt != minRt || currentMaxRt != maxRt ||
                currentMinConfidence != minConfidence || currentMaxConfidence != maxConfidence
        ) return true;
        if (!adducts.isEmpty()) return true;

        if (getCategorizedQualityFilters().stream().anyMatch(QualityFilter::isEnabled) || getFeatureQualityFilter().isEnabled() || isLipidFilterEnabled() || isPfasFilterEnabled() || isElementFilterEnabled() || isDbFilterEnabled())
            return true;

        if (getSampleBlankFoldChange().isEnabled()) // contributes a range clause to toLuceneQuery
            return true;

        return false;
    }

    public boolean isMaxMzFilterActive() {
        return currentMaxMz != maxMz;
    }

    public boolean isMinMzFilterActive() {
        return currentMinMz != minMz;
    }

    public boolean isMzFilterActive() {
        return isMinMzFilterActive() || isMaxMzFilterActive();
    }

    public boolean isMaxRtFilterActive() {
        return currentMaxRt != maxRt;
    }

    public boolean isMinRtFilterActive() {
        return currentMinRt != minRt;
    }

    public boolean isRtFilterActive() {
        return isMinRtFilterActive() || isMaxRtFilterActive();
    }


    public boolean isMaxConfidenceFilterActive() {
        return currentMaxConfidence != maxConfidence;
    }

    public boolean isMinConfidenceFilterActive() {
        return currentMinConfidence != minConfidence;
    }

    @Synchronized
    public void updateAdducts(Collection<PrecursorIonType> detectedAdductsInProject) {
        Set<PrecursorIonType> oldAdducts = new HashSet<>(this.adducts);

        Set<PrecursorIonType> listAdducts = new HashSet<>();
        listAdducts.addAll(detectedAdductsInProject);

        Set<PrecursorIonType> newAdducts = new HashSet<>(listAdducts);
        newAdducts.removeAll(possibleAdducts);
        possibleAdducts.retainAll(listAdducts);
        adducts.retainAll(listAdducts);
        possibleAdducts.addAll(newAdducts);
        adducts.addAll(newAdducts.stream().filter(FeatureFilterModel::isSupportedAdduct).collect(Collectors.toSet()));

        if (isAdductFilterActive() && !oldAdducts.equals(adducts)) { // if list of adducts in the actual filter changed, we have to refilter.
            fireUpdateCompleted();
        }else {
            pcs.firePropertyChange("possibleAdductsUpdated", null, this); // just notify gui components that the available adductes have changed.
        }
    }

    @Synchronized
    public Set<PrecursorIonType> getPossibleAdducts() {
        return Collections.unmodifiableSet(possibleAdducts);
    }

    @Synchronized
    public Set<PrecursorIonType> getSelectedAdducts() {
        return Collections.unmodifiableSet(adducts);
    }

    @Synchronized
    public boolean isAdductFilterActive() {
        return adducts != null && !adducts.isEmpty();
    }

    /**
     * A "supported" adduct: single-charged and monomeric. SIRIUS can only run its annotation (molecular
     * formula, structure and compound-class identification) on features with such adducts, so they are
     * the default adduct selection.
     */
    public static boolean isSupportedAdduct(@NotNull PrecursorIonType ion) {
        return !ion.isMultimere() && !ion.isMultipleCharged();
    }

    @Synchronized
    public boolean isMultiAdductsAllowed() {
        return !isAdductFilterActive() || adducts.stream().anyMatch(p -> !isSupportedAdduct(p));
    }

    @Synchronized
    public void removeMultiAdducts() {
        adducts = adducts.stream().filter(FeatureFilterModel::isSupportedAdduct).collect(Collectors.toSet());
        if (adducts.isEmpty())
            adducts = possibleAdducts.stream().filter(FeatureFilterModel::isSupportedAdduct).collect(Collectors.toSet());
    }

    @Synchronized
    public void addMultiAdducts() {
        if (!isAdductFilterActive())
            return;

        possibleAdducts.stream().filter(p -> !isSupportedAdduct(p)).forEach(adducts::add);
    }


    @Override
    public MutableHiddenChangeSupport pcs() {
        return pcs;
    }

    public void addUpdateCompleteListener(PropertyChangeListener listener) {
        addPropertyChangeListener("filterUpdateCompleted", listener);
    }

    public void resetFilter() {
        setInverted(false);

        clearSearchText();

        //trigger events
        setCurrentMinMz(minMz);
        setCurrentMaxMz(maxMz);
        setCurrentMinRt(minRt);
        setCurrentMaxRt(maxRt);
        setCurrentMaxConfidence(maxConfidence);
        setCurrentMinConfidence(minConfidence);
        getFeatureQualityFilter().reset();
        getPeakShapeQualityFilter().reset();
        setLipidClassDetected(null);
        setPfasDetected(null);
        setDbFilter(null);
        setElementFilter(ElementFilter.disabled());
        adducts = Set.of();
        setHasMs1(false);
        setHasMsMs(false);
        sampleBlankFoldChange.reset();
    }

    private void clearSearchText() {
        try {
            searchTextDoc.remove(0, searchTextDoc.getLength()); // Clear existing content
        } catch (BadLocationException e) {
            //ignored
        }
    }

    public boolean isSearchTextFilterActive() {
        return searchTextDoc.getLength() > 0;
    }

    public String getSearchText() {
        int l = searchTextDoc.getLength();
        if (l > 0) {
            try {
                return searchTextDoc.getText(0, l);
            } catch (BadLocationException e) {
                return null;
            }
        }
        return null;
    }


    private static final String FAKE_FIELD = "__FAKE_FIELD__";
    private final QueryParser textFieldParser;

    /**
     * The executed lucene query, or empty when no filter is active. Derived from the SAME facet nodes
     * the search bar renders ({@link PanelQueryNodeFactory}) compiled via {@link LuceneQueryCompiler},
     * combined (AND) with the free-text segment, and wrapped for inversion - so the executed query and
     * the chips share one definition (see the equivalence tests, incl. the live-backend one).
     */
    public Optional<String> toLuceneQuery(@NotNull ConfidenceDisplayMode confidenceMode) {
        if (!isActive())
            return Optional.empty();
        String core = compileCore(confidenceMode);
        if (core.isBlank())
            return Optional.empty();
        // inversion: a pure-negative query matches nothing, so anchor with match-all and exclude
        return Optional.of(isInverted() ? "*:* AND NOT (" + core + ")" : core);
    }

    /**
     * The active structured facets (as query nodes) AND the free-text segment, compiled to lucene as it
     * is EXECUTED - facets that are pure negations (no lipid class, no pfas tag) get the match-all anchor
     * they need to match anything at all, see {@link LuceneQueryCompiler#compileExecutable}.
     */
    private String compileCore(@NotNull ConfidenceDisplayMode confidenceMode) {
        List<QueryNode> nodes = PanelQueryNodeFactory.nodesFor(this, confidenceMode);
        List<LogicOp> ands = new ArrayList<>(Math.max(0, nodes.size() - 1));
        for (int i = 1; i < nodes.size(); i++)
            ands.add(LogicOp.AND);
        return LuceneQueryCompiler.compileExecutable(new QueryContainer(nodes, ands), freeTextQuery());
    }

    /**
     * The full-text segment: the user's search text used as-is when it parses as a lucene query,
     * otherwise quoted as a phrase so malformed input still runs (against the default search fields)
     * instead of failing the whole query.
     */
    private String freeTextQuery() {
        if (!isSearchTextFilterActive())
            return "";
        String text = getSearchText();
        try {
            textFieldParser.parse(text);
            return text;
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
    // of truth, used both to build the executed query (below) and to render the facets as query chips
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
