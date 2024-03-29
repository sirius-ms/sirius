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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import de.unijena.bionf.spectral_alignment.SpectralAlignmentType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SpectralSearchResult implements Iterable<SpectralSearchResult.SearchResult>, ResultAnnotation {

    private Deviation precursorDeviation;

    private Deviation peakDeviation;

    private SpectralAlignmentType alignmentType;

    private List<SearchResult> results;

    @NotNull
    @Override
    public Iterator<SearchResult> iterator() {
        return results.iterator();
    }

    @SuperBuilder
    @Getter
    @Setter
    @Jacksonized
    public static class SearchResult {
        @Builder.Default
        private int rank = -1;

        private SpectralSimilarity similarity;

        private int querySpectrumIndex;

        private String dbName;

        private String dbId;

        private long uuid;

        private String splash;

        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
        private MolecularFormula molecularFormula;

        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(using = SimpleSerializers.PrecursorIonTypeDeserializer.class)
        private PrecursorIonType adduct;
        private double exactMass;
        private String smiles;

        private String candidateInChiKey;
    }

    /*
    in case we perform analog search in the future, this threshold determines the exact matches
    we only use exact matches to get the formula and adduct, not an analog with e.g. H2O difference
     */
    private final static double EXACT_SEARCH_MZ_THRESHOLD = 0.2;

    public Set<MolecularFormula> deriveDistinctFormulaSetWithThreshold(Ms2Experiment exp, double minSimilarity, double minSharedPeaks) {
        return deriveDistinctSetWithThreshold(exp, SearchResult::getMolecularFormula, minSimilarity, minSharedPeaks);
    }

    public Set<PrecursorIonType> deriveDistinctAdductsSetWithThreshold(Ms2Experiment exp, double minSimilarity, double minSharedPeaks) {
        return deriveDistinctSetWithThreshold(exp, SearchResult::getAdduct, minSimilarity, minSharedPeaks);
    }

    /**
     * ignores any matches from analog spectral library search (different precursor mass)
     */
    public <T> Set<T> deriveDistinctSetWithThreshold(Ms2Experiment exp, Function<SearchResult, T> f, double minSimilarity, double minSharedPeaks) {
        return getResults().stream()
                .filter(r -> r.getSimilarity().similarity >= minSimilarity && r.getSimilarity().sharedPeaks >= minSharedPeaks)
                .filter(r -> Math.abs(exp.getIonMass() - r.getExactMass()) < EXACT_SEARCH_MZ_THRESHOLD)
                .map(f).collect(Collectors.toSet());
    }
}
