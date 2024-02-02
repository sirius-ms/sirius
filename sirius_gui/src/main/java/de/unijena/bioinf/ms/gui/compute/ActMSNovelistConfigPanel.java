/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;

public class ActMSNovelistConfigPanel extends ActivatableConfigPanel<SubToolConfigPanel<FingerprintOptions>> {

    public ActMSNovelistConfigPanel() {
        super("MSNovelist", Icons.FINGER_32, true, () -> { //todo change icon
            SubToolConfigPanel<FingerprintOptions> p = new SubToolConfigPanel<>(FingerprintOptions.class) { //todo NewWorkflow: set correct options
            };
            return p;
        });
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        //todo maybe add citation dialog?
        String adviceKey = "de.unijena.bioinf.sirius.ui.advise.msnovelist";

        if (enabled && !PropertyManager.getBoolean(adviceKey, false)) {
            if (new QuestionDialog(MainFrame.MF, "Do you require de novo structure generation?",
                    GuiUtils.formatToolTip("Please note that de novo structure elucidation from MS data remains challenging. For most applications, searching in a molecular structure database with CSI:FingerID shouldbe default.", "", "Do you wish to continue anyways?"),
                    adviceKey).isAbort()) {
                super.setComponentsEnabled(false);
                super.activationButton.setSelected(false);
                return;
            }
        }
        super.setComponentsEnabled(enabled);
    }

}
