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
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import io.sirius.ms.sdk.SiriusClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * The chips are rendered from the {@link SearchBarOverlay.Commit} snapshot of the last commit,
 * never by parsing the compiled query; if the shared search document was changed elsewhere (filter
 * dialog fulltext field, reset), the bar falls back to rendering the document text plainly.
 */
public class LuceneSearchBar extends JPanel {

    private final FeatureFilterModel filterModel;
    private final SearchableFieldsProvider fieldsProvider;
    private final Supplier<List<ModelChip>> modelChipSupplier;
    private final Runnable openFilterDialog;
    private final Supplier<ConfidenceDisplayMode> confidenceMode;

    private final JPanel chipStrip;

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
    private SearchBarOverlay.Commit lastCommit;

    public LuceneSearchBar(@NotNull SiriusClient siriusClient, @NotNull String projectId,
                           @NotNull FeatureFilterModel filterModel,
                           @NotNull Supplier<List<ModelChip>> modelChipSupplier,
                           @NotNull Runnable openFilterDialog,
                           @NotNull Supplier<ConfidenceDisplayMode> confidenceMode) {
        super(new BorderLayout());
        this.filterModel = filterModel;
        this.modelChipSupplier = modelChipSupplier;
        this.openFilterDialog = openFilterDialog;
        this.confidenceMode = confidenceMode;
        this.fieldsProvider = new SearchableFieldsProvider(siriusClient, projectId);

        // look like an (active) text field, not like a disabled one
        setBorder(UIManager.getBorder("TextField.border"));
        setBackground(UIManager.getColor("TextField.background"));
        setOpaque(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setFocusable(true);

        chipStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        chipStrip.setOpaque(false);
        add(chipStrip, BorderLayout.CENTER);

        // clipped single row at text-field height; the overlay is the place that wraps
        int fieldHeight = new JTextField().getPreferredSize().height;
        setPreferredSize(new Dimension(100, fieldHeight));
        setMinimumSize(new Dimension(60, fieldHeight));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, fieldHeight));

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
            overlay = new SearchBarOverlay(owner, filterModel, fieldsProvider, modelChipSupplier,
                    openFilterDialog, commit -> {
                        lastCommit = commit;
                        refreshSummary();
                    }, confidenceMode);
        }
        if (!overlay.isOpen())
            overlay.openAt(this, typeAhead);
    }

    /**
     * Re-renders the collapsed summary from the committed state.
     */
    public void refreshSummary() {
        chipStrip.removeAll();
        Runnable open = this::openOverlay;

        List<ModelChip> modelChips = modelChipSupplier.get();
        for (int i = 0; i < modelChips.size(); i++) {
            if (i > 0)
                chipStrip.add(andLabel());
            chipStrip.add(new ChipComponent(modelChips.get(i).label(), modelChips.get(i).tooltip(),
                    ChipComponent.Style.MODEL, open, null));
        }

        String docText = Optional.ofNullable(filterModel.getSearchText()).orElse("");
        if (lastCommit != null && docText.equals(lastCommit.compiled())) {
            // the document still holds what we compiled - render the real chips
            boolean hasUserPart = !lastCommit.root().items().isEmpty() || !lastCommit.freeText().isEmpty();
            if (!modelChips.isEmpty() && hasUserPart)
                chipStrip.add(andLabel());
            for (QueryNode node : lastCommit.root().items())
                chipStrip.add(userChip(node, open));
            if (!lastCommit.freeText().isEmpty())
                chipStrip.add(new ChipComponent("“" + lastCommit.freeText() + "”",
                        "Full-text search in the default fields", ChipComponent.Style.USER, open, null));
        } else if (!docText.isEmpty()) {
            // edited elsewhere - show the raw query text
            chipStrip.add(plainLabel(docText));
        }

        if (chipStrip.getComponentCount() == 0) {
            JLabel placeholder = plainLabel("Search or add filters...");
            placeholder.setForeground(UIManager.getColor("TextField.inactiveForeground"));
            chipStrip.add(placeholder);
        }

        setToolTipText(docText.isEmpty()
                ? GuiUtils.formatToolTip("Search the feature list - click to open the query builder "
                + "with suggestions for all searchable fields.")
                : GuiUtils.formatToolTip("Current search query:", docText));
        chipStrip.revalidate();
        chipStrip.repaint();
    }

    private JComponent userChip(QueryNode node, Runnable open) {
        if (node instanceof QueryClause clause && clause.isFreeText())
            return new ChipComponent((clause.negated() ? "NOT " : "") + "“" + clause.value1() + "”",
                    "Full-text search in the default fields", ChipComponent.Style.USER, open, null);
        String text = node instanceof QueryClause clause
                ? (clause.negated() ? "NOT " : "") + clause.field() + " " + SearchBarOverlay.clauseBody(clause)
                : LuceneQueryCompiler.render(node); // groups collapse to their compiled form
        return new ChipComponent(text, LuceneQueryCompiler.render(node), ChipComponent.Style.USER, open, null);
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
