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
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.nightsky.sdk.model.AccountInfo;
import de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionCheck;
import de.unijena.bioinf.ms.nightsky.sdk.model.Subscription;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

public class ActMSNovelistConfigPanel extends ActivatableConfigPanel<SubToolConfigPanel<MsNovelistOptions>> {

    public ActMSNovelistConfigPanel(SiriusGui gui) {
        super(gui, "MSNovelist", Icons.DENOVO_32, false, () -> new SubToolConfigPanel<>(MsNovelistOptions.class) {});
        listener = evt -> checkConnection(((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck());
        gui.getConnectionMonitor().addConnectionStateListener(listener);
        checkConnection(gui.getConnectionMonitor().getCurrentCheckResult());
    }

    private void checkConnection(@Nullable ConnectionCheck checkResult) {
        if (checkResult == null)
            return;

        boolean connected = isConnected(checkResult);
        if (!connected) {
            setButtonEnabled(false);
            return;
        }

        AccountInfo info = gui.getSiriusClient().account().getAccountInfo(true);

        Subscription sub = info.getSubscriptions() == null ? null : info.getSubscriptions()
                .stream()
                .filter(s -> s.getSid() != null)
                .filter(s -> s.getSid().equals(info.getActiveSubscriptionId())).findFirst()
                .orElse(null);
        boolean isExplorerLic = sub != null && Optional.ofNullable(sub.getServiceUrl()).map(s -> s.contains("agilent")).orElse(false) && !sub.getSid().equals("sub|888983e6-da01-4af7-b431-921c59f0028f");

        setButtonEnabled(!isExplorerLic, "Your Subscription does not contain the de novo structure generation feature.");
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
    protected void setButtonEnabled(boolean enabled) {
        setButtonEnabled(enabled, "Can't connect to prediction server!");
    }

}
