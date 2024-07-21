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

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.auth.UserPortal;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ExplorerLicRegisterAction extends AbstractAction {
    protected final Frame popupOwner;

    public ExplorerLicRegisterAction(Frame popupOwner) {
        super("Register Explorer");
        putValue(Action.SHORT_DESCRIPTION, "Check for an active MassHunter Explorer license on this PC and request the corresponding SIRIUS subscription.");
        this.popupOwner = popupOwner;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            boolean success = Jobs.runInBackgroundAndLoad(popupOwner, "Checking for Explorer license...", () -> {
                Path siriusHone = Path.of(PropertyManager.getProperty("de.unijena.bioinf.sirius.homeDir"));
                Path checkerExe = Path.of(PropertyManager.getProperty("de.unijena.bioinf.sirius.explorer.licenseChecker", null, "ExplorerLicTester/ExplorerLicTester.exe"));

                Process proc = Runtime.getRuntime().exec(siriusHone.resolve(checkerExe).toAbsolutePath().toString());
                try (BufferedReader reader = proc.inputReader()) {
                    String licenseInfo = reader.lines().filter(l -> l.startsWith("LicenseInfo:")).findFirst()
                            .map(k -> k.split(":")[1]).orElse(null);
                    if (licenseInfo != null) {
                        GuiUtils.openURL(popupOwner, UserPortal.explorerLicURL(licenseInfo), "Register Explorer License", true);
                        return true;
                    } else {
                        return false;
                    }
                }
            }).awaitResult();
            if (!success)
                new WarningDialog(popupOwner, GuiUtils.formatToolTip("No valid MassHunter Explorer license found on you system. Please ensure that MassHunter Explorer is installed and activated."));
        } catch (ExecutionException ex) {
            log.error("Error when checking for MassHunter Explorer license.", ex);
            new ExceptionDialog(popupOwner, GuiUtils.formatToolTip("Error when checking for MassHunter Explorer license. Error: " + ex.getMessage()));
        }
    }
}
