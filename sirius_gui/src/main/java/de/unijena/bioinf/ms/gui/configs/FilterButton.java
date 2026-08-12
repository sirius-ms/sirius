/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * A filter-funnel {@link ToolbarButton} that tints its glyph to signal the active filter state: the
 * accent colour while a filter is active (the inverted accent while it is inverted) and an idle
 * colour otherwise. Encapsulates that colouring so hosts only call {@link #setFilterActive}.
 */
public class FilterButton extends ToolbarButton {

    private final FunnelIcon icon;
    private final Color idleColor;

    public FilterButton(int size, @Nullable Color idleColor, String tooltip, boolean borderless) {
        this(new FunnelIcon(size, idleColor != null ? idleColor : defaultIdleColor()), tooltip, borderless);
    }

    private FilterButton(FunnelIcon icon, String tooltip, boolean borderless) {
        super(icon, tooltip, borderless);
        this.icon = icon;
        this.idleColor = icon.getColor();
    }

    /** Tints the funnel to reflect the active filter state (accent / inverted accent / idle). */
    public void setFilterActive(boolean active, boolean inverted) {
        icon.setColor(active
                ? (inverted ? Colors.Menu.FILTER_BUTTON_INVERTED : Colors.Menu.FILTER_BUTTON)
                : idleColor);
        repaint();
    }

    /** FlatLaf's search-field clear-icon colour, or the data foreground if the L&F does not define it. */
    @NotNull
    private static Color defaultIdleColor() {
        Color c = UIManager.getColor("SearchField.clearIconColor");
        return c != null ? c : Colors.FOREGROUND_DATA;
    }
}
