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

import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

class ImportDatabaseDialog extends JDialog {
    private final DatabaseDialog databaseDialog;
    protected DatabaseImportConfigPanel configPanel;

    public ImportDatabaseDialog(DatabaseDialog databaseDialog) {
        this(databaseDialog, null);
    }

    public ImportDatabaseDialog(DatabaseDialog databaseDialog, @Nullable CustomDatabase db) {
        super(databaseDialog, db != null ? "Import into " + db.name() : "Create custom database", true);
        this.databaseDialog = databaseDialog;

        setPreferredSize(new Dimension(640, 480));

        configPanel = new DatabaseImportConfigPanel(db, databaseDialog.customDatabases.stream().map(SearchableDatabase::name).collect(Collectors.toSet()));
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

    protected void runImportJob() {
        try {
            try {
                Jobs.runEDTAndWait(() -> {
                    ConnectionMonitor.ConnectionCheck check = MF.CONNECTION_MONITOR().checkConnection();
                    if (!check.isConnected() || !check.isLoggedIn()) {
                        throw new RuntimeException("Not connected or logged in!");
                    }
                });
            } catch (RuntimeException e) {
                throw new ExecutionException(e);
            }

            List<String> command = new ArrayList<>();
            command.add(configPanel.toolCommand());
            command.addAll(configPanel.asParameterList());

            Jobs.runCommandAndLoad(command, null,
                            null, this,
                            "Importing into '" + configPanel.getDbFilePath() + "'...",
                            false)
                    .awaitResult();

            databaseDialog.whenCustomDbIsAdded(configPanel.getDbFilePath());
        } catch (ExecutionException ex) {
            LoggerFactory.getLogger(getClass()).error("Error during Custom DB import.", ex);

            if (ex.getCause() != null)
                new StacktraceDialog(this, ex.getCause().getMessage(), ex.getCause());
            else
                new StacktraceDialog(this, "Unexpected error when importing custom DB!", ex);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB import.", e);
            new StacktraceDialog(MF, "Fatal Error during Custom DB import.", e);
        }
    }
}
