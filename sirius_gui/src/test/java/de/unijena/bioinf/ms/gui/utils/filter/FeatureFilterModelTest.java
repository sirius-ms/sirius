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

package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FeatureFilterModel#isActive()} - specifically that every filter facet which
 * contributes a clause to {@link FeatureFilterModel#toLuceneQuery} also counts as "active", so the
 * compiled query is actually run.
 */
public class FeatureFilterModelTest {

    /**
     * Neutralizes the default-active filters (MS/MS present, feature quality excludes BAD/LOWEST)
     * so the model starts from a truly inactive slate.
     */
    private static FeatureFilterModel cleanSlate() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset();
        return model;
    }

    @Test
    public void testCleanSlateIsInactive() {
        assertFalse(cleanSlate().isActive());
    }

    @Test
    public void testBlankFoldChangeAloneIsActive() {
        FeatureFilterModel model = cleanSlate();
        model.getSampleBlankFoldChange().setEnabled(true);

        // the blank fold-change filter contributes a range clause to the lucene query, so a model
        // with only this filter enabled must be considered active
        assertTrue(model.isActive(), "blank fold-change filter alone must mark the model active");

        // ... and the compiled query must therefore be present (isActive() gates toLuceneQuery)
        Optional<String> query = model.toLuceneQuery(ConfidenceDisplayMode.EXACT);
        assertTrue(query.isPresent(), "an active blank fold-change filter must produce a lucene query");
    }

    @Test
    public void testIsSupportedAdduct() {
        assertTrue(FeatureFilterModel.isSupportedAdduct(PrecursorIonType.fromString("[M + H]+")),
                "single-charged monomeric adduct is supported");
        // multiply-charged adducts are not constructible yet (MultipleChargeException), so only the
        // multimeric case can be exercised here; isSupportedAdduct still guards both conditions.
        assertFalse(FeatureFilterModel.isSupportedAdduct(PrecursorIonType.fromString("[2M + H]+")),
                "multimeric adduct is not supported");
    }
}
