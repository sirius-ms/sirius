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
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.rest.ProxyManager;
import de.unijena.bioinf.webapi.Tokens;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;


public class SubscriptionDialog extends JDialog {
    private static final String TITLE = "Select Subscription";

    private JComboBox<Subscription> comboBox;

    Action applyAction;
    Action cancelAction;

    private boolean performedChange = false;

    public SubscriptionDialog(SiriusGui gui, boolean modal, List<Subscription> subs) {
        super(gui.getMainFrame(), TITLE, modal);
        this.
        setLayout(new BorderLayout());
        comboBox = new JComboBox<>(subs.toArray(Subscription[]::new));
        comboBox.setRenderer(new SubscriptionHTMLRenderer(350));
        comboBox.setMaximumSize(new Dimension(comboBox.getMaximumSize().width, 200));
        comboBox.setMaximumRowCount(5);
        add(comboBox, BorderLayout.CENTER);

        //============= SOUTH =================
        cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performedChange = false;
                dispose();
            }
        };
        final JButton cancel = new JButton("Cancel");
        cancel.addActionListener(cancelAction);

        applyAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Jobs.runInBackgroundAndLoad(SubscriptionDialog.this, "Changing Subscription...", () -> {
                    try {
                        ProxyManager.withConnectionLock((ExFunctions.Runnable) () -> {
                            Subscription sub = (Subscription) comboBox.getSelectedItem();
                            SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty(Tokens.ACTIVE_SUBSCRIPTION_KEY, sub.getSid());
                            ApplicationCore.WEB_API.changeActiveSubscription(sub);
                            ProxyManager.reconnect();
                        });
                        performedChange = true;
                        Jobs.runEDTLater(SubscriptionDialog.this::dispose);
                    } catch (Exception ex) {
                        LoggerFactory.getLogger(getClass()).error("Error when changing active subscription!", ex);
                        new ExceptionDialog(SubscriptionDialog.this, (ex instanceof OAuth2AccessTokenErrorResponse)?((OAuth2AccessTokenErrorResponse) ex).getErrorDescription() : ex.getMessage(), "Login failed!");
                        performedChange = false;
                    } finally {
                       gui.getConnectionMonitor().checkConnectionInBackground();
                    }
                });
            }
        };
        final JButton apply = new JButton("Apply");
        apply.setToolTipText("Apply Selection. May reconnect SIRIUS.");
        apply.addActionListener(applyAction);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(apply);

        add(buttons, BorderLayout.SOUTH);

        configureActions();

//        setMinimumSize(new Dimension(350, getMinimumSize().height));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        String enterAction = "select-sub";
        String escAction = "cancel";
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), enterAction);
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), escAction);
        getRootPane().getActionMap().put(enterAction, applyAction);
        getRootPane().getActionMap().put(escAction, cancelAction);
    }

    public boolean hasPerformedChange() {
        return performedChange;
    }
}
