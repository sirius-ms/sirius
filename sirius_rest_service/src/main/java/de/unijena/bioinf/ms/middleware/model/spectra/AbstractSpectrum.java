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
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class AbstractSpectrum<P extends Peak> implements OrderedSpectrum<P> {
    /**
     * Optional Displayable name of this spectrum.
     */
    @Schema(nullable = true)
    @Getter @Setter
    protected String name = null;

    /**
     * MS level of the measured spectrum.
     * Artificial spectra with no msLevel (e.g. Simulated Isotope patterns) use null or zero
     */
    @Schema(nullable = true)
    protected Integer msLevel = null;

    /**
     * Collision energy used for MS/MS spectra
     * Null for spectra where collision energy is not applicable
     */
    @Schema(nullable = true)
    protected String collisionEnergy = null;

    /**
     * Instrument information.
     */
    @Schema(nullable = true)
    @Getter @Setter
    protected String instrument;

    /**
     * Precursor m/z of the MS/MS spectrum
     * Null for spectra where precursor m/z is not applicable
     */
    @Schema(nullable = true)
    @Getter
    @Setter
    protected Double precursorMz = null;

    /**
     * Scan number of the spectrum.
     * Might be null for artificial spectra with no scan number (e.g. Simulated Isotope patterns or merged spectra)
     */
    @Schema(nullable = true)
    @Getter
    @Setter
    protected Integer scanNumber = -1;

    /**
     * The peaks of this spectrum which might contain additional annotations such as molecular formulas.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<P> peaks;

    /**
     * Factor to convert relative intensities to absolute intensities.
     * Might be null or 1 for spectra where absolute intensities are not available (E.g. artificial or merged spectra)
     * <p>
     * DEPRECATED: Spectra are always returned with raw intensities.
     * Use provided normalization factors to normalize on the fly.
     */
    @Deprecated
    @Schema(nullable = true)
    @Getter
    @Setter
    protected Double absIntensityFactor = null;

    /**
     * Factor to convert absolute intensities to MAX norm.
     */
    @Schema(nullable = true)
    @Getter
    @Setter
    protected Double maxNormFactor;

    /**
     * Factor to convert absolute intensities to SUM norm.
     */
    @Schema(nullable = true)
    @Getter
    @Setter
    protected Double sumNormFactor;

    /**
     * Factor to convert absolute intensities to L2 (Euclidean) norm.
     */
    @Schema(nullable = true)
    @Getter
    @Setter
    protected Double l2NormFactor;

    /**
     * Factor to convert absolute intensities to normalize intensities by first peak intensity.
     */
    @Schema(nullable = true)
    @Getter
    @Setter
    protected Double firstPeakNormFactor;


    List<P> getPeaks() {
        return peaks;
    }

    public double[] getMasses() {
        return peaks.stream().mapToDouble(Peak::getMass).toArray();
    }

    public double[] getIntensities() {
        return peaks.stream().mapToDouble(Peak::getIntensity).toArray();
    }

    @Override
    public double getMzAt(int index) {
        return peaks.get(index).getMass();
    }

    @Override
    public double getIntensityAt(int index) {
        return peaks.get(index).getIntensity();
    }


    @Override
    public P getPeakAt(int index) {
        return peaks.get(index);
    }

    @Override
    public int size() {
        return peaks.size();
    }

    @NotNull
    @Override
    public Iterator<P> iterator() {
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < peaks.size();
            }

            @Override
            public P next() {
                return getPeakAt(index++);
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return peaks.isEmpty();
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
        return peaks.stream().mapToDouble(Peak::getIntensity).max().orElse(Double.NaN);
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

    protected void computeNormalizationFactors() {
        maxNormFactor = Spectrums.computeNormalizationScale(this, Normalization.Max);
        sumNormFactor = Spectrums.computeNormalizationScale(this, Normalization.Sum);
        firstPeakNormFactor = Spectrums.computeNormalizationScale(this, Normalization.First);
        l2NormFactor = Spectrums.computeNormalizationScale(this, Normalization.L2);
    }
}
