
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


public interface Spectrum<T extends Peak> extends Iterable<T>, Cloneable {

    double getMzAt(int index);

    double getIntensityAt(int index);

    T getPeakAt(int index);

    int size();

    // methods for convenience

    default boolean isEmpty() {
        return size()==0;
    }


    /*
     * This are the only extensions we need to be Compatible with myxo viewers and stuff, i know this is not the perfect model
     * but it is painless and costs no memory for non MSn data.
     *
     * TODO: Kai: PLEASE remove that as soon as we get rid of this myso stuff
     */

    /**
     * @return the collision energy (type) of the fragmentation cell. If no MSn data it returns CollisionEnergy.none()
     */
    default CollisionEnergy getCollisionEnergy() {
        return CollisionEnergy.none();
    }

    /**
     * The MS level. use 1 for MS1 and 2 for MS2 spectra.
     *
     * @return MS of the spectrum
     */
    default int getMsLevel() {
        return 1;
    }

    /**
     * @return The highest intensity of all peaks in the spectrum
     */
    default double getMaxIntensity() {
        final int s = size();
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < s; i++) {
            max = Math.max(getIntensityAt(i), max);
        }
        return max;
    }

}
