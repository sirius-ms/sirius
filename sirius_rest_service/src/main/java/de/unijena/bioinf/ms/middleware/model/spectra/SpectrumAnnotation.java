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


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpectrumAnnotation {

    /**
     * Molecular formula that has been annotated to this spectrum
     */
    @Schema(nullable = true)
    private String molecularFormula;

    /**
     * Ionization that has been annotated to this spectrum
     */
    @Schema(nullable = true)
    private String ionization;

    /**
     * Exact mass based on the annotated molecular formula and ionization
     */
    @Schema(nullable = true)
    private Double exactMass;

    /**
     * Absolute mass deviation of the exact mass to the precursor mass (precursorMz) of this spectrum in mDa
     */
    @Schema(nullable = true)
    private Double massDeviationMz;

    /**
     * Relative mass deviation of the exact mass to the precursor mass (precursorMz) of this spectrum in ppm
     */
    @Schema(nullable = true)
    private Double massDeviationPpm;

    /**
     * Smiles of the structure candidate used to derive substructure peak annotations via epimetheus insilico fragmentation
     * Substructure highlighting (bond and atom indices) refer to this specific SMILES.
     * If you standardize or canonicalize this SMILES in any way the indices of substructure highlighting might
     * not match correctly anymore.
     *
     * Null if substructure annotation not available or not requested.
     */
    @Schema(nullable = true)
    private String structureAnnotationSmiles;

    /**
     * Overall score of all substructure annotations computed for this structure candidate (structureAnnotationSmiles)
     *
     * Null if substructure annotation not available or not requested.
     */
    @Schema(nullable = true)
    private Double structureAnnotationScore;
}
