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
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class UserPasswordResetDialog extends JDialog {
    private final JTextField email = new JTextField();
    private final AuthService service;

    private boolean performedReset = false;

    Action resetAction;
    Action cancelAction;

    public UserPasswordResetDialog(Frame owner, AuthService service) {
        super(owner, true);
        this.service = service;
        build();
    }

    public UserPasswordResetDialog(Dialog owner, AuthService service) {
        super(owner, true);
        this.service = service;
        build();
    }

    private void build() {
        setTitle("Reset Password");
        setLayout(new BorderLayout());

        //============= NORTH =================
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        header.add(new JLabel("<html><div style='text-align: center;'>Enter your email address and we will send you<br>instructions to reset your password.</div></html>"));
        add(header, BorderLayout.NORTH);


        //============= CENTER =================
        TwoColumnPanel center = new TwoColumnPanel();
        center.addNamed("Email", email);

        add(center, BorderLayout.CENTER);

        //============= SOUTH =================
        cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performedReset = false;
                dispose();
            }
        };
        final JButton cancel = new JButton("Cancel");
        cancel.addActionListener(cancelAction);

        resetAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Jobs.runInBackgroundAndLoad(UserPasswordResetDialog.this, "Sending password reset link...", () -> {
                    try {
                        service.sendPasswordReset(email.getText());
                        performedReset = true;
                        Jobs.runEDTLater(UserPasswordResetDialog.this::dispose);
                    } catch (Throwable ex) {
                        LoggerFactory.getLogger(getClass()).error("Error during password reset.",ex);
                        new ExceptionDialog(UserPasswordResetDialog.this, (ex instanceof OAuth2AccessTokenErrorResponse)?((OAuth2AccessTokenErrorResponse) ex).getErrorDescription() : ex.getMessage(), "Login failed!");
                        performedReset = false;
                    }
                });
            }
        };
        final JButton reset = new JButton("Send email");
        reset.setToolTipText("Send Password reset link to given email address.");
        reset.addActionListener(resetAction);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(reset);

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
        getRootPane().getActionMap().put(enterAction, resetAction);
        getRootPane().getActionMap().put(escAction, cancelAction);
    }

    public boolean hasPerformedReset() {
        return performedReset;
    }
}
