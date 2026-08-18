/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Colors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

/**
 * A segmented control for a filter criterion a feature can either have or not have, plus the third state
 * of "do not filter on this at all": three adjacent segments in one capsule of which exactly one is
 * selected, so all options are visible and each is one click away.
 * <p>
 * The neutral segment sits in the MIDDLE, between "must not have it" (left) and "must have it" (right),
 * so the segments read like the ordinal scales next to them: absent on the left, present on the right.
 * <p>
 * Painting is done here rather than by the segments: the buttons only draw their (look-and-feel) text,
 * while this panel draws the capsule, the selected cell in the filter accent
 * ({@link Colors.Menu#FILTER_BUTTON}, the same accent the search-bar chips use), the hover cell and the
 * separators between two unselected neighbours. That is what makes the segments look like one control
 * instead of three buttons.
 */
public class SegmentedFilterToggle extends JPanel {

    private static final Insets SEGMENT_MARGIN = new Insets(3, 12, 3, 12);

    private final JToggleButton noSegment, anySegment, yesSegment;

    private final Supplier<String> description;

    /**
     * @param anyLabel    the "do not filter" segment (shown in the middle), e.g. {@code "any"}
     * @param yesLabel    the "must have it" segment (shown right), e.g. {@code "is a lipid"}
     * @param noLabel     the "must not have it" segment (shown left), e.g. {@code "not a lipid"}
     * @param description describes the criterion; asked for whenever a tooltip is shown, so a
     *                    description that is only fetched later (e.g. from the search index) appears
     */
    public SegmentedFilterToggle(@NotNull String anyLabel, @NotNull String yesLabel, @NotNull String noLabel,
                                 @NotNull Supplier<String> description) {
        this.description = description;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        noSegment = segment(noLabel);
        anySegment = segment(anyLabel);
        yesSegment = segment(yesLabel);
        // NOT setToolTipText: that only registers a component whose previous text was null, and our
        // getToolTipText() never returns null - so it would silently never show a tooltip at all
        ToolTipManager.sharedInstance().registerComponent(this);

        ButtonGroup group = new ButtonGroup();
        // no gaps: the segments sit directly next to each other inside one capsule
        for (JToggleButton segment : new JToggleButton[]{noSegment, anySegment, yesSegment}) {
            group.add(segment);
            add(segment);
            // selection and hover are painted by this panel, so both need a repaint
            segment.getModel().addChangeListener(e -> {
                updateSegmentForegrounds();
                repaint();
            });
        }
        // the two outer segments mirror each other: same width (from the wider label) and centered text,
        // so the control stays symmetric around the neutral one in the middle
        int outerWidth = Math.max(noSegment.getPreferredSize().width, yesSegment.getPreferredSize().width);
        for (JToggleButton segment : new JToggleButton[]{noSegment, yesSegment})
            fixWidth(segment, outerWidth);

        anySegment.setSelected(true); // no filter until the user picks one
        updateSegmentForegrounds();
    }

    private static void fixWidth(JToggleButton segment, int width) {
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

    /** The filter state: {@code null} = no filter, {@code TRUE} = must have, {@code FALSE} = must not have. */
    public @Nullable Boolean getFilterState() {
        if (yesSegment.isSelected())
            return Boolean.TRUE;
        if (noSegment.isSelected())
            return Boolean.FALSE;
        return null;
    }

    /** @see #getFilterState() */
    public void setFilterState(@Nullable Boolean filterState) {
        segmentFor(filterState).setSelected(true);
    }

    /**
     * Runs {@code onChange} on every state change, whether clicked or set programmatically. Fires once
     * per change: switching segments deselects one and selects another, only the latter is reported.
     */
    public void onChange(@NotNull Runnable onChange) {
        ItemListener listener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                onChange.run();
        };
        noSegment.addItemListener(listener);
        anySegment.addItemListener(listener);
        yesSegment.addItemListener(listener);
    }

    /** The segment representing the given filter state. */
    JToggleButton segmentFor(@Nullable Boolean filterState) {
        if (filterState == null)
            return anySegment;
        return filterState ? yesSegment : noSegment;
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
    private void updateSegmentForegrounds() {
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

    private JToggleButton segment(@NotNull String label) {
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
        segment.setMargin(SEGMENT_MARGIN);
        ToolTipManager.sharedInstance().registerComponent(segment); // see the constructor
        return segment;
    }
}
