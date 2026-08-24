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

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A {@link FlowLayout} whose preferred size accounts for wrapping, so a panel of chips can grow in
 * height instead of being cut off. Classic Swing workaround (FlowLayout itself reports the
 * single-row width as preferred size).
 * <p>
 * On top of that it can keep a rectangle in the target's top-right corner free (see
 * {@link #setReservedTopRight}), so a component overlaid on that corner narrows only the rows it
 * actually covers instead of stealing a full-height column from every row.
 * <p>
 * Rows are laid out left-to-right; the alignment applies within a row's available width.
 */
public class WrapLayout extends FlowLayout {

    /**
     * Supplies the size of the rectangle kept free in the target's top-right corner (measured from
     * the target's top edge), or {@code null} / a zero size for "reserve nothing".
     */
    private @Nullable Supplier<Dimension> reservedTopRight;

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * Keeps the target's top-right corner free: every row reaching into the reserved height wraps
     * that much earlier, all rows below it use the full width. Meant for a component painted over
     * that corner (e.g. a button bar) that should only narrow the first line.
     */
    public void setReservedTopRight(@Nullable Supplier<Dimension> reservedTopRight) {
        this.reservedTopRight = reservedTopRight;
    }

    private Dimension reserved() {
        Dimension d = reservedTopRight == null ? null : reservedTopRight.get();
        return d == null ? new Dimension(0, 0) : d;
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

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int hgap = getHgap();
            int maxWidth = target.getWidth() - (insets.left + insets.right + hgap * 2);
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            for (Row row : rows(target, true, maxWidth)) {
                int x = insets.left + hgap + rowIndent(row, ltr);
                for (Component m : row.items) {
                    Dimension d = m.getPreferredSize();
                    // like FlowLayout: components are centred within their row
                    m.setBounds(x, row.top + (row.height - d.height) / 2, d.width, d.height);
                    x += d.width + hgap;
                }
            }
        }
    }

    /** The free space an alignment other than left puts in front of a row's first component. */
    private int rowIndent(Row row, boolean ltr) {
        int free = Math.max(0, row.available - row.width);
        return switch (getAlignment()) {
            case RIGHT -> free;
            case CENTER -> free / 2;
            case LEADING -> ltr ? 0 : free;
            case TRAILING -> ltr ? free : 0;
            default -> 0; // LEFT
        };
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

            List<Row> rows = rows(target, preferred, maxWidth);
            Dimension dim = new Dimension(0, 0);
            for (Row row : rows)
                // a narrowed row only fits its content next to the reserved corner, so the corner
                // is part of the width that row needs
                dim.width = Math.max(dim.width, row.width + (maxWidth - row.available));
            Row last = rows.get(rows.size() - 1);
            dim.height = last.top + last.height + vgap + insets.bottom;

            dim.width += horizontalInsetsAndGap;

            // when inside a scroll pane, do not count the scroll pane's own width
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null && target.isValid())
                dim.width -= (hgap + 1);
            return dim;
        }
    }

    /**
     * Breaks the target's visible children into rows at {@code maxWidth}, honouring the reserved
     * top-right corner. Single source of truth for both the reported size and the actual layout, so
     * the two cannot drift apart. Always returns at least one (possibly empty) row.
     */
    private List<Row> rows(Container target, boolean preferred, int maxWidth) {
        int hgap = getHgap();
        int vgap = getVgap();
        Insets insets = target.getInsets();
        Dimension reserved = reserved();

        List<Row> rows = new ArrayList<>();
        Row row = new Row(insets.top + vgap, maxWidth, reserved);
        for (int i = 0; i < target.getComponentCount(); i++) {
            Component m = target.getComponent(i);
            if (!m.isVisible())
                continue;
            Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
            int needed = row.items.isEmpty() ? d.width : row.width + hgap + d.width;
            if (needed > row.available) {
                if (row.items.isEmpty()) {
                    // nothing placed yet and it still does not fit: too wide to sit beside the
                    // reserved corner, so let the row start below the corner instead of wrapping
                    // into an empty row (if it does not fit the full width either it overflows,
                    // exactly as FlowLayout does)
                    row.moveBelow(reserved, maxWidth, vgap);
                } else {
                    rows.add(row);
                    row = new Row(row.top + row.height + vgap, maxWidth, reserved);
                }
                needed = d.width;
            }
            row.width = needed;
            row.height = Math.max(row.height, d.height);
            row.items.add(m);
        }
        rows.add(row);
        return rows;
    }

    /** One laid-out row: where it starts, how wide it may grow and what sits on it. */
    private static final class Row {
        private final List<Component> items = new ArrayList<>();
        private int top;
        private int available;
        private int width;
        private int height;

        Row(int top, int maxWidth, Dimension reserved) {
            this.top = top;
            this.available = availableWidth(top, maxWidth, reserved);
        }

        void moveBelow(Dimension reserved, int maxWidth, int vgap) {
            top = Math.max(top, reserved.height + vgap);
            available = availableWidth(top, maxWidth, reserved);
        }

        private static int availableWidth(int top, int maxWidth, Dimension reserved) {
            return reserved.width > 0 && top < reserved.height
                    ? Math.max(0, maxWidth - reserved.width)
                    : maxWidth;
        }
    }
}
