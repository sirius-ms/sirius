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

import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import de.unijena.bioinf.ChemistryBase.utils.ExFunctions;
import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.auth.AuthServices;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.ActionJLabel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.webView.WebviewHTMLTextJPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.Term;
import de.unijena.bioinf.rest.ProxyManager;
import de.unijena.bioinf.webapi.Tokens;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class UserLoginDialog extends JDialog {
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final AuthService service;

    private boolean performedLogin = false;
    private final JCheckBox boxAcceptTerms = new JCheckBox();
    Action signInAction;
    Action cancelAction;

    public UserLoginDialog(SiriusGui gui, AuthService service) {
        super(gui.getMainFrame(), true);
        this.service = service;
        setTitle("Login");
        setLayout(new BorderLayout());

        //============= NORTH =================
        add(new DialogHeader(Icons.KEY_64), BorderLayout.NORTH);


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

        signInAction = new AbstractAction("Log in") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Jobs.runInBackgroundAndLoad(UserLoginDialog.this, "Logging in...", () -> {
                    try {
                        ProxyManager.withConnectionLock((ExFunctions.Runnable) () -> {
                            service.login(username.getText(), new String(password.getPassword()));
                            AuthServices.writeRefreshToken(service, ApplicationCore.TOKEN_FILE);
                            ApplicationCore.WEB_API.changeActiveSubscription(Tokens.getActiveSubscription(service.getToken().orElse(null)));
                            ProxyManager.reconnect();
                        });
                        performedLogin = true;
                        Jobs.runEDTLater(UserLoginDialog.this::dispose);
                        if (boxAcceptTerms.isSelected())
                            ApplicationCore.WEB_API.acceptTermsAndRefreshToken();
                    } catch (Throwable ex) {
                        LoggerFactory.getLogger(getClass()).error("Error during Login.", ex);
                        new ExceptionDialog(UserLoginDialog.this, (ex instanceof OAuth2AccessTokenErrorResponse) ? ((OAuth2AccessTokenErrorResponse) ex).getErrorDescription() : ex.getMessage(), "Login failed!");
                        try {
                            AuthServices.clearRefreshToken(service, ApplicationCore.TOKEN_FILE);
                        } catch (IOException ex2) {
                            LoggerFactory.getLogger(getClass()).warn("Error when cleaning login state!", ex2);
                        }
                        try {
                            Jobs.runEDTAndWait(() -> password.setText(null));
                        } catch (InvocationTargetException | InterruptedException ignored) {
                        }
                    } finally {
                        gui.getConnectionMonitor().checkConnection();
                    }
                });
            }
        };
        final JButton login = new JButton(signInAction);
        Box buttons = Box.createHorizontalBox();
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        buttons.add(new JButton(SiriusActions.SIGN_UP.getInstance(gui, true)));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancel);
        buttons.add(login);

        add(buttons, BorderLayout.SOUTH);

        //============= CENTER =================
        TwoColumnPanel center = new TwoColumnPanel();
        center.addNamed("Email", username);
        center.addNamed("Password", password);
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        flow.add(new ActionJLabel("Forgot your Password?", SiriusActions.RESET_PWD.getInstance(gui, true)));
        center.add(null,  flow, 0, true);

        if (PropertyManager.getBoolean("de.unijena.bioinf.webservice.login.terms", false))
            addTermsPanel(center);

        add(center, BorderLayout.CENTER);


        configureActions();

        setMinimumSize(new Dimension(400, getMinimumSize().height));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
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

    public void addTermsPanel(@NotNull TwoColumnPanel center) {
        List<Term> terms = ApplicationCore.WEB_API.getAuthService().getToken()
                .map(Tokens::getActiveSubscriptionTerms).orElse(List.of());

        if (!terms.isEmpty()) {
            boxAcceptTerms.setSelected(false);
            signInAction.setEnabled(false);
            boxAcceptTerms.addActionListener(evt -> signInAction.setEnabled(((JCheckBox) evt.getSource()).isSelected()));

            WebviewHTMLTextJPanel htmlPanel = new WebviewHTMLTextJPanel("I accept " + Term.toLinks(terms) + ".");
            htmlPanel.setPreferredSize(new Dimension(getPreferredSize().width, 40));
            center.add(boxAcceptTerms, htmlPanel, GuiUtils.MEDIUM_GAP, false);
            htmlPanel.load();
        }
    }
}
