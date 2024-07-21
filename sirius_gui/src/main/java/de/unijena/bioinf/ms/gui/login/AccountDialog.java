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

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AccountDialog extends JDialog implements PropertyChangeListener {
    private final Action signIn;
    private final Action signOut;
    private AccountPanel center;

    public AccountDialog(SiriusGui gui, AuthService service) {
        super(gui.getMainFrame(), true);
        setTitle("Account");
        setLayout(new BorderLayout());

        //============= NORTH =================
//        add(new DialogHeader(Icons.USER_64), BorderLayout.NORTH);


        //============= CENTER =================
        center = new AccountPanel(gui, service);
        add(center, BorderLayout.CENTER);

        signIn = SiriusActions.SIGN_IN.getInstance(gui, true);
        signIn.addPropertyChangeListener(this);

        signOut = SiriusActions.SIGN_OUT.getInstance(gui, true);
        signOut.addPropertyChangeListener(this);

        configureActions();

        setMinimumSize(new Dimension(500, getMinimumSize().height));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
        setVisible(true);
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        String escAction = "cancel";
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), escAction);
        getRootPane().getActionMap().put(escAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        if (signIn != null)
            signIn.removePropertyChangeListener(this);
        if (signOut != null)
            signOut.removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == signIn || e.getSource() == signOut)
            center.reloadChanges();
    }
}



