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

import de.unijena.bioinf.auth.UserPortal;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.login.UserPasswordResetDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URI;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PasswdResetAction extends AbstractUserPortalAction {

    public PasswdResetAction() {
        super("Reset Password");
        putValue(Action.SHORT_DESCRIPTION, "Open password reset dialog.");
    }

    @Override
    URI path() {
        return UserPortal.pwResetURL();
    }

    @Override
    @Deprecated(forRemoval = true) //todo use super method instead
    public synchronized void actionPerformed(ActionEvent e) {
        boolean r = new UserPasswordResetDialog(MF, ApplicationCore.WEB_API.getAuthService()).hasPerformedReset();
        firePropertyChange("pwd-reset", false, r);
    }
}
