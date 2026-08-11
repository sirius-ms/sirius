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

package de.unijena.bioinf.ms.gui.utils.search;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * A small monochrome filter-funnel glyph (the universal "filter" symbol), drawn with Java2D so it is
 * crisp at any size and can be tinted to any colour (e.g. the accent while a filter is active). Used
 * as the in-field "open filter panel" affordance of the collapsed search bar and the overlay. A
 * funnel reads as "narrow the list down", which is exactly what the filter panel does.
 */
public final class FunnelIcon implements Icon {
    private final int size;
    private Color color;

    public FunnelIcon(int size, @NotNull Color color) {
        this.size = size;
        this.color = color;
    }

    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            double u = size / 16.0; // author in a 16-unit box, scaled to the icon size
            Path2D funnel = new Path2D.Double();
            funnel.moveTo(2.5 * u, 3.0 * u);   // wide top-left
            funnel.lineTo(13.5 * u, 3.0 * u);  // wide top-right
            funnel.lineTo(9.5 * u, 8.5 * u);   // taper to the right of the stem
            funnel.lineTo(9.5 * u, 13.0 * u);  // stem bottom-right (lower)
            funnel.lineTo(6.5 * u, 11.5 * u);  // stem bottom-left (higher -> classic angled foot)
            funnel.lineTo(6.5 * u, 8.5 * u);   // taper to the left of the stem
            funnel.closePath();
            g2.setColor(color);
            g2.fill(funnel);
        } finally {
            g2.dispose();
        }
    }
}
