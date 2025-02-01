/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.table.list_stats;

import org.jetbrains.annotations.Nullable;
import static java.util.Objects.requireNonNullElse;

/**
 * Created by fleisch on 12.05.17.
 */
public class DoubleListStats implements ListStats {
    protected double scoreSum;
    protected double minScoreValue;
    protected double maxScoreValue;

    protected final double rangeMin;
    protected final double rangeMax;

    public DoubleListStats(double[] values) {
        this(values, null, null);
    }

    public DoubleListStats(double[] values, @Nullable Double rangeMin, @Nullable Double rangeMax) {
        this.rangeMin = requireNonNullElse(rangeMin, Double.NaN);
        this.rangeMax = requireNonNullElse(rangeMax, Double.NaN);
        update(values);
    }

    public DoubleListStats() {
        this(null, null);
    }

    public DoubleListStats(@Nullable Double rangeMin, @Nullable Double rangeMax) {
        this.rangeMin = requireNonNullElse(rangeMin, Double.NaN);
        this.rangeMax = requireNonNullElse(rangeMax, Double.NaN);
        reset();
    }

    @Override
    public double getMax() {
        return maxScoreValue;
    }

    @Override
    public double getMin() {
        return minScoreValue;
    }

    @Override
    public double getSum() {
        return scoreSum;
    }

    @Override
    public double getRangeMin() {
        return Double.isNaN(rangeMin) ? getMin() : rangeMin;
    }

    @Override
    public double getRangeMax() {
        return Double.isNaN(rangeMax) ? getMax() : rangeMax;
    }

    public DoubleListStats update(double[] values) {
        reset();
        if (values != null) {
            for (double score : values) {
                addValue(score);
            }
        }
        return this;
    }

    public DoubleListStats addValue(double score) {
        scoreSum += score;
        minScoreValue = Math.min(minScoreValue, score);
        maxScoreValue = Math.max(maxScoreValue, score);
        return this;
    }

    public DoubleListStats reset() {
        scoreSum = 0d;
        minScoreValue = Double.POSITIVE_INFINITY;
        maxScoreValue = Double.NEGATIVE_INFINITY;
        return this;
    }

    public DoubleListStats setMinScoreValue(double minScoreValue) {
        this.minScoreValue = minScoreValue;
        return this;
    }

    public DoubleListStats setMaxScoreValue(double maxScoreValue) {
        this.maxScoreValue = maxScoreValue;
        return this;
    }

    public DoubleListStats setScoreSum(double scoreSum) {
        this.scoreSum = scoreSum;
        return this;
    }
}
