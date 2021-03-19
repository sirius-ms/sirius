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

import de.unijena.bioinf.ms.gui.utils.ToolbarButton;

import javax.swing.*;

import static de.unijena.bioinf.ms.gui.configs.Icons.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Buttons {

    public static JButton getExportButton24(String tootip) {
        return new ToolbarButton(EXPORT_24, tootip);
    }

    public static ToolbarButton getExportButton2(String tootip) {
        return new ToolbarButton(EXPORT_20, tootip);
    }

    public static ToolbarButton getZoomInButton24() {
        return new ToolbarButton(Zoom_In_24, "Zoom in");
    }

    public static ToolbarButton getZoomOutButton24() {
        return new ToolbarButton(Zoom_Out_24, "Zoom out");
    }

    public static ToolbarButton getAddButton16(String tootip) {
        return new ToolbarButton(LIST_ADD_16, tootip);
    }

    public static ToolbarButton getAddButton16() {
        return new ToolbarButton(LIST_ADD_16, "add");
    }

    public static ToolbarButton getRemoveButton16() {
        return new ToolbarButton(LIST_REMOVE_16, "remove");
    }

    public static ToolbarButton getRemoveButton16(String tootip) {
        return new ToolbarButton(LIST_REMOVE_16, tootip);
    }

    public static ToolbarButton getFileChooserButton16() {
        return new ToolbarButton(CHOOSE_FILE_16, "choose file/dir");
    }

    public static ToolbarButton getFileChooserButton16(String tootip) {
        return new ToolbarButton(CHOOSE_FILE_16, tootip);
    }
}
