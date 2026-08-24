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

package de.unijena.bioinf.ms.backgroundruns;

import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fold changes are equal when they describe the same comparison, whatever they measured for it - their
 * abundances are excluded from equality. That makes merging by equality subtle: a recompute produces entries
 * that are equal to the ones they replace, so the wrong container keeps the superseded numbers and reports
 * them as current.
 */
public class FoldChangeMergeTest {

    private static Statistics sampleOverBlank(double foldChange, QuantMeasure quantification) {
        return FoldChange.builder()
                .quantType(QuantRowType.FEATURES)
                .objectId("1")
                .leftGroup("Sample")
                .rightGroup("Blank")
                .quantification(quantification)
                .aggregation(AggregationType.AVG)
                .leftAbundance(foldChange)
                .rightAbundance(1.0)
                .foldChange(foldChange)
                .build();
    }

    private static double onlyFoldChangeOf(List<Statistics> merged) {
        assertEquals(1, merged.size(), "one comparison, one entry: " + merged);
        return ((FoldChange) merged.getFirst()).getFoldChange();
    }

    @Test
    public void testARecomputedFoldChangeReplacesTheOneItRepeats() {
        List<Statistics> merged = FoldChangeWorkflow.mergeStats(
                List.of(sampleOverBlank(2.0, QuantMeasure.APEX_INTENSITY)),
                List.of(sampleOverBlank(4.0, QuantMeasure.APEX_INTENSITY)));

        assertEquals(4.0, onlyFoldChangeOf(merged), Double.MIN_VALUE,
                "the recomputed value must win over the one it supersedes");
    }

    /**
     * The counterpart: a comparison the recompute did not touch is a different slot and has to survive
     * untouched. Replacing everything would be as wrong as replacing nothing.
     */
    @Test
    public void testAFoldChangeForAnotherComparisonIsKept() {
        List<Statistics> merged = FoldChangeWorkflow.mergeStats(
                List.of(sampleOverBlank(2.0, QuantMeasure.AREA_UNDER_CURVE)),
                List.of(sampleOverBlank(4.0, QuantMeasure.APEX_INTENSITY)));

        assertEquals(2, merged.size(), "the untouched comparison is kept next to the new one: " + merged);
    }

    @Test
    public void testAFeatureWithoutFoldChangesTakesTheRecomputedOnes() {
        assertEquals(4.0, onlyFoldChangeOf(
                        FoldChangeWorkflow.mergeStats(null, List.of(sampleOverBlank(4.0, QuantMeasure.APEX_INTENSITY)))),
                Double.MIN_VALUE);
    }
}
