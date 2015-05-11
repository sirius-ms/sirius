/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For peaks in EI spectra the allowed deviation increases with increasing intensity.
 */
@HasParameters
public class EIIntensityDeviation extends Deviation{
    private double smallAbsError;
    private double largeAbsError;
    private double smallErrorPpm;
    private double largeErrorPpm;
    private double intensity;

    /**
     * small errors at 0 relative intensity and large errors at full intensity. absolute error in Da.
     * @param smallErrorPpm
     * @param largeErrorPpm
     * @param smallAbsError
     * @param largeAbsError
     */
    public EIIntensityDeviation(@Parameter("minPpm")double smallErrorPpm, @Parameter("maxPpm")double largeErrorPpm, @Parameter("minAbsolute")double smallAbsError, @Parameter("maxAbsolute")double largeAbsError) {
        super(0, 0);
        if (smallAbsError>largeAbsError || smallErrorPpm>largeErrorPpm) throw new IllegalArgumentException("large errors have to be greater than small errors");
        this.smallErrorPpm = smallErrorPpm;
        this.largeErrorPpm = largeErrorPpm;
        this.smallAbsError = smallAbsError;
        this.largeAbsError = largeAbsError;

    }


    /**
     * use for intensity Normalization.Max(1d) only
     */
    @Override
    public double absoluteFor(double value) {
        final double absolute = (intensity*(largeAbsError-smallAbsError)/1d+smallAbsError);
        final double relative = (intensity*(largeErrorPpm-smallErrorPpm)/1d+smallErrorPpm)*1e-6*value;
        return Math.max(relative,absolute);
    }

    @Override
    public boolean inErrorWindow(double center, double value) {
        return super.inErrorWindow(center, value);
    }

    @Override
    public Deviation multiply(int scalar) {
        return new EIIntensityDeviation(smallErrorPpm*scalar, largeErrorPpm*scalar, smallAbsError*2, largeAbsError*2);
    }

    @Override
    public Deviation multiply(double scalar) {
        return new EIIntensityDeviation(smallErrorPpm*scalar, largeErrorPpm*scalar, smallAbsError*2, largeAbsError*2);
    }

    public void setRelIntensity(double relIntensity){
        this.intensity = relIntensity;
    }

    public double getSmallAbsError() {
        return smallAbsError;
    }

    public double getLargeAbsError() {
        return largeAbsError;
    }

    public double getSmallErrorPpm() {
        return smallErrorPpm;
    }

    public double getLargeErrorPpm() {
        return largeErrorPpm;
    }

    public double getIntensity() {
        return intensity;
    }

    public String toString() {
        return smallErrorPpm + " minPpm " + largeErrorPpm+" maxPpm (" + smallAbsError + " min m/z " + largeAbsError+ " max m/z)";
    }

    private static Pattern pattern = Pattern.compile("(.+) minPpm (.+) maxPpm \\((.+) min m\\/z (.+) max m\\/z\\)");
    public static EIIntensityDeviation fromString(String s) {
        final Matcher m = pattern.matcher(s);
        if (!m.find()) throw new IllegalArgumentException("Pattern should have the format <number> ppm (<number> m/z)");
        return new EIIntensityDeviation(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4)));
    }
}
