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

import de.unijena.bioinf.ms.gui.configs.Colors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The three states of {@link SegmentedFilterToggle}: no filter, must have, must not have - one labelled
 * segment each, exactly one of them selected at any time.
 */
public class SegmentedFilterToggleTest {

    private static SegmentedFilterToggle toggle() {
        return new SegmentedFilterToggle("any", "is a lipid", "not a lipid", () -> "detected by El Gordo");
    }

    @Test
    public void testStartsUnfiltered() {
        SegmentedFilterToggle toggle = toggle();
        assertNull(toggle.getFilterState(), "no filter until the user picks a segment");
        assertTrue(toggle.segmentFor(null).isSelected());
    }

    @Test
    public void testEverySegmentIsOneClickAway() {
        SegmentedFilterToggle toggle = toggle();

        toggle.segmentFor(Boolean.TRUE).doClick();
        assertEquals(Boolean.TRUE, toggle.getFilterState());

        // straight from "yes" to "no" - no cycling through the other states
        toggle.segmentFor(Boolean.FALSE).doClick();
        assertEquals(Boolean.FALSE, toggle.getFilterState());

        toggle.segmentFor(null).doClick();
        assertNull(toggle.getFilterState());
    }

    @Test
    public void testExactlyOneSegmentIsSelected() {
        SegmentedFilterToggle toggle = toggle();
        for (Boolean state : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE}) {
            toggle.setFilterState(state);
            assertTrue(toggle.segmentFor(state).isSelected(), "the segment of the current state is selected");
            for (Boolean other : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE})
                if (other != state)
                    assertFalse(toggle.segmentFor(other).isSelected(), "the other segments are not");
        }
    }

    @Test
    public void testStateCanBeSetProgrammatically() {
        SegmentedFilterToggle toggle = toggle();
        toggle.setFilterState(Boolean.FALSE);
        assertEquals(Boolean.FALSE, toggle.getFilterState());
        toggle.setFilterState(Boolean.TRUE);
        assertEquals(Boolean.TRUE, toggle.getFilterState());
        toggle.setFilterState(null);
        assertNull(toggle.getFilterState());
    }

    @Test
    public void testOnChangeFiresOnceForClicksAndProgrammaticChanges() {
        SegmentedFilterToggle toggle = toggle();
        int[] changes = {0};
        toggle.onChange(() -> changes[0]++);

        toggle.segmentFor(Boolean.TRUE).doClick();
        assertEquals(1, changes[0], "switching segments must not fire twice (deselect + select)");

        toggle.setFilterState(null); // a programmatic reset must refresh the query chips as well
        assertEquals(2, changes[0]);

        toggle.setFilterState(null); // no change, no event
        assertEquals(2, changes[0]);

        toggle.segmentFor(null).doClick(); // clicking the selected segment changes nothing
        assertEquals(2, changes[0]);
    }

    @Test
    public void testNeutralSegmentSitsBetweenTheNegativeAndThePositiveOne() {
        // left to right: must not have it | no filter | must have it - the same direction as the
        // ordinal scales next to it (absent left, present right)
        SegmentedFilterToggle toggle = toggle();
        assertEquals(3, toggle.getComponentCount());
        assertSame(toggle.segmentFor(Boolean.FALSE), toggle.getComponent(0));
        assertSame(toggle.segmentFor(null), toggle.getComponent(1));
        assertSame(toggle.segmentFor(Boolean.TRUE), toggle.getComponent(2));
    }

    @Test
    public void testSelectedSegmentGetsTheFilterAccentForeground() {
        // the panel fills the selected cell with the filter accent, so its label needs the accent's text color
        SegmentedFilterToggle toggle = toggle();
        toggle.setFilterState(Boolean.TRUE);
        assertEquals(Colors.Menu.FILTER_BUTTON_TEXT, toggle.segmentFor(Boolean.TRUE).getForeground());
        assertNotEquals(Colors.Menu.FILTER_BUTTON_TEXT, toggle.segmentFor(null).getForeground());
    }

    @Test
    public void testSegmentsPaintNoBackgroundOfTheirOwnSoTheyLookLikeOneControl() {
        SegmentedFilterToggle toggle = toggle();
        assertFalse(toggle.isOpaque(), "the capsule is painted, not filled by the panel background");
        for (Boolean state : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE}) {
            javax.swing.JToggleButton segment = toggle.segmentFor(state);
            assertFalse(segment.isContentAreaFilled(), "no per-button background");
            assertFalse(segment.isBorderPainted(), "no per-button border, hence no gaps or double edges");
        }
    }

    @Test
    public void testOuterSegmentsAreSymmetricAndSizedFromTheWiderLabel() {
        SegmentedFilterToggle toggle = new SegmentedFilterToggle("any", "is a lipid", "definitely not a lipid", () -> "x");
        int no = toggle.segmentFor(Boolean.FALSE).getPreferredSize().width;
        int yes = toggle.segmentFor(Boolean.TRUE).getPreferredSize().width;
        assertEquals(no, yes, "both outer segments have the same width");

        // ... and that width is the one the longer label needs
        javax.swing.JToggleButton widest = new javax.swing.JToggleButton("definitely not a lipid");
        widest.setMargin(toggle.segmentFor(Boolean.FALSE).getMargin());
        assertEquals(widest.getPreferredSize().width, no);

        for (Boolean state : new Boolean[]{Boolean.FALSE, Boolean.TRUE})
            assertEquals(javax.swing.SwingConstants.CENTER, toggle.segmentFor(state).getHorizontalAlignment(),
                    "the label is centered in its segment");
    }

    @Test
    public void testTheCapsuleIsNotStretchedByTheSurroundingLayout() {
        // a segmented control with empty space inside it looks broken, so it never grows past its content
        SegmentedFilterToggle toggle = toggle();
        assertEquals(toggle.getPreferredSize(), toggle.getMaximumSize());
    }

    @Test
    public void testTheDescriptionIsAskedForOnEveryTooltipSoALateOneStillShows() {
        // the lipid description comes from the API's searchable-field metadata, which arrives in the
        // background after the dialog opened - so it must not be baked in at construction time
        String[] description = {"first"};
        SegmentedFilterToggle toggle = new SegmentedFilterToggle("any", "yes", "no", () -> description[0]);
        assertTrue(toggle.getToolTipText().contains("first"));
        description[0] = "second";
        assertTrue(toggle.getToolTipText().contains("second"));
    }

    @Test
    public void testEveryHoverTargetIsRegisteredWithTheToolTipManager() {
        // JComponent.setToolTipText only registers when the PREVIOUS text was null - and our getter never
        // returns null, so relying on it silently leaves the component unregistered and no tooltip shows
        SegmentedFilterToggle toggle = toggle();
        assertTrue(registeredForToolTips(toggle), "the control itself");
        for (Boolean state : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE})
            assertTrue(registeredForToolTips(toggle.segmentFor(state)), "every segment");
    }

    @Test
    public void testTheTextIsDeliveredForTheEventTheManagerAsksWith() {
        SegmentedFilterToggle toggle = toggle();
        for (Boolean state : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE}) {
            javax.swing.JToggleButton segment = toggle.segmentFor(state);
            String shown = segment.getToolTipText(hover(segment));
            assertNotNull(shown, "ToolTipManager asks with a MouseEvent - that path must yield the text");
            assertTrue(shown.contains("detected by El Gordo"));
        }
    }

    private static java.awt.event.MouseEvent hover(javax.swing.JComponent component) {
        return new java.awt.event.MouseEvent(component, java.awt.event.MouseEvent.MOUSE_MOVED, 0L, 0, 1, 1, 0, false);
    }

    private static boolean registeredForToolTips(javax.swing.JComponent component) {
        return java.util.Arrays.stream(component.getMouseListeners())
                .anyMatch(listener -> listener instanceof javax.swing.ToolTipManager);
    }

    @Test
    public void testEverySegmentCarriesTheDescriptionAsTooltip() {
        SegmentedFilterToggle toggle = toggle();
        for (Boolean state : new Boolean[]{null, Boolean.TRUE, Boolean.FALSE}) {
            String tooltip = toggle.segmentFor(state).getToolTipText();
            assertNotNull(tooltip, "segments need their own tooltip - a parent tooltip does not show on children");
            assertTrue(tooltip.contains("detected by El Gordo"));
        }
    }
}
