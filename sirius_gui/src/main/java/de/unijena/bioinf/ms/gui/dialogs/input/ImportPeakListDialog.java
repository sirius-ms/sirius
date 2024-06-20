/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.dialogs.input;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;

public class ImportPeakListDialog extends QuestionDialog {
    private static final String TITLE = "Import LC-MS/MS Data";
    private static final String DO_NOT_SHOW_AGAIN_KEY = "";
    private ImportPeakListPanel panel;

    public ImportPeakListDialog(Window owner) {
        super(owner, TITLE, "Please select how your data should be imported.", DO_NOT_SHOW_AGAIN_KEY);
    }

    @Override
    protected void decorateBodyPanel(TwoColumnPanel twoColumnBodyPanel) {
        panel = new ImportPeakListPanel();
        twoColumnBodyPanel.add(panel);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        final JButton ok = new JButton("Import");
        ok.addActionListener(e -> {
            rv = ReturnValue.Success;
            saveDoNotAskMeAgain();
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
    protected void saveDoNotAskMeAgain() {
        if (dontAsk != null && property != null && !property.isBlank() && dontAsk.isSelected())
            SiriusProperties.setProperty(property, getResult());
        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.ui.allowMs1Only", String.valueOf(panel.allowMS1Only.isSelected()));
        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.ui.ignoreFormulas", String.valueOf(panel.ignoreFormulas.isSelected()));
        SiriusJobs.runInBackground(() -> SiriusProperties.SIRIUS_PROPERTIES_FILE().store());
    }

    @Override
    protected Icon makeDialogIcon() {
        return Icons.DOCS_32;
    }

    @Override
    protected String getResult() {
        return String.valueOf(dontAsk.isSelected());
    }

    public static class ImportPeakListPanel extends TwoColumnPanel {
        final JCheckBox allowMS1Only, ignoreFormulas;

        public ImportPeakListPanel() {
            //props.setProperty("de.unijena.bioinf.sirius.ui.allowMs1Only", String.valueOf(allowMS1Only.isSelected()));
            //        props.setProperty("de.unijena.bioinf.sirius.ui.ignoreFormulas", String.valueOf(ignoreFormulas.isSelected()));
            add(Box.createVerticalStrut(10));
            add(new JXTitledSeparator("Import Options"));
            allowMS1Only = new JCheckBox();
            allowMS1Only.setSelected(Boolean.parseBoolean(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.allowMs1Only", null, "true")));
            allowMS1Only.setToolTipText(GuiUtils.formatToolTip("If checked data without MS/MS spectra will be imported. Otherwise they will be skipped during import."));
            addNamed("Import data without MS/MS", allowMS1Only);
            ignoreFormulas = new JCheckBox();
            ignoreFormulas.setSelected(Boolean.parseBoolean(SiriusProperties.getProperty("de.unijena.bioinf.sirius.ui.ignoreFormulas", null, "false")));
            ignoreFormulas.setToolTipText(GuiUtils.formatToolTip("If checked molecular formula and structure annotations will be ignored during import when  given in the input file."));
            addNamed("Ignore formulas", ignoreFormulas);
        }
    }
}
