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
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.persistence.model.annotation.CompoundAnnotation;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaIdResult;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import lombok.*;
import one.microstream.reference.Lazy;

import java.util.List;
import java.util.stream.Stream;

/**
 * Features aligned over several {@link Run}s (same m/z and RT)
 * Annotations/identifications are calculated and annotated at this level
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlignedFeatures {
    long alignedFeatureId;

    protected Lazy<SimpleSpectrum> isotopePattern;
    protected double mergedIonMass;
    protected RetentionTime mergedRT;

    protected List<Feature> features;

    public Stream<MSMSScan> getMSMSScans(){
        return features.stream().flatMap(f -> f.getMsms().get().stream());
    }

    protected Lazy<List<CorrelatedIonPair>> adductCandidates; // todo or just a list of features and a list of scores?

    Lazy<SpectralSearchResult> librarySearchResults;
    Lazy<List<FormulaIdResult>> formulaResults;


    /**
     * Top annotations for this feature.
     */
    protected CompoundAnnotation topAnnotation;

    /**
     * Manual/User selected Annotations
     */
    protected CompoundAnnotation manualAnnotation;

    public boolean containsBlank() {
        List<Feature> f = getFeatures();
        if (f == null)
            throw new IllegalArgumentException("No features found. Please fetch features before checking for blank run.");
        return f.stream().anyMatch(Feature::isBlank);
    }

    public static AlignedFeatures singleton(Feature feature) {
        return AlignedFeatures.builder()
                .features(List.of(feature))
                .mergedIonMass(feature.ionMass)
                /*.mergedRT(feature.traces)*/.build();
                //todo check rt creatino
    }
}
