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

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;

/**
 * A spectrum which can be tracked back to a Scan within an LCMS source file
 */
public class Scan {

    /**
     * Unique index usually the scanNumber or scanNumber - 1
     */
    private final int index;

    /**
     * retention time in milliseconds
     */
    private final long retentionTime;

    private final Polarity polarity;

    /**
     * For MS/MS only: precursor information
     */
    private final Precursor precursor;

    private final double TIC;
    private final int numberOfPeaks;
    private final CollisionEnergy collisionEnergy;
    private final boolean centroided;

    public Scan(int index, Polarity polarity, long retentionTime, CollisionEnergy collisionEnergy, int numberOfPeaks, double TIC, boolean centroided) {
        this(index,polarity,retentionTime,collisionEnergy,numberOfPeaks,TIC,centroided,null);
    }

    public Scan(int index, Polarity polarity, long retentionTime, CollisionEnergy collisionEnergy, int numberOfPeaks, double TIC, boolean centroided, Precursor precursor) {
        this.index = index;
        this.retentionTime = retentionTime;
        this.collisionEnergy=collisionEnergy;
        this.precursor = precursor;
        this.polarity = polarity;
        this.TIC = TIC;
        this.numberOfPeaks = numberOfPeaks;
        this.centroided = centroided;
    }

    public boolean isCentroided() {
        return centroided;
    }

    public boolean isProfiled() {
        return !centroided;
    }

    public int getNumberOfPeaks() {
        return numberOfPeaks;
    }

    public double getTIC() {
        return TIC;
    }

    public int getIndex() {
        return index;
    }

    public boolean isMsMs() {
        return precursor!=null;
    }

    public Precursor getPrecursor() {
        return precursor;
    }

    public Polarity getPolarity() {
        return polarity;
    }

    public long getRetentionTime() {
        return retentionTime;
    }

    public CollisionEnergy getCollisionEnergy(){return collisionEnergy;}

    @Override
    public String toString() {
        return precursor!=null ? ("MS/MS " + index + ", m/z = " + precursor.getMass()) : "MS " + index;
    }

}
