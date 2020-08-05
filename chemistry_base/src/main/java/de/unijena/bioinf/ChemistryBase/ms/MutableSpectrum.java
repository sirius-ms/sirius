
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

public interface MutableSpectrum<T extends Peak> extends Spectrum<T> {

    void addPeak(T peak);

    void addPeak(double mz, double intensity);

    void setPeakAt(int index, T peak);

    void setMzAt(int index, double mass);

    void setIntensityAt(int index, double intensity);

    Peak removePeakAt(int index);

    void swap(int index1, int index2);

    default void clear() {
        for (int k=size()-1; k >= 0; --k) {
            removePeakAt(k);
        }
    }

}
