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

import de.unijena.bioinf.ms.frontend.subtools.msnovelist.MsNovelistOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import io.sirius.ms.sdk.model.AllowedFeatures;
import io.sirius.ms.sdk.model.ConnectionCheck;

import java.util.Map;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

public class ActMSNovelistConfigPanel extends ActivatableConfigPanel<SubToolConfigPanel<MsNovelistOptions>> {

    public ActMSNovelistConfigPanel(SiriusGui gui) {
        super(gui, "MSNovelist", null, Icons.DENOVO.derive(32,32), true, () -> new SubToolConfigPanel<>(MsNovelistOptions.class),
                SoftwareTourInfoStore.BatchCompute_MsNovelist);
        notConnectedMessage = "Can't connect to prediction server!";
    }

    @Override
    protected void processConnectionCheck(ConnectionCheck check) {
        if (check == null)
            return;

        if (!isConnected(check)) {
            setButtonEnabled(false, notConnectedMessage);
            return;
        }


        boolean hasDenovoFeature = gui.getAllowedFeatures().map(AllowedFeatures::isDeNovo).orElse(false);
        setButtonEnabled(hasDenovoFeature, "Your Subscription does not contain the de novo structure generation feature (or you are not logged in).");
        setButtonEnabled(true, notConnectedMessage);
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        String adviceKey = "de.unijena.bioinf.sirius.ui.advise.msnovelist";

        if (enabled) {
            if (new QuestionDialog(gui.getMainFrame(), "Do you require de novo structure generation?",
                    GuiUtils.formatToolTip("Please note that de novo structure elucidation from MS data remains challenging. For most applications, searching in a molecular structure database with CSI:FingerID should be default.",
                            "Additionally, please note that MSNovelist may increase overall running time considerably.",
                            "", "Do you wish to continue anyways?"),
                    adviceKey, ReturnValue.Success).isCancel()) {
                super.setComponentsEnabled(false);
                super.activationButton.setSelected(false);
                return;
            }
        }
        super.setComponentsEnabled(enabled);
    }

    @Override
    public void applyValuesFromPreset(boolean enable, Map<String, String> preset) {
        if (enable != isToolSelected()) {
            activationButton.doClick(0);
        }
    }
}
