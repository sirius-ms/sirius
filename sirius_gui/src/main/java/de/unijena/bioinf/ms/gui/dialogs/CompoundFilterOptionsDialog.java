package de.unijena.bioinf.ms.gui.dialogs;
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
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.DeleteExperimentAction;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.DBSelectionList;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import io.sirius.ms.sdk.model.SearchableDatabase;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Dialog allows to adjust filter criteria of the {@link CompoundFilterModel} which is used to filter compound list.
 */
public class CompoundFilterOptionsDialog extends JDialog implements ActionListener {

    final PlaceholderTextField searchField;
    final JTextField searchFieldDialogCopy;
    final JSpinner minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner, minConfidenceSpinner, maxConfidenceSpinner, candidateSpinner;
    public final JCheckboxListPanel<PrecursorIonType> adductOptions;
    JButton discard, apply, reset;
    final JCheckBox invertFilter, deleteSelection, elementsMatchFormula, elementsMatchPrecursorFormula, hasMs1, hasMsMs;

    final JCheckBox blankFilter, controlFilter;
    final JSpinner blankSpinner, controlSpinner;

    final CompoundFilterModel filterModel;
    final CompoundList compoundList;


    final JComboBox<CompoundFilterModel.LipidFilter> lipidFilterBox;
    final PlaceholderTextField elementsField;

    final JCheckboxListPanel<SearchableDatabase> searchDBList;

    private final QualityFilterPanel overallQualityPanel;
    private final List<QualityFilterPanel> qualityPanels;

    final SiriusGui gui;

    public CompoundFilterOptionsDialog(SiriusGui gui, PlaceholderTextField searchField, CompoundFilterModel filterModel, CompoundList compoundList) {
        super(gui.getMainFrame(), "Filter configuration", true);
        this.gui = gui;
        this.searchField = searchField;
        this.filterModel = filterModel;
        this.compoundList = compoundList;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        TwoColumnPanel centerPanel = new TwoColumnPanel();
        add(centerPanel, BorderLayout.CENTER);

        //text search
        {
            searchFieldDialogCopy = new JTextField(searchField.getText());
            centerPanel.addNamed("Fulltext search", searchFieldDialogCopy);
        }

        centerPanel.add(Box.createVerticalStrut(10));
        final JTabbedPane centerTab = new JTabbedPane();
        centerPanel.add(centerTab);

        // filter modifiers
        {
            centerPanel.add(new JXTitledSeparator("Filter modifiers"));

            invertFilter = new JCheckBox("Invert Filter");
            invertFilter.setSelected(compoundList.isFilterInverted());

            deleteSelection = new JCheckBox("<html>Delete all <b>non-</b>matching compounds</html>");
            deleteSelection.setSelected(false);

            final Box group = Box.createHorizontalBox();
            group.add(invertFilter);
            group.add(Box.createHorizontalStrut(25));
            group.add(deleteSelection);
            group.add(Box.createHorizontalGlue());
            centerPanel.add(group);

//            centerPanel.addVerticalGlue();
        }

        //input data filters
        {
            final TwoColumnPanel inputParameters = new TwoColumnPanel();
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
                adductOptions = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Adducts", GuiUtils.formatToolTip("Select adducts to  filter by. Selecting all or none mean every adducts can pass"));
                adductOptions.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>(PrecursorIonType.fromString("[M + H20 + Na]+"), false));

                List<PrecursorIonType> ionizations = new ArrayList<>(filterModel.getPossibleAdducts());
                Collections.sort(ionizations);

                adductOptions.checkBoxList.replaceElements(ionizations);
                adductOptions.checkBoxList.uncheckAll();
                adductOptions.setEnabled(true);

                adductOptions.checkBoxList.checkAll(filterModel.getSelectedAdducts());
                inputParameters.add(adductOptions);
            }

            inputParameters.addVerticalGlue();
        }

        {

            // fold change / blank subtraction filter

            final TwoColumnPanel foldParameters = new TwoColumnPanel();
            centerTab.addTab("Fold Change", foldParameters);

            foldParameters.add(Box.createVerticalStrut(6));
            foldParameters.add(new JXTitledSeparator("Minimum Fold Change"));

            blankSpinner = makeSpinner(filterModel.getBlankSubtraction().getBlankSubtractionFoldChange(), 0.1, Double.POSITIVE_INFINITY, 0.1);
            controlSpinner = makeSpinner(filterModel.getBlankSubtraction().getCtrlSubtractionFoldChange(), 0.1, Double.POSITIVE_INFINITY, 0.1);

            blankFilter = new JCheckBox("sample/blank");
            blankFilter.setSelected(filterModel.getBlankSubtraction().isBlankSubtractionEnabled());
            blankFilter.setToolTipText("<html>Aligned feature must have at least this fold change<br> of sample feature intensity divided by blank feature intensity</html>");

            controlFilter = new JCheckBox("sample/control");
            controlFilter.setSelected(filterModel.getBlankSubtraction().isCtrlSubtractionEnabled());
            controlFilter.setToolTipText("<html>Aligned feature must have at least this fold change<br> of sample feature intensity divided by control feature intensity</html>");

            blankSpinner.setEnabled(filterModel.getBlankSubtraction().isBlankSubtractionEnabled());
            controlSpinner.setEnabled(filterModel.getBlankSubtraction().isCtrlSubtractionEnabled());

            blankFilter.addChangeListener((e) -> blankSpinner.setEnabled(blankFilter.isSelected()));
            controlFilter.addChangeListener((e) -> controlSpinner.setEnabled(controlFilter.isSelected()));

            foldParameters.add(blankFilter, blankSpinner);
            foldParameters.add(controlFilter, controlSpinner);

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
                hasMs1.setToolTipText("Feature must have a least one MS1 Spectrum");
                hasMs1.setSelected(filterModel.isHasMs1());
                hasMsMs = new JCheckBox("MS/MS");
                hasMsMs.setToolTipText("Feature must have a least one MS/MS Spectrum");
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
                qualityPanels = filterModel.getIoQualityFilters().stream().map(qf -> {
                    QualityFilterPanel qfp = new QualityFilterPanel(qf);
                    dataParameters.addNamed(qf.getName(), qfp);
                    return qfp;
                }).toList();
            }

            dataParameters.addVerticalGlue();
        }

        final JPanel resultParameters = new JPanel();
        resultParameters.setLayout(new BoxLayout(resultParameters, BoxLayout.Y_AXIS));
        resultParameters.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
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
            resultParameters.add(new JXTitledSeparator("Elements"));

            JPanel elementSelector = new JPanel();
            elementSelector.setLayout(new BoxLayout(elementSelector, BoxLayout.X_AXIS));
            JButton selectElements = new JButton("...");
            elementsField = new PlaceholderTextField(20);
            if (filterModel.getElementFilter().isActive())
                elementsField.setText(filterModel.getElementFilter().getConstraints().toString());

            selectElements.addActionListener(e -> {
                FormulaConstraints elements = new CompoundFilterModel.ElementFilter(elementsField.getText()).getConstraints();
                ElementSelectionDialog diag = new ElementSelectionDialog(this, "Filter Elements", elements);
                elements = diag.getConstraints();
                if (elements.equals(FormulaConstraints.empty()))
                    elementsField.setText(null);
                else
                    elementsField.setText(elements.toString());
            });
            elementsField.setPlaceholder("Insert or Select formula constraints");
            elementSelector.add(elementsField);
            elementSelector.add(selectElements);
            elementsMatchFormula = new JCheckBox("Molecular Formula");
            elementsMatchFormula.setSelected(filterModel.getElementFilter().isMatchFormula());
            elementsMatchPrecursorFormula = new JCheckBox("Precursor Formula");
            elementsMatchPrecursorFormula.setSelected(filterModel.getElementFilter().isMatchPrecursorFormula());

            final Box group = Box.createHorizontalBox();
            group.add(elementsMatchFormula);
            group.add(Box.createHorizontalStrut(25));
            group.add(elementsMatchPrecursorFormula);
            group.add(Box.createHorizontalGlue());

            resultParameters.add(elementSelector);
            resultParameters.add(group);
        }

        {
            resultParameters.add(Box.createVerticalStrut(10));
            resultParameters.add(new JXTitledSeparator("Lipid Class Filter"));

            //lipid filter
            TwoColumnPanel lipidFilterPanel = new TwoColumnPanel();
            lipidFilterBox = new JComboBox<>();
            java.util.List.copyOf(EnumSet.allOf(CompoundFilterModel.LipidFilter.class)).forEach(lipidFilterBox::addItem);
            lipidFilterPanel.addNamed("Lipid filter", lipidFilterBox);
            lipidFilterBox.setSelectedItem(filterModel.getLipidFilter());
            resultParameters.add(lipidFilterPanel);
        }


        // db filter
        {
            resultParameters.add(Box.createVerticalStrut(10));
            searchDBList = new JCheckboxListPanel<>(DBSelectionList.fromSearchableDatabases(gui.getSiriusClient()), "Hit in structure DB");
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
        buttons.add(discard);
        buttons.add(apply);

        add(buttons, BorderLayout.SOUTH);

        setMaximumSize(GuiUtils.getEffectiveScreenSize(getGraphicsConfiguration()));
        configureActions();
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
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
            if (invertFilter.isSelected() != compoundList.isFilterInverted())
                compoundList.toggleInvertFilter();

            filterModel.fireUpdateCompleted();
        }
    }

    private void applyToModel(@NotNull CompoundFilterModel filterModel) {
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
        Iterator<CompoundFilterModel.QualityFilter> qualityFilterIt = filterModel.getIoQualityFilters().iterator();
        while (qualityPanelIt.hasNext() && qualityFilterIt.hasNext())
            qualityPanelIt.next().updateModel(qualityFilterIt.next());

        filterModel.setLipidFilter((CompoundFilterModel.LipidFilter) lipidFilterBox.getSelectedItem());

        filterModel.setElementFilter(new CompoundFilterModel.ElementFilter(
                        elementsField.getText() == null || elementsField.getText().isBlank()
                                ? FormulaConstraints.empty()
                                : FormulaConstraints.fromString(elementsField.getText()),
                        elementsMatchFormula.isSelected(), elementsMatchPrecursorFormula.isSelected()
                )
        );

        filterModel.setDbFilter(new CompoundFilterModel.DbFilter(searchDBList.checkBoxList.getCheckedItems(),
                ((SpinnerNumberModel) candidateSpinner.getModel()).getNumber().intValue()));
        saveTextFilter();

        filterModel.getBlankSubtraction().setBlankSubtractionEnabled(blankFilter.isSelected());
        filterModel.getBlankSubtraction().setCtrlSubtractionEnabled(controlFilter.isSelected());
        filterModel.getBlankSubtraction().setBlankSubtractionFoldChange((Double) blankSpinner.getValue());
        filterModel.getBlankSubtraction().setCtrlSubtractionFoldChange((Double) controlSpinner.getValue());
    }

    private void saveTextFilter() {
        searchField.setText(searchFieldDialogCopy.getText());
        searchField.postActionEvent();
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

        // create deletion matcher
        CompoundFilterModel tmpModel = new CompoundFilterModel(gui);
        applyToModel(tmpModel);
        CompoundFilterMatcher matcher = new CompoundFilterMatcher(gui.getProperties(), tmpModel);
        boolean inverted = invertFilter.isSelected();
        // reset global filter and close
        resetFilter();
        saveChanges();
        dispose();

        // clear selection to prevent unnecessary updates during deletions
        gui.getMainFrame().getCompoundList().getCompoundListSelectionModel().clearSelection();

        // collect instances to delete
        List<InstanceBean> toDelete = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Filtering...", new TinyBackgroundJJob<List<InstanceBean>>() {
                    @Override
                    protected List<InstanceBean> compute() {
                        final int max = compoundList.getSortedSource().size();
                        AtomicInteger progress = new AtomicInteger(0);
                        if (inverted) {
                            return compoundList.getSortedSource().stream()
                                    .peek(i -> updateProgress(max, progress.getAndIncrement(), i.getGUIName()))
                                    .filter(matcher::matches).collect(Collectors.toList());
                        } else {
                            return compoundList.getSortedSource().stream()
                                    .peek(i -> updateProgress(max, progress.getAndIncrement(), i.getGUIName()))
                                    .filter(i -> !matcher.matches(i)).collect(Collectors.toList());
                        }
                    }
                }
        ).getResult();

        //delete instances
        ((DeleteExperimentAction) SiriusActions.DELETE_EXP.getInstance(gui)).deleteCompounds(toDelete);
    }

    /**
     * only reset values in the dialog, not the actual filter model
     */
    private void resetFilter() {
        resetSpinnerValues();
        adductOptions.checkBoxList.uncheckAll();
        overallQualityPanel.reset();
        qualityPanels.forEach(QualityFilterPanel::reset);

        lipidFilterBox.setSelectedItem(CompoundFilterModel.LipidFilter.KEEP_ALL_COMPOUNDS);
        elementsField.setText(null);
        searchDBList.checkBoxList.uncheckAll();
        searchFieldDialogCopy.setText("");
        invertFilter.setSelected(false);
        deleteSelection.setSelected(false);
        hasMs1.setSelected(false);
        hasMsMs.setSelected(false);

        blankFilter.setEnabled(false);
        controlFilter.setEnabled(false);
    }

    private void resetSpinnerValues() {
        minMzSpinner.setValue(filterModel.getMinMz());
        maxMzSpinner.setValue(filterModel.getMaxMz());
        minRtSpinner.setValue(filterModel.getMinRt());
        maxRtSpinner.setValue(filterModel.getMaxRt());
        minConfidenceSpinner.setValue(filterModel.getMinConfidence());
        maxConfidenceSpinner.setValue(filterModel.getMaxConfidence());
        candidateSpinner.setValue(1);
        blankSpinner.setValue(filterModel.getBlankSubtraction().getBlankSubtractionFoldChange());
        controlSpinner.setValue(filterModel.getBlankSubtraction().getCtrlSubtractionFoldChange());
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

    private static class QualityFilterPanel extends JPanel {
        JCheckBox[] qualityBoxes;

        public QualityFilterPanel(@NotNull CompoundFilterModel.QualityFilter qualityFilterModel) {
            super();
            final BoxLayout groupLayout = new BoxLayout(this, BoxLayout.X_AXIS);
            setLayout(groupLayout);

            qualityBoxes = qualityFilterModel.getPossibleQualities().stream().map(JCheckBox::new).toArray(JCheckBox[]::new);
            for (int i = 0; i < qualityBoxes.length; ++i) {
                add(Box.createHorizontalGlue());
                add(qualityBoxes[i]);
                qualityBoxes[i].setSelected(qualityFilterModel.isQualitySelected(i));
            }
            add(Box.createHorizontalStrut(10));
        }

        public void reset() {
            for (JCheckBox jCheckBox : qualityBoxes)
                jCheckBox.setSelected(true);
        }

        public void updateModel(CompoundFilterModel.QualityFilter qualityFilter) {
            for (int k = 0; k < qualityBoxes.length; ++k)
                qualityFilter.setQualitySelected(k, qualityBoxes[k].isSelected());
        }
    }
}
