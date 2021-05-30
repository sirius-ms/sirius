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
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.webView.WebViewBrowserDialog;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SignUpAction extends AbstractAction {

    public SignUpAction() {
        super("Sign up");
//        putValue(Action.LARGE_ICON_KEY, Icons.GEAR_32);
        putValue(Action.SHORT_DESCRIPTION, "Create a new SIRIUS Account.");
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        try {
            new WebViewBrowserDialog(MF, "Sign up", ApplicationCore.WEB_API.getSignUpURL());
        } catch (Exception ex2) {
            LoggerFactory.getLogger(getClass()).error("Could not Open SignUp page in System Browser", ex2);
            new ExceptionDialog(MF, "Could not Open SignUp page in System Browser: " + ex2.getMessage());
        }
    }
}
