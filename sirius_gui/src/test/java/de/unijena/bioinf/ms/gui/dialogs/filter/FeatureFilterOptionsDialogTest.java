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

package de.unijena.bioinf.ms.gui.dialogs.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the search-bar chip -> filter-dialog tab mapping (the pure part of chip->tab navigation).
 * Facet ids come from PanelQueryNodeFactory; unknown ids map to null (dialog keeps its default tab).
 */
public class FeatureFilterOptionsDialogTest {

    @Test
    public void testFacetsMapToTheirOwningTab() {
        assertEquals("Input", FeatureFilterOptionsDialog.tabTitleForFacet("mz"));
        assertEquals("Input", FeatureFilterOptionsDialog.tabTitleForFacet("rt"));
        assertEquals("Input", FeatureFilterOptionsDialog.tabTitleForFacet("adducts"));
        assertEquals("Fold Change", FeatureFilterOptionsDialog.tabTitleForFacet("blank"));
        assertEquals("Data Quality", FeatureFilterOptionsDialog.tabTitleForFacet("hasMs1"));
        assertEquals("Data Quality", FeatureFilterOptionsDialog.tabTitleForFacet("hasMsMs"));
        assertEquals("Data Quality", FeatureFilterOptionsDialog.tabTitleForFacet("quality"));
        assertEquals("Results", FeatureFilterOptionsDialog.tabTitleForFacet("confidence"));
        assertEquals("Results", FeatureFilterOptionsDialog.tabTitleForFacet("elements"));
        assertEquals("Results", FeatureFilterOptionsDialog.tabTitleForFacet("lipid"));
        assertEquals("Results", FeatureFilterOptionsDialog.tabTitleForFacet("db"));
    }

    @Test
    public void testCategorizedQualityFacetMapsToDataQualityViaItsBaseSegment() {
        // categorized quality facet ids look like "quality.<categoryId>"
        assertEquals("Data Quality", FeatureFilterOptionsDialog.tabTitleForFacet("quality.PEAK_QUALITY"));
    }

    @Test
    public void testUnknownOrNullFacetHasNoTab() {
        assertNull(FeatureFilterOptionsDialog.tabTitleForFacet("somethingElse"));
        assertNull(FeatureFilterOptionsDialog.tabTitleForFacet(null));
    }
}
