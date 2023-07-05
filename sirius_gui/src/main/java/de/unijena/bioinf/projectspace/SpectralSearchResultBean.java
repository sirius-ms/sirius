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

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.spectraldb.SpectralLibrary;

import java.util.*;

public class SpectralSearchResultBean {

    private final List<SpectralLibrary.SearchResult> allResults = new ArrayList<>();
    private final Map<String, List<SpectralLibrary.SearchResult>> resultMap = new HashMap<>();
    private final int rankLimit;

    public SpectralSearchResultBean() {
        this(25);
        // TODO is limit rank <= 25 okay? -> set the rank limit parameter somewhere!
    }

    public SpectralSearchResultBean(int rankLimit) {
        this.rankLimit = rankLimit;
    }

    public <P extends Peak> void setResults(Iterable<SpectralLibrary.SearchResult> results) {
        this.resultMap.clear();
        this.allResults.clear();
        results.forEach(res -> {
            if (res.getRank() <= rankLimit) {
                if (!resultMap.containsKey(res.getReference().getCandidateInChiKey())) {
                    resultMap.put(res.getReference().getCandidateInChiKey(), new ArrayList<>());
                }
                resultMap.get(res.getReference().getCandidateInChiKey()).add(res);
            }
            allResults.add(res);
        });
    }

    public boolean isFPCandidateInResults(String inchiKey) {
        return this.resultMap.containsKey(inchiKey);
    }

    public Optional<List<SpectralLibrary.SearchResult>> getMatchingSpectraForFPCandidate(String inchiKey) {
        if (this.resultMap.containsKey(inchiKey)) {
            return Optional.of(this.resultMap.get(inchiKey));
        } else {
            return Optional.empty();
        }
    }

    public Optional<SpectralLibrary.SearchResult> getBestMatchingSpectrumForFPCandidate(String inchiKey) {
        if (this.resultMap.containsKey(inchiKey)) {
            return Optional.of(this.resultMap.get(inchiKey).get(0));
        } else {
            return Optional.empty();
        }
    }

    public List<SpectralLibrary.SearchResult> getAllResults() {
        return allResults;
    }

}
