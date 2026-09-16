/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.features;

import de.unijena.bioinf.ms.middleware.model.features.FeatureIntensities.Intensities;
import de.unijena.bioinf.ms.middleware.model.features.FeatureIntensities.RunMaxima;
import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The intensities the API reports for an aligned feature are the maxima of the RAW per-run values - never the
 * aligned feature's own {@code apexIntensity}, which comes from the merged trace and is normalized per run and
 * summed over runs. The relative intensities put each run feature in proportion to the most intense feature of
 * its own run (or, without runs, its own source file) and take the best run.
 */
public class FeatureIntensitiesTest {

    private static final long RUN_A = 1, RUN_B = 2, RUN_C = 3;

    private static Feature runFeature(long runId, Double apexIntensity, double areaUnderCurve) {
        return Feature.builder().runId(runId).apexIntensity(apexIntensity).areaUnderCurve(areaUnderCurve).build();
    }

    private static AlignedFeatures aligned(Double mergedApexIntensity, Feature... runFeatures) {
        return AlignedFeatures.builder()
                .apexIntensity(mergedApexIntensity)
                .features(Arrays.asList(runFeatures))
                .build();
    }

    /** A peak-list feature: one run feature without a run, the aligned feature knows the file it came from. */
    private static AlignedFeatures fromPeakList(String file, double apexIntensity) {
        AlignedFeatures feature = aligned(apexIntensity, Feature.builder().apexIntensity(apexIntensity).build());
        feature.setDataSource(DataSource.fromPath(file));
        return feature;
    }

    private static RunMaxima maximaOf(AlignedFeatures... features) {
        RunMaxima.Builder builder = RunMaxima.builder();
        for (AlignedFeatures feature : features)
            feature.getFeatures().orElse(List.of()).forEach(f -> builder.add(FeatureIntensities.groupOf(f, feature), f));
        return builder.build();
    }

    /** Run maxima of the worked example: A 1,000,000 / B 2,000,000 / C 500,000 (areas ten times that). */
    private static RunMaxima maxima() {
        return maximaOf(
                aligned(null, runFeature(RUN_A, 1_000_000d, 10_000_000d)),
                aligned(null, runFeature(RUN_B, 2_000_000d, 20_000_000d)),
                aligned(null, runFeature(RUN_C, 500_000d, 5_000_000d)));
    }

    @Test
    public void testRawMaximaOverRunFeaturesNotTheMergedValue() {
        // 0.4 + 0.4 + 0.4 of the runs' normalization constants is what the merged trace stores
        AlignedFeatures feature = aligned(1.2,
                runFeature(RUN_A, 400_000d, 4_000_000d), runFeature(RUN_B, 800_000d, 8_000_000d), runFeature(RUN_C, 200_000d, 2_000_000d));
        Intensities intensities = FeatureIntensities.of(feature, maxima());
        assertEquals(800_000d, intensities.apex());
        assertEquals(8_000_000d, intensities.auc());
    }

    @Test
    public void testRelativeIntensityIsPerRunAndTakesTheBestRun() {
        // 40 % of A, 40 % of B, 40 % of C: the feature is at 40 % of its run maximum everywhere
        AlignedFeatures even = aligned(1.2,
                runFeature(RUN_A, 400_000d, 4_000_000d), runFeature(RUN_B, 800_000d, 8_000_000d), runFeature(RUN_C, 200_000d, 2_000_000d));
        assertEquals(40d, FeatureIntensities.of(even, maxima()).apexRelative(), 1e-9);
        assertEquals(40d, FeatureIntensities.of(even, maxima()).aucRelative(), 1e-9);

        // weak everywhere in counts, but 60 % of run C's maximum: the best run decides
        AlignedFeatures strongInC = aligned(0.9, runFeature(RUN_A, 100_000d, 1_000_000d), runFeature(RUN_C, 300_000d, 3_000_000d));
        Intensities intensities = FeatureIntensities.of(strongInC, maxima());
        assertEquals(300_000d, intensities.apex());
        assertEquals(60d, intensities.apexRelative(), 1e-9);

        // the run's most intense feature is at 100 %
        assertEquals(100d, FeatureIntensities.of(aligned(1.5, runFeature(RUN_B, 2_000_000d, 20_000_000d)), maxima()).apexRelative(), 1e-9);
    }

    @Test
    public void testRunFeaturesWithoutAValueAreSkipped() {
        AlignedFeatures feature = aligned(1.5, runFeature(RUN_A, null, 0d), runFeature(RUN_B, 2_000_000d, 0d));
        Intensities intensities = FeatureIntensities.of(feature, maxima());
        assertEquals(2_000_000d, intensities.apex());
        assertNull(intensities.auc(), "an area of zero means no area was measured");
        assertNull(intensities.aucRelative());
    }

    @Test
    public void testRunFeaturesPresentButNoneWithAValueIsUnknownNotTheMergedValue() {
        AlignedFeatures feature = aligned(1.8, runFeature(RUN_A, null, 0d), runFeature(RUN_B, null, 0d));
        assertEquals(Intensities.UNKNOWN, FeatureIntensities.of(feature, maxima()));
    }

    @Test
    public void testNoRunFeaturesFallsBackToTheAlignedFeaturesOwnApexWithoutARelativeValue() {
        // an aligned feature imported over the API has no run features; its own value is the MS1 precursor peak
        // intensity written at import, so it is in raw counts too - but there is no group to relate it to
        AlignedFeatures feature = AlignedFeatures.builder().apexIntensity(5_000_000d).build();
        assertEquals(new Intensities(5_000_000d, null, null, null), FeatureIntensities.of(feature, maxima()));

        AlignedFeatures emptyList = aligned(5_000_000d);
        emptyList.setFeatures(List.of());
        assertEquals(5_000_000d, FeatureIntensities.of(emptyList, maxima()).apex());
    }

    @Test
    public void testPeakListFeaturesAreGroupedByTheirSourceFile() {
        // two files, each one measurement: the brightest feature of each file is that file's 100 %
        AlignedFeatures lau = fromPeakList("laudanosine.mgf", 135_545_632d);
        AlignedFeatures bicuculline = fromPeakList("bicuculline.ms", 74_298_448d);
        AlignedFeatures weakInSameFile = fromPeakList("bicuculline.ms", 7_429_845d);
        RunMaxima maxima = maximaOf(lau, bicuculline, weakInSameFile);

        assertEquals(100d, FeatureIntensities.of(lau, maxima).apexRelative(), 1e-9);
        assertEquals(100d, FeatureIntensities.of(bicuculline, maxima).apexRelative(), 1e-9,
                "the file's strongest feature, however weak compared with another file");
        assertEquals(10d, FeatureIntensities.of(weakInSameFile, maxima).apexRelative(), 1e-6);
    }

    @Test
    public void testChangedGroupsAreThoseWhoseMaximumMoved() {
        RunMaxima before = maxima();
        // run B's strongest feature is gone, run C gained a brighter one, A is unchanged, D is new
        RunMaxima after = maximaOf(
                aligned(null, runFeature(RUN_A, 1_000_000d, 10_000_000d)),
                aligned(null, runFeature(RUN_B, 800_000d, 8_000_000d)),
                aligned(null, runFeature(RUN_C, 900_000d, 5_000_000d)),
                aligned(null, runFeature(4, 50_000d, 500_000d)));
        assertEquals(Set.of(FeatureIntensities.runGroup(RUN_B), FeatureIntensities.runGroup(RUN_C), FeatureIntensities.runGroup(4)),
                after.changedGroups(before));
        assertEquals(Set.of(), before.changedGroups(maxima()));
    }
}
