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
 * A horizontal expand/collapse arrows glyph: the arrows point outward ({@code <-|->}) in the
 * {@code expand} state and inward ({@code ->|<-}) otherwise. The meaning (current state vs. the
 * action a click performs) is left to the caller - for an action-oriented toggle, show {@code expand}
 * while the target is collapsed so the icon depicts what clicking will do. Colourable; authored in a
 * 16-unit box scaled to the icon size.
 */
public class CompactToggleIcon extends FlatAbstractIcon {

    private boolean expand;

    public CompactToggleIcon(int size, @NotNull Color color, boolean expand) {
        super(size, size, color); // FlatAbstractIcon applies `color` to the graphics before paintIcon
        this.expand = expand;
    }

    /** {@code true} draws the outward (expand) arrows; {@code false} the inward (collapse) arrows. */
    public void setExpand(boolean expand) {
        this.expand = expand;
    }

    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(width / 16.0, height / 16.0);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double mid = 8, hh = 2.6; // vertical centre and arrow-head half-height
        if (expand) {
            // <- -> : arrows point outward
            g.draw(new Line2D.Double(2.5, mid, 7, mid));
            g.draw(head(4.9, mid - hh, 2.5, mid, 4.9, mid + hh));
            g.draw(new Line2D.Double(9, mid, 13.5, mid));
            g.draw(head(11.1, mid - hh, 13.5, mid, 11.1, mid + hh));
        } else {
            // -> <- : arrows point inward
            g.draw(new Line2D.Double(2.5, mid, 7, mid));
            g.draw(head(4.6, mid - hh, 7, mid, 4.6, mid + hh));
            g.draw(new Line2D.Double(13.5, mid, 9, mid));
            g.draw(head(11.4, mid - hh, 9, mid, 11.4, mid + hh));
        }
    }

    private static Path2D head(double x1, double y1, double x2, double y2, double x3, double y3) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        return p;
    }
}
