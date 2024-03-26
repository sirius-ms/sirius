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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import de.unijena.bionf.spectral_alignment.SpectralAlignmentType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        @Builder.Default
        private int rank = -1;

        private SpectralSimilarity similarity;

        private int querySpectrumIndex;

        private String dbName;

        private String dbId;

        private long uuid;

        private String splash;

        private MolecularFormula molecularFormula;
        private PrecursorIonType adduct;
        private double exactMass;
        private String smiles;

        private String candidateInChiKey;

        public PrecursorIonType derivePrecursorAdductOrNull(Ms2Experiment exp) {
            //to be future proof, make sure, if it is not a hit from analog search
            if (Math.abs(exp.getIonMass() - getExactMass())<0.2) return getAdduct();
            else return null;
        }

        public MolecularFormula deriveNeutralFormulaOrNull(Ms2Experiment exp) {
            //to be future proof, make sure, if it is not a hit from analog search
            if(Math.abs(exp.getIonMass() - getExactMass())<0.2) return getMolecularFormula();
            else return null;
        }
    }

    public Set<MolecularFormula> deriveDistinctFormulaSetWithThreshold(Ms2Experiment exp, double minSimilarity, double minSharedPeaks) {
        return deriveDistinctSetWithThreshold(exp, (e, r) -> r.deriveNeutralFormulaOrNull(e), minSimilarity, minSharedPeaks);
    }

    public Set<PrecursorIonType> deriveDistinctAdductsSetWithThreshold(Ms2Experiment exp, double minSimilarity, double minSharedPeaks) {
        return deriveDistinctSetWithThreshold(exp, (e, r) -> r.derivePrecursorAdductOrNull(e), minSimilarity, minSharedPeaks);
    }

    public <T> Set<T> deriveDistinctSetWithThreshold(Ms2Experiment exp, BiFunction<Ms2Experiment, SearchResult, T> f, double minSimilarity, double minSharedPeaks) {
        return getResults().stream()
                .filter(r -> r.getSimilarity().similarity >= minSimilarity && r.getSimilarity().sharedPeaks >= minSharedPeaks)
                .map(r -> f.apply(exp, r))
                .filter(Objects::nonNull)
                .distinct().collect(Collectors.toSet());
    }
}
