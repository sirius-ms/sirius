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

package de.unijena.bioinf.ms.gui.configs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 11.10.16.
 */

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Colors {


    public final static Color ICON_BLUE = new Color(17, 145, 187);
    public final static Color ICON_GREEN = new Color(0, 191, 48);
    public final static Color ICON_RED = new Color(204, 71, 41);
    public final static Color ICON_YELLOW = new Color(255, 204, 0);

    public final static Color LIST_SELECTED_BACKGROUND = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
    public final static Color LIST_SELECTED_FOREGROUND = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
    public final static Color LIST_EVEN_BACKGROUND = Color.WHITE;
    public final static Color LIST_DISABLED_BACKGROUND = UIManager.getColor("ComboBox.background");
    public final static Color LIST_UNEVEN_BACKGROUND = new Color(213, 227, 238);
    public final static Color LIST_ACTIVATED_FOREGROUND = UIManager.getColor("List.foreground");
    public final static Color LIST_DEACTIVATED_FOREGROUND = Color.GRAY;
    public final static Color LIST_LIGHT_GREEN = new Color(161, 217, 155);
    public final static Color LIST_SELECTED_GREEN = new Color(49, 163, 84);

    public final static Color DB_LINKED = new Color(155, 166, 219);
    public final static Color DB_UNLINKED = Color.GRAY;
    public final static Color DB_CUSTOM = ICON_YELLOW;
    public final static Color DB_TRAINING = Color.BLACK;
    public final static Color DB_UNKNOWN = new Color(178,34,34);

    public final static Color CLASSIFIER_MAIN = new Color(0xe5f5e0);
    public final static Color CLASSIFIER_OTHER = new Color(0xdeebf7);
}
