
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
        final Random random = new Random(10323L);
        for (int i=0; i < 1000; ++i) {
            spec.addPeak(new SimplePeak(random.nextDouble(), 1d));
        }
        Spectrums.sortSpectrumByMass(spec);
        for (int i=1; i < 1000; ++i) {
            assertTrue((spec.getMzAt(i) + " >= " + spec.getMzAt(i-1)), spec.getMzAt(i) >= spec.getMzAt(i-1));
        }
    }

}
