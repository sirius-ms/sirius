package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import junit.framework.TestCase;
import org.junit.Test;

public class SpectrumTest extends TestCase {

	@Test
	public void testSort() {
		SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
		spectrum.addPeak(new Peak(5,3));
		spectrum.addPeak(new Peak(3,2));
		spectrum.addPeak(new Peak(1,4));
		Spectrums.sortSpectrumByMass(spectrum);
		Peak last = null;
		for (Peak p : spectrum) {
			if (last != null) {
				assertTrue(p.mass>last.mass);
			}
		}
		
		SimpleSpectrum sp = new SimpleSpectrum(new double[]{5,3,1,2,8,4,11,2,10,1}, new double[]{2,3,4,5,2,1,1,2,1,3});
		assertTrue(sp instanceof OrderedSpectrum);
		last = null;
		for (Peak p : sp) {
			if (last != null) {
				assertTrue(p.mass>last.mass);
			}
		}
	}
	
	@Test
	public void testSearch() {
		SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
		spectrum.addPeak(new Peak(5,3));
		spectrum.addPeak(new Peak(3,2));
		spectrum.addPeak(new Peak(1,4));
		Spectrums.sortSpectrumByMass(spectrum);
		assertTrue(Spectrums.binarySearch(spectrum, 18)<0);
		assertEquals(2, Spectrums.binarySearch(spectrum, 5));
		assertEquals(1, Spectrums.binarySearch(spectrum, 3));
		assertEquals(0, Spectrums.binarySearch(spectrum,1));
		
		SimpleSpectrum sp = new SimpleSpectrum(new double[]{1,2,3,5}, new double[]{2,3,4,5});
		assertTrue(Spectrums.binarySearch(sp, 18)<0);
		assertEquals(3, Spectrums.binarySearch(sp, 5));
		assertEquals(2, Spectrums.binarySearch(sp, 3));
		assertEquals(1, Spectrums.binarySearch(sp, 2));
		assertEquals(0, Spectrums.binarySearch(sp, 1));
	}
	
}
