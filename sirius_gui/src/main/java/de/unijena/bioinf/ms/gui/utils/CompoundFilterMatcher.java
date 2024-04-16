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
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.ms.utils.WrapperSpectrum;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.nightsky.sdk.model.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        if (filterModel.isAdductFilterActive() && !filterModel.getAdducts().contains(item.getIonType()))
            return false;

        return anyIOIntenseFilterMatches(item, filterModel);
    }

    private boolean anyIOIntenseFilterMatches(InstanceBean item, CompoundFilterModel filterModel) {
        if (filterModel.isElementFilterEnabled())
            if (!matchesElementFilter(item, filterModel)) return false;

        if (filterModel.isPeakShapeFilterEnabled())
            if (!filterByPeakShape(item, filterModel)) return false;

        if(filterModel.isMinIsotopePeaksFilterEnabled())
            if(!filterByMinIsotopePeaks(item,filterModel)) return false;

        if (filterModel.isLipidFilterEnabled())
            if (!matchesLipidFilter(item, filterModel)) return false;

        if (filterModel.isDbFilterEnabled())
            if (!matchesDBFilter(item, filterModel)) return false;

        return true;
    }

    private boolean filterByMinIsotopePeaks(InstanceBean item, CompoundFilterModel filterModel){
       return Optional.ofNullable(item.getMsData())
               .map(MsData::getMergedMs1)
               .map(ms1 -> WrapperSpectrum.of(ms1.getPeaks(), SimplePeak::getMz, SimplePeak::getIntensity))
               .map(s -> Spectrums.extractIsotopePattern(s,
                       PropertyManager.DEFAULTS.createInstanceWithDefaults(MS1MassDeviation.class),
                       item.getIonMass(), item.getIonType().getCharge(), true))
               .map(s -> s.size() >= filterModel.getCurrentMinIsotopePeaks())
               .orElse(false);
                //todo nightsky: -> add isotope pattern to MS/MS data to make such things easier?.
    }

    private boolean filterByPeakShape(InstanceBean item, CompoundFilterModel filterModel) {
        /*final CompoundContainer compoundContainer = item.loadCompoundContainer(LCMSPeakInformation.class);
        final Optional<LCMSPeakInformation> annotation = compoundContainer.getAnnotation(LCMSPeakInformation.class);
        if (annotation.isEmpty()) return false;
        final LCMSPeakInformation lcmsPeakInformation = annotation.get();
        for (int k = 0; k < lcmsPeakInformation.length(); ++k) {
            final Optional<CoelutingTraceSet> tracesFor = lcmsPeakInformation.getTracesFor(k);
            if (tracesFor.isPresent()) {
                final CoelutingTraceSet coelutingTraceSet = tracesFor.get();
                LCMSQualityCheck.Quality peakQuality = LCMSCompoundSummary.checkPeakQuality(coelutingTraceSet, coelutingTraceSet.getIonTrace());
                if (filterModel.getPeakShapeQuality(peakQuality)) {
                    return true;
                }
            }
        }
        return false;

        */
        LoggerFactory.getLogger(getClass()).warn("Filter by PeakShape not implemented via NIghtSky -> Fileter is always True");
        //todo nightsky: -> implement peak shape filter into api
        return false;
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

        final PageStructureCandidateFormula candidates = item.getStructureCandidatesPage(k, false);

        if (candidates == null || candidates.getContent() == null || candidates.getContent().isEmpty())
            return false;

        if (filterDbs == null)
            return true;

        return candidates.getContent().stream()
                .map(StructureCandidateFormula::getDbLinks)
                .filter(Objects::nonNull).flatMap(List::stream)
                .map(DBLink::getName).distinct()
                .filter(Objects::nonNull)
                .anyMatch(filterDbs::contains);
    }

    private boolean matchesElementFilter(InstanceBean item, CompoundFilterModel filterModel) {
        CompoundFilterModel.ElementFilter filter = filterModel.getElementFilter();
        @NotNull FormulaConstraints constraints = filter.constraints;
        return item.getFormulaAnnotationAsBean().map(fc ->
                (filter.matchFormula && constraints.isSatisfied(fc.getMolecularFormulaObj(), fc.getAdductObj().getIonization()))
                        || (filter.matchPrecursorFormula && constraints.isSatisfied(fc.getAdductObj().neutralMoleculeToMeasuredNeutralMolecule(fc.getMolecularFormulaObj()), fc.getAdductObj().getIonization()))
        ).orElse(false);
    }
}
