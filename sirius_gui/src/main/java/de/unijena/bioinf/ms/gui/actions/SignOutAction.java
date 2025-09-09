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

import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.rest.ProxyManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SignOutAction extends AbstractGuiAction {

    public SignOutAction(SiriusGui gui) {
        super("Log out", gui);
        putValue(Action.SHORT_DESCRIPTION, "Logout from the current account.");
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        boolean r = Jobs.runInBackgroundAndLoad(mainFrame, "Logging out...", () -> {
            try {
                ProxyManager.withConnectionLock((ExFunctions.Runnable) () -> {
                    ApplicationCore.WEB_API().changeActiveSubscription(null);
                    AuthServices.clearRefreshToken(ApplicationCore.WEB_API().getAuthService(), ApplicationCore.TOKEN_FILE);
                    ProxyManager.reconnect();
                });
                return true;
            } catch (IOException ex) {
                LoggerFactory.getLogger(getClass()).warn("Error during logout!", ex);
                return false;
            } finally {
                gui.getConnectionMonitor().checkConnectionInBackground();
            }
        }).getResult();
        firePropertyChange("logout", null, r);
    }
}
