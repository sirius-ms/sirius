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

package de.unijena.bioinf.ms.middleware.model.info;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Map;

@Getter
@Builder
public class Info {
    /**
     * API version of the SIRIUS Nightsky API
     */
    @Schema(nullable = true)
    private final String nightSkyApiVersion;;
    /**
     * Version of the SIRIUS application
     */
    @Schema(nullable = true)
    private final String siriusVersion;

    /**
     * Latest available Version of the SIRIUS application
     */
    @Schema(nullable = true)
    private final String latestSiriusVersion;

    /**
     * Link to the latest available Version of the SIRIUS application
     */
    @Schema(nullable = true)
    private final String latestSiriusLink;

    /**
     * true if newer SIRIUS version is available
     */
    @Schema(nullable = false)
    private final boolean updateAvailable;


    /**
     * Version of the SIRIUS libraries
     */
    @Schema(nullable = true)
    private final String siriusLibVersion;
    /**
     * Version of the CSI:FingerID libraries
     */
    @Schema(nullable = true)
    private final String fingerIdLibVersion;
    /**
     * Version of the Chemical Database available via SIRIUS web services
     */
    @Schema(nullable = true)
    private final String chemDbVersion;
    /**
     * Version of the Machine learning models used for Fingerprint, Compound Class and Structure Prediction
     * Not available if web service is not reachable.
     */
    @Schema(nullable = true)
    private final String fingerIdModelVersion;
    /**
     * Version of the Molecular Fingerprint used by SIRIUS
     */
    @Schema(nullable = true)
    private final String fingerprintId;

    /**
     * Set of solvers that are configured correctly and can be loaded
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final EnumSet<TreeBuilderFactory.DefaultBuilder> availableILPSolvers;

    /**
     * Set of ILP Solvers that are Supported and their version information
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final Map<TreeBuilderFactory.DefaultBuilder, String> supportedILPSolvers =  Map.of(
            TreeBuilderFactory.DefaultBuilder.CLP, TreeBuilderFactory.CBC_VERSION,
            TreeBuilderFactory.DefaultBuilder.CPLEX, TreeBuilderFactory.CPLEX_VERSION,
            TreeBuilderFactory.DefaultBuilder.GUROBI, TreeBuilderFactory.GUROBI_VERSION
    );
}
