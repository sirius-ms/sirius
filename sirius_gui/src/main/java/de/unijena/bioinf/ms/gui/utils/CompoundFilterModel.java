package de.unijena.bioinf.ms.gui.utils;/*
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

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import io.sirius.ms.sdk.model.DataQuality;
import io.sirius.ms.sdk.model.SearchableDatabase;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.apache.commons.text.CaseUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This model stores the filter criteria for a compound list
 */
public class CompoundFilterModel implements SiriusPCS {
    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

//    private static final Set<PrecursorIonType> DEFAULT_ADDUCTS = Collections.unmodifiableSet(PeriodicTable.getInstance().getAdductsAndUnKnowns()
//            .stream().filter(p -> !p.isMultipleCharged()).filter(p -> !p.isMultimere()).collect(Collectors.toSet()));

    /*
    currently selected values
     */
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
    private final QualityFilter featureQualityFilter = new QualityFilter("Feature Quality");
    @Getter
    private final QualityFilter peakShapeQualityFilter = new QualityFilter("Peak Quality");
    @Getter
    private final QualityFilter alignmentQuality = new QualityFilter( "Alignment Quality");
    @Getter
    private final QualityFilter isotopePatternQuality = new QualityFilter( "Isotope Pattern Quality");
    @Getter
    private final QualityFilter fragmentationPatternQuality = new QualityFilter("Fragmentation Pattern Quality");
    @Getter
    private final QualityFilter adductAssignmentQuality = new QualityFilter("Adduct Assignment Quality");
    @Getter
    private final List<QualityFilter> ioQualityFilters = List.of(peakShapeQualityFilter, alignmentQuality, isotopePatternQuality, fragmentationPatternQuality, adductAssignmentQuality);

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


    public CompoundFilterModel() {
        this(0, 5000d, 0, 10000d, 0, 1d);
    }


    /**
     * the filter model is initialized with the min / max possible values
     * MAX VALUES SHOULD BE USED FOR DISPLAY ONLY. AND IF SELECTED VALUES EQUAL THE MAXIMUM, INFINITY SHOULD BE ASSUMED, see {@link CompoundFilterMatcher} and is[...]Active() methods.
     *
     * @param minMz
     * @param maxMz
     * @param minRt
     * @param maxRt
     * @param minConfidence
     * @param maxConfidence
     */
    public CompoundFilterModel(double minMz, double maxMz, double minRt, double maxRt, double minConfidence, double maxConfidence) {
        this.featureQualityFilter.dataQualities.remove(DataQuality.BAD);
        this.featureQualityFilter.dataQualities.remove(DataQuality.LOWEST);
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
        if (currentMinMz != minMz || currentMaxMz != maxMz ||
                currentMinRt != minRt || currentMaxRt != maxRt ||
                currentMinConfidence != minConfidence || currentMaxConfidence != maxConfidence
        ) return true;
        if (!adducts.isEmpty()) return true;
        if (getIoQualityFilters().stream().anyMatch(QualityFilter::isEnabled) || getFeatureQualityFilter().isEnabled() || isLipidFilterEnabled() || isElementFilterEnabled() || isDbFilterEnabled())
            return true;

        return false;
    }

    public boolean isMaxMzFilterActive() {
        return currentMaxMz != maxMz;
    }

    public boolean isMaxRtFilterActive() {
        return currentMaxRt != maxRt;
    }

    public boolean isMaxConfidenceFilterActive() {
        return currentMaxConfidence != maxConfidence;
    }

    public boolean isMinConfidenceFilterActive() {
        return currentMinConfidence != minConfidence;
    }

    @Synchronized
    public void updateAdducts(List<InstanceBean> compoundList) {
        Set<PrecursorIonType> listAdducts = new HashSet<>();
        for (InstanceBean instanceBean : compoundList) {
            listAdducts.addAll(instanceBean.getDetectedAdductsIncludingUnknown());
        }
        Set<PrecursorIonType> newAdducts = new HashSet<>(listAdducts);
        newAdducts.removeAll(possibleAdducts);
        possibleAdducts.retainAll(listAdducts);
        adducts.retainAll(listAdducts);
        possibleAdducts.addAll(newAdducts);
        adducts.addAll(newAdducts.stream().filter(p -> !p.isMultimere() && !p.isMultipleCharged()).collect(Collectors.toSet()));
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
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    public void resetFilter() {
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
    }

    public enum LipidFilter {
        KEEP_ALL_COMPOUNDS, ANY_LIPID_CLASS_DETECTED, NO_LIPID_CLASS_DETECTED
    }

    public static class DbFilter {
        final List<SearchableDatabase> dbs;
        final int numOfCandidates;

        public DbFilter(List<SearchableDatabase> dbs) {
            this(dbs, 5);
        }

        public DbFilter(List<SearchableDatabase> dbFilter, int numOfCandidates) {
            this.dbs = dbFilter;
            this.numOfCandidates = numOfCandidates;
        }

        public int getNumOfCandidates() {
            return numOfCandidates;
        }

        public List<SearchableDatabase> getDbs() {
            return dbs;
        }
    }

    public static class ElementFilter {
        @NotNull
        final FormulaConstraints constraints;
        final boolean matchFormula;
        final boolean matchPrecursorFormula;

        public ElementFilter(@Nullable FormulaConstraints constraints) {
            this(constraints, true, true);
        }

        public ElementFilter(@Nullable String constraints) {
            this(constraints, true, true);
        }

        public ElementFilter(@Nullable String constraints, boolean matchFormula, boolean matchPrecursorFormula) {
            this((constraints != null && !constraints.isBlank())
                            ? FormulaConstraints.fromString(constraints)
                            : FormulaConstraints.empty(),
                    matchFormula, matchPrecursorFormula);
        }

        public ElementFilter(FormulaConstraints constraints, boolean matchFormula, boolean matchPrecursorFormula) {
            this.constraints = constraints == null ? FormulaConstraints.empty() : constraints;
            this.matchFormula = matchFormula;
            this.matchPrecursorFormula = matchPrecursorFormula;
        }

        public boolean isActive() {
            return !constraints.equals(FormulaConstraints.empty()) && (matchFormula || matchPrecursorFormula);
        }

        public FormulaConstraints getConstraints() {
            return constraints;
        }

        public boolean isMatchFormula() {
            return matchFormula;
        }

        public boolean isMatchPrecursorFormula() {
            return matchPrecursorFormula;
        }

        public static final ElementFilter DISABLED = new ElementFilter(FormulaConstraints.empty(), true, true);

        public static ElementFilter disabled() {
            return DISABLED;
        }
    }


    public class QualityFilter {
        private final static List<DataQuality> DEFAULT_STATE = Arrays.asList(DataQuality.values()).subList(1,DataQuality.values().length);

        @Getter
        private final String name;
        private final EnumSet<DataQuality> dataQualities = EnumSet.copyOf(DEFAULT_STATE);

        public QualityFilter() {
            this("quality");
        }

        public QualityFilter(String name) {
            this.name = name;
        }

        public boolean addQuality(int publicIndex) {
            return addQuality(DEFAULT_STATE.get(publicIndex));
        }

        public boolean addQuality(DataQuality quality) {
            if (quality == DataQuality.NOT_APPLICABLE) //no error thrown because this is GUI stuff, just silently ignore
                return false;

            if (dataQualities.add(quality)) {
                pcs.firePropertyChange(name, null, quality);
                return true;
            }
            return false;
        }

        public boolean removeQuality(int publicIndex) {
            return removeQuality(DEFAULT_STATE.get(publicIndex));
        }

        public boolean removeQuality(DataQuality quality) {
            if (quality == DataQuality.NOT_APPLICABLE) //no error thrown because this is GUI stuff, just silently ignore
                return false;

            if (dataQualities.remove(quality)) {
                pcs.firePropertyChange(name, quality, null);
                return true;
            }
            return false;
        }

        public boolean isQualitySelected(int publicIndex) {
            return isQualitySelected(DEFAULT_STATE.get(publicIndex));
        }

        public boolean isQualitySelected(DataQuality quality) {
            return dataQualities.contains(quality);
        }

        public boolean setQualitySelected(int publicIndex, boolean selected){
            if (selected)
                return addQuality(publicIndex);
            return removeQuality(publicIndex);
        }

        public boolean isEnabled() {
            return dataQualities.size() < DEFAULT_STATE.size();
        }

        public void reset(){
            dataQualities.clear();
            dataQualities.addAll(DEFAULT_STATE);
        }

        public List<java.lang.String> getPossibleQualities(){
            return DEFAULT_STATE.stream()
                    .map(dq -> CaseUtils.toCamelCase(dq.name(), true, '_',' ','\t'))
                    .toList();
        }

    }
}
