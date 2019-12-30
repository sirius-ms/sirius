package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpectrumsTest {


    final static long seed = 1077;

    @Test
    public void testMedian() {
        final Random r = new Random(seed);
        for (int k=0; k < 12; ++k) {
            final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
            for (int l=0; l < 1<<k; ++l) {
                spec.addPeak(r.nextDouble()*100d, r.nextDouble()*10000d);
            }
            final double m1 = Spectrums.__getMedianIntensity(spec);
            final double m2 = Spectrums.getMedianIntensity(spec);
            assertEquals(m1, m2, 1);
        }


    }

    @Test
    public void testSort() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(new SimplePeak(5,3));
        spectrum.addPeak(new SimplePeak(3,2));
        spectrum.addPeak(new SimplePeak(1,4));
        Spectrums.sortSpectrumByMass(spectrum);
        Peak last = null;
        for (Peak p : spectrum) {
            if (last != null) {
                assertTrue(p.getMass()>last.getMass());
            }
        }
        // test small spectrum
        {
            SimpleSpectrum sp = new SimpleSpectrum(new double[]{5, 3, 1, 2, 8, 4, 11, 2, 10, 1}, new double[]{2, 3, 4, 5, 2, 1, 1, 2, 1, 3});
            assertTrue(sp instanceof OrderedSpectrum);
            last = null;
            for (Peak p : sp) {
                if (last != null) {
                    assertTrue(p.getMass() > last.getMass());
                }
            }
        }
        // test large spectrum
        {
            final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
            final Random r = new Random(seed);
            for (int i=0; i < 5000; ++i) {
                spec.addPeak(Math.abs(r.nextDouble()), Math.abs(r.nextDouble()));
            }
            Spectrums.sortSpectrumByMass(spec);
            boolean isSorted = true;
            for (int i=1; i < spec.size(); ++i) {
                if (spec.getMzAt(i) < spec.getMzAt(i-1)) isSorted = false;
            }
            assertTrue("spectrum should be sorted", isSorted);
        }

        // test large spectrum with a lot of zeros
        {
            final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
            final Random r = new Random(seed);
            for (int i=0; i < 5000; ++i) {
                spec.addPeak(0, Math.abs(r.nextDouble()));
            }
            for (int i=0; i <= 50; ++i) {
                spec.setMzAt(r.nextInt(spec.size()), Math.abs(r.nextDouble()));
            }
            Spectrums.sortSpectrumByMass(spec);
            boolean isSorted = true;
            for (int i=1; i < spec.size(); ++i) {
                if (spec.getMzAt(i) < spec.getMzAt(i-1)) isSorted = false;
            }
            assertTrue("spectrum should be sorted", isSorted);
        }

    }

    @Test
    public void testSearch() {
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        spectrum.addPeak(new SimplePeak(5,3));
        spectrum.addPeak(new SimplePeak(3,2));
        spectrum.addPeak(new SimplePeak(1,4));
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

    @Test
    public void testFilterIsotpes(){
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
        //C36H37N5
        spectrum.addPeak(new SimplePeak(540.3121726360905, 0.6640867687951271));
        spectrum.addPeak(new SimplePeak(541.3152753047328, 0.2737396761795133));
        spectrum.addPeak(new SimplePeak(542.318370128813, 0.05499724422532549));
        spectrum.addPeak(new SimplePeak(543.3214567823122, 0.007176310800034031));


        //C27H33N13
        spectrum.addPeak(new SimplePeak(540.3054645080905, 0.7104014905351598));
        spectrum.addPeak(new SimplePeak(541.307970285248, 0.2443438820957737));
        spectrum.addPeak(new SimplePeak(542.3104462599396, 0.0408362176639628));
        spectrum.addPeak(new SimplePeak(543.3128911877274, 0.004418409705103798));

        //C36H38N5
        spectrum.addPeak(new SimplePeak(541.3199976680905, 0.6640109468088424));
        spectrum.addPeak(new SimplePeak(542.323101222116, 0.27378479204311157));
        spectrum.addPeak(new SimplePeak(543.3261969812916, 0.05502244501368937));
        spectrum.addPeak(new SimplePeak(544.3292846236769, 0.007181816134356579));

        ChemicalAlphabet alphabet = new ChemicalAlphabet();
        Deviation deviation = new Deviation(10);

//        //noise
//        for (int i = 0; i < spectrum.size(); i++) {
//            double mass = spectrum.getMzAt(i);
//            double absDev = deviation.absoluteFor(mass)/2;
//            double newMass = mass+absDev*Math.random()-absDev/2; //just use half dev for noise;
//            spectrum.setMzAt(i, newMass);
//        }

        Spectrums.filterIsotpePeaks(spectrum, deviation, 0.2, 0.55, 4, alphabet);

        assertEquals(3, spectrum.size());
        assertEquals(new SimplePeak(540.3054645080905, 0.7104014905351598), spectrum.getPeakAt(0));
        assertEquals(new SimplePeak(540.3121726360905, 0.6640867687951271), spectrum.getPeakAt(1));
        assertEquals(new SimplePeak(541.3199976680905, 0.6640109468088424), spectrum.getPeakAt(2));

    }

}
