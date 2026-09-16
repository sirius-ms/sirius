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

package de.unijena.bioinf.ms.gui.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The segmented switch between a fixed set of options: one segment per option, exactly one selected, the
 * first by default, all segments the same width.
 */
public class SegmentedChoiceTest {

    private enum Unit {ABSOLUTE, PERCENT}

    private static SegmentedChoice<Unit> choice() {
        return new SegmentedChoice<>(List.of(Unit.values()), unit -> unit == Unit.ABSOLUTE ? "absolute" : "% of run max",
                () -> "the unit of the minimum");
    }

    @Test
    public void testTheFirstOptionIsSelectedByDefault() {
        assertEquals(Unit.ABSOLUTE, choice().getSelected());
    }

    @Test
    public void testClickingASegmentSelectsItsOption() {
        SegmentedChoice<Unit> choice = choice();
        choice.segmentFor(Unit.PERCENT).doClick();
        assertEquals(Unit.PERCENT, choice.getSelected());
        assertFalse(choice.segmentFor(Unit.ABSOLUTE).isSelected(), "exactly one segment is selected");
    }

    @Test
    public void testSettingTheOptionSelectsItsSegment() {
        SegmentedChoice<Unit> choice = choice();
        choice.setSelected(Unit.PERCENT);
        assertTrue(choice.segmentFor(Unit.PERCENT).isSelected());
        assertEquals(Unit.PERCENT, choice.getSelected());
        choice.setSelected(Unit.ABSOLUTE);
        assertEquals(Unit.ABSOLUTE, choice.getSelected());
    }

    @Test
    public void testChangeFiresOncePerChangeButNotForReselecting() {
        SegmentedChoice<Unit> choice = choice();
        int[] changes = {0};
        choice.onChange(() -> changes[0]++);
        choice.setSelected(Unit.PERCENT);
        assertEquals(1, changes[0], "switching deselects one segment and selects another: one change");
        choice.setSelected(Unit.PERCENT);
        assertEquals(1, changes[0], "re-selecting the selected option is no change");
        choice.segmentFor(Unit.ABSOLUTE).doClick();
        assertEquals(2, changes[0]);
    }

    @Test
    public void testAllSegmentsShareTheWidthOfTheWidestLabel() {
        SegmentedChoice<Unit> choice = choice();
        assertEquals(choice.segmentFor(Unit.PERCENT).getPreferredSize().width, choice.segmentFor(Unit.ABSOLUTE).getPreferredSize().width);
        assertEquals(choice.getPreferredSize(), choice.getMaximumSize(), "the capsule hugs its segments");
    }

    @Test
    public void testAlignedChoicesShareOneSegmentWidth() {
        SegmentedChoice<Unit> unit = choice();
        SegmentedChoice<String> measure = new SegmentedChoice<>(List.of("apex", "area"), s -> s, () -> "measure");
        assertTrue(measure.getPreferredSize().width < unit.getPreferredSize().width, "the shorter labels make a narrower switch");

        SegmentedChoice.alignSegmentWidths(unit, measure);
        assertEquals(unit.getPreferredSize().width, measure.getPreferredSize().width, "aligned switches are equally wide");
        assertEquals(unit.segmentFor(Unit.ABSOLUTE).getPreferredSize().width, measure.segmentFor("apex").getPreferredSize().width,
                "and split at the same place");
    }

    @Test
    public void testAnUnknownOptionIsRejected() {
        SegmentedChoice<String> choice = new SegmentedChoice<>(List.of("apex", "area"), s -> s, () -> "measure");
        assertThrows(IllegalArgumentException.class, () -> choice.setSelected("height"));
        assertThrows(IllegalArgumentException.class, () -> new SegmentedChoice<>(List.<String>of(), s -> s, () -> "empty"));
    }
}
