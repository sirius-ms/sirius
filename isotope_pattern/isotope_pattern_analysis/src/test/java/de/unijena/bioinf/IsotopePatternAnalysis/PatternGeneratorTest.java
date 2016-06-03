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
package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistributionJSONFile;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatternGeneratorTest {

	@Test
	public void testNonChargedPatternGeneration() {
		try {
            PeriodicTable.getInstance().setDistribution(new IsotopicDistributionJSONFile().fromClassPath("/chemcalc_distribution.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		// Generator
		final PatternGenerator generator = new PatternGenerator(new Charge(1), Normalization.Max(100));
		// test data
		final MolecularFormula mol = MolecularFormula.parse("C6H12O6");
		// test
		final Spectrum<? extends Peak> spectrum = generator.generatePattern(mol, 6);
		assertEquals(6, spectrum.size());
		assertEquals(180.06, spectrum.getMzAt(0), 0.01);
		assertEquals(181.07, spectrum.getMzAt(1), 0.01);
		assertEquals(182.07, spectrum.getMzAt(2), 0.01);
		assertEquals(183.07, spectrum.getMzAt(3), 0.01);
		assertEquals(184.07, spectrum.getMzAt(4), 0.01);
		assertEquals(100, spectrum.getIntensityAt(0), 0.01);
		assertEquals(6.856, spectrum.getIntensityAt(1), 0.001);
		assertEquals(1.432, spectrum.getIntensityAt(2), 0.001);
		assertEquals(0.0866, spectrum.getIntensityAt(3), 0.001);
		assertEquals(0.009, spectrum.getIntensityAt(4), 0.001);

	}

    @Test
    public void testStrangePatternGeneration() {
        try {
            PeriodicTable.getInstance().setDistribution(new IsotopicDistributionJSONFile().fromClassPath("/chemcalc_distribution.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Generator
        final PatternGenerator generator = new PatternGenerator(new Charge(1), Normalization.Max(100));
        // test data
        final MolecularFormula mol = MolecularFormula.parse("C27H42FeN9O12");
        // test
        final Spectrum<? extends Peak> spectrum = generator.generatePattern(mol, 5);
        assertEquals(5, spectrum.size());
        assertEquals(738.2, spectrum.getMzAt(0), 0.1);
        assertEquals(739.2, spectrum.getMzAt(1), 0.1);
        assertEquals(740.2, spectrum.getMzAt(2), 0.1);
        assertEquals(741.2, spectrum.getMzAt(3), 0.1);
        assertEquals(742.2, spectrum.getMzAt(4), 0.1);
        final MutableSpectrum<Peak> s = new SimpleMutableSpectrum(spectrum);
        Spectrums.normalize(s, Normalization.Max(100));
        assertEquals(6.3384650311, s.getIntensityAt(0), 0.1);
        assertEquals(2.121285592, s.getIntensityAt(1), 0.01);
        assertEquals(100, s.getIntensityAt(2), 0.01);
        assertEquals(35.6837261759, s.getIntensityAt(3), 0.01);
        assertEquals(8.9481921844, s.getIntensityAt(4), 0.01);

        final Spectrum<?> testP = generator.generatePatternWithTreshold(mol, 1e-3);
        assertTrue("for ferrum: third peak is greater than first peak", s.getIntensityAt(0) < s.getIntensityAt(2));
        assertTrue("for ferrum: third peak is greater than second peak", s.getIntensityAt(1) < s.getIntensityAt(2));
    }
	
	@Test
	public void testChargedPatternGeneration() {
		final PatternGenerator generator = new PatternGenerator(PeriodicTable.getInstance().ionByName("[M+Na]+").getIonization(), Normalization.Max(100));
		final PatternGenerator generatorRaw = 
				new PatternGenerator(new Charge(1), Normalization.Max(100));
		final MolecularFormula glucose = MolecularFormula.parse("C6H12O6");
		final MolecularFormula glucoseIonized = MolecularFormula.parse("C6H12O6Na");
		final Spectrum<? extends Peak> spectrum = generator.generatePattern(glucose, 5);
		final Spectrum<? extends Peak> spectrum2 = generatorRaw.generatePattern(glucoseIonized, 5);
		assertTrue(Spectrums.haveEqualPeaks(spectrum, spectrum2));
	}
	
	@Test
	public void testMultipleChargedPatternGeneration() {
        /*
		final PatternGenerator generator = new PatternGenerator(new Charge(2), Normalization.Max(100));
		final PatternGenerator generatorRaw = 
				new PatternGenerator(new Charge(1), Normalization.Max(100));
		final MolecularFormula glucose = MolecularFormula.parse("C6H12O6");
        final MolecularFormula proton = MolecularFormula.parse("H");
		final Spectrum<? extends Peak> spectrum = generator.generatePattern(glucose.add(proton.multiply(2)), 5);
		final Spectrum<? extends Peak> spectrum2 = generatorRaw.generatePattern(glucose.add(proton), 5);
		spectrum.equals(spectrum2);
        final SimpleMutableSpectrum s = new SimpleMutableSpectrum(Spectrums.neutralMassSpectrum(spectrum, new Charge(2)));
        Spectrums.addOffset(s, -proton.getMass(), 0);
		assertTrue(Spectrums.haveEqualPeaks(s, spectrum2));
		*/
	}
}
