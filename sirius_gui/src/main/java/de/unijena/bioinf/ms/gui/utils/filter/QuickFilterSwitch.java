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

package de.unijena.bioinf.ms.gui.utils.filter;

import io.sirius.ms.sdk.model.DataQuality;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * A two-state shortcut over a {@link FeatureFilter}: what it reads as "on" and how to turn it on or off. The
 * switch has no state of its own - an editor derives its position from the working value on every render
 * and applies {@link #apply} as one edit, so a switch, a chip's remove and Restore defaults can never disagree
 * (the last edit wins, and the switch follows).
 * <p>
 * The three {@link #standard()} switches <em>relax</em> the three restrictions of the default filter, so a
 * user can lift one for a quick look and put it back without opening the dialog.
 *
 * @param label   the short text next to the switch
 * @param tooltip what "on" means
 * @param isOn    whether the filter currently reads as "on"
 * @param turnOn  the filter with the switch on
 * @param turnOff the filter with the switch off
 */
public record QuickFilterSwitch(@NotNull String label,
                                @NotNull String tooltip,
                                @NotNull Predicate<FeatureFilter> isOn,
                                @NotNull BiFunction<FeatureFilter, FilterContext, FeatureFilter> turnOn,
                                @NotNull BiFunction<FeatureFilter, FilterContext, FeatureFilter> turnOff) {

    /** The filter with this switch set to {@code on}. */
    public FeatureFilter apply(@NotNull FeatureFilter filter, @NotNull FilterContext ctx, boolean on) {
        return on ? turnOn.apply(filter, ctx) : turnOff.apply(filter, ctx);
    }

    /** MS1-only features are included too: lifts the default "MS/MS present" requirement. */
    public static final QuickFilterSwitch MS1_ONLY = new QuickFilterSwitch(
            "MS1 only",
            "Include features that have no MS/MS spectrum (lifts the default MS/MS requirement)",
            f -> !f.hasMsMs(),
            (f, ctx) -> f.withHasMsMs(false),
            (f, ctx) -> f.withHasMsMs(true));

    /**
     * Multimeric and multiply-charged adducts pass too: lifts the default restriction to the adducts SIRIUS
     * can annotate. Off restricts back to the supported adducts - all supported detected ones when that
     * would otherwise leave the selection empty, see {@link FeatureFilter#withSupportedAdductsOnly}.
     */
    public static final QuickFilterSwitch ALL_ADDUCTS = new QuickFilterSwitch(
            "All adducts",
            "Include features with multimeric or multiply-charged adducts, which SIRIUS cannot annotate",
            FeatureFilter::isMultiAdductsAllowed,
            FeatureFilter::withMultiAdducts,
            FeatureFilter::withSupportedAdductsOnly);

    /** Features of BAD quality pass too (LOWEST never does through this switch). */
    public static final QuickFilterSwitch SHOW_BAD = new QuickFilterSwitch(
            "Show bad",
            "Include features of bad quality (the default keeps only decent and good ones)",
            f -> f.featureQuality().contains(DataQuality.BAD),
            (f, ctx) -> f.withFeatureQuality(with(f.featureQuality(), DataQuality.BAD)),
            (f, ctx) -> f.withFeatureQuality(without(f.featureQuality(), DataQuality.BAD)));

    /** The three switches over the default filter's restrictions, in footer order. */
    public static List<QuickFilterSwitch> standard() {
        return List.of(MS1_ONLY, ALL_ADDUCTS, SHOW_BAD);
    }

    private static Set<DataQuality> with(Set<DataQuality> qualities, DataQuality quality) {
        EnumSet<DataQuality> set = qualities.isEmpty() ? EnumSet.noneOf(DataQuality.class) : EnumSet.copyOf(qualities);
        set.add(quality);
        return set;
    }

    private static Set<DataQuality> without(Set<DataQuality> qualities, DataQuality quality) {
        EnumSet<DataQuality> set = qualities.isEmpty() ? EnumSet.noneOf(DataQuality.class) : EnumSet.copyOf(qualities);
        set.remove(quality);
        return set;
    }
}
