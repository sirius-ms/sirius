
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

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;

/**
 * Minimal interface for MS2 data
 */
public interface Ms2Spectrum<P extends Peak> extends Spectrum<P> {

    /**
     * @return the mass-to-charge ratio of the precursor ion
     */
    double getPrecursorMz();

    /**
     * (OPTIONAL)
     * @return  the total number of measured iondetection in the spectrum if given, otherwise 0.
     */
    double getTotalIonCount();

    /**
     * The ionization of the fragment peaks - assuming that all fragments have the same ionization.
     */
    Ionization getIonization();


    /**
     * For future:
     * - getCollisionType => (hard ionisation, soft ionization, CID or ACPI, and so on....)
     */

}
