/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollisionEnergyTest {

    @Test
    void rangeKeepsItsMaximum() {
        CollisionEnergy ce = new CollisionEnergy(20, 40);
        assertEquals(20, ce.minEnergySource(), 1e-12);
        assertEquals(40, ce.maxEnergySource(), 1e-12);
        assertEquals(40, ce.getMaxEnergy(false), 1e-12);
        assertTrue(ce.isRamp());
    }

    @Test
    void rampIsParsedAsRange() {
        CollisionEnergy ce = CollisionEnergy.fromString("20-40 eV");
        assertEquals(20, ce.getMinEnergy(false), 1e-12);
        assertEquals(40, ce.getMaxEnergy(false), 1e-12);
    }

    @Test
    void copyWithoutCorrectionKeepsRange() {
        CollisionEnergy corrected = new CollisionEnergy(25, 50, 20, 40);
        CollisionEnergy copy = CollisionEnergy.copyWithoutCorrection(corrected);
        assertFalse(copy.isCorrected());
        assertEquals(20, copy.minEnergySource(), 1e-12);
        assertEquals(40, copy.maxEnergySource(), 1e-12);
    }

    @Test
    void singleEnergyIsNoRamp() {
        CollisionEnergy ce = new CollisionEnergy(35);
        assertEquals(35, ce.getMinEnergy(false), 1e-12);
        assertEquals(35, ce.getMaxEnergy(false), 1e-12);
        assertFalse(ce.isRamp());
    }

    @Test
    void minimumAboveMaximumIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CollisionEnergy(40, 20));
    }
}
