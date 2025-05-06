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
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
        computeNormalizationFactors();
    }

    public BasicSpectrum(@NotNull Spectrum<Peak> spec) {
        this(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec));
    }

    public BasicSpectrum(double[] masses, double[] intensities) {
        init(masses, intensities);
    }

    protected void init(double[] masses, double[] intensities) {
        if (masses == null)
            throw new IllegalArgumentException("Masses are Null but must be non Null.");
        if (intensities == null)
            throw new IllegalArgumentException("Intensities are Null but must be non Null.");

        if (masses.length != intensities.length)
            throw new IllegalArgumentException("Masses and Intensities do not have same length but must have.");

        peaks = new ArrayList<>(masses.length);

        for (int i = 0; i < masses.length; i++)
            peaks.add(new SimplePeak(masses[i], intensities[i]));

        computeNormalizationFactors();
    }
}
