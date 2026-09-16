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

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A segmented switch between a fixed set of options of which exactly one is selected - one segment per
 * option, in the given order, all of the same width so the capsule reads as one control. The first option
 * is selected until told otherwise. How the capsule is painted is {@link SegmentedControl}'s.
 *
 * @param <T> the option type, typically an enum
 */
public class SegmentedChoice<T> extends SegmentedControl {

    private final List<T> options;
    private final List<JToggleButton> segments;

    /**
     * @param options     the options in display order, left to right; at least one
     * @param label       the segment label of an option
     * @param description describes the choice; asked for whenever a tooltip is shown
     */
    public SegmentedChoice(@NotNull List<T> options, @NotNull Function<T, String> label, @NotNull Supplier<String> description) {
        super(description);
        if (options.isEmpty())
            throw new IllegalArgumentException("A choice needs at least one option.");
        this.options = List.copyOf(options);
        List<JToggleButton> created = new ArrayList<>(options.size());
        for (T option : this.options) {
            JToggleButton segment = segment(label.apply(option));
            addSegment(segment);
            created.add(segment);
        }
        segments = List.copyOf(created);
        int width = segments.stream().mapToInt(segment -> segment.getPreferredSize().width).max().orElse(0);
        segments.forEach(segment -> fixWidth(segment, width));

        segments.get(0).setSelected(true);
        updateSegmentForegrounds();
    }

    /**
     * Gives the segments of all {@code choices} the width of the widest segment among them, so switches stacked
     * in a column are equally wide and their splits line up.
     */
    public static void alignSegmentWidths(@NotNull SegmentedChoice<?>... choices) {
        int width = 0;
        for (SegmentedChoice<?> choice : choices)
            for (JToggleButton segment : choice.segments)
                width = Math.max(width, segment.getPreferredSize().width);
        for (SegmentedChoice<?> choice : choices) {
            for (JToggleButton segment : choice.segments)
                fixWidth(segment, width);
            choice.invalidate(); // drops the layout's cached sizes, also while the control is not shown yet
            choice.revalidate();
        }
    }

    /** The selected option. */
    public @NotNull T getSelected() {
        for (int i = 0; i < segments.size(); i++)
            if (segments.get(i).isSelected())
                return options.get(i);
        return options.get(0);
    }

    /** Selects {@code option}; change listeners fire if that is a change. */
    public void setSelected(@NotNull T option) {
        segmentFor(option).setSelected(true);
    }

    /** The segment representing the given option. */
    JToggleButton segmentFor(@NotNull T option) {
        int index = options.indexOf(option);
        if (index < 0)
            throw new IllegalArgumentException("Not one of this choice's options: " + option);
        return segments.get(index);
    }
}
