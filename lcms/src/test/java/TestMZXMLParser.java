import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.chromatogram.MassTrace;
import de.unijena.bioinf.lcms.chromatogram.MassTraceBuilder;
import de.unijena.bioinf.lcms.chromatogram.MassTraceCache;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bioinf.recal.ChebychevPolynomialFunction;
import de.unijena.bioinf.recal.MzRecalibration;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class TestMZXMLParser {


    @Test
    public void testTraces() {
        final File mzxmlFile = new File("/home/kaidu/analysis/large.mzXML");
        InMemoryStorage storage= new InMemoryStorage();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();
        try {
            final LCMSRun parse = new MzXMLParser().parse(mzxmlFile, storage);
            final ProcessedSample sample = i.addSample(parse, storage);
            final MassTraceCache cache = new MassTraceCache();
            final MassTraceBuilder b = new MassTraceBuilder();
            Scan before = null;
            for (Scan s : sample.run.getScans()) {
                if (s.isMsMs()) {
                    // build mass trace
                    if (before!=null) {
                        b.detect(cache, sample, before, s.getPrecursor().getMass());
                    }

                } else before = s;
            }

            try (PrintStream ap = new PrintStream("traces.csv")) {
                ap.println("span\tintensity\tabs\trel\taboveNoise");
                for (MassTrace trace : cache.getAllMassTraces()) {
                    for (int k=1; k < trace.size(); ++k) {
                        long span = trace.getRetentionTimeAtIndex(k) - trace.getRetentionTimeAtIndex(k-1);
                        double abs = trace.getIntensityAtIndex(k) - trace.getIntensityAtIndex(k-1);
                        double rel = trace.getIntensityAtIndex(k) / trace.getIntensityAtIndex(k-1);
                        final double noiseLevel = sample.ms1NoiseModel.getNoiseLevel(trace.getScanPointAtIndex(k).getScanNumber(), trace.getMzAtIndex(k));
                        System.out.println(noiseLevel);
                        final double intensity = (trace.getIntensityAtIndex(k)+trace.getIntensityAtIndex(k-1))/2d;
                        ap.printf("%d\t%f\t%f\t%f\t%d\n", span, intensity, abs, rel, intensity >= 10*noiseLevel ? 1 : 0);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSplines() {
        final double[] Y = new double[]{
                0,
                0,
                0.1,
                0.2,
                0.3,
                0.4,
                0.5,
                0.6,
                0.7,
                0.8,
                0.9,
                1.0,
                3.0
        };
        final double[] X = new double[]{
                0.0, 0.05,0.14,0.23,0.32,0.41,0.42,0.59,0.68,0.77,0.86,0.95,3.0
        };

        {
            final ChebychevPolynomialFunction firstOrderChebyshevRecalibration = MzRecalibration.getFirstOrderChebyshevRecalibration(X, Y);
            final double[] Z = new double[X.length];
            for (int k=0; k < X.length; ++k)
                Z[k] = firstOrderChebyshevRecalibration.value(X[k]);

            System.out.println(Arrays.toString(Z));
        }
        {
            final double[] X2 = new double[]{
                    0.05,0.14,0.23,0.32,0.41,0.42,0.59,0.68,0.77,0.86,0.95,2.0,3.0
            };
            final PolynomialSplineFunction interpolate = new LoessInterpolator().interpolate(X, Y);
            final double[] Z = new double[X2.length];
            for (int k=0; k < X2.length; ++k)
                Z[k] = interpolate.value(X2[k]);


            System.out.println(Arrays.toString(Z));
        }



    }

    @Test
    public void testMs2CosineDetector() {
        final LCMSProccessingInstance instance = new LCMSProccessingInstance();
        for (File f : Arrays.stream(new File("/home/kaidu/analysis/example2").listFiles()).toArray(File[]::new)) {
            try {
                final MemoryFileStorage storage = new MemoryFileStorage();
                final LCMSRun run = new MzXMLParser().parse(f, storage);
                final ProcessedSample processedSample = instance.addSample(run, storage);
                instance.detectFeatures(processedSample);
                final TDoubleArrayList peakSlope = new TDoubleArrayList(), peakWidths = new TDoubleArrayList();
                final TIntArrayList segments = new TIntArrayList();
                for (FragmentedIon ion : processedSample.ions) {
                    segments.add(ion.getPeak().getSegments().size());
                }
                segments.sort();
                final int medianNumberOfSegments = segments.get(segments.size()/2);
                System.out.println("Median number of segments = " + medianNumberOfSegments);

                for (FragmentedIon ion : processedSample.ions) {
                    final ChromatographicPeak p = ion.getPeak();
                    if (p.getSegments().size()<=medianNumberOfSegments) {

                        for (ChromatographicPeak.Segment s : p.getSegments()) {
                            peakWidths.add(s.fwhm());
                            peakSlope.add(p.getIntensityAt(s.getApexIndex()) / (p.getIntensityAt(s.getStartIndex())+p.getIntensityAt(s.getEndIndex())));
                        }
                    }
                }

                peakWidths.sort();
                peakSlope.sort();
                System.out.println("Median Peak Width = " + peakWidths.getQuick(peakWidths.size()/2));
                System.out.println("Median Peak Slope = " + peakSlope.getQuick(peakSlope.size()/2));
                System.out.println(peakWidths);

                System.out.println(peakSlope);



                storage.backOnDisc();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final ConsensusFeature[] consensusFeatures = instance.makeConsensusFeatures(instance.alignAndGapFilling());

        try (final BufferedWriter BW = FileUtils.getWriter(new File("features.ms"))) {
            for (ConsensusFeature f : consensusFeatures) {
                new JenaMsWriter().write(BW, f.toMs2Experiment());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
