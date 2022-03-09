/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.login;

import com.auth0.jwt.interfaces.DecodedJWT;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class AccountPanel extends JPanel {
    private final AuthService service;
//    private JTextField webserverURL;
    private JLabel userIconLabel, userInfoLabel;
    private JButton login, reset, create;

    public AccountPanel(AuthService service) {
        super(new BorderLayout());
        this.service = service;
        buildPanel();
    }

    private void buildPanel() {
        TwoColumnPanel center = new TwoColumnPanel();

//        webserverURL = new JTextField(PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.host"));
//        webserverURL.setEditable(false);
//        center.addNamed("Web service URL", webserverURL);
//        center.addVerticalGlue();

        userIconLabel = new JLabel();
        userInfoLabel = new JLabel();

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(userIconLabel, BorderLayout.CENTER);

        center.add(iconPanel, userInfoLabel);
        center.addVerticalGlue();
        add(center, BorderLayout.CENTER);


        //south
        reset = new JButton(SiriusActions.RESET_PWD.getInstance());
        create = new JButton();
        login = new JButton();
        Box buttons = Box.createHorizontalBox();
        buttons.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        buttons.add(reset);
        buttons.add(create);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(login);
        add(buttons, BorderLayout.SOUTH);

        reloadChanges();
    }

    private DecodedJWT getLogin() {
        return Jobs.runInBackgroundAndLoad(SwingUtilities.getWindowAncestor(this), "Checking Login",
                () -> AuthServices.getIDToken(service)).getResult();
    }



    public void reloadChanges() {
        DecodedJWT userInfo = getLogin();
        if (userInfo == null) {
            userIconLabel.setIcon(Icons.USER_128);
            userInfoLabel.setText("Please log in!");
            create.setAction(SiriusActions.SIGN_UP.getInstance());
            login.setAction(SiriusActions.SIGN_IN.getInstance());
        } else {
            try {
                Image image = ImageIO.read(new URL(userInfo.getClaim("picture").asString()));//Toolkit.getDefaultToolkit().getImage();
                image = Icons.makeEllipse(image);

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
            create.setAction(SiriusActions.DELETE_ACCOUNT.getInstance());
            login.setAction(SiriusActions.SIGN_OUT.getInstance());
        }
    }

    public String name() {
        return "Account";
    }
}
