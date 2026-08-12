package de.unijena.bioinf.ms.gui.dialogs.filter;
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.DBSelectionList;
import de.unijena.bioinf.ms.gui.dialogs.ElementSelectionDialog;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.DbFilter;
import de.unijena.bioinf.ms.gui.utils.filter.ElementFilter;
import de.unijena.bioinf.ms.gui.utils.filter.QualityFilter;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.gui.utils.search.FilterEditorHost;
import de.unijena.bioinf.ms.gui.utils.search.FilterTerm;
import de.unijena.bioinf.ms.gui.utils.search.PanelFilterTerms;
import de.unijena.bioinf.ms.gui.utils.search.QueryEditorPanel;
import de.unijena.bioinf.ms.gui.utils.search.SearchRenderState;
import de.unijena.bioinf.ms.gui.utils.search.SearchableFieldsProvider;
import io.sirius.ms.sdk.model.SearchableDatabase;
import lombok.extern.slf4j.Slf4j;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

import static de.unijena.bioinf.ms.gui.utils.GuiUtils.MEDIUM_GAP;

/**
 * Dialog allows to adjust filter criteria of the {@link FeatureFilterModel} which is used to filter compound list.
 */
@Slf4j
public class FeatureFilterOptionsDialog extends JDialog implements ActionListener {

    /**
     * The embedded query editor: renders the full filter configuration (all tabs' widget state) plus
     * the user's own query as chips, live-updating as the widgets change, and lets the user type/edit
     * the query with autocomplete. Replaces the former plain "Fulltext search" field. See
     * {@link QueryEditorPanel}.
     */
    QueryEditorPanel queryEditor;
    final JSpinner minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner, minConfidenceSpinner, maxConfidenceSpinner, candidateSpinner;
    public final JCheckboxListPanel<PrecursorIonType> adductOptions;
    JButton discard, apply, reset;
    final JCheckBox invertFilter, deleteSelection, /*elementsMatchFormula, elementsMatchPrecursorFormula,*/ hasMs1, hasMsMs;

    final JCheckBox blankFilter;
    final JSpinner blankSpinner;

    final FeatureFilterModel filterModel;
    final CompoundList compoundList;


    final JComboBox<FeatureFilterModel.LipidFilter> lipidFilterBox;
    final PlaceholderTextField elementsField;

    final JCheckboxListPanel<SearchableDatabase> searchDBList;

    private final QualityFilterPanel overallQualityPanel;
    private final List<QualityFilterPanel> qualityPanels;

    private JTabbedPane centerTab;

    final SiriusGui gui;

    public FeatureFilterOptionsDialog(SiriusGui gui, FeatureFilterModel filterModel, CompoundList compoundList) {
        this(gui, filterModel, compoundList, null);
    }

    /**
     * @param selectFacetId if non-null, the tab owning that filter facet (see {@link #tabTitleForFacet})
     *                      is preselected - used when a search-bar chip is clicked to jump to its control.
     */
    public FeatureFilterOptionsDialog(SiriusGui gui, FeatureFilterModel filterModel, CompoundList compoundList,
                                      @Nullable String selectFacetId) {
        super(gui.getMainFrame(), "Filter configuration", true);
        this.gui = gui;
        this.filterModel = filterModel;
        this.compoundList = compoundList;
        setPreferredSize(GuiUtils.getPreferredSizeLimitedByScreenSize(new Dimension(700, 800)));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true); // the embedded query editor's chip area benefits from extra room

        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, MEDIUM_GAP, 0, MEDIUM_GAP));
        add(optionsPanel, BorderLayout.CENTER);

        centerTab = new JTabbedPane();
        optionsPanel.add(centerTab, BorderLayout.NORTH);

        // search query + live view of the full filter configuration as chips (replaces the plain
        // fulltext field): the tab facets render as read-only chips (click jumps to their tab), the
        // user's own query is typed/edited here with autocomplete, and both are applied together on Apply.
        {
            Box searchPanel = Box.createVerticalBox();
            JXTitledSeparator sep = new JXTitledSeparator("Search query");
            sep.setToolTipText("Visualizes the final filter query that will be executed when applying the filter configuration.");
            searchPanel.add(sep);
            searchPanel.add(Box.createVerticalStrut(3));
            queryEditor = buildQueryEditor();
            searchPanel.add(queryEditor);
            searchPanel.add(Box.createVerticalStrut(10));
            optionsPanel.add(searchPanel, BorderLayout.CENTER);
        }

        // filter modifiers
        {
            invertFilter = new JCheckBox("Invert Filter");
            invertFilter.setSelected(filterModel.isInverted());
            invertFilter.setToolTipText(GuiUtils.formatAndStripToolTip("Invert the full filter configuration: all matching features become non-matching and vice versa."));

            deleteSelection = new JCheckBox("<html>Delete all <b>non-</b>matching features</html>");
            deleteSelection.setSelected(false);
            deleteSelection.setToolTipText(GuiUtils.formatAndStripToolTip("Delete all non-matching features, reducing the project to the matching ones. Deleted features cannot be recovered."));

            final Box group = Box.createHorizontalBox();
            group.add(invertFilter);
            group.add(Box.createHorizontalStrut(25));
            group.add(deleteSelection);
            group.add(Box.createHorizontalGlue());
            optionsPanel.add(group, BorderLayout.SOUTH);
        }

        //input data filters
        {
            final Box inputParameters = Box.createVerticalBox();
            centerTab.addTab("Input", inputParameters);

            inputParameters.add(Box.createVerticalStrut(3));
            inputParameters.add(new JXTitledSeparator("Thresholds"));

            {
                TwoColumnPanel min = new TwoColumnPanel();
                TwoColumnPanel max = new TwoColumnPanel();
                Box box = Box.createHorizontalBox();

                box.add(min);
                box.add(Box.createHorizontalGlue());
                box.add(max);

                minMzSpinner = makeSpinner(filterModel.getCurrentMinMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
                maxMzSpinner = makeSpinner(filterModel.getCurrentMaxMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
                ((JSpinner.DefaultEditor) maxMzSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxMzSpinner.getModel(), filterModel.getMaxMz()));
                min.addNamed("Min m/z", minMzSpinner);
                max.addNamed("Max m/z", maxMzSpinner);
                ensureCompatibleBounds(minMzSpinner, maxMzSpinner);

                minRtSpinner = makeSpinner(filterModel.getCurrentMinRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
                maxRtSpinner = makeSpinner(filterModel.getCurrentMaxRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
                ((JSpinner.DefaultEditor) maxRtSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxRtSpinner.getModel(), filterModel.getMaxRt()));

                min.addNamed("Min RT (in sec)", minRtSpinner);
                max.addNamed("Max RT (in sec)", maxRtSpinner);
                ensureCompatibleBounds(minRtSpinner, maxRtSpinner);

                inputParameters.add(box);
            }

            // Adduct filter
            {
                inputParameters.add(Box.createVerticalStrut(5));
                adductOptions = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Adducts", GuiUtils.formatToolTip("Select adducts to filter by. Selecting all or none means any adduct can pass."));
                adductOptions.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>(PrecursorIonType.fromString("[M + H20 + Na]+"), false));
                adductOptions.checkBoxList.setVisibleRowCount(5);

                List<PrecursorIonType> ionizations = new ArrayList<>(filterModel.getPossibleAdducts());
                Collections.sort(ionizations);

                adductOptions.checkBoxList.replaceElements(ionizations);
                adductOptions.checkBoxList.uncheckAll();
                adductOptions.setEnabled(true);

                adductOptions.checkBoxList.checkAll(filterModel.getSelectedAdducts());
                inputParameters.add(adductOptions);
            }
            inputParameters.add(Box.createVerticalGlue());
        }

        {

            // fold change / blank subtraction filter

            final TwoColumnPanel foldParameters = new TwoColumnPanel();
            centerTab.addTab("Fold Change", foldParameters);

            foldParameters.add(Box.createVerticalStrut(6));
            foldParameters.add(new JXTitledSeparator("Minimum Fold Change Filters"));

            blankSpinner = makeSpinner(filterModel.getSampleBlankFoldChange().getCurrentMinFoldChange(), 0.1, Double.POSITIVE_INFINITY, 0.1);

            blankFilter = new JCheckBox("Filter Blanks");
            blankFilter.setSelected(filterModel.getSampleBlankFoldChange().isEnabled());
            blankFilter.setToolTipText("<html>Aligned feature must have at least this fold change<br> of sample feature intensity divided by blank feature intensity</html>");
            blankSpinner.setEnabled(filterModel.getSampleBlankFoldChange().isEnabled());

            blankFilter.addChangeListener((e) -> blankSpinner.setEnabled(blankFilter.isSelected()));

            foldParameters.add(blankFilter, blankSpinner);
            foldParameters.addVerticalGlue();
        }

        {
            final TwoColumnPanel dataParameters = new TwoColumnPanel();
            centerTab.addTab("Data Quality", dataParameters);

            //MS data availability filter
            {
                dataParameters.add(Box.createVerticalStrut(6));
                dataParameters.add(new JXTitledSeparator("MS Data Quality"));
                hasMs1 = new JCheckBox("MS1");
                hasMs1.setToolTipText("Feature must have at least one MS1 spectrum");
                hasMs1.setSelected(filterModel.isHasMs1());
                hasMsMs = new JCheckBox("MS/MS");
                hasMsMs.setToolTipText("Feature must have at least one MS/MS spectrum");
                hasMsMs.setSelected(filterModel.isHasMsMs());

                Box box = Box.createHorizontalBox();
                box.add(hasMs1);
                box.add(Box.createHorizontalStrut(50));
                box.add(hasMsMs);
                box.add(Box.createHorizontalStrut(10));
                dataParameters.add(box);
            }

            //quality filter
            {
                dataParameters.add(Box.createVerticalStrut(5));
                dataParameters.add(new JXTitledSeparator("Feature Quality"));
                overallQualityPanel = new QualityFilterPanel(filterModel.getFeatureQualityFilter());
                dataParameters.addNamed("<html><b>Overall quality</b></html>", overallQualityPanel);
                dataParameters.add(Box.createVerticalStrut(5));
                qualityPanels = filterModel.getCategorizedQualityFilters().stream().map(qf -> {
                    QualityFilterPanel qfp = new QualityFilterPanel(qf);
                    dataParameters.addNamed(qf.getName(), qfp);
                    return qfp;
                }).toList();
            }

            dataParameters.addVerticalGlue();
        }

        final Box resultParameters = Box.createVerticalBox();
        resultParameters.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0));
        centerTab.addTab("Results", resultParameters);

        // Confidence score filter
        {
            resultParameters.add(Box.createVerticalStrut(9));
            resultParameters.add(new JXTitledSeparator("Confidence score"));

            TwoColumnPanel min = new TwoColumnPanel();
            TwoColumnPanel max = new TwoColumnPanel();

            Box box = Box.createHorizontalBox();
            box.add(min);
            box.add(Box.createHorizontalGlue());
            box.add(max);

            minConfidenceSpinner = makeSpinner(filterModel.getCurrentMinConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
            maxConfidenceSpinner = makeSpinner(filterModel.getCurrentMaxConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
            min.addNamed("Min confidence", minConfidenceSpinner);
            max.addNamed("Max confidence", maxConfidenceSpinner);
            ensureCompatibleBounds(minConfidenceSpinner, maxConfidenceSpinner);

            resultParameters.add(box);

        }

        // Element filter
        {
            resultParameters.add(Box.createVerticalStrut(9));
            resultParameters.add(new JXTitledSeparator("Elements in top molecular formula"));

            JPanel elementSelector = new JPanel();
            elementSelector.setLayout(new BoxLayout(elementSelector, BoxLayout.X_AXIS));
            JButton selectElements = new JButton("...");
            elementsField = new PlaceholderTextField(20);
            if (filterModel.getElementFilter().isActive())
                elementsField.setText(filterModel.getElementFilter().getConstraints().toString());

            selectElements.addActionListener(e -> {
                FormulaConstraints elements = new ElementFilter(elementsField.getText()).getConstraints();
                ElementSelectionDialog diag = new ElementSelectionDialog(this, "Filter Elements", elements);
                elements = diag.getConstraints();
                if (elements.equals(FormulaConstraints.empty()))
                    elementsField.setText(null);
                else
                    elementsField.setText(elements.toString());
            });
            elementsField.setPlaceholder("Enter or select formula constraints");
            elementSelector.add(elementsField);
            elementSelector.add(selectElements);

            resultParameters.add(elementSelector);
        }

        {
            resultParameters.add(Box.createVerticalStrut(10));
            resultParameters.add(new JXTitledSeparator("Lipid Class Filter"));

            //lipid filter
            TwoColumnPanel lipidFilterPanel = new TwoColumnPanel();
            lipidFilterBox = new JComboBox<>();
            java.util.List.copyOf(EnumSet.allOf(FeatureFilterModel.LipidFilter.class)).forEach(lipidFilterBox::addItem);
            lipidFilterPanel.addNamed("Lipid filter", lipidFilterBox);
            lipidFilterBox.setSelectedItem(filterModel.getLipidFilter());
            resultParameters.add(lipidFilterPanel);
        }

        // db filter
        {
            resultParameters.add(Box.createVerticalStrut(10));
            searchDBList = new JCheckboxListPanel<>(DBSelectionList.fromSearchableDatabases(gui.getSiriusClient()), "Hit in structure DB");
            searchDBList.checkBoxList.setVisibleRowCount(5);
            searchDBList.remove(searchDBList.buttons);
            searchDBList.checkBoxList.uncheckAll();

            candidateSpinner = makeSpinner(1, 1, 100, 1);
            searchDBList.addFooter(new TwoColumnPanel("Candidates to check", candidateSpinner));

            resultParameters.add(searchDBList);
            resultParameters.add(Box.createVerticalBox());

            if (filterModel.isDbFilterEnabled()) { //null check
                searchDBList.checkBoxList.checkAll(filterModel.getDbFilter().getDbs());
                candidateSpinner.setValue(filterModel.getDbFilter().getNumOfCandidates());
            }
        }

        reset = new JButton("Reset");
        reset.addActionListener(this);
        discard = new JButton("Discard");
        discard.addActionListener(this);
        apply = new JButton("Apply");
        apply.addActionListener(this);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons.add(reset);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(apply);
        buttons.add(discard);

        add(buttons, BorderLayout.SOUTH);

        setMaximumSize(GuiUtils.getEffectiveScreenSize());
        wireLiveChipRefresh();          // widget edits -> live re-render of the panel chips
        queryEditor.openSession(null);  // seed the user query from the shared doc + refresh autocomplete fields
        configureActions();
        selectTabForFacet(selectFacetId); // jump to the clicked chip's control, if any
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    /** The tab title that owns a given filter facet id (see PanelQueryNodeFactory facet ids); null if unknown. */
    static @Nullable String tabTitleForFacet(@Nullable String facetId) {
        if (facetId == null)
            return null;
        String base = facetId.contains(".") ? facetId.substring(0, facetId.indexOf('.')) : facetId;
        return switch (base) {
            case "mz", "rt", "adducts" -> "Input";
            case "blank" -> "Fold Change";
            case "hasMs1", "hasMsMs", "quality" -> "Data Quality";
            case "confidence", "elements", "lipid", "db" -> "Results";
            default -> null;
        };
    }

    /** Preselects the tab owning {@code facetId}; a no-op (keeps the default tab) if it is unknown. */
    private void selectTabForFacet(@Nullable String facetId) {
        String title = tabTitleForFacet(facetId);
        if (title == null)
            return;
        for (int i = 0; i < centerTab.getTabCount(); i++)
            if (title.equals(centerTab.getTitleAt(i))) {
                centerTab.setSelectedIndex(i);
                return;
            }
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
        String enterAction = "compute";
        String escAction = "abort";
        inputMap.put(enterKey, enterAction);
        inputMap.put(escKey, escAction);
        getRootPane().getActionMap().put(enterAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveChanges();
                dispose();
            }
        });
        getRootPane().getActionMap().put(escAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void saveChanges() {
        if (deleteSelection.isSelected()) {
            deleteSelectedCompoundsAndResetFilter();

        } else {
            applyToModel(filterModel);
            queryEditor.commitToDocument(); // bake the user query into the shared doc as part of Apply
            filterModel.fireUpdateCompleted();
        }
    }

    private void applyToModel(@NotNull FeatureFilterModel filterModel) {
        filterModel.setInverted(invertFilter.isSelected());

        filterModel.setCurrentMinMz(getMinMz());
        filterModel.setCurrentMaxMz(getMaxMz());
        filterModel.setCurrentMinRt(getMinRt());
        filterModel.setCurrentMaxRt(getMaxRt());
        filterModel.setCurrentMinConfidence(getMinConfidence());
        filterModel.setCurrentMaxConfidence(getMaxConfidence());
        filterModel.setHasMs1(hasMs1.isSelected());
        filterModel.setHasMsMs(hasMsMs.isSelected());
        filterModel.setAdducts(new HashSet<>(adductOptions.checkBoxList.getCheckedItems()));

        overallQualityPanel.updateModel(filterModel.getFeatureQualityFilter());

        Iterator<QualityFilterPanel> qualityPanelIt = qualityPanels.iterator();
        Iterator<QualityFilter> qualityFilterIt = filterModel.getCategorizedQualityFilters().iterator();
        while (qualityPanelIt.hasNext() && qualityFilterIt.hasNext())
            qualityPanelIt.next().updateModel(qualityFilterIt.next());

        filterModel.setLipidFilter((FeatureFilterModel.LipidFilter) lipidFilterBox.getSelectedItem());

        filterModel.setElementFilter(new ElementFilter(
                        elementsField.getText() == null || elementsField.getText().isBlank()
                                ? FormulaConstraints.empty()
                                : FormulaConstraints.fromString(elementsField.getText()),
//                        elementsMatchFormula.isSelected(), elementsMatchPrecursorFormula.isSelected()
                        true, false
                )
        );

        filterModel.setDbFilter(new DbFilter(searchDBList.checkBoxList.getCheckedItems(),
                ((SpinnerNumberModel) candidateSpinner.getModel()).getNumber().intValue()));

        filterModel.getSampleBlankFoldChange().setEnabled(blankFilter.isSelected());
        filterModel.getSampleBlankFoldChange().setCurrentMinFoldChange((Double) blankSpinner.getValue());
    }

    // --- embedded query editor (see queryEditor) ---

    /**
     * Builds the embedded {@link QueryEditorPanel}. Its panel chips are rendered from a live snapshot
     * of the dialog's own widget state ({@link #workingTerms()}), so they mirror the tabs without
     * touching the real model until Apply; the host wires the dialog's transaction boundary
     * (Esc = Discard, Enter = Apply) and turns a model-chip click into a jump to its tab.
     */
    private QueryEditorPanel buildQueryEditor() {
        SearchableFieldsProvider fieldsProvider = new SearchableFieldsProvider(
                gui.getSiriusClient(), gui.getProjectManager().getProjectId());
        SearchRenderState renderState = new SearchRenderState(fieldsProvider);
        FilterEditorHost jumpToTab = term -> selectTabForFacet(term.id());
        QueryEditorPanel.Host host = new QueryEditorPanel.Host() {
            @Override
            public void editorContentChanged() {
                // the chip area / autocomplete list changed height - relayout NORTH vs. the tabs
                queryEditor.revalidate();
                queryEditor.repaint();
            }

            @Override
            public void editorCloseRequested() {
                dispose(); // Esc inside the editor == Discard
            }

            @Override
            public void editorCommitRequested() {
                saveChanges(); // Enter inside the editor == Apply
                dispose();
            }

            @Override
            public void editorHandoff(@NotNull Runnable openFullEditor) {
                openFullEditor.run(); // a model chip was clicked -> just jump to its tab, don't close
            }
        };
        // embedded: no in-editor funnel or full-reset clear (the dialog has its own Reset/Discard/Apply)
        return new QueryEditorPanel(filterModel, fieldsProvider, this::workingTerms, jumpToTab, renderState,
                commit -> {}, () -> {}, host, true, null, null);
    }

    /**
     * The active panel facets of the dialog's CURRENT (uncommitted) widget state, as filter terms.
     * Compiles the widgets into a throwaway model - exactly as the delete path does - so the chips
     * reflect the working copy without mutating the real model. Defensive: a half-typed element
     * formula (etc.) that cannot be parsed yields no chips rather than an error.
     */
    private List<FilterTerm> workingTerms() {
        try {
            FeatureFilterModel working = new FeatureFilterModel();
            applyToModel(working);
            return PanelFilterTerms.of(working, gui.getProperties().getConfidenceDisplayMode());
        } catch (Exception ex) {
            log.debug("Could not build the working filter snapshot for the live chips", ex);
            return List.of();
        }
    }

    /** Re-renders the panel chips whenever any filter widget changes, so they track the tabs live. */
    private void wireLiveChipRefresh() {
        Runnable refresh = () -> queryEditor.rebuild();
        for (JSpinner s : new JSpinner[]{minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner,
                minConfidenceSpinner, maxConfidenceSpinner, candidateSpinner, blankSpinner})
            s.addChangeListener(e -> refresh.run());
        for (JCheckBox c : new JCheckBox[]{invertFilter, hasMs1, hasMsMs, blankFilter})
            c.addActionListener(e -> refresh.run());
        lipidFilterBox.addActionListener(e -> refresh.run());
        adductOptions.checkBoxList.addCheckBoxListener(e -> refresh.run());
        searchDBList.checkBoxList.addCheckBoxListener(e -> refresh.run());
        overallQualityPanel.onChange(refresh);
        qualityPanels.forEach(p -> p.onChange(refresh));
        elementsField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh.run(); }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == reset) {
            resetFilter();
            return;
        }
        if (e.getSource() == apply) {
            saveChanges();
        }
        this.dispose();
    }

    private void deleteSelectedCompoundsAndResetFilter() {
        // Build the query for the features to DELETE: everything that does NOT match the current filter.
        // Flipping the filter's inversion turns the "keep" (visible) query into its complement.
        FeatureFilterModel deleteMatcher = new FeatureFilterModel();
        applyToModel(deleteMatcher);
        deleteMatcher.setInverted(!deleteMatcher.isInverted());
        Optional<String> deleteQuery = deleteMatcher.toLuceneQuery(gui.getProperties().getConfidenceDisplayMode());

        // A blank/absent query means no filter is active -> everything matches -> nothing is "non-matching"
        // to delete. Otherwise confirm this destructive, irreversible action before doing anything.
        if (deleteQuery.isPresent()) {
            if (!new QuestionDialog(gui.getMainFrame(), "Delete non-matching features",
                    "<html>This will <b>permanently delete</b> all features that do <b>not</b> match the current filter.<br>This cannot be undone. Continue?</html>",
                    (String) null).isSuccess())
                return; // aborted: leave the filter untouched; the dialog is disposed by the caller
        }

        // reset global filter and close
        resetFilter();
        saveChanges();
        dispose();

        // clear selection to prevent unnecessary updates during deletions
        gui.getMainFrame().getCompoundList().getCompoundListSelectionModel().clearSelection();

        if (deleteQuery.isEmpty())
            return;

        // Delete server-side by query (one call; cascades DB + search index) and refresh the project
        // counters and feature list.
        final String query = deleteQuery.get();
        Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Deleting non-matching features...", false, new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {
                gui.getProjectManager().deleteAlignedFeaturesByQuery(query);
                return true;
            }
        });
    }

    /**
     * only reset values in the dialog, not the actual filter model
     */
    private void resetFilter() {
        resetSpinnerValues();
        adductOptions.checkBoxList.uncheckAll();
        overallQualityPanel.reset();
        qualityPanels.forEach(QualityFilterPanel::reset);

        lipidFilterBox.setSelectedItem(FeatureFilterModel.LipidFilter.KEEP_ALL_COMPOUNDS);
        elementsField.setText(null);
        searchDBList.checkBoxList.uncheckAll();
        invertFilter.setSelected(false);
        deleteSelection.setSelected(false);
        hasMs1.setSelected(false);
        hasMsMs.setSelected(false);

        blankFilter.setSelected(false);

        // clear the user query and re-render the chips from the freshly-reset widget state (a
        // programmatic checkbox reset does not fire the live-refresh listeners)
        queryEditor.clearUserQuery();
    }

    private void resetSpinnerValues() {
        minMzSpinner.setValue(filterModel.getMinMz());
        maxMzSpinner.setValue(filterModel.getMaxMz());
        minRtSpinner.setValue(filterModel.getMinRt());
        maxRtSpinner.setValue(filterModel.getMaxRt());
        minConfidenceSpinner.setValue(filterModel.getMinConfidence());
        maxConfidenceSpinner.setValue(filterModel.getMaxConfidence());
        candidateSpinner.setValue(1);
        blankSpinner.setValue(filterModel.getSampleBlankFoldChange().getMinFoldChange());
    }

    public double getMinMz() {
        return getDoubleValue(minMzSpinner);
    }

    public double getMaxMz() {
        return getDoubleValue(maxMzSpinner);
    }

    public double getMinRt() {
        return getDoubleValue(minRtSpinner);
    }

    public double getMaxRt() {
        return getDoubleValue(maxRtSpinner);
    }

    public double getMinConfidence() {
        return getDoubleValue(minConfidenceSpinner);
    }

    public double getMaxConfidence() {
        return getDoubleValue(maxConfidenceSpinner);
    }


    public double getDoubleValue(JSpinner spinner) {
        return ((SpinnerNumberModel) spinner.getModel()).getNumber().doubleValue();
    }

    public int getIntValue(JSpinner spinner) {
        return ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue();
    }

    private void ensureCompatibleBounds(JSpinner minSpinner, JSpinner maxSpinner) {
        minSpinner.addChangeListener(e -> {
            if (e.getSource() == minSpinner) {
                double min = ((SpinnerNumberModel) minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel) maxSpinner.getModel()).getNumber().doubleValue();
                if (min > max) {
                    maxSpinner.setValue(min);
                }
            }
        });

        maxSpinner.addChangeListener(e -> {
            if (e.getSource() == maxSpinner) {
                double min = ((SpinnerNumberModel) minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel) maxSpinner.getModel()).getNumber().doubleValue();
                if (min > max) {
                    minSpinner.setValue(max);
                }
            }
        });
    }

    public JSpinner makeSpinner(double value, double minimum, double maximum, double stepSize) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, minimum, maximum, stepSize);
        JSpinner spinner = new JSpinner(model);
        spinner.setMinimumSize(new Dimension(200, 26));
        spinner.setPreferredSize(new Dimension(200, 26));

        return spinner;
    }

}
