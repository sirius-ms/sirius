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

package de.unijena.bioinf.ChemistryBase.ms;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Range;

import java.io.Serializable;
import java.util.Locale;

public class IsolationWindow implements Serializable {

    protected final double windowOffset;
    protected final double windowWidth;


    public IsolationWindow(@JsonProperty("windowOffset") double windowOffset, @JsonProperty("windowWidth") double windowWidth) {
        this.windowOffset = windowOffset;
        this.windowWidth = windowWidth;
    }

    public double getWindowOffset() {
        return windowOffset;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public double getLeftOffset() {
        return (windowOffset - windowWidth/2d);
    }

    public double getRightOffset() {
        return (windowWidth/2d  + windowOffset);
    }

    public Range<Double> getWindowFor(double peak) {
        return Range.of(peak+windowOffset-windowWidth/2d, peak+windowOffset+windowWidth/2d);
    }

    public static IsolationWindow fromOffsets(double lowerOffset, double higherOffset) {
        double width = lowerOffset + higherOffset;
        double offset = (higherOffset - lowerOffset) / 2d;
        return new IsolationWindow(offset, width);
    }

    public boolean isUndefined() {
        return windowWidth<=0 || !Double.isFinite(windowWidth);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "IsolationWindow: offset = %f, width=%f.",windowOffset,windowWidth);
    }
}
