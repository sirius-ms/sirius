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

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class OpenOnlineDocumentationAction extends AbstractAction {
    protected final Frame popupOwner;
    public OpenOnlineDocumentationAction(Frame popupOwner) {
        super("Help");
        putValue(Action.LARGE_ICON_KEY, Icons.HELP.derive(32,32));
        putValue(Action.SHORT_DESCRIPTION,"Open online documentation");
        this.popupOwner = popupOwner;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String url = PropertyManager.getProperty("de.unijena.bioinf.sirius.docu.url");
        try {
            URI uri = new URI(url);
            try {
                GuiUtils.openURL(popupOwner, uri, "Open Online Documentation", true);
            } catch (IOException er) {
                LoggerFactory.getLogger(getClass()).error("Could not 'Online Documentation' in system browser, Try internal browser as fallback.", er);
                try {
                    GuiUtils.openURL(popupOwner, uri, "Open Online Documentation (internal)", false);
                } catch (IOException ex2) {
                    LoggerFactory.getLogger(getClass()).error("Could neither open 'Online Documentation' in system browser nor in internal Browser." +   System.lineSeparator() + "Please copy the url to your browser: " + uri, ex2);
                    new ExceptionDialog(popupOwner, "Could neither open 'Online Documentation' in system browser nor in SIRIUS' internal browser: " + ex2.getMessage() + System.lineSeparator() + "Please copy the url to your browser: " + uri);
                }
                LoggerFactory.getLogger(this.getClass()).error(er.getMessage(), er);
            }
        } catch (URISyntaxException ex) {
            new ExceptionDialog(popupOwner,"Malformed URL '" + url + "'. Cause: " + ex.getMessage());
        }
    }
}
