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

import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.Locale;
import java.util.Objects;

public class ScanPoint implements Peak {
    private final int scanNumber;
    private final long retentionTime;
    private final float mass, intensity;

    public ScanPoint(int scanNumber, long retentionTime, double mz, double intensity) {
        this.mass = (float)mz;
        this.intensity = (float)intensity;
        this.scanNumber = scanNumber;
        this.retentionTime = retentionTime;
    }

    public ScanPoint(Scan scan, double mass, double intensity) {
        this(scan.getIndex(), scan.getRetentionTime(), mass, intensity);

    }

    public int getScanNumber() {
        return scanNumber;
    }

    public long getRetentionTime() {
        return retentionTime;
    }

    public String toString() {
        return String.format(Locale.US, "m/z = %.5f, intensity = %.1f, scanID = %d, rt = %.2f", mass,intensity,scanNumber, retentionTime/60000d);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScanPoint scanPoint = (ScanPoint) o;
        return scanNumber == scanPoint.scanNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), scanNumber);
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }
}
