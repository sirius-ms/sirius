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
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.login.AccountDialog;
import de.unijena.bioinf.ms.gui.net.ConnectionChecks;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import io.sirius.ms.sdk.model.ConnectionCheck;
import de.unijena.bioinf.webapi.Tokens;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ShowAccountDialog extends AbstractGuiAction {

    public ShowAccountDialog(SiriusGui gui) {
        super("Account", gui);
        putValue(Action.LARGE_ICON_KEY, Icons.USER_NOT_LOGGED_IN.derive(32,32));
        putValue(Action.SHORT_DESCRIPTION, "Show user account information and settings.");

        this.gui.getConnectionMonitor().addConnectionStateListener(evt -> {
            ConnectionCheck check = ((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck();
            setIcon(check);
        });

        Jobs.runInBackground(() -> setIcon(this.gui.getConnectionMonitor().checkConnection()));
    }

    protected synchronized void setIcon(final @Nullable ConnectionCheck check) {
        if (check != null) {
            if (ConnectionChecks.isLoggedIn(check)) {
                URI imageURI = ApplicationCore.WEB_API.getAuthService().getToken()
                        .flatMap(Tokens::getUserImage).orElse(null);

                if (imageURI == null) {
                    putValue(Action.LARGE_ICON_KEY, Icons.USER.derive(32,32)); //bad login
                    return;
                }

                try {
                    Image image = ImageIO.read(imageURI.toURL());
                    image = Icons.makeEllipse(image);
                    image = Icons.scaledInstance(image, 32, 32);
                    putValue(Action.LARGE_ICON_KEY, new ImageIcon(image));
                } catch (IOException e) {
                    putValue(Action.LARGE_ICON_KEY, Icons.USER_GREEN.derive(32,32)); //login is fine but image is broken
                    LoggerFactory.getLogger(getClass()).warn("Could not load User image from token. Using placeholder instead.");
                }
            } else {
                putValue(Action.LARGE_ICON_KEY, Icons.USER_NOT_LOGGED_IN.derive(32,32));
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new AccountDialog(gui, ApplicationCore.WEB_API.getAuthService());
    }
}
