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

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * A half-filled circle - the classic "invert" glyph: the outline is drawn and the left semicircle is
 * filled, reading as tone inversion. Colourable; authored in a 16-unit box scaled to the icon size.
 */
public class InvertIcon extends FlatAbstractIcon {

    public InvertIcon(int size, @NotNull Color color) {
        super(size, size, color); // FlatAbstractIcon applies `color` to the graphics before paintIcon
    }

    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(width / 16.0, height / 16.0);

        double cx = 8, cy = 8, r = 5.6;
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        // fill the left half: an arc from 12 o'clock, 180 deg counter-clockwise (through 9 o'clock),
        // closed by the vertical diameter (CHORD)
        g.fill(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 90, 180, Arc2D.CHORD));
    }
}
