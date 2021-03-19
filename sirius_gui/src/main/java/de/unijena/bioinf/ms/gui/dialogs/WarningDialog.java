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

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class WarningDialog extends DoNotShowAgainDialog {


    public WarningDialog(Window owner, String warning) {
        this(owner, warning, null);
    }

    /**
     * @param owner       see JDialog
     * @param warning     Warning of this dialog
     * @param propertyKey name of the property with which the 'don't ask' flag is saved persistently
     */
    public WarningDialog(Window owner, String warning, String propertyKey) {
        this(owner, "", warning, propertyKey);
    }

    public WarningDialog(Window owner, String title, String warning, String propertyKey) {
        this(owner, title, () -> warning, propertyKey);
    }

    public WarningDialog(Window owner, String title, Supplier<String> messageProvider, String propertyKey) {
        super(owner, title, messageProvider, propertyKey);
        this.setVisible(true);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        boxedButtonPanel.add(Box.createHorizontalGlue());
        addOKButton(boxedButtonPanel);
    }

    protected void addOKButton(JPanel boxedButtonPanel) {
        final JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            saveDoNotAskMeAgain();
            dispose();
        });

        boxedButtonPanel.add(ok);
    }

    @Override
    protected Icon makeDialogIcon() {
        return UIManager.getIcon("OptionPane.warningIcon");
    }
}
