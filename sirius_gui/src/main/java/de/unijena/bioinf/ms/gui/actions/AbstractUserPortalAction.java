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

import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;


public abstract class AbstractUserPortalAction extends AbstractAction {

    protected final Frame owner;

    public AbstractUserPortalAction(Frame owner) {
        super();
        this.owner = owner;
    }

    public AbstractUserPortalAction(String name, Frame owner) {
        super(name);
        this.owner = owner;
    }

    public AbstractUserPortalAction(String name, Icon icon, Frame owner) {
        super(name, icon);
        this.owner = owner;
    }

    abstract URI path();

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        try {
            GuiUtils.openURL(owner, path(), "Open User Portal", true);
        } catch (Exception ex2) {
            LoggerFactory.getLogger(getClass()).error("Could not Open 'User Portal' in system browser, Try internal browser as fallback.", ex2);
            try {
                GuiUtils.openURL(owner, path(), "Open User Portal (internal)", false);
            } catch (IOException ex) {
                LoggerFactory.getLogger(getClass()).error("Could neither open 'User Portal' in system browser nor in internal Browser." +   System.lineSeparator() + "Please copy the url to your browser: " + path(), ex2);
                new ExceptionDialog(owner, "Could neither open 'User Portal' in system browser nor in internal Browser: " + ex2.getMessage() + System.lineSeparator() + "Please copy the url to your browser: " + path());
            }
        }
    }

}
