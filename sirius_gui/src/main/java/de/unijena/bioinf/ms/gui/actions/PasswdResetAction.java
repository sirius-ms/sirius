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
import de.unijena.bioinf.ms.gui.SiriusGui;

import javax.swing.*;
import java.net.URI;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PasswdResetAction extends AbstractUserPortalAction {

    public PasswdResetAction(SiriusGui gui) {
        super("Reset Password", gui);
        putValue(Action.SHORT_DESCRIPTION, "Open password reset dialog.");
    }

    @Override
    URI path() {
        return UserPortal.pwResetURL();
    }
}
