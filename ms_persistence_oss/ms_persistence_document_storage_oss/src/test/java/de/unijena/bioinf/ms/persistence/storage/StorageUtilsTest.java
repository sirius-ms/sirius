/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The MS1 intensity fallback for features that were imported without an LC-MS trace: the intensity of the
 * precursor peak in the merged MS1 spectrum.
 */
public class StorageUtilsTest {

    private static final Deviation DEV = new Deviation(10);

    /**
     * A realistic MS1 window around a C20 compound ([M+H]+ at 301.2162): the monoisotopic peak, its 13C
     * isotopologue about 22 % as high (20 carbons x ~1.1 %), a smaller M+2, a co-eluting unrelated ion and
     * some noise.
     */
    private static SimpleSpectrum ms1() {
        return new SimpleSpectrum(
                new double[]{300.8011, 301.2162, 302.2195, 303.2228, 305.1022, 310.4411},
                new double[]{1_200d, 2_400_000d, 528_000d, 60_000d, 3_100_000d, 900d});
    }

    @Test
    public void testPrecursorPeakNotItsIsotopologueNorAStrongerNeighbour() {
        // the co-eluting ion at 305.10 is more intense than the precursor, the M+1 is within 1 Da: neither counts
        assertEquals(2_400_000d, StorageUtils.precursorIntensityOf(ms1(), 301.2162, DEV));
    }

    @Test
    public void testMostIntensePeakWithinTheMassWindowWins() {
        // two peaks inside the 10 ppm window (a split peak): the higher one is the precursor intensity
        SimpleSpectrum split = new SimpleSpectrum(
                new double[]{301.2150, 301.2170},
                new double[]{100_000d, 150_000d});
        assertEquals(150_000d, StorageUtils.precursorIntensityOf(split, 301.2162, DEV));
    }

    @Test
    public void testNoPeakWithinTheMassWindow() {
        // the precursor is missing from the MS1 (a common case for weak precursors)
        assertNull(StorageUtils.precursorIntensityOf(ms1(), 450.3000, DEV));
    }

    @Test
    public void testEmptyOrMissingSpectrum() {
        assertNull(StorageUtils.precursorIntensityOf(new SimpleSpectrum(new double[0], new double[0]), 301.2162, DEV));
        assertNull(StorageUtils.precursorIntensityOf(null, 301.2162, DEV));
    }

    @Test
    public void testNonPositiveIntensityIsNoIntensity() {
        SimpleSpectrum zero = new SimpleSpectrum(new double[]{301.2162}, new double[]{0d});
        assertNull(StorageUtils.precursorIntensityOf(zero, 301.2162, DEV));
    }
}
