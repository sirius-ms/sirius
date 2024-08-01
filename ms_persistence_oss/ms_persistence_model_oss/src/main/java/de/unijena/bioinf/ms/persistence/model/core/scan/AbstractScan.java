/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.persistence.model.core.scan;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import lombok.*;

/**
 * A measured Mass Spectrum (usually MS1) with metadata.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AbstractScan {
    /**
     * Database ID of the Run this Scan belongs to
     */
    protected long runId;

    /**
     * mzML spectrum ID or mzXML scan num
     */
    protected String sourceScanId;

    /**
     * Time this scan took place (in minutes)
     */
    protected double scanTime;

    /**
     * Collisional Cross-Section (CCS) in Å^2
     */
    protected double ccs;

    /**
     * The actual spectrum that has been measured (masses and intensities)
     */
    @ToString.Exclude
    protected SimpleSpectrum peaks;
}
