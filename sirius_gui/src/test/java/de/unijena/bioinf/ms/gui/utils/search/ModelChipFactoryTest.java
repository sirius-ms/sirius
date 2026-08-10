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

package de.unijena.bioinf.ms.gui.utils.search;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import io.sirius.ms.sdk.model.DataQuality;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the active structured filters of the {@link FeatureFilterModel} render into chips and
 * that removing a chip resets exactly its part of the model. Chips are rendered FROM the model
 * state (never parsed from the compiled lucene query).
 */
public class ModelChipFactoryTest {

    @Test
    public void testInactiveModelHasNoChips() {
        // note: a fresh model starts with hasMsMs=true (default MS/MS quick filter), which IS an
        // active filter and must be visible as a chip
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset(); // default excludes BAD/LOWEST - neutralize for a clean slate
        assertTrue(ModelChipFactory.chipsFor(model).isEmpty());
    }

    @Test
    public void testMzFilterChipAndReset() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset(); // default excludes BAD/LOWEST - neutralize for a clean slate
        model.setCurrentMinMz(300);
        model.setCurrentMaxMz(400);

        List<ModelChip> chips = ModelChipFactory.chipsFor(model);
        assertEquals(1, chips.size());
        assertTrue(chips.get(0).label().contains("m/z"));
        assertTrue(chips.get(0).label().contains("300"));

        chips.get(0).onRemove().run();
        assertFalse(model.isMzFilterActive());
        assertTrue(ModelChipFactory.chipsFor(model).isEmpty());
    }

    @Test
    public void testOpenEndedRangeRendersWithInfinitySymbol() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset(); // default excludes BAD/LOWEST - neutralize for a clean slate
        model.setCurrentMinMz(300); // max stays at the model maximum = open ended

        List<ModelChip> chips = ModelChipFactory.chipsFor(model);
        assertEquals(1, chips.size());
        assertTrue(chips.get(0).label().contains("∞"), "open-ended max must render as infinity: " + chips.get(0).label());
    }

    @Test
    public void testMsDataAndInvertedChips() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.getFeatureQualityFilter().reset(); // default excludes BAD/LOWEST - neutralize for a clean slate
        model.setHasMsMs(true);
        model.setHasMs1(true);
        model.setInverted(true);

        List<ModelChip> chips = ModelChipFactory.chipsFor(model);
        assertEquals(3, chips.size());
        assertTrue(chips.stream().anyMatch(c -> c.label().contains("MS/MS")));
        assertTrue(chips.stream().anyMatch(c -> c.label().contains("MS1")));
        assertTrue(chips.stream().anyMatch(c -> c.label().toLowerCase().contains("invert")));

        chips.forEach(c -> c.onRemove().run());
        assertFalse(model.isHasMsMs());
        assertFalse(model.isHasMs1());
        assertFalse(model.isInverted());
    }

    @Test
    public void testQualityChipListsSelectedQualities() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        // the default feature-quality selection (GOOD+DECENT, i.e. BAD/LOWEST removed) is enabled
        assertTrue(model.getFeatureQualityFilter().isEnabled());

        List<ModelChip> chips = ModelChipFactory.chipsFor(model);
        assertEquals(1, chips.size());
        assertTrue(chips.get(0).label().contains("quality"));

        chips.get(0).onRemove().run();
        assertFalse(model.getFeatureQualityFilter().isEnabled());
    }

    @Test
    public void testAdductElementAndLipidChips() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset(); // default excludes BAD/LOWEST - neutralize for a clean slate
        model.updateAdducts(List.of(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString("[M+Na]+")));
        model.setAdducts(Set.of(PrecursorIonType.fromString("[M+H]+")));
        model.setElementFilter(new ElementFilter("CHNOPS"));
        model.setLipidFilter(FeatureFilterModel.LipidFilter.ANY_LIPID_CLASS_DETECTED);

        List<ModelChip> chips = ModelChipFactory.chipsFor(model);
        assertEquals(3, chips.size());
        assertTrue(chips.stream().anyMatch(c -> c.label().contains("adduct")));
        assertTrue(chips.stream().anyMatch(c -> c.label().contains("elements")));
        assertTrue(chips.stream().anyMatch(c -> c.label().toLowerCase().contains("lipid")));

        chips.forEach(c -> c.onRemove().run());
        assertFalse(model.isAdductFilterActive());
        assertFalse(model.isElementFilterEnabled());
        assertFalse(model.isLipidFilterEnabled());
    }

    @Test
    public void testBlankSubtractionChip() {
        FeatureFilterModel model = new FeatureFilterModel();
        model.setHasMsMs(false);
        model.getFeatureQualityFilter().reset(); // default excludes BAD/LOWEST - neutralize for a clean slate
        model.getSampleBlankFoldChange().setEnabled(true);
        model.getSampleBlankFoldChange().setCurrentMinFoldChange(3.5);

        List<ModelChip> chips = ModelChipFactory.chipsFor(model);
        assertEquals(1, chips.size());
        assertTrue(chips.get(0).label().contains("blank"));
        assertTrue(chips.get(0).label().contains("3.5"));

        chips.get(0).onRemove().run();
        assertFalse(model.getSampleBlankFoldChange().isEnabled());
    }
}
