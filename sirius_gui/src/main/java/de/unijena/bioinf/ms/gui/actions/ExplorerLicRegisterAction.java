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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
            ResultMessage resultMessage = Jobs.runInBackgroundAndLoad(popupOwner, "Checking for Explorer license...", () -> {
                Path siriusHome = Path.of(PropertyManager.getProperty("de.unijena.bioinf.sirius.homeDir"));
                Path checkerExe = Path.of(PropertyManager.getProperty("de.unijena.bioinf.sirius.explorer.licenseChecker", null, "ExplorerLicTester/ExplorerLicTester.exe"));

                Process proc = Runtime.getRuntime().exec(siriusHome.resolve(checkerExe).toAbsolutePath().toString());
                List<String> info;
                try (BufferedReader reader = proc.inputReader()) {
                    info = reader.lines().toList();
                }
                List<String> errorOutput;
                try (BufferedReader reader = proc.errorReader()) {
                    errorOutput = reader.lines().toList();
                }
                boolean finished = proc.waitFor(30, TimeUnit.SECONDS);
                int exitValue = proc.exitValue();

                String licenseInfo = info.stream().filter(l -> l.startsWith("LicenseInfo:")).findFirst()
                        .map(k -> k.split(":")[1]).orElse(null);

                if (licenseInfo != null && licenseInfo.length() > 20) { //information should be much longer than 20 chars.
                    GuiUtils.openURL(popupOwner, UserPortal.explorerLicURL(licenseInfo), "Register Explorer License", true);
                    return new ResultMessage(true);
                } else {
                    return new ResultMessage(false, Stream.of(
                                    Stream.of("Explorer license validation could not read license information.",
                                            finished ? "Finished with exit value: " + exitValue : "License validation did not finish.",
                                            "Provided information: "),
                                    info.stream(),
                                    !errorOutput.isEmpty() ? Stream.of("Error output: ") : Stream.empty(),
                                    errorOutput.stream())
                            .flatMap(s -> (Stream<String>) s).toArray(String[]::new));
                }
            }).awaitResult();
            if (!resultMessage.success) {
                log.warn(String.join(" | ", resultMessage.message));
                new WarningDialog(popupOwner, GuiUtils.formatToolTip("No valid MassHunter Explorer license found on your system. Please ensure that MassHunter Explorer is installed and activated.", "For details, please see the 'Log' in the top-right corner."));
            }
        } catch (ExecutionException ex) {
            log.error("Error when checking for MassHunter Explorer license.", ex);
            new ExceptionDialog(popupOwner, GuiUtils.formatToolTip("Error when checking for MassHunter Explorer license. Error: " + ex.getMessage(), " For details, please see the 'Log' in the top-right corner."));
        }
    }

    /**
     * if license retrieval is not successful, this provides an error message.
     */
    private static class ResultMessage {
        boolean success;
        String[] message;

        public ResultMessage(boolean success, String... message) {
            this.success = success;
            this.message = message;
        }


    }
}
