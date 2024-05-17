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

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import jakarta.persistence.Id;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// TODO: kaidu: warum ist MSData eine n:1 relationship zu AlignedFeature. Es sollte eine 1:1 relationship sein
public class MSData {

    @Id
    private long msDataId;

    /**
     * ID of the aligned feature that own this ms data. Can be the id of an AlignedIsotopicFeatures or an AlignedFeature
     */
    private long alignedFeatureId;

    /**
     * MSn spectra
     * @markus: previously we had something like a merged msn spectrum here. I do not know that this is supposed to be,
     * but if we need something like this (e.g. for visualization) we either can compute it on the fly or add it
     * again here.
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

}
