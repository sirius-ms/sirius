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
import io.sirius.ms.sdk.model.SearchableField;
import io.sirius.ms.sdk.model.SearchableFieldType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * The expanded state of the feature search bar: a popup-style window hosting the chip-based lucene
 * query builder. Opens anchored at the collapsed bar and extends over the result view, so the
 * narrow left rail does not constrain query building.
 * <p>
 * An undecorated owned dialog rather than a JPopupMenu/PopupFactory popup (those are built to be
 * non-focusable and to auto-dismiss - wrong for an editor full of text fields and combos) and
 * rather than a layered-pane overlay (manual bounds/z-order/click-outside handling). Combo
 * dropdowns are owned child windows and therefore do not count as focus loss; dismissal is
 * windowLostFocus + Esc.
 * <p>
 * The builder state (committed clause chips, the open group the cursor is in, the draft clause,
 * free text) lives here and compiles into the {@link FeatureFilterModel}'s shared search text
 * document on commit - the model itself needs no change and the filter dialog's fulltext field
 * shows the compiled query automatically.
 */
@Slf4j
public class SearchBarOverlay extends JDialog {

    private static final int MAX_WIDTH = 900;
    private static final int MIN_WIDTH = 500;
    private static final int MAX_HEIGHT = 420;

    private static final EnumSet<SearchableFieldType> NUMERIC_TYPES = EnumSet.of(
            SearchableFieldType.INTEGER, SearchableFieldType.LONG, SearchableFieldType.DOUBLE,
            SearchableFieldType.FLOAT, SearchableFieldType.DATE, SearchableFieldType.TIME);

    private final FeatureFilterModel filterModel;
    private final SearchableFieldsProvider fieldsProvider;
    private final Supplier<List<ModelChip>> modelChipSupplier;
    private final Runnable openFilterDialog;
    private final Runnable onCommitted;

    // --- builder state ---
    private QueryContainer root = QueryContainer.empty();
    private int[] openPath = new int[0];
    private LogicOp nextLogic = LogicOp.AND;
    @Nullable
    private Draft draft;
    /**
     * The query string this overlay last wrote into the shared search document. If the document
     * differs on open, it was edited elsewhere (filter dialog) - the builder then degrades the
     * document content into its free-text segment instead of trying to parse it back into chips.
     */
    private String lastCompiled = "";

    // --- ui ---
    private final JPanel chipsPanel;
    private final PlaceholderTextField freeText;
    private final JButton hintButton;
    private final JButton searchButton;
    @Nullable
    private Completion currentCompletion;

    public SearchBarOverlay(@NotNull Window owner, @NotNull FeatureFilterModel filterModel,
                            @NotNull SearchableFieldsProvider fieldsProvider,
                            @NotNull Supplier<List<ModelChip>> modelChipSupplier,
                            @NotNull Runnable openFilterDialog,
                            @NotNull Runnable onCommitted) {
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
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        setContentPane(content);

        chipsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 4));
        JScrollPane chipsScroll = new JScrollPane(chipsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chipsScroll.setBorder(BorderFactory.createEmptyBorder());
        chipsScroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(chipsScroll, BorderLayout.CENTER);

        // --- bottom row: add-filter | free text | hint | copy | clear | search ---
        Box bottom = Box.createHorizontalBox();

        JButton addFilter = new JButton("Add Filter ▾");
        addFilter.setToolTipText("Add a field filter or a group to the query");
        addFilter.addActionListener(e -> buildAddFilterMenu().show(addFilter, 0, addFilter.getHeight()));
        bottom.add(addFilter);
        bottom.add(Box.createHorizontalStrut(6));

        freeText = new PlaceholderTextField();
        freeText.setPlaceholder("Search or type a field name and hit Tab...");
        freeText.setToolTipText(GuiUtils.formatToolTip(
                "Free text is searched in the default search fields (name, adducts, formula, structure, ...).",
                "Type a field name (e.g. 'ionMass', 'quality') and hit Tab to add a field filter.",
                "'not <field>' negates, 'and'/'or' choose how it joins, '(' opens a group, ')' closes it.",
                "Enter starts the search."));
        freeText.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateCompletionHint();
            }

            public void removeUpdate(DocumentEvent e) {
                updateCompletionHint();
            }

            public void changedUpdate(DocumentEvent e) {
                updateCompletionHint();
            }
        });
        // Tab applies the completion when one is on offer; otherwise it keeps traversing focus
        freeText.setFocusTraversalKeysEnabled(false);
        freeText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (currentCompletion != null) {
                        applyCompletion();
                    } else {
                        if (e.isShiftDown()) freeText.transferFocusBackward();
                        else freeText.transferFocus();
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && draft == null) {
                    commitSearch();
                }
            }
        });
        bottom.add(freeText);
        bottom.add(Box.createHorizontalStrut(4));

        hintButton = new JButton();
        hintButton.setVisible(false);
        hintButton.setFocusable(false);
        hintButton.addActionListener(e -> applyCompletion());
        bottom.add(hintButton);

        JButton copy = new JButton("⧉");
        copy.setFocusable(false);
        copy.setToolTipText("Copy the compiled search query to the clipboard");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(compileQuery()), null));
        bottom.add(copy);

        JButton clear = new JButton("Clear");
        clear.setFocusable(false);
        clear.setToolTipText("Clear the whole search query (filters set in the filter dialog are kept)");
        clear.addActionListener(e -> {
            root = QueryContainer.empty();
            openPath = new int[0];
            draft = null;
            freeText.setText("");
            rebuild();
        });
        bottom.add(clear);
        bottom.add(Box.createHorizontalStrut(4));

        searchButton = new JButton("Search");
        searchButton.setToolTipText("Apply the query and filter the feature list (Enter)");
        searchButton.addActionListener(e -> commitSearch());
        bottom.add(searchButton);

        bottom.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        content.add(bottom, BorderLayout.SOUTH);

        // Esc hides the overlay; the builder state is kept for the next open
        getRootPane().registerKeyboardAction(e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // focus moving to a window not owned by this overlay (combo popups are owned) closes it
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
            draft = null;
            freeText.setText(docText);
            lastCompiled = docText;
        }

        // tags are dynamic - refresh the searchable fields in the background while the user types
        Jobs.runInBackground(fieldsProvider::refreshIfStale);

        rebuild();

        Point anchorOnScreen = anchor.getLocationOnScreen();
        Window owner = getOwner();
        int available = owner.getX() + owner.getWidth() - anchorOnScreen.x - 20;
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, available));
        setSize(width, 10); // height fixed by resizeToFit
        resizeToFit();
        setLocation(anchorOnScreen.x, anchorOnScreen.y + anchor.getHeight());
        setVisible(true);
        freeText.requestFocusInWindow();
    }

    private void resizeToFit() {
        int height = Math.min(Math.max(getPreferredSize().height, 90), MAX_HEIGHT);
        setSize(getWidth(), height);
        validate();
    }

    // --- compile & commit ---

    private String compileQuery() {
        return LuceneQueryCompiler.compile(root, freeText.getText());
    }

    private void commitSearch() {
        if (draft != null)
            return; // finish or discard the draft first
        String compiled = compileQuery();
        writeDocument(compiled);
        lastCompiled = compiled;
        filterModel.fireUpdateCompleted();
        setVisible(false);
        onCommitted.run();
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

    // --- completion ---

    private void updateCompletionHint() {
        if (draft != null) { // Tab belongs to the draft's own controls while one is open
            currentCompletion = null;
        } else {
            currentCompletion = CompletionParser.parse(freeText.getText(), fieldsProvider.getCached(),
                    !QueryTreeOps.containerAt(root, openPath).isEmpty(), openPath.length > 0).orElse(null);
        }

        if (currentCompletion == null) {
            hintButton.setVisible(false);
        } else {
            hintButton.setText(hintText(currentCompletion) + " ⇥");
            hintButton.setToolTipText("Press Tab to apply");
            hintButton.setVisible(true);
        }
        hintButton.getParent().revalidate();
        validateFreeText();
    }

    /**
     * Advisory live validation: syntax problems and unknown fields show as a warning outline
     * (FlatLaf) with the explanation as tooltip; the search still runs either way. No warning
     * while a completion is offered - a half-typed field name is not a mistake yet.
     */
    private void validateFreeText() {
        String problem = currentCompletion != null ? null
                : QueryValidator.validate(freeText.getText(), fieldsProvider.getCached()).orElse(null);
        freeText.putClientProperty("JComponent.outline", problem == null ? null : "warning");
        if (problem != null)
            freeText.setToolTipText(GuiUtils.formatToolTip(problem));
        else
            freeText.setToolTipText(null);
    }

    private static String hintText(Completion completion) {
        if (completion instanceof Completion.CloseGroup)
            return ") close group";
        if (completion instanceof Completion.OpenGroup group) {
            return (group.logic() != null ? group.logic() + " " : "")
                    + (group.groupNegated() ? "NOT " : "") + "( "
                    + (group.clause() != null ? clauseHint(group.clause()) : "group");
        }
        return clauseHint((Completion.ClauseStart) completion);
    }

    private static String clauseHint(Completion.ClauseStart clause) {
        return (clause.logic() != null ? clause.logic() + " " : "")
                + (clause.negated() ? "NOT " : "") + clause.field().getName();
    }

    private void applyCompletion() {
        Completion completion = currentCompletion;
        if (completion == null)
            return;
        freeText.setText("");

        if (completion instanceof Completion.CloseGroup) {
            doCloseGroup();
        } else if (completion instanceof Completion.OpenGroup group) {
            // the connector joins the group itself and must not leak into the next clause
            doOpenGroup(group.groupNegated(), group.logic() != null ? group.logic() : nextLogic);
            if (group.clause() != null)
                beginDraft(group.clause().field(), group.clause().negated());
        } else if (completion instanceof Completion.ClauseStart clause) {
            if (clause.logic() != null)
                nextLogic = clause.logic();
            beginDraft(clause.field(), clause.negated());
        }
        rebuild();
    }

    // --- tree mutations ---

    private void doOpenGroup(boolean negated, LogicOp logic) {
        QueryTreeOps.PathResult result = QueryTreeOps.openGroup(root, openPath, negated, logic);
        root = result.root();
        openPath = result.path();
    }

    private void doCloseGroup() {
        QueryTreeOps.PathResult result = QueryTreeOps.closeGroup(root, openPath);
        root = result.root();
        openPath = result.path();
    }

    private void removeNode(String nodeId) {
        root = QueryTreeOps.removeNodeById(root, nodeId);
        openPath = QueryTreeOps.resolvePath(root, openPath);
        rebuild();
    }

    private void beginDraft(SearchableField field, boolean negated) {
        draft = new Draft(field, negated);
        rebuild();
        SwingUtilities.invokeLater(draft::focusValue);
    }

    private void commitDraft() {
        if (draft == null)
            return;
        QueryClause clause = draft.toClause();
        if (clause == null)
            return; // no value yet
        root = QueryTreeOps.append(root, openPath, clause, nextLogic);
        draft = null;
        rebuild();
        freeText.requestFocusInWindow();
    }

    private void cancelDraft() {
        draft = null;
        rebuild();
        freeText.requestFocusInWindow();
    }

    // --- rendering ---

    private void rebuild() {
        chipsPanel.removeAll();

        for (ModelChip chip : modelChipSupplier.get())
            chipsPanel.add(new ChipComponent(chip.label(), chip.tooltip() == null
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
                chipsPanel.add(buildLogicComponent(root.logics().get(i - 1), new int[]{i}));
            chipsPanel.add(buildNode(items.get(i), new int[]{i}));
        }
        if (openPath.length == 0 && draft != null)
            chipsPanel.add(draft.panel);

        if (chipsPanel.getComponentCount() == 0) {
            JLabel empty = new JLabel("No filters yet - type a field name below or use 'Add Filter'");
            empty.setEnabled(false);
            chipsPanel.add(empty);
        }

        updateCompletionHint();
        chipsPanel.revalidate();
        chipsPanel.repaint();
        if (isVisible())
            resizeToFit();
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
                    null, () -> removeNode(clause.id()));
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

        // clauses typed while this group is open belong inside it, so the draft renders here
        if (open && draft != null)
            groupPanel.add(draft.panel);

        JLabel closing = parenLabel(")");
        if (open) {
            closing.setToolTipText("Close this group");
            closing.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closing.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    doCloseGroup();
                    rebuild();
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
                    removeNode(group.id());
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
     * text-like clauses read as {@code : value}.
     */
    private static String clauseBody(QueryClause clause) {
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

    // --- add filter menu ---

    private JPopupMenu buildAddFilterMenu() {
        JPopupMenu menu = new JPopupMenu();
        List<SearchableField> fields = fieldsProvider.getCached().stream()
                .sorted(Comparator.comparing(SearchableField::getName)).toList();

        if (fields.isEmpty()) {
            JMenuItem loading = new JMenuItem("Loading searchable fields...");
            loading.setEnabled(false);
            menu.add(loading);
        }

        // nested fields (topAnnotations.*, tags.*, ...) group into submenus by their first segment
        Map<String, JMenu> submenus = new LinkedHashMap<>();
        for (SearchableField field : fields) {
            JMenuItem item = new JMenuItem(field.getName());
            if (field.getDescription() != null)
                item.setToolTipText(GuiUtils.formatToolTip(field.getDescription()));
            item.addActionListener(e -> beginDraft(field, false));

            int dot = field.getName().indexOf('.');
            if (dot > 0) {
                String prefix = field.getName().substring(0, dot);
                submenus.computeIfAbsent(prefix, p -> {
                    JMenu sub = new JMenu(p);
                    menu.add(sub);
                    return sub;
                }).add(item);
            } else {
                menu.add(item);
            }
        }

        menu.addSeparator();
        JMenuItem openGroup = new JMenuItem("Open group (");
        openGroup.setToolTipText("Group filters with parentheses, e.g. a AND (b OR c)");
        openGroup.addActionListener(e -> {
            doOpenGroup(false, nextLogic);
            rebuild();
            freeText.requestFocusInWindow();
        });
        menu.add(openGroup);
        if (openPath.length > 0) {
            JMenuItem closeGroup = new JMenuItem("Close group )");
            closeGroup.addActionListener(e -> {
                doCloseGroup();
                rebuild();
                freeText.requestFocusInWindow();
            });
            menu.add(closeGroup);
        }
        return menu;
    }

    // --- draft clause editor ---

    /**
     * The clause being built: connector (2nd+ clause), NOT toggle, field, operator (numeric fields)
     * and value controls - enum/boolean values as combo, text with wildcard support, numeric and
     * date/time as plain text validated by the server.
     */
    private final class Draft {
        private final SearchableField field;
        private final JPanel panel;
        private final JToggleButton notButton;
        @Nullable
        private final JComboBox<NumberOp> opBox;
        @Nullable
        private final JComboBox<String> valueCombo;
        @Nullable
        private final JTextField value1;
        @Nullable
        private JTextField value2;
        @Nullable
        private JLabel toLabel;

        Draft(SearchableField field, boolean negated) {
            this.field = field;
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
            panel.setOpaque(false);
            panel.setBorder(BorderFactory.createDashedBorder(Colors.Menu.FILTER_BUTTON, 4, 2));

            if (!QueryTreeOps.containerAt(root, openPath).isEmpty()) {
                JComboBox<LogicOp> connector = new JComboBox<>(LogicOp.values());
                connector.setToolTipText("How this filter joins the expression before it");
                connector.setSelectedItem(nextLogic);
                connector.addActionListener(e -> nextLogic = (LogicOp) connector.getSelectedItem());
                panel.add(connector);
            }

            notButton = new JToggleButton("NOT", negated);
            notButton.setToolTipText("Negate this filter");
            panel.add(notButton);

            JLabel fieldLabel = new JLabel(field.getName());
            fieldLabel.setFont(fieldLabel.getFont().deriveFont(Font.BOLD));
            if (field.getDescription() != null)
                fieldLabel.setToolTipText(GuiUtils.formatToolTip(field.getDescription()));
            panel.add(fieldLabel);

            List<String> suggestions = CompletionParser.valueSuggestions(field);
            if (NUMERIC_TYPES.contains(field.getFieldType())) {
                opBox = new JComboBox<>(NumberOp.values());
                opBox.setToolTipText("How the value is compared - [ ] includes the bounds, { } excludes them");
                opBox.addActionListener(e -> updateRangeVisibility());
                panel.add(opBox);
                valueCombo = null;

                value1 = makeValueField(valuePlaceholder(true));
                panel.add(value1);
                value2 = makeValueField(valuePlaceholder(false));
                toLabel = new JLabel("TO");
                panel.add(toLabel);
                panel.add(value2);
            } else if (!suggestions.isEmpty()) {
                opBox = null;
                value1 = null;
                valueCombo = new JComboBox<>(suggestions.toArray(String[]::new));
                panel.add(valueCombo);
            } else {
                opBox = null;
                valueCombo = null;
                PlaceholderTextField text = makeValueField("value (*, ?, ~ wildcards)");
                value1 = text;
                panel.add(text);
            }

            JButton add = new JButton("+");
            add.setToolTipText("Add this filter to the query");
            add.addActionListener(e -> commitDraft());
            panel.add(add);

            JButton cancel = new JButton("✕");
            cancel.setToolTipText("Discard this filter");
            cancel.addActionListener(e -> cancelDraft());
            panel.add(cancel);
        }

        private PlaceholderTextField makeValueField(String placeholder) {
            PlaceholderTextField valueField = new PlaceholderTextField(9);
            valueField.setPlaceholder(placeholder);
            valueField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        commitDraft();
                    else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                        cancelDraft();
                }
            });
            return valueField;
        }

        private String valuePlaceholder(boolean lower) {
            return switch (field.getFieldType()) {
                case DATE -> "yyyy-MM-dd";
                case TIME -> "HH:mm:ss";
                default -> lower ? "min" : "max";
            };
        }

        private void updateRangeVisibility() {
            if (opBox == null || value2 == null || toLabel == null)
                return;
            boolean range = ((NumberOp) opBox.getSelectedItem()).isRange();
            value2.setVisible(range);
            toLabel.setVisible(range);
            panel.revalidate();
            panel.repaint();
            resizeToFit();
        }

        void focusValue() {
            if (valueCombo != null)
                valueCombo.requestFocusInWindow();
            else if (value1 != null)
                value1.requestFocusInWindow();
        }

        /**
         * The committed clause, or null while no value is set.
         */
        @Nullable
        QueryClause toClause() {
            boolean negated = notButton.isSelected();
            if (valueCombo != null)
                return QueryClause.text(field.getName(), (String) valueCombo.getSelectedItem(), negated);

            String v1 = value1 == null ? "" : value1.getText().trim();
            if (opBox != null) {
                NumberOp op = (NumberOp) opBox.getSelectedItem();
                String v2 = op.isRange() && value2 != null ? value2.getText().trim() : null;
                if (v1.isEmpty() && (v2 == null || v2.isEmpty()))
                    return null;
                return QueryClause.numeric(field.getName(), op, v1, v2, negated);
            }
            if (v1.isEmpty())
                return null;
            return QueryClause.text(field.getName(), v1, negated);
        }
    }
}
