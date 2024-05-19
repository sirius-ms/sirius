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

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import jakarta.persistence.Id;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MSData {

    /**
     * ID of the aligned feature that own this ms data. Can be the id of an AlignedIsotopicFeatures or an AlignedFeature.
     * Is also pkey of this object.
     */
    @Id
    private long alignedFeatureId;

    /**
     * MSn spectra merge among features form multiple aligned MS runs
     */
    private List<MergedMSnSpectrum> msnSpectra;

    /**
     * Extracted isotope pattern
     */
    private IsotopePattern isotopePattern;

    /**
     * A merged or representative MS1 spectrum e.g. for visualization in the gui
     */
    private SimpleSpectrum mergedMs1Spectrum;

    /**
     * A merged or representative MSm spectrum e.g. for visualization in the gui.
     * This spectrum is usually based on the `msnSpectra`
     */
    private SimpleSpectrum mergedMSnSpectrum;

}
