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

package de.unijena.bioinf.ms.middleware.model.features;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FeatureImport {
    @Schema(nullable = true)
    protected String name;
    @Schema(nullable = true)
    protected String featureId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected Double ionMass;
    /**
     * Adduct of this feature. If not know specify [M+?]+ or [M+?]- to define the charge
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected String adduct;

    @Schema(nullable = true)
    protected Double rtStartSeconds;
    @Schema(nullable = true)
    protected Double rtEndSeconds;

    /**
     * Mass Spec data of this feature (input data)
     */
    @Schema(nullable = true)
    protected BasicSpectrum mergedMs1;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<BasicSpectrum> ms1Spectra;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<BasicSpectrum> ms2Spectra;
}
