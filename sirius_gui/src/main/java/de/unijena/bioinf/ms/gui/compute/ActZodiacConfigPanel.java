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

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;

public class ActZodiacConfigPanel extends ActivatableConfigPanel<ZodiacConfigPanel> {

    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_COMP = "de.unijena.bioinf.sirius.computeDialog.zodiac.compounds.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_MEM = "de.unijena.bioinf.sirius.computeDialog.zodiac.memory.dontAskAgain";

    private final int compoundsNumber;

    public ActZodiacConfigPanel(SiriusGui gui, boolean displayAdvancedParameters, int compoundsNumber) {
        super(gui, "ZODIAC", Icons.ZODIAC.derive(32,32), () -> new ZodiacConfigPanel(displayAdvancedParameters),
                SoftwareTourInfoStore.BatchCompute_ZODIAC);
        this.compoundsNumber = compoundsNumber;
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        if (enabled) {
            if (new QuestionDialog(gui.getMainFrame(), "Low number of Compounds",
                    GuiUtils.formatToolTip("Please note that ZODIAC is meant to improve molecular formula annotations on complete LC-MS/MS datasets. Using a low number of compounds may not result in improvements.", "", "Do you wish to continue anyways?"),
                    DO_NOT_SHOW_AGAIN_KEY_Z_COMP, ReturnValue.Success).isCancel()) {
                activationButton.setSelected(false);
                return;
            }

            if ((compoundsNumber > 2000 && (Runtime.getRuntime().maxMemory() / 1024 / 1024 / 1024) < 8)) {
                if (new QuestionDialog(gui.getMainFrame(), "High Memory Consumption",
                        GuiUtils.formatToolTip("Your ZODIAC analysis contains `" + compoundsNumber + "` compounds and may therefore consume more system memory than available.", "", "Do you wish to continue anyways?"),
                        DO_NOT_SHOW_AGAIN_KEY_Z_MEM, ReturnValue.Success).isCancel()) {
                    activationButton.setSelected(false);
                    return;
                }
            }
        }

        super.setComponentsEnabled(enabled);
    }
}
