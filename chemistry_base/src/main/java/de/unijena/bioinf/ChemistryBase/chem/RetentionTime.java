/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

package de.unijena.bioinf.ChemistryBase.chem;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE)
@Slf4j
public final class RetentionTime implements Ms2ExperimentAnnotation, Comparable<RetentionTime> {

    private final double start, middle, end;
    private static final RetentionTime MISSING_RT_VALUE = new RetentionTime(Double.NaN);

    /**
     * When parsing a retention time without a unit, everything less is considered in minutes, everything greater or equal - in seconds.
     */
    public static final double RETENTION_TIME_UNIT_GUESS_THRESHOLD = 50;

    public RetentionTime(double start, double end) {
        this(start, end, start + (end - start) / 2d);
    }

    /**
     *
     * @param start end start of window
     * @param end end of window
     * @param maximum maximum intensity (apex) time
     */
    @JsonCreator
    public RetentionTime(@JsonProperty("start") double start, @JsonProperty("end") double end, @JsonProperty("middle") double maximum) {
        if (!Double.isNaN(start)) {
            /**
             * This is really annoying, but:
             * - the current implementation of the class seem to be not really friendly to using retention time ranges and single scalars interchangeably
             * - it CAN happen during preprocessing, that a ion spans several scan ids that all match to the same retention time after recalibration. I don't think we can avoid that
             * - so I allow for this edge case (which happens super rarely anyways): retention time ranges might have zero length.
             */
            if (start > end)
                throw new IllegalArgumentException("No proper interval given: [" + start + ", " + end + "]" );
            if (maximum < start || maximum > end) {
                throw new IllegalArgumentException("Given retention time middle is not in range: " + maximum + " is not in [" + start + ", " + end + "]" );
            }
        }
        this.start = start;
        this.end = end;
        this.middle = maximum;
    }

    public RetentionTime(double timeInSeconds) {
        this(Double.NaN, Double.NaN, timeInSeconds);
    }

    public RetentionTime merge(RetentionTime other) {
        if (isInterval() && other.isInterval())
            return new RetentionTime(Math.min(start, other.start), Math.max(end, other.end));
        else
            return new RetentionTime(Math.min(start, other.start), Math.max(end, other.end), (middle+other.middle)/2);
    }

    public boolean isInterval() {
        return !Double.isNaN(start);
    }

    public double getRetentionTimeInSeconds() {
        return middle;
    }

    public double getStartTime() {
        if (!isInterval()) throw new RuntimeException("No retention time range given");
        return start;
    }

    public double getEndTime() {
        if (!isInterval()) throw new RuntimeException("No retention time range given");
        return end;
    }

    public Range<Double> asRange() {
        if (!isInterval()) throw new RuntimeException("No retention time range given");
        return Range.of(start, end);
    }

    public double getMiddleTime() {
        return middle;
    }

    @Override
    public String toString() {
        return middle + " in [" + start + ", " + end + "]";
    }

    public String asStringValue() {
        return asStringValue(this);
    }

    public static String asStringValue(RetentionTime rt) {
        return rt.middle + ":" + rt.start + "," + rt.end;
    }

    public static RetentionTime fromStringValue(String rt) {
        String[] one = rt.split(":");
        String[] two = one[1].split(",");
        return new RetentionTime(Double.parseDouble(two[0].strip()), Double.parseDouble(two[1].strip()), Double.parseDouble(one[0].strip()));
    }

    public record ParsedParameters(@Nullable Double from, @Nullable Double to, @Nullable String unit) {}

    /**
     * Parses a string with retention time
     * @param s string in format "from[-to][unit]", such as "2.5 min" or "120s" or "1-2 sec" or "0.5". Expected units - s, sec, min
     * @return ParsedParameters map with relevant substrings parsed ("sec" unit will be substituted with "s"). Can contain nulls if parameters could not be extracted
     */
    public static ParsedParameters parseRetentionTimeParameters(String s) {
        Pattern pattern = Pattern.compile("(^|[^-])(?<from>\\d+\\.?\\d*)(\\s*-\\s*(?<to>\\d+\\.?\\d*))?(\\s*(?<unit>s|sec|min|minutes)(\\W|$))?");
        Matcher matcher = pattern.matcher(s);

        Double from = null;
        Double to = null;
        String unit = null;

        if (matcher.find()) {
            from = Double.valueOf(matcher.group("from"));
            if (matcher.group("to") != null) {
                to = Double.valueOf(matcher.group("to"));
            }
            if (matcher.group("unit") != null) {
                unit = matcher.group("unit");
                if (unit.equals("sec")) {
                    unit = "s";
                }
                if (unit.equals("minutes")) {
                    unit = "min";
                }
            }
        }
        return new ParsedParameters(from, to, unit);
    }

    /**
     * @return RetentionTime constructed from the passed values, or null if "from" parameter is missing
     */
    public static @Nullable RetentionTime fromParameters(ParsedParameters parameters) {
        if (parameters.from() == null) {
            return null;
        }

        String unit = parameters.unit();
        if (unit == null) {
            unit = parameters.from() < RETENTION_TIME_UNIT_GUESS_THRESHOLD ? "min" : "s";
            log.warn("Retention time unit not specified for value " + parameters.from() + ", assuming \"" + unit + "\".");
        }
        int unitFactor = unit.equals("min") ? 60 : 1;

        if (parameters.to() != null) {
            return new RetentionTime(parameters.from() * unitFactor, parameters.to() * unitFactor);
        } else {
            return new RetentionTime(parameters.from() * unitFactor);
        }
    }

    /**
     * Tries to parse retention time with optional range and unit from the passed string
     * @return Optional with retention time if parsing succeeded, or empty otherwise
     */
    public static Optional<RetentionTime> tryParse(String s) {
        return Optional.ofNullable(fromParameters(parseRetentionTimeParameters(s)));
    }

    @Override
    public int compareTo(@NotNull RetentionTime o) {
        return Double.compare(middle, o.middle);
    }

    public static <T extends RetentionTime> T NA(@NotNull Class<T> retentionTime) {
        return (T) NA();
    }

    public static RetentionTime NA() {
        return  MISSING_RT_VALUE;
    }

}
