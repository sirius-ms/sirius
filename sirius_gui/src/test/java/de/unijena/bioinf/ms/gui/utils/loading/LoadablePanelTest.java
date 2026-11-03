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

package de.unijena.bioinf.ms.gui.utils.loading;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class LoadablePanelTest {

    /** A content panel that counts how often it is hidden, which is what the card switch does to it. */
    private static final class HideCountingPanel extends JPanel {
        int hidden = 0;

        @Override
        public void setVisible(boolean visible) {
            if (!visible)
                hidden++;
            super.setVisible(visible);
        }
    }

    /** The one child of the panel that is not the content: the loading animation, or the overlay holding it. */
    private static Component loadingComponent(LoadablePanel panel, JComponent content) {
        for (Component child : panel.getComponents())
            if (child != content)
                return child;
        throw new AssertionError("no loading component");
    }

    @Test
    void overlayKeepsTheContentVisibleWhileLoading() {
        HideCountingPanel content = new HideCountingPanel();
        LoadablePanel panel = LoadablePanel.overlaying(content);

        panel.setLoading(true, true);
        assertTrue(content.isVisible(), "the content stays visible under the loading animation");
        assertTrue(loadingComponent(panel, content).isVisible(), "the loading animation is shown");

        panel.setLoading(false, true);
        assertTrue(content.isVisible());
        assertFalse(loadingComponent(panel, content).isVisible(), "the loading animation is gone again");

        assertEquals(0, content.hidden, "a web view that is hidden stops its keyboard hook, which can deadlock on Linux");
    }

    @Test
    void overlayCoversTheWholeContent() {
        JPanel content = new JPanel();
        LoadablePanel panel = LoadablePanel.overlaying(content);
        panel.setSize(400, 300);
        panel.setLoading(true, true);
        panel.doLayout();

        assertEquals(new Rectangle(0, 0, 400, 300), content.getBounds());
        assertEquals(new Rectangle(0, 0, 400, 300), loadingComponent(panel, content).getBounds());
    }

    @Test
    void overlayIsPaintedAboveTheContent() {
        JPanel content = new JPanel();
        LoadablePanel panel = LoadablePanel.overlaying(content);

        // Swing paints the child with the lowest index last, that is on top; overlapping children are only painted
        // correctly when the parent says they may overlap
        assertTrue(panel.getComponentZOrder(loadingComponent(panel, content)) < panel.getComponentZOrder(content));
        assertFalse(panel.isOptimizedDrawingEnabled());
    }

    @Test
    void loadingCountsNestedRequestsInOverlayMode() {
        JPanel content = new JPanel();
        LoadablePanel panel = LoadablePanel.overlaying(content);

        assertTrue(panel.setLoading(true, false));
        assertTrue(panel.setLoading(true, false));
        assertTrue(panel.setLoading(false, false), "one of two requests is still loading");
        assertTrue(loadingComponent(panel, content).isVisible());
        assertFalse(panel.setLoading(false, false));
        assertFalse(loadingComponent(panel, content).isVisible());
    }

    @Test
    void defaultModeStillShowsTheLoadingAnimationInPlaceOfTheContent() {
        HideCountingPanel content = new HideCountingPanel();
        LoadablePanel panel = new LoadablePanel(content);

        panel.setLoading(true, true);
        assertFalse(content.isVisible());

        panel.setLoading(false, true);
        assertTrue(content.isVisible());
    }
}
