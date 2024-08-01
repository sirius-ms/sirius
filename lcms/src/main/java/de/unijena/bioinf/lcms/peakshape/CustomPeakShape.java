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

public class CustomPeakShape implements PeakShape{

    protected Quality quality;
    protected double score;

    public CustomPeakShape(double probability) {
        this.score = Math.log(probability);
        if (probability >= 0.25) quality = Quality.GOOD;
        else if (probability >= 0.01) quality = Quality.DECENT;
        else if (probability > 0.001) quality = Quality.BAD;
        else quality = Quality.UNUSABLE;
    }

    @Override
    public double getScore() {
        return score;
    }

    @Override
    public double expectedIntensityAt(long rt) {
        return 0;
    }

    @Override
    public double getLocation() {
        return 0;
    }

    @Override
    public Quality getPeakShapeQuality() {
        return quality;
    }
}
