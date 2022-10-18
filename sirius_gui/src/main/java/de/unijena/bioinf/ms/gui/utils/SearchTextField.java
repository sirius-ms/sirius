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

package de.unijena.bioinf.ms.gui.utils;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by fleisch on 18.05.17.
 */
public class SearchTextField extends TwoColumnPanel {
    public final PlaceholderTextField textField;

    public SearchTextField() {
        this("Type to search...");
    }

    public SearchTextField(@Nullable String placeHolder) {
        super();
        setBorder(new EmptyBorder(0, 0, 0, 0));
        textField = new PlaceholderTextField();
        textField.setPlaceholder(placeHolder);
        textField.setPreferredSize(new Dimension(115, textField.getPreferredSize().height));
        add(new JLabel("Filter"), textField);
    }
}
