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
import de.unijena.bioinf.ms.gui.settings.SettingsPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Properties;

public class AccountSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    private final Properties props;
    private final AuthService service;
    private JTextField webserverURL;
    private JLabel userIconLabel, userInfoLabel;
    private JButton login, reset;

    public AccountSettingsPanel(Properties properties, AuthService service) {
        super();
        this.props = properties;
        this.service = service;
        buildPanel();
    }

    private void buildPanel() {
        webserverURL = new JTextField(props.getProperty("de.unijena.bioinf.fingerid.web.host", PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.host")));
        addNamed("Web service URL", webserverURL);
        addVerticalGlue();

        login = new JButton();
        userIconLabel = new JLabel();
        userInfoLabel = new JLabel();

        reset = new JButton(SiriusActions.RESET_PWD.getInstance());

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(userIconLabel, BorderLayout.CENTER);

        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonContainer.add(reset);
        buttonContainer.add(login);
        iconPanel.add(buttonContainer, BorderLayout.SOUTH);
        add(iconPanel, userInfoLabel);
        addVerticalGlue();

        SiriusActions.SIGN_IN.getInstance().addPropertyChangeListener(evt -> reloadChanges());
        SiriusActions.SIGN_OUT.getInstance().addPropertyChangeListener(evt -> reloadChanges());
        reloadChanges();
    }

    private DecodedJWT getLogin() {
        return Jobs.runInBackgroundAndLoad(SwingUtilities.getWindowAncestor(this), "Checking Login",
                () -> AuthServices.getIDToken(service)).getResult();
    }

    @Override
    public void saveProperties() {

    }

    @Override
    public void reloadChanges() {
        SettingsPanel.super.reloadChanges();
        DecodedJWT userInfo = getLogin();
        if (userInfo == null) {
            userIconLabel.setIcon(Icons.USER_128);
            userInfoLabel.setText("Please log in!");
            login.setAction(SiriusActions.SIGN_IN.getInstance());
        } else {
            try {
                userIconLabel.setIcon(new ImageIcon(new URL(userInfo.getClaim("picture").asString())));
            } catch (Throwable e) {
                LoggerFactory.getLogger(getClass()).warn("Could not load profile image: " + e.getMessage());
                userIconLabel.setIcon(Icons.USER_128);
            }
            userInfoLabel.setText("<html>Logged in as:<br><b>" + userInfo.getClaim("email").asString() + "</b></html>");
            login.setAction(SiriusActions.SIGN_OUT.getInstance());
        }
    }

    @Override
    public String name() {
        return "Account";
    }
}
