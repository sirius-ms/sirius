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

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import jakarta.persistence.Id;
import lombok.*;

/**
 * A measured MS/MS Spectrum (usually MS2) with metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class MSMSScan extends AbstractScan {
    @Builder
    public MSMSScan(long scanId, long runId, String scanNumber, double scanTime, double ccs, SimpleSpectrum peaks, long precursorScanId, byte msLevel, IsolationWindow isolationWindow, CollisionEnergy collisionEnergy, double mzOfInterest) {
        super(runId, scanNumber, scanTime, ccs, peaks);
        this.scanId = scanId;
        this.precursorScanId = precursorScanId;
        this.msLevel = msLevel;
        this.isolationWindow = isolationWindow;
        this.collisionEnergy = collisionEnergy;
        this.mzOfInterest = mzOfInterest;
    }

    /**
     * Database ID
     */
    @Id
    protected long scanId;

    protected Long featureId;
    protected Long precursorScanId;

    protected byte msLevel;

    /**
     * Isolation window used to filter the precursor ions in Da
     */
    protected IsolationWindow isolationWindow;

    /**
     * Target precursor mz
     */
    protected Double mzOfInterest;


    /**
     * Collision Energy (CE) in eV
     */
    protected CollisionEnergy collisionEnergy;

    //we can add measurement parameters for other fragmentation techniques as separate fields if needed
}
