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

package de.unijena.bioinf.ms.gui.login;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.LoginException;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.rest.ProxyManager;
import de.unijena.bioinf.webapi.Tokens;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.net.URL;


public class AccountPanel extends JPanel {
    private final AuthService service;
    private JLabel userIconLabel, userInfoLabel;
    private JButton login, create, changeSub, registerExplorer;
    private ToolbarButton refresh;

    private SiriusGui gui;

    public AccountPanel(SiriusGui gui, AuthService service) {
        super(new BorderLayout());
        this.service = service;
        this.gui = gui;
        TwoColumnPanel center = new TwoColumnPanel();

        userIconLabel = new JLabel();
        userInfoLabel = new JLabel();

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(userIconLabel, BorderLayout.CENTER);
        refresh = new ToolbarButton(Icons.REFRESH_32);
        refresh.addActionListener(e ->
                Jobs.runInBackgroundAndLoad(SwingUtilities.getWindowAncestor(this), () -> {
                    try {
                        ProxyManager.withConnectionLock((ExFunctions.Runnable) () -> {
                            ApplicationCore.WEB_API.changeActiveSubscription(null);
                            AuthService.Token t = ApplicationCore.WEB_API.getAuthService().refreshIfNeeded(true);
                            ApplicationCore.WEB_API.changeActiveSubscription(Tokens.getActiveSubscription(t));
                            ProxyManager.reconnect();
                        });
                    } catch (LoginException ex) {
                        LoggerFactory.getLogger(getClass()).error("Error when refreshing access_token!", ex);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex); //should not happen
                    } finally {
                        gui.getConnectionMonitor().checkConnectionInBackground();
                    }
                }));
        refresh.setPreferredSize(new Dimension(45, 45));
        refresh.setToolTipText(
                GuiUtils.formatToolTip("Refresh access_token (also reloads account an license information)."));

        Box right = Box.createHorizontalBox();
        right.add(Box.createHorizontalGlue());
        right.add(refresh);

        center.add(iconPanel, TwoColumnPanel.of(userInfoLabel, right));
        center.addVerticalGlue();
        add(center, BorderLayout.CENTER);


        //south
        create = new JButton();
        login = new JButton();
        registerExplorer = new JButton(SiriusActions.REGISTER_EXPLORER.getInstance(gui, true));
        changeSub = new JButton(SiriusActions.SELECT_SUBSCRIPTION.getInstance(gui, true));
        Box buttons = Box.createHorizontalBox();
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons.add(create);
        buttons.add(Box.createHorizontalGlue());
        if (SystemUtils.IS_OS_WINDOWS)
            buttons.add(registerExplorer);
        buttons.add(changeSub);
        buttons.add(login);
        add(buttons, BorderLayout.SOUTH);

        reloadChanges();
    }

    private DecodedJWT getLogin() {
        return Jobs.runInBackgroundAndLoad(SwingUtilities.getWindowAncestor(this), "Checking Login",
                () -> service.getToken().map(AuthService.Token::getDecodedIdToken).orElse(null)).getResult();
    }


    public void reloadChanges() {
        DecodedJWT userInfo = getLogin();
        if (userInfo == null) {
            userIconLabel.setIcon(Icons.USER_128);
            userInfoLabel.setText("Please log in!");
            create.setAction(SiriusActions.SIGN_UP.getInstance(gui, true));
            login.setAction(SiriusActions.SIGN_IN.getInstance(gui, true));
            refresh.setEnabled(false);
            changeSub.setEnabled(false);
            registerExplorer.setEnabled(false);
        } else {
            refresh.setEnabled(true);
            changeSub.setEnabled(true);
            registerExplorer.setEnabled(true);
            try {
                Image image = ImageIO.read(new URL(userInfo.getClaim("picture").asString()));
                image = Icons.makeEllipse(image);
                image = Icons.scaledInstance(image, 128, 128);

                userIconLabel.setIcon(new ImageIcon(image));
            } catch (Throwable e) {
                LoggerFactory.getLogger(getClass()).warn("Could not load profile image: " + e.getMessage());
                userIconLabel.setIcon(Icons.USER_GREEN_128);
            }
            userInfoLabel.setText("<html>Logged in as:<br><b>"
                    + userInfo.getClaim("email").asString() + "</b>"
                    + "<br>"
                    + "(" + userInfo.getClaim("sub").asString() + ")"
                    + "</html>");
            create.setAction(SiriusActions.MANAGE_ACCOUNT.getInstance(gui, true));
            login.setAction(SiriusActions.SIGN_OUT.getInstance(gui, true));
        }
    }

    public String name() {
        return "Account";
    }
}
