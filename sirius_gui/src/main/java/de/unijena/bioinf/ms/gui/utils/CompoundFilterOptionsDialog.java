package de.unijena.bioinf.ms.gui.utils;/*
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

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CompoundFilterOptionsDialog extends JDialog implements ActionListener {

    final JSpinner minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner;
    JButton discard, save, reset;
    CompoundFilterModel filterModel;

    public CompoundFilterOptionsDialog(MainFrame owner, CompoundFilterModel filterModel) {
        super(owner, "Filter options", true);
        this.filterModel = filterModel;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        final TwoColumnPanel smallParameters = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Filter by:", smallParameters), BorderLayout.CENTER);


        minMzSpinner = makeSpinner(filterModel.getCurrentMinMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
        smallParameters.addNamed("minimum m/z: ", minMzSpinner);

        maxMzSpinner = makeSpinner(filterModel.getCurrentMaxMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
        smallParameters.addNamed("maximum m/z: ", maxMzSpinner);

        ensureCompatibleBounds(minMzSpinner, maxMzSpinner);

        minRtSpinner = makeSpinner(filterModel.getCurrentMinRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
        smallParameters.addNamed("minimum RT in sec: ", minRtSpinner);

        maxRtSpinner = makeSpinner(filterModel.getCurrentMaxRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
        smallParameters.addNamed("minimum RT in sec ", maxRtSpinner);

        ensureCompatibleBounds(minRtSpinner, maxRtSpinner);

        reset = new JButton("Reset");
        reset.addActionListener(this);

        JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resetPanel.add(reset);
        smallParameters.add(resetPanel);

        discard = new JButton("Discard");
        discard.addActionListener(this);
        save = new JButton("Save");
        save.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(discard);
        buttons.add(save);

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

    private void saveChanges(){
        filterModel.setCurrentMinMz(getMinMz());
        filterModel.setCurrentMaxMz(getMaxMz());
        filterModel.setCurrentMinRt(getMinRt());
        filterModel.setCurrentMaxRt(getMaxRt());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == reset) {
            resetFilter();
            return;
        }
        if (e.getSource() == save){
            saveChanges();
        }
        this.dispose();
    }

    private void resetFilter() {
        filterModel.resetFilter();
        resetSpinnerValues();
    }

    private void resetSpinnerValues() {
        minMzSpinner.setValue(filterModel.getMinMz());
        maxMzSpinner.setValue(filterModel.getMaxMz());
        minRtSpinner.setValue(filterModel.getMinRt());
        maxRtSpinner.setValue(filterModel.getMaxRt());
    }

    public double getMinMz() {
        return ((SpinnerNumberModel)minMzSpinner.getModel()).getNumber().doubleValue();
    }

    public double getMaxMz() {
        return ((SpinnerNumberModel)maxMzSpinner.getModel()).getNumber().doubleValue();
    }

    public double getMinRt() {
        return ((SpinnerNumberModel)minRtSpinner.getModel()).getNumber().doubleValue();
    }

    public double getMaxRt() {
        return ((SpinnerNumberModel)maxRtSpinner.getModel()).getNumber().doubleValue();
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
