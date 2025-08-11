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

import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import io.sirius.ms.sdk.model.*;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CompoundFilterMatcher implements Matcher<InstanceBean> {
    final CompoundFilterModel filterModel;
    private final GuiProperties properties;

    public CompoundFilterMatcher(GuiProperties properties, CompoundFilterModel filterModel) {
        this.filterModel = filterModel;
        this.properties = properties;
    }

    @Override
    public boolean matches(InstanceBean item) {
        double mz = item.getIonMass();
        double rt = item.getRT().map(RetentionTime::getRetentionTimeInSeconds).orElse(Double.NaN);
        //todo hotfix, since the confidence score is a FormulaScore which sets all NaN to -Infinity (after computation, and thus also in project space)
        double confidence = item.getConfidenceScore(properties.getConfidenceDisplayMode()).filter(conf -> !Double.isInfinite(conf)).orElse(Double.NaN);

        {
            if (mz < filterModel.getCurrentMinMz())
                return false;
            if (filterModel.isMaxMzFilterActive() && mz > filterModel.getCurrentMaxMz())
                return false;
        }

        if (!Double.isNaN(rt)) { //never filter NaN because RT is just not available
            if (rt < filterModel.getCurrentMinRt())
                return false;
            if (filterModel.isMaxRtFilterActive() && rt > filterModel.getCurrentMaxRt())
                return false;
        }

        if (!Double.isNaN(confidence)) {
            if (filterModel.isMinConfidenceFilterActive() && confidence < filterModel.getCurrentMinConfidence())
                return false;
            if (filterModel.isMaxConfidenceFilterActive() && confidence > filterModel.getCurrentMaxConfidence())
                return false;
        } else if (filterModel.isMinConfidenceFilterActive()) { // filter NaN if min filter is set
            return false;
        }

        if (filterModel.isHasMs1() && Boolean.FALSE.equals(item.getSourceFeature().isHasMs1()))
            return false;

        if (filterModel.isHasMsMs() && Boolean.FALSE.equals(item.getSourceFeature().isHasMsMs()))
            return false;

        if (filterModel.isAdductFilterActive()) {
            Set<PrecursorIonType> itemAdducts = item.getDetectedAdductsOrUnknown();
            if (filterModel.getSelectedAdducts().stream().noneMatch(itemAdducts::contains))
                return false;
        }

        if (item.getSourceFeature().getQuality() != null) //always allow to pass the filter if now quality data is available
            if (filterModel.getFeatureQualityFilter().isEnabled() && !filterModel.getFeatureQualityFilter().isQualitySelected(item.getSourceFeature().getQuality()))
                return false;

        return anyIOIntenseFilterMatches(item, filterModel);
    }

    private boolean anyIOIntenseFilterMatches(InstanceBean item, CompoundFilterModel filterModel) {
        if (filterModel.getBlankSubtraction().isEnabled())
            if (!matchesFoldChangeFilter(item, filterModel)) return false;

        if (filterModel.getIoQualityFilters().stream().anyMatch(CompoundFilterModel.QualityFilter::isEnabled)) {
            AlignedFeatureQualityExperimental qualityReport = item.getQualityReport();
            if (qualityReport != null) { //always allow to pass the filter if now quality data is available
                Map<String, Category> categories = qualityReport.getCategories();
                for (CompoundFilterModel.QualityFilter filter : filterModel.getIoQualityFilters()) {
                    if (filter.isEnabled()) {
                        Category q = categories.get(filter.getName());
                        if (q != null && !filter.isQualitySelected(q.getOverallQuality()))
                            return false;
                    }
                }
            }
        }

        if (filterModel.isElementFilterEnabled())
            if (!matchesElementFilter(item, filterModel)) return false;


        if (filterModel.isLipidFilterEnabled())
            if (!matchesLipidFilter(item, filterModel)) return false;

        if (filterModel.isDbFilterEnabled())
            if (!matchesDBFilter(item, filterModel)) return false;

        return true;
    }

    private boolean matchesFoldChangeFilter(InstanceBean item, CompoundFilterModel filterModel) {
        return filterModel.getBlankSubtraction().matches(item);
    }

    private boolean matchesLipidFilter(InstanceBean item, CompoundFilterModel filterModel) {
        boolean hasAnyLipidHit = item.getFormulaCandidates().stream().anyMatch(FormulaResultBean::isLipid);
        return (filterModel.getLipidFilter() == CompoundFilterModel.LipidFilter.ANY_LIPID_CLASS_DETECTED && hasAnyLipidHit)
                || (filterModel.getLipidFilter() == CompoundFilterModel.LipidFilter.NO_LIPID_CLASS_DETECTED && !hasAnyLipidHit);
    }

    private boolean matchesDBFilter(InstanceBean item, CompoundFilterModel filterModel) {
        final int k;
        List<String> filterDbs;
        if (filterModel.isDbFilterEnabled()) {
            k = filterModel.getDbFilter().getNumOfCandidates();
            filterDbs = filterModel.getDbFilter().getDbs().stream().map(SearchableDatabase::getDatabaseId).toList();
        } else {
            k = 1;
            filterDbs = null;
        }

        if (k == 0)
            return false;

        final PagedModelStructureCandidateFormula candidates = item.getStructureCandidatesPage(k, false);

        if (candidates == null || candidates.getContent() == null || candidates.getContent().isEmpty())
            return false;

        if (filterDbs == null)
            return true;

        return candidates.getContent().stream()
                .map(StructureCandidateFormula::getDbLinks)
                .filter(Objects::nonNull).flatMap(List::stream)
                .map(DBLink::getName).distinct()
                .anyMatch(filterDbs::contains);
    }

    private boolean matchesElementFilter(InstanceBean item, CompoundFilterModel filterModel) {
        CompoundFilterModel.ElementFilter filter = filterModel.getElementFilter();
        @NotNull FormulaConstraints constraints = filter.constraints;
        return item.getFormulaAnnotation().map(fc ->
                (filter.matchFormula && constraints.isSatisfied(fc.getMolecularFormulaObj()))  //check if compound satisfies element constraints
                        || (filter.matchPrecursorFormula && constraints.isSatisfied(fc.getAdductObj().neutralMoleculeToMeasuredNeutralMolecule(fc.getMolecularFormulaObj()))) //check if precursor formula satisfies element constraints
        ).orElse(false);
    }
}
