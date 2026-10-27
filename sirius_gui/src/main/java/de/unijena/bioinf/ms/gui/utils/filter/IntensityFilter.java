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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The minimum MS1 intensity a feature must reach, measured either as peak height (apex) or as area under the
 * chromatographic peak. The threshold is an absolute value in raw instrument counts, or a percentage of the most
 * intense feature of the same run; the two are separate fields of the search index, so the unit decides which
 * field the clause names and nothing about the project is needed here. Like the m/z and RT thresholds, the filter
 * is active as soon as the minimum is above zero.
 * <p>
 * A value, like the {@link FeatureFilter} it is part of. It keeps the threshold of both units, so switching the
 * unit back and forth loses neither.
 *
 * @param minAbsolute the threshold in {@link Mode#ABSOLUTE}: an intensity in raw counts
 * @param minPercent  the threshold in {@link Mode#RELATIVE_TO_RUN_MAX}: percent of the run maximum
 */
public record IntensityFilter(@NotNull Measure measure, @NotNull Mode mode, double minAbsolute, double minPercent) {

    /** Which intensity of the feature the threshold applies to. */
    public enum Measure {
        /** peak height at the apex of the chromatographic peak */
        APEX("intensity.apex", "intensity.apexPercent", "peak height"),
        /** area under the chromatographic peak */
        AUC("intensity.auc", "intensity.aucPercent", "peak area");

        /** the search index field holding this measure, in the unit of the input data */
        private final String field;
        /** the one holding it in percent of the run maximum - a field of its own, not a way of writing the above */
        private final String relativeField;
        private final String label;

        Measure(String field, String relativeField, String label) {
            this.field = field;
            this.relativeField = relativeField;
            this.label = label;
        }

        public String getField() {
            return field;
        }

        public String getRelativeField() {
            return relativeField;
        }

        /** The field a threshold of this measure is matched against, in the given unit. */
        public String getField(@NotNull Mode mode) {
            return mode == Mode.ABSOLUTE ? field : relativeField;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Mode {
        /** the threshold is an intensity in raw counts */
        ABSOLUTE("absolute"),
        /** the threshold is a percentage of the most intense feature of the same run */
        RELATIVE_TO_RUN_MAX("% of run max");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * The sign that says a typed threshold is a percentage rather than an intensity. Reading it lets "10 %" be
     * typed instead of picking {@link Mode#RELATIVE_TO_RUN_MAX} - a shortcut and never the only way there, since
     * the unit is a choice of its own. It is consumed in the reading: the unit belongs to that choice, and a
     * field that also carried it would state it twice.
     */
    public static final char PERCENT_SIGN = '%';

    /** Whether {@code text} ends in a percent sign, i.e. names a percentage of the run maximum. */
    public static boolean isPercentTyped(@Nullable String text) {
        return text != null && text.strip().endsWith(String.valueOf(PERCENT_SIGN));
    }

    /** {@code text} without a trailing percent sign, so the rest can be read as a plain number. */
    public static String stripPercent(@Nullable String text) {
        if (text == null)
            return "";
        String stripped = text.strip();
        if (stripped.endsWith(String.valueOf(PERCENT_SIGN)))
            stripped = stripped.substring(0, stripped.length() - 1).strip();
        return stripped;
    }

    /** The largest a percentage of a maximum can be - a threshold above this can only be an intensity. */
    public static final double MAX_PERCENT = 100d;

    /**
     * The unit a typed threshold is in, given the unit currently shown.
     * <p>
     * A percent sign says it outright. A bare number keeps the unit that is shown, because dropping the sign is
     * how a percentage is usually typed - except when it is larger than {@link #MAX_PERCENT}, which no share of
     * a maximum can be, so it can only be an intensity.
     *
     * @param shown the unit the threshold is currently shown in, null while a switch is in flight
     */
    public static @Nullable Mode typedUnit(@Nullable String text, double value, @Nullable Mode shown) {
        if (isPercentTyped(text))
            return Mode.RELATIVE_TO_RUN_MAX;
        if (shown == Mode.RELATIVE_TO_RUN_MAX && value > MAX_PERCENT)
            return Mode.ABSOLUTE;
        return shown;
    }

    public static final double DEFAULT_MIN_ABSOLUTE = 0d;
    public static final double DEFAULT_MIN_PERCENT = 0d;

    /** No intensity filter: peak height, absolute, both thresholds at zero. */
    public static final IntensityFilter DEFAULT =
            new IntensityFilter(Measure.APEX, Mode.ABSOLUTE, DEFAULT_MIN_ABSOLUTE, DEFAULT_MIN_PERCENT);

    public IntensityFilter {
        Objects.requireNonNull(measure);
        Objects.requireNonNull(mode);
    }

    /** Whether the filter narrows anything: the minimum of the selected mode is above zero. */
    public boolean isActive() {
        return min() > 0;
    }

    /** The threshold of the selected mode. */
    public double min() {
        return min(mode);
    }

    /** The threshold kept for {@code mode}, selected or not. */
    public double min(@NotNull Mode mode) {
        return mode == Mode.ABSOLUTE ? minAbsolute : minPercent;
    }

    /** The search index field the threshold is matched against. */
    public String field() {
        return measure.getField(mode);
    }

    public IntensityFilter withMeasure(@NotNull Measure measure) {
        return new IntensityFilter(measure, mode, minAbsolute, minPercent);
    }

    public IntensityFilter withMode(@NotNull Mode mode) {
        return new IntensityFilter(measure, mode, minAbsolute, minPercent);
    }

    public IntensityFilter withMinAbsolute(double minAbsolute) {
        return new IntensityFilter(measure, mode, minAbsolute, minPercent);
    }

    public IntensityFilter withMinPercent(double minPercent) {
        return new IntensityFilter(measure, mode, minAbsolute, minPercent);
    }

    /** The filter with the threshold of {@code mode} replaced, the other one kept. */
    public IntensityFilter withMin(@NotNull Mode mode, double min) {
        return mode == Mode.ABSOLUTE ? withMinAbsolute(min) : withMinPercent(min);
    }
}
