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

import de.unijena.bioinf.ms.nightsky.sdk.model.SpectralLibraryMatch;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SpectralMatchingResult {

    private List<SpectralLibraryMatch> libraryMatches;
    private final Map<String, List<SpectralLibraryMatch>> resultMap = new HashMap<>();

    private static final int MIN_SHARED_PEAKS = PropertyManager.getInteger("de.unijena.bioinf.sirius.spectralAlignment.minPeaks", 1);

    private static final double MIN_SIMILARITY = PropertyManager.getDouble("de.unijena.bioinf.sirius.spectralAlignment.minScore", 0.2);

    public SpectralMatchingResult(List<SpectralLibraryMatch> libraryMatches) {
        for (SpectralLibraryMatch match : libraryMatches) {
            try {
                resultMap.computeIfAbsent(match.getCandidateInChiKey(), k -> new ArrayList<>()).add(match);
            } catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error("No such database: '" + match.getDbName() + "'");
                break;
            }
        }
        this.libraryMatches = libraryMatches;
    }

    public boolean isFPCandidateInResults(String inchiKey) {
        if (resultMap.containsKey(inchiKey)) {
            return resultMap.get(inchiKey).stream().anyMatch(r -> r.getSharedPeaks() >= MIN_SHARED_PEAKS && r.getSimilarity() >= MIN_SIMILARITY);
        }
        return false;
    }

    public Optional<List<SpectralLibraryMatch>> getMatchingSpectraForFPCandidate(String inchiKey) {
        if (resultMap.containsKey(inchiKey)) {
            return Optional.of(resultMap.get(inchiKey).stream().filter(r -> r.getSharedPeaks() >= MIN_SHARED_PEAKS && r.getSimilarity() >= MIN_SIMILARITY).toList());
        }
        return Optional.empty();
    }

    public Optional<SpectralLibraryMatch> getBestMatchingSpectrumForFPCandidate(String inchiKey) {
        if (resultMap.containsKey(inchiKey)) {
            return resultMap.get(inchiKey).stream().filter(r -> r.getSharedPeaks() >= MIN_SHARED_PEAKS && r.getSimilarity() >= MIN_SIMILARITY).findFirst();
        }
        return Optional.empty();
    }

    public List<SpectralLibraryMatch> getAllResults() {
        return libraryMatches.stream().filter(r -> r.getSharedPeaks() >= MIN_SHARED_PEAKS && r.getSimilarity() >= MIN_SIMILARITY).toList();
    }
}
