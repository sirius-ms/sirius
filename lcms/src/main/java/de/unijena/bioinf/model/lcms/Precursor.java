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

package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

public class Precursor implements Peak {

    /**
     * index (scan number) or -1 if number is unknown
     */
    private final int index;
    private final int charge;
    private final IsolationWindow isolationWindow;
    private final float mass, intensity;

    public Precursor(int index, double targetMz, double intensity, int charge, double isolationWindowWidth) {
        this(index, targetMz, intensity, charge, new IsolationWindow(0, isolationWindowWidth));
    }

    public Precursor(int index, double targedMz, double intensity, int charge, IsolationWindow isolationWindow) {
        this.mass = (float)targedMz;
        this.intensity = (float)intensity;
        this.index = index;
        this.charge = charge;
        this.isolationWindow = isolationWindow;
    }

    public int getIndex() {
        return index;
    }

    public int getCharge() {
        return charge;
    }

    public double getIsolationWindowWidth() {
        return isolationWindow.getWindowWidth();
    }

    public double getIsolationWindowOffset() {
        return isolationWindow.getWindowOffset();
    }

    public IsolationWindow getIsolationWindow() {
        return isolationWindow;
    }

    @Override
    /**
     * this represents the isolation window target m/z, if known. Else the selected iom m/z
     */
    public double getMass() {
        return mass;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }
}
