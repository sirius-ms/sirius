/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;


@Getter
public class Precursor implements Peak {

    private final int scanId;
    @Nullable  private final String scanIdentifier;
    private final int charge;
    private final IsolationWindow isolationWindow;
    /**
     * mass represents the isolation window target m/z, if known. Else the selected iom m/z
     */
    private final double mass, intensity;

    public Precursor(@Nullable  String scanIdentifier, int scanId, double targedMz, double intensity, int charge, IsolationWindow isolationWindow) {
        this.mass = targedMz;
        this.intensity = intensity;
        this.scanIdentifier = scanIdentifier;
        this.scanId = scanId;
        this.charge = charge;
        this.isolationWindow = isolationWindow;
    }

}
