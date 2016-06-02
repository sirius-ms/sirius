/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
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
    public double getPrecursorMz();

    /**
     * @return the collision energy (type) of the fragmentation cell
     */
    public CollisionEnergy getCollisionEnergy();

    /**
     * (OPTIONAL)
     * @return  the total number of measured ions in the spectrum if given, otherwise 0.
     */
    public double getTotalIonCount();

    /**
     * The ionization of the fragment peaks - assuming that all fragments have the same ionization.
     */
    public Ionization getIonization();

    /**
     * The MS level. use 1 for MS1 and 2 for MS2 spectra.
     * @return
     */
    public int getMsLevel();

    /**
     * For future:
     * - getCollisionType => (hard ionisation, soft ionization, CID or ACPI, and so on....)
     */

}
