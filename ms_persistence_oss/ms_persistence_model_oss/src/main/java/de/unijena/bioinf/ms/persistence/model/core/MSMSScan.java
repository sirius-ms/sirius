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

package de.unijena.bioinf.ms.persistence.model.core;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import lombok.*;
import one.microstream.reference.Lazy;

/**
 * A measured MS/MS Spectrum (usually MS2) with metadata.
 */
@Getter
@Setter
@NoArgsConstructor
public class MSMSScan extends Scan {

    @Builder(builderMethodName = "builderMsMs")
    public MSMSScan(Run run, String scanNumber, RetentionTime retentionTime, Double ccs, SimpleSpectrum peaks, Lazy<Scan> precursorScan, byte msLevel, IsolationWindow isolationWindow, CollisionEnergy collisionEnergy, Double mzOfInterest) {
        super(run, scanNumber, retentionTime, ccs, peaks);
        this.precursorScan = precursorScan;
        this.msLevel = msLevel;
        this.isolationWindow = isolationWindow;
        this.collisionEnergy = collisionEnergy;
        this.mzOfInterest = mzOfInterest;
    }

    /**
     * MS1 scan of the precursion ion.
     */
    protected Lazy<Scan> precursorScan;

    protected byte msLevel;

    /**
     * Isolation window used to filter the precursor ions
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

    //just add measurement parameters for other fragmentation techniques as separate fields if needed
}
