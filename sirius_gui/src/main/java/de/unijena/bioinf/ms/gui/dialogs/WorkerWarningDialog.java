/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.net.ConnectionCheckPanel;

import javax.swing.*;
import java.awt.*;

public class WorkerWarningDialog extends WarningDialog {

    public static final String NEVER_ASK_AGAIN_KEY = PropertyManager.PROPERTY_BASE + ".sirius.dialog.check_worker_action.never_ask_again";


    public WorkerWarningDialog(Window owner) {
        this(owner, false);
    }

    public WorkerWarningDialog(Window owner, final boolean noWorkerInfoError) {
        super(owner
                , "<html>" + (
                        noWorkerInfoError
                                ? ConnectionCheckPanel.WORKER_INFO_MISSING_MESSAGE
                                : ConnectionCheckPanel.WORKER_WARNING_MESSAGE
                ) + "<br><br> See the Webservice information for details.</html>"
                , NEVER_ASK_AGAIN_KEY
        );
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        boxedButtonPanel.add(Box.createHorizontalGlue());
        addOpenConnectionDialogButton(boxedButtonPanel);
        addOKButton(boxedButtonPanel);
    }

    protected void addOpenConnectionDialogButton(JPanel boxedButtonPanel) {
        final JButton details = new JButton("Details");
        details.setToolTipText("Open Webservice information for details.");
        details.setAction(SiriusActions.CHECK_CONNECTION.getInstance());
        details.setIcon(Icons.NET_16);
        boxedButtonPanel.add(details);
    }
}
