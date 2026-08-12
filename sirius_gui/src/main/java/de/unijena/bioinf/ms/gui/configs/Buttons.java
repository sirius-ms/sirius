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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static de.unijena.bioinf.ms.gui.configs.Icons.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class Buttons {

    public static JButton getExportButton24(String tootip) {
        return new ToolbarButton(EXPORT.derive(24,24), tootip);
    }

    public static ToolbarButton getExportButton16(String tootip) {
        return getExportButton(16, tootip);
    }

    public static ToolbarButton getExportButton(int size, String tootip) {
        return getExportButton(size, tootip, false);
    }

    public static ToolbarButton getExportButton(int size, String tootip, boolean borderless) {
        return new ToolbarButton(PLAIN_EXPORT.derive(size, size), tootip, borderless);
    }

    public static ToolbarButton getZoomInButton24() {
        return new ToolbarButton(Zoom_In.derive(24,24), "Zoom in");
    }

    public static ToolbarButton getZoomOutButton24() {
        return new ToolbarButton(Zoom_Out.derive(24,24), "Zoom out");
    }

    public static ToolbarButton getEditButton16(String tootip) {
        return getEditButton(16, tootip);
    }

    public static ToolbarButton getEditButton16() {
        return getEditButton16( "add");
    }

    public static ToolbarButton getEditButton(int size, String tootip) {
        return getEditButton(size, tootip, false);
    }

    public static ToolbarButton getEditButton(int size, String tootip, boolean borderless) {
        return new ToolbarButton(LIST_EDIT.derive(size, size), tootip, borderless);
    }

    public static ToolbarButton getAddButton16(String tootip) {
        return getAddButton(16, tootip);
    }

    public static ToolbarButton getAddButton16() {
        return getAddButton16("add");
    }

    public static ToolbarButton getAddButton(int size, String tootip) {
        return getAddButton(size, tootip, false);
    }

    public static ToolbarButton getAddButton(int size, String tootip, boolean borderless) {
        return new ToolbarButton(LIST_ADD.derive(size, size), tootip, borderless);
    }

    public static ToolbarButton getRemoveButton16() {
        return getRemoveButton16("remove");
    }

    public static ToolbarButton getRemoveButton16(String tootip) {
        return getRemoveButton(16, tootip);
    }

    public static ToolbarButton getRemoveButton(int size, String tootip) {
        return getRemoveButton(size, tootip, false);
    }

    public static ToolbarButton getRemoveButton(int size, String tootip, boolean borderless) {
        return new ToolbarButton(LIST_REMOVE.derive(size, size), tootip, borderless);
    }

    public static ToolbarButton getFileChooserButton16() {
        return new ToolbarButton(FOLDER_FILE.derive(16,16), "choose file/dir");
    }

    public static ToolbarButton getFileChooserButton16(String tootip) {
        return new ToolbarButton(FOLDER_FILE.derive(16,16), tootip);
    }

    public static ToolbarButton getDownloadButton16(String tootip) {
        return getDownloadButton(16, tootip);
    }

    public static ToolbarButton getDownloadButton(int size, String tootip) {
        return getDownloadButton(size, tootip, false);
    }

    public static ToolbarButton getDownloadButton(int size, String tootip, boolean borderless) {
        return new ToolbarButton(PLAIN_DOWNLOAD.derive(size, size), tootip, borderless);
    }

    public static ToolbarButton getPlainFolderButton16(String tootip) {
        return getPlainFolderButton(16, tootip);
    }

    public static ToolbarButton getPlainFolderButton(int size, String tootip) {
        return getPlainFolderButton(size, tootip, false);
    }

    public static ToolbarButton getPlainFolderButton(int size, String tootip, boolean borderless) {
        return new ToolbarButton(PLAIN_FOLDER.derive(size, size), tootip, borderless);
    }

    public static JButton getBackspaceButton16(String tooltip, boolean borderless) {
        return getBackspaceButton16(null, tooltip, borderless);
    }

    public static JButton getBackspaceButton16(@Nullable Color color, String tooltip, boolean borderless) {
        return getBackspaceButton(16, color, tooltip, borderless);
    }

    public static FilterButton getFilterButton(int size, String tootip, boolean borderless) {
        return getFilterButton(size, null, tootip, borderless);
    }

    public static FilterButton getFilterButton(int size, @Nullable Color idleColor, String tootip, boolean borderless) {
        return new FilterButton(size, idleColor, tootip, borderless);
    }

    /**
     * A borderless icon button showing a {@link BackspaceIcon} (a clear/reset affordance). The glyph
     * fills the button at {@code size} and is drawn in {@code color}; the button itself is transparent
     * at rest and animates its hover/pressed background from the theme (FlatLaf borderless button
     * type), so no parent-background needs to be supplied.
     */
    public static JButton getBackspaceButton(int size, @Nullable Color color, String tooltip, boolean borderless) {
        return new ToolbarButton(new BackspaceIcon(size, color != null ? color : defaultBackspaceColor()), tooltip, borderless);
    }

    public static JButton getBackspaceButton(int size, String tooltip, boolean borderless) {
        return getBackspaceButton(size, null, tooltip, borderless);
    }

    /** FlatLaf's search-field clear-icon color, or the data foreground if the L&F does not define it. */
    private static Color defaultBackspaceColor() {
        Color c = UIManager.getColor("SearchField.clearIconColor");
        return c != null ? c : Colors.FOREGROUND_DATA;
    }
}
