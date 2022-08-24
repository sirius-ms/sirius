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

package de.unijena.bioinf.ms.middleware.spectrum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotatedSpectrum {
    @Nullable private Integer mslevel = null;
    @Nullable private CollisionEnergy collisionEnergy = null;
    private AnnotatedPeak[] peaks;

    public AnnotatedSpectrum(@NotNull Spectrum<Peak> spec) {
        this(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec));
    }

    public AnnotatedSpectrum(double[] masses, double[] intensities) {
        this(masses, intensities, null);
    }

    public AnnotatedSpectrum(double[] masses, double[] intensities, @Nullable PeakAnnotation[] peakAnnotations) {
        if (masses == null)
            throw new IllegalArgumentException("Masses are Null but must be non Null.");
        if (intensities == null)
            throw new IllegalArgumentException("Intensities are Null but must be non Null.");

        if (masses.length != intensities.length)
            throw new IllegalArgumentException("Masses and Intensities do not have same length but must have.");

        peaks = new AnnotatedPeak[masses.length];
        if (peakAnnotations != null) {
            for (int i = 0; i < masses.length; i++)
                peaks[i] = new AnnotatedPeak(masses[i], intensities[i], peakAnnotations[i]);
        } else {
            for (int i = 0; i < masses.length; i++)
                peaks[i] = new AnnotatedPeak(masses[i], intensities[i], null);
        }
    }

    public AnnotatedPeak[] getPeaks() {
        return peaks;
    }

    public void setPeaks(AnnotatedPeak[] peaks) {
        this.peaks = peaks;
    }

    @JsonIgnore
    public double[] getMasses() {
        return Arrays.stream(peaks).mapToDouble(AnnotatedPeak::getMass).toArray();
    }

    @JsonIgnore
    public double[] getIntensities() {
        return Arrays.stream(peaks).mapToDouble(AnnotatedPeak::getIntensity).toArray();
    }

    @JsonIgnore
    public double getMzAt(int index) {
        return peaks[index].getMass();
    }

    @JsonIgnore
    public double getIntensityAt(int index) {
        return peaks[index].getMass();
    }

    @JsonIgnore
    public PeakAnnotation getPeakAnnotationAt(int index) {
        return peaks[index].getPeakAnnotation();
    }


    @JsonIgnore
    public Peak getPeakAt(int index) {
        return peaks[index];
    }

    @JsonIgnore
    public int size() {
        return peaks.length;
    }

    @NotNull
    @JsonIgnore
    public Iterator<Peak> iterator() {
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < peaks.length;
            }

            @Override
            public Peak next() {
                return getPeakAt(index++);
            }
        };
    }

    @JsonIgnore
    public boolean isEmpty() {
        return peaks.length == 0;
    }

    @Nullable
    public CollisionEnergy getCollisionEnergy() {
        return collisionEnergy;
    }

    @Nullable
    public Integer getMsLevel() {
        return mslevel;
    }

    public void setMslevel(@Nullable Integer mslevel) {
        this.mslevel = mslevel;
    }

    public void setCollisionEnergy(@Nullable CollisionEnergy collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

    @JsonIgnore
    public double getMaxIntensity() {
        return Arrays.stream(peaks).mapToDouble(AnnotatedPeak::getIntensity).max().orElse(Double.NaN);
    }
}
