package de.unijena.bioinf.ms.gui.dialogs;
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.compute.DBSelectionList;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * Dialog allows to adjust filter criteria of the {@link CompoundFilterModel} which is used to filter compound list.
 */
public class CompoundFilterOptionsDialog extends JDialog implements ActionListener {

    final SearchTextField searchField;
    final JTextField searchFieldDialogCopy;
    final JSpinner minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner, minConfidenceSpinner, maxConfidenceSpinner;
    public final JCheckboxListPanel<PrecursorIonType> adductOptions;
    JButton discard, apply, reset;
    JCheckBox invertFilter;
    JCheckBox deleteSelection;
    final CompoundFilterModel filterModel;
    final CompoundList compoundList;

    JCheckBox[] peakShape;

    final JComboBox<CompoundFilterModel.LipidFilter> lipidFilterBox;
    final PlaceholderTextField elementsField;
    final JCheckBox elementsMatchFormula;
    final JCheckBox elementsMatchPrecursorFormula;

    final JCheckboxListPanel<CustomDataSources.Source> searchDBList;

    public CompoundFilterOptionsDialog(MainFrame owner, SearchTextField searchField, CompoundFilterModel filterModel, CompoundList compoundList) {
        super(owner, "Filter configuration", true);
        this.searchField = searchField;
        this.filterModel = filterModel;
        this.compoundList = compoundList;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        final TwoColumnPanel smallParameters = new TwoColumnPanel();
        add(smallParameters, BorderLayout.CENTER);

        smallParameters.add(new JXTitledSeparator("Filter criteria"));

        searchFieldDialogCopy = new JTextField(searchField.textField.getText());
        smallParameters.addNamed("filter by text", searchFieldDialogCopy);

        minMzSpinner = makeSpinner(filterModel.getCurrentMinMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
        smallParameters.addNamed("minimum m/z: ", minMzSpinner);
        maxMzSpinner = makeSpinner(filterModel.getCurrentMaxMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
        smallParameters.addNamed("maximum m/z: ", maxMzSpinner);
        ((JSpinner.DefaultEditor) maxMzSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxMzSpinner.getModel(), filterModel.getMaxMz()));

        ensureCompatibleBounds(minMzSpinner, maxMzSpinner);

        minRtSpinner = makeSpinner(filterModel.getCurrentMinRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
        smallParameters.addNamed("minimum RT in sec: ", minRtSpinner);
        maxRtSpinner = makeSpinner(filterModel.getCurrentMaxRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
        smallParameters.addNamed("maximum RT in sec: ", maxRtSpinner);
        ((JSpinner.DefaultEditor) maxRtSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxRtSpinner.getModel(), filterModel.getMaxRt()));

        ensureCompatibleBounds(minRtSpinner, maxRtSpinner);

        minConfidenceSpinner = makeSpinner(filterModel.getCurrentMinConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
        smallParameters.addNamed("minimum Confidence: ", minConfidenceSpinner);
        maxConfidenceSpinner = makeSpinner(filterModel.getCurrentMaxConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
        smallParameters.addNamed("maximum Confidence: ", maxConfidenceSpinner);

        ensureCompatibleBounds(minConfidenceSpinner, maxConfidenceSpinner);


        //peak shape filter
        {
            peakShape = new JCheckBox[]{
                    new JCheckBox("Low"),
                    new JCheckBox("Medium"),
                    new JCheckBox("High"),
            };
            final JPanel group = new JPanel();
            final BoxLayout groupLayout = new BoxLayout(group, BoxLayout.X_AXIS);
            group.setLayout(groupLayout);
            for (JCheckBox box : peakShape) {
                group.add(Box.createHorizontalGlue());
                group.add(box);
            }
            group.add(Box.createHorizontalGlue());
            for (int i = 0; i < peakShape.length; ++i) {
                peakShape[i].setSelected(filterModel.getPeakShapeQuality(i));
            }
            smallParameters.addNamed("Peak shape quality: ", group);
        }

        //lipid filter
        {
            lipidFilterBox = new JComboBox<>();
            java.util.List.copyOf(EnumSet.allOf(CompoundFilterModel.LipidFilter.class)).forEach(lipidFilterBox::addItem);
            smallParameters.addNamed("Lipid filter: ", lipidFilterBox);
            lipidFilterBox.setSelectedItem(filterModel.getLipidFilter());
        }

        // Element filter
        {
            smallParameters.add(new JXTitledSeparator("Elements"));

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
            smallParameters.add(elementSelector);
            final JPanel group = new JPanel();
            final BoxLayout groupLayout = new BoxLayout(group, BoxLayout.X_AXIS);
            group.setLayout(groupLayout);

            elementsMatchFormula = new JCheckBox("Molecular Formula");
            elementsMatchFormula.setSelected(filterModel.getElementFilter().isMatchFormula());
            elementsMatchPrecursorFormula = new JCheckBox("Precursor Formula");
            elementsMatchPrecursorFormula.setSelected(filterModel.getElementFilter().isMatchPrecursorFormula());
            group.add(Box.createHorizontalGlue());
            group.add(elementsMatchFormula);
            group.add(Box.createHorizontalGlue());
            group.add(elementsMatchPrecursorFormula);
            group.add(Box.createHorizontalGlue());
            smallParameters.add(group);
        }

        // Adduct filter
        adductOptions = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Adducts", GuiUtils.formatToolTip("Select adducts to  filter by. Selecting all or none mean every adducts can pass"));
        adductOptions.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>(PrecursorIonType.fromString("[M + H20 + Na]+"), false));

        List<PrecursorIonType> ionizations = new ArrayList<>();
        ionizations.add(PrecursorIonType.unknownPositive());
        ionizations.add(PrecursorIonType.unknownNegative());
        ionizations.addAll(PeriodicTable.getInstance().getPositiveAdducts());
        ionizations.addAll(PeriodicTable.getInstance().getNegativeAdducts());
        ionizations.sort(Comparator.comparing(PrecursorIonType::toString));

        adductOptions.checkBoxList.replaceElements(ionizations);
        adductOptions.checkBoxList.uncheckAll();
        adductOptions.setEnabled(true);

        smallParameters.add(adductOptions);
        adductOptions.checkBoxList.checkAll(filterModel.getAdducts());

        // db filter
        {
            searchDBList = new JCheckboxListPanel<>(new DBSelectionList(), "Hit in structure DB");
            smallParameters.add(searchDBList);
            searchDBList.checkBoxList.uncheckAll();
            if (filterModel.isDbFilterEnabled()) //null check
                searchDBList.checkBoxList.checkAll(filterModel.getDbFilter());
        }

        smallParameters.add(new JXTitledSeparator("Filter options"));

        invertFilter = new JCheckBox("Invert Filter");
        invertFilter.setSelected(compoundList.isFilterInverted());
        smallParameters.add(invertFilter);

        deleteSelection = new JCheckBox("<html>Delete all <b>non-</b>matching compounds</html>");
        deleteSelection.setSelected(false);
        smallParameters.add(deleteSelection);

        reset = new JButton("Reset");
        reset.addActionListener(this);
        smallParameters.add(new JSeparator(SwingConstants.VERTICAL));

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
        filterModel.setAdducts(new HashSet<>(adductOptions.checkBoxList.getCheckedItems()));

        for (int k = 0; k < peakShape.length; ++k) {
            filterModel.setPeakShapeQuality(k, peakShape[k].isSelected());
        }

        filterModel.setLipidFilter((CompoundFilterModel.LipidFilter) lipidFilterBox.getSelectedItem());

        filterModel.setElementFilter(new CompoundFilterModel.ElementFilter(
                        elementsField.getText() == null || elementsField.getText().isBlank()
                                ? FormulaConstraints.empty()
                                : FormulaConstraints.fromString(elementsField.getText()),
                        elementsMatchFormula.isSelected(), elementsMatchPrecursorFormula.isSelected()
                )
        );

        filterModel.setDbFilter(searchDBList.checkBoxList.getCheckedItems());

        saveTextFilter();
    }

    private void saveTextFilter() {
        searchField.textField.setText(searchFieldDialogCopy.getText());
        searchField.textField.postActionEvent();
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
        CompoundFilterModel tmpModel = new CompoundFilterModel();
        applyToModel(tmpModel);
        CompoundFilterMatcher matcher = new CompoundFilterMatcher(tmpModel);

        // reset global filter and close
        resetFilter();
        saveChanges();
        dispose();

        // clear selection to prevent unnecessary updates during deletions
        MF.getCompoundList().getCompoundListSelectionModel().clearSelection();

        // collect instances to delete
        List<InstanceBean> toDelete = Jobs.runInBackgroundAndLoad(MF, "Collecting Instances...", () -> invertFilter.isSelected()
                ? compoundList.getCompoundList().stream().filter(matcher::matches).collect(Collectors.toList())
                : compoundList.getCompoundList().stream().filter(i -> !matcher.matches(i)).collect(Collectors.toList())
        ).getResult();

        //delete instances
        MF.ps().deleteCompounds(toDelete);
    }

    /**
     * only reset values in the dialog, not the actual filter model
     */
    private void resetFilter() {
        resetSpinnerValues();
        adductOptions.checkBoxList.uncheckAll();
        for (int i = 0; i < peakShape.length; ++i) {
            peakShape[i].setSelected(true);
        }
        lipidFilterBox.setSelectedItem(CompoundFilterModel.LipidFilter.KEEP_ALL_COMPOUNDS);
        elementsField.setText(null);
        searchDBList.checkBoxList.uncheckAll();
        searchFieldDialogCopy.setText("");
        invertFilter.setSelected(false);
        deleteSelection.setSelected(false);
    }

    private void resetSpinnerValues() {
        minMzSpinner.setValue(filterModel.getMinMz());
        maxMzSpinner.setValue(filterModel.getMaxMz());
        minRtSpinner.setValue(filterModel.getMinRt());
        maxRtSpinner.setValue(filterModel.getMaxRt());
        minConfidenceSpinner.setValue(filterModel.getMinConfidence());
        maxConfidenceSpinner.setValue(filterModel.getMaxConfidence());
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
