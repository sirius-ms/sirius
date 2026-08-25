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
import java.awt.geom.Path2D;

/**
 * A crisp, colourable "restore defaults" glyph: a SINGLE circular arrow (~300°, with a gap at the
 * top) capped by an arrowhead, drawn with Java2D in a 16-unit box scaled to the icon size. Authored
 * as a single arrow on purpose so it is visually distinct from the two-arrow "refresh"/reload icon -
 * the same symbol should always mean the same action. Drawn vector-crisp (STROKE_PURE + a finely
 * sampled arc), tinted by the constructor colour; rollover/pressed feedback is left to the hosting
 * (borderless FlatLaf) button.
 */
public class RestoreDefaultsIcon extends FlatAbstractIcon {

    public RestoreDefaultsIcon(int size, @NotNull Color color) {
        super(size, size, color); // FlatAbstractIcon applies `color` to the graphics before paintIcon
    }

    /** Recolour the glyph (e.g. to tint it to an accent). */
    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(width / 16.0, height / 16.0); // author in a 16-unit box, scale to the icon size

        final double cx = 8.0, cy = 8.0, r = 5.1;
        // clockwise sweep (screen coords: y grows downward) leaving a ~60° gap at the top for the head
        final double startDeg = 300.0, sweepDeg = 300.0;

        // the ring: sampled into short segments so it stays smooth and crisp at any icon size
        Path2D ring = new Path2D.Double();
        final int steps = 72;
        for (int i = 0; i <= steps; i++) {
            double t = Math.toRadians(startDeg + sweepDeg * i / steps);
            double x = cx + r * Math.cos(t), y = cy + r * Math.sin(t);
            if (i == 0) ring.moveTo(x, y); else ring.lineTo(x, y);
        }
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(ring);

        // filled arrowhead at the arc's end, aligned to the (clockwise) tangent so it points into the
        // top gap - the closing head of the circular "reset" motion
        double tEnd = Math.toRadians(startDeg + sweepDeg);
        double ex = cx + r * Math.cos(tEnd), ey = cy + r * Math.sin(tEnd);
        double dx = -Math.sin(tEnd), dy = Math.cos(tEnd); // unit tangent (direction of motion)
        double px = -dy, py = dx;                         // unit perpendicular
        final double head = 3.0, halfW = 1.95;
        double tipx = ex + dx * head * 0.55, tipy = ey + dy * head * 0.55;
        double basex = ex - dx * head * 0.45, basey = ey - dy * head * 0.45;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(tipx, tipy);
        arrow.lineTo(basex + px * halfW, basey + py * halfW);
        arrow.lineTo(basex - px * halfW, basey - py * halfW);
        arrow.closePath();
        g.fill(arrow);
    }
}
