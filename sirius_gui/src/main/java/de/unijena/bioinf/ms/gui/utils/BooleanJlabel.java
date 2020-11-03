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

package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;

/**
 * Created by fleisch on 05.06.17.
 */
public class BooleanJlabel extends JLabel {

    public void setState(boolean state) {
        setIcon(state ? Icons.YES_16:Icons.NO_16);
    }

    public boolean isTrue() {
        return getIcon().equals(Icons.YES_16);
    }

    public BooleanJlabel(boolean state) {
        setState(state);
    }

    public BooleanJlabel() {
        this(false);
    }
}
