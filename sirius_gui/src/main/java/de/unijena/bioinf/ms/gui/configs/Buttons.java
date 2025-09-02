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

package de.unijena.bioinf.ms.gui.configs;

import de.unijena.bioinf.ms.gui.utils.ToolbarButton;

import javax.swing.*;

import static de.unijena.bioinf.ms.gui.configs.Icons.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Buttons {

    public static JButton getExportButton24(String tootip) {
        return new ToolbarButton(EXPORT.derive(24,24), tootip);
    }

    public static ToolbarButton getExportButton16(String tootip) {
        return new ToolbarButton(EXPORT.derive(16,16), tootip);
    }

    public static ToolbarButton getZoomInButton24() {
        return new ToolbarButton(Zoom_In.derive(24,24), "Zoom in");
    }

    public static ToolbarButton getZoomOutButton24() {
        return new ToolbarButton(Zoom_Out.derive(24,24), "Zoom out");
    }

    public static ToolbarButton getEditButton16(String tootip) {
        return new ToolbarButton(LIST_EDIT.derive(16,16), tootip);
    }

    public static ToolbarButton getEditButton16() {
        return getEditButton16( "add");
    }

    public static ToolbarButton getAddButton16(String tootip) {
        return new ToolbarButton(LIST_ADD.derive(16,16), tootip);
    }

    public static ToolbarButton getAddButton16() {
        return getAddButton16("add");
    }

    public static ToolbarButton getRemoveButton16() {
        return getRemoveButton16("remove");
    }

    public static ToolbarButton getRemoveButton16(String tootip) {
        return new ToolbarButton(LIST_REMOVE.derive(16,16), tootip);
    }

    public static ToolbarButton getFileChooserButton16() {
        return new ToolbarButton(FOLDER_FILE.derive(16,16), "choose file/dir");
    }

    public static ToolbarButton getFileChooserButton16(String tootip) {
        return new ToolbarButton(FOLDER_FILE.derive(16,16), tootip);
    }
}
