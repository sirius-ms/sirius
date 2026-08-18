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

import de.unijena.bioinf.ms.gui.utils.filter.PfasFilter.PfasEvidence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ordinal PFAS evidence filter: an inclusive range over {@code none < potential < formula < structure},
 * selected as a whole scale means "no filter" (like {@link QualityFilter}).
 */
public class PfasFilterTest {

    private static PfasFilter filter() {
        return new FeatureFilterModel().getPfasFilter();
    }

    @Test
    public void testScaleIsOrdinalFromNoPfasToStructure() {
        assertEquals(List.of("None", "Potential", "Formula", "Structure"), filter().getPossibleLevels());
        // the indexed tag values behind the scale; "no PFAS" is the absence of the tag, so it has none
        assertNull(PfasEvidence.NO_PFAS.getTagValue());
        assertEquals("Potential PFAS", PfasEvidence.POTENTIAL.getTagValue());
        assertEquals("PFAS Molecular Formula", PfasEvidence.MOLECULAR_FORMULA.getTagValue());
        assertEquals("PFAS Molecular Structure", PfasEvidence.MOLECULAR_STRUCTURE.getTagValue());
    }

    @Test
    public void testTheFullScaleIsNoFilter() {
        PfasFilter filter = filter();
        assertFalse(filter.isEnabled(), "all levels selected means the filter is disabled");
        for (int i = 0; i < filter.getPossibleLevels().size(); i++)
            assertTrue(filter.isLevelSelected(i));
    }

    @Test
    public void testDroppingAnyLevelEnablesTheFilter() {
        PfasFilter filter = filter();
        assertTrue(filter.setLevelSelected(0, false));
        assertTrue(filter.isEnabled());
        assertFalse(filter.isLevelSelected(0));
        // re-selecting it disables the filter again
        assertTrue(filter.setLevelSelected(0, true));
        assertFalse(filter.isEnabled());
    }

    @Test
    public void testRepeatedSelectionChangeIsANoOp() {
        PfasFilter filter = filter();
        assertFalse(filter.setLevelSelected(1, true), "already selected");
        assertTrue(filter.setLevelSelected(1, false));
        assertFalse(filter.setLevelSelected(1, false), "already unselected");
    }

    @Test
    public void testResetRestoresTheFullScale() {
        PfasFilter filter = filter();
        filter.setLevelSelected(0, false);
        filter.setLevelSelected(3, false);
        filter.reset();
        assertFalse(filter.isEnabled());
    }

    @Test
    public void testSelectedAndExcludedTagValuesSplitAlongTheSelection() {
        PfasFilter filter = filter();
        // keep "none" and "potential": the excluded evidence levels are formula and structure
        filter.setLevelSelected(2, false);
        filter.setLevelSelected(3, false);
        assertTrue(filter.isNoPfasSelected());
        assertEquals(List.of(PfasEvidence.POTENTIAL.getTagValue()), filter.getSelectedTagValues());
        assertEquals(List.of(PfasEvidence.MOLECULAR_FORMULA.getTagValue(), PfasEvidence.MOLECULAR_STRUCTURE.getTagValue()), filter.getExcludedTagValues());
    }

    @Test
    public void testDroppingNoPfasKeepsOnlyTaggedFeatures() {
        PfasFilter filter = filter();
        filter.setLevelSelected(0, false);
        assertFalse(filter.isNoPfasSelected());
        assertEquals(List.of(PfasEvidence.POTENTIAL.getTagValue(), PfasEvidence.MOLECULAR_FORMULA.getTagValue(), PfasEvidence.MOLECULAR_STRUCTURE.getTagValue()),
                filter.getSelectedTagValues());
        assertTrue(filter.getExcludedTagValues().isEmpty());
    }

    @Test
    public void testAnEmptySelectionExcludesEveryTagValue() {
        // not reachable from the slider (it always keeps a non-empty range), but the model allows it
        PfasFilter filter = filter();
        for (int i = 0; i < filter.getPossibleLevels().size(); i++)
            filter.setLevelSelected(i, false);
        assertTrue(filter.isEnabled());
        assertFalse(filter.isNoPfasSelected());
        assertTrue(filter.getSelectedTagValues().isEmpty());
        assertEquals(3, filter.getExcludedTagValues().size());
    }

    @Test
    public void testTheFilterIsPartOfTheModelsActiveStateAndItsReset() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.resetFilter();
        assertFalse(model.isActive());

        model.getPfasFilter().setLevelSelected(PfasEvidence.NO_PFAS, false);
        assertTrue(model.isActive(), "an enabled pfas filter makes the filter model active");

        model.resetFilter();
        assertFalse(model.getPfasFilter().isEnabled(), "resetting the model resets the pfas scale");
        assertFalse(model.isActive());
    }
}
