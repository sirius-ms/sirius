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

public class ImportMSDataDialog extends DoNotShowAgainDialog {

    private static final String TITLE = "Import MS Data";
    private LCMSConfigPanel panel;

    protected ReturnValue rv;

    private static class LCMSConfigPanel extends SubToolConfigPanel<LcmsAlignOptions> {

        private final JCheckBox alignCheckBox;

        private final JCheckBox ignoreFormulas;

        public LCMSConfigPanel(TwoColumnPanel paras) {
            super(LcmsAlignOptions.class);

            paras.add(new JXTitledSeparator("Import Options"));
            paras.add(Box.createVerticalStrut(5));

            ignoreFormulas = new JCheckBox("Ignore formulas");
            ignoreFormulas.setSelected(Boolean.parseBoolean(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.ignoreFormulas", null, "false")));
            ignoreFormulas.setToolTipText(GuiUtils.formatToolTip("If checked, molecular formula and structure annotations will be ignored during peaklist import when  given in the input file."));
            paras.add(ignoreFormulas);

            alignCheckBox = makeGenericOptionCheckBox("Align and merge LC/MS runs", "align");
            alignCheckBox.setToolTipText(GuiUtils.formatToolTip("If checked, all LC/MS runs will be aligned and combined to one merged LC/MS run."));
            paras.add(alignCheckBox);

            paras.addVerticalGlue();
        }

    }

    public ImportMSDataDialog(Window owner, boolean showLCMSOptions, boolean alignAllowed, boolean showPeakListOptions) {
        super(owner, TITLE, "Please select how your MS data should be imported.", null);

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
            SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.ui.ignoreFormulas", String.valueOf(panel.ignoreFormulas.isSelected()));
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
