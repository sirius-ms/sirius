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
import de.unijena.bioinf.chemdb.DBLink;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = { "molecularFormula", "adduct", "csiScore", "tanimotoSimilarity", "confidenceExactMatch", "confidenceApproxMatch", "fingerprint"}, ignoreUnknown = true)
public class StructureCandidate {

    protected String inchiKey;
    protected String smiles;


    @Schema(nullable = true)
    protected String structureName;

    @Schema(nullable = true)
    protected Double xlogP;

    //Extended Results
    /**
     * List of structure database links belonging to this structure candidate
     * OPTIONAL: needs to be added by parameter
     */
    @Schema(nullable = true)
    List<DBLink> dbLinks;
    /**
     * List of spectral library matches belonging to this structure candidate
     * OPTIONAL: needs to be added by parameter
     */
    @Schema(nullable = true)
    List<SpectralLibraryMatch> spectralLibraryMatches;
}
