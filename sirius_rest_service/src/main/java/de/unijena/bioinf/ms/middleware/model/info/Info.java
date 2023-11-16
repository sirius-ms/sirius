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

package de.unijena.bioinf.ms.middleware.model.info;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Info {
    /**
     * API version of the SIRIUS Nightsky API
     */
    private final String nightSkyApiVersion;;
    /**
     * Version of the SIRIUS application
     */
    private final String siriusVersion;
    /**
     * Version of the SIRIUS libraries
     */
    private final String siriusLibVersion;
    /**
     * Version of the CSI:FingerID libraries
     */
    private final String fingerIdLibVersion;
    /**
     * Version of the Chemical Database available via SIRIUS web services
     */
    private final String chemDbVersion;
    /**
     * Version of the Machine learning models used for Fingerprint, Compound Class and Structure Prediction
     * Not available if web service is not reachable.
     */
    private final String fingerIdModelVersion;
    /**
     * Version of the Molecular Fingerprint used by SIRIUS
     */
    private final String fingerprintId;
}
