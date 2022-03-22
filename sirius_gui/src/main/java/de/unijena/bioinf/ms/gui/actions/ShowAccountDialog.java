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

import com.auth0.jwt.interfaces.DecodedJWT;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.login.AccountDialog;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ShowAccountDialog extends AbstractAction {

    public ShowAccountDialog() {
        super("Account");
        putValue(Action.LARGE_ICON_KEY, Icons.USER_32);
        putValue(Action.SHORT_DESCRIPTION, "Show user account information and settings.");

        MF.CONNECTION_MONITOR().addConnectionStateListener(evt -> {
            ConnectionMonitor.ConnetionCheck check = ((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck();
            setIcon(check);
        });

        Jobs.runInBackground(() -> setIcon(MF.CONNECTION_MONITOR().checkConnection()));
    }

    protected synchronized void setIcon(final @Nullable ConnectionMonitor.ConnetionCheck check) {
        if (check != null) {
            if (check.isLoggedIn()) {
                @Nullable DecodedJWT token = AuthServices.getIDToken(ApplicationCore.WEB_API.getAuthService());
                if (token == null) {
                    putValue(Action.LARGE_ICON_KEY, Icons.USER_32); //bad login
                    return;
                }

                try {
                    Image image = ImageIO.read(new URL(token.getClaim("picture").asString()));
                    image = Icons.makeEllipse(image);
                    image = Icons.scaledInstance(image, 32, 32);
                    putValue(Action.LARGE_ICON_KEY, new ImageIcon(image));
                    return;
                } catch (IOException e) {
                    putValue(Action.LARGE_ICON_KEY, Icons.USER_GREEN_32); //login is fine but image is broken
                    LoggerFactory.getLogger(getClass()).warn("Could not load User image from token. Using placeholder instead.");
                }
            } else {
                putValue(Action.LARGE_ICON_KEY, Icons.USER_32);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new AccountDialog(MF, ApplicationCore.WEB_API.getAuthService());
    }
}