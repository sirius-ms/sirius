/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class AnnotatedSpectrum extends AbstractSpectrum<AnnotatedPeak> {
    /**
     * Optional Annotations of this spectrum.
     */
    @Schema(nullable = true)
    @Getter
    @Setter
    private SpectrumAnnotation spectrumAnnotation;

    public AnnotatedSpectrum(@NotNull List<AnnotatedPeak> peaks) {
        this.peaks = peaks;
    }

    public AnnotatedSpectrum(@NotNull Spectrum<Peak> spec) {
        this(spec, true);
    }

    public AnnotatedSpectrum(@NotNull Spectrum<Peak> spec, boolean makeRelative) {
        Double factor = null;
        if (makeRelative) {
            double maxInt = spec.getMaxIntensity();
            if (maxInt > 1d){
                factor = maxInt;
                spec = Spectrums.getNormalizedSpectrum(spec, Normalization.Max);
            }
        }
        init(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec), factor, null);
    }

    public AnnotatedSpectrum(double[] masses, double[] intensities, @Nullable Double intFactor, @Nullable PeakAnnotation[] peakAnnotations) {
        init(masses, intensities, intFactor, peakAnnotations);
    }

    protected void init(double[] masses, double[] intensities, @Nullable Double intFactor, @Nullable PeakAnnotation[] peakAnnotations) {
        if (masses == null)
            throw new IllegalArgumentException("Masses are Null but must be non Null.");
        if (intensities == null)
            throw new IllegalArgumentException("Intensities are Null but must be non Null.");

        if (masses.length != intensities.length)
            throw new IllegalArgumentException("Masses and Intensities do not have same length but must have.");

        peaks = new ArrayList<>(masses.length);
        if (peakAnnotations != null) {
            for (int i = 0; i < masses.length; i++)
                peaks.add(new AnnotatedPeak(masses[i], intensities[i], peakAnnotations[i]));
        } else {
            for (int i = 0; i < masses.length; i++)
                peaks.add(new AnnotatedPeak(masses[i], intensities[i], null));
        }

        absIntensityFactor = intFactor;
    }




    public PeakAnnotation getPeakAnnotationAt(int index) {
        return peaks.get(index).getPeakAnnotation();
    }
}
