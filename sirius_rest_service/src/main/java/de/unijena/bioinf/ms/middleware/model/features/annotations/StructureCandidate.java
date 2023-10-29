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

package de.unijena.bioinf.ms.middleware.model.features.annotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.chemdb.DBLink;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({ "molecularFormula", "adduct"})
public class StructureCandidate {
    public enum OptFields {fingerprint, dbLinks, refSpectraLinks, pubmedIds}

    protected String structureName;
    protected String smiles;

    protected Double csiScore;
    protected Double tanimotoSimilarity;
    protected Double confidenceScore;

    protected Integer numOfPubMedIds;
    protected Double xlogP;
    protected String inchiKey;

    //Extended Results
    /**
     * Array containing the indices of the molecular fingerprint that are available in the structure (1 if present)
     * OPTIONAL: needs to be added by parameter
     */
    BinaryFingerprint fingerprint;
    /**
     * List of structure database links belonging to this structure candidate
     * OPTIONAL: needs to be added by parameter
     */
    List<DBLink> dbLinks;
    /**
     * List of spectral library links belonging to this structure candidate
     * OPTIONAL: needs to be added by parameter
     */
    List<DBLink> refSpectraLinks;
    /**
     * PubMed IDs belonging to this structure candidate
     * OPTIONAL: needs to be added by parameter
     */
    int[] pubmedIds;



    //todo add spectral library
}
