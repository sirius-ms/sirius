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

import com.formdev.flatlaf.icons.FlatClearIcon;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/**
 * The expanded state of the feature search bar: the inline lucene query builder, modeled after
 * GitLab's filtered search. ONE wrapping line holds the filter-dialog state as outlined chips, the
 * user's committed clause chips (groups as nested paren chips), the staged fragments of the token
 * being built and the text input; an embedded suggestion list below it offers all candidates for
 * the current stage ({@link TokenInputModel}) and narrows while typing.
 * <p>
 * It is an undecorated, non-modal heavyweight {@link JDialog} anchored on top of the collapsed bar,
 * so it floats above the native JxBrowser result views (a lightweight layered-pane panel would be
 * hidden behind those). It closes on Esc/Cancel, on Enter/Search (run the search), or when it loses
 * window focus - the latter also covers a click on the native result view or anywhere else in the app.
 * <p>
 * Up/Down navigate the suggestion list and Tab is the only key that builds the query (it picks the
 * highlighted suggestion or commits a typed value). Backspace on empty input pops a stage. Enter
 * runs the search, accepting a terminal token as a chip first.
 * <p>
 * The builder state compiles into the {@link FeatureFilterModel}'s shared search text document on
 * commit - the model itself needs no change and the filter dialog's fulltext field shows the
 * compiled query automatically.
 */
@Slf4j
public class SearchBarOverlay extends JDialog {

    private static final int MAX_WIDTH = 900;
    private static final int MIN_WIDTH = 500;
    private static final int MAX_HEIGHT = 460;
    private static final int MAX_LIST_ROWS = 10;

    /**
     * What a commit produced - the collapsed bar renders its chips from this snapshot (never by
     * parsing the compiled string).
     */
    public record Commit(@NotNull QueryContainer root, @NotNull String freeText, @NotNull String compiled) {
    }

    private final FeatureFilterModel filterModel;
    private final SearchableFieldsProvider fieldsProvider;
    private final Supplier<List<FilterTerm>> termSupplier;
    private final FilterEditorHost editorHost;
    private final SearchRenderState renderState;
    private final Runnable refreshCollapsedBar;
    private final java.util.function.Consumer<Commit> onCommitted;

    // --- builder state ---
    private QueryContainer root = QueryContainer.empty();
    private int[] openPath = new int[0];
    private final TokenInputModel tokenModel = new TokenInputModel();
    /**
     * The applied query the working state reverts to on Cancel: captured when the overlay opens and
     * whenever a search is committed. Uncommitted edits made while open are discarded back to this.
     */
    private QueryContainer baselineRoot = QueryContainer.empty();
    private String baselineFreeText = "";
    /**
     * Whether the overlay has held window focus since it was shown. Gates the focus-loss cancel so
     * the transient focus changes while the window is coming up cannot close it immediately.
     */
    private boolean focusEstablished = false;
    /**
     * The query string this overlay last wrote into the shared search document. If the document
     * differs on open, it was edited elsewhere (filter dialog) - the builder then degrades the
     * document content into its free-text segment instead of trying to parse it back into chips.
     */
    private String lastCompiled = "";
    /**
     * Model-filter chip removals staged in the overlay but not yet applied. Like the user's own
     * clauses, they take effect only on commit (Search) and are discarded on Cancel - so the
     * {@link FeatureFilterModel} is left untouched until the query is actually run. Keyed by chip
     * label so {@link #rebuild} can hide a staged-removed chip without mutating the model.
     */
    private final LinkedHashMap<String, Runnable> pendingModelRemovals = new LinkedHashMap<>();

    // --- ui ---
    private final JPanel inlineRow;
    private final PlaceholderTextField input;
    private final JList<TokenInputModel.Suggestion> suggestionList;
    private final JScrollPane suggestionScroll;

    private int overlayWidth = MIN_WIDTH;
    @org.jetbrains.annotations.Nullable
    private Component anchor;

    public SearchBarOverlay(@NotNull Window owner,
                            @NotNull FeatureFilterModel filterModel,
                            @NotNull SearchableFieldsProvider fieldsProvider,
                            @NotNull Supplier<List<FilterTerm>> termSupplier,
                            @NotNull FilterEditorHost editorHost,
                            @NotNull SearchRenderState renderState,
                            @NotNull java.util.function.Consumer<Commit> onCommitted,
                            @NotNull Runnable refreshCollapsedBar) {
        super(owner);
        this.filterModel = filterModel;
        this.fieldsProvider = fieldsProvider;
        this.termSupplier = termSupplier;
        this.editorHost = editorHost;
        this.renderState = renderState;
        this.onCommitted = onCommitted;
        this.refreshCollapsedBar = refreshCollapsedBar;

        // A heavyweight top-level window so it floats above the native JxBrowser windows of the
        // result views (a lightweight layered-pane panel is hidden behind those). Undecorated so it
        // reads as an inline expansion of the collapsed bar. NON-modal on purpose: a modal dialog
        // would block the rest of the UI, and the outside-click dismissal relies on focus moving
        // away to another window (see the window-focus listener) - which modality would prevent.
        // Only ONE window (the suggestion list is embedded), avoiding the multi-window focus/paint
        // fragility.
        setUndecorated(true);
        setModalityType(ModalityType.MODELESS);
        setFocusableWindowState(true);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIManager.getColor("TextField.background"));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.Menu.FILTER_BUTTON, 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        setContentPane(content);

        // --- top: the one inline row (chips + staged fragments + input) plus trailing controls ---
        inlineRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 4));
        inlineRow.setOpaque(false);

        input = new PlaceholderTextField(18);
        input.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        input.setOpaque(false);
        wireInput();

        Box controls = Box.createHorizontalBox();

        // field-name display toggle (compact terminal names <-> fully-qualified), shared with the bar
        JButton modeToggle = new JButton();
        modeToggle.setFocusable(false);
        styleModeToggle(modeToggle);
        modeToggle.addActionListener(e -> {
            renderState.toggleMode();
            styleModeToggle(modeToggle);
            rebuild();
            refreshCollapsedBar.run();
        });
        controls.add(modeToggle);
        controls.add(Box.createHorizontalStrut(6));

        JButton clear = iconButton(new FlatClearIcon(), "Clear the whole search query (filter-dialog filters are kept)");
        clear.addActionListener(e -> {
            root = QueryContainer.empty();
            openPath = new int[0];
            tokenModel.reset();
            input.setText("");
            pendingModelRemovals.clear(); // "filter-dialog filters are kept" -> restore any staged-removed chips
            rebuild();
            input.requestFocusInWindow();
        });
        controls.add(clear);

        controls.add(Box.createHorizontalStrut(6));
        JButton cancel = new JButton("Cancel");
        cancel.setFocusable(false);
        cancel.setToolTipText("Close without applying (Esc)");
        cancel.addActionListener(e -> close());
        controls.add(cancel);

        JButton search = new JButton("Search");
        search.setToolTipText("Run the search and close (Enter)");
        search.addActionListener(e -> runSearch());
        controls.add(search);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(inlineRow, BorderLayout.CENTER);
        JPanel controlsAligned = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        controlsAligned.setOpaque(false);
        controlsAligned.add(controls);
        top.add(controlsAligned, BorderLayout.EAST);
        content.add(top, BorderLayout.NORTH);

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
                    chooseSelectedSuggestion();
                }
            }
        });
        suggestionList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0)
                    suggestionList.setSelectedIndex(index);
            }
        });
        suggestionScroll = new JScrollPane(suggestionList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.Menu.FILTER_BUTTON));
        suggestionScroll.setVisible(false);
        content.add(suggestionScroll, BorderLayout.CENTER);

        // Esc closes the overlay even when focus sits on a button inside it
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeSearchOverlay");
        getRootPane().getActionMap().put("closeSearchOverlay", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

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

        // structured filters may change while the overlay is open (dialog, quick toggles)
        filterModel.addUpdateCompleteListener(evt -> {
            if (isVisible())
                rebuild();
        });
    }

    private static JButton iconButton(Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setFocusable(false);
        button.setToolTipText(tooltip);
        return button;
    }

    /** Labels the field-name display toggle for the current mode. */
    private void styleModeToggle(JButton button) {
        boolean compact = renderState.mode() == FieldDisplay.Mode.COMPACT;
        button.setText(compact ? "abc" : "a.b.c");
        button.setToolTipText(GuiUtils.formatToolTip(compact
                ? "Field names: compact (terminal name). Click to show fully-qualified names."
                : "Field names: fully-qualified. Click to show compact terminal names."));
    }

    public boolean isOpen() {
        return isVisible();
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
                        // Tab is the only key that builds the query: pick the highlighted
                        // suggestion, or (at a free-form value stage with no suggestion) commit
                        // the typed value / advance the token.
                        if (!chooseSelectedSuggestion()) {
                            tokenModel.submitTyped(input.getText()).ifPresent(SearchBarOverlay.this::applyEvent);
                            input.setText("");
                            rebuild();
                        }
                        e.consume();
                    }
                    case KeyEvent.VK_ENTER -> {
                        // Enter runs the search, accepting a terminal token as a chip first. It
                        // never *selects* a field/operator/connector - Tab does.
                        runSearch();
                        e.consume();
                    }
                    case KeyEvent.VK_BACK_SPACE -> {
                        if (input.getText().isEmpty()) {
                            tokenModel.backspaceOnEmpty().ifPresent(SearchBarOverlay.this::applyEvent);
                            rebuild();
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        close();
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
        root = QueryTreeOps.removeNodeById(root, container.items().get(container.items().size() - 1).id());
        openPath = QueryTreeOps.resolvePath(root, openPath);
    }

    // --- embedded suggestion list handling ---

    private void refreshSuggestions() {
        tokenModel.updateContext(fieldsProvider.getCached(),
                !QueryTreeOps.containerAt(root, openPath).isEmpty(), openPath.length > 0);
        if (isVisible())
            showSuggestions(tokenModel.suggestions(input.getText()));
        validateInput();
    }

    private void showSuggestions(List<TokenInputModel.Suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            if (suggestionScroll.isVisible()) {
                suggestionScroll.setVisible(false);
                resizeToFit();
            }
            return;
        }
        TokenInputModel.Suggestion previous = suggestionList.getSelectedValue();
        suggestionList.setListData(suggestions.toArray(TokenInputModel.Suggestion[]::new));
        int keep = previous == null ? -1 : suggestions.indexOf(previous);
        suggestionList.setSelectedIndex(Math.max(keep, 0));
        suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
        suggestionList.setVisibleRowCount(Math.min(suggestions.size(), MAX_LIST_ROWS));
        suggestionScroll.setVisible(true);
        resizeToFit();
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
     * Picks the highlighted suggestion; false when there is nothing to pick, so the caller can fall
     * back to its own handling.
     */
    private boolean chooseSelectedSuggestion() {
        if (!suggestionScroll.isVisible())
            return false;
        TokenInputModel.Suggestion selected = suggestionList.getSelectedValue();
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

    // --- opening / closing ---

    /**
     * Opens the overlay anchored at the collapsed bar, extending right over the result view.
     * The optional type-ahead is the keystroke that triggered the expansion from the collapsed bar,
     * applied before showing so typing continues seamlessly.
     */
    public void openAt(@NotNull JComponent anchor, @org.jetbrains.annotations.Nullable String typeAhead) {
        this.anchor = anchor;

        // the shared document was edited elsewhere -> degrade its content into the free text
        String docText = Optional.ofNullable(filterModel.getSearchText()).orElse("");
        if (!docText.equals(lastCompiled)) {
            root = QueryContainer.empty();
            openPath = new int[0];
            tokenModel.reset();
            input.setText(docText);
            lastCompiled = docText;
        }
        // the applied query is the baseline Cancel reverts to (before any type-ahead edit)
        baselineRoot = root;
        baselineFreeText = input.getText();
        if (typeAhead != null)
            input.setText(input.getText() + typeAhead);

        // tags are dynamic - refresh the searchable fields in the background; list updates on arrival
        Jobs.runInBackground(() -> {
            fieldsProvider.refreshIfStale();
            SwingUtilities.invokeLater(() -> {
                if (isVisible())
                    refreshSuggestions();
            });
        });

        rebuild();
        resizeToFit();
        reposition();
        setVisible(true);
        toFront();
        // focus the input and show the dropdown once the window is up
        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            input.setCaretPosition(input.getText().length());
            refreshSuggestions();
        });
    }

    /**
     * Closes the overlay, discarding any uncommitted edits: the working state reverts to the last
     * applied query (the baseline). Used by Cancel, Esc, focus loss and the open-filter-dialog chip.
     * A commit updates the baseline first, so closing right after committing keeps the applied query.
     */
    public void close() {
        focusEstablished = false;
        root = baselineRoot;
        openPath = new int[0];
        tokenModel.reset();
        input.setText(baselineFreeText);
        pendingModelRemovals.clear(); // discard staged model-filter removals -> model stays as applied
        // Destroy the native peer, don't just hide it: on some (X)Wayland compositors an undecorated
        // heavyweight window's surface can linger after a plain hide - still grabbing mouse input at
        // its old location while painting nothing. That is what makes a "click on the collapsed bar"
        // land on the invisible overlay's model chip and reopen the filter dialog once the overlay
        // has been closed once. dispose() releases the surface (and hides the window); openAt()
        // re-realizes it via setVisible(true). The builder state lives in fields, so it survives.
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

    // --- compile & commit ---

    /**
     * The input text only counts as a free-text search segment at an entry stage; at a mid-token
     * stage it is a value/operator fragment that Enter discards (Tab commits it into a clause).
     */
    private String freeTextForCommit() {
        return tokenModel.atEntryStage() ? input.getText().trim() : "";
    }

    /**
     * Accepts a terminal token as a chip, then runs the search. Shared by Enter and the Search
     * button so both behave the same.
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
        // overlay (Cancel would instead have discarded them in close())
        pendingModelRemovals.values().forEach(Runnable::run);
        pendingModelRemovals.clear();
        String compiled = LuceneQueryCompiler.compile(root, freeText);
        writeDocument(compiled);
        lastCompiled = compiled;
        // the applied query becomes the new baseline, so the close() below keeps it (no revert)
        baselineRoot = root;
        baselineFreeText = freeText;
        filterModel.fireUpdateCompleted();
        Commit commit = new Commit(root, freeText, compiled);
        close();
        onCommitted.accept(commit);
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

    private void rebuild() {
        // removeAll() detaches the (focused) input, which drops the keyboard focus; restore it
        // after the re-layout so typing continues seamlessly after choosing a suggestion
        boolean refocus = input.isFocusOwner();
        inlineRow.removeAll();

        // render only the model filters not staged for removal - a staged removal hides the chip but
        // does not touch the model until the search is committed (Cancel discards it, see close())
        List<FilterTerm> terms = termSupplier.get().stream()
                .filter(t -> !pendingModelRemovals.containsKey(t.id()))
                .toList();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0)
                inlineRow.add(ChipComponent.implicitAndLabel()); // dialog filters always AND together
            FilterTerm term = terms.get(i);
            QueryNode node = term.toQueryNode();
            inlineRow.add(new ChipComponent(
                    QueryNodeRenderer.label(node, renderState.mode(), renderState.suffixLengthResolver()),
                    GuiUtils.formatToolTip(LuceneQueryCompiler.render(node),
                            "Filter from the filter dialog - click to edit; combined with AND"),
                    ChipComponent.Style.MODEL,
                    () -> {
                        // close the overlay first, then open the full editor (avoid nested modals)
                        close();
                        SwingUtilities.invokeLater(() -> term.openEditor(editorHost));
                    },
                    () -> {
                        // stage the removal (applied on commit, reverted on Cancel) instead of
                        // mutating the model right away
                        pendingModelRemovals.put(term.id(), term::remove);
                        rebuild();
                    }));
        }

        List<QueryNode> items = root.items();
        // the dialog filters and the user's own query are combined with AND - make that visible
        if (!terms.isEmpty() && !items.isEmpty())
            inlineRow.add(ChipComponent.implicitAndLabel());
        for (int i = 0; i < items.size(); i++) {
            if (i > 0)
                inlineRow.add(buildLogicComponent(root.logics().get(i - 1), new int[]{i}));
            inlineRow.add(buildNode(items.get(i), new int[]{i}));
        }

        // the staged token fragments and the input render inside the open group, if any
        if (openPath.length == 0)
            addStagedFragmentsAndInput(inlineRow);

        input.setPlaceholder(tokenModel.stagePrompt());
        refreshSuggestions();
        inlineRow.revalidate();
        inlineRow.repaint();
        if (isVisible())
            resizeToFit();
        if (refocus)
            SwingUtilities.invokeLater(input::requestFocusInWindow);
    }

    private void addStagedFragmentsAndInput(JPanel target) {
        for (String fragment : tokenModel.pendingFragments())
            target.add(new ChipComponent(fragment, "Being built - Backspace removes it",
                    ChipComponent.Style.USER, null, null));
        target.add(input);
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
        return new ChipComponent(logic.toString(), null, ChipComponent.Style.USER, null, null);
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
                    ? "Full-text search in the default fields"
                    : LuceneQueryCompiler.render(clause);
            return new ChipComponent(text, tooltip, ChipComponent.Style.USER,
                    null, () -> {
                root = QueryTreeOps.removeNodeById(root, clause.id());
                openPath = QueryTreeOps.resolvePath(root, openPath);
                rebuild();
            });
        }

        QueryGroup group = (QueryGroup) node;
        boolean open = Arrays.equals(path, openPath);
        JPanel groupPanel = new GroupPanel(open);

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
            groupPanel.add(new ChipComponent("NOT", null, ChipComponent.Style.USER, null, null));
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

        // tokens built while this group is open belong inside it, so fragments + input render here
        if (open)
            addStagedFragmentsAndInput(groupPanel);

        JLabel closing = parenLabel(")");
        if (open) {
            closing.setToolTipText("Close this group");
            closing.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closing.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    applyEvent(new TokenInputModel.Event.CloseGroup());
                    rebuild();
                    input.requestFocusInWindow();
                }
            });
        }
        groupPanel.add(closing);

        if (!open)
            groupPanel.add(ChipComponent.closeLabel(Colors.Menu.FILTER_BUTTON, "Remove this group with all its filters",
                    () -> {
                        root = QueryTreeOps.removeNodeById(root, group.id());
                        openPath = QueryTreeOps.resolvePath(root, openPath);
                        rebuild();
                    }));
        return groupPanel;
    }

    private static JLabel parenLabel(String paren) {
        JLabel label = new JLabel(paren);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 2));
        label.setForeground(Colors.Menu.FILTER_BUTTON);
        return label;
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

        GroupPanel(boolean open) {
            super(new FlowLayout(FlowLayout.LEFT, 3, 2));
            this.open = open;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Colors.Menu.FILTER_BUTTON);
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
