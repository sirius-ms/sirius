package de.unijena.bioinf.ms.gui.utils;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.lcms.LCMSCompoundSummary;
import de.unijena.bioinf.projectspace.CompoundContainer;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CompoundFilterMatcher implements Matcher<InstanceBean> {
    final CompoundFilterModel filterModel;

    public CompoundFilterMatcher(CompoundFilterModel filterModel) {
        this.filterModel = filterModel;
    }

    @Override
    public boolean matches(InstanceBean item) {
        double mz = item.getIonMass();
        double rt = item.getID().getRt().map(RetentionTime::getRetentionTimeInSeconds).orElse(Double.NaN);
        double confidence = item.getID().getConfidenceScore().orElse(Double.NaN);
        if ((mz < filterModel.getCurrentMinMz()) ||
                (filterModel.isMaxMzFilterActive() && mz > filterModel.getCurrentMaxMz())) {
            return false;
        }
        if (!Double.isNaN(rt)) {
            if ((rt < filterModel.getCurrentMinRt()) ||
                    (filterModel.isMaxRtFilterActive() && rt > filterModel.getCurrentMaxRt())) {
                return false;
            }
        }
        if (!Double.isNaN(confidence)) {
            if ((confidence < filterModel.getCurrentMinConfidence()) ||
                    (filterModel.isMaxConfidenceFilterActive() && confidence > filterModel.getCurrentMaxConfidence())) {
                return false;
            }
        }

        final Set<PrecursorIonType> adducts = filterModel.getAdducts();
        if (!adducts.isEmpty() && !adducts.contains(item.getIonization()))
            return false;

        return anyIOIntenseFilterMatches(item, filterModel);
    }

    private boolean anyIOIntenseFilterMatches(InstanceBean item, CompoundFilterModel filterModel) {
        if (filterModel.isElementFilterEnabled())
            if (!matchesElementFilter(item, filterModel)) return false;

        if (filterModel.isPeakShapeFilterEnabled())
            if (!filterByPeakShape(item, filterModel)) return false;

        if (filterModel.isLipidFilterEnabled())
            if (!matchesLipidFilter(item, filterModel)) return false;

        return true;
    }

    private boolean filterByPeakShape(InstanceBean item, CompoundFilterModel filterModel) {
        final CompoundContainer compoundContainer = item.loadCompoundContainer(LCMSPeakInformation.class);
        final Optional<LCMSPeakInformation> annotation = compoundContainer.getAnnotation(LCMSPeakInformation.class);
        if (annotation.isEmpty()) return false;
        final LCMSPeakInformation lcmsPeakInformation = annotation.get();
        for (int k = 0; k < lcmsPeakInformation.length(); ++k) {
            final Optional<CoelutingTraceSet> tracesFor = lcmsPeakInformation.getTracesFor(k);
            if (tracesFor.isPresent()) {
                final CoelutingTraceSet coelutingTraceSet = tracesFor.get();
                LCMSCompoundSummary.Quality peakQuality = LCMSCompoundSummary.checkPeakQuality(coelutingTraceSet, coelutingTraceSet.getIonTrace());
                if (filterModel.getPeakShapeQuality(peakQuality)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesLipidFilter(InstanceBean item, CompoundFilterModel filterModel) {
        boolean hasAnyLipidHit = item.getResults().stream()
                .map(FormulaResultBean::getFragTree).flatMap(Optional::stream)
                .map(ft -> ft.getAnnotation(LipidSpecies.class)).flatMap(Optional::stream)
                .findAny().isPresent();
        return (filterModel.getLipidFilter() == CompoundFilterModel.LipidFilter.ANY_LIPID_CLASS_DETECTED && hasAnyLipidHit) || (filterModel.getLipidFilter() == CompoundFilterModel.LipidFilter.NO_LIPID_CLASS_DETECTED && !hasAnyLipidHit);
    }

    private boolean matchesElementFilter(InstanceBean item, CompoundFilterModel filterModel) {
        CompoundFilterModel.ElementFilter filter = filterModel.getElementFilter();
        @NotNull FormulaConstraints constraints = filter.constraints;
        boolean r1 = item.loadTopFormulaResult(List.of(TopCSIScore.class)).map(FormulaResult::getId)
                .map(id ->
                        (filter.matchFormula && constraints.isSatisfied(id.getMolecularFormula(), id.getIonType().getIonization()))
                        ||
                        (filter.matchPrecursorFormula && constraints.isSatisfied(id.getPrecursorFormula(), id.getIonType().getIonization()))
                ).orElse(false);

        boolean r2 = item.loadTopFormulaResult(List.of(ZodiacScore.class, SiriusScore.class)).map(FormulaResult::getId)
                .map(id ->
                        (filter.matchFormula && constraints.isSatisfied(id.getMolecularFormula(), id.getIonType().getIonization()))
                                ||
                                (filter.matchPrecursorFormula && constraints.isSatisfied(id.getPrecursorFormula(), id.getIonType().getIonization()))
                ).orElse(false);

        return r1 || r2;
    }
}
