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

import com.google.common.collect.Streams;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.stream.StreamSupport;

public class SpectralSearchResultBean {

    private final List<SearchEntry> results = new ArrayList<>();

    public SpectralSearchResultBean() {}

    public <P extends Peak> void addResults(Ms2Spectrum<P> query, Iterable<Pair<SpectralSimilarity, Ms2SpectralMetadata>> results) {
        this.results.add(new SearchEntry(query, results));
    }

    public List<SearchEntry> getResults() {
        return results;
    }

    public List<SearchResult> getMatchingSpectra(InChI inchi) {
        List<SearchResult> res = new ArrayList<>();
        for (SearchEntry entry : this.results) {
            for (Triple<Integer, SpectralSimilarity, Ms2SpectralMetadata> r : entry.results) {
                if (r.getRight().getCandidateInChiKey().equals(inchi.key)) {
                    if (res.stream().noneMatch(r2 -> r2.hit.getLibraryId().equals(r.getRight().getLibraryId()))) {
                        res.add(new SearchResult(entry.query, r.getLeft(), r.getMiddle(), r.getRight()));
                    }
                    break;
                }
            }
        }
        return res;
    }

    public Optional<SearchResult> getBestMatchingSpectrum(InChI inchi) {
        List<SearchResult> results = getMatchingSpectra(inchi);
        return results.stream().min(Comparator.comparingInt(r -> r.rank));
    }

    private static final class SearchEntry {

        Ms2Spectrum<? extends Peak> query;

        List<Triple<Integer, SpectralSimilarity, Ms2SpectralMetadata>> results;

        public SearchEntry(Ms2Spectrum<? extends Peak> query, Iterable<Pair<SpectralSimilarity, Ms2SpectralMetadata>> results) {
            this.query = query;
            this.results = Streams.mapWithIndex(StreamSupport.stream(results.spliterator(), false), (pair, idx) -> Triple.of((int) idx + 1, pair.getLeft(), pair.getRight())).toList();
        }

    }

    public static final class SearchResult {

        public Ms2Spectrum<? extends Peak> query;

        public int rank;

        public SpectralSimilarity similarity;

        public Ms2SpectralMetadata hit;

        public SearchResult(Ms2Spectrum<? extends Peak> query, int rank, SpectralSimilarity similarity, Ms2SpectralMetadata hit) {
            this.query = query;
            this.rank = rank;
            this.similarity = similarity;
            this.hit = hit;
        }

    }

}
