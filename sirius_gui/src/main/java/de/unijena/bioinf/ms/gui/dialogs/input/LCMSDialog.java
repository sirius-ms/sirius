/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs.input;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.DataSmoothing;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignOptions;
import de.unijena.bioinf.ms.gui.compute.ParameterBinding;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DoNotShowAgainDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.function.Function;

public class LCMSDialog extends DoNotShowAgainDialog {

    private static final String TITLE = "Import LC-MS/MS Data";
    private LCMSConfigPanel panel;

    protected ReturnValue rv;

    private static class LCMSConfigPanel extends SubToolConfigPanel<LcmsAlignOptions> {

        private final JCheckBox alignCheckBox;

        private final JCheckBox allowMS1Only;

        public LCMSConfigPanel(TwoColumnPanel paras, JDialog parent) {
            super(LcmsAlignOptions.class);

            JComboBox<String> tagBox = makeGenericOptionComboBox("tag", List.of("blank", "control", "sample"), Function.identity());
            tagBox.setSelectedIndex(2);
            tagBox.setEditable(true);
            paras.addNamed("Data tag", tagBox);
            paras.addVerticalGlue();

            paras.add(new JXTitledSeparator("Import Options"));

            alignCheckBox = makeGenericOptionCheckBox("Align and merge runs", "align");
            alignCheckBox.setToolTipText(GuiUtils.formatToolTip("If checked, all LC/MS runs will be aligned and combined to one merged LC/MS run."));
            paras.add(alignCheckBox);

            allowMS1Only = new JCheckBox("Import data without MS/MS");
            allowMS1Only.setSelected(Boolean.parseBoolean(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.allowMs1Only", null, "true")));
            allowMS1Only.setToolTipText(GuiUtils.formatToolTip("If checked, data without MS/MS spectra will be imported. Otherwise they will be skipped during import."));
            paras.add(allowMS1Only);

            paras.add(new JXTitledSeparator("Data Smoothing Options"));

            TwoColumnPanel filterParas = new TwoColumnPanel();
            paras.add(filterParas);

            JComboBox<DataSmoothing> smoothingBox = makeGenericOptionComboBox("filter", DataSmoothing.class);

            JSpinner sigmaSpinner = makeGenericOptionSpinner("sigma", 3.0, 0.1, 10.0, 0.1, (model) -> Double.toString(model.getNumber().doubleValue()));
            JSpinner scaleSpinner = makeGenericOptionSpinner("scale", 20, 12, 100, 2, (model) -> Long.toString(Math.round(model.getNumber().doubleValue())));
            JSpinner windowSpinner = makeGenericOptionSpinner("window", 10, 1, 100, 1, (model) -> Double.toString(model.getNumber().doubleValue()));

            JLabel sigmaLabel = new JLabel("Sigma (kernel width)");
            JLabel scaleLabel = new JLabel("Wavelet coefficients");
            JLabel windowLabel = new JLabel("Window size [%]");

            filterParas.add(sigmaLabel, sigmaSpinner);
            filterParas.add(scaleLabel, scaleSpinner);
            filterParas.add(windowLabel, windowSpinner);

            sigmaLabel.setVisible(false);
            scaleLabel.setVisible(false);
            windowLabel.setVisible(false);

            sigmaSpinner.setVisible(false);
            scaleSpinner.setVisible(false);
            windowSpinner.setVisible(false);

            filterParas.addVerticalGlue();

            smoothingBox.addItemListener((event) -> {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    Object item = event.getItem();
                    if (item instanceof DataSmoothing filter) {
                        switch (filter) {
                            case GAUSSIAN -> SwingUtilities.invokeLater(() -> {
                                sigmaLabel.setVisible(true);
                                scaleLabel.setVisible(false);
                                windowLabel.setVisible(false);

                                sigmaSpinner.setVisible(true);
                                scaleSpinner.setVisible(false);
                                windowSpinner.setVisible(false);
                            });
                            case WAVELET -> SwingUtilities.invokeLater(() -> {
                                sigmaLabel.setVisible(false);
                                scaleLabel.setVisible(true);
                                windowLabel.setVisible(true);

                                sigmaSpinner.setVisible(false);
                                scaleSpinner.setVisible(true);
                                windowSpinner.setVisible(true);
                            });
                            default -> SwingUtilities.invokeLater(() -> {
                                sigmaLabel.setVisible(false);
                                scaleLabel.setVisible(false);
                                windowLabel.setVisible(false);

                                sigmaSpinner.setVisible(false);
                                scaleSpinner.setVisible(false);
                                windowSpinner.setVisible(false);
                            });
                        }
                        filterParas.revalidate();
                        filterParas.repaint();
                    }
                }
            });

            TwoColumnPanel smoothingPanel = new TwoColumnPanel();
            smoothingPanel.add(smoothingBox);
            smoothingPanel.addVerticalGlue();

            paras.add(smoothingPanel, filterParas);

            paras.addVerticalGlue();

            paras.add(new JXTitledSeparator("Feature Detection Options"));

            JSpinner noiseSpinner = makeGenericOptionSpinner("noise", 2.0, 0.01, 1000, 0.5, (model) -> Double.toString(model.getNumber().doubleValue()));
            paras.addNamed("Min. noise ratio", noiseSpinner);

            JSpinner persistenceSpinner = makeGenericOptionSpinner("persistence", 0.1, 0.01, 1.0, 0.1, (model) -> Double.toString(model.getNumber().doubleValue()));
            paras.addNamed("Min. baseline ratio", persistenceSpinner);

            JSpinner mergeSpinner = makeGenericOptionSpinner("merge", 0.8, 0.1, 1.0, 0.1, (model) -> Double.toString(model.getNumber().doubleValue()));
            paras.addNamed("Max valley ratio", mergeSpinner);

        }

    }

    public LCMSDialog(Window owner, boolean alignAllowed) {
        super(owner, TITLE, "Please select how your LC-MS/MS data should be imported.", null);

        if (alignAllowed) {
            panel.alignCheckBox.setSelected(true);
        } else {
            panel.alignCheckBox.setSelected(false);
            panel.alignCheckBox.setVisible(false);
        }

        rv = ReturnValue.Cancel;
        this.setVisible(true);
    }

    public ParameterBinding getParamterBinging() {
        return panel.getParameterBinding();
    }

    @Override
    protected String getResult() {
        return rv.name();
    }

    public boolean isSuccess() {
        return rv.equals(ReturnValue.Success);
    }

    @Override
    protected void decorateBodyPanel(TwoColumnPanel twoColumnBodyPanel) {
        panel = new LCMSConfigPanel(twoColumnBodyPanel, this);
        twoColumnBodyPanel.add(panel);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        final JButton ok = new JButton("Import");
        ok.addActionListener(e -> {
            SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.ui.allowMs1Only", String.valueOf(panel.allowMS1Only.isSelected()));
            rv = ReturnValue.Success;
            dispose();
        });


        final JButton abort = new JButton("Cancel");
        abort.addActionListener(e -> {
            rv = ReturnValue.Cancel;
            dispose();
        });

        boxedButtonPanel.add(Box.createHorizontalGlue());
        boxedButtonPanel.add(ok);
        boxedButtonPanel.add(abort);
    }

    @Override
    protected Icon makeDialogIcon() {
        return Icons.DOCS_32;
    }



}
