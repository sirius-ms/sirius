/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.detection;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import lombok.Getter;

@Getter
class MaxPeakSpectrum extends SimpleMutableSpectrum {

    private int maxIndex;

    public <T extends Peak, S extends Spectrum<T>> MaxPeakSpectrum(S immutable) {
        super(immutable);
        maxIndex = Spectrums.getIndexOfPeakWithMaximalIntensity(this);
    }

    @Override
    public Peak removePeakAt(int index) {
        Peak peak = super.removePeakAt(index);
        if (index < maxIndex) {
            maxIndex--;
        } else if (index == maxIndex) {
            maxIndex = Spectrums.getIndexOfPeakWithMaximalIntensity(this);
        }
        return peak;
    }

}
