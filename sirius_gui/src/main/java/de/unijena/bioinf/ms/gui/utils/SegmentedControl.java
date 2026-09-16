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

package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Colors;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

/**
 * What the segmented controls share: adjacent toggle segments in one capsule of which exactly one is
 * selected, so all options are visible and each is one click away. A subclass creates its segments with
 * {@link #segment} and hands them to {@link #addSegment} in display order; what the segments mean is its
 * business.
 * <p>
 * Painting is done here rather than by the segments: the buttons only draw their (look-and-feel) text,
 * while this panel draws the capsule, the selected cell in the filter accent
 * ({@link Colors.Menu#FILTER_BUTTON}, the same accent the search-bar chips use), the hover cell and the
 * separators between two unselected neighbours. That is what makes the segments look like one control
 * instead of a row of buttons.
 */
public abstract class SegmentedControl extends JPanel {

    /** Extra room left and right of a segment label; the vertical margin stays the look and feel's. */
    protected static final int SEGMENT_PADDING = 12;

    private final Supplier<String> description;
    private final ButtonGroup group = new ButtonGroup();

    /**
     * @param description describes the criterion; asked for whenever a tooltip is shown, so a description
     *                    that is only fetched later (e.g. from the search index) appears
     */
    protected SegmentedControl(@NotNull Supplier<String> description) {
        this.description = description;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        // NOT setToolTipText: that only registers a component whose previous text was null, and our
        // getToolTipText() never returns null - so it would silently never show a tooltip at all
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /** Adds a segment created with {@link #segment} as the next one to the right, directly beside the last. */
    protected void addSegment(@NotNull JToggleButton segment) {
        group.add(segment);
        add(segment); // no gaps: the segments sit directly next to each other inside one capsule
        // selection and hover are painted by this panel, so both need a repaint
        segment.getModel().addChangeListener(e -> {
            updateSegmentForegrounds();
            repaint();
        });
    }

    protected static void fixWidth(JToggleButton segment, int width) {
        Dimension size = new Dimension(width, segment.getPreferredSize().height);
        segment.setPreferredSize(size);
        segment.setMinimumSize(size);
        segment.setMaximumSize(size);
    }

    /**
     * The capsule hugs its segments instead of being stretched by the surrounding layout - a segmented
     * control with empty space inside it looks broken. Place it in a horizontal box with a glue behind
     * it (or any layout that honours the maximum size) to keep it left aligned in a wide row.
     */
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /**
     * Runs {@code onChange} on every change of the selected segment, whether clicked or set programmatically.
     * Fires once per change: switching segments deselects one and selects another, only the latter is reported.
     */
    public void onChange(@NotNull Runnable onChange) {
        ItemListener listener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                onChange.run();
        };
        for (Component component : getComponents())
            ((JToggleButton) component).addItemListener(listener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D capsule = capsule();

            // the unselected cells show this background, so the capsule is filled as a whole
            g2.setColor(color("Button.background", Colors.BACKGROUND));
            g2.fill(capsule);

            // selected (accent) and hovered cell, clipped so the outer segments keep the round ends
            Graphics2D cells = (Graphics2D) g2.create();
            try {
                cells.clip(capsule);
                for (Component component : getComponents()) {
                    JToggleButton segment = (JToggleButton) component;
                    if (segment.isSelected())
                        cells.setColor(Colors.Menu.FILTER_BUTTON);
                    else if (segment.getModel().isRollover())
                        cells.setColor(color("Button.hoverBackground", Colors.BACKGROUND));
                    else
                        continue;
                    cells.fill(segment.getBounds());
                }
            } finally {
                cells.dispose();
            }

            // hairlines between two unselected neighbours (a selected cell separates by its own fill)
            g2.setColor(color("Component.borderColor", Colors.FOREGROUND_INTERFACE));
            for (int i = 0; i < getComponentCount() - 1; i++) {
                JToggleButton left = (JToggleButton) getComponent(i);
                JToggleButton right = (JToggleButton) getComponent(i + 1);
                if (left.isSelected() || right.isSelected())
                    continue;
                int x = right.getX();
                g2.drawLine(x, 1, x, getHeight() - 2);
            }

            // the outline last, so an accent-filled end cell cannot paint over it
            g2.setColor(isFocusInside() ? Colors.Menu.FILTER_BUTTON
                    : color("Component.borderColor", Colors.FOREGROUND_INTERFACE));
            g2.draw(capsule);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private RoundRectangle2D capsule() {
        float arc = getHeight() - 1f; // fully rounded ends
        return new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc);
    }

    private boolean isFocusInside() {
        for (Component component : getComponents())
            if (component.isFocusOwner())
                return true;
        return false;
    }

    /** Keeps the label of the selected (accent-filled) segment readable. */
    protected void updateSegmentForegrounds() {
        for (Component component : getComponents()) {
            JToggleButton segment = (JToggleButton) component;
            segment.setForeground(segment.isSelected()
                    ? Colors.Menu.FILTER_BUTTON_TEXT
                    : color("Button.foreground", Colors.FOREGROUND_INTERFACE));
        }
    }

    private static Color color(String uiKey, Color fallback) {
        Color color = UIManager.getColor(uiKey);
        return color != null ? color : fallback;
    }

    /** The criterion description, as shown when any part of the control is hovered. */
    public String composeToolTip() {
        return GuiUtils.formatToolTip(description.get());
    }

    @Override
    public String getToolTipText() {
        return composeToolTip();
    }

    protected JToggleButton segment(@NotNull String label) {
        JToggleButton segment = new JToggleButton(label) {
            @Override
            public String getToolTipText() {
                return composeToolTip(); // the segments are what the user actually hovers
            }
        };
        // the panel paints selection, hover and outline - the button only contributes its text
        segment.setContentAreaFilled(false);
        segment.setBorderPainted(false);
        segment.setFocusPainted(false);
        segment.setOpaque(false);
        segment.setRolloverEnabled(true);
        // widen the segment horizontally but keep the look-and-feel's vertical margin, so the capsule is
        // exactly as high as a normal button or text field next to it
        Insets margin = segment.getMargin();
        segment.setMargin(new Insets(margin.top, SEGMENT_PADDING, margin.bottom, SEGMENT_PADDING));
        ToolTipManager.sharedInstance().registerComponent(segment); // see the constructor
        return segment;
    }
}
