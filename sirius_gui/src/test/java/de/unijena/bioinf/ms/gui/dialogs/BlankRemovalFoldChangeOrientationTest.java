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

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import io.sirius.ms.sdk.model.AggregationType;
import io.sirius.ms.sdk.model.FoldChangeJobSubmission;
import io.sirius.ms.sdk.model.QuantMeasure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The blank filter reads a fold change the run type dialog wrote, and the two sides address it by nothing but
 * a field name assembled from the job's run groups. They are written independently - the dialog builds a job
 * submission, the filter builds a query string - so nothing but this test makes them agree, and a
 * disagreement is silent: the query names a field that was never written and matches no feature at all.
 */
public class BlankRemovalFoldChangeOrientationTest {

    /**
     * The naming rule of {@code FoldChangeMapper#toIndexableFields}, which lives in the rest service and
     * cannot be reached from here. {@code BlankRemovalIndexFieldNameTest} pins the rule itself against the
     * real mapper, so this literal cannot drift away from it unnoticed.
     */
    private static String indexFieldNameFor(FoldChangeJobSubmission submission,
                                            QuantMeasure quantification, AggregationType aggregation) {
        return "stats.foldChange"
                + "." + submission.getLeftRunGroup()
                + "." + submission.getRightRunGroup()
                + "." + quantification
                + "." + aggregation;
    }

    @Test
    public void testTheSubmittedGroupOrderIsTheOneTheBlankFilterQueries() {
        FoldChangeJobSubmission submission = LCMSRunDialog.blankRemovalJobSubmission();

        assertEquals(FeatureFilterModel.BLANK_REMOVAL_SEARCH_FIELD_NAME,
                indexFieldNameFor(submission, QuantMeasure.APEX_INTENSITY, AggregationType.AVG),
                "the dialog's run groups must be ordered as the blank filter's field name spells them, "
                        + "or the filter queries a field the job never wrote");
    }

    /**
     * The order alone is not enough: the field only exists for the quantification and aggregation the job was
     * actually asked to compute.
     */
    @Test
    public void testTheJobComputesTheQuantificationAndAggregationTheFilterReads() {
        FoldChangeJobSubmission submission = LCMSRunDialog.blankRemovalJobSubmission();

        assertTrue(submission.getQuantificationMeasures().contains(QuantMeasure.APEX_INTENSITY),
                "the blank filter reads APEX_INTENSITY: " + submission.getQuantificationMeasures());
        assertTrue(submission.getAggregationTypes().contains(AggregationType.AVG),
                "the blank filter reads AVG: " + submission.getAggregationTypes());
    }

    /**
     * A fold change is {@code leftAbundance / rightAbundance}, and the filter keeps features at or above a
     * minimum - so the sample group has to be the left one for "at least 2" to mean twice the blank rather
     * than half of it. Pinned separately from the field name: both would be satisfied by swapping the
     * filter's constant instead of the job, which would fix the lookup and invert the meaning.
     */
    @Test
    public void testTheFoldChangeIsSampleOverBlank() {
        FoldChangeJobSubmission submission = LCMSRunDialog.blankRemovalJobSubmission();

        assertEquals(de.unijena.bioinf.ms.persistence.model.core.tags.Groups.SAMPLE_RUNS.getGroupName(),
                submission.getLeftRunGroup(), "the numerator of the fold change");
        assertEquals(de.unijena.bioinf.ms.persistence.model.core.tags.Groups.BLANK_RUNS.getGroupName(),
                submission.getRightRunGroup(), "the denominator of the fold change");
    }
}
