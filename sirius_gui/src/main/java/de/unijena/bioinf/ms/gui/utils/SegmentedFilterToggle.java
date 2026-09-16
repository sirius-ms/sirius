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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * A segmented control for a filter criterion a feature can either have or not have, plus the third state
 * of "do not filter on this at all": three adjacent segments in one capsule of which exactly one is
 * selected, so all options are visible and each is one click away.
 * <p>
 * The neutral segment sits in the MIDDLE, between "must not have it" (left) and "must have it" (right),
 * so the segments read like the ordinal scales next to them: absent on the left, present on the right.
 * How the capsule is painted is {@link SegmentedControl}'s.
 */
public class SegmentedFilterToggle extends SegmentedControl {

    private final JToggleButton noSegment, anySegment, yesSegment;

    /**
     * @param anyLabel    the "do not filter" segment (shown in the middle), e.g. {@code "any"}
     * @param yesLabel    the "must have it" segment (shown right), e.g. {@code "is a lipid"}
     * @param noLabel     the "must not have it" segment (shown left), e.g. {@code "not a lipid"}
     * @param description describes the criterion; asked for whenever a tooltip is shown, so a
     *                    description that is only fetched later (e.g. from the search index) appears
     */
    public SegmentedFilterToggle(@NotNull String anyLabel, @NotNull String yesLabel, @NotNull String noLabel,
                                 @NotNull Supplier<String> description) {
        super(description);
        noSegment = segment(noLabel);
        anySegment = segment(anyLabel);
        yesSegment = segment(yesLabel);
        for (JToggleButton segment : new JToggleButton[]{noSegment, anySegment, yesSegment})
            addSegment(segment);
        // the two outer segments mirror each other: same width (from the wider label) and centered text,
        // so the control stays symmetric around the neutral one in the middle
        int outerWidth = Math.max(noSegment.getPreferredSize().width, yesSegment.getPreferredSize().width);
        for (JToggleButton segment : new JToggleButton[]{noSegment, yesSegment})
            fixWidth(segment, outerWidth);

        anySegment.setSelected(true); // no filter until the user picks one
        updateSegmentForegrounds();
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

    /** The segment representing the given filter state. */
    JToggleButton segmentFor(@Nullable Boolean filterState) {
        if (filterState == null)
            return anySegment;
        return filterState ? yesSegment : noSegment;
    }
}
