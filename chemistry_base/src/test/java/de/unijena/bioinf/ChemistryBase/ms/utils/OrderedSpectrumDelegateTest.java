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

package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrderedSpectrumDelegateTest {

    @Test
    public void testConstructor() {
        new OrderedSpectrumDelegate<>(SimpleSpectrum.empty());
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {1d}, new double[] {1d}));
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {1d, 1d}, new double[] {1d, 1d}));
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {1d, 2d}, new double[] {1d, 1d}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnordered() {
        new OrderedSpectrumDelegate<>(Spectrums.wrap(new double[] {2d, 1d}, new double[] {1d, 1d}));
    }

    @Test
    public void testDelegation() {
        Spectrum<Peak> innerSpectrum = Spectrums.wrap(new double[]{1d, 2d}, new double[]{1d, 1d});
        OrderedSpectrumDelegate<Peak> spectrum = new OrderedSpectrumDelegate<>(innerSpectrum);
        assertTrue(Spectrums.haveEqualPeaks(innerSpectrum, spectrum));
    }
}