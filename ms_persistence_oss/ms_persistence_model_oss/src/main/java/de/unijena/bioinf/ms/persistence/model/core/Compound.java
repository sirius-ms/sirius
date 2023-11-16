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

package de.unijena.bioinf.ms.persistence.model.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import it.unimi.dsi.fastutil.longs.LongList;
import jakarta.persistence.Id;
import lombok.*;

import java.util.List;
import java.util.Optional;

/**
 * Multiple {@link AlignedFeatures} (e.g. Adducts, Isotope peals)that belong to the same Ion (Compound/IonGroup)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Compound {
    @Id
    private long compoundId;
    protected RetentionTime rt;
    protected double neutralMass;
    protected String name;

    /**
     * Group of edges between pairs of feature alignments that correlate among each other. Connected components
     * from the whole graph of correlated pairs (whole dataset)
     */
    @ToString.Exclude
    LongList correlatedIonPairIds; //todo denormalize instead?

    //foreign fields
    @JsonIgnore
    @ToString.Exclude
    List<AlignedFeatures> adductFeatures;

    public Optional<List<AlignedFeatures>> getAdductFeatures() {
        return Optional.ofNullable(adductFeatures);
    }

    @JsonIgnore
    @ToString.Exclude
    List<CorrelatedIonPair> correlatedIonPairs;

    public Optional<List<CorrelatedIonPair>> getCorrelatedIonPairs() {
        return Optional.ofNullable(correlatedIonPairs);
    }
    //todo add annotations/identifications
}
