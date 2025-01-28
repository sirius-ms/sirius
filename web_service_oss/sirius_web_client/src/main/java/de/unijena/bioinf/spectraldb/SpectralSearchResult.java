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

package de.unijena.bioinf.spectraldb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SpectralSearchResult implements Iterable<SpectralSearchResult.SearchResult>, ResultAnnotation {

    private Deviation precursorDeviation;

    private Deviation peakDeviation;

    private SpectralMatchingType alignmentType;

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
    public static class SearchResult implements Comparable<SearchResult> {
        @Builder.Default
        private int rank = -1;

        private SpectralSimilarity similarity;

        private int querySpectrumIndex;

        private String dbName;

        private String dbId;

        // match against merged spectrum?
        @JsonProperty(defaultValue = "SPECTRUM")
        private SpectrumType spectrumType;
        private boolean analog; // true if analog query result

        /**
         * This is the uuid of the corresponding reference spectrum
         */
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

        public SearchResult(LibraryHit hit, int rank) {
            this(
                    rank, hit.getSimilarity(), hit.getQueryIndex(), hit.getDbName(), hit.getDbId(), hit.getSpectrumType(), hit.isAnalog(),
                    hit.getUuid(), hit.getSplash(), hit.getMolecularFormula(), hit.getAdduct(), hit.getExactMass(),
                    hit.getSmiles(), hit.getCandidateInChiKey()
            );
        }

        /**
         * This is the inchikey of the corresponding structure candidate
         */
        private String candidateInChiKey;

        @Override
        public int compareTo(@NotNull SpectralSearchResult.SearchResult o) {
            return similarity.compareTo(o.similarity);
        }
    }
}
