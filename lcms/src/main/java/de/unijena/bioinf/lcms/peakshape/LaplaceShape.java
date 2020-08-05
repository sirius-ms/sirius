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

package de.unijena.bioinf.lcms.peakshape;

import de.unijena.bioinf.lcms.quality.Quality;
import org.apache.commons.math3.distribution.LaplaceDistribution;

public class LaplaceShape implements PeakShape {

    protected LaplaceDistribution distribution;
    protected double score;
    protected double maxIntensity;

    public LaplaceShape(double score, long median, double deviation, double maxIntensity) {
        this.distribution = new LaplaceDistribution(median, deviation);
        this.score = score;
        this.maxIntensity = maxIntensity;
    }

    public LaplaceDistribution getDistribution() {
        return distribution;
    }

    public double getScore() {
        return score;
    }

    public double getMaxIntensity() {
        return maxIntensity;
    }

    public double expectedIntensityAt(long k) {
        return maxIntensity*distribution.density(k)/distribution.density(distribution.getLocation());
    }

    @Override
    public double getLocation() {
        return distribution.getLocation();
    }

    @Override
    public Quality getPeakShapeQuality() {
        if (score < -0.6) return Quality.UNUSABLE;
        if (score < -0.4) return Quality.BAD;
        if (score < -0.25) return Quality.DECENT;
        return Quality.GOOD;
    }

}
