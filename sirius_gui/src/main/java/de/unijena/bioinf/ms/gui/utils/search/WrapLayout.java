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

import javax.swing.*;
import java.awt.*;

/**
 * A {@link FlowLayout} whose preferred size accounts for wrapping, so a panel of chips can grow in
 * height instead of being cut off. Classic Swing workaround (FlowLayout itself reports the
 * single-row width as preferred size).
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // wrap at the target's current width; fall back to its parent's width before the
            // first layout so the initial preferred size is already wrapped
            int targetWidth = target.getSize().width;
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null)
                container = container.getParent();
            targetWidth = container.getSize().width > 0 ? container.getSize().width : targetWidth;
            if (targetWidth == 0)
                targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible())
                    continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowWidth + d.width > maxWidth) {
                    addRow(dim, rowWidth, rowHeight, vgap);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0)
                    rowWidth += hgap;
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(dim, rowWidth, rowHeight, vgap);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;

            // when inside a scroll pane, do not count the scroll pane's own width
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null && target.isValid())
                dim.width -= (hgap + 1);
            return dim;
        }
    }

    private void addRow(Dimension dim, int rowWidth, int rowHeight, int vgap) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0)
            dim.height += vgap;
        dim.height += rowHeight;
    }
}
