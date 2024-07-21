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
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@SuperBuilder
@Jacksonized
public class ConsensusAnnotationsDeNovo extends ConsensusAnnotations{

    @Schema(enumAsRef = true, name = "ConsensusCriterionDeNovo", nullable = true)
    public enum Criterion {
        MAJORITY_FORMULA,
        TOP_FORMULA,
        SINGLETON_FORMULA
    }

    @Schema(nullable = true)
    protected Criterion selectionCriterion;

//    /**
//     * DeNovo Structure candidate (predicted with MSNovelist) corresponding to the molecularFormula
//     * Might be null if no consensus structure is available.
//     */
//    protected StructureCandidate structureMsNovelist;
}
