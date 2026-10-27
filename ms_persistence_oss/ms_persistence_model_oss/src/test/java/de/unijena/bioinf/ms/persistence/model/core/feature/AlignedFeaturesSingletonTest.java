/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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

package de.unijena.bioinf.ms.persistence.model.core.feature;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An aligned feature of a single measurement mirrors what that measurement says about mass and retention
 * time. Its apex intensity is not one of those: on an aligned feature that field is the apex of a merged
 * trace, normalized per run and summed over runs, and a measured intensity in counts is a different quantity.
 * It stays on the feature, which is where every reader of an intensity finds it.
 */
public class AlignedFeaturesSingletonTest {

    @Test
    public void testTheAlignedFeatureTakesNoIntensityFromItsFeature() {
        Feature measured = Feature.builder()
                .averageMass(42d)
                .charge((byte) 1)
                .apexIntensity(1_000_000d)
                .build();

        AlignedFeatures aligned = AlignedFeatures.singleton(measured);

        assertNull(aligned.getApexIntensity(), "an intensity in counts is not what that field means here");
        assertEquals(1_000_000d, measured.getApexIntensity(), "the feature keeps it");
    }

    @Test
    public void testTheAlignedFeatureStillMirrorsWhatWasMeasured() {
        Feature measured = Feature.builder()
                .averageMass(42d)
                .apexMass(42.1)
                .charge((byte) 1)
                .apexIntensity(1_000_000d)
                .build();

        AlignedFeatures aligned = AlignedFeatures.singleton(measured);

        assertEquals(42d, aligned.getAverageMass());
        assertEquals(42.1, aligned.getApexMass());
        assertEquals((byte) 1, aligned.getCharge());
        assertEquals(List.of(measured), aligned.getFeatures().orElseThrow());
    }
}
