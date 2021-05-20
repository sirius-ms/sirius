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
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.settings.SettingsPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class AccountSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    private final Properties props;
    private final AuthService service;
    private JTextField webserverURL;
    private JLabel userIconLabel, userInfoLabel;

    public AccountSettingsPanel(Properties properties, AuthService service) {
        super();
        this.props = properties;
        this.service = service;
        buildPanel();
        refreshValues();
    }

    private void buildPanel() {
        webserverURL = new JTextField(props.getProperty("de.unijena.bioinf.fingerid.web.host", PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.host")));
        addNamed("Web service URL", webserverURL);
        addVerticalGlue();


        JButton login = new JButton();
//        login.setPreferredSize(new Dimension(128, login.getPreferredSize().height));
        userIconLabel = new JLabel();
        userInfoLabel = new JLabel();

        DecodedJWT userInfo = getLogin();
        if (userInfo == null) {
            userIconLabel.setIcon(Icons.USER_128);
            userInfoLabel.setText("Please Login!");
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


        add(userIconLabel, userInfoLabel, GuiUtils.LARGE_GAP, false);

//        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        buttons.add(login);
        add(login,null, GuiUtils.LARGE_GAP,true);
        addVerticalGlue();


        //todo register, login and clear button
        // save and cancel via parent panel
        // Server url, login state (Account info??) ->  user image =)
    }

    private DecodedJWT getLogin() {
        return Jobs.runInBackgroundAndLoad(SwingUtilities.getWindowAncestor(this), "Checking Login",
                () -> AuthServices.getIDToken(service)).getResult();
    }

    @Override
    public void saveProperties() {

    }

    @Override
    public String name() {
        return "Account";
    }
}
