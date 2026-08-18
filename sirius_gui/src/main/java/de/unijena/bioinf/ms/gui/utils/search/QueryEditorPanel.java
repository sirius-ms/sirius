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

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.CompactToggleIcon;
import de.unijena.bioinf.ms.gui.configs.FilterButton;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.configs.InvertIcon;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The reusable, host-agnostic inline lucene query builder, modeled after GitLab's filtered search.
 * Two stacked zones: an upper CHIPS zone holds the finished terms (filter-dialog state as outlined
 * chips, the user's committed clause chips, groups as nested paren chips), wrapping across rows in a
 * height-capped scrollable area; a lower TYPING zone holds only the term currently being built - open
 * -group markers, the staged token fragments and the text input - so the stage prompt stays readable
 * however many chips fill the zone above. An embedded suggestion list below offers all candidates for
 * the current stage ({@link TokenInputModel}) and narrows while typing.
 * <p>
 * This is a plain {@link JPanel} with no window/lifecycle knowledge: it delegates the two
 * host-specific concerns - "my content changed height, re-fit me" and "I want to be dismissed" - to
 * a {@link Host}. {@link SearchBarOverlay} hosts it in a floating dialog; the feature filter dialog
 * embeds it directly.
 * <p>
 * Up/Down navigate the suggestion list; Enter and Tab both ADD the highlighted suggestion (or the
 * typed value) to the query. An untouched input highlights the run-action row ("Search") on top
 * instead, so Enter on an empty input runs the search - typing a term and pressing Enter twice is the
 * quick full-text search. Tab never runs the search (it skips that row); Backspace on empty input
 * pops a stage.
 * <p>
 * The builder state compiles into the {@link FeatureFilterModel}'s shared search text document on
 * commit - the model itself needs no change and the filter dialog's fulltext field shows the
 * compiled query automatically.
 */
@Slf4j
public class QueryEditorPanel extends JPanel {

    private static final int ICON_SIZE = 24; // px; sized to sit on the input row next to the field
    private static final int MAX_LIST_ROWS = 10;
    /**
     * The finished-chips zone shows at most this many rows, then scrolls (keeps the typing row visible).
     */
    private static final int MAX_CHIP_ROWS = 3;
    /**
     * Vertical gap between chip rows; shared by the chips-zone {@link WrapLayout} and the height cap.
     */
    private static final int CHIP_VGAP = 4;

    /**
     * What a commit produced - the collapsed bar renders its chips from this snapshot (never by
     * parsing the compiled string).
     */
    public record Commit(@NotNull QueryContainer root, @NotNull String freeText, @NotNull String compiled) {
    }

    /**
     * The window/lifecycle concerns the editor delegates to whatever hosts it.
     */
    public interface Host {
        /**
         * The editor's content changed height; re-fit the surrounding container (e.g. resize the overlay).
         */
        void editorContentChanged();

        /**
         * The editor asked to be dismissed (Esc / Cancel / after commit).
         */
        void editorCloseRequested();

        /**
         * Enter was pressed with nothing left to add, i.e. the query should be run/applied. The
         * floating overlay handles that itself (it runs the search), so this is only overridden by
         * the embedded host (the filter dialog), for which it means Apply.
         */
        default void editorCommitRequested() {
        }

        /**
         * A model chip was clicked to open its full editor. The floating overlay steps aside (closes)
         * and then opens it; a host that embeds the editor next to that full editor (the filter
         * dialog) just runs it - e.g. selecting the owning tab - without dismissing itself.
         */
        default void editorHandoff(@NotNull Runnable openFullEditor) {
            editorCloseRequested();
            SwingUtilities.invokeLater(openFullEditor);
        }
    }

    private final FeatureFilterModel filterModel;
    private final SearchableFieldsProvider fieldsProvider;
    private final Supplier<List<FilterTerm>> termSupplier;
    private final FilterEditorHost editorHost;
    private final SearchRenderState renderState;
    private final Runnable refreshCollapsedBar;
    private final Consumer<Commit> onCommitted;
    private final Host host;
    /**
     * Embedded (in the filter dialog) vs. floating (the overlay). Embedded drops its own commit
     * controls, Esc binding and model-update listener - the dialog owns commit/dismissal - and
     * renders model chips as read-only navigation targets (their value is edited via the widgets).
     */
    private final boolean embedded;
    /**
     * Opens the full filter dialog from the overlay's in-field funnel icon; null when there is no
     * such affordance (the embedded host is already the dialog).
     */
    @Nullable
    private final Runnable openFilterPanel;
    /**
     * Full reset of every filter and the search query (the overlay's Clear button). When null the
     * Clear button only clears the user's own query (the embedded dialog keeps its own Reset).
     */
    @Nullable
    private final Runnable clearFilter;
    /**
     * Restores the default filter configuration (SIRIUS's recommended starting filter). When null the
     * restore button is hidden (e.g. the embedded dialog manages its own defaults).
     */
    @Nullable
    private final Runnable restoreDefaults;
    /**
     * The overlay's funnel button, kept so its tint can track the active filter state; null when embedded.
     */
    @Nullable
    private FilterButton filterButton;
    /**
     * The field-name display toggle's glyph, kept so it can flip between compact/expanded on toggle.
     */
    private CompactToggleIcon modeIcon;
    /** The invert toggle's glyph, kept so it can be re-tinted when the inversion state flips. */
    private InvertIcon invertIcon;
    private JButton invertToggle;

    // --- builder state ---
    private QueryContainer root = QueryContainer.empty();
    private int[] openPath = new int[0];
    private final TokenInputModel tokenModel = new TokenInputModel();
    /** Whether the whole query is inverted ({@code *:* AND NOT (...)}); a model-level flag surfaced and
     *  edited here (see the invert toggle), applied to the model at the host's commit point. */
    private boolean inverted;
    /**
     * The applied query the working state reverts to on Cancel: captured when the session opens and
     * whenever a search is committed. Uncommitted edits made while open are discarded back to this.
     */
    private QueryContainer baselineRoot = QueryContainer.empty();
    private String baselineFreeText = "";
    private boolean baselineInverted;
    /**
     * The query string this editor last wrote into the shared search document. If the document
     * differs on open, it was edited elsewhere (filter dialog) - the builder then degrades the
     * document content into its free-text segment instead of trying to parse it back into chips.
     */
    private String lastCompiled = "";
    /**
     * Model-filter chip removals staged in the editor but not yet applied. Like the user's own
     * clauses, they take effect only on commit (Search) and are discarded on Cancel - so the
     * {@link FeatureFilterModel} is left untouched until the query is actually run. Keyed by chip
     * label so {@link #rebuild} can hide a staged-removed chip without mutating the model.
     */
    private final LinkedHashMap<String, Runnable> pendingModelRemovals = new LinkedHashMap<>();

    // --- ui ---
    /**
     * Upper zone: the finished terms as chips (wraps across rows, height-capped + scrollable).
     */
    private final JPanel chipsRow;
    /**
     * The scroll pane wrapping {@link #chipsRow}; kept so rebuilds can scroll it to the newest chips.
     */
    private JScrollPane chipsScroll;
    /**
     * Lower zone, left part: the open-group markers and staged fragments of the term being built.
     * The input itself takes the rest of that line (see {@link #typingLine}).
     */
    private final JPanel typingRow;
    /** Right end of the typing line: the close-group control (while a group is open) and the key hint. */
    private final JPanel typingTrailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
    /**
     * The whole typing line: staged fragments (west), the input (center, grows to the full remaining
     * width) and the trailing controls (east).
     */
    private JPanel typingLine;
    private final PlaceholderTextField input;
    /**
     * Trailing hint on the typing row telling what the accept/run keys do RIGHT NOW. Only shown while
     * something is typed - on an empty input the highlighted run-action row says it instead.
     */
    private final JLabel keyHint = new JLabel();
    private final JList<TokenInputModel.Suggestion> suggestionList;
    private final JScrollPane suggestionScroll;
    /**
     * Where the pointer sat when the session opened. Hover-selection is ignored while it is still
     * there, so an editor popping up UNDER the cursor (Ctrl+F with the mouse over the result view)
     * does not silently move the selection off the default row. Cleared by the first real move.
     */
    @Nullable
    private Point hoverAnchor;

    public QueryEditorPanel(@NotNull FeatureFilterModel filterModel,
                            @NotNull SearchableFieldsProvider fieldsProvider,
                            @NotNull Supplier<List<FilterTerm>> termSupplier,
                            @NotNull FilterEditorHost editorHost,
                            @NotNull SearchRenderState renderState,
                            @NotNull Consumer<Commit> onCommitted,
                            @NotNull Runnable refreshCollapsedBar,
                            @NotNull Host host,
                            boolean embedded,
                            @Nullable Runnable openFilterPanel,
                            @Nullable Runnable clearFilter,
                            @Nullable Runnable restoreDefaults) {
        super(new BorderLayout());
        this.filterModel = filterModel;
        this.fieldsProvider = fieldsProvider;
        this.termSupplier = termSupplier;
        this.editorHost = editorHost;
        this.renderState = renderState;
        this.onCommitted = onCommitted;
        this.refreshCollapsedBar = refreshCollapsedBar;
        this.host = host;
        this.embedded = embedded;
        this.openFilterPanel = openFilterPanel;
        this.clearFilter = clearFilter;
        this.restoreDefaults = restoreDefaults;

        // the top row of an untouched input: Enter runs the search (overlay) / applies the dialog
        tokenModel.setRunAction(new TokenInputModel.RunAction("Search",
                "Apply the filter and close (Enter)",
                "Type to add query terms"));

        setBackground(UIManager.getColor("TextField.background"));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.Menu.FILTER_BUTTON, 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)));

        // --- two zones: finished terms as chips (upper) over the term being built (lower) ---
        // the chips zone wraps across rows in a height-capped, vertically-scrollable area so the
        // typing row and its instructions stay visible no matter how many finished chips there are
        chipsRow = new WrapScrollPanel(new WrapLayout(FlowLayout.LEFT, 4, CHIP_VGAP));
        chipsScroll = new JScrollPane(chipsRow,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(d.width, Math.min(d.height, chipsZoneMaxHeight()));
            }
        };
        chipsScroll.setOpaque(false);
        chipsScroll.getViewport().setOpaque(false);
        chipsScroll.setBorder(BorderFactory.createEmptyBorder());

        input = new PlaceholderTextField(18);
        input.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        input.setOpaque(false);
        wireInput();

        typingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2)); // the term being built, on its own line
        typingRow.setOpaque(false);

        keyHint.setForeground(UIManager.getColor("TextField.inactiveForeground"));
        keyHint.setFont(keyHint.getFont().deriveFont(Font.PLAIN, keyHint.getFont().getSize2D() - 1f));
        keyHint.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
        typingTrailing.setOpaque(false);
        // the staged fragments on the left and the trailing controls on the right take their natural
        // width; everything in between belongs to the input, so there is room to type a long term
        typingLine = new JPanel(new BorderLayout());
        typingLine.setOpaque(false);
        typingLine.add(typingRow, BorderLayout.WEST);
        typingLine.add(input, BorderLayout.CENTER);
        typingLine.add(typingTrailing, BorderLayout.EAST);

        Box controls = Box.createHorizontalBox();


        // borderless in-field buttons on the left of the controls (same affordances as the collapsed
        // bar): Clear does a full reset of all filters + the query; the funnel opens the full dialog

        JButton clear = Buttons.getBackspaceButton(ICON_SIZE, "Clear all filters and the search query", true);
        clear.setFocusable(false); // must not grab focus (e.g. would auto-close the search overlay)
        clear.setMargin(new Insets(0, 0, 0, 0));
        clear.addActionListener(e -> {
            clearAll();
            input.requestFocusInWindow();
        });

        // restore the default filter configuration (distinct from Clear, which removes all filters);
        // only shown when the host provides the action (the embedded filter dialog; the search overlay
        // passes null, so no restore button there)
        JButton restore = null;
        if (restoreDefaults != null) {
            restore = Buttons.getRestoreDefaultsButton(ICON_SIZE, "Restore default filters", true);
            restore.setFocusable(false); // must not grab focus (would auto-close the search overlay)
            restore.setMargin(new Insets(0, 0, 0, 0));
            restore.addActionListener(e -> {
                restoreDefaultsAll();
                input.requestFocusInWindow();
            });
        }
        if (openFilterPanel != null) {

            // hands off via the host so the overlay steps aside before the (modal) dialog opens
            filterButton = Buttons.getFilterButton(ICON_SIZE, "Open the filter panel", true);
            filterButton.setFocusable(false);
            filterButton.addActionListener(e -> host.editorHandoff(openFilterPanel));
            filterButton.setMargin(new Insets(0, 0, 0, 0));
        }

        // field-name display toggle (compact terminal names <-> fully-qualified): a borderless icon
        // button whose expand/collapse arrows show the ACTION a click performs (expand arrows while
        // names are compact, collapse arrows while fully-qualified), matching the other in-field icons
        modeIcon = new CompactToggleIcon(ICON_SIZE, Colors.searchFieldIconColor(), renderState.mode() == FieldDisplay.Mode.COMPACT);
        ToolbarButton modeToggle = new ToolbarButton(modeIcon, null, true);
        modeToggle.setFocusable(false);
        modeToggle.setMargin(new Insets(0, 0, 0, 0));
        modeToggle.addActionListener(e -> {
            renderState.toggleMode();
            styleModeToggle(modeToggle);
            rebuild();
            refreshCollapsedBar.run();
        });
        styleModeToggle(modeToggle);

        // invert toggle: flips the whole query to its complement (a model-level flag surfaced here),
        // tinted like the funnel (grey / blue / red). A borderless in-field icon like the others.
        invertIcon = new InvertIcon(ICON_SIZE, Colors.searchFieldIconColor());
        invertToggle = new ToolbarButton(invertIcon, null, true);
        invertToggle.setFocusable(false);
        invertToggle.setMargin(new Insets(0, 0, 0, 0));
        invertToggle.addActionListener(e -> setInverted(!inverted));

        // order left-to-right: query-state actions (clear, restore, invert) then meta controls (display mode, funnel)
        controls.add(clear);
        if (restore != null)
            controls.add(restore);
        controls.add(invertToggle);
        controls.add(modeToggle);
        if (filterButton != null)
            controls.add(filterButton);

        // chips zone with the trailing controls (clear / invert / mode / funnel) at its top-right,
        // and the typing zone stacked directly beneath it
        JPanel chipsHeader = new JPanel(new BorderLayout());
        chipsHeader.setOpaque(false);
        chipsHeader.add(chipsScroll, BorderLayout.CENTER);
        JPanel controlsAligned = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        controlsAligned.setOpaque(false);
        controlsAligned.add(controls);
        chipsHeader.add(controlsAligned, BorderLayout.EAST);

        Box northStack = Box.createVerticalBox();
        northStack.setOpaque(false);
        northStack.add(chipsHeader);
        northStack.add(typingLine);
        add(northStack, BorderLayout.NORTH);

        // the floating overlay's main actions live in a right-aligned footer (like the app's dialogs);
        // the embedded editor omits it - the filter dialog owns commit/dismissal (Apply / Discard)
        if (!embedded) {
            JButton apply = new JButton("Apply");
            apply.setFocusable(false); // keep keyboard focus in the input (Enter still applies)
            apply.setToolTipText("Apply the filter and close (Enter on an empty input)");
            apply.addActionListener(e -> runSearch());

            JButton discard = new JButton("Discard");
            discard.setFocusable(false);
            discard.setToolTipText("Close without applying (Esc)");
            discard.addActionListener(e -> host.editorCloseRequested());

            // icon-only copy of the whole query (filters + user query), left-aligned in the footer
            ToolbarButton copy = new ToolbarButton(Icons.CLIP_BOARD.derive(ICON_SIZE, ICON_SIZE),
                    "Copy the full search query to the clipboard", true);
            copy.setFocusable(false); // a focus grab would auto-close the overlay
            copy.setMargin(new Insets(0, 0, 0, 0));
            copy.addActionListener(e -> copyQueryToClipboard());
            JPanel copyBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            copyBox.setOpaque(false);
            copyBox.add(copy);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
            actions.setOpaque(false);
            actions.add(apply);
            actions.add(discard);

            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.Menu.FILTER_BUTTON));
            footer.add(copyBox, BorderLayout.WEST);
            footer.add(actions, BorderLayout.EAST);
            add(footer, BorderLayout.SOUTH);
        }

        // --- embedded suggestion list (no separate window) ---
        suggestionList = new JList<>();
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFocusable(false);
        suggestionList.setCellRenderer(new SuggestionRenderer());
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    suggestionList.setSelectedIndex(index);
                    // a click on a row does what Enter on it would do (the run-action row runs the search)
                    if (isRunActionSelected())
                        runOrCommit();
                    else
                        acceptCurrentToken(false);
                }
            }
        });
        suggestionList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // a list that appears under a resting cursor gets a mouseMoved without any actual
                // movement - that must not hijack the default selection (the run-action row)
                if (hoverAnchor != null) {
                    if (hoverAnchor.equals(e.getLocationOnScreen()))
                        return;
                    hoverAnchor = null;
                }
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0)
                    suggestionList.setSelectedIndex(index);
            }
        });
        suggestionScroll = new JScrollPane(suggestionList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.Menu.FILTER_BUTTON));
        suggestionScroll.setVisible(false);
        add(suggestionScroll, BorderLayout.CENTER);

        if (!embedded) {
            // Esc dismisses via the host even when focus sits on a button inside the editor
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeSearchOverlay");
            getActionMap().put("closeSearchOverlay", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    host.editorCloseRequested();
                }
            });

            // Structured filters may change under the floating overlay (dialog, quick toggles).
            // The embedded editor instead re-renders from the dialog's live widget state (see the
            // widget listeners the dialog attaches) and would leak a listener per dialog open.
            // The event can be fired off the EDT (background project reload), so marshal onto it.
            filterModel.addUpdateCompleteListener(evt -> SwingUtilities.invokeLater(() -> {
                if (isShowing())
                    rebuild();
            }));
        }
    }

    /**
     * Clears the user's own query (typed clauses + free text); the filter-dialog filters are kept.
     */
    public void clearUserQuery() {
        root = QueryContainer.empty();
        openPath = new int[0];
        tokenModel.reset();
        input.setText("");
        pendingModelRemovals.clear(); // restore any staged-removed chips
        rebuild();
    }

    /**
     * Clear: reset every filter and the search query. The contract differs by host:
     * <ul>
     *   <li><b>Embedded dialog</b> - resets the backing widgets via {@link #clearFilter} and wipes the
     *       working query. Staged like every dialog edit: applied on Apply, reverted by Discard/Esc.</li>
     *   <li><b>Overlay</b> - stages a removal of every active facet and clears the working query, leaving
     *       the baseline intact. Applied on Apply/Enter (commit), reverted on Esc/close; the model is not
     *       touched on click.</li>
     * </ul>
     */
    public void clearAll() {
        if (embedded) {
            if (clearFilter != null)
                clearFilter.run(); // reset the backing widgets (staged; the model is not touched now)
            resetLocalQueryToEmptyBaseline();
        } else {
            // overlay: stage a removal of every active facet and clear the working query, leaving the
            // baseline intact so Esc/close restores the applied filter and Apply/Enter applies the clear.
            for (FilterTerm term : termSupplier.get())
                pendingModelRemovals.put(term.id(), term::remove);
            root = QueryContainer.empty();
            openPath = new int[0];
            tokenModel.reset();
            input.setText("");
            inverted = false; // a full clear also drops the inversion
            rebuild();
        }
    }

    /**
     * Restore the default filter configuration (SIRIUS's recommended starting filter) instead of clearing
     * everything. Dialog-only and staged: {@link #restoreDefaults} resets the backing widgets to their
     * defaults (applied on Apply, reverted by Discard/Esc), and the editor's own query state is reset to
     * match.
     */
    public void restoreDefaultsAll() {
        if (restoreDefaults != null)
            restoreDefaults.run();
        resetLocalQueryToEmptyBaseline();
    }

    /**
     * Resets the editor's own working query (chips + free text + inversion) to empty and adopts that as the
     * new baseline, so a later Cancel/close does not resurrect the old query. Shared by the embedded Clear
     * and the Restore-defaults action.
     */
    private void resetLocalQueryToEmptyBaseline() {
        root = QueryContainer.empty();
        openPath = new int[0];
        tokenModel.reset();
        input.setText("");
        pendingModelRemovals.clear();
        inverted = false;
        baselineRoot = root;
        baselineFreeText = "";
        baselineInverted = false;
        lastCompiled = Optional.ofNullable(filterModel.getSearchText()).orElse("");
        rebuild();
    }

    /**
     * Copies the whole query as currently shown - the panel facets (minus any staged removals) AND the
     * user's own query, wrapped for inversion - to the system clipboard. Mirrors the executed-query
     * composition in {@link FeatureFilterModel} (facets AND free-text segment, {@code *:* AND NOT (...)}
     * when inverted), but from the live editor state rather than the committed model, so it reflects
     * unsaved edits.
     */
    private void copyQueryToClipboard() {
        List<QueryNode> facets = new ArrayList<>();
        for (FilterTerm term : termSupplier.get())
            if (!pendingModelRemovals.containsKey(term.id()))
                facets.add(term.toQueryNode());
        List<LogicOp> ands = new ArrayList<>(Math.max(0, facets.size() - 1));
        for (int i = 1; i < facets.size(); i++)
            ands.add(LogicOp.AND);
        // the user query rides in as the free-text segment, so it becomes "(facets) AND (userQuery)".
        // The facets are compiled as executed (incl. the match-all anchor for negation-only facets such
        // as "no lipid class"), so the copied query runs as-is; the user query stays verbatim.
        String userQuery = LuceneQueryCompiler.compile(root, freeTextForCommit());
        String core = LuceneQueryCompiler.compileExecutable(new QueryContainer(facets, ands), userQuery);
        String whole = inverted && !core.isBlank() ? "*:* AND NOT (" + core + ")" : core;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(whole), null);
    }

    /** Whether the working query is inverted (applied to the model at the host's commit point). */
    public boolean isInverted() {
        return inverted;
    }

    /** Sets the working inversion and re-renders (the toggle tint and the inversion chip). */
    public void setInverted(boolean inverted) {
        if (this.inverted == inverted)
            return;
        this.inverted = inverted;
        rebuild();
    }

    /**
     * Labels the field-name display toggle for the current mode.
     */
    private void styleModeToggle(JButton button) {
        boolean compact = renderState.mode() == FieldDisplay.Mode.COMPACT;
        // action-oriented: show the arrows for what a click will do - expand (outward) while compact,
        // collapse (inward) while fully-qualified
        modeIcon.setExpand(compact);
        button.setToolTipText(GuiUtils.formatToolTip(compact
                ? "Show fully-qualified field names"
                : "Show compact field names"));
        button.repaint();
    }

    /** Tints the invert toggle exactly like the funnel: idle grey when no filter is active, the accent
     *  (blue) when active and not inverted, the inverted accent (red) when inverted. */
    private void styleInvertToggle() {
        invertIcon.setColor(hasWorkingQuery()
                ? (inverted ? Colors.Menu.FILTER_BUTTON_INVERTED : Colors.Menu.FILTER_BUTTON)
                : Colors.searchFieldIconColor());
        invertToggle.setToolTipText(GuiUtils.formatToolTip(inverted
                ? "Query is inverted - click to un-invert (matching features become non-matching and vice versa)"
                : "Invert the whole query (matching features become non-matching and vice versa)"));
        invertToggle.repaint();
    }

    /** The accent colour for the query chips/groups: the inverted tone while the query is inverted,
     *  the normal filter accent otherwise - so an inverted query reads red-ish rather than blue-ish. */
    private Color queryAccent() {
        return inverted ? Colors.Menu.FILTER_BUTTON_INVERTED : Colors.Menu.FILTER_BUTTON;
    }

    /** Whether the working query has anything to filter/invert: a user clause, free text, or a
     *  (not-staged-for-removal) panel facet. Drives the live funnel / invert-toggle tint. */
    private boolean hasWorkingQuery() {
        return !root.items().isEmpty()
                || !freeTextForCommit().isEmpty()
                || termSupplier.get().stream().anyMatch(t -> !pendingModelRemovals.containsKey(t.id()));
    }

    // --- session lifecycle (driven by the host) ---

    /**
     * Prepares the editor for a fresh editing session: degrades an externally-edited document into
     * free text, captures the revert baseline, applies the optional type-ahead keystroke and kicks
     * off a background refresh of the searchable fields. The host handles making itself visible and
     * then calls {@link #focusInputAndRefresh()}.
     */
    public void openSession(@Nullable String typeAhead) {
        hoverAnchor = pointerLocation(); // ignore hover-selection until the mouse really moves
        String docText = Optional.ofNullable(filterModel.getSearchText()).orElse("");
        if (!docText.equals(lastCompiled)) {
            openPath = new int[0];
            tokenModel.reset();
            // hydrate the existing query into editable chips; if it uses constructs we do not model
            // (or is not valid lucene) fall back to showing its text in the input as a free-text edit
            Optional<QueryContainer> parsed = QueryStringParser.parse(docText, fieldsProvider.getCached());
            if (parsed.isPresent()) {
                root = parsed.get();
                input.setText("");
            } else {
                root = QueryContainer.empty();
                input.setText(docText);
            }
            lastCompiled = docText;
        }
        // seed the working inversion from the applied model state each open
        inverted = filterModel.isInverted();
        // the applied query is the baseline Cancel reverts to (before any type-ahead edit)
        baselineRoot = root;
        baselineFreeText = input.getText();
        baselineInverted = inverted;
        if (typeAhead != null)
            input.setText(input.getText() + typeAhead);

        // tags are dynamic - refresh the searchable fields in the background; list updates on arrival
        Jobs.runInBackground(() -> {
            fieldsProvider.refreshIfStale();
            SwingUtilities.invokeLater(() -> {
                if (isShowing())
                    refreshSuggestions();
            });
        });

        rebuild();
    }

    /** The pointer's screen position, or null when it cannot be determined (headless, no pointer). */
    @Nullable
    private static Point pointerLocation() {
        try {
            PointerInfo info = MouseInfo.getPointerInfo();
            return info == null ? null : info.getLocation();
        } catch (RuntimeException e) {
            log.debug("Could not read the pointer location", e);
            return null;
        }
    }

    /**
     * Focuses the input and shows the dropdown; the host calls this once it is on screen.
     */
    public void focusInputAndRefresh() {
        input.requestFocusInWindow();
        input.setCaretPosition(input.getText().length());
        refreshSuggestions();
    }

    /**
     * Discards any uncommitted edits: the working state reverts to the last applied query (the
     * baseline). A commit updates the baseline first, so reverting right after committing keeps the
     * applied query.
     */
    public void revertToBaseline() {
        root = baselineRoot;
        inverted = baselineInverted;
        openPath = new int[0];
        tokenModel.reset();
        input.setText(baselineFreeText);
        pendingModelRemovals.clear(); // discard staged model-filter removals -> model stays as applied
    }

    // --- input wiring: suggestions, keyboard semantics ---

    private void wireInput() {
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onTyped();
            }

            public void removeUpdate(DocumentEvent e) {
                onTyped();
            }

            public void changedUpdate(DocumentEvent e) {
                onTyped();
            }
        });

        input.setFocusTraversalKeysEnabled(false);
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> {
                        moveSuggestionSelection(1);
                        e.consume();
                    }
                    case KeyEvent.VK_UP -> {
                        moveSuggestionSelection(-1);
                        e.consume();
                    }
                    case KeyEvent.VK_TAB -> {
                        // Tab only ever ADDS to the query: it picks the highlighted suggestion (the
                        // run-action row is skipped - Tab never runs the search) or commits the
                        // typed value / advances the token.
                        acceptCurrentToken(true);
                        e.consume();
                    }
                    case KeyEvent.VK_ENTER -> {
                        // Enter applies the highlighted row: the run-action row on top of an
                        // untouched input runs the search, everything else is added like Tab. With
                        // nothing left to add, Enter runs the search too - so typing a term and
                        // pressing Enter twice is the quick full-text search. Embedded, "run" is
                        // the dialog's Apply (which bakes a terminal token via commitToDocument).
                        if (isRunActionSelected() || !acceptCurrentToken(false))
                            runOrCommit();
                        e.consume();
                    }
                    case KeyEvent.VK_BACK_SPACE -> {
                        if (input.getText().isEmpty()) {
                            tokenModel.backspaceOnEmpty().ifPresent(QueryEditorPanel.this::applyEvent);
                            rebuild();
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        host.editorCloseRequested();
                        e.consume();
                    }
                }
            }
        });
    }

    private void onTyped() {
        refreshSuggestions();
    }

    private void applySuggestion(TokenInputModel.Suggestion suggestion) {
        tokenModel.choose(suggestion).ifPresent(this::applyEvent);
        input.setText("");
        rebuild(); // restores input focus (see rebuild)
    }

    private void applyEvent(TokenInputModel.Event event) {
        if (event instanceof TokenInputModel.Event.ClauseCompleted completed) {
            root = QueryTreeOps.append(root, openPath, completed.clause(), completed.logic());
        } else if (event instanceof TokenInputModel.Event.QueryParsed parsed) {
            // splice the parsed query into the open container: first item joins the existing chips by
            // the connector logic, the rest by the parsed query's own connectors
            List<QueryNode> items = parsed.container().items();
            List<LogicOp> logics = parsed.container().logics();
            for (int i = 0; i < items.size(); i++)
                root = QueryTreeOps.append(root, openPath, items.get(i), i == 0 ? parsed.logic() : logics.get(i - 1));
        } else if (event instanceof TokenInputModel.Event.OpenGroup group) {
            QueryTreeOps.PathResult result = QueryTreeOps.openGroup(root, openPath, group.negated(), group.logic());
            root = result.root();
            openPath = result.path();
        } else if (event instanceof TokenInputModel.Event.CloseGroup) {
            QueryTreeOps.PathResult result = QueryTreeOps.closeGroup(root, openPath);
            root = result.root();
            openPath = result.path();
        } else if (event instanceof TokenInputModel.Event.RemoveLastNode) {
            removeLastNode();
        }
    }

    /**
     * Backspace with nothing staged: removes the last chip of the open container; an empty open
     * group closes (and thereby drops) instead.
     */
    private void removeLastNode() {
        QueryContainer container = QueryTreeOps.containerAt(root, openPath);
        if (container.isEmpty()) {
            if (openPath.length > 0) {
                QueryTreeOps.PathResult result = QueryTreeOps.closeGroup(root, openPath);
                root = result.root();
                openPath = result.path();
            }
            return;
        }
        QueryTreeOps.PathResult removed = QueryTreeOps.removeNode(root, openPath, container.items().get(container.items().size() - 1).id());
        root = removed.root();
        openPath = removed.path();
    }

    // --- embedded suggestion list handling ---

    private void refreshSuggestions() {
        tokenModel.updateContext(fieldsProvider.getCached(),
                !QueryTreeOps.containerAt(root, openPath).isEmpty(), openPath.length > 0);
        if (isShowing())
            showSuggestions(tokenModel.suggestions(input.getText()));
        validateInput();
        updateKeyHint();
    }

    private void showSuggestions(List<TokenInputModel.Suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            if (suggestionScroll.isVisible()) {
                suggestionScroll.setVisible(false);
                host.editorContentChanged();
            }
            return;
        }
        TokenInputModel.Suggestion previous = suggestionList.getSelectedValue();
        suggestionList.setListData(suggestions.toArray(TokenInputModel.Suggestion[]::new));
        // while the run-action row is offered it IS the default pick - keeping an earlier selection
        // would leave a field highlighted after the input was emptied again (Enter would add it)
        boolean runActionOnTop = suggestions.get(0) instanceof TokenInputModel.Suggestion.RunActionSuggestion;
        int keep = previous == null || runActionOnTop ? -1 : suggestions.indexOf(previous);
        suggestionList.setSelectedIndex(Math.max(keep, 0));
        suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
        suggestionList.setVisibleRowCount(Math.min(suggestions.size(), MAX_LIST_ROWS));
        suggestionScroll.setVisible(true);
        host.editorContentChanged();
    }

    private void moveSuggestionSelection(int delta) {
        int size = suggestionList.getModel().getSize();
        if (!suggestionScroll.isVisible() || size == 0)
            return;
        int index = ((suggestionList.getSelectedIndex() + delta) % size + size) % size;
        suggestionList.setSelectedIndex(index);
        suggestionList.ensureIndexIsVisible(index);
    }

    /**
     * Adds the current token to the query: the highlighted suggestion, or the typed text where the
     * stage can take it. False when there is nothing to add at all - the caller (Enter) then runs
     * the search instead.
     *
     * @param skipRunAction Tab's contract: never run the action, use the first real row below it
     */
    private boolean acceptCurrentToken(boolean skipRunAction) {
        if (chooseSelectedSuggestion(skipRunAction))
            return true;
        if (!tokenModel.canAccept(input.getText()))
            return false;
        tokenModel.submitTyped(input.getText()).ifPresent(QueryEditorPanel.this::applyEvent);
        input.setText("");
        rebuild();
        return true;
    }

    /** Runs the query: the search for the floating overlay, Apply for the embedded dialog. */
    private void runOrCommit() {
        if (embedded)
            host.editorCommitRequested();
        else
            runSearch();
    }

    /** Whether the highlighted row is the run-action row (Enter on it runs the search / applies). */
    private boolean isRunActionSelected() {
        return suggestionScroll.isVisible()
                && suggestionList.getSelectedValue() instanceof TokenInputModel.Suggestion.RunActionSuggestion;
    }

    /**
     * Picks the highlighted suggestion; false when there is nothing to pick, so the caller can fall
     * back to its own handling. The run-action row is never "picked" here: Enter handles it before
     * calling (it runs the search), and Tab - which must not run anything - either steps over it to
     * the first real row below ({@code skipRunAction}) or reports nothing to pick.
     */
    private boolean chooseSelectedSuggestion(boolean skipRunAction) {
        if (!suggestionScroll.isVisible())
            return false;
        TokenInputModel.Suggestion selected = suggestionList.getSelectedValue();
        if (selected instanceof TokenInputModel.Suggestion.RunActionSuggestion) {
            int next = suggestionList.getSelectedIndex() + 1;
            if (!skipRunAction || next >= suggestionList.getModel().getSize())
                return false;
            selected = suggestionList.getModel().getElementAt(next);
        }
        if (selected == null)
            return false;
        applySuggestion(selected);
        return true;
    }

    /**
     * Advisory live validation of the free-text segment: syntax problems and unknown fields show as
     * a warning outline with the explanation as tooltip. Suppressed while the list offers something.
     */
    private void validateInput() {
        String problem = tokenModel.atEntryStage() && !suggestionScroll.isVisible()
                ? QueryValidator.validate(input.getText(), fieldsProvider.getCached()).orElse(null)
                : null;
        input.putClientProperty("JComponent.outline", problem == null ? null : "warning");
        input.setToolTipText(problem == null ? null : GuiUtils.formatToolTip(problem));
    }

    /**
     * The trailing key hint: what Enter/Tab do with what is currently typed. An empty input shows
     * nothing here - there the highlighted run-action row is the hint (and the field's placeholder,
     * {@link TokenInputModel#stagePrompt()}, invites typing).
     */
    private void updateKeyHint() {
        keyHint.setText(keyHintText());
    }

    private String keyHintText() {
        if (input.getText().isEmpty())
            return "";
        boolean addable = (suggestionScroll.isVisible() && suggestionList.getSelectedValue() != null
                && !isRunActionSelected()) || tokenModel.canAccept(input.getText());
        if (addable)
            return "Tab or Enter to add";
        return "Enter to apply";
    }

    // --- compile & commit ---

    /**
     * The input text only counts as a free-text search segment at an entry stage; at a mid-token
     * stage it is a value/operator fragment that running the query discards (the accept key would
     * have committed it into a clause first).
     */
    private String freeTextForCommit() {
        return tokenModel.atEntryStage() ? input.getText().trim() : "";
    }

    /** The compiled user query (committed chips + any trailing free text) as it stands, WITHOUT
     *  committing to the shared document - for callers that need the current search-bar query, e.g.
     *  the dialog's delete-non-matching complement. Excludes a half-built (unaccepted) clause. */
    public String userQuery() {
        return LuceneQueryCompiler.compile(root, freeTextForCommit());
    }

    /**
     * Accepts a terminal token as a chip, then runs the search. Shared by the Apply button and by
     * Enter (once there is nothing left to add) so both behave the same.
     */
    private void runSearch() {
        if (tokenModel.isTerminal(input.getText())) {
            if (tokenModel.atEntryStage())
                applyEvent(tokenModel.completeFreeText(input.getText()));
            else
                tokenModel.submitTyped(input.getText()).ifPresent(this::applyEvent);
            input.setText("");
        }
        commitSearch();
    }

    private void commitSearch() {
        String freeText = freeTextForCommit();
        tokenModel.reset(); // a half-built token is not part of the query
        // now that the search is actually being run, apply the model-filter removals staged in the
        // editor (Cancel would instead have discarded them in revertToBaseline())
        pendingModelRemovals.values().forEach(Runnable::run);
        pendingModelRemovals.clear();
        String compiled = LuceneQueryCompiler.compile(root, freeText);
        writeDocument(compiled);
        filterModel.setInverted(inverted); // the overlay applies inversion here (dialog does it in applyToModel)
        lastCompiled = compiled;
        // the applied query becomes the new baseline, so the close below keeps it (no revert)
        baselineRoot = root;
        baselineFreeText = freeText;
        baselineInverted = inverted;
        filterModel.fireUpdateCompleted();
        Commit commit = new Commit(root, freeText, compiled);
        host.editorCloseRequested();
        onCommitted.accept(commit);
    }

    /**
     * Bakes the user's query into the shared search document WITHOUT firing an update or dismissing -
     * for the embedded host (the filter dialog), whose Apply writes the widget model and this query
     * together and then fires a single update. A half-built terminal token is accepted first, like
     * {@link #runSearch()}.
     */
    public void commitToDocument() {
        if (tokenModel.isTerminal(input.getText())) {
            if (tokenModel.atEntryStage())
                applyEvent(tokenModel.completeFreeText(input.getText()));
            else
                tokenModel.submitTyped(input.getText()).ifPresent(this::applyEvent);
            input.setText("");
        }
        String freeText = freeTextForCommit();
        tokenModel.reset();
        String compiled = LuceneQueryCompiler.compile(root, freeText);
        writeDocument(compiled);
        lastCompiled = compiled;
        baselineRoot = root;
        baselineFreeText = freeText;
        baselineInverted = inverted; // the dialog's applyToModel reads isInverted() to apply it
    }

    private void writeDocument(String text) {
        Document doc = filterModel.getSearchTextDoc();
        try {
            doc.remove(0, doc.getLength());
            doc.insertString(0, text, null);
        } catch (BadLocationException e) {
            log.error("Could not write search query into the filter document", e);
        }
    }

    // --- rendering ---

    public void rebuild() {
        // keep the funnel and invert toggle in sync with the WORKING filter state (grey / blue / red),
        // so they recolour live as the query is built/inverted - not only after the change is committed
        if (filterButton != null)
            filterButton.setFilterActive(hasWorkingQuery(), inverted);
        styleInvertToggle();

        // removeAll() detaches the (focused) input, which drops the keyboard focus; restore it
        // after the re-layout so typing continues seamlessly after choosing a suggestion
        boolean refocus = input.isFocusOwner();
        chipsRow.removeAll();

        // --- upper zone: the finished terms as chips ---
        // render only the model filters not staged for removal - a staged removal hides the chip but
        // does not touch the model until the search is committed (Cancel discards it, see revertToBaseline())
        List<FilterTerm> terms = termSupplier.get().stream()
                .filter(t -> !pendingModelRemovals.containsKey(t.id()))
                .toList();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0)
                chipsRow.add(ChipComponent.implicitAndLabel()); // dialog filters always AND together
            FilterTerm term = terms.get(i);
            QueryNode node = term.toQueryNode();
            chipsRow.add(new ChipComponent(
                    QueryNodeRenderer.label(node, renderState.mode(), renderState.suffixLengthResolver()),
                    GuiUtils.formatToolTip(LuceneQueryCompiler.render(node),
                            embedded ? "Filter from the tabs above - click to jump to its control; combined with AND"
                                    : "Filter from the filter dialog - click to edit; combined with AND"),
                    ChipComponent.Style.MODEL,
                    // hand off to the full editor (overlay: close + open dialog; embedded dialog: jump to the tab)
                    () -> host.editorHandoff(() -> term.openEditor(editorHost)),
                    // remove the model filter: the embedded dialog resets the backing widget through the
                    // host (its chips mirror widget state); the overlay stages a removal on its own
                    // snapshot model (applied on commit, reverted on Cancel)
                    embedded ? () -> editorHost.removeFilter(term) : () -> {
                        pendingModelRemovals.put(term.id(), term::remove);
                        rebuild();
                    }).withAccent(queryAccent()));
        }

        List<QueryNode> items = root.items();
        // the dialog filters and the user's own query are combined with AND - make that visible
        if (!terms.isEmpty() && !items.isEmpty())
            chipsRow.add(ChipComponent.implicitAndLabel());
        for (int i = 0; i < items.size(); i++) {
            if (i > 0)
                chipsRow.add(buildLogicComponent(root.logics().get(i - 1), new int[]{i}));
            chipsRow.add(buildNode(items.get(i), new int[]{i}));
        }

        // --- lower zone: the term currently being built, on its own line ---
        buildTypingRow();

        // refresh first: it re-syncs the token stage with the (possibly changed) sibling context,
        // which the stage prompt below reads
        refreshSuggestions();
        input.setPlaceholder(tokenModel.stagePrompt());
        chipsRow.revalidate();
        chipsRow.repaint();
        typingLine.revalidate();
        typingLine.repaint();
        if (isShowing())
            host.editorContentChanged();
        // when the chips zone overflows (e.g. while building a group), keep the newest / in-progress
        // chips in view rather than the already-finished ones at the top
        SwingUtilities.invokeLater(this::scrollChipsToBottom);
        if (refocus)
            SwingUtilities.invokeLater(input::requestFocusInWindow);
    }

    /**
     * Scrolls the chips zone to the bottom; a no-op when everything already fits (no scrollbar).
     */
    private void scrollChipsToBottom() {
        JScrollBar bar = chipsScroll.getVerticalScrollBar();
        bar.setValue(bar.getMaximum());
    }

    /**
     * (Re)builds the lower typing zone: one open-group marker per open nesting level (so the user
     * sees the committed clause will land inside those groups), then the staged token fragments and
     * the text input. The input always lives on this line, so the stage prompt stays readable no
     * matter how many finished chips fill the zone above.
     */
    private void buildTypingRow() {
        typingRow.removeAll();
        for (int depth = 0; depth < openPath.length; depth++)
            typingRow.add(openGroupMarker());
        for (String fragment : tokenModel.pendingFragments())
            typingRow.add(new ChipComponent(fragment, "Being built - Backspace removes it",
                    ChipComponent.Style.USER, null, null));
        // the input sits between these two (BorderLayout.CENTER), so it fills the rest of the line
        typingTrailing.removeAll();
        // a clickable ) at the end of the line being typed, to close the innermost open group and continue
        if (openPath.length > 0)
            typingTrailing.add(closeGroupControl());
        typingTrailing.add(keyHint);
    }

    /**
     * A dimmed "(" marker shown in the typing row while a group is open (one per nesting level).
     */
    private JComponent openGroupMarker() {
        JLabel marker = parenLabel("(");
        marker.setToolTipText(GuiUtils.formatToolTip(
                "Adding inside a group - the finished clause lands in the open group above; use the \")\" here to close it."));
        return marker;
    }

    /**
     * The interactive ")" in the typing row: closes the innermost open group (same as typing ")").
     */
    private JComponent closeGroupControl() {
        JLabel close = parenLabel(")");
        close.setToolTipText(GuiUtils.formatToolTip("Close the group and keep adding filters (or type \")\")"));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                closeOpenGroup();
            }
        });
        return close;
    }

    /**
     * Closes the innermost open group and returns focus to the input.
     */
    private void closeOpenGroup() {
        applyEvent(new TokenInputModel.Event.CloseGroup());
        rebuild();
        input.requestFocusInWindow();
    }

    private JComponent buildLogicComponent(LogicOp logic, int[] pathOfFollowingNode) {
        // the operator joining the open group is still unsettled and stays editable;
        // everything committed is inert
        if (Arrays.equals(pathOfFollowingNode, openPath)) {
            JComboBox<LogicOp> combo = new JComboBox<>(LogicOp.values());
            combo.setSelectedItem(logic);
            combo.addActionListener(e -> setLogicBefore(pathOfFollowingNode, (LogicOp) combo.getSelectedItem()));
            return combo;
        }
        return new ChipComponent(logic.toString(), null, ChipComponent.Style.USER, null, null).withAccent(queryAccent());
    }

    private void setLogicBefore(int[] childPath, LogicOp logic) {
        int index = childPath[childPath.length - 1];
        if (index == 0)
            return;
        root = QueryTreeOps.updateContainer(root, Arrays.copyOf(childPath, childPath.length - 1), container -> {
            List<LogicOp> logics = new ArrayList<>(container.logics());
            logics.set(index - 1, logic);
            return new QueryContainer(container.items(), logics);
        });
    }

    private JComponent buildNode(QueryNode node, int[] path) {
        if (node instanceof QueryClause clause) {
            String text = clause.isFreeText()
                    ? (clause.negated() ? "NOT " : "") + "“" + clause.value1() + "”"
                    : (clause.negated() ? "NOT " : "")
                      + QueryNodeRenderer.displayField(clause.field(), renderState.mode(), renderState.suffixLengthResolver())
                      + " " + clauseBody(clause);
            String tooltip = clause.isFreeText()
                    // reveal the (possibly faded-out) full phrase on hover, plus what it does
                    ? GuiUtils.formatToolTip("“" + clause.value1() + "”", "Full-text search in the default fields")
                    : LuceneQueryCompiler.render(clause);
            return new ChipComponent(text, tooltip, ChipComponent.Style.USER,
                    null, () -> {
                QueryTreeOps.PathResult removed = QueryTreeOps.removeNode(root, openPath, clause.id());
                root = removed.root();
                openPath = removed.path();
                rebuild();
            }).withAccent(queryAccent());
        }

        QueryGroup group = (QueryGroup) node;
        boolean open = Arrays.equals(path, openPath);
        JPanel groupPanel = new GroupPanel(open, queryAccent());

        if (open) {
            JToggleButton not = new JToggleButton("NOT", group.negated());
            not.setToolTipText(group.negated() ? "Click to un-negate this group" : "Click to negate this group");
            not.addActionListener(e -> {
                root = QueryTreeOps.updateContainer(root, Arrays.copyOf(path, path.length - 1), container -> {
                    List<QueryNode> newItems = new ArrayList<>(container.items());
                    int idx = path[path.length - 1];
                    newItems.set(idx, newItems.get(idx).withNegated(!newItems.get(idx).negated()));
                    return new QueryContainer(newItems, container.logics());
                });
                rebuild();
            });
            groupPanel.add(not);
        } else if (group.negated()) {
            groupPanel.add(new ChipComponent("NOT", null, ChipComponent.Style.USER, null, null).withAccent(queryAccent()));
        }

        groupPanel.add(parenLabel("("));

        List<QueryNode> children = group.items();
        for (int i = 0; i < children.size(); i++) {
            int[] childPath = Arrays.copyOf(path, path.length + 1);
            childPath[path.length] = i;
            if (i > 0)
                groupPanel.add(buildLogicComponent(group.logics().get(i - 1), childPath));
            groupPanel.add(buildNode(children.get(i), childPath));
        }

        // the in-progress token no longer renders inside the group - it lives in the typing row below,
        // and so does the interactive close (see buildTypingRow / closeGroupControl); the open group
        // here shows only its committed children, so its ) is purely visual
        groupPanel.add(parenLabel(")"));

        if (!open)
            groupPanel.add(ChipComponent.closeLabel(queryAccent(), "Remove this group with all its filters",
                    () -> {
                        QueryTreeOps.PathResult removed = QueryTreeOps.removeNode(root, openPath, group.id());
                        root = removed.root();
                        openPath = removed.path();
                        rebuild();
                    }));
        return groupPanel;
    }

    private JLabel parenLabel(String paren) {
        JLabel label = new JLabel(paren);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 2));
        label.setForeground(queryAccent());
        return label;
    }

    /**
     * Height of one chip row: a representative chip WITH a close button (its bold "x" label is the
     * tallest element - the empty-close sample used before under-measured the real chips), plus a
     * few px headroom so slightly taller rows (group / paren chips, e.g. adducts and quality) are
     * not clipped either.
     */
    private static int chipRowHeight() {
        return new ChipComponent("Ag", null, ChipComponent.Style.MODEL, null, () -> {
        }).getPreferredSize().height + 4;
    }

    /**
     * The capped height of the chips zone: {@link WrapLayout} lays out N rows as
     * {@code N*rowHeight + (N+1)*vgap}, so cap at exactly {@link #MAX_CHIP_ROWS} rows (fully visible,
     * then scroll).
     */
    private static int chipsZoneMaxHeight() {
        return MAX_CHIP_ROWS * chipRowHeight() + (MAX_CHIP_ROWS + 1) * CHIP_VGAP;
    }

    /**
     * A panel that reports {@code true} for {@link #getScrollableTracksViewportWidth()} so its
     * {@link WrapLayout} wraps to the enclosing scroll pane's viewport width (and grows in height)
     * rather than scrolling horizontally.
     */
    private static final class WrapScrollPanel extends JPanel implements Scrollable {
        WrapScrollPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Operator + value as shown on a clause chip, e.g. {@code >= 100} or {@code [100 TO 200]};
     * text-like clauses read as {@code : value}. Package-visible: the collapsed bar renders its
     * committed chips with the same wording.
     */
    static String clauseBody(QueryClause clause) {
        if (clause.op() == null)
            return ": " + clause.value1();
        String v1 = clause.value1().isEmpty() ? "*" : clause.value1();
        String v2 = clause.value2() == null || clause.value2().isEmpty() ? "*" : clause.value2();
        return switch (clause.op()) {
            case EQ -> "= " + v1;
            case LT -> "< " + v1;
            case LTE -> "<= " + v1;
            case GT -> "> " + v1;
            case GTE -> ">= " + v1;
            case RANGE_INCLUSIVE -> "[" + v1 + " TO " + v2 + "]";
            case RANGE_EXCLUSIVE -> "{" + v1 + " TO " + v2 + "}";
        };
    }

    private static class GroupPanel extends JPanel {
        private final boolean open;
        private final Color accent;

        GroupPanel(boolean open, @NotNull Color accent) {
            super(new FlowLayout(FlowLayout.LEFT, 3, 2));
            this.open = open;
            this.accent = accent;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                if (open) {
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            0, new float[]{4, 3}, 0));
                }
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Renders a suggestion row: display text with the (dimmed, truncated) description behind it. In
     * compact mode, ONLY field-name rows are shortened (per the field's significantSuffixLength) - the
     * fully-qualified name is moved into the dimmed text so it stays discoverable; operator, value and
     * connector rows are shown verbatim (compacting a value like {@code 195.08} would corrupt it).
     */
    private class SuggestionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            TokenInputModel.Suggestion suggestion = (TokenInputModel.Suggestion) value;
            String display = suggestion.display();
            String description = suggestion.description();

            if (suggestion instanceof TokenInputModel.Suggestion.FieldSuggestion fieldSuggestion
                    && renderState.mode() == FieldDisplay.Mode.COMPACT) {
                var field = fieldSuggestion.field();
                int suffix = field.getSignificantSuffixLength() != null ? field.getSignificantSuffixLength() : 1;
                String compact = FieldDisplay.compact(field.getName(), suffix);
                if (!compact.equals(field.getName())) {
                    display = compact;
                    // keep the fully-qualified name visible (and searchable) in the dimmed text
                    description = field.getName()
                            + (description == null || description.isBlank() ? "" : "  ·  " + description);
                }
            }

            String text = description == null || description.isBlank()
                    ? escape(display)
                    : "<html><b>" + escape(display) + "</b>&nbsp;&nbsp;<span style='color:gray'>"
                      + escape(truncate(description)) + "</span></html>";
            Component component = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            // the run-action row is not a query part - set it apart by the accent tint and a rule
            // below it, so the rows underneath read as "and these add to the query"
            if (suggestion instanceof TokenInputModel.Suggestion.RunActionSuggestion) {
                ((JComponent) component).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.Menu.FILTER_BUTTON),
                        BorderFactory.createEmptyBorder(3, 8, 3, 8)));
                if (!isSelected)
                    component.setForeground(Colors.Menu.FILTER_BUTTON);
            } else {
                ((JComponent) component).setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            }
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
