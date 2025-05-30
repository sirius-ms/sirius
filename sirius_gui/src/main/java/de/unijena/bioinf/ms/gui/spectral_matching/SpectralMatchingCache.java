/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.spectral_matching;

import io.sirius.ms.sdk.model.PagedModelSpectralLibraryMatch;
import io.sirius.ms.sdk.model.SpectralLibraryMatch;
import io.sirius.ms.sdk.model.SpectralLibraryMatchSummary;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SpectralMatchingCache {

    private final InstanceBean instanceBean;

    private final @Nullable String inchiKey;

    private SpectralLibraryMatchSummary summary;

    private Integer currentPage;

    private List<SpectralMatchBean> currentPageContent;

    private List<SpectralMatchBean> currentPageBest;

    private Map<Long, List<SpectralMatchBean>> currentPageGrouped;

    private List<SpectralMatchBean> allMatches;

    private List<SpectralMatchBean> allMatchesBest;

    private Map<Long, List<SpectralMatchBean>> allMatchesGrouped;

    private static final int MIN_SHARED_PEAKS = PropertyManager.getInteger("de.unijena.bioinf.sirius.spectralAlignment.minPeaks", 1);

    private static final double MIN_SIMILARITY = PropertyManager.getDouble("de.unijena.bioinf.sirius.spectralAlignment.minScore", 0.2);

    public static final int PAGE_SIZE = 100;

    private static final Collector<SpectralMatchBean, ?, Map<Long, List<SpectralMatchBean>>> REF_SPEC_GROUPER = Collectors.groupingBy(bean -> bean.getMatch().getUuid());

    public SpectralMatchingCache(InstanceBean instanceBean) {
        this(instanceBean, null);
    }

    public SpectralMatchingCache(InstanceBean instanceBean, @Nullable String inchiKey) {
        this.instanceBean = instanceBean;
        this.inchiKey = inchiKey;
    }

    public synchronized SpectralLibraryMatchSummary getSummary() {
        if (summary == null) {
            summary = instanceBean.withIds((pid, fid) -> instanceBean.getClient().features().getSpectralLibraryMatchesSummaryWithResponseSpec(pid, fid, MIN_SHARED_PEAKS, MIN_SIMILARITY, inchiKey).bodyToMono(SpectralLibraryMatchSummary.class).onErrorComplete().block());
        }
        return summary;
    }

    public synchronized List<SpectralMatchBean> getPage(int page) {
        final int pg = Math.max(page, 0);
        if (currentPage != null && currentPageContent != null && currentPage == page) {
            return currentPageContent;
        }
        if (allMatches != null) {
            // get from all match list
            int from = pg * PAGE_SIZE;
            int to = Math.min((pg + 1) * PAGE_SIZE, allMatches.size());
            currentPageContent = (from < to) ? allMatches.subList(from, to) : List.of();
        } else {
            List<SpectralLibraryMatch> matches = instanceBean
                    .withIds((pid, fid) -> instanceBean.getClient().features().getSpectralLibraryMatchesPagedWithResponseSpec(pid, fid, pg, PAGE_SIZE, List.of("similarity,desc", "sharedPeaks,desc"), MIN_SHARED_PEAKS, MIN_SIMILARITY, inchiKey, List.of()).bodyToMono(PagedModelSpectralLibraryMatch.class).onErrorComplete().block()).getContent();
            currentPageContent = (matches != null) ? matches.stream().map(match -> new SpectralMatchBean(match, instanceBean)).sorted().toList() : List.of();
        }
        currentPage = pg;
        return currentPageContent;
    }

    public synchronized List<SpectralMatchBean> getPageFiltered(int page) {
        final int pg = Math.max(page, 0);
        if (currentPage != null && currentPageBest != null && currentPage == page) {
            return currentPageBest;
        }
        currentPageGrouped = getPage(page).stream().collect(REF_SPEC_GROUPER);
        currentPageBest = currentPageGrouped.values().stream().filter(beans -> !beans.isEmpty()).map(beans -> beans.get(0)).sorted().toList();
        return currentPageBest;
    }

    public synchronized List<SpectralMatchBean> getGroupOnPage(int page, long refSpecUUID) {
        final int pg = Math.max(page, 0);
        if (currentPage != null && currentPageGrouped != null && currentPage == page) {
            return currentPageGrouped.containsKey(refSpecUUID) ? currentPageGrouped.get(refSpecUUID).stream().sorted().toList() : List.of();
        }
        getPageFiltered(page);
        return currentPageGrouped.containsKey(refSpecUUID) ? currentPageGrouped.get(refSpecUUID).stream().sorted().toList() : List.of();
    }

    public synchronized List<SpectralMatchBean> getAll() {
        if (allMatches == null) {
            List<SpectralLibraryMatch> matches = instanceBean
                    .withIds((pid, fid) -> instanceBean.getClient().features().getSpectralLibraryMatchesPagedWithResponseSpec(pid, fid, 0, Integer.MAX_VALUE, List.of("similarity,desc", "sharedPeaks,desc"), MIN_SHARED_PEAKS, MIN_SIMILARITY, inchiKey, List.of()).bodyToMono(PagedModelSpectralLibraryMatch.class).onErrorComplete().block()).getContent();
            allMatches = (matches != null) ? matches.stream().map(match -> new SpectralMatchBean(match, instanceBean)).sorted().toList() : List.of();
        }
        return allMatches;
    }

    public synchronized List<SpectralMatchBean> getAllFiltered() {
        if (allMatchesBest == null) {
            allMatchesGrouped = getAll().stream().collect(REF_SPEC_GROUPER);
            allMatchesBest = allMatchesGrouped.values().stream().filter(beans -> !beans.isEmpty()).map(beans -> beans.get(0)).sorted().toList();
        }
        return allMatchesBest;
    }

    public synchronized List<SpectralMatchBean> getGroup(long refSpecUUID) {
        if (allMatchesGrouped == null) {
            getAllFiltered();
        }
        return allMatchesGrouped.containsKey(refSpecUUID) ? allMatchesGrouped.get(refSpecUUID).stream().sorted().toList() : List.of();
    }

}
