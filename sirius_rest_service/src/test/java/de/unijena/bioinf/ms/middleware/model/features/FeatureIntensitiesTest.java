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
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectMaximumIntensities;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The intensities the API reports for an aligned feature are the maxima of the RAW per-run values - never the
 * aligned feature's own {@code apexIntensity}, which comes from the merged trace and is normalized per run and
 * summed over runs. The relative intensities put each run feature in proportion to the most intense feature of
 * its own run (or, without a run, of the whole project) and take the best run.
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

    /** A peak-list feature: one run feature without a run, holding the precursor peak height. */
    private static AlignedFeatures fromPeakList(double apexIntensity) {
        return aligned(apexIntensity, Feature.builder().apexIntensity(apexIntensity).build());
    }

    private static ProjectMaximumIntensities maximaOf(AlignedFeatures... features) {
        ProjectMaximumIntensities maxima = new ProjectMaximumIntensities();
        for (AlignedFeatures feature : features)
            maxima.fold(feature.getFeatures().orElse(List.of()));
        return maxima;
    }

    /** Run maxima of the worked example: A 1,000,000 / B 2,000,000 / C 500,000 (areas ten times that). */
    private static ProjectMaximumIntensities maxima() {
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
    public void testNoRunFeaturesIsUnknownWhateverTheAlignedFeatureItselfSays() {
        // every import writes what it measured as a feature of its own, so an aligned feature without one has
        // nothing to report - its own value belongs to a merged trace and is not an intensity in counts
        AlignedFeatures feature = AlignedFeatures.builder().apexIntensity(5_000_000d).build();
        assertEquals(Intensities.UNKNOWN, FeatureIntensities.of(feature, maxima()));

        AlignedFeatures emptyList = aligned(5_000_000d);
        emptyList.setFeatures(List.of());
        assertEquals(Intensities.UNKNOWN, FeatureIntensities.of(emptyList, maxima()));
    }

    @Test
    public void testNoRunFeaturesButAMergedTraceIsUnknown() {
        // An LC-MS feature keeps no run feature when every run's segment was too narrow to pick one (12 of 14312
        // aligned features in a real tomato project). Its own apexIntensity is then the merged trace's apex -
        // normalized per run and summed over runs, so a dimensionless value near 1 rather than an intensity.
        AlignedFeatures feature = AlignedFeatures.builder()
                .apexIntensity(0.204)
                .traceRef(new TraceRef(42L, 0, 1, 2, 3))
                .build();
        assertEquals(Intensities.UNKNOWN, FeatureIntensities.of(feature, maxima()),
                "the merged value must not be reported as the feature's intensity");

        AlignedFeatures emptyList = aligned(0.204);
        emptyList.setTraceRef(new TraceRef(42L, 0, 1, 2, 3));
        emptyList.setFeatures(List.of());
        assertEquals(Intensities.UNKNOWN, FeatureIntensities.of(emptyList, maxima()));
    }

    @Test
    public void testFoldingIntoACopyLeavesTheMaximaItStartedFromAlone() {
        ProjectMaximumIntensities before = maxima();
        ProjectMaximumIntensities after = before.copy();
        assertTrue(after.fold(List.of(runFeature(RUN_A, 9_000_000d, 0d))));

        assertEquals(Set.of(RUN_A), after.runsRaisedSince(before), "only the run the feature was added to");
        assertTrue(after.globalRaisedSince(before), "9,000,000 is also the strongest feature of the project");
        AlignedFeatures atOldMaximum = aligned(null, runFeature(RUN_A, 1_000_000d, 10_000_000d));
        assertEquals(100d, FeatureIntensities.of(atOldMaximum, before).apexRelative(), 1e-9);
        assertEquals(1_000_000d / 9_000_000d * 100d, FeatureIntensities.of(atOldMaximum, after).apexRelative(), 1e-9);
    }

    @Test
    public void testPeakListFeaturesAreHeldAgainstTheProjectMaximum() {
        // a peak list is no measurement of its own: every feature without a run shares one reference, the
        // strongest feature of the project
        AlignedFeatures lau = fromPeakList(135_545_632d);
        AlignedFeatures bicuculline = fromPeakList(74_298_448d);
        AlignedFeatures weak = fromPeakList(13_554_563.2);
        ProjectMaximumIntensities maxima = maximaOf(lau, bicuculline, weak);

        assertEquals(100d, FeatureIntensities.of(lau, maxima).apexRelative(), 1e-9);
        assertEquals(74_298_448d / 135_545_632d * 100d, FeatureIntensities.of(bicuculline, maxima).apexRelative(), 1e-9);
        assertEquals(10d, FeatureIntensities.of(weak, maxima).apexRelative(), 1e-6);
        assertNull(FeatureIntensities.of(lau, maxima).aucRelative(), "a peak list has no area");
    }

    @Test
    public void testRaisedRunsAreThoseWhoseMaximumRose() {
        ProjectMaximumIntensities before = maxima();
        ProjectMaximumIntensities after = before.copy();
        // run C gains a brighter feature, run D is new, A and B are folded below their maximum
        after.fold(List.of(runFeature(RUN_C, 900_000d, 5_000_000d), runFeature(4, 50_000d, 500_000d),
                runFeature(RUN_A, 10d, 10d), runFeature(RUN_B, 1_999_999d, 0d)));

        assertEquals(Set.of(RUN_C, 4L), after.runsRaisedSince(before));
        assertFalse(after.globalRaisedSince(before), "run B's 2,000,000 is still the strongest");
        assertEquals(Set.of(), before.runsRaisedSince(maxima()));
    }
}
