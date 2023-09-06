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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchSubtoolJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class SpectralSearchResultBean {

    private SpectralSearchResult result;
    private final Map<String, List<SpectralSearchResult.SearchResult>> resultMap = new HashMap<>();

    private static final int MIN_SHARED_PEAKS = PropertyManager.getInteger("de.unijena.bioinf.sirius.spectralAlignment.minPeaks", 3);

    private static final double MIN_SIMILARITY = PropertyManager.getDouble("de.unijena.bioinf.sirius.spectralAlignment.minScore", 0.9);

    public SpectralSearchResultBean(SpectralSearchResult result) {
        for (SpectralSearchResult.SearchResult r : result) {
            try {
                SpectralLibrary db = SearchableDatabases.getCustomDatabase(r.getDbName()).orElseThrow().toSpectralLibraryOrThrow();
                Ms2ReferenceSpectrum reference = db.getReferenceSpectrum(r.getReferenceUUID());
                if (!resultMap.containsKey(reference.getCandidateInChiKey())) {
                    resultMap.put(reference.getCandidateInChiKey(), new ArrayList<>());
                }
                resultMap.get(reference.getCandidateInChiKey()).add(r);
            } catch (ChemicalDatabaseException e) {
                LoggerFactory.getLogger(this.getClass()).error("Error reading spectrum " + r.getReferenceUUID(), e);
            }  catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error("No such database: '" + r.getDbName() + "'");
                break;
            }
        }
        this.result = result;
    }

    public boolean isFPCandidateInResults(String inchiKey) {
        if (resultMap.containsKey(inchiKey)) {
            return resultMap.get(inchiKey).stream().anyMatch(r -> r.getSimilarity().shardPeaks >= MIN_SHARED_PEAKS && r.getSimilarity().similarity >= MIN_SIMILARITY);
        }
        return false;
    }

    public Optional<List<SpectralSearchResult.SearchResult>> getMatchingSpectraForFPCandidate(String inchiKey) {
        if (resultMap.containsKey(inchiKey)) {
            return Optional.of(resultMap.get(inchiKey).stream().filter(r -> r.getSimilarity().shardPeaks >= MIN_SHARED_PEAKS && r.getSimilarity().similarity >= MIN_SIMILARITY).toList());
        }
        return Optional.empty();
    }

    public Optional<SpectralSearchResult.SearchResult> getBestMatchingSpectrumForFPCandidate(String inchiKey) {
        if (resultMap.containsKey(inchiKey)) {
            return resultMap.get(inchiKey).stream().filter(r -> r.getSimilarity().shardPeaks >= MIN_SHARED_PEAKS && r.getSimilarity().similarity >= MIN_SIMILARITY).findFirst();
        }
        return Optional.empty();
    }

    public List<SpectralSearchResult.SearchResult> getAllResults() {
        return result.getResults().stream().filter(r -> r.getSimilarity().shardPeaks >= MIN_SHARED_PEAKS && r.getSimilarity().similarity >= MIN_SIMILARITY).toList();
    }

    public static class MatchBean implements SiriusPCS, Comparable<MatchBean> {

        private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

        private final SpectralSearchResult.SearchResult match;

        private Ms2ReferenceSpectrum reference;

        private String queryName;

        public MatchBean(SpectralSearchResult.SearchResult match) {
            this(match, null);
        }

        public MatchBean(SpectralSearchResult.SearchResult match, InstanceBean instance) {
            this.match = match;
            try {
                SpectralLibrary db = SearchableDatabases.getCustomDatabase(match.getDbName()).orElseThrow().toSpectralLibraryOrThrow();
                this.reference = db.getReferenceSpectrum(match.getReferenceUUID());
                if (instance != null) {
                    MutableMs2Spectrum query = instance.getMs2Spectra().get(match.getQuerySpectrumIndex());
                    this.queryName = SpectraSearchSubtoolJob.getQueryName(query, match.getQuerySpectrumIndex());
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Error retrieving spectral matching data.", e);
            }
        }

        public SpectralSearchResult.SearchResult getMatch() {
            return match;
        }

        public Ms2ReferenceSpectrum getReference() {
            return reference;
        }

        public String getQueryName() {
            return queryName;
        }

        @Override
        public HiddenChangeSupport pcs() {
            return pcs;
        }

        @Override
        public int compareTo(@NotNull SpectralSearchResultBean.MatchBean o) {
            return Integer.compare(match.getRank(), o.getMatch().getRank());
        }
    }

}
