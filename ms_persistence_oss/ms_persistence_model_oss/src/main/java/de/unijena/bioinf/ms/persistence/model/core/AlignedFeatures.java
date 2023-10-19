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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.persistence.model.annotation.CompoundAnnotation;
import it.unimi.dsi.fastutil.longs.LongList;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Features aligned over several {@link Run}s (same m/z and RT)
 * Annotations/identifications are calculated and annotated at this level
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE)
public class AlignedFeatures {
    @Id
    private long alignedFeatureId;

    protected Long compoundId;
    /**
     * FeatureId of the feature that provides the isotope pattern
     * TODO should we denormalize instead?
     */
    private long isotopePatternFeatureId;

    protected double mergedIonMass;
    protected RetentionTime mergedRT;
    protected LongList correlationPairIds;

    protected CompoundAnnotation topAnnotation;
    protected CompoundAnnotation manualAnnotation;

    //foreign objects
    //todo do we want do deduplicate msms
    public Optional<List<MSMSScan>> getMsms() {
        if (getFeatures().map(fs -> fs.stream().anyMatch(f -> f.getMsms().isEmpty())).orElse(true))
            return Optional.empty();

        return getFeatures().map(fs -> fs.stream().flatMap(f -> f.getMsms().stream().flatMap(List::stream)))
                .map(Stream::toList);
    }

    @JsonIgnore
    protected List<Feature> features;

    public Optional<List<Feature>> getFeatures() {
        return Optional.ofNullable(features);
    }


    //todo should we denormalize and store a boolean if blank feature is aligned so that we do not have to fetch the features to check?

    public boolean containsBlank() {
        return getFeatures().orElseThrow(() -> new IllegalArgumentException("No features found. Please fetch features before checking for blank run."))
                .stream().anyMatch(Feature::isBlank);
    }

    public SimpleSpectrum getIsotopePattern() {
        return getFeatures().orElseThrow(() -> new IllegalStateException("No features found. Please fetch features before requesting isotope pattern."))
                .stream()
                .filter(f -> f.getFeatureId() == isotopePatternFeatureId)
                .findFirst()
                .map(Feature::getIsotopePattern).orElse(null);
    }

    public static AlignedFeatures singleton(Feature feature) {
        AlignedFeaturesBuilder b = AlignedFeatures.builder()
                .features(List.of(feature))
                .mergedIonMass(feature.ionMass)
                .isotopePatternFeatureId(0);
        {
            //todo check rt creation -> use traces if available?
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            List<Double> times = Stream.concat(
                    feature.getApexScan().stream(),
                    feature.getMsms().stream().flatMap(Collection::stream)
            ).map(AbstractScan::getScanTime).toList();

            for (Double time : times) {
                min = Math.min(min, time);
                max = Math.max(max, time);
            }

            b.mergedRT(Double.compare(min,max) == 0 ? new RetentionTime(min) : new RetentionTime(min, max));
        }

        return b.build();
    }
}
