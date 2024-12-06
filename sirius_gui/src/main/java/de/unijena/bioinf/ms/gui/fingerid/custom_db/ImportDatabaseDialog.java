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

package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.compute.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import io.sirius.ms.sdk.jjobs.SseProgressJJob;
import io.sirius.ms.sdk.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;
import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isLoggedIn;

class ImportDatabaseDialog extends JDialog {

    public static final String DO_NOT_SHOW_AGAIN_KEY_CENTROIDED_WARNING = "de.unijena.bioinf.sirius.importDatabaseDialog.centroided.dontAskAgain";

    private static final String CENTROIDED_QUESTION = "SIRIUS supports only centroided mass spectra!<br>" +
            "Importing non-centroided spectra will negatively impact its performance.<br>" +
            "Are you sure the spectra are centroided and wish to continue?";

    private final DatabaseDialog databaseDialog;
    protected DatabaseImportConfigPanel configPanel;

    public ImportDatabaseDialog(@NotNull DatabaseDialog databaseDialog) {
        this(databaseDialog, null);
    }

    public ImportDatabaseDialog(@NotNull DatabaseDialog databaseDialog, @Nullable SearchableDatabase db) {
        super(databaseDialog, db != null ? "Import into " + db.getDatabaseId() : "Create custom database", true);
        this.databaseDialog = databaseDialog;

        setPreferredSize(new Dimension(640, 480));

        configPanel = new DatabaseImportConfigPanel(databaseDialog.getGui(), db);
        add(configPanel);

        configPanel.importButton.addActionListener(e -> {
            dispose();
            runImportJob();
        });

        GuiUtils.closeOnEscape(this);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (configPanel != null)
            configPanel.destroy();
    }

    protected void runImportJob() {
        try {
            if (configPanel.hasSpectraFiles() && new QuestionDialog(
                    databaseDialog.gui.getMainFrame(),
                    CENTROIDED_QUESTION,
                    DO_NOT_SHOW_AGAIN_KEY_CENTROIDED_WARNING
            ) {
                @Override
                protected void saveDoNotAskMeAgain() {
                    // always save success! (otherwise db import won't get started!)
                    if (dontAsk != null && property != null && !property.isBlank() && dontAsk.isSelected())
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(property, ReturnValue.Success.name());
                }
            }.isCancel()) {
                throw new CancellationException();
            }

            LoadingBackroundTask<Boolean> job = Jobs.runInBackgroundAndLoad(
                    databaseDialog.gui.getMainFrame(), "Checking Server Connection...", () -> {
                        ConnectionCheck check = databaseDialog.getGui().getConnectionMonitor().checkConnection();
                        return isConnected(check) && isLoggedIn(check);
                    });
            if (!job.getResult()) {
                throw new ExecutionException(new Exception("Not connected or logged in!"));
            }

            CommandSubmission command = new CommandSubmission();
            command.addCommandItem(configPanel.toolCommand());
            configPanel.asParameterList().forEach(command::addCommandItem);

            databaseDialog.gui.applySiriusClient((c, pid) -> {
                Job j = c.jobs().startCommand(pid, command, List.of(JobOptField.PROGRESS));
                return LoadingBackroundTask.runInBackground(databaseDialog.gui.getMainFrame(),
                        "Importing into '" + configPanel.getDbFilePath() + "'...", null,
                        new SseProgressJJob(databaseDialog.gui.getSiriusClient(), pid, j));
            }).awaitResult();
        } catch (Exception ex) {
            if (ex instanceof ExecutionException) {
                LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB import.", ex);
                if (ex.getCause() != null)
                    new StacktraceDialog(this, ex.getCause().getMessage(), ex.getCause());
                else
                    new StacktraceDialog(this, "Unexpected error when importing custom DB!", ex);
            } else if (!(ex instanceof CancellationException) && !(ex.getCause() instanceof CancellationException)) {
                LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB import.", ex);
                new StacktraceDialog(databaseDialog.getGui().getMainFrame(), "Fatal Error during Custom DB import.", ex);
            }

            if (new QuestionDialog(
                    databaseDialog.gui.getMainFrame(),
                    "Do you want to keep the incompletely imported database?").isCancel()) {

                databaseDialog.whenCustomDbIsAdded(configPanel.getDbFilePath()).map(SearchableDatabase::getDatabaseId)
                        .ifPresent(dbId -> Jobs.runInBackgroundAndLoad(databaseDialog.gui.getMainFrame(),
                                "Deleting database '" + dbId + "'...", () ->
                                        databaseDialog.gui.acceptSiriusClient((c, pid) -> c.databases().removeDatabase(dbId, true))
                        ).getResult());
            }
        } finally {
            databaseDialog.whenCustomDbIsAdded(configPanel.getDbFilePath());
        }
    }
}
