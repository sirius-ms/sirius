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

package de.unijena.bioinf.ms.persistence.model.core;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import lombok.*;
import one.microstream.reference.Lazy;

import java.util.List;

/**
 * Multiple {@link AlignedFeatures} (e.g. Adducts, Isotope peals)that belong to the same Ion (Compound/IonGroup)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compound {

    protected String name;

    protected RetentionTime rt;
    protected double neutralMass;

    /**
     * List of adduct features that belong to the same (this) compound
     */
    Lazy<List<AlignedFeatures>> adductFeatures;
    /**
     * Group of edges between pairs of feature alignments that correlate among each other. Connected components
     * from the whole graph of correlated pairs (whole dataset)
     */
    Lazy<List<CorrelatedIonPair>> correlatedIonPairs;
}
