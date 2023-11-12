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

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ConnectionDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * Created by fleisch on 08.06.17.
 */
@ThreadSafe
public class CheckConnectionAction extends AbstractMainFrameAction {

    protected CheckConnectionAction(MainFrame mainFrame) {
        super("Webservice", mainFrame);
        putValue(Action.SHORT_DESCRIPTION, "Check and refresh webservice connection");

        MF.CONNECTION_MONITOR().addConnectionStateListener(evt -> {
            ConnectionMonitor.ConnectionCheck check = ((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck();
            Jobs.runEDTLater(() -> {
                setIcon(check);
                if (!check.isConnected())
                    ConnectionDialog.of(MF, check.errors, check.workerInfo, check.licenseInfo);
            });
        });

        Jobs.runInBackground(() -> setIcon(MF.CONNECTION_MONITOR().checkConnection()));
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            ConnectionMonitor.ConnectionCheck r = checkConnectionAndLoad(MF);
            if (r != null) {
                setIcon(r);
                ConnectionDialog.of(MF, r.errors, r.workerInfo, r.licenseInfo);
            }
        } catch (Exception e1) {
            LoggerFactory.getLogger(getClass()).error("Error when checking connection by action", e1);
        }
    }


    public static ConnectionMonitor.ConnectionCheck checkConnectionAndLoad(MainFrame mainFrame) {
        return Jobs.runInBackgroundAndLoad(mainFrame, "Checking connection...",
                () -> mainFrame.CONNECTION_MONITOR().checkConnection()).getResult();
    }

    protected synchronized void setIcon(final @Nullable ConnectionMonitor.ConnectionCheck check) {

        if (check != null) {
            if (check.isConnected())
                putValue(Action.LARGE_ICON_KEY, Icons.NET_YES_32);
            else if (check.hasOnlyWarning())
                putValue(Action.LARGE_ICON_KEY, Icons.NET_WARN_32);
            else
                putValue(Action.LARGE_ICON_KEY, Icons.NET_NO_32);
        }
    }
}
