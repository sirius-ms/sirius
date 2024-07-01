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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ImportMSDataDialog extends DoNotShowAgainDialog {

    private static final String TITLE = "Import MS Data";
    private LCMSConfigPanel panel;

    protected ReturnValue rv;

    private static class LCMSConfigPanel extends SubToolConfigPanel<LcmsAlignOptions> {

        private final JCheckBox alignCheckBox;

        private final JCheckBox allowMS1Only;

        private final JCheckBox ignoreFormulas;

        private final List<JComponent> lcmsComponents = new ArrayList<>();

        public LCMSConfigPanel(TwoColumnPanel paras) {
            super(LcmsAlignOptions.class);

            paras.add(new JXTitledSeparator("Import Options"));

            JComboBox<String> tagBox = makeGenericOptionComboBox("tag", List.of("blank", "control", "sample"), Function.identity());
            tagBox.setSelectedIndex(2);
            tagBox.setEditable(true);
            paras.addNamed("Data tag", tagBox);
            paras.addVerticalGlue();

            allowMS1Only = new JCheckBox("Import data without MS/MS");
            allowMS1Only.setSelected(Boolean.parseBoolean(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.allowMs1Only", null, "true")));
            allowMS1Only.setToolTipText(GuiUtils.formatToolTip("If checked, data without MS/MS spectra will be imported. Otherwise they will be skipped during import."));
            paras.add(allowMS1Only);

            ignoreFormulas = new JCheckBox("Ignore formulas");
            ignoreFormulas.setSelected(Boolean.parseBoolean(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.ignoreFormulas", null, "false")));
            ignoreFormulas.setToolTipText(GuiUtils.formatToolTip("If checked, molecular formula and structure annotations will be ignored during peaklist import when  given in the input file."));
            paras.add(ignoreFormulas);

            paras.addVerticalGlue();
            paras.add(Box.createVerticalStrut(5));

            JXTitledSeparator lcmsSep = new JXTitledSeparator("LC/MS Import Options");
            lcmsComponents.add(lcmsSep);
            paras.add(lcmsSep);

            alignCheckBox = makeGenericOptionCheckBox("Align and merge runs", "align");
            alignCheckBox.setToolTipText(GuiUtils.formatToolTip("If checked, all LC/MS runs will be aligned and combined to one merged LC/MS run."));
            lcmsComponents.add(alignCheckBox);
            paras.add(alignCheckBox);

            JLabel smoothLabel = new JLabel("Data smoothing");
            JComboBox<DataSmoothing> smoothBox = makeGenericOptionComboBox("filter", DataSmoothing.class);

            paras.add(smoothLabel, smoothBox);

            lcmsComponents.add(smoothLabel);
            lcmsComponents.add(smoothBox);

            JSpinner sigmaSpinner = makeGenericOptionSpinner("sigma", 3.0, 0.1, 10.0, 0.1, (model) -> Double.toString(model.getNumber().doubleValue()));
            JSpinner scaleSpinner = makeGenericOptionSpinner("scale", 20, 12, 100, 2, (model) -> Long.toString(Math.round(model.getNumber().doubleValue())));
            JSpinner windowSpinner = makeGenericOptionSpinner("window", 10, 1, 100, 1, (model) -> Double.toString(model.getNumber().doubleValue()));

            JLabel sigmaLabel = new JLabel("Sigma (kernel width)");
            JLabel scaleLabel = new JLabel("Wavelet coefficients");
            JLabel windowLabel = new JLabel("Window size [%]");

            paras.add(sigmaLabel, sigmaSpinner);
            paras.add(scaleLabel, scaleSpinner);
            paras.add(windowLabel, windowSpinner);

            lcmsComponents.addAll(List.of(sigmaLabel, scaleLabel, windowLabel, sigmaSpinner, scaleSpinner, windowSpinner));

            sigmaLabel.setEnabled(false);
            scaleLabel.setEnabled(false);
            windowLabel.setEnabled(false);

            sigmaSpinner.setEnabled(false);
            scaleSpinner.setEnabled(false);
            windowSpinner.setEnabled(false);

            paras.addVerticalGlue();

            smoothBox.addItemListener((event) -> {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    Object item = event.getItem();
                    if (item instanceof DataSmoothing filter) {
                        switch (filter) {
                            case GAUSSIAN -> SwingUtilities.invokeLater(() -> {
                                sigmaLabel.setEnabled(true);
                                scaleLabel.setEnabled(false);
                                windowLabel.setEnabled(false);

                                sigmaSpinner.setEnabled(true);
                                scaleSpinner.setEnabled(false);
                                windowSpinner.setEnabled(false);
                            });
                            case WAVELET -> SwingUtilities.invokeLater(() -> {
                                sigmaLabel.setEnabled(false);
                                scaleLabel.setEnabled(true);
                                windowLabel.setEnabled(true);

                                sigmaSpinner.setEnabled(false);
                                scaleSpinner.setEnabled(true);
                                windowSpinner.setEnabled(true);
                            });
                            default -> SwingUtilities.invokeLater(() -> {
                                sigmaLabel.setEnabled(false);
                                scaleLabel.setEnabled(false);
                                windowLabel.setEnabled(false);

                                sigmaSpinner.setEnabled(false);
                                scaleSpinner.setEnabled(false);
                                windowSpinner.setEnabled(false);
                            });
                        }
                    }
                }
            });

            paras.add(Box.createVerticalStrut(5));

            JXTitledSeparator featSep = new JXTitledSeparator("LC/MS Feature Detection Options");
            lcmsComponents.add(featSep);
            paras.add(featSep);

            JLabel noiseLabel = new JLabel("Min. noise ratio");
            JSpinner noiseSpinner = makeGenericOptionSpinner("noise", 2.0, 0.01, 1000, 0.5, (model) -> Double.toString(model.getNumber().doubleValue()));
            lcmsComponents.add(noiseLabel);
            lcmsComponents.add(noiseSpinner);
            paras.add(noiseLabel, noiseSpinner);

            JLabel persistenceLabel = new JLabel("Min. baseline ratio");
            JSpinner persistenceSpinner = makeGenericOptionSpinner("persistence", 0.1, 0.01, 1.0, 0.1, (model) -> Double.toString(model.getNumber().doubleValue()));
            lcmsComponents.add(persistenceLabel);
            lcmsComponents.add(persistenceSpinner);
            paras.add(persistenceLabel, persistenceSpinner);

            JLabel mergeLabel = new JLabel("Max valley ratio");
            JSpinner mergeSpinner = makeGenericOptionSpinner("merge", 0.8, 0.1, 1.0, 0.1, (model) -> Double.toString(model.getNumber().doubleValue()));
            lcmsComponents.add(mergeLabel);
            lcmsComponents.add(mergeSpinner);
            paras.add(mergeLabel, mergeSpinner);
        }

    }

    public ImportMSDataDialog(Window owner, boolean showLCMSOptions, boolean alignAllowed, boolean showPeakListOptions) {
        super(owner, TITLE, "Please select how your MS data should be imported.", null);

        for (JComponent comp : panel.lcmsComponents)
            comp.setVisible(showLCMSOptions);

        if (showLCMSOptions && alignAllowed) {
            panel.alignCheckBox.setSelected(true);
        } else {
            panel.alignCheckBox.setSelected(false);
            panel.alignCheckBox.setVisible(false);
        }

        panel.ignoreFormulas.setVisible(showPeakListOptions);

        rv = ReturnValue.Cancel;
        this.pack();
        this.setVisible(true);
    }

    public ParameterBinding getParamterBinding() {
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
        panel = new LCMSConfigPanel(twoColumnBodyPanel);
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
