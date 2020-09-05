/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;

public class ActZodiacConfigPanel extends ActivatableConfigPanel<ZodiacConfigPanel> {
    public static final String DO_NOT_SHOW_AGAIN_KEY = "de.unijena.bioinf.sirius.computeDialog.zodiac.dontAskAgain";

    public ActZodiacConfigPanel() {
        super("ZODIAC", Icons.NET_32, false, ZodiacConfigPanel::new);
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        if (enabled && !PropertyManager.getBoolean(DO_NOT_SHOW_AGAIN_KEY, false)) {
            if (new QuestionDialog(MainFrame.MF, "Low number of Compounds",
                    GuiUtils.formatToolTip("Please note that ZODIAC is meant to improve molecular formula annotations on complete LC-MS/MS datasets. Using a low number of compounds may not result in improvements.", "", "Do you wish to continue anyways?"),
                    DO_NOT_SHOW_AGAIN_KEY).isAbort()) {
                activationButton.setSelected(false);
                return;
            }
        }

        super.setComponentsEnabled(enabled);
    }
}
