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

import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.PfasFilter;
import de.unijena.bioinf.ms.gui.utils.filter.PfasFilter.PfasEvidence;
import org.junit.jupiter.api.Test;

import javax.swing.JSlider;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The PFAS evidence slider must carry a selection into the dialog and back out again unchanged - the
 * range slider needs its bounds set in a specific order to do that (see the panel).
 */
public class PfasFilterPanelTest {

    private static PfasFilter filter(PfasEvidence... selected) {
        PfasFilter filter = new FeatureFilterModel().getPfasFilter();
        List<PfasEvidence> keep = Arrays.asList(selected);
        for (PfasEvidence level : PfasEvidence.values())
            filter.setLevelSelected(level, keep.contains(level));
        return filter;
    }

    private static List<PfasEvidence> selectionOf(PfasFilter filter) {
        return Arrays.stream(PfasEvidence.values()).filter(filter::isLevelSelected).toList();
    }

    /** Puts {@code from} on a slider and reads the slider back into a fresh filter. */
    private static List<PfasEvidence> roundTrip(PfasFilter from) {
        PfasFilterPanel panel = new PfasFilterPanel(from, () -> null);
        PfasFilter target = new FeatureFilterModel().getPfasFilter();
        panel.updateModel(target);
        return selectionOf(target);
    }

    @Test
    public void testFullScaleRoundTrips() {
        PfasFilter full = new FeatureFilterModel().getPfasFilter();
        assertEquals(List.of(PfasEvidence.values()), roundTrip(full));
    }

    @Test
    public void testEvidenceOnlyRangeRoundTrips() {
        // [Potential .. Structure]: everything with a pfas tag
        List<PfasEvidence> expected = List.of(PfasEvidence.POTENTIAL, PfasEvidence.MOLECULAR_FORMULA,
                PfasEvidence.MOLECULAR_STRUCTURE);
        assertEquals(expected, roundTrip(filter(expected.toArray(PfasEvidence[]::new))));
    }

    @Test
    public void testLowerRangeRoundTrips() {
        // [None .. Potential]: untagged features and weak evidence only
        List<PfasEvidence> expected = List.of(PfasEvidence.NO_PFAS, PfasEvidence.POTENTIAL);
        assertEquals(expected, roundTrip(filter(expected.toArray(PfasEvidence[]::new))));
    }

    @Test
    public void testSingleLevelRoundTrips() {
        assertEquals(List.of(PfasEvidence.NO_PFAS), roundTrip(filter(PfasEvidence.NO_PFAS)));
        assertEquals(List.of(PfasEvidence.MOLECULAR_STRUCTURE), roundTrip(filter(PfasEvidence.MOLECULAR_STRUCTURE)));
    }

    @Test
    public void testNonContiguousSelectionIsWidenedToTheRangeItSpans() {
        // the slider cannot express a gap, so the ends are kept rather than silently dropped
        assertEquals(List.of(PfasEvidence.values()),
                roundTrip(filter(PfasEvidence.NO_PFAS, PfasEvidence.MOLECULAR_STRUCTURE)));
    }

    @Test
    public void testTooltipUsesTheFieldDescriptionTheIndexReportsAndCoversTheWholeRow() {
        // the description comes from the searchable-field metadata of the API, never from server-side
        // tag definitions - the GUI must keep working against a remote middleware
        PfasFilterPanel panel = new PfasFilterPanel(new FeatureFilterModel().getPfasFilter(),
                () -> "Whatever the index says the pfas tag means");

        String tooltip = panel.getToolTipText();
        assertTrue(tooltip.contains("Whatever the index says the pfas tag means"));
        for (PfasEvidence level : PfasEvidence.values())
            if (level.getTagValue() != null)
                assertTrue(tooltip.contains(level.getTagValue()), "every evidence level names its tag value");

        // hovering the slider must explain the same thing as hovering the row or its name
        JSlider slider = (JSlider) Arrays.stream(panel.getComponents())
                .filter(c -> c instanceof JSlider).findFirst().orElseThrow();
        assertEquals(tooltip, slider.getToolTipText());
    }

    @Test
    public void testTooltipIsComposedOnDemandSoALateFieldDescriptionStillShows() {
        // the fields are fetched in the background after the dialog opened, so the panel must not cache
        String[] description = {null};
        PfasFilterPanel panel = new PfasFilterPanel(new FeatureFilterModel().getPfasFilter(), () -> description[0]);

        assertTrue(panel.getToolTipText().contains("potential PFAS"), "fallback until the index answers");
        description[0] = "Arrived later";
        assertTrue(panel.getToolTipText().contains("Arrived later"));
    }

    @Test
    public void testEveryHoverTargetIsRegisteredWithTheToolTipManager() {
        // see SegmentedFilterToggleTest: setToolTipText does not register a component whose getter
        // already returns a text, so panel and slider must be registered explicitly
        PfasFilterPanel panel = new PfasFilterPanel(new FeatureFilterModel().getPfasFilter(), () -> null);
        assertTrue(registeredForToolTips(panel), "the row panel");
        JSlider slider = (JSlider) Arrays.stream(panel.getComponents())
                .filter(c -> c instanceof JSlider).findFirst().orElseThrow();
        assertTrue(registeredForToolTips(slider), "the slider, which is what the user hovers");
    }

    @Test
    public void testTheTextIsDeliveredForTheEventTheManagerAsksWith() {
        PfasFilterPanel panel = new PfasFilterPanel(new FeatureFilterModel().getPfasFilter(), () -> "from the index");
        JSlider slider = (JSlider) Arrays.stream(panel.getComponents())
                .filter(c -> c instanceof JSlider).findFirst().orElseThrow();
        for (javax.swing.JComponent component : new javax.swing.JComponent[]{panel, slider}) {
            String shown = component.getToolTipText(new java.awt.event.MouseEvent(component,
                    java.awt.event.MouseEvent.MOUSE_MOVED, 0L, 0, 1, 1, 0, false));
            assertNotNull(shown, "ToolTipManager asks with a MouseEvent - that path must yield the text");
            assertTrue(shown.contains("from the index"));
        }
    }

    private static boolean registeredForToolTips(javax.swing.JComponent component) {
        return Arrays.stream(component.getMouseListeners())
                .anyMatch(listener -> listener instanceof javax.swing.ToolTipManager);
    }

    @Test
    public void testResetSelectsTheWholeScaleAgain() {
        PfasFilterPanel panel = new PfasFilterPanel(filter(PfasEvidence.POTENTIAL), () -> null);
        panel.reset();

        PfasFilter target = new FeatureFilterModel().getPfasFilter();
        panel.updateModel(target);
        assertFalse(target.isEnabled(), "the whole scale means no pfas filter");
    }

    @Test
    public void testSetFromModelReplacesTheShownRange() {
        PfasFilterPanel panel = new PfasFilterPanel(new FeatureFilterModel().getPfasFilter(), () -> null);
        panel.setFromModel(filter(PfasEvidence.POTENTIAL, PfasEvidence.MOLECULAR_FORMULA));

        PfasFilter target = new FeatureFilterModel().getPfasFilter();
        panel.updateModel(target);
        assertEquals(List.of(PfasEvidence.POTENTIAL, PfasEvidence.MOLECULAR_FORMULA), selectionOf(target));
    }
}
