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

package de.unijena.bioinf.ms.persistence.model.core.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import lombok.Getter;

@Getter
public class IsotopePattern extends SimpleSpectrum {

    public enum Type {
        AVERAGE, REPRESENTATIVE, MERGED_APEX
    }

    public IsotopePattern() {
        super();
        this.type = Type.MERGED_APEX;
    }

    public IsotopePattern(SimpleSpectrum spec, Type type) {
        super(spec);
        this.type = type;
    }

    protected IsotopePattern(double[] masses, double[] intensities, boolean NoCopyAndOrder, Type type) {
        super(masses, intensities, NoCopyAndOrder);
        this.type = type;
    }

    public IsotopePattern(double[] masses, double[] intensities, Type type) {
        super(masses, intensities);
        this.type = type;
    }

    public <T extends Peak, S extends Spectrum<T>> IsotopePattern(S ts, Type type) {
        super(ts);
        this.type = type;
    }

    private final Type type;

}
