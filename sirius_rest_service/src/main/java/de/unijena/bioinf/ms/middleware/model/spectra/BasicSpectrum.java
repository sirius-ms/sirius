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
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import it.unimi.dsi.fastutil.Pair;
import lombok.NoArgsConstructor;
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
public class BasicSpectrum extends AbstractSpectrum<SimplePeak> {

    public BasicSpectrum(@NotNull List<SimplePeak> peaks) {
        this.peaks = peaks;
    }

    public BasicSpectrum(@NotNull Spectrum<Peak> spec) {
        this(spec, true);
    }

    public BasicSpectrum(@NotNull Spectrum<Peak> spec, boolean makeRelative) {
        Double scale = null;
        if (makeRelative) {
            final double maxInt = spec.getMaxIntensity();
            //check if spectrum is not normalized or max normalized
            if (maxInt > 1d || Math.abs(maxInt - 1d) < 0.000001d){
                Pair<SimpleSpectrum, Double> specAndScale = Spectrums.getNormalizedSpectrumWithScale(spec, Normalization.Sum);
                spec = specAndScale.first();
                scale = specAndScale.second();
            }
        }
        init(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec), scale);
    }

    public BasicSpectrum(double[] masses, double[] intensities, @Nullable Double intFactor) {
        init(masses, intensities, intFactor);
    }

    protected void init(double[] masses, double[] intensities, @Nullable Double intFactor) {
        if (masses == null)
            throw new IllegalArgumentException("Masses are Null but must be non Null.");
        if (intensities == null)
            throw new IllegalArgumentException("Intensities are Null but must be non Null.");

        if (masses.length != intensities.length)
            throw new IllegalArgumentException("Masses and Intensities do not have same length but must have.");

        peaks = new ArrayList<>(masses.length);

        for (int i = 0; i < masses.length; i++)
            peaks.add(new SimplePeak(masses[i], intensities[i]));

        absIntensityFactor = intFactor;
    }

    /**
     *
     * @param ref
     * @param renormalize if true, the square root transformation that was applied on library spectra is removed
     * @return
     */
    public static BasicSpectrum from(ReferenceSpectrum ref, boolean renormalize) {
        Spectrum<Peak> s = ref.getQuerySpectrum();
        if (renormalize) {
            SimpleMutableSpectrum buf = new SimpleMutableSpectrum(s);
            for (int j=0; j < buf.size(); ++j) {
                buf.setIntensityAt(j, buf.getIntensityAt(j)*buf.getIntensityAt(j));
            }
        }

        BasicSpectrum spec = new BasicSpectrum(s);
        // basic information
        spec.setMsLevel(2);
        spec.setName(ref.getName());
        spec.setPrecursorMz(ref.getPrecursorMz());
        // extended information
        if (ref instanceof Ms2ReferenceSpectrum) {
            Ms2ReferenceSpectrum ms2ref = (Ms2ReferenceSpectrum) ref;
            if (ms2ref.getInstrumentation() != null) {
                spec.setInstrument(ms2ref.getInstrumentation().description());
            } else if (ms2ref.getInstrumentType() != null && ms2ref.getInstrument() != null
                    && !ms2ref.getInstrumentType().isBlank() && !ms2ref.getInstrument().isBlank()) {
                spec.setInstrument(ms2ref.getInstrumentType() + " (" + ms2ref.getInstrument() + ")");
            } else if (ms2ref.getInstrumentType() != null && !ms2ref.getInstrumentType().isBlank()) {
                spec.setInstrument(ms2ref.getInstrumentType());
            } else if (ms2ref.getInstrument() != null && !ms2ref.getInstrument().isBlank()) {
                spec.setInstrument(ms2ref.getInstrument());
            }
            if (ms2ref.getCollisionEnergy() != null) {
                spec.setCollisionEnergy(ms2ref.getCollisionEnergy());
            } else {
                spec.setCollisionEnergyStr(ms2ref.getCe());
            }
            spec.setCollisionEnergy(ms2ref.getCollisionEnergy());;
        } else if (ref instanceof MergedReferenceSpectrum) {
            spec.setCollisionEnergy(CollisionEnergy.none());
        }
        return spec;
    }
}
