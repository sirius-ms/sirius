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

import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Display-bound tests for the suggestion dropdown (selection movement with wrap-around, choose
 * callback, hide on empty). Skipped in headless environments; the staged input logic behind the
 * popup is covered display-independently by {@link TokenInputModelTest}.
 */
public class SuggestionPopupTest {

    private JFrame frame;
    private JTextField anchor;
    private SuggestionPopup popup;
    private final AtomicReference<TokenInputModel.Suggestion> chosen = new AtomicReference<>();

    private static List<TokenInputModel.Suggestion> suggestions(String... names) {
        return java.util.Arrays.stream(names)
                .map(n -> (TokenInputModel.Suggestion) new TokenInputModel.Suggestion.FieldSuggestion(
                        new SearchableField().name(n).fieldType(SearchableFieldType.TEXT)))
                .toList();
    }

    @BeforeEach
    public void setup() throws InterruptedException, InvocationTargetException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("SuggestionPopupTest");
            anchor = new JTextField(20);
            frame.add(anchor);
            frame.pack();
            frame.setVisible(true);
            popup = new SuggestionPopup(anchor, chosen::set);
        });
    }

    @AfterEach
    public void teardown() throws InterruptedException, InvocationTargetException {
        if (frame != null)
            SwingUtilities.invokeAndWait(() -> {
                popup.dispose();
                frame.dispose();
            });
    }

    private void onEdt(Runnable r) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(r);
    }

    @Test
    public void testShowPreselectsFirstAndChoosePicksIt() throws Exception {
        onEdt(() -> {
            popup.showSuggestions(suggestions("alpha", "beta", "gamma"));
            assertTrue(popup.isVisible());
            assertEquals("alpha", popup.selected().orElseThrow().display());
            assertTrue(popup.chooseSelected());
        });
        assertEquals("alpha", chosen.get().display());
        onEdt(() -> assertFalse(popup.isVisible(), "choosing hides the popup"));
    }

    @Test
    public void testSelectionMovesAndWraps() throws Exception {
        onEdt(() -> {
            popup.showSuggestions(suggestions("alpha", "beta", "gamma"));
            popup.moveSelection(1);
            assertEquals("beta", popup.selected().orElseThrow().display());
            popup.moveSelection(-2);
            assertEquals("gamma", popup.selected().orElseThrow().display(), "selection must wrap around");
        });
    }

    @Test
    public void testSelectionSurvivesNarrowingWhenStillPresent() throws Exception {
        onEdt(() -> {
            List<TokenInputModel.Suggestion> all = suggestions("alpha", "beta", "gamma");
            popup.showSuggestions(all);
            popup.moveSelection(1); // beta
            popup.showSuggestions(List.of(all.get(1), all.get(2))); // narrowed, beta still there
            assertEquals("beta", popup.selected().orElseThrow().display());
        });
    }

    @Test
    public void testEmptySuggestionsHideAndChooseFallsThrough() throws Exception {
        onEdt(() -> {
            popup.showSuggestions(suggestions("alpha"));
            popup.showSuggestions(List.of());
            assertFalse(popup.isVisible());
            assertFalse(popup.chooseSelected(), "nothing to choose when hidden");
        });
        assertNull(chosen.get());
    }
}
