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
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/**
 * The expanded state of the feature search bar: a popup-style window hosting the inline lucene
 * query builder, modeled after GitLab's filtered search. ONE wrapping line holds the filter-dialog
 * state as outlined chips, the user's committed clause chips (groups as nested paren chips), the
 * staged fragments of the token being built, and the inline text input. A suggestion dropdown under
 * the input lists all candidates for the current stage ({@link TokenInputModel}) and narrows while
 * typing; Up/Down navigate, Tab (or Enter after navigating) chooses, Backspace on empty input pops
 * a stage, Enter searches.
 * <p>
 * An undecorated owned dialog rather than a JPopupMenu/PopupFactory popup (those are built to be
 * non-focusable and to auto-dismiss - wrong for an editor) and rather than a layered-pane overlay.
 * <p>
 * The builder state compiles into the {@link FeatureFilterModel}'s shared search text document on
 * commit - the model itself needs no change and the filter dialog's fulltext field shows the
 * compiled query automatically. Text typed into the inline input that resolves to no token is the
 * free-text segment of the search.
 */
@Slf4j
public class SearchBarOverlay extends JDialog {

    private static final int MAX_WIDTH = 900;
    private static final int MIN_WIDTH = 500;
    private static final int MAX_HEIGHT = 420;

    /**
     * What a commit produced - the collapsed bar renders its chips from this snapshot (never by
     * parsing the compiled string).
     */
    public record Commit(@NotNull QueryContainer root, @NotNull String freeText, @NotNull String compiled) {
    }

    private final FeatureFilterModel filterModel;
    private final SearchableFieldsProvider fieldsProvider;
    private final Supplier<List<ModelChip>> modelChipSupplier;
    private final Runnable openFilterDialog;
    private final java.util.function.Consumer<Commit> onCommitted;

    // --- builder state ---
    private QueryContainer root = QueryContainer.empty();
    private int[] openPath = new int[0];
    private final TokenInputModel tokenModel = new TokenInputModel();
    /**
     * The query string this overlay last wrote into the shared search document. If the document
     * differs on open, it was edited elsewhere (filter dialog) - the builder then degrades the
     * document content into its free-text segment instead of trying to parse it back into chips.
     */
    private String lastCompiled = "";

    // --- ui ---
    private final JPanel inlineRow;
    private final PlaceholderTextField input;
    private final SuggestionPopup suggestionPopup;
    /**
     * Enter only picks the dropdown selection at IDLE after the user navigated with the arrow
     * keys (GitLab behavior) - otherwise Enter submits the typed text / runs the search.
     */
    private boolean listEngaged = false;

    public SearchBarOverlay(@NotNull Window owner, @NotNull FeatureFilterModel filterModel,
                            @NotNull SearchableFieldsProvider fieldsProvider,
                            @NotNull Supplier<List<ModelChip>> modelChipSupplier,
                            @NotNull Runnable openFilterDialog,
                            @NotNull java.util.function.Consumer<Commit> onCommitted) {
        super(owner);
        this.filterModel = filterModel;
        this.fieldsProvider = fieldsProvider;
        this.modelChipSupplier = modelChipSupplier;
        this.openFilterDialog = openFilterDialog;
        this.onCommitted = onCommitted;

        setUndecorated(true);
        setModalityType(ModalityType.MODELESS);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.Menu.FILTER_BUTTON, 1),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        setContentPane(content);

        // --- the one inline row: chips + staged fragments + input, wrapping when long ---
        inlineRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 4));
        JScrollPane rowScroll = new JScrollPane(inlineRow,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rowScroll.setBorder(BorderFactory.createEmptyBorder());
        rowScroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(rowScroll, BorderLayout.CENTER);

        input = new PlaceholderTextField(18);
        input.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        input.setOpaque(false);
        suggestionPopup = new SuggestionPopup(input, this::applySuggestion);
        wireInput();

        // --- trailing controls ---
        Box controls = Box.createHorizontalBox();
        JButton copy = new JButton("⧉");
        copy.setFocusable(false);
        copy.setToolTipText("Copy the compiled search query to the clipboard");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(compileQuery()), null));
        controls.add(copy);

        JButton clear = new JButton("Clear");
        clear.setFocusable(false);
        clear.setToolTipText("Clear the whole search query (filters set in the filter dialog are kept)");
        clear.addActionListener(e -> {
            root = QueryContainer.empty();
            openPath = new int[0];
            tokenModel.reset();
            input.setText("");
            rebuild();
            input.requestFocusInWindow();
        });
        controls.add(clear);
        controls.add(Box.createHorizontalStrut(4));

        JButton search = new JButton("Search");
        search.setToolTipText("Apply the query and filter the feature list (Enter)");
        search.addActionListener(e -> commitSearch());
        controls.add(search);

        JPanel controlsAligned = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        controlsAligned.add(controls);
        content.add(controlsAligned, BorderLayout.EAST);

        // Esc: first closes the dropdown, then the overlay (handled in the input's key listener);
        // this binding covers Esc while focus is on a button
        getRootPane().registerKeyboardAction(e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // focus moving to a window not owned by this overlay closes it (covers other applications;
        // in-app clicks are covered by the global mouse listener of the dismissal handling)
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                for (Window w = e.getOppositeWindow(); w != null; w = w.getOwner())
                    if (w == SearchBarOverlay.this)
                        return;
                setVisible(false);
            }
        });

        // moving/resizing the main window invalidates the anchor position - just hide
        owner.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                setVisible(false);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                setVisible(false);
            }
        });

        // structured filters may change while the overlay is open (dialog, quick toggles)
        filterModel.addUpdateCompleteListener(evt -> {
            if (isVisible())
                rebuild();
        });

        // In-app clicks outside the overlay close it. windowLostFocus alone is not enough: clicking
        // many components of the main frame does not transfer X window focus. The AWTEventListener
        // sees every press in this application before dispatch; presses inside this overlay or any
        // window it owns (suggestion dropdown, combo popups) are ignored. Registered only while
        // visible so the global listener does not linger.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
                suggestionPopup.hide(); // hiding the overlay must always take the dropdown with it
            }
        });
    }

    private final AWTEventListener outsideClickListener = event -> {
        if (!(event instanceof MouseEvent mouse) || mouse.getID() != MouseEvent.MOUSE_PRESSED)
            return;
        if (!(mouse.getComponent() instanceof Component component))
            return;
        for (Window w = SwingUtilities.getWindowAncestor(component); w != null; w = w.getOwner())
            if (w == this)
                return;
        setVisible(false);
    };

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

        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                refreshSuggestions();
            }
        });

        input.setFocusTraversalKeysEnabled(false);
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> {
                        suggestionPopup.moveSelection(1);
                        listEngaged = true;
                        e.consume();
                    }
                    case KeyEvent.VK_UP -> {
                        suggestionPopup.moveSelection(-1);
                        listEngaged = true;
                        e.consume();
                    }
                    case KeyEvent.VK_TAB -> {
                        // Tab is the explicit "complete" key, like the old grammar hint
                        if (suggestionPopup.chooseSelected())
                            e.consume();
                    }
                    case KeyEvent.VK_ENTER -> {
                        onEnter();
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
                        if (suggestionPopup.isVisible())
                            suggestionPopup.hide();
                        else
                            setVisible(false);
                        e.consume();
                    }
                }
            }
        });
    }

    private void onTyped() {
        listEngaged = false;
        refreshSuggestions();
    }

    /**
     * Enter: mid-token stages take the dropdown selection (or the typed text as value); at IDLE it
     * takes the selection only after arrow navigation, otherwise the typed text is applied as
     * grammar input ({@code or not ion}) and, failing that, the search runs with it as free text.
     */
    private void onEnter() {
        boolean wasIdle = tokenModel.stage() == TokenInputModel.Stage.IDLE;
        if (suggestionPopup.isVisible() && (listEngaged || !wasIdle)) {
            if (suggestionPopup.chooseSelected())
                return;
        }
        Optional<TokenInputModel.Event> event = tokenModel.submitTyped(input.getText());
        event.ifPresent(this::applyEvent);
        // typed text at IDLE that neither produced an event nor advanced a stage is free text -
        // Enter runs the search with it (also covers the plain empty-input Enter)
        if (wasIdle && event.isEmpty() && tokenModel.stage() == TokenInputModel.Stage.IDLE) {
            commitSearch();
            return;
        }
        input.setText("");
        rebuild();
    }

    private void applySuggestion(TokenInputModel.Suggestion suggestion) {
        tokenModel.choose(suggestion).ifPresent(this::applyEvent);
        listEngaged = false;
        input.setText("");
        rebuild();
        input.requestFocusInWindow();
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

    private void refreshSuggestions() {
        tokenModel.updateContext(fieldsProvider.getCached(),
                !QueryTreeOps.containerAt(root, openPath).isEmpty(), openPath.length > 0);
        if (isVisible() && input.isFocusOwner())
            suggestionPopup.showSuggestions(tokenModel.suggestions(input.getText()));
        validateInput();
    }

    /**
     * Advisory live validation of the free-text segment: syntax problems and unknown fields show
     * as a warning outline with the explanation as tooltip. Suppressed while the dropdown offers
     * something - a half-typed field name is not a mistake yet.
     */
    private void validateInput() {
        String problem = tokenModel.stage() == TokenInputModel.Stage.IDLE && !suggestionPopup.isVisible()
                ? QueryValidator.validate(input.getText(), fieldsProvider.getCached()).orElse(null)
                : null;
        input.putClientProperty("JComponent.outline", problem == null ? null : "warning");
        input.setToolTipText(problem == null ? null : GuiUtils.formatToolTip(problem));
    }

    // --- opening / closing ---

    /**
     * Opens the overlay anchored at the collapsed bar, spanning to the right over the result view.
     */
    public void openAt(@NotNull Component anchor) {
        // the shared document was edited elsewhere -> degrade its content into the free text
        String docText = Optional.ofNullable(filterModel.getSearchText()).orElse("");
        if (!docText.equals(lastCompiled)) {
            root = QueryContainer.empty();
            openPath = new int[0];
            tokenModel.reset();
            input.setText(docText);
            lastCompiled = docText;
        }

        // tags are dynamic - refresh the searchable fields in the background while the user types;
        // the dropdown updates once they arrive
        Jobs.runInBackground(() -> {
            fieldsProvider.refreshIfStale();
            SwingUtilities.invokeLater(this::refreshSuggestions);
        });

        rebuild();

        Point anchorOnScreen = anchor.getLocationOnScreen();
        Window owner = getOwner();
        int available = owner.getX() + owner.getWidth() - anchorOnScreen.x - 20;
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, available));
        setSize(width, 10); // height set by resizeToFit
        resizeToFit();
        setLocation(anchorOnScreen.x, anchorOnScreen.y);
        setVisible(true);
        input.requestFocusInWindow();
    }

    private void resizeToFit() {
        int height = Math.min(Math.max(getPreferredSize().height, 40), MAX_HEIGHT);
        setSize(getWidth(), height);
        validate();
        suggestionPopup.relocate();
    }

    // --- compile & commit ---

    private String compileQuery() {
        return LuceneQueryCompiler.compile(root, input.getText());
    }

    private void commitSearch() {
        tokenModel.reset(); // a half-built token is not part of the query
        String compiled = compileQuery();
        writeDocument(compiled);
        lastCompiled = compiled;
        filterModel.fireUpdateCompleted();
        setVisible(false);
        onCommitted.accept(new Commit(root, input.getText().trim(), compiled));
    }

    /**
     * Appends text to the inline input - used by the collapsed bar to forward the keystroke that
     * opened the overlay, so typing into the collapsed field "just continues" here.
     */
    public void typeAhead(@NotNull String text) {
        input.setText(input.getText() + text);
        input.setCaretPosition(input.getText().length());
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
        inlineRow.removeAll();

        for (ModelChip chip : modelChipSupplier.get())
            inlineRow.add(new ChipComponent(chip.label(), chip.tooltip() == null
                    ? "Filter from the filter dialog - click to open it"
                    : chip.tooltip() + " (filter dialog)", ChipComponent.Style.MODEL,
                    () -> {
                        setVisible(false);
                        openFilterDialog.run();
                    },
                    () -> {
                        chip.onRemove().run();
                        filterModel.fireUpdateCompleted();
                    }));

        List<QueryNode> items = root.items();
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
            String text = (clause.negated() ? "NOT " : "") + clause.field() + " " + clauseBody(clause);
            return new ChipComponent(text, LuceneQueryCompiler.render(clause), ChipComponent.Style.USER,
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

        if (!open) {
            JLabel remove = new JLabel("✕");
            remove.setToolTipText("Remove this group with all its filters");
            remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            remove.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    root = QueryTreeOps.removeNodeById(root, group.id());
                    openPath = QueryTreeOps.resolvePath(root, openPath);
                    rebuild();
                }
            });
            groupPanel.add(remove);
        }
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
}
