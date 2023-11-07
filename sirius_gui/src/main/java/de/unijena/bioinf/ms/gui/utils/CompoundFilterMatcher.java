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
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.chemdb.ChemDBs;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.lcms.LCMSCompoundSummary;
import de.unijena.bioinf.projectspace.CompoundContainer;
import de.unijena.bioinf.projectspace.FormulaResult;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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
        //todo hotfix, since the confidence score is a FormulaScore which sets all NaN to -Infinity (after computation, and thus also in project space)
        double confidence = item.getID().getConfidenceScore().filter(conf -> !Double.isInfinite(conf)).orElse(Double.NaN);

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

        if (filterModel.isAdductFilterActive() && !filterModel.getAdducts().contains(item.getIonization()))
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

        if (filterModel.isDbFilterEnabled())
            if (!matchesDBFilter(item, filterModel)) return false;

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

    private boolean matchesDBFilter(InstanceBean item, CompoundFilterModel filterModel) {
        final int k;
        final long requestFilter;
        if (filterModel.isDbFilterEnabled()) {
            k = filterModel.getDbFilter().getNumOfCandidates();
            requestFilter = filterModel.getDbFilter().getDbFilterBits();
        } else {
            k = 1;
            requestFilter = 0;
        }

        final List<Scored<CompoundCandidate>> candidates;
        switch (k) {
            case 0 -> {
                return false;
            }
            case 1 -> candidates = item.loadTopFormulaResult(List.of(TopCSIScore.class), FBCandidates.class)
                    .flatMap(i -> i.getAnnotation(FBCandidates.class).map(FBCandidates::getResults))
                    .map(s -> s.stream().limit(k).toList()).orElse(null);
            default -> candidates = item.loadTopKFormulaResults(k, List.of(TopCSIScore.class), FBCandidates.class)
                    .stream().filter(i -> i.getCandidate().hasAnnotation(FBCandidates.class))
                    .flatMap(i -> i.getCandidate().getAnnotation(FBCandidates.class)
                            .map(FBCandidates::getResults).stream().flatMap(Collection::stream)).limit(k).toList();
        }

        if (candidates == null || candidates.isEmpty())
            return false;

        if (requestFilter == 0)
            return true;

        return candidates.stream().map(SScored::getCandidate).anyMatch(c -> ChemDBs.inFilter(c.getBitset(), requestFilter));
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
