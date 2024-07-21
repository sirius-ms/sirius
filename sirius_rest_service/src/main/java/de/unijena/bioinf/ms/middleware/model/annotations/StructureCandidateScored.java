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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = { "molecularFormula", "adduct", "formulaId"}, ignoreUnknown = true)
public class StructureCandidateScored extends StructureCandidate {
    @Schema(enumAsRef = true, name = "StructureCandidateOptField", nullable = true)
    public enum OptField {none, fingerprint, dbLinks, libraryMatches}

    /**
     * the overall rank of this candidate among all candidates of this feature
     */
    protected Integer rank;

    /**
     * CSI:FingerID score of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID
     * This is the score used for ranking structure candidates
     */
    protected Double csiScore;
    /**
     * Tanimoto similarly of the fingerprint of this compound to the predicted fingerprint of CSI:FingerID
     */
    @Schema(nullable = true)
    protected Double tanimotoSimilarity;

    /**
     * Maximum Common Edge Subgraph (MCES) distance to the top scoring hit (CSI:FingerID) in a candidate list.
     * @see <a href="https://doi.org/10.1101/2023.03.27.534311">Small molecule machine learning: All models are wrong, some may not even be useful</a>
     */
    @Schema(nullable = true)
    protected Double mcesDistToTopHit;

    //Extended Results
    /**
     * Array containing the indices of the molecular fingerprint that are available in the structure (1 if present)
     * OPTIONAL: needs to be added by parameter
     */
    @Schema(nullable = true)
    BinaryFingerprint fingerprint;
}
