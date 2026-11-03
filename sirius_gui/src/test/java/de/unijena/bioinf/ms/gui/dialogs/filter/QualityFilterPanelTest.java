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

import io.sirius.ms.sdk.model.DataQuality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The quality slider must read back exactly the selection it was given - both thumbs, at both ends of the
 * scale. Reading the upper thumb from the wrong field made a wrong range still look plausible; it just
 * quietly widened the filter.
 */
public class QualityFilterPanelTest {

    @BeforeEach
    public void requiresADisplay() {
        // RangeSliderUI renders its thumb in its constructor, which JSlider triggers from its own constructor
        // via updateUI(), so the component cannot even be instantiated without a screen device. These tests
        // therefore only run where there is a display and are skipped on a headless build agent.
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display: RangeSlider cannot be built headless");
    }

    private static Set<DataQuality> selection(DataQuality... selected) {
        return selected.length == 0 ? EnumSet.noneOf(DataQuality.class) : EnumSet.copyOf(List.of(selected));
    }

    /** Round trip: show a selection on a panel and read it back, expecting the same selection. */
    private static Set<DataQuality> roundTrip(DataQuality... selected) {
        QualityFilterPanel panel = new QualityFilterPanel();
        panel.setSelection(selection(selected));
        return panel.getSelection();
    }

    @Test
    public void theTopOfTheScaleRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.DECENT, DataQuality.GOOD),
                roundTrip(DataQuality.DECENT, DataQuality.GOOD));
    }

    @Test
    public void theBottomOfTheScaleRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD),
                roundTrip(DataQuality.LOWEST, DataQuality.BAD));
    }

    @Test
    public void aSingleQualityRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.GOOD), roundTrip(DataQuality.GOOD));
        assertEquals(EnumSet.of(DataQuality.LOWEST), roundTrip(DataQuality.LOWEST));
        assertEquals(EnumSet.of(DataQuality.DECENT), roundTrip(DataQuality.DECENT));
    }

    @Test
    public void theFullScaleRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD),
                roundTrip(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD));
    }

    @Test
    public void setSelectionAppliesLaterChangesToo() {
        // the dialog reuses one panel: it is built once and re-seeded whenever the working value changes
        QualityFilterPanel panel = new QualityFilterPanel();
        panel.setSelection(selection(DataQuality.LOWEST, DataQuality.BAD));
        panel.setSelection(selection(DataQuality.DECENT, DataQuality.GOOD));
        assertEquals(EnumSet.of(DataQuality.DECENT, DataQuality.GOOD), panel.getSelection());
    }

    @Test
    public void resetSelectsTheWholeScale() {
        QualityFilterPanel panel = new QualityFilterPanel();
        panel.setSelection(selection(DataQuality.GOOD));
        panel.reset();
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD), panel.getSelection());
    }

    @Test
    public void aNonContiguousSelectionIsWidenedToItsSpan() {
        // a slider cannot express a gap; widening is the safe direction, dropping the ends is not
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD),
                roundTrip(DataQuality.LOWEST, DataQuality.GOOD));
    }

    @Test
    public void anEmptySelectionFallsBackToTheFullRange() {
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD), roundTrip());
    }
}
