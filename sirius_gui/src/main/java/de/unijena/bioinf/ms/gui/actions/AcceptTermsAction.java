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

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class AcceptTermsAction extends AbstractGuiAction {

    public AcceptTermsAction(SiriusGui gui) {
        super("Accept Terms", gui);
        putValue(Action.SHORT_DESCRIPTION, "Accept Terms of Service and Privacy Policy of the current Webservice.");
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        Jobs.runInBackgroundAndLoad(mainFrame, "Accepting and Refreshing...", () -> {
            try {
                ApplicationCore.WEB_API().acceptTermsAndRefreshToken();
                return true;
            } catch (IOException ex) {
                LoggerFactory.getLogger(getClass()).warn("Error when accepting terms.", ex);
                return false;
            }finally {
                gui.getConnectionMonitor().checkConnection();
            }
        });
    }
}
