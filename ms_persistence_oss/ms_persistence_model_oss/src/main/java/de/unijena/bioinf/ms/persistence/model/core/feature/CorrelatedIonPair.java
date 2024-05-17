/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

package de.unijena.bioinf.ms.persistence.model.core.feature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Id;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Edge between two {@link AlignedFeatures}s
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CorrelatedIonPair {
    public enum Type {ADDUCT, INSOURCE, MULTIMERE, ISOMERE, UNKNOWN}

    @Id
    private long ionPairId;

    private long compoundId;

    private long alignedFeatureId1;
    private long alignedFeatureId2;

    @JsonIgnore
    @ToString.Exclude
    private AlignedFeatures alignedFeatures1;

    @JsonIgnore
    @ToString.Exclude
    private AlignedFeatures alignedFeatures2;

    public Optional<AlignedFeatures> getAlignedFeatures1() {
        return Optional.ofNullable(alignedFeatures1);
    }

    public Optional<AlignedFeatures> getAlignedFeatures2() {
        return Optional.ofNullable(alignedFeatures2);
    }

    @NotNull
    private Type type;

    //MetaInformation...
    private Double score;
    private Double correlationCoefficient;
    private Double ms2ModifiedCosineSimilarity;
}
