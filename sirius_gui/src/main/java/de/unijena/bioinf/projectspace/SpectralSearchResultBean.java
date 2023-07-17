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

import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.ms.frontend.subtools.spectra_db.SpectralDatabases;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class SpectralSearchResultBean {

    private SpectralSearchResult result;
    private final Map<String, List<SpectralSearchResult.SearchResult>> resultMap = new HashMap<>();

    public SpectralSearchResultBean(SpectralSearchResult result) {
        for (SpectralSearchResult.SearchResult r : result) {
            try {
                SpectralLibrary db = SpectralDatabases.getSpectralLibrary(Path.of(r.getDbLocation())).orElseThrow();
                Ms2ReferenceSpectrum reference = db.getReferenceSpectrum(r.getReferenceId());
                if (!resultMap.containsKey(reference.getCandidateInChiKey())) {
                    resultMap.put(reference.getCandidateInChiKey(), new ArrayList<>());
                }
                resultMap.get(reference.getCandidateInChiKey()).add(r);
            } catch (ChemicalDatabaseException e) {
                LoggerFactory.getLogger(this.getClass()).error("Error reading spectrum " + r.getReferenceId(), e);
            }  catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error("No such database: '" + r.getDbLocation() + "'");
                break;
            }
        }
        this.result = result;
    }

    public boolean isFPCandidateInResults(String inchiKey, int minSharedPeaks, double minSimilarity) {
        if (resultMap.containsKey(inchiKey)) {
            return resultMap.get(inchiKey).stream().anyMatch(r -> r.getSimilarity().shardPeaks >= minSharedPeaks && r.getSimilarity().similarity >= minSimilarity);
        }
        return false;
    }

    public Optional<List<SpectralSearchResult.SearchResult>> getMatchingSpectraForFPCandidate(String inchiKey, int minSharedPeaks, double minSimilarity) {
        if (resultMap.containsKey(inchiKey)) {
            return Optional.of(resultMap.get(inchiKey).stream().filter(r -> r.getSimilarity().shardPeaks >= minSharedPeaks && r.getSimilarity().similarity >= minSimilarity).toList());
        }
        return Optional.empty();
    }

    public Optional<SpectralSearchResult.SearchResult> getBestMatchingSpectrumForFPCandidate(String inchiKey, int minSharedPeaks, double minSimilarity) {
        if (resultMap.containsKey(inchiKey)) {
            return resultMap.get(inchiKey).stream().filter(r -> r.getSimilarity().shardPeaks >= minSharedPeaks && r.getSimilarity().similarity >= minSimilarity).findFirst();
        }
        return Optional.empty();
    }

    public SpectralSearchResult getAllResults() {
        return result;
    }

}
