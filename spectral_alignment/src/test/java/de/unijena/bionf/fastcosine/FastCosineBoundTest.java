package de.unijena.bionf.fastcosine;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FastCosineBoundTest {
    private static double parentMass = 432.10556;
    private static double[] testMz = new double[]{
            432.10556,
            255.069992,
            233.085526,
            226.043472,
            216.059357,
            206.037506,
            200.027679,
            192.059204,
            191.051254,
            190.04361,
            189.035843,
            178.043579,
            174.024658,
            167.027542,
            163.032471,
            161.016817,
            157.063507,
            156.985107,
            156.055514,
            150.024658,
            149.016556,
            148.021576,
            146.071274,
            136.021482,
            132.068085,
            131.06012,
            128.990099,
            123.013659,
            119.060308,
            111.001074,
            110.059963,
            108.983874,
            105.044662,
            100.008858,
            98.005821,
            96.044309,
            94.021242,
            92.049384,
            86.005814,
            84.998006,
            80.0494,
            79.04157,
            78.033751,
            74.015015,
            74.005814,
            65.03852,
            58.994967,
            57.987153,
            53.038572,
            51.022932
    };
    private static double[] testInt = new double[]{
            0.47470907654633154,
            0.006985935042585999,
            0.03143409881594466,
            0.03334451059631093,
            0.0024731836901714342,
            0.0025699301226449083,
            0.05964294696370634,
            0.015875581537906525,
            0.018407526661371725,
            1.0,
            0.018631683590225902,
            0.03221716880749104,
            0.002393293476483791,
            0.0019084880852395223,
            0.28839564525280154,
            0.0024776046117371436,
            0.05108744723894856,
            0.16340348137348515,
            0.004009498901663985,
            0.005743345244796518,
            0.0028604209625316796,
            0.002319733833625277,
            0.01180429904172514,
            0.016227640942071884,
            0.003015608110124582,
            0.004943649311005712,
            0.002161898696444069,
            0.0025031687296264054,
            0.007195986816116189,
            0.005719892068074468,
            0.002473425011138037,
            0.010796969811277875,
            0.18568583561230953,
            0.018028116893811445,
            0.0434844581772511,
            0.005950309156541569,
            0.0019521272081576785,
            0.0027959095379066668,
            0.09965950679325965,
            0.0032029288435369767,
            0.02591875522647891,
            0.004206387977802471,
            0.01616960743109901,
            0.004604014530225928,
            0.005175670571365728,
            0.005845423412398373,
            0.011391824252110343,
            0.00478247684115614,
            0.009785887387917205,
            0.012338946796069623
    };

    private static final double SHIFT = PrecursorIonType.getPrecursorIonType("[M+Na]+").getIonization().getMass() - PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization().getMass();

    static SimpleSpectrum makeMixedTestSpectrumSameCompound(int seed, double baseIntensity) {
        final Random R = new Random(seed);
        final Deviation massdev = new Deviation(2);
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
        for (int i=0; i < testMz.length; ++i) {
            final double intensity = testInt[i] * Math.max(0.01,R.nextGaussian(1, 0.25));
            final boolean mask = R.nextDouble() <= Math.pow(intensity, 0.25);
            if (mask) {
                final double shift = R.nextDouble()>=0.5 ? SHIFT : 0d;
                buf.addPeak(shift + (testMz[i] + R.nextGaussian(0,massdev.absoluteFor(testMz[i]))), intensity*baseIntensity );
            }
        }
        return new SimpleSpectrum(buf);
    }

    static SimpleSpectrum makeShiftedTestSpectrumSameCompound(int seed, double baseIntensity) {
        final Random R = new Random(seed);
        final Deviation massdev = new Deviation(2);
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
        for (int i=0; i < testMz.length; ++i) {
            final double intensity = testInt[i] * Math.max(0.01,R.nextGaussian(1, 0.25));
            final boolean mask = R.nextDouble() <= Math.pow(intensity, 0.25);
            if (mask) {
                buf.addPeak(SHIFT + (testMz[i] + R.nextGaussian(0,massdev.absoluteFor(testMz[i]))), intensity*baseIntensity );
            }
        }
        return new SimpleSpectrum(buf);
    }

    static SimpleSpectrum makeTestSpectrumSameCompound(int seed, double baseIntensity) {
        final Random R = new Random(seed);
        final Deviation massdev = new Deviation(2);
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
        for (int i=0; i < testMz.length; ++i) {
            final double intensity = testInt[i] * Math.max(0.01,R.nextGaussian(1, 0.25));
            final boolean mask = R.nextDouble() <= Math.pow(intensity, 0.25);
            if (mask) {
                buf.addPeak(testMz[i] + R.nextGaussian(0,massdev.absoluteFor(testMz[i])), intensity*baseIntensity );
            }
        }
        return new SimpleSpectrum(buf);
    }

    @Test
    public void testMergedLowerboundCosine() {
        final List<SearchPreparedSpectrum> spectra = new ArrayList<>();
        final FastCosine fastCosine = new FastCosine();
        for (int i=0; i < 10; ++i) {
            spectra.add(fastCosine.prepareQuery(parentMass, makeTestSpectrumSameCompound(93*(i+1), 1000)));
        }
        final SearchPreparedMergedSpectrum merged = fastCosine.prepareMergedQuery(spectra);
        final SearchPreparedSpectrum query = fastCosine.prepareQuery(parentMass, makeTestSpectrumSameCompound(1234, 2000));
        // the cosine of query against merged is a upperbound of query against any of the spectra!
        double maxCosine = 0d;
        for (int i=0; i < 10; ++i) {
            maxCosine = Math.max(fastCosine.fastCosine(query, spectra.get(i)).similarity, maxCosine);
        }
        final double upperbound = fastCosine.fastCosine(query, merged.asUpperboundQuerySpectrum()).similarity;
        assertTrue("cosine against merged upperbound spectrum is an upperbound to maximum cosine against all individual spectra.",
                maxCosine <= upperbound);

    }

    @Test
    public void testCompareWithSpectralAlignment() {
        final SimpleSpectrum a = makeTestSpectrumSameCompound(1234, 12345678);
        final SimpleSpectrum b = makeTestSpectrumSameCompound(8976, 6732012);
        final FastCosine fastCosine = new FastCosine();
        final SpectralSimilarity fast = fastCosine.fastCosine(fastCosine.prepareQuery(parentMass, a), fastCosine.prepareQuery(parentMass, b));
        final CosineQueryUtils utils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(fastCosine.getMaxDeviation()));
        final SpectralSimilarity base = utils.cosineProduct(utils.createQuery(a, parentMass, true, false),
                utils.createQuery(b, parentMass, true, false));

        // shared peaks can be different due to noise thresholds in fast cosine!
        assertEquals(base.sharedPeaks, fast.sharedPeaks);
        assertEquals(base.similarity, fast.similarity, 0.01);


        // runtime test
        {
            final SearchPreparedSpectrum aa = fastCosine.prepareQuery(parentMass, a);
            final SearchPreparedSpectrum bb = fastCosine.prepareQuery(parentMass, b);
            long t1 = System.currentTimeMillis();
            double cosine = 0d;
            for (int k=0; k < 1000000; ++k) {
                cosine += fastCosine.fastCosine(aa,bb).similarity;
            }
            long t2 = System.currentTimeMillis();
            System.out.println((cosine/1000000d) + " <= " + (t2-t1) + " ms");
        }
        {
            final CosineQuerySpectrum aa = utils.createQuery(a,  parentMass, true, false);
            final CosineQuerySpectrum bb = utils.createQuery(b,  parentMass, true, false);
            long t1 = System.currentTimeMillis();
            double cosine = 0d;
            for (int k=0; k < 1000000; ++k) {
                cosine += utils.cosineProduct(aa,bb).similarity;
            }
            long t2 = System.currentTimeMillis();
            System.out.println((cosine/1000000d) + " <= " + (t2-t1) + " ms");
        }

    }


    @Test
    public void testCompareReverseCosineWithSpectralAlignment() {
        final SimpleSpectrum a = makeTestSpectrumSameCompound(4821, 12345678);
        final SimpleSpectrum b = makeShiftedTestSpectrumSameCompound(91285, 6732012);
        final FastCosine fastCosine = new FastCosine();
        final SpectralSimilarity fast = fastCosine.fastReverseCosine(fastCosine.prepareQuery(parentMass, a), fastCosine.prepareQuery(parentMass+SHIFT, b));
        final CosineQueryUtils utils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(fastCosine.getMaxDeviation()));
        final SpectralSimilarity base = utils.cosineProductOfInverse(utils.createQuery(a, parentMass, true, false),
                utils.createQuery(b, parentMass+SHIFT, true, false));

        // shared peaks can be different due to noise thresholds in fast cosine!
        assertEquals(base.sharedPeaks, fast.sharedPeaks);
        assertEquals(base.similarity, fast.similarity, 0.01);


        // runtime test
        {
            final SearchPreparedSpectrum aa = fastCosine.prepareQuery(parentMass, a);
            final SearchPreparedSpectrum bb = fastCosine.prepareQuery(parentMass+SHIFT, b);
            long t1 = System.currentTimeMillis();
            double cosine = 0d;
            for (int k=0; k < 1000000; ++k) {
                cosine += fastCosine.fastReverseCosine(aa,bb).similarity;
            }
            long t2 = System.currentTimeMillis();
            System.out.println((cosine/1000000d) + " <= " + (t2-t1) + " ms");
        }
        {
            final CosineQuerySpectrum aa = utils.createQuery(a,  parentMass, true, false);
            final CosineQuerySpectrum bb = utils.createQuery(b,  parentMass+SHIFT, true, false);
            long t1 = System.currentTimeMillis();
            double cosine = 0d;
            for (int k=0; k < 1000000; ++k) {
                cosine += utils.cosineProductOfInverse(aa,bb).similarity;
            }
            long t2 = System.currentTimeMillis();
            System.out.println((cosine/1000000d) + " <= " + (t2-t1) + " ms");
        }

    }


    @Test
    public void testCompareModifiedCosineWithSpectralAlignment() {
        final SimpleSpectrum a = makeTestSpectrumSameCompound(4821, 12345678);
        final SimpleSpectrum b = makeMixedTestSpectrumSameCompound(91285, 6732012);
        final FastCosine fastCosine = new FastCosine();
        SearchPreparedSpectrum aa = fastCosine.prepareQuery(parentMass, a);
        SearchPreparedSpectrum bb = fastCosine.prepareQuery(parentMass + SHIFT, b);
        final SpectralSimilarity fast = fastCosine.fastModifiedCosine(aa, bb);
        System.out.println(fast);
        final SpectralSimilarity rev = fastCosine.fastReverseCosine(aa,bb);
        final SpectralSimilarity dot = fastCosine.fastCosine(aa,bb);
        assertTrue("modified cosine should be at least as high as maximum of reverse and cosine", fast.similarity >= Math.max(rev.similarity, dot.similarity) );
        assertEquals("If the spectrum is a perfect decomposition, modified cosine should be sum of cosine and reverse",
                rev.similarity + dot.similarity, fast.similarity, 0.01 );


    }
}
