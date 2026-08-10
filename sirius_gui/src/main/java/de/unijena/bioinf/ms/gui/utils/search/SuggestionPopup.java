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

import de.unijena.bioinf.ms.gui.configs.Colors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The suggestion dropdown of the search bar (GitLab-filtered-search style): a non-focusable owned
 * window with a list of {@link TokenInputModel.Suggestion}s below the inline input. The text field
 * keeps the keyboard focus - the owner routes Up/Down/Enter/Tab to {@link #moveSelection} and
 * {@link #chooseSelected}; the mouse selects on hover and chooses on click. The first row is
 * preselected so Enter picks the top match directly.
 */
public class SuggestionPopup {

    private static final int MAX_VISIBLE_ROWS = 12;
    private static final int MIN_WIDTH = 320;

    private final JComponent anchor;
    private final Consumer<TokenInputModel.Suggestion> onChoose;

    private final JList<TokenInputModel.Suggestion> list;
    private final JScrollPane scrollPane;
    @Nullable
    private JWindow window;

    public SuggestionPopup(@NotNull JComponent anchor, @NotNull Consumer<TokenInputModel.Suggestion> onChoose) {
        this.anchor = anchor;
        this.onChoose = onChoose;

        list = new JList<>();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);
        list.setCellRenderer(new SuggestionRenderer());
        list.setVisibleRowCount(MAX_VISIBLE_ROWS);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    list.setSelectedIndex(index);
                    chooseSelected();
                }
            }
        });
        list.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0)
                    list.setSelectedIndex(index);
            }
        });

        scrollPane = new JScrollPane(list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(Colors.Menu.FILTER_BUTTON, 1));
    }

    /**
     * Shows (or updates) the popup below the anchor; an empty list hides it. Keeps the selection on
     * the previously selected suggestion when it is still present, otherwise preselects the first row.
     */
    public void showSuggestions(@NotNull List<TokenInputModel.Suggestion> suggestions) {
        if (suggestions.isEmpty() || !anchor.isShowing()) {
            hide();
            return;
        }
        TokenInputModel.Suggestion previouslySelected = list.getSelectedValue();
        list.setListData(suggestions.toArray(TokenInputModel.Suggestion[]::new));
        int keep = previouslySelected == null ? -1 : suggestions.indexOf(previouslySelected);
        list.setSelectedIndex(Math.max(keep, 0));
        list.ensureIndexIsVisible(list.getSelectedIndex());

        if (window == null) {
            Window owner = SwingUtilities.getWindowAncestor(anchor);
            window = new JWindow(owner);
            window.setFocusableWindowState(false); // the text field keeps the keyboard focus
            window.setType(Window.Type.POPUP);
            window.getContentPane().add(scrollPane);
        }
        relocate();
        if (!window.isVisible())
            window.setVisible(true);
    }

    /**
     * (Re)positions below the anchor - call after the anchor moved or resized while visible.
     */
    public void relocate() {
        if (window == null || !anchor.isShowing())
            return;
        Point anchorOnScreen = anchor.getLocationOnScreen();
        int rows = Math.min(list.getModel().getSize(), MAX_VISIBLE_ROWS);
        list.setVisibleRowCount(rows);
        Dimension pref = scrollPane.getPreferredSize();
        window.setSize(Math.max(MIN_WIDTH, Math.min(pref.width + 4, 640)), pref.height + 4);
        window.setLocation(anchorOnScreen.x, anchorOnScreen.y + anchor.getHeight() + 2);
        window.validate();
    }

    public void hide() {
        if (window != null && window.isVisible())
            window.setVisible(false);
    }

    public boolean isVisible() {
        return window != null && window.isVisible();
    }

    /**
     * Moves the selection by the given delta (wrapping), e.g. +1 for Down, -1 for Up.
     */
    public void moveSelection(int delta) {
        int size = list.getModel().getSize();
        if (size == 0)
            return;
        int index = ((list.getSelectedIndex() + delta) % size + size) % size;
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
    }

    public Optional<TokenInputModel.Suggestion> selected() {
        return Optional.ofNullable(isVisible() ? list.getSelectedValue() : null);
    }

    /**
     * Chooses the selected suggestion (if any): hides the popup and hands it to the owner.
     * Returns false when nothing was chosen, so the caller can fall back to its own handling.
     */
    public boolean chooseSelected() {
        TokenInputModel.Suggestion selected = selected().orElse(null);
        if (selected == null)
            return false;
        hide();
        onChoose.accept(selected);
        return true;
    }

    public void dispose() {
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    /**
     * Row rendering: the display text, with the (dimmed, truncated) description behind it.
     */
    private static class SuggestionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> jList, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            TokenInputModel.Suggestion suggestion = (TokenInputModel.Suggestion) value;
            String description = suggestion.description();
            String text = description == null || description.isBlank()
                    ? escape(suggestion.display())
                    : "<html><b>" + escape(suggestion.display()) + "</b>&nbsp;&nbsp;<span style='color:gray'>"
                    + escape(truncate(description)) + "</span></html>";
            Component component = super.getListCellRendererComponent(jList, text, index, isSelected, cellHasFocus);
            ((JComponent) component).setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            return component;
        }

        private static String truncate(String text) {
            String firstLine = text.split("\n", 2)[0];
            return firstLine.length() > 80 ? firstLine.substring(0, 77) + "..." : firstLine;
        }

        private static String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
