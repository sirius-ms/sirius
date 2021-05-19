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

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class LoginDialog extends JDialog {
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();

    public LoginDialog(Frame owner) {
        super(owner, true);
        setTitle("Login");
        setLayout(new BorderLayout());

        //============= NORTH =================
        add(new DialogHeader(Icons.KEY_64), BorderLayout.NORTH);

        //============= CENTER =================
        TwoColumnPanel center = new TwoColumnPanel();
        center.addNamed("Email", username);
        center.addNamed("Password", password);

        add(center, BorderLayout.CENTER);

        //============= SOUTH =================
        final JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        final JButton login = new JButton("Login");
        login.addActionListener(e -> {
            Jobs.runInBackgroundAndLoad(this, "Logging in...", () -> {
                final AuthService service = ApplicationCore.WEB_API.getAuthService();
                try {
                    service.login(username.getText(), new String(password.getPassword()));
                    AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                } catch (IOException | ExecutionException | InterruptedException ex) {
                    new StacktraceDialog(LoginDialog.this, "Error during Login!", ex);
                    try {
                        AuthServices.clearRefreshToken(service, ApplicationCore.TOKEN_FILE);
                    } catch (IOException ex2) {
                        LoggerFactory.getLogger(getClass()).warn("Error when cleaning login state!", ex2);
                    }
                }
            });
            dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(login);

        add(buttons, BorderLayout.SOUTH);
    }
}
