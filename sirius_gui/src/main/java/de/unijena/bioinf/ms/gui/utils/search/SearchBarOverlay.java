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
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.query.*;

import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The expanded state of the feature search bar: a floating, undecorated, non-modal heavyweight
 * {@link JDialog} that hosts the reusable {@link QueryEditorPanel} and anchors it on top of the
 * collapsed bar, extending right over the result view. A heavyweight window is required so the
 * editor floats above the native JxBrowser result views (a lightweight layered-pane panel would be
 * hidden behind those).
 * <p>
 * This class owns only the window concerns - anchoring/sizing, showing/disposing, and dismissal (Esc,
 * Cancel, commit, or window focus loss). Everything about building and editing the query lives in the
 * hosted {@link QueryEditorPanel}; the overlay implements {@link QueryEditorPanel.Host} to resize
 * itself when the editor's content changes and to close on the editor's request.
 */
@Slf4j
public class SearchBarOverlay extends JDialog implements QueryEditorPanel.Host {

    private static final int MAX_WIDTH = 900;
    private static final int MIN_WIDTH = 500;
    private static final int MAX_HEIGHT = 460;

    private final QueryEditorPanel editor;

    private int overlayWidth = MIN_WIDTH;
    @org.jetbrains.annotations.Nullable
    private Component anchor;
    /**
     * Whether the overlay has held window focus since it was shown. Gates the focus-loss cancel so
     * the transient focus changes while the window is coming up cannot close it immediately.
     */
    private boolean focusEstablished = false;

    public SearchBarOverlay(@NotNull Window owner,
                            @NotNull FeatureFilterModel filterModel,
                            @NotNull SearchableFieldsProvider fieldsProvider,
                            @NotNull Supplier<List<FilterTerm>> termSupplier,
                            @NotNull FilterEditorHost editorHost,
                            @NotNull SearchRenderState renderState,
                            @NotNull Consumer<QueryEditorPanel.Commit> onCommitted,
                            @NotNull Runnable refreshCollapsedBar,
                            @NotNull Runnable openFilterPanel,
                            @NotNull Runnable clearFilter) {
        super(owner);

        // A heavyweight top-level window so it floats above the native JxBrowser windows of the
        // result views (a lightweight layered-pane panel is hidden behind those). Undecorated so it
        // reads as an inline expansion of the collapsed bar. NON-modal on purpose: a modal dialog
        // would block the rest of the UI, and the outside-click dismissal relies on focus moving
        // away to another window (see the window-focus listener) - which modality would prevent.
        // Only ONE window (the suggestion list is embedded), avoiding the multi-window focus/paint
        // fragility.
        GuiUtils.setUndecorated(this);
        setModalityType(ModalityType.MODELESS);
        setFocusableWindowState(true);

        editor = new QueryEditorPanel(filterModel, fieldsProvider, termSupplier, editorHost, renderState,
                onCommitted, refreshCollapsedBar, this, false, openFilterPanel, clearFilter, null); // restore defaults is dialog-only
        setContentPane(editor);

        // keep anchored while the main window is moved or resized (only while actually open - a
        // WM-driven or programmatic move that does not steal focus would otherwise detach the overlay)
        owner.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible())
                    reposition();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (isVisible())
                    reposition();
            }
        });

        // Cancel when the overlay loses window focus. This is the single dismissal mechanism for
        // clicking outside: any click elsewhere in the app (including the native JxBrowser result
        // view, which produces no AWT mouse event) activates another window and moves focus away.
        // Guards: only after the overlay has actually held focus once (so the transient focus dance
        // while showing cannot close it), and never when focus moves into a window we own (the
        // AND/OR combo popup).
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                focusEstablished = true;
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (!isVisible() || !focusEstablished)
                    return;
                for (Window w = e.getOppositeWindow(); w != null; w = w.getOwner())
                    if (w == SearchBarOverlay.this)
                        return; // focus went to our own owned popup, keep open
                SwingUtilities.invokeLater(SearchBarOverlay.this::close);
            }
        });
    }

    public boolean isOpen() {
        return isVisible();
    }

    // --- QueryEditorPanel.Host ---

    @Override
    public void editorContentChanged() {
        if (isVisible())
            resizeToFit();
    }

    @Override
    public void editorCloseRequested() {
        close();
    }

    // --- opening / closing ---

    /**
     * Opens the overlay anchored at the collapsed bar, extending right over the result view.
     * The optional type-ahead is the keystroke that triggered the expansion from the collapsed bar,
     * applied before showing so typing continues seamlessly.
     */
    public void openAt(@NotNull JComponent anchor, @org.jetbrains.annotations.Nullable String typeAhead) {
        this.anchor = anchor;
        editor.openSession(typeAhead);
        resizeToFit();
        reposition();
        setVisible(true);
        toFront();
        // focus the input and show the dropdown once the window is up
        SwingUtilities.invokeLater(editor::focusInputAndRefresh);
    }

    /**
     * Closes the overlay, discarding any uncommitted edits (the editor reverts to the last applied
     * query). Used by Cancel, Esc, focus loss and the open-filter-dialog chip. A commit updates the
     * baseline first, so closing right after committing keeps the applied query.
     */
    public void close() {
        focusEstablished = false;
        editor.revertToBaseline();
        // Destroy the native peer, don't just hide it: on some (X)Wayland compositors an undecorated
        // heavyweight window's surface can linger after a plain hide - still grabbing mouse input at
        // its old location while painting nothing. That is what makes a "click on the collapsed bar"
        // land on the invisible overlay's model chip and reopen the filter dialog once the overlay
        // has been closed once. dispose() releases the surface (and hides the window); openAt()
        // re-realizes it via setVisible(true). The builder state lives in the editor, so it survives.
        dispose();
    }

    /**
     * Anchors the top-left corner of the overlay at the collapsed bar and sets the width to extend
     * over the result view (clamped). No-op while hidden or before the bar is shown.
     */
    private void reposition() {
        if (anchor == null || !anchor.isShowing())
            return;
        Point origin = anchor.getLocationOnScreen();
        int available = getOwner().getX() + getOwner().getWidth() - origin.x - 12;
        overlayWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, available));
        setLocation(origin.x, origin.y);
        resizeToFit();
    }

    /**
     * Sizes the window to the current content height (clamped) at the anchored width.
     */
    private void resizeToFit() {
        setSize(overlayWidth, Math.max(getHeight(), 1));
        validate();
        int height = Math.min(getPreferredSize().height, MAX_HEIGHT);
        setSize(overlayWidth, height);
        validate();
    }
}
