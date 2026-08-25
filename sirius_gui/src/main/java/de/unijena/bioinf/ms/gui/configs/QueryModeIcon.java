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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * A branching-structure glyph for the query builder's mode: a trunk with two leaves in the
 * {@code advanced} state carries a third leaf branching off the lower one - the nesting that mode
 * allows - while the simple state stays one level deep. The glyph shows the CURRENT state (like
 * {@link InvertIcon}), the tooltip says what a click does. Colourable; authored in a 16-unit box
 * scaled to the icon size.
 */
public class QueryModeIcon extends FlatAbstractIcon {

    private boolean advanced;

    public QueryModeIcon(int size, @NotNull Color color, boolean advanced) {
        super(size, size, color); // FlatAbstractIcon applies `color` to the graphics before paintIcon
        this.advanced = advanced;
    }

    /** {@code true} draws the nested (advanced) structure, {@code false} the flat (simple) one. */
    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }

    public void setColor(@NotNull Color color) {
        this.color = color;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(width / 16.0, height / 16.0);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double trunkX = 3.4, upper = 4.6, lower = 11.4, stem = 8.6;
        // the trunk and the two branches it splits into: the shape both modes share
        g.draw(elbow(trunkX, upper, stem));
        g.draw(elbow(trunkX, lower, stem));
        g.draw(new java.awt.geom.Line2D.Double(trunkX, upper, trunkX, lower));
        leaf(g, stem, upper);

        if (!advanced) {
            leaf(g, stem, lower);
            return;
        }
        // advanced: the lower branch splits once more - a group inside a group
        double nestX = stem + 1.2, nestUpper = lower - 2.4, nestLower = lower + 2.4;
        g.draw(elbow(nestX, nestUpper, nestX + 3.2));
        g.draw(elbow(nestX, nestLower, nestX + 3.2));
        g.draw(new java.awt.geom.Line2D.Double(nestX, nestUpper, nestX, nestLower));
        leaf(g, nestX + 3.2, nestUpper);
        leaf(g, nestX + 3.2, nestLower);
    }

    /** The horizontal run from the trunk at {@code x} to a leaf at {@code endX}, on row {@code y}. */
    private static Path2D elbow(double x, double y, double endX) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y);
        p.lineTo(endX, y);
        return p;
    }

    /** A term at the end of a branch. */
    private static void leaf(Graphics2D g, double x, double y) {
        double r = 1.5;
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }
}
