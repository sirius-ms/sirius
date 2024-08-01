/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpectralSearchResults {
    /*
    in case we perform analog search in the future, this threshold determines the exact matches
    we only use exact matches to get the formula and adduct, not an analog with e.g. H2O difference
     */
    private final static double EXACT_SEARCH_MZ_THRESHOLD = 0.2;

    public static Set<MolecularFormula> deriveDistinctFormulaSetWithThreshold(List<SpectralSearchResult.SearchResult> results, double ionMass, double minSimilarity, double minSharedPeaks) {
        return deriveDistinctSetWithThreshold(results, ionMass, minSimilarity, minSharedPeaks, SpectralSearchResult.SearchResult::getMolecularFormula);
    }

    public static Set<PrecursorIonType> deriveDistinctAdductsSetWithThreshold(List<SpectralSearchResult.SearchResult> results, double ionMass, double minSimilarity, double minSharedPeaks) {
        return deriveDistinctSetWithThreshold(results, ionMass, minSimilarity, minSharedPeaks, SpectralSearchResult.SearchResult::getAdduct);
    }

    /**
     * ignores any matches from analog spectral library search (different precursor mass)
     */
    public static <T> Set<T> deriveDistinctSetWithThreshold(List<SpectralSearchResult.SearchResult> results, double ionMass, double minSimilarity, double minSharedPeaks, Function<SpectralSearchResult.SearchResult, T> f) {
        return results.stream()
                .filter(r -> r.getSimilarity().similarity >= minSimilarity && r.getSimilarity().sharedPeaks >= minSharedPeaks)
                .filter(r -> Math.abs(ionMass - r.getExactMass()) < EXACT_SEARCH_MZ_THRESHOLD)
                .map(f).collect(Collectors.toSet());
    }
}
