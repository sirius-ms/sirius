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

import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class UserLoginDialog extends JDialog {
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final AuthService service;

    private boolean performedLogin = false;

    Action signInAction;
    Action cancelAction;

    public UserLoginDialog(Frame owner, AuthService service) {
        super(owner, true);
        this.service = service;
        build();
    }

    public UserLoginDialog(Dialog owner, AuthService service) {
        super(owner, true);
        this.service = service;
        build();
    }

    private void build() {
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
        cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performedLogin = false;
                dispose();
            }
        };
        final JButton cancel = new JButton("Cancel");
        cancel.addActionListener(cancelAction);

        signInAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Jobs.runInBackgroundAndLoad(UserLoginDialog.this, "Logging in...", () -> {
                    try {
                        service.login(username.getText(), new String(password.getPassword()));
                        AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                        performedLogin = true;
                        Jobs.runEDTLater(UserLoginDialog.this::dispose);
                    } catch (Throwable ex) {
                        new ExceptionDialog(UserLoginDialog.this, (ex instanceof OAuth2AccessTokenErrorResponse)?((OAuth2AccessTokenErrorResponse) ex).getErrorDescription() : ex.getMessage(), "Login failed!");
                        try {
                            AuthServices.clearRefreshToken(service, ApplicationCore.TOKEN_FILE);
                        } catch (IOException ex2) {
                            LoggerFactory.getLogger(getClass()).warn("Error when cleaning login state!", ex2);
                        }
                        try {
                            Jobs.runEDTAndWait(() -> password.setText(null));
                        } catch (InvocationTargetException | InterruptedException ignored) {}
                    }
                });
            }
        };
        final JButton login = new JButton("Sign in");
        login.addActionListener(signInAction);

        Box buttons = Box.createHorizontalBox();
        buttons.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        buttons.add(new JButton(SiriusActions.SIGN_UP.getInstance()));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancel);
        buttons.add(login);

        add(buttons, BorderLayout.SOUTH);

        configureActions();

        setMinimumSize(new Dimension(350, getMinimumSize().height));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        String enterAction = "signIn";
        String escAction = "cancel";
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), enterAction);
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), escAction);
        getRootPane().getActionMap().put(enterAction, signInAction);
        getRootPane().getActionMap().put(escAction, cancelAction);
    }

    public boolean hasPerformedLogin() {
        return performedLogin;
    }
}
