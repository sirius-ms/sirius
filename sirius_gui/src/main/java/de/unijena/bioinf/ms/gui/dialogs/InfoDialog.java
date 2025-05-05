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

package de.unijena.bioinf.ms.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class InfoDialog extends WarningDialog {

    public InfoDialog(JDialog owner, String info) {
        super(owner, info);
    }

    public InfoDialog(JDialog owner, String info, String propertyKey) {
        super(owner, info, propertyKey);
    }

    public InfoDialog(JDialog owner, String title, String info, String propertyKey) {
        super(owner, title, info, propertyKey);
    }

    public InfoDialog(JDialog owner, String title, Supplier<String> messageProvider, String propertyKey) {
        super(owner, title, messageProvider, propertyKey);
    }

    public InfoDialog(JDialog owner, String title, Supplier<String> messageProvider, String propertyKey, boolean setVisible) {
        super(owner, title, messageProvider, propertyKey, setVisible);
    }

    public InfoDialog(Window owner, String info) {
        super(owner, info);
    }

    public InfoDialog(Window owner, String info, String propertyKey) {
        super(owner, info, propertyKey);
    }

    public InfoDialog(Window owner, String title, String warning, String propertyKey) {
        super(owner, title, warning, propertyKey);
    }

    public InfoDialog(Window owner, String title, Supplier<String> messageProvider, String propertyKey) {
        super(owner, title, messageProvider, propertyKey);
    }

    public InfoDialog(Window owner, String title, Supplier<String> messageProvider, String propertyKey, boolean setVisible) {
        super(owner, title, messageProvider, propertyKey, setVisible);
    }

    @Override
    protected Icon makeDialogIcon() {
        return UIManager.getIcon("OptionPane.informationIcon");
    }
}
