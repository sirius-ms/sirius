/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker, Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoiseInformation implements Ms2ExperimentAnnotation {

    private final double meanNoiseIntensity, medianNoiseIntensity;

    /**
     * intensity at which a peak is most likely noise or signal
     */
    private final double noiseLevel, signalLevel;

    private final ParetoDistribution noiseDistribution;

    public String toString() {
        return "Noise (mean = " + meanNoiseIntensity + ", median = " + medianNoiseIntensity + "), noise level = " + noiseLevel + ", signal level = " + signalLevel + ", Pareto(" + noiseDistribution.getK() + ", " + noiseDistribution.getXmin() + ")";
    }

    private final static Pattern pattern = Pattern.compile(
            "Noise \\(mean = (\\S+), median = (\\S+)\\), noise level = (\\S+), signal level = (\\S+), Pareto\\((\\S+), (\\S+)\\)");

    public static NoiseInformation fromString(String s) {
        final Matcher M = pattern.matcher(s);
        if (M.find()) {
            final double mean = Double.parseDouble(M.group(1));
            final double median = Double.parseDouble(M.group(2));
            final double noiseLevel = Double.parseDouble(M.group(3));
            final double signalLevel = Double.parseDouble(M.group(4));
            final ParetoDistribution dist = new ParetoDistribution(Double.parseDouble(M.group(5)), Double.parseDouble(M.group(6)));
            return new NoiseInformation(mean, median, noiseLevel, signalLevel, dist);
        } else throw new IllegalArgumentException("Cannot parse '" + s + "'");
    }

    public NoiseInformation(double meanNoiseIntensity, double medianNoiseIntensity, double noiseLevel, double signalLevel, ParetoDistribution noiseDistribution) {
        this.meanNoiseIntensity = meanNoiseIntensity;
        this.medianNoiseIntensity = medianNoiseIntensity;
        this.noiseLevel = noiseLevel;
        this.signalLevel = signalLevel;
        this.noiseDistribution = noiseDistribution;
    }

    public double getMeanNoiseIntensity() {
        return meanNoiseIntensity;
    }

    public double getMedianNoiseIntensity() {
        return medianNoiseIntensity;
    }

    public double getSignalLevel() {
        return signalLevel;
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public ParetoDistribution getNoiseDistribution() {
        return noiseDistribution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoiseInformation that = (NoiseInformation) o;
        return Double.compare(that.meanNoiseIntensity, meanNoiseIntensity) == 0 &&
                Double.compare(that.medianNoiseIntensity, medianNoiseIntensity) == 0 &&
                Double.compare(that.noiseLevel, noiseLevel) == 0 &&
                noiseDistribution.equals(that.noiseDistribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meanNoiseIntensity, medianNoiseIntensity, noiseLevel, noiseDistribution);
    }
}
