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

import de.unijena.bioinf.ms.gui.actions.DeleteExperimentAction;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog allows to adjust filter criteria of the {@link CompoundFilterModel} which is used to filter compound list.
 */
public class CompoundFilterOptionsDialog extends JDialog implements ActionListener {

    final SearchTextField searchField;
    final JTextField searchFieldDialogCopy;
    final JSpinner minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner;
    JButton discard, save, reset, deleteSelection;
    JCheckBox invertFilter;
    final CompoundFilterModel filterModel;
    final CompoundList compoundList;

    JCheckBox[] peakShape;

    final JComboBox<CompoundFilterModel.LipidFilter> lipidFilterBox;

    public CompoundFilterOptionsDialog(MainFrame owner, SearchTextField searchField, CompoundFilterModel filterModel, CompoundList compoundList) {
        super(owner, "Filter options", true);
        this.searchField = searchField;
        this.filterModel = filterModel;
        this.compoundList = compoundList;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        final TwoColumnPanel smallParameters = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Select by:", smallParameters), BorderLayout.CENTER);

        searchFieldDialogCopy = new JTextField(searchField.textField.getText());
        smallParameters.addNamed("filter by text", searchFieldDialogCopy);

        minMzSpinner = makeSpinner(filterModel.getCurrentMinMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
        smallParameters.addNamed("minimum m/z: ", minMzSpinner);
        maxMzSpinner = makeSpinner(filterModel.getCurrentMaxMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
        smallParameters.addNamed("maximum m/z: ", maxMzSpinner);
        ((JSpinner.DefaultEditor) maxMzSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel)maxMzSpinner.getModel(), filterModel.getMaxMz()));

        ensureCompatibleBounds(minMzSpinner, maxMzSpinner);

        minRtSpinner = makeSpinner(filterModel.getCurrentMinRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
        smallParameters.addNamed("minimum RT in sec: ", minRtSpinner);
        maxRtSpinner = makeSpinner(filterModel.getCurrentMaxRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
        smallParameters.addNamed("minimum RT in sec ", maxRtSpinner);
        ((JSpinner.DefaultEditor) maxRtSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel)maxRtSpinner.getModel(), filterModel.getMaxRt()));

        ensureCompatibleBounds(minRtSpinner, maxRtSpinner);

        {
            peakShape = new JCheckBox[]{
                    new JCheckBox("Low"),
                    new JCheckBox("Medium"),
                    new JCheckBox("High"),
            };
            final JPanel group = new JPanel();
            final BoxLayout groupLayout = new BoxLayout(group, BoxLayout.X_AXIS);
            group.setLayout(groupLayout);
            group.add(new JLabel("Peak shape quality: "));
            for (JCheckBox box : peakShape) {
                group.add(Box.createHorizontalStrut(12));
                group.add(box);
            }
            for (int i=0; i < peakShape.length; ++i) {
                peakShape[i].setSelected(filterModel.getPeakShapeQuality(i));
            }
            smallParameters.add(group);


        }

        //lipid filter
        lipidFilterBox = new JComboBox<>();
        java.util.List.copyOf(EnumSet.allOf(CompoundFilterModel.LipidFilter.class)).forEach(lipidFilterBox::addItem);
        smallParameters.addNamed("Lipid filter: ",lipidFilterBox);
        lipidFilterBox.setSelectedItem(filterModel.getLipidFilter());

        invertFilter = new JCheckBox("select non-matching");
        invertFilter.setSelected(compoundList.isFilterInverted());
        JPanel invertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        invertPanel.add(invertFilter);
        smallParameters.add(invertPanel);

        smallParameters.add(new JSeparator(SwingConstants.VERTICAL));


        reset = new JButton("Reset");
        reset.addActionListener(this);
        JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resetPanel.add(reset);
        smallParameters.add(resetPanel);

        smallParameters.add(new JSeparator(SwingConstants.VERTICAL));

        deleteSelection = new JButton("<html>Delete all <b>non-</b>selected compounds");
        deleteSelection.addActionListener(this);
        JPanel deleteSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        deleteSelectionPanel.add(deleteSelection);


        discard = new JButton("Discard");
        discard.addActionListener(this);
        save = new JButton("Apply filter");
        save.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(discard);
        buttons.add(save);

        add(new TwoColumnPanel(deleteSelectionPanel, buttons), BorderLayout.SOUTH);

//        add(buttons, BorderLayout.SOUTH);

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
        filterModel.setCurrentMinMz(getMinMz());
        filterModel.setCurrentMaxMz(getMaxMz());
        filterModel.setCurrentMinRt(getMinRt());
        filterModel.setCurrentMaxRt(getMaxRt());

        for (int k = 0; k < peakShape.length; ++k) {
            filterModel.setPeakShapeQuality(k, peakShape[k].isSelected());
        }

        filterModel.setLipidFilter((CompoundFilterModel.LipidFilter) lipidFilterBox.getSelectedItem());

        searchField.textField.setText(searchFieldDialogCopy.getText());
//            searchField.textField.setEnabled(!filterModel.isPeakShapeFilterEnabled() && !filterModel.isLipidFilterEnabled());

        if (invertFilter.isSelected() != compoundList.isFilterInverted()) {
            compoundList.toggleInvertFilter();
        }

        System.out.println("Is EDT: " + SwingUtilities.isEventDispatchThread());
        filterModel.fireUpdateCompleted();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == reset) {
            resetFilter();
            return;
        }
        if (e.getSource() == deleteSelection) {
            deleteSelectedCompoundsAndResetFilter();
            return;
        }
        if (e.getSource() == save){
            saveChanges();
        }
        this.dispose();
    }

    private void deleteSelectedCompoundsAndResetFilter() {
        saveChanges();
        //invert selection to remove all non-selected
        //todo currently the displayed FilterList ist used to select the compounds to remove.
        //this makes copying necessary to make it not look strange.
        // maybe it is better to apply the filter in a way to not change the displayed list
        compoundList.toggleInvertFilter();
        List<InstanceBean> toRemoveList = compoundList.getCompoundList().stream().collect(Collectors.toList());
        compoundList.toggleInvertFilter();
        (new DeleteExperimentAction(toRemoveList)).deleteCompounds();
        resetFilter();
        saveChanges();
    }

    /**
     * only reset values in the dialog, not the actual filter model
     */
    private void resetFilter() {
        resetSpinnerValues();
        for (int i=0; i < peakShape.length; ++i) {
            peakShape[i].setSelected(true);
        }
        lipidFilterBox.setSelectedItem(CompoundFilterModel.LipidFilter.KEEP_ALL_COMPOUNDS);
        searchFieldDialogCopy.setText("");
        invertFilter.setSelected(false);
    }

    private void resetSpinnerValues() {
        minMzSpinner.setValue(filterModel.getMinMz());
        maxMzSpinner.setValue(filterModel.getMaxMz());
        minRtSpinner.setValue(filterModel.getMinRt());
        maxRtSpinner.setValue(filterModel.getMaxRt());
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

    public double getDoubleValue(JSpinner spinner) {
        return ((SpinnerNumberModel)spinner.getModel()).getNumber().doubleValue();
    }

    private void ensureCompatibleBounds(JSpinner minSpinner, JSpinner maxSpinner) {
        minSpinner.addChangeListener(e -> {
            if (e.getSource() == minSpinner) {
                double min = ((SpinnerNumberModel)minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel)maxSpinner.getModel()).getNumber().doubleValue();
                if (min > max) {
                    maxSpinner.setValue(min);
                }
            }
        });

        maxSpinner.addChangeListener(e -> {
            if (e.getSource() == maxSpinner) {
                double min = ((SpinnerNumberModel)minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel)maxSpinner.getModel()).getNumber().doubleValue();
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
