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

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
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
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.stream.Collectors;

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

    @Getter
    private LipidFilter lipidFilter = LipidFilter.KEEP_ALL_COMPOUNDS;

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
        return lipidFilter != LipidFilter.KEEP_ALL_COMPOUNDS;
    }

    public void setLipidFilter(LipidFilter value) {
        LipidFilter oldValue = lipidFilter;
        lipidFilter = value;
        pcs.firePropertyChange("setLipidFilter", oldValue, value);
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

        if (getCategorizedQualityFilters().stream().anyMatch(QualityFilter::isEnabled) || getFeatureQualityFilter().isEnabled() || isLipidFilterEnabled() || isElementFilterEnabled() || isDbFilterEnabled())
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
        adducts.addAll(newAdducts.stream().filter(p -> !p.isMultimere() && !p.isMultipleCharged()).collect(Collectors.toSet()));

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

    @Synchronized
    public boolean isMultiAdductsAllowed() {
        return !isAdductFilterActive() || adducts.stream().anyMatch(p -> p.isMultipleCharged() || p.isMultimere());
    }

    @Synchronized
    public void removeMultiAdducts() {
        adducts = adducts.stream().filter(p -> !p.isMultipleCharged()).filter(p -> !p.isMultimere()).collect(Collectors.toSet());
        if (adducts.isEmpty())
            adducts = possibleAdducts.stream().filter(p -> !p.isMultimere() && !p.isMultipleCharged()).collect(Collectors.toSet());
    }

    @Synchronized
    public void addMultiAdducts() {
        if (!isAdductFilterActive())
            return;

        possibleAdducts.stream().filter(p -> p.isMultimere() || p.isMultipleCharged()).forEach(adducts::add);
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
        setLipidFilter(LipidFilter.KEEP_ALL_COMPOUNDS);
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


    public enum LipidFilter {
        KEEP_ALL_COMPOUNDS, ANY_LIPID_CLASS_DETECTED, NO_LIPID_CLASS_DETECTED
    }


    private static final String FAKE_FIELD = "__FAKE_FIELD__";
    private static final String FAKE_FIELD_REPLACE = FAKE_FIELD + ":";
    private final QueryParser textFieldParser;

    public Optional<String> toLuceneQuery(@NotNull ConfidenceDisplayMode confidenceMode) {
        if (!isActive())
            return Optional.empty();
        return finalizeQuery(toLuceneQueryBuilder(confidenceMode, getSearchText()).build());
    }

    /**
     * The full query (model filters combined with the given user query) as it would be executed,
     * independent of the shared search-text document. Used to copy the complete query - including the
     * filter-panel part - while the user query is still being built in the search overlay.
     */
    public Optional<String> toLuceneQuery(@NotNull ConfidenceDisplayMode confidenceMode, @Nullable String userQuery) {
        BooleanQuery mainQuery = toLuceneQueryBuilder(confidenceMode, userQuery).build();
        if (mainQuery.clauses().isEmpty())
            return Optional.empty();
        return finalizeQuery(mainQuery);
    }

    private Optional<String> finalizeQuery(BooleanQuery mainQuery) {
        if (isInverted()) {
            mainQuery = new BooleanQuery.Builder()
                    .add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST)  // Include all documents (*:*)
                    .add(mainQuery, BooleanClause.Occur.MUST_NOT)  // Exclude the original query
                    .build();
        }

        return Optional.of(mainQuery.toString()
                .replace(FAKE_FIELD_REPLACE, "") //fake field to trick lucene
                .replace("Infinity]", "*]").replace("[-Infinity", "[*")); // tostring of query seems to be buggy, Infinity is the suggested way by lucene but the resulting query string is wrong.
    }

    public Optional<String> toLuceneQueryWithIds(@NotNull ConfidenceDisplayMode confidenceMode, String... alFeatureIds) {
        if (!isActive() && alFeatureIds.length == 0)
            return Optional.empty();

        BooleanQuery.Builder builder = toLuceneQueryBuilder(confidenceMode, getSearchText());
        if (alFeatureIds.length > 0) {
            BooleanQuery.Builder idQuery = new BooleanQuery.Builder();
            for (String fid : alFeatureIds) {
                idQuery.add(new TermQuery(new Term("alignedFeatureId", fid)), BooleanClause.Occur.SHOULD);
            }
            builder.add(idQuery.build(), BooleanClause.Occur.MUST);
        }

        if (isInverted()) {
            builder = new BooleanQuery.Builder()
                    .add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST)  // Include all documents (*:*)
                    .add(builder.build(), BooleanClause.Occur.MUST_NOT);  // Exclude the original query
        }

        return Optional.of(builder.build().toString()
                .replace(FAKE_FIELD_REPLACE, "")  //fake field to trick lucene
                .replace("Infinity]", "*]").replace("[-Infinity", "[*")); // tostring of query seems to be buggy, Infinity is the suggested way by lucene but the resulting query string is wrong.
    }

    private static final String BLANK_REMOVAL_SEARCH_FIELD_NAME = "stats.foldChange" +
            "." + Groups.SAMPLE_RUNS.getGroupName() +
            "." + Groups.BLANK_RUNS.getGroupName() +
            "." + QuantMeasure.APEX_INTENSITY +
            "." + AggregationType.AVG;

    private BooleanQuery.Builder toLuceneQueryBuilder(@NotNull ConfidenceDisplayMode confidenceMode, @Nullable String searchText) {
        // Combine queries using BooleanQuery.Builder
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        if (isMzFilterActive())
            booleanQuery.add(DoublePoint.newRangeQuery("ionMass", currentMinMz, currentMaxMz), BooleanClause.Occur.MUST);

        if (isRtFilterActive()) {
            BooleanQuery.Builder rtQuery = new BooleanQuery.Builder();

            rtQuery.add(DoublePoint.newRangeQuery("rtStartSeconds", currentMinRt, currentMaxRt), BooleanClause.Occur.SHOULD);
            rtQuery.add(DoublePoint.newRangeQuery("rtApexSeconds", currentMinRt, currentMaxRt), BooleanClause.Occur.SHOULD);
            rtQuery.add(DoublePoint.newRangeQuery("rtEndSeconds", currentMinRt, currentMaxRt), BooleanClause.Occur.SHOULD);

            booleanQuery.add(rtQuery.build(), BooleanClause.Occur.MUST);
        }

        if (isMinConfidenceFilterActive() || isMaxConfidenceFilterActive()) {
            String confidenceField = confidenceMode == ConfidenceDisplayMode.APPROXIMATE ? "topAnnotations.confidenceApproxMatch" : "topAnnotations.confidenceExactMatch";
            booleanQuery.add(DoublePoint.newRangeQuery(confidenceField, currentMinConfidence, currentMaxConfidence), BooleanClause.Occur.MUST);
        }

        if (isHasMs1())
            booleanQuery.add(new TermQuery(new Term("hasMs1", "true")), BooleanClause.Occur.MUST);

        if (isHasMsMs())
            booleanQuery.add(new TermQuery(new Term("hasMsMs", "true")), BooleanClause.Occur.MUST);

        if (isAdductFilterActive())
            booleanQuery.add(makeAdductQuery("detectedAdducts", getSelectedAdducts()), BooleanClause.Occur.MUST);

        if (getFeatureQualityFilter().isEnabled())
            booleanQuery.add(makeQualityQuery("quality", getFeatureQualityFilter()).build(), BooleanClause.Occur.MUST);

        getCategorizedQualityFilters().stream().filter(QualityFilter::isEnabled)
                .forEach(filter -> {
                    BooleanQuery.Builder qualityQuery = makeQualityQuery("qualities." + filter.getId(), filter);
                    //to allow matching data without quality data.
                    qualityQuery.add(new TermQuery(new Term("quality", DataQuality.NOT_APPLICABLE.toString())), BooleanClause.Occur.SHOULD);
                    booleanQuery.add(qualityQuery.build(), BooleanClause.Occur.MUST);
                });

        if (isElementFilterEnabled())
            booleanQuery.add(makeElementFilter("topAnnotations.formulaAnnotation.molecularFormula.", elementFilter.getConstraints()), BooleanClause.Occur.MUST);

        //TAG FILTERS
        if (getSampleBlankFoldChange().isEnabled())
            booleanQuery.add(DoublePoint.newRangeQuery(BLANK_REMOVAL_SEARCH_FIELD_NAME, getSampleBlankFoldChange().getCurrentMinFoldChange(), Double.POSITIVE_INFINITY), BooleanClause.Occur.MUST);

        //RESULT FILTERS
        //todo we can now also filter efficiently for exact lipid classes.
        if (isLipidFilterEnabled()) {
            TermQuery lipidQuery = new TermQuery(new Term("topAnnotations.formulaAnnotation.lipidAnnotation.lipid", "true"));
            if (lipidFilter == LipidFilter.ANY_LIPID_CLASS_DETECTED) {
                booleanQuery.add(lipidQuery, BooleanClause.Occur.MUST);
            } else if (lipidFilter == LipidFilter.NO_LIPID_CLASS_DETECTED) {
                booleanQuery.add(lipidQuery, BooleanClause.Occur.MUST_NOT);
            }
        }

        if (isDbFilterEnabled())
            booleanQuery.add(makeDbQuery("topAnnotations.matchedDatabases.", dbFilter), BooleanClause.Occur.MUST);

        // Handling searchText (Full-text search)
        if (searchText != null && !searchText.isBlank()) {
            try {
                // Try to parse the search text as a Lucene query
                booleanQuery.add(textFieldParser.parse(searchText), BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                // If parsing fails, treat as a simple text search
                // The backend already has standard fields defined for simple search queries
                booleanQuery.add(new TermQuery(new Term(FAKE_FIELD, searchText)), BooleanClause.Occur.MUST);
            }
        }

        return booleanQuery;
    }

    //todo Optimize: we could check if constrains are just a formula and build exact match query instead.
    private static Query makeElementFilter(String fieldPrefix, FormulaConstraints constrains) {
        BooleanQuery.Builder dbQuery = new BooleanQuery.Builder();
        constrains.getChemicalAlphabet().forEach(element -> {
            int lower = constrains.getLowerbound(element);
            int upper = constrains.getUpperbound(element);
            dbQuery.add(IntPoint.newRangeQuery(fieldPrefix + element.getSymbol(), lower, upper), BooleanClause.Occur.MUST);
        });
        return dbQuery.build();
    }

    private static Query makeDbQuery(String fieldPrefix, DbFilter filter) {
        BooleanQuery.Builder dbQuery = new BooleanQuery.Builder();
        filter.dbs.forEach(db -> dbQuery.add(IntPoint.newRangeQuery(fieldPrefix + db.getDatabaseId(), 1, filter.getNumOfCandidates()), BooleanClause.Occur.SHOULD));
        return dbQuery.build();
    }

    private static BooleanQuery.Builder makeQualityQuery(String fieldName, QualityFilter filter) {
        BooleanQuery.Builder qualityQuery = new BooleanQuery.Builder();
        filter.getDataQualities().forEach(q -> qualityQuery.add(new TermQuery(new Term(fieldName, q.toString())), BooleanClause.Occur.SHOULD));
        //to allow matching data without quality data.
        qualityQuery.add(new TermQuery(new Term(fieldName, DataQuality.NOT_APPLICABLE.toString())), BooleanClause.Occur.SHOULD);
        return qualityQuery;
    }

    private static Query makeAdductQuery(String fieldName, Set<PrecursorIonType> adducts) {
        BooleanQuery.Builder qualityQuery = new BooleanQuery.Builder();
        adducts.stream().map(p -> "\"" + p + "\"").forEach(adduct -> qualityQuery.add(new TermQuery(new Term(fieldName, adduct)), BooleanClause.Occur.SHOULD));
        return qualityQuery.build();

    }
}
