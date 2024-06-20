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

package de.unijena.bioinf.ms.middleware.model.annotations;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LipidAnnotation {

    /**
     * Predicted lipid species in LIPID MAPS notation.
     * NULL if not classified as lipid.
     */
    @Schema(nullable = true)
    private final String lipidSpecies;

    /**
     * LIPID MAPS id of the predicted lipid class.
     */
    @Schema(nullable = true)
    private final String lipidMapsId;

    /**
     * Human-readable name of the predicted lipid class.
     */
    @Schema(nullable = true)
    private final String lipidClassName;

    /**
     * Hypothetical molecular structure of the predicted lipid species as SMILES.
     * NULL if hypothetical structure not available.
     */
    @Schema(nullable = true)
    private final String hypotheticalStructure;

    /**
     * True of the formula composition of the chains could not be determined from the MS/MS.
     */
    @Schema(nullable = true)
    private boolean chainsUnknown;
}