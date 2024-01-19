/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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

package de.unijena.bioinf.ms.middleware.model.spectra;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;

@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class AnnotatedSpectrum implements OrderedSpectrum<Peak> {
    /**
     * MS level of the measured spectrum.
     * Artificial spectra with no msLevel (e.g. Simulated Isotope patterns) use null or zero
     */
    @Schema(nullable = true)
    private Integer msLevel = null;

    /**
     * Collision energy used for MS/MS spectra
     * Null for spectra where collision energy is not applicable
     */
    @Schema(nullable = true)
    private String collisionEnergy = null;

    /**
     * Scan number of the spectrum.
     * Might be null for artificial spectra with no scan number (e.g. Simulated Isotope patterns or merged spectra)
     */
    @Schema(nullable = true)
    private Integer scanNumber = -1;

    /**
     * The peaks of this spectrum which might contain additional annotations such as molecular formulas.
     */
    @Schema(required = true)
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

    public double[] getMasses() {
        return Arrays.stream(peaks).mapToDouble(AnnotatedPeak::getMass).toArray();
    }

    public double[] getIntensities() {
        return Arrays.stream(peaks).mapToDouble(AnnotatedPeak::getIntensity).toArray();
    }

    @Override
    public double getMzAt(int index) {
        return peaks[index].getMass();
    }

    @Override
    public double getIntensityAt(int index) {
        return peaks[index].getMass();
    }

    public PeakAnnotation getPeakAnnotationAt(int index) {
        return peaks[index].getPeakAnnotation();
    }

    @Override
    public Peak getPeakAt(int index) {
        return peaks[index];
    }

    @Override
    public int size() {
        return peaks.length;
    }

    @NotNull
    @Override
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

    @Override
    public boolean isEmpty() {
        return peaks.length == 0;
    }

    public boolean hasMsLevel() {
        return msLevel != null && msLevel > 0;
    }

    @Override
    public int getMsLevel() {
        if (msLevel == null)
            return 0;
        return msLevel;
    }

    public void setMsLevel(@Nullable Integer msLevel) {
        this.msLevel = msLevel;
    }

    public double getMaxIntensity() {
        return Arrays.stream(peaks).mapToDouble(AnnotatedPeak::getIntensity).max().orElse(Double.NaN);
    }

    @Nullable
    public String getCollisionEnergyStr() {
        return collisionEnergy;
    }

    public void setCollisionEnergyStr(@Nullable String collisionEnergy) {
        this.collisionEnergy = collisionEnergy;
    }

    @Override
    public CollisionEnergy getCollisionEnergy() {
        if (getCollisionEnergyStr() == null || getCollisionEnergyStr().isBlank())
            return null;
        return CollisionEnergy.fromString(getCollisionEnergyStr());
    }

    public void setCollisionEnergy(@Nullable CollisionEnergy collisionEnergy) {
            setCollisionEnergyStr(collisionEnergy == null ? null : collisionEnergy.toString());
    }

    public Integer getScanNumber() {
        return scanNumber;
    }

    public void setScanNumber(Integer scanNumber) {
        this.scanNumber = scanNumber;
    }
}
