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

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * The minimum MS1 intensity a feature must reach, measured either as peak height (apex) or as area under the
 * chromatographic peak. The threshold is an absolute value in raw instrument counts, or a percentage of the most
 * intense feature of the same run. A percentage goes into the query as written ({@code intensityApex:[10% TO *]});
 * the search index matches it against the feature's intensity relative to its run maximum, so it needs nothing
 * about the project here. Like the m/z and RT thresholds, the filter is active as soon as the minimum is above zero.
 */
@Getter
public class IntensityFilter {

    /** Which intensity of the feature the threshold applies to. */
    public enum Measure {
        /** peak height at the apex of the chromatographic peak */
        APEX("intensityApex", "apex"),
        /** area under the chromatographic peak */
        AUC("intensityAUC", "area");

        /** the search index field holding this measure */
        private final String field;
        private final String label;

        Measure(String field, String label) {
            this.field = field;
            this.label = label;
        }

        public String getField() {
            return field;
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
        RELATIVE_TO_RUN_MAX("relative");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final double DEFAULT_MIN_ABSOLUTE = 0d;
    public static final double DEFAULT_MIN_PERCENT = 0d;

    @Setter
    @NotNull
    private Measure measure = Measure.APEX;
    @Setter
    @NotNull
    private Mode mode = Mode.ABSOLUTE;
    /** the threshold in {@link Mode#ABSOLUTE}: an intensity in raw counts */
    @Setter
    private double minAbsolute = DEFAULT_MIN_ABSOLUTE;
    /** the threshold in {@link Mode#RELATIVE_TO_RUN_MAX}: percent of the run maximum */
    @Setter
    private double minPercent = DEFAULT_MIN_PERCENT;

    /** Whether the filter narrows anything: the minimum of the selected mode is above zero. */
    public boolean isActive() {
        return mode == Mode.ABSOLUTE ? minAbsolute > 0 : minPercent > 0;
    }

    public void reset() {
        measure = Measure.APEX;
        mode = Mode.ABSOLUTE;
        minAbsolute = DEFAULT_MIN_ABSOLUTE;
        minPercent = DEFAULT_MIN_PERCENT;
    }
}
