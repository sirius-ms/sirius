/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.login.SubscriptionDialog;
import io.sirius.ms.utils.jwt.AccessTokens;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SelectActiveSubscriptionAction extends AbstractGuiAction {

    public SelectActiveSubscriptionAction(SiriusGui gui) {
        super("Change Subscription", gui);
        putValue(Action.SHORT_DESCRIPTION, "Specify subscription that shall be used for computations (active subscription). Might also change the Web Service host.");
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        boolean r = new SubscriptionDialog(gui, true, ApplicationCore.WEB_API().getAuthService().getToken().map(AccessTokens.ACCESS_TOKENS::getSubscriptions).orElse(List.of())).hasPerformedChange();
        if (r)
            firePropertyChange("change-sub", null, ApplicationCore.WEB_API().getActiveSubscription());
    }
}
