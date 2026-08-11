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
import de.unijena.bioinf.ms.gui.utils.query.*;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import io.sirius.ms.sdk.SiriusClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The collapsed state of the feature search bar: a text-field-looking strip in the narrow left rail
 * that renders the committed query as chips (filter-dialog state outlined, user clauses filled,
 * free text as trailing text), clipped to the available width with the full query as tooltip.
 * Clicking, focusing or typing expands the {@link SearchBarOverlay} exactly on top of this field -
 * one perceived control that grows over the result view while editing and shrinks back on close.
 * A keystroke that triggered the expansion is forwarded, so typing "just continues" in the overlay.
 * <p>
 * The chips are rendered from the {@link QueryEditorPanel.Commit} snapshot of the last commit,
 * never by parsing the compiled query; if the shared search document was changed elsewhere (filter
 * dialog fulltext field, reset), the bar falls back to rendering the document text plainly.
 */
public class LuceneSearchBar extends JPanel {

    private final FeatureFilterModel filterModel;
    private final SearchableFieldsProvider fieldsProvider;
    private final Supplier<List<FilterTerm>> termSupplier;
    private final FilterEditorHost editorHost;
    private final SearchRenderState renderState;
    /** Opens the full filter dialog; triggered by the in-field funnel icon (see {@link #filterButton}). */
    private final Runnable openFilterPanel;

    private final JPanel chipStrip;
    /** In-field funnel icon that opens the full filter dialog; tinted to the accent when a filter is active. */
    private final JLabel filterButton;
    private final FunnelIcon funnelIcon = new FunnelIcon(15, Colors.FOREGROUND_DATA);
    /** The full summary cells in order; {@link #relayoutCells} decides how many fit. */
    private final List<JComponent> cells = new ArrayList<>();

    /**
     * Opens the overlay on a press anywhere on the collapsed bar. Shared so it can be attached to
     * the bar, the chip strip and the non-interactive labels alike (mouse events do not bubble to
     * the parent, so every surface that should open needs it).
     */
    private final MouseAdapter opener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            openOverlay();
        }
    };

    @Nullable
    private SearchBarOverlay overlay;
    @Nullable
    private QueryEditorPanel.Commit lastCommit;

    public LuceneSearchBar(@NotNull SiriusClient siriusClient, @NotNull String projectId,
                           @NotNull FeatureFilterModel filterModel,
                           @NotNull Supplier<List<FilterTerm>> termSupplier,
                           @NotNull FilterEditorHost editorHost,
                           @NotNull Runnable openFilterPanel) {
        super(new BorderLayout());
        this.filterModel = filterModel;
        this.termSupplier = termSupplier;
        this.editorHost = editorHost;
        this.openFilterPanel = openFilterPanel;
        this.fieldsProvider = new SearchableFieldsProvider(siriusClient, projectId);
        this.renderState = new SearchRenderState(fieldsProvider);

        // look like an (active) text field, not like a disabled one
        setBorder(UIManager.getBorder("TextField.border"));
        setBackground(UIManager.getColor("TextField.background"));
        setOpaque(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setFocusable(true);

        chipStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        chipStrip.setOpaque(false);
        // center the chip row vertically in the (now taller) bar: a GridBag cell with WEST anchor
        // and horizontal fill keeps the strip at its natural (single-row) height, centered.
        JPanel centerer = new JPanel(new GridBagLayout());
        centerer.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 1;
        centerer.add(chipStrip, gbc);
        add(centerer, BorderLayout.CENTER);

        // in-field funnel icon on the right that opens the full filter dialog (replaces the former
        // separate "..." button next to the bar); it does NOT open the overlay (no `opener` on it)
        filterButton = new JLabel(funnelIcon);
        filterButton.setToolTipText(GuiUtils.formatToolTip("Open the filter panel"));
        filterButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
        filterButton.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.OpenFilterPanelButton);
        filterButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                openFilterPanel.run();
            }
        });
        add(filterButton, BorderLayout.EAST);

        // re-truncate the summary (ellipsis) whenever the available width changes
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutCells();
            }
        });

        // clipped single row, tall enough for a chip to sit in without being cut off (the overlay
        // is where the query wraps); a bare text field is a touch too short for the chips.
        int fieldHeight = new JTextField().getPreferredSize().height;
        int chipHeight = new ChipComponent("Ag", null, ChipComponent.Style.MODEL, null, null).getPreferredSize().height;
        int barHeight = Math.max(fieldHeight, chipHeight) + 6;
        setPreferredSize(new Dimension(100, barHeight));
        setMinimumSize(new Dimension(60, barHeight));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, barHeight));

        // Open only on an explicit gesture - a click or the first typed character. Deliberately NOT
        // on focusGained: the bar receives focus during normal traversal (and on startup), which
        // must not pop the overlay open. The listener is added to the bar AND to chipStrip so a
        // click in the gaps between chips opens too (mouse events do not bubble to the parent); the
        // non-interactive labels get it in refreshSummary.
        addMouseListener(opener);
        chipStrip.addMouseListener(opener);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char typed = e.getKeyChar();
                if (!Character.isISOControl(typed))
                    openOverlay(String.valueOf(typed));
            }
        });

        // Ctrl+F (Cmd+F on macOS) opens the search from anywhere in the main window - the standard
        // "find" shortcut. WHEN_IN_FOCUSED_WINDOW so it fires regardless of which component is
        // focused (as long as the main frame is the active window).
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutMask), "openFeatureSearch");
        getActionMap().put("openFeatureSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openOverlay();
            }
        });

        // keep the summary in sync with commits from anywhere (overlay, dialog, reset)
        filterModel.addUpdateCompleteListener(evt -> refreshSummary());
        refreshSummary();
    }

    public void openOverlay() {
        openOverlay(null);
    }

    /**
     * Expands the query-builder overlay on top of this bar, optionally forwarding the keystroke
     * that triggered the expansion.
     */
    public void openOverlay(@Nullable String typeAhead) {
        if (!isShowing())
            return;
        if (overlay == null) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner == null)
                return;
            overlay = new SearchBarOverlay(owner, filterModel, fieldsProvider, termSupplier, editorHost,
                    renderState, commit -> {
                        lastCommit = commit;
                        refreshSummary();
                    }, this::refreshSummary, openFilterPanel);
        }
        // Reopen unconditionally. If we reach here the collapsed bar received the gesture, which means
        // the overlay is NOT the active window - so a still-"open" overlay is a stale/stuck instance
        // (e.g. a hide that did not take after a filter-dialog round-trip). Force it closed first so
        // openAt always starts from a clean baseline; otherwise the old isOpen() no-op would leave the
        // overlay permanently un-reopenable.
        if (overlay.isOpen())
            overlay.close();
        overlay.openAt(this, typeAhead);
    }

    /**
     * Re-renders the collapsed summary from the committed state. The summary cells are built once
     * here; how many of them fit (and where the ellipsis goes) is decided in {@link #relayoutCells}.
     */
    public void refreshSummary() {
        Runnable open = this::openOverlay;
        cells.clear();

        List<FilterTerm> terms = termSupplier.get();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0)
                cells.add(andLabel());
            cells.add(QueryNodeRenderer.chip(terms.get(i).toQueryNode(), ChipComponent.Style.MODEL, open,
                    renderState.mode(), renderState.suffixLengthResolver()));
        }

        String docText = Optional.ofNullable(filterModel.getSearchText()).orElse("");
        if (lastCommit != null && docText.equals(lastCommit.compiled())) {
            // the document still holds what we compiled - render the real chips
            boolean hasUserPart = !lastCommit.root().items().isEmpty() || !lastCommit.freeText().isEmpty();
            if (!terms.isEmpty() && hasUserPart)
                cells.add(andLabel());
            for (QueryNode node : lastCommit.root().items())
                cells.add(userChip(node, open));
            if (!lastCommit.freeText().isEmpty())
                cells.add(new ChipComponent("“" + lastCommit.freeText() + "”",
                        "Full-text search in the default fields", ChipComponent.Style.USER, open, null));
        } else if (!docText.isEmpty()) {
            // edited elsewhere - show the raw query text
            cells.add(plainLabel(docText));
        }

        if (cells.isEmpty()) {
            JLabel placeholder = plainLabel("Search or add filters...");
            placeholder.setForeground(UIManager.getColor("TextField.inactiveForeground"));
            cells.add(placeholder);
        }

        setToolTipText(docText.isEmpty()
                ? GuiUtils.formatToolTip("Search the feature list - click to open the query builder "
                + "with suggestions for all searchable fields.")
                : GuiUtils.formatToolTip("Current search query:", docText));

        // tint the funnel to the accent while a structured filter is active (orange when inverted),
        // mirroring the old filter button's active/inverted colouring
        funnelIcon.setColor(filterModel.isActive()
                ? (filterModel.isInverted() ? Colors.Menu.FILTER_BUTTON_INVERTED : Colors.Menu.FILTER_BUTTON)
                : Colors.FOREGROUND_DATA);
        filterButton.repaint();

        relayoutCells();
    }

    /**
     * Places as many summary cells into the (single-row) strip as fit the current width, appending
     * a "…" marker when the rest is clipped so it is clear the summary is shortened. Recomputed on
     * every resize.
     */
    private void relayoutCells() {
        chipStrip.removeAll();
        Insets in = getInsets();
        // reserve the funnel button's width so summary chips (and the ellipsis) never run under it
        int buttonSpace = filterButton == null ? 0
                : (filterButton.getWidth() > 0 ? filterButton.getWidth() : filterButton.getPreferredSize().width);
        int available = getWidth() - in.left - in.right - buttonSpace - 6;
        int ellipsisWidth = ellipsisLabel().getPreferredSize().width + 6;

        int used = 0;
        boolean truncated = false;
        for (int i = 0; i < cells.size(); i++) {
            JComponent cell = cells.get(i);
            int cellWidth = cell.getPreferredSize().width + 3; // + FlowLayout hgap
            boolean last = i == cells.size() - 1;
            // once the width is known, stop as soon as a cell (leaving room for "…" unless it is the
            // last one) would overflow - but always keep at least the first cell
            if (available > 0 && chipStrip.getComponentCount() > 0
                    && used + cellWidth + (last ? 0 : ellipsisWidth) > available) {
                truncated = true;
                break;
            }
            chipStrip.add(cell);
            used += cellWidth;
        }
        if (truncated)
            chipStrip.add(ellipsisLabel());

        chipStrip.revalidate();
        chipStrip.repaint();
    }

    private JLabel ellipsisLabel() {
        JLabel label = plainLabel("…");
        label.setToolTipText(GuiUtils.formatToolTip("The summary is shortened - open the search to see the full query."));
        return label;
    }

    private JComponent userChip(QueryNode node, Runnable open) {
        return QueryNodeRenderer.chip(node, ChipComponent.Style.USER, open,
                renderState.mode(), renderState.suffixLengthResolver());
    }

    private JLabel plainLabel(String text) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        label.addMouseListener(opener); // non-interactive text still opens the overlay on click
        return label;
    }

    /**
     * The dimmed "AND" connector for the collapsed summary, made clickable so a press on it opens
     * the overlay like the rest of the bar (a plain label would swallow the click otherwise).
     */
    private JLabel andLabel() {
        JLabel label = ChipComponent.implicitAndLabel();
        label.addMouseListener(opener);
        return label;
    }
}
