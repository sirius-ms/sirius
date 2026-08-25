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
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * A general, colourable backspace/delete glyph (a left-pointing tag with an {@code x}) drawn with
 * Java2D. Sized and coloured via the constructor and authored in a 16-unit box scaled to the icon
 * size. Rollover/pressed feedback is left to the hosting button (e.g. a borderless FlatLaf button
 * whose background animates on hover), so the icon itself is a single flat colour.
 */
public class BackspaceIcon extends FlatAbstractIcon {

    public BackspaceIcon(int size, @NotNull Color color) {
        super(size, size, color); // FlatAbstractIcon applies `color` to the graphics before paintIcon
    }

    /** Recolour the glyph (e.g. to tint it to an accent). */
    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(width / 16.0, height / 16.0); // author in a 16-unit box, scale to the icon size

        // backspace-key outline: pointed on the left, rounded right corners; nearly fills the box
        double r = 2.6, top = 2.0, bottom = 14.0, right = 14.2, shoulderX = 6.4, tipX = 1.2, midY = 8.0;
        Path2D tag = new Path2D.Float();
        tag.moveTo(shoulderX, top);
        tag.lineTo(right - r, top);
        tag.quadTo(right, top, right, top + r);        // top-right round corner
        tag.lineTo(right, bottom - r);
        tag.quadTo(right, bottom, right - r, bottom);  // bottom-right round corner
        tag.lineTo(shoulderX, bottom);
        tag.lineTo(tipX, midY);                        // left point
        tag.closePath();

        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(tag);
        // the x inside
        g.draw(new Line2D.Float(7.5f, 6.4f, 10.6f, 9.6f));
        g.draw(new Line2D.Float(10.6f, 6.4f, 7.5f, 9.6f));
    }
}
