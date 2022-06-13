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

package de.unijena.bioinf.ms.middleware.compute.model;

import de.unijena.bioinf.ms.middleware.compute.model.tools.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Object to submit a job to be executed by SIRIUS
 */
@Getter
@Setter
public class JobSubmission {
    /**
     * Compounds that should be the input for this Job
     */
    List<String> compoundIds;

    /**
     * Indicate if already existing result for a tool to be executed should be overwritten or not.
     */
    boolean recompute;

    /**
     * Parameter Object for molecular formula identification tool (CLI-Tool: formula, sirius).
     * If NULL the tool will not be executed.
     */
    Sirius formulaIdParas;
    /**
     * Parameter Object for network  based molecular formula re-ranking (CLI-Tool: zodiac).
     * If NULL the tool will not be executed.
     */
    Zodiac zodiacParas;
    /**
     * Parameter Object for Fingerprint prediction with CSI:FingerID (CLI-Tool: fingerint).
     * If NULL the tool will not be executed.
     */
    FingerprintPrediction fingerprintPredictionParas;
    /**
     * Parameter Object for structure database search with CSI:FingerID (CLI-Tool: structure).
     * If NULL the tool will not be executed.
     */
    StructureDbSearch structureDbSearchParas;
    /**
     * Parameter Object for CANOPUS compound class prediction tool (CLI-Tool: canopus).
     * If NULL the tool will not be executed.
     */
    Canopus canopusParas;
}
