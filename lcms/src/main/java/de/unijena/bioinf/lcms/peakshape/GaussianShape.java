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

import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.lcms.quality.Quality;

public class GaussianShape implements PeakShape {

    protected double score;
    protected double mean;
    protected double standardDeviation, maxIntensity;

    public GaussianShape(double score, double mean, double standardDeviation, double maxIntensity) {
        this.score = score;
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.maxIntensity = maxIntensity;
    }

    public double getLocation() {
        return mean;
    }

    @Override
    public Quality getPeakShapeQuality() {
        if (score < -0.6) return Quality.UNUSABLE;
        if (score < -0.4) return Quality.BAD;
        if (score < -0.25) return Quality.DECENT;
        return Quality.GOOD;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double expectedIntensityAt(long k) {
        final NormalDistribution distribution = new NormalDistribution(mean, standardDeviation * standardDeviation);
        return maxIntensity*distribution.getDensity(k)/distribution.getDensity(mean);
    }

    @Override
    public double getScore() {
        return score;
    }
}
