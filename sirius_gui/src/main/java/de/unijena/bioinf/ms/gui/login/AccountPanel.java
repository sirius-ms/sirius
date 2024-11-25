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
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.rest.ProxyManager;
import de.unijena.bioinf.webapi.Tokens;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.net.URL;


public class AccountPanel extends LoadablePanel {
    private final AuthService service;
    private JLabel userIconLabel, userInfoLabel;
    private JButton login, create, changeSub, registerExplorer;
    private ToolbarButton refresh;

    private SiriusGui gui;

    public AccountPanel(SiriusGui gui, AuthService service) {
        super();
        this.service = service;
        this.gui = gui;

        JPanel content = setAndGetContentPanel(new JPanel(new BorderLayout()));;
        content.setMinimumSize(new Dimension(500, 180));
        content.setOpaque(false);

        TwoColumnPanel center = new TwoColumnPanel();
        center.setOpaque(false);

        userIconLabel = new JLabel();
        userInfoLabel = new JLabel();

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);

        iconPanel.add(userIconLabel, BorderLayout.CENTER);
        refresh = new ToolbarButton(Icons.REFRESH.derive(32,32));
        refresh.setBorderPainted(false);
        refresh.setBackground(getBackground());


        refresh.addActionListener(e ->
                runInBackgroundAndLoad(() -> {
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
                GuiUtils.formatToolTip("Refresh access token (also reloads account an license information)."));

        Box right = Box.createHorizontalBox();
        right.add(Box.createHorizontalGlue());
        right.add(refresh);

        TwoColumnPanel infoPanel = TwoColumnPanel.of(userInfoLabel, right);
        infoPanel.setOpaque(false);

        center.add(iconPanel, infoPanel);
        center.addVerticalGlue();
        content.add(center, BorderLayout.CENTER);


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
        content.add(buttons, BorderLayout.SOUTH);

        reloadChanges();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (refresh != null)
            refresh.setBackground(bg);
    }

    public void reloadChanges() {
        runInBackgroundAndLoad(() -> {
            DecodedJWT userInfo = service.getToken().map(AuthService.Token::getDecodedIdToken).orElse(null);
            if (userInfo == null) {
                userIconLabel.setIcon(Icons.USER_NOT_LOGGED_IN.derive(128,128));
                userInfoLabel.setText("Please log in!");
                create.setAction(SiriusActions.SIGN_UP.getInstance(gui, true));
                login.setAction(SiriusActions.SIGN_IN.getInstance(gui, true));
                login.setBackground(Colors.CUSTOM_GREEN);
                login.setForeground(Color.WHITE);
                login.setFont(login.getFont().deriveFont(Font.BOLD));

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
                    LoggerFactory.getLogger(getClass()).warn("Could not load profile image: {}", e.getMessage());
                    userIconLabel.setIcon(Icons.USER_GREEN.derive(128,128));
                }
                userInfoLabel.setText("<html>Logged in as:<br><b>"
                        + userInfo.getClaim("email").asString() + "</b>"
                        + "<br>"
                        + "(" + userInfo.getClaim("sub").asString() + ")"
                        + "</html>");
                create.setAction(SiriusActions.MANAGE_ACCOUNT.getInstance(gui, true));
                login.setAction(SiriusActions.SIGN_OUT.getInstance(gui, true));
                login.setBackground(UIManager.getLookAndFeel().getDefaults().getColor("Button.background"));
                login.setForeground(UIManager.getLookAndFeel().getDefaults().getColor("Button.foreground"));
                login.setFont(login.getFont().deriveFont(Font.PLAIN));

            }
        });

    }

    public String name() {
        return "Account";
    }
}
