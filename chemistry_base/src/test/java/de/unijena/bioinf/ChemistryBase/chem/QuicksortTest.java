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
package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class QuicksortTest {
    @Test
    public void testQuicksort() {
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        final Random random = new Random(10323l);
        for (int i=0; i < 1000; ++i) {
            spec.addPeak(new SimplePeak(random.nextDouble(), 1d));
        }
        Spectrums.sortSpectrumByMass(spec);
        for (int i=1; i < 1000; ++i) {
            assertTrue(("" + spec.getMzAt(i) + " >= " + spec.getMzAt(i-1)), spec.getMzAt(i) >= spec.getMzAt(i-1));
        }
    }

}
