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

/**
 * Link from annotated fragment peak to its parent fragment peak connected by their neutral loss.
 */
@Getter
@Builder
public class ParentPeak {
    /**
     * Index to the parent peak connected by this loss in this particular spectrum
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parentIdx;

    /**
     * Identifier of the parent fragment connected via this loss. Can be used to map fragments and peaks
     * among fragmentation trees and spectra.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parentFragmentId;

    /**
     * Molecular formula of the neutral loss that connects these two peaks.
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String lossFormula;
}
